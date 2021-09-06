/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectComponentDetailsOverviewVersionExplorerSlice,
  selectVersionExplorerRequestData,
} from '../../../../main/frontend/componentDetails/overview/overviewSelectors';

describe('overviewSelectors', () => {
  const graphExplorerData = {
    loading: false,
    loadError: null,
    data: {
      someData: 'data',
    },
  };
  const mockState = {
    router: {
      currentParams: {
        hash: 'some-component-hash',
        publicId: 'publicId',
        scanId: 'scanId',
      },
    },
    applicationReport: {
      selectedReport: {
        displayedEntries: [
          {
            hash: 'some-component-hash',
            matchState: 'exact',
            proprietary: false,
            identificationSource: 'is',
            componentIdentifier: {
              format: 'format',
              coordinates: {},
            },
          },
        ],
      },
    },
    componentDetailsOverview: {
      graphExplorerData,
    },
  };

  describe('selectComponentDetailsOverviewSlice', () => {
    it('selects the graphExplorerData slice of the state', () => {
      const expectedSelection = graphExplorerData;
      const actualSelection = selectComponentDetailsOverviewVersionExplorerSlice(mockState);
      expect(actualSelection).toEqual(expectedSelection);
    });

    it('selects the selectVersionExplorerRequestData slice of the state', () => {
      const expectedSelection = {
        clientType: 'ci',
        ownerType: 'application',
        ownerId: 'publicId',
        matchState: 'exact',
        proprietary: false,
        identificationSource: 'is',
        componentIdentifier: {
          componentType: 'format',
          coordinates: {},
        },
        hash: 'some-component-hash',
        scanId: 'scanId',
      };
      const actualSelection = selectVersionExplorerRequestData(mockState);
      expect(actualSelection).toEqual(expectedSelection);
    });
  });
});
