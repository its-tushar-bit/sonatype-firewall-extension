/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectVulnerabilitiesSlice,
  selectVulnerabilitiesSortedSlice,
} from '../../../../main/frontend/componentDetails/VulnerabilitiesTableTile/vulnerabilitiesSelectors';

describe('vulnerabilitiesSelectors', () => {
  const mockState = {
    router: {
      currentParams: {
        hash: 'some-component-hash',
      },
    },
    componentDetailsVulnerabilities: {
      vulnerabilities: {
        data: [
          {
            refId: '1',
            severity: 8,
            status: 'status 1',
          },
          {
            refId: '2',
            severity: 9.2,
            status: 'status 2',
          },
        ],
        loading: false,
        error: null,
      },
    },
  };

  describe('selectVulnerabilitiesSlice', () => {
    it('selects the componentDetailsVulnerabilities slice of the state', () => {
      const expectedSelection = {
        vulnerabilities: {
          data: [
            {
              refId: '1',
              severity: 8,
              status: 'status 1',
            },
            {
              refId: '2',
              severity: 9.2,
              status: 'status 2',
            },
          ],
          loading: false,
          error: null,
        },
      };

      const actualSelection = selectVulnerabilitiesSlice(mockState);
      expect(actualSelection).toEqual(expectedSelection);
    });
  });

  describe('selectVulnerabilitiesSortedSlice', () => {
    it('returns the vulnerabilities currently contained in the selectVulnerabilitiesSortedSlice sorted by sevrity', () => {
      const expectedSelection = {
        data: [
          {
            refId: '2',
            severity: 9.2,
            status: 'status 2',
          },
          {
            refId: '1',
            severity: 8,
            status: 'status 1',
          },
        ],
        loading: false,
        error: null,
      };

      const actualSelection = selectVulnerabilitiesSortedSlice(mockState);
      expect(actualSelection).toEqual(expectedSelection);
    });
  });
});
