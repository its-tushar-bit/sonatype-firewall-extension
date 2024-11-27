/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  SORT_BY_FIELDS,
  SORT_DIRECTION,
  defaultSortConfiguration,
  policyViolationDetailsDrawerInitialState,
  sbomPolicyViolationsInitialState,
} from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSlice';

describe('SBOM Manager componentDetailsSlice', function () {
  describe('sbomComponentDetailsPage/setActiveTabIndex', function () {
    it('sets activeTabIndex', () => {
      const state = {
        activeTabIndex: 0,
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/setActiveTabIndex',
        payload: 1,
      });

      expect(newState.activeTabIndex).toBe(1);
    });
  });

  describe('sbomComponentDetailsPage/loadComponentDetails', function () {
    it('/pending', () => {
      const state = {
        publicAppId: null,
        componentDetails: null,
        loadError: null,
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/loadComponentDetails/pending',
      });

      expect(newState.publicAppId).toBe(null);
      expect(newState.componentDetails).toBe(null);
      expect(newState.loadError).toBe(null);
      expect(newState.loading).toBe(true);
    });

    it('/failed', () => {
      const state = {
        loading: false,
        loadError: null,
        publicAppId: null,
        componentDetails: null,
      };

      const payload = {
        response: {
          data: 'payload error',
        },
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/loadComponentDetails/rejected',
        payload: payload,
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe('payload error');
      expect(newState.publicAppId).toBe(null);
      expect(newState.componentDetails).toBe(null);
    });

    it('/fulfilled', () => {
      const state = {
        loading: false,
        loadError: null,
        componentDetails: null,
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/loadComponentDetails/fulfilled',
        payload: { name: 'abc123' },
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe(null);
      expect(newState.componentDetails.name).toBe('abc123');
    });
  });

  describe('sortConfiguration', () => {
    describe('cycleDisclosedVulnerabilitiesSortDirection', () => {
      it('cycles cvssScore properly', () => {
        const state0 = {
          disclosedVulnerabilitiesSortConfiguration: { ...defaultSortConfiguration },
        };

        expect(state0.disclosedVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state0.disclosedVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

        const state1 = reducer(state0, {
          type: 'sbomComponentDetailsPage/cycleDisclosedVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.cvssScore,
          },
        });

        expect(state1.disclosedVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state1.disclosedVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

        const state2 = reducer(state1, {
          type: 'sbomComponentDetailsPage/cycleDisclosedVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.cvssScore,
          },
        });

        expect(state2.disclosedVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state2.disclosedVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);
      });

      it('cycles analysisStatus properly', () => {
        const state0 = {
          disclosedVulnerabilitiesSortConfiguration: { ...defaultSortConfiguration },
        };

        expect(state0.disclosedVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state0.disclosedVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

        const state1 = reducer(state0, {
          type: 'sbomComponentDetailsPage/cycleDisclosedVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.analysisStatus,
          },
        });

        expect(state1.disclosedVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.analysisStatus);
        expect(state1.disclosedVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

        const state2 = reducer(state1, {
          type: 'sbomComponentDetailsPage/cycleDisclosedVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.analysisStatus,
          },
        });

        expect(state2.disclosedVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.analysisStatus);
        expect(state2.disclosedVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

        const state3 = reducer(state2, {
          type: 'sbomComponentDetailsPage/cycleDisclosedVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.analysisStatus,
          },
        });

        expect(state3.disclosedVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state3.disclosedVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);
      });

      it('mixes cycles between cvssScore and analysisStatus properly', () => {
        const state0 = {
          disclosedVulnerabilitiesSortConfiguration: {
            sortBy: SORT_BY_FIELDS.cvssScore,
            sortDirection: SORT_DIRECTION.ASC,
          },
        };

        const state1 = reducer(state0, {
          type: 'sbomComponentDetailsPage/cycleDisclosedVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.analysisStatus,
          },
        });

        expect(state1.disclosedVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.analysisStatus);
        expect(state1.disclosedVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

        const state2 = reducer(state1, {
          type: 'sbomComponentDetailsPage/cycleDisclosedVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.cvssScore,
          },
        });

        expect(state2.disclosedVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state2.disclosedVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);
      });
    });

    describe('cycleAdditionalVulnerabilitiesSortDirection', () => {
      it('cycles cvssScore properly', () => {
        const state0 = {
          additionalVulnerabilitiesSortConfiguration: { ...defaultSortConfiguration },
        };

        expect(state0.additionalVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state0.additionalVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

        const state1 = reducer(state0, {
          type: 'sbomComponentDetailsPage/cycleAdditionalVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.cvssScore,
          },
        });

        expect(state1.additionalVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state1.additionalVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

        const state2 = reducer(state1, {
          type: 'sbomComponentDetailsPage/cycleAdditionalVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.cvssScore,
          },
        });

        expect(state2.additionalVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state2.additionalVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);
      });

      it('cycles analysisStatus properly', () => {
        const state0 = {
          additionalVulnerabilitiesSortConfiguration: { ...defaultSortConfiguration },
        };

        expect(state0.additionalVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state0.additionalVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

        const state1 = reducer(state0, {
          type: 'sbomComponentDetailsPage/cycleAdditionalVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.analysisStatus,
          },
        });

        expect(state1.additionalVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.analysisStatus);
        expect(state1.additionalVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

        const state2 = reducer(state1, {
          type: 'sbomComponentDetailsPage/cycleAdditionalVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.analysisStatus,
          },
        });

        expect(state2.additionalVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.analysisStatus);
        expect(state2.additionalVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);

        const state3 = reducer(state2, {
          type: 'sbomComponentDetailsPage/cycleAdditionalVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.analysisStatus,
          },
        });

        expect(state3.additionalVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state3.additionalVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.DESC);
      });

      it('mixes cycles between cvssScore and analysisStatus properly', () => {
        const state0 = {
          additionalVulnerabilitiesSortConfiguration: {
            sortBy: SORT_BY_FIELDS.cvssScore,
            sortDirection: SORT_DIRECTION.ASC,
          },
        };

        const state1 = reducer(state0, {
          type: 'sbomComponentDetailsPage/cycleAdditionalVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.analysisStatus,
          },
        });

        expect(state1.additionalVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.analysisStatus);
        expect(state1.additionalVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);

        const state2 = reducer(state1, {
          type: 'sbomComponentDetailsPage/cycleAdditionalVulnerabilitiesSortDirection',
          payload: {
            sortBy: SORT_BY_FIELDS.cvssScore,
          },
        });

        expect(state2.additionalVulnerabilitiesSortConfiguration.sortBy).toBe(SORT_BY_FIELDS.cvssScore);
        expect(state2.additionalVulnerabilitiesSortConfiguration.sortDirection).toBe(SORT_DIRECTION.ASC);
      });
    });
  });

  describe('policyViolationDetailsDrawer', () => {
    describe('showPolicyViolationDetailsDrawer', () => {
      it('sets the correct state values', () => {
        const violationDetails = {
          policyViolationId: 123,
          policyThreatCategory: 'SECURITY',
          policyThreatLevel: 10,
          constraints: [
            {
              constraintId: 12345,
              constraintName: 'asdf',
              conditions: [
                {
                  conditionReason: 'asdf',
                },
              ],
            },
          ],
        };
        const state0 = {
          policyViolationDetailsDrawer: { ...policyViolationDetailsDrawerInitialState },
          sbomPolicyViolations: {
            policy: {
              activeViolations: [violationDetails],
            },
          },
        };

        const state1 = reducer(state0, {
          type: 'sbomComponentDetailsPage/showPolicyViolationDetailsDrawer',
          payload: 123,
        });

        expect(state1.policyViolationDetailsDrawer.showDrawer).toBe(true);
        expect(state1.policyViolationDetailsDrawer.policyViolationId).toBe(123);
        expect(state1.policyViolationDetailsDrawer.violationDetails.policyThreatLevel).toBe(10);
        expect(state1.policyViolationDetailsDrawer.violationDetails.policyThreatCategory).toBe('SECURITY');
      });
    });

    describe('hidePolicyViolationDetailsDrawer', () => {
      it('sets the correct state values', () => {
        const state0 = {
          policyViolationDetailsDrawer: {
            showDrawer: true,
            policyViolationId: 123,
            violationDetails: {
              policyViolationId: 123,
              policyThreatLevel: 10,
              policyThreatCategory: 'SECURITY',
            },
          },
        };

        const state1 = reducer(state0, {
          type: 'sbomComponentDetailsPage/hidePolicyViolationDetailsDrawer',
          payload: 123,
        });

        expect(state1.policyViolationDetailsDrawer.showDrawer).toBe(false);
        expect(state1.policyViolationDetailsDrawer.policyViolationId).toBe(null);
        expect(state1.policyViolationDetailsDrawer.violationDetails).toBe(null);
      });
    });

    // describe('loadViolationDetails', () => {
    //   const pendingState = {
    //     loading: true,
    //     error: null,
    //     violationDetails: null,
    //   };

    //   it('/pending', () => {
    //     const state = {
    //       policyViolationDetailsDrawer: {
    //         ...policyViolationDetailsDrawerInitialState,
    //       },
    //     };

    //     const newState = reducer(state, {
    //       type: 'sbomComponentDetailsPage/loadViolationDetails/pending',
    //     });

    //     expect(newState.policyViolationDetailsDrawer.loading).toBe(true);
    //     expect(newState.policyViolationDetailsDrawer.error).toBe(null);
    //     expect(newState.policyViolationDetailsDrawer.violationDetails).toBe(null);
    //   });

    //   it('/fulfilled', () => {
    //     const state = {
    //       policyViolationDetailsDrawer: {
    //         ...pendingState,
    //       },
    //     };

    //     const payload = {
    //       policyThreatLevel: 10,
    //       policyThreatCategory: 'SECURITY',
    //       policyOwner: {
    //         id: 'abc123',
    //         publicId: 'abc123',
    //         name: 'test1',
    //         type: 'organization',
    //       },
    //     };

    //     const newState = reducer(state, {
    //       type: 'sbomComponentDetailsPage/loadViolationDetails/fulfilled',
    //       payload: payload,
    //     });

    //     const violationDetails = newState.policyViolationDetailsDrawer.violationDetails;

    //     expect(newState.policyViolationDetailsDrawer.loading).toBe(false);
    //     expect(newState.policyViolationDetailsDrawer.error).toBe(null);
    //     expect(violationDetails.policyThreatLevel).toBe(10);
    //     expect(violationDetails.policyThreatCategory).toBe('SECURITY');
    //     expect(violationDetails.policyOwner.id).toBe('abc123');
    //     expect(violationDetails.policyOwner.publicId).toBe('abc123');
    //     expect(violationDetails.policyOwner.name).toBe('test1');
    //     expect(violationDetails.policyOwner.type).toBe('organization');
    //   });

    //   it('/rejected', () => {
    //     const state = {
    //       policyViolationDetailsDrawer: {
    //         ...pendingState,
    //       },
    //     };

    //     const newState = reducer(state, {
    //       type: 'sbomComponentDetailsPage/loadViolationDetails/rejected',
    //       payload: { message: 'error' },
    //     });

    //     expect(newState.policyViolationDetailsDrawer.loading).toBe(false);
    //     expect(newState.policyViolationDetailsDrawer.error).toBe('error');
    //     expect(newState.policyViolationDetailsDrawer.violationDetails).toBe(null);
    //   });
    // });
  });

  describe('sbomComponentDetailsPage/loadSbomPolicyViolationReport', function () {
    it('/pending', () => {
      const state = {
        sbomPolicyViolations: {
          ...sbomPolicyViolationsInitialState,
        },
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/loadSbomPolicyViolationReport/pending',
      });

      expect(newState.sbomPolicyViolations.loading).toBe(true);
      expect(newState.sbomPolicyViolations.error).toBe(null);
      expect(newState.sbomPolicyViolations.policy).toBe(null);
    });

    it('/failed', () => {
      const state = {
        sbomPolicyViolations: {
          ...sbomPolicyViolationsInitialState,
        },
      };

      const payload = {
        response: {
          data: 'payload-error',
        },
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/loadSbomPolicyViolationReport/rejected',
        payload: payload,
      });

      expect(newState.sbomPolicyViolations.loading).toBe(false);
      expect(newState.sbomPolicyViolations.error).toBe('payload-error');
      expect(newState.sbomPolicyViolations.policy).toBe(null);
    });

    it('/fulfilled', () => {
      const state = {
        sbomPolicyViolations: {
          ...sbomPolicyViolationsInitialState,
        },
      };

      const fakePartialReportData = {
        policyId: '123',
        policyName: 'Security-High',
        policyThreatLevel: 9,
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/loadSbomPolicyViolationReport/fulfilled',
        payload: fakePartialReportData,
      });

      expect(newState.sbomPolicyViolations.loading).toBe(false);
      expect(newState.sbomPolicyViolations.error).toBe(null);
      expect(newState.sbomPolicyViolations.policy).toEqual(fakePartialReportData);
    });
  });

  describe('sbomComponentDetailsPage/loadVulnerabilityDetails', () => {
    it('/pending', () => {
      const state = {
        loadingVulnerabilityDetail: false,
        loadVulnerabilityDetailError: null,
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/loadVulnerabilityDetails/pending',
      });

      expect(newState.loadingVulnerabilityDetail).toBe(true);
      expect(newState.loadVulnerabilityDetailError).toBe(null);
    });

    it('/failed', () => {
      const state = {
        loadingVulnerabilityDetail: true,
        loadVulnerabilityDetailError: null,
      };

      const payload = {
        response: {
          data: 'payload-error',
        },
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/loadVulnerabilityDetails/rejected',
        payload,
      });

      expect(newState.loadingVulnerabilityDetail).toBe(false);
      expect(newState.loadVulnerabilityDetailError).toBe('payload-error');
      expect(newState.vulnerabilityDetails).toBe(null);
    });

    it('/fulfilled', () => {
      const state = {
        loadingVulnerabilityDetail: true,
        loadVulnerabilityDetailError: null,
      };

      const fakeVulnerabilityDetails = {
        identifier: 'CVE-2024-2398',
        vulnerabilityLink: 'https://igel.com/',
        source: {
          shortName: 'INTERNAL',
          longName: 'INTERNAL',
        },
        mainSeverity: {
          source: 'cve_cvss_3',
          sourceLabel: 'CVE CVSS 3',
          score: 7.5,
          vector: 'AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N',
        },
        severityScores: null,
        weakness: {
          cweSource: '',
          cweIds: [],
        },
        categories: null,
        description: null,
        explanationMarkdown: '',
        detectionMarkdown: null,
        recommendationMarkdown: '',
        advisories: [],
        researchType: null,
        isAdvancedVulnerabilityDetection: false,
        detectionType: null,
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/loadVulnerabilityDetails/fulfilled',
        payload: fakeVulnerabilityDetails,
      });

      expect(newState.loadingVulnerabilityDetail).toBe(false);
      expect(newState.loadVulnerabilityDetailError).toBe(null);
      expect(newState.vulnerabilityDetails).toEqual(fakeVulnerabilityDetails);
    });
  });
});
