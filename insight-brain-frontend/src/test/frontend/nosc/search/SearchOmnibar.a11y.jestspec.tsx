/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { render, screen, waitFor } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import { axe, toHaveNoViolations } from 'jest-axe';
import MockAdapter from 'axios-mock-adapter';
import axios from 'axios';
import { SearchOmnibar } from 'MainRoot/nosc/search/SearchOmnibar';
import router from 'MainRoot/router/routerInstance';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';
import { SuggestResponse } from 'MainRoot/nosc/search/searchTypes';
import { recentSearchesStorageKey } from 'MainRoot/nosc/search/useRecentSearches';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';

expect.extend(toHaveNoViolations);

/**
 * Axe pass over every panel state the omnibar can reach. The panel is a combobox
 * composite (input + listboxes + a tab strip), which is easy to break in ways
 * only an ARIA rule engine notices — duplicate ids, dangling aria-controls,
 * options outside a listbox, non-option listbox children.
 *
 * Only critical/serious impacts are gated: jsdom cannot evaluate the
 * contrast-dependent rules, and Radix's own portals carry moderate advisories
 * that are not this component's to fix.
 */
const SUGGEST_RE = /\/rest\/search\/suggest/;
const SEARCH_QUERY = 'log4j';

/** Signed-in user these tests run as; recent searches are stored under this account's key. */
const TEST_USERNAME = 'test-user';

const FIXTURE_RESPONSE: SuggestResponse = {
  bestMatch: {
    id: 'CVE-2021-44228',
    type: 'VULNERABILITY',
    source: 'local',
    title: 'CVE-2021-44228',
    subtitle: 'Log4Shell',
  },
  groups: [
    {
      type: 'COMPONENT',
      source: 'local',
      results: [{ id: 'c-1', type: 'COMPONENT', source: 'local', title: 'log4j-core', subtitle: 'maven' }],
    },
    {
      type: 'APPLICATION',
      source: 'local',
      results: [{ id: 'app-1', type: 'APPLICATION', source: 'local', title: 'Webgoat', subtitle: 'webgoat' }],
    },
  ],
};

const EMPTY_RESPONSE: SuggestResponse = { bestMatch: null, groups: [] };

/** Fail only on the impacts jsdom can judge reliably. */
async function expectNoSeriousViolations(container: HTMLElement): Promise<void> {
  const results = await axe(container);
  const blocking = results.violations.filter(
    (violation) => violation.impact === 'critical' || violation.impact === 'serious'
  );
  expect(blocking.map((violation) => `${violation.id} (${violation.impact})`)).toEqual([]);
}

describe('SearchOmnibar accessibility across panel states', () => {
  let mock: MockAdapter;

  beforeAll(() => {
    installRadixJsdomShims();
  });

  beforeEach(() => {
    window.localStorage.clear();
    mock = new MockAdapter(axios);
    mock.onGet(SUGGEST_RE).reply(200, FIXTURE_RESPONSE);
    if (!router.stateRegistry.get('nexusOneSearch')) {
      router.stateRegistry.register({ name: 'nexusOneSearch', url: '/search?q&tab&source' });
    }
    _setBaseUrlForTesting('http://localhost');
  });

  afterEach(() => {
    mock.restore();
  });

  function renderOmnibar() {
    return render(
      <Theme>
        <SearchOmnibar />
      </Theme>,
      {
        preloadedState: {
          productFeatures: {
            productFeatures: { 'catalog-federation': true },
            loading: false,
            loadError: null,
          },
          // Recent searches are keyed by account, so a signed-in user is needed for
          // the panel to render any history.
          userSession: { data: { username: TEST_USERNAME }, loading: false, error: null },
        },
      }
    );
  }

  function input(): HTMLElement {
    return screen.getByTestId('nosc-search-input');
  }

  it('closed: the resting field is clean', async () => {
    const { container } = renderOmnibar();
    await expectNoSeriousViolations(container);
  });

  it('focused-empty: recent searches with no history is clean', async () => {
    const user = userEvent.setup();
    const { container } = renderOmnibar();

    await user.click(input());
    await screen.findByTestId('nosc-search-recent-empty');

    await expectNoSeriousViolations(container);
  });

  it('focused-empty: recent searches with history is clean', async () => {
    window.localStorage.setItem(
      recentSearchesStorageKey(TEST_USERNAME) as string,
      JSON.stringify([
        { q: 'log4j', ts: 2 },
        { q: 'spring', ts: 1 },
      ])
    );
    const user = userEvent.setup();
    const { container } = renderOmnibar();

    await user.click(input());
    await screen.findByTestId('nosc-search-recent-row-0');

    await expectNoSeriousViolations(container);
  });

  it('focused-short: a below-minimum query is clean', async () => {
    const user = userEvent.setup();
    const { container } = renderOmnibar();

    await user.type(input(), 'l');
    await screen.findByTestId('nosc-search-panel-body');

    await expectNoSeriousViolations(container);
  });

  it('loading: an in-flight request is clean', async () => {
    mock.reset();
    mock.onGet(SUGGEST_RE).reply(() => new Promise(() => undefined));
    const user = userEvent.setup();
    const { container } = renderOmnibar();

    await user.type(input(), SEARCH_QUERY);
    await screen.findByTestId('nosc-search-placeholder-loading');

    await expectNoSeriousViolations(container);
  });

  it('loaded-empty: a query with no matches is clean', async () => {
    mock.reset();
    mock.onGet(SUGGEST_RE).reply(200, EMPTY_RESPONSE);
    const user = userEvent.setup();
    const { container } = renderOmnibar();

    await user.type(input(), SEARCH_QUERY);
    await screen.findByTestId('nosc-search-placeholder-empty');

    await expectNoSeriousViolations(container);
  });

  it('loaded: results with the tab strip and row list is clean', async () => {
    const user = userEvent.setup();
    const { container } = renderOmnibar();

    await user.type(input(), SEARCH_QUERY);
    await screen.findByTestId('nosc-search-results-view');
    await screen.findByTestId('nosc-search-panel-tabs');

    await expectNoSeriousViolations(container);
  });

  it('loaded with the filter bar open is clean', async () => {
    const user = userEvent.setup();
    const { container } = renderOmnibar();

    await user.type(input(), SEARCH_QUERY);
    await screen.findByTestId('nosc-search-results-view');
    await user.click(screen.getByTestId('nosc-search-filter-toggle'));
    await screen.findByRole('toolbar', { name: 'Search filters' });

    await expectNoSeriousViolations(container);
  });

  it('loaded with a row highlighted keeps aria-activedescendant resolvable', async () => {
    const user = userEvent.setup();
    const { container } = renderOmnibar();

    await user.type(input(), SEARCH_QUERY);
    await screen.findByTestId('nosc-search-result-row-0');
    await user.keyboard('{ArrowDown}');

    await waitFor(() =>
      expect(input()).toHaveAttribute('aria-activedescendant', 'nosc-search-row-0')
    );
    const activeId = input().getAttribute('aria-activedescendant') ?? '';
    expect(document.getElementById(activeId)).not.toBeNull();

    await expectNoSeriousViolations(container);
  });

  it('never emits a duplicate DOM id in any loaded state', async () => {
    const user = userEvent.setup();
    const { container } = renderOmnibar();

    await user.type(input(), SEARCH_QUERY);
    await screen.findByTestId('nosc-search-results-view');

    const ids = Array.from(container.querySelectorAll('[id]')).map((element) => element.id);
    expect(new Set(ids).size).toBe(ids.length);
  });
});
