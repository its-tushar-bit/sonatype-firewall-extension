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
import { getSearchCatalogUrl } from 'MainRoot/util/CLMLocation';
import router from 'MainRoot/router/routerInstance';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { MOCK_COMPONENTS_CATALOG_RESPONSE } from 'TestRoot/nosc/componentsList/mockComponentsListData';
import { renderNexusOneRoute } from 'TestRoot/nosc/renderNexusOneRoute';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';

describe('ComponentsList', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;

  beforeAll(() => {
    installRadixJsdomShims();
    axiosMock = axiosMockAdapter();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  it('mounts the native Components page and loads catalog rows', async () => {
    axiosMock.onPost(getSearchCatalogUrl()).reply(200, MOCK_COMPONENTS_CATALOG_RESPONSE);

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
    axiosMock.onPost(getSearchCatalogUrl()).reply(200, MOCK_COMPONENTS_CATALOG_RESPONSE);

    renderNexusOneRoute(<ComponentsList />, NEXUS_ONE_COMPONENTS_STATE_NAME, {
      q: 'guava',
      org: 'Java Team',
      ecosystem: 'maven',
    });

    await waitFor(() => {
      const hydrated = axiosMock.history.post.find((request) => {
        const body = JSON.parse(String(request.data));
        return (
          body.entityType === 'COMPONENT'
          && body.source === 'local'
          && body.filters?.query === 'guava'
          && body.filters?.organizations?.includes('Java Team')
          && body.filters?.ecosystems?.includes('maven')
        );
      });
      expect(hydrated).toBeDefined();
    });
  });

  it('writes catalog source into the address bar when the Catalog tab is selected', async () => {
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
