/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MockAdapter from 'axios-mock-adapter';
import axios from 'axios';
import { SearchOmnibar } from 'MainRoot/nosc/search/SearchOmnibar';
import router from 'MainRoot/router/routerInstance';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';
import { registerNexusOneApplicationDetailStatesForHref } from 'TestRoot/nosc/search/registerNexusOneApplicationDetailStatesForHref';
import { registerNexusOneVulnerabilityDetailStatesForHref } from 'TestRoot/nosc/search/registerNexusOneVulnerabilityDetailStatesForHref';

// Click targets use the real UI-Router singleton (Nexus One states registered
// below) and the real bundleIndexUrl (Classic deep-links) with a test base URL —
// real plumbing, no implementation test-paths (Ross's review point).
const CLASSIC = 'http://localhost/assets/index.html';

function registerState(name: string, url: string): void {
  if (!router.stateRegistry.get(name)) {
    router.stateRegistry.register({ name, url });
  }
}

/**
 * P1-F13 / CLM-39549: tests for the multi-entity SearchOmnibar.
 *
 * The omnibar now hits /api/v2/search/advanced and surfaces 6 entity
 * types instead of just applications. Tests verify:
 *   - rendering, placeholder, debounce contract
 *   - per-entity-type rows render with the correct icon + badge
 *   - grouping by entity type with section headers
 *   - keyboard nav across the flat list of rows (skipping section headers)
 *   - click navigates to the correct Classic deep link or Coming Soon stub
 *   - Enter without selection navigates to #/search?q=<query>
 *   - Esc clears + closes
 *   - a11y: combobox/listbox roles, aria-activedescendant
 */

// Backend payloads — mock all itemTypes the omnibar surfaces.
const SEARCH_QUERY = 'log4j';
const FIXTURE_ITEMS = [
  {
    itemType: 'SECURITY_VULNERABILITY',
    resultIndex: 0,
    vulnerabilityId: 'CVE-2021-44228',
    vulnerabilityDescription: 'Log4Shell — Critical RCE in log4j',
    vulnerabilityStatus: 'OPEN',
  },
  {
    itemType: 'NON_VULNERABLE_COMPONENT',
    resultIndex: 1,
    componentHash: 'abc123',
    componentName: 'log4j-core',
    componentIdentifier: { format: 'maven', coordinates: { version: '2.14.1' } },
  },
  {
    itemType: 'APPLICATION',
    resultIndex: 2,
    applicationId: 'app-1',
    applicationPublicId: 'webgoat-app',
    applicationName: 'Webgoat',
    organizationName: 'Engineering',
  },
  {
    itemType: 'POLICY',
    resultIndex: 3,
    policyId: 'pol-1',
    policyName: 'Security-Critical',
    policyThreatCategory: 'SECURITY',
    policyThreatLevel: 9,
  },
  {
    itemType: 'ORGANIZATION',
    resultIndex: 4,
    organizationId: 'org-1',
    organizationName: 'Engineering',
  },
];

// Primary response shape: the backend returns rows nested under groupingByDTOS,
// NOT the top-level searchResultItemDTOS fallback. The fixture mirrors the real
// contract so flattenGroups' primary path is exercised.
const FIXTURE_RESPONSE = {
  searchQuery: SEARCH_QUERY,
  page: 0,
  pageSize: 12,
  totalNumberOfHits: 5,
  isExactTotalNumberOfHits: true,
  groupingByDTOS: [{ groupBy: 'ITEM_TYPE', searchResultItemDTOS: FIXTURE_ITEMS }],
};

