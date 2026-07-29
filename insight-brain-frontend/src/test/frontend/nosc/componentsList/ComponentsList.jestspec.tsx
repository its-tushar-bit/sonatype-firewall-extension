/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ComponentsList from 'MainRoot/nosc/componentsList/ComponentsList';
import { NEXUS_ONE_COMPONENTS_STATE_NAME } from 'MainRoot/nosc/componentsList/componentsRoute';
import { getComponentsListUrl, getSearchCatalogUrl } from 'MainRoot/util/CLMLocation';
import router from 'MainRoot/router/routerInstance';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { MOCK_COMPONENTS_CATALOG_RESPONSE } from 'TestRoot/nosc/componentsList/mockComponentsListData';
import { renderNexusOneRoute } from 'TestRoot/nosc/renderNexusOneRoute';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';

const MOCK_DASHBOARD_RESPONSE = {
  components: [
    {
      hash: 'abc123',
      derivedComponentName: 'guava',
      scoreCritical: 1,
      scoreSevere: 0,
      scoreModerate: 2,
      scoreLow: 0,
      affectedApplications: 3,
    },
    {
      hash: 'def456',
      derivedComponentName: 'commons-lang',
      scoreCritical: 0,
      scoreSevere: 1,
      scoreModerate: 0,
      scoreLow: 1,
      affectedApplications: 1,
    },
  ],
  total: 2,
  page: 0,
  pageSize: 50,
  hasNextPage: false,
  source: 'index',
  facets: {
    totalComponents: 2,
    organizations: { 'org-1': 2 },
    organizationNames: { 'org-1': 'Java Team' },
  },
};

describe('ComponentsList', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;

  beforeAll(() => {
    installRadixJsdomShims();
    axiosMock = axiosMockAdapter();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  it('mounts the native Components page and loads My Scan Data hybrid rows', async () => {
    axiosMock.onPost(getComponentsListUrl()).reply(200, MOCK_DASHBOARD_RESPONSE);

    renderNexusOneRoute(<ComponentsList />, NEXUS_ONE_COMPONENTS_STATE_NAME);

    expect(screen.getByTestId('preview-components-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Components' })).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getAllByTestId('component-card-name')[0]).toHaveTextContent('guava');
    });
    expect(screen.getByTestId('components-toolbar-count')).toHaveTextContent('2 components');
    expect(screen.getByTestId('components-toolbar-csv')).toBeEnabled();
    expect(screen.getByTestId('components-tab-my-scan-data')).toHaveAttribute('aria-selected', 'true');
  });

  it('hydrates list state from deep-linked route params on the first fetch', async () => {
    axiosMock.onPost(getComponentsListUrl()).reply(200, MOCK_DASHBOARD_RESPONSE);

    renderNexusOneRoute(<ComponentsList />, NEXUS_ONE_COMPONENTS_STATE_NAME, {
      q: 'guava',
      // Route org values are friendly names (see componentsRoute); dashboard posts them as organizationIds.
      org: 'Java Team',
    });

    await waitFor(() => {
      const hydrated = axiosMock.history.post.find((request) => {
        if (request.url !== getComponentsListUrl()) return false;
        const body = JSON.parse(String(request.data));
        return body.search === 'guava' && body.organizationIds?.includes('Java Team');
      });
      expect(hydrated).toBeDefined();
    });
  });

  it('writes catalog source into the address bar when the Catalog tab is selected', async () => {
    axiosMock.onPost(getComponentsListUrl()).reply(200, MOCK_DASHBOARD_RESPONSE);
    axiosMock.onPost(getSearchCatalogUrl()).reply(200, {
      ...MOCK_COMPONENTS_CATALOG_RESPONSE,
      source: 'catalog',
    });
    const goSpy = jest.spyOn(router.stateService, 'go');
    const user = userEvent.setup();

    renderNexusOneRoute(<ComponentsList />, NEXUS_ONE_COMPONENTS_STATE_NAME);

    await waitFor(() => {
      expect(screen.getByTestId('components-tab-catalog')).toBeInTheDocument();
    });

    await user.click(screen.getByTestId('components-tab-catalog'));

    await waitFor(() => {
      expect(goSpy).toHaveBeenCalledWith(
        NEXUS_ONE_COMPONENTS_STATE_NAME,
        expect.objectContaining({ source: 'catalog' }),
        expect.objectContaining({ notify: false, location: 'replace' }),
      );
    });
  });
});
