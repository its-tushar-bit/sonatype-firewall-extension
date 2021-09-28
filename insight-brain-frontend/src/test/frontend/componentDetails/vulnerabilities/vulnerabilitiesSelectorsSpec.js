/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectVulnerabilitiesSlice,
  selectVulnerabilitiesSortedSlice,
  selectVulnerabityRefId,
  selectVulnerabilityDetailsSlice,
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
      selectedRefId: '2',
      vulnerabilityDetails: {
        loading: false,
        error: null,
        details: {
          identifier: 'CVE-2014-3625',
          description: 'Directory traversal vulnerability',
          categories: ['data', 'operational'],
        },
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
        selectedRefId: '2',
        vulnerabilityDetails: {
          loading: false,
          error: null,
          details: {
            identifier: 'CVE-2014-3625',
            description: 'Directory traversal vulnerability',
            categories: ['data', 'operational'],
          },
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

  describe('selectVulnerabityRefId', () => {
    it('returns proper refId', () => {
      const actualSelection = selectVulnerabityRefId(mockState);
      expect(actualSelection).toBe('2');
    });
  });

  describe('selectVulnerabilityDetailsSlice', () => {
    it('returns vulnerability details contained in the vulnerabilitiesDataSlice', () => {
      const expectedSelection = {
        loading: false,
        error: null,
        details: {
          identifier: 'CVE-2014-3625',
          description: 'Directory traversal vulnerability',
          categories: ['data', 'operational'],
        },
      };

      const actualSelection = selectVulnerabilityDetailsSlice(mockState);
      expect(actualSelection).toEqual(expectedSelection);
    });
  });
});