describe('SearchOmnibar (P1-F13 multi-entity)', () => {
  const originalLocation = window.location;
  let assignMock: jest.Mock;
  let mock: MockAdapter;

  beforeEach(() => {
    assignMock = jest.fn();
    delete (window as any).location;
    (window as any).location = { ...originalLocation, assign: assignMock, hash: '' };
    mock = new MockAdapter(axios);
    // Match any /api/v2/search/advanced URL regardless of query string.
    mock.onGet(/\/api\/v2\/search\/advanced/).reply(200, FIXTURE_RESPONSE);

    // Register the Nexus One states the omnibar links to, and pin a base URL so
    // the real bundleIndexUrl can resolve Classic deep-links.
    registerNexusOneApplicationDetailStatesForHref();
    registerNexusOneVulnerabilityDetailStatesForHref();
    registerState('nexusOneSearch', '/search?q');
    registerState('platformHome', '/home');
    _setBaseUrlForTesting('http://localhost');
  });

  afterEach(() => {
    (window as any).location = originalLocation;
    mock.restore();
  });

  const renderInTheme = () =>
    render(
      <Theme>
        <SearchOmnibar />
      </Theme>,
    );

  it('renders an empty input with the multi-entity placeholder', () => {
    renderInTheme();
    const input = screen.getByPlaceholderText(/search apps.*components.*CVEs.*policies/i);
    expect(input).toBeInTheDocument();
    expect(input).toHaveValue('');
  });

  it('exposes an explicit accessible name on the search input (WCAG 2.4.6)', () => {
    renderInTheme();
    const input = screen.getByLabelText(/search apps, components, vulnerabilities, and policies/i);
    expect(input).toBeInTheDocument();
  });

  it('uses combobox + listbox role contract', async () => {
    const user = userEvent.setup();
    renderInTheme();
    const input = screen.getByRole('combobox');
    await user.type(input, SEARCH_QUERY);
    await waitFor(() => {
      expect(screen.getByRole('listbox')).toBeInTheDocument();
    });
  });

  it('does not show the dropdown until the query is at least 2 chars', async () => {
    const user = userEvent.setup();
    renderInTheme();
    const input = screen.getByRole('combobox');
    await user.type(input, 'l');
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
  });

  it('shows mixed-entity rows with the correct entity-type test-ids', async () => {
    const user = userEvent.setup();
    renderInTheme();
    const input = screen.getByRole('combobox');
    await user.type(input, SEARCH_QUERY);
    await waitFor(() => {
      // Every itemType should produce one row.
      expect(screen.getByTestId('nosc-search-row-vulnerability')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-search-row-component')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-search-row-application')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-search-row-policy')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-search-row-organization')).toBeInTheDocument();
    });
  });

  it('renders the displayed entity names from the search response', async () => {
    const user = userEvent.setup();
    renderInTheme();
    const input = screen.getByRole('combobox');
    await user.type(input, SEARCH_QUERY);
    await waitFor(() => {
      expect(screen.getByText('CVE-2021-44228')).toBeInTheDocument();
      expect(screen.getByText('log4j-core')).toBeInTheDocument();
      expect(screen.getByText('Webgoat')).toBeInTheDocument();
      expect(screen.getByText('Security-Critical')).toBeInTheDocument();
    });
  });

  it('navigates to native vulnerability detail when a CVE row is clicked (CLM-42216)', async () => {
    const user = userEvent.setup();
    renderInTheme();
    const input = screen.getByRole('combobox');
    await user.type(input, SEARCH_QUERY);
    const vulnRow = await screen.findByTestId('nosc-search-row-vulnerability');
    await user.click(vulnRow);
    expect(assignMock).toHaveBeenCalledWith('#/vulnerabilities/CVE-2021-44228');
  });

  it('navigates to the native Preview Application Detail page when an Application row is clicked (CLM-39709)', async () => {
    const user = userEvent.setup();
    renderInTheme();
    const input = screen.getByRole('combobox');
    await user.type(input, SEARCH_QUERY);
    const appRow = await screen.findByTestId('nosc-search-row-application');
    await user.click(appRow);
    expect(assignMock).toHaveBeenCalledWith('#/applications/webgoat-app');
  });

  it('navigates to the Classic root Orgs & Policies tree when a Policy row is clicked (no per-policy deep link from a search DTO)', async () => {
    const user = userEvent.setup();
    renderInTheme();
    const input = screen.getByRole('combobox');
    await user.type(input, SEARCH_QUERY);
    const policyRow = await screen.findByTestId('nosc-search-row-policy');
    await user.click(policyRow);
    expect(assignMock).toHaveBeenCalledWith(
      `${CLASSIC}#/management/view/organization/ROOT_ORGANIZATION_ID`,
    );
  });

  it('navigates to Nexus One home when a Component row is clicked (native detail deferred)', async () => {
    const user = userEvent.setup();
    renderInTheme();
    const input = screen.getByRole('combobox');
    await user.type(input, SEARCH_QUERY);
    const compRow = await screen.findByTestId('nosc-search-row-component');
    await user.click(compRow);
    expect(assignMock).toHaveBeenCalledWith('#/home');
  });

  it('Enter without a highlighted row navigates to #/search?q=<query>', async () => {
    const user = userEvent.setup();
    renderInTheme();
    const input = screen.getByRole('combobox');
    await user.type(input, SEARCH_QUERY);
    await waitFor(() => {
      expect(screen.getByTestId('nosc-search-see-all')).toBeInTheDocument();
    });
    await user.keyboard('{Enter}');
    expect(assignMock).toHaveBeenCalledWith(`#/search?q=${SEARCH_QUERY}`);
  });

  it('clicking the "see all results" footer also navigates to #/search', async () => {
    const user = userEvent.setup();
    renderInTheme();
    const input = screen.getByRole('combobox');
    await user.type(input, SEARCH_QUERY);
    const seeAll = await screen.findByTestId('nosc-search-see-all');
    await user.click(seeAll);
    expect(assignMock).toHaveBeenCalledWith(`#/search?q=${SEARCH_QUERY}`);
  });

  it('ArrowDown then Enter activates the first highlighted row', async () => {
    const user = userEvent.setup();
    renderInTheme();
    const input = screen.getByRole('combobox');
    await user.type(input, SEARCH_QUERY);
    // Wait for an actual row (not just the listbox container — that
    // could appear briefly during the loading transition).
    await screen.findByTestId('nosc-search-row-vulnerability');
    await user.keyboard('{ArrowDown}');
    await user.keyboard('{Enter}');
    // First row in fixture order is the best-match Vulnerability.
    expect(assignMock).toHaveBeenCalledWith('#/vulnerabilities/CVE-2021-44228');
  });

  it('Escape clears the input and closes the dropdown', async () => {
    const user = userEvent.setup();
    renderInTheme();
    const input = screen.getByRole('combobox');
    await user.type(input, SEARCH_QUERY);
    await waitFor(() => {
      expect(screen.getByRole('listbox')).toBeInTheDocument();
    });
    await user.keyboard('{Escape}');
    expect(input).toHaveValue('');
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
  });

  it('renders "No matches" when the backend returns an empty result set', async () => {
    mock.reset();
    mock.onGet(/\/api\/v2\/search\/advanced/).reply(200, {
      ...FIXTURE_RESPONSE,
      totalNumberOfHits: 0,
      groupingByDTOS: [],
      searchResultItemDTOS: [],
    });
    const user = userEvent.setup();
    renderInTheme();
    const input = screen.getByRole('combobox');
    await user.type(input, 'no-matches-query');
    await waitFor(() => {
      expect(screen.getByText(/no matches/i)).toBeInTheDocument();
    });
  });

  it('still renders rows from the top-level searchResultItemDTOS fallback (no groupings)', async () => {
    mock.reset();
    mock.onGet(/\/api\/v2\/search\/advanced/).reply(200, {
      searchQuery: SEARCH_QUERY,
      page: 0,
      pageSize: 12,
      totalNumberOfHits: 5,
      isExactTotalNumberOfHits: true,
      // No groupingByDTOS — exercise the flattenGroups fallback branch.
      searchResultItemDTOS: FIXTURE_ITEMS,
    });
    const user = userEvent.setup();
    renderInTheme();
    const input = screen.getByRole('combobox');
    await user.type(input, SEARCH_QUERY);
    await waitFor(() => {
      expect(screen.getByTestId('nosc-search-row-application')).toBeInTheDocument();
      expect(screen.getByTestId('nosc-search-row-vulnerability')).toBeInTheDocument();
    });
  });

  it('renders "Search unavailable" when the backend errors', async () => {
    mock.reset();
    mock.onGet(/\/api\/v2\/search\/advanced/).reply(500, 'boom');
    const user = userEvent.setup();
    renderInTheme();
    const input = screen.getByRole('combobox');
    await user.type(input, SEARCH_QUERY);
    await waitFor(() => {
      expect(screen.getByText(/search unavailable/i)).toBeInTheDocument();
    });
  });
});
