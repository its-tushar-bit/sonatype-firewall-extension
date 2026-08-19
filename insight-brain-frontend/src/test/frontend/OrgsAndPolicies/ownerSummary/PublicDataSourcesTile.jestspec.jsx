/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import PublicDataSourcesTile from 'MainRoot/OrgsAndPolicies/ownerSummary/PublicDataSourcesTile';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import * as publicDataSourcesSelectors from 'MainRoot/OrgsAndPolicies/publicDataSources/publicDataSourcesSelectors';
import router from 'MainRoot/router/routerInstance';

describe('PublicDataSourcesTile', () => {
  let renderComponent, initialState;
  const PUBLIC_DATA_SOURCES_TILE_BASE_MSG = 'Public Data Sources are';
  const PUBLIC_DATA_SOURCES_TILE_TITLE = 'Public Data Sources';

  const mockSelectorsForRenderingTile = (isSbomManager, isCpeMatchingEnabled) => {
    jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(isSbomManager);
    jest.spyOn(productFeaturesSelectors, 'selectIsCpeMatchingSupported').mockReturnValue(isCpeMatchingEnabled);
    jest.spyOn(publicDataSourcesSelectors, 'selectPublicDatasourcesLinkParams').mockReturnValue({
      to: 'public-data-sources-editor',
      params: {
        ownerId: 'ownerId',
        ownerType: 'organization',
      },
    });
  };

  const mockSelectorsForRenderingTileContent = (data) => {
    jest.spyOn(publicDataSourcesSelectors, 'selectCpeConfiguration').mockReturnValue(data);
    jest.spyOn(publicDataSourcesSelectors, 'selectLoading').mockReturnValue(false);
    jest.spyOn(publicDataSourcesSelectors, 'selectLoadError').mockReturnValue(null);
  };

  const getInheritedText = (inheritedFromOrganizationName) => {
    return '(Inherited from ' + inheritedFromOrganizationName + ')';
  };

  beforeEach(() => {
    initialState = {
      orgsAndPolicies: {
        publicDataSources: {
          loading: false,
          data: null,
          loadError: null,
        },
        root: {
          selectedOwner: {
            id: 'ownerId',
            name: 'ownerName',
          },
        },
      },
      router: {
        currentState: { name: 'organization', url: 'fakeUrl' },
        currentParams: {
          organizationId: 'ownerId',
        },
      },
    };

    jest.spyOn(router.stateService, 'href').mockReturnValue('#');
    jest.spyOn(router.stateService, 'get').mockReturnValue('#');
    jest.spyOn(router.stateService, 'includes').mockReturnValue(false);

    renderComponent = async (preloadedState = initialState) => {
      return render(<PublicDataSourcesTile />, { preloadedState });
    };
  });

  describe('Tile is rendered', () => {
    beforeEach(() => {
      mockSelectorsForRenderingTile(false, true);
    });

    it('renders enabled content', () => {
      mockSelectorsForRenderingTileContent({
        enabled: true,
        inheritedFromOrganizationName: null,
      });
      renderComponent({
        orgsAndPolicies: {
          publicDataSources: {
            loading: false,
            data: {
              enabled: true,
              inheritedFromOrganizationName: null,
            },
            loadError: null,
          },
          root: {
            selectedOwner: {
              id: 'ownerId',
              name: 'ownerName',
            },
          },
        },
      });

      expect(screen.getByText(`${PUBLIC_DATA_SOURCES_TILE_TITLE}`)).toBeVisible();
      expect(screen.getByText(`${PUBLIC_DATA_SOURCES_TILE_BASE_MSG} enabled`)).toBeVisible();
    });

    it('renders enabled inherited content', () => {
      mockSelectorsForRenderingTileContent({
        enabled: true,
        inheritedFromOrganizationName: 'Test Org',
      });
      renderComponent();

      expect(screen.getByText(`${PUBLIC_DATA_SOURCES_TILE_TITLE}`)).toBeVisible();
      expect(
        screen.getByText(`${PUBLIC_DATA_SOURCES_TILE_BASE_MSG} enabled ${getInheritedText('Test Org')}`)
      ).toBeVisible();
    });

    it('renders disabled content', () => {
      mockSelectorsForRenderingTileContent({
        enabled: false,
        inheritedFromOrganizationName: null,
      });
      renderComponent();

      expect(screen.getByText(`${PUBLIC_DATA_SOURCES_TILE_TITLE}`)).toBeVisible();
      expect(screen.getByText(`${PUBLIC_DATA_SOURCES_TILE_BASE_MSG} disabled`)).toBeVisible();
    });

    it('renders disabled inherited content', () => {
      mockSelectorsForRenderingTileContent({
        enabled: false,
        inheritedFromOrganizationName: 'Test Org',
      });
      renderComponent();

      expect(screen.getByText(`${PUBLIC_DATA_SOURCES_TILE_TITLE}`)).toBeVisible();
      expect(
        screen.getByText(`${PUBLIC_DATA_SOURCES_TILE_BASE_MSG} disabled ${getInheritedText('Test Org')}`)
      ).toBeVisible();
    });

    it('renders disabled content because selectOwnerPublicDataSourcesInfo selector was null', () => {
      mockSelectorsForRenderingTileContent(null);
      renderComponent();

      expect(screen.getByText(`${PUBLIC_DATA_SOURCES_TILE_TITLE}`)).toBeVisible();
      expect(screen.getByText(`${PUBLIC_DATA_SOURCES_TILE_BASE_MSG} disabled`)).toBeVisible();
    });

    it('renders disabled content because selectOwnerPublicDataSourcesInfo selector was undefined', () => {
      mockSelectorsForRenderingTileContent(undefined);
      renderComponent();

      expect(screen.getByText(`${PUBLIC_DATA_SOURCES_TILE_TITLE}`)).toBeVisible();
      expect(screen.getByText(`${PUBLIC_DATA_SOURCES_TILE_BASE_MSG} disabled`)).toBeVisible();
    });
  });
});
