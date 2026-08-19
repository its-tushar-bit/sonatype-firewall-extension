/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectSelectedComponent } from 'MainRoot/applicationReport/applicationReportSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectSelectedVulnerability,
  selectVulnerabilitiesSlice,
  selectVulnerabilitiesSortedSlice,
  selectVulnerabilityDetailsSlice,
  selectVulnerabilitiesRequestData,
  selectVulnerabityRefId,
} from 'MainRoot/componentDetails/VulnerabilitiesTableTile/vulnerabilitiesSelectors';

describe('vulnerabilitiesSelectors', () => {
  const mockState = {
    router: {
      currentParams: {
        hash: 'some-component-hash',
        scanId: 'scanId',
        publicId: 'publicId',
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

  const applicationReport = {
    selectedReport: {
      allEntries: [
        {
          hash: 'some-component-hash',
          componentIdentifier: {
            format: 'format',
            coordinates: {
              version: '2.4.9',
            },
          },
          proprietary: false,
          matchState: 'exact',
          identificationSource: 'is',
        },
        {
          hash: 'some-component-hash-2',
          componentIdentifier: {
            format: 'format',
            coordinates: {
              version: '2.5.9',
            },
          },
          proprietary: false,
          matchState: 'exact',
          identificationSource: 'is',
        },
      ],
      displayedEntries: [
        {
          hash: 'some-component-hash',
          componentIdentifier: {
            format: 'format',
            coordinates: {
              version: '2.4.9',
            },
          },
          proprietary: false,
          matchState: 'exact',
          identificationSource: 'is',
        },
      ],
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
    it('returns the vulnerabilities currently contained in the selectVulnerabilitiesSortedSlice sorted by severity', () => {
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

  describe('selectVulnerabilitiesRequestData', () => {
    it('calls `selectSelectedComponent` and `selectRouterCurrentParams`', () => {
      expect(selectVulnerabilitiesRequestData.dependencies).toEqual([
        selectSelectedComponent,
        selectRouterCurrentParams,
      ]);
    });
    it('returns an object with the request data based on the selected component and the current route', () => {
      const expectedData = {
        clientType: 'ci',
        ownerType: 'application',
        ownerId: 'publicId',
        componentIdentifier: '{"format":"format","coordinates":{"version":"2.4.9"}}',
        hash: 'some-component-hash',
        identificationSource: 'is',
        scanId: 'scanId',
      };
      const actual = selectVulnerabilitiesRequestData({
        ...mockState,
        applicationReport,
      });
      expect(actual).toEqual(expectedData);
    });
  });
  describe('selectSelectedVulnerability', () => {
    it('returns null if there is no selectedRefId', () => {
      const expectedSelection = null;
      const noSelectedRefState = {
        ...mockState,
        componentDetailsVulnerabilities: { ...mockState.componentDetailsVulnerabilities, selectedRefId: null },
      };

      const actualSelection = selectSelectedVulnerability(noSelectedRefState);
      expect(actualSelection).toEqual(expectedSelection);
    });

    it('returns the vulnerability information that matches the selectedRefId', () => {
      const expectedSelection = {
        refId: '2',
        severity: 9.2,
        status: 'status 2',
      };

      const actualSelection = selectSelectedVulnerability(mockState);
      expect(actualSelection).toEqual(expectedSelection);
    });
  });
});
