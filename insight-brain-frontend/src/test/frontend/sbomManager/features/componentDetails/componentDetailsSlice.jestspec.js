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
  actions,
} from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSlice';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import {
  getLicensesWithSyntheticFilterUrl,
  getComponentMultiLicensesUrl,
  getLicenseOverrideUrl,
  getSbomMetadataUrl,
} from 'MainRoot/util/CLMLocation';
import { normalizeComponentIdentifier } from 'MainRoot/sbomManager/features/componentDetails/sbomLicenseUtils';

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

  describe('sbomComponentDetailsPage/loadComponents', function () {
    it('sets loadingComponents to true when loadComponents is pending', () => {
      const state = {
        loadingComponents: false,
        errorLoadingComponents: null,
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/loadComponents/pending',
      });

      expect(newState.loadingComponents).toBe(true);
      expect(newState.errorLoadingComponents).toBe(null);
    });

    it('updates the value for errorLoadingComponents when loadComponents fails', () => {
      const state = {
        loadingComponents: true,
        errorLoadingComponents: null,
      };

      const payload = {
        response: {
          data: 'payload-error',
        },
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/loadComponents/rejected',
        payload: payload,
      });

      expect(newState.loadingComponents).toBe(false);
      expect(newState.errorLoadingComponents).toBe('payload-error');
    });

    it('updates the pagesData when the components load successfully', () => {
      const state = {
        loadingComponents: true,
        errorLoadingComponents: null,
        componentDetailsPaginationData: {
          pagesData: {},
          pagination: {
            nextPage: 1,
          },
        },
      };

      const payload = {
        results: [{ hash: 'abc', name: 'Component 1' }],
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/loadComponents/fulfilled',
        payload: payload,
      });

      expect(newState.loadingComponents).toBe(false);
      expect(newState.errorLoadingComponents).toBe(null);
      expect(newState.componentDetailsPaginationData.pagesData[1]).toEqual(payload.results);
    });
  });

  describe('sbomComponentDetailsPage/loadInternalAppId', function () {
    it('sets the loadingInternalAppId when the load is pending', () => {
      const state = {
        loadingInternalAppId: false,
        errorInternalAppId: null,
        applicationName: null,
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/loadInternalAppId/pending',
      });

      expect(newState.loadingInternalAppId).toBe(true);
      expect(newState.errorInternalAppId).toBe(null);
      expect(newState.applicationName).toBe(null);
    });

    it('updates the errorInternalAppId when the load fails', () => {
      const state = {
        loadingInternalAppId: true,
        errorInternalAppId: null,
        internalAppId: null,
        publicApplicationId: null,
      };

      const payload = {
        response: {
          data: 'payload-error',
        },
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/loadInternalAppId/rejected',
        payload: payload,
      });

      expect(newState.loadingInternalAppId).toBe(false);
      expect(newState.errorInternalAppId).toBe('payload-error');
      expect(newState.internalAppId).toBe(null);
      expect(newState.publicApplicationId).toBe(null);
    });

    it('updates the internal id when it loads', () => {
      const state = {
        loadingInternalAppId: true,
        errorInternalAppId: null,
        internalAppId: null,
        applicationName: null,
      };

      const payload = {
        id: 'internalAppId123',
        name: 'Application Name',
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/loadInternalAppId/fulfilled',
        payload: payload,
      });

      expect(newState.loadingInternalAppId).toBe(false);
      expect(newState.errorInternalAppId).toBe(null);
      expect(newState.internalAppId).toBe('internalAppId123');
      expect(newState.applicationName).toBe('Application Name');
    });
  });

  describe('sbomComponentDetailsPage/loadComponentLicenses', function () {
    let mock;
    const applicationPublicId = 'test-app-id';
    const internalAppId = 'internal-app-id';
    const sbomVersion = '1.0.0';
    const sbomScanId = 'scan-123';
    const componentIdentifier = {
      format: 'maven',
      coordinates: { groupId: 'com.example', artifactId: 'lib', version: '1.0' },
    };
    const normalizedCI = normalizeComponentIdentifier(componentIdentifier);
    const componentIdentifierStr = JSON.stringify(normalizedCI);
    const multiLicensesUrl = getComponentMultiLicensesUrl({
      clientType: 'ci',
      ownerType: 'application',
      ownerId: applicationPublicId,
      componentIdentifier: componentIdentifierStr,
      identificationSource: 'SBOM',
      scanId: sbomScanId,
    });
    const licenseOverrideUrl = getLicenseOverrideUrl('application', applicationPublicId, componentIdentifierStr);

    beforeAll(() => {
      mock = axiosMockAdapter();
    });

    it('testLoadComponentLicenses_ReturnsNullDeclaredLicensesWhenNoSbomLicenses', (done) => {
      mock.onGet(getSbomMetadataUrl(internalAppId, sbomVersion)).reply(200, { scanId: sbomScanId });
      mock.onGet(getLicensesWithSyntheticFilterUrl()).reply(200, []);
      mock.onGet(multiLicensesUrl).reply(200, {
        declaredLicenses: null,
        observedLicenses: [],
        effectiveLicenses: [],
        selectableLicenses: [],
        hiddenObservedLicenses: false,
        supportAlpObservedLicenses: false,
      });
      mock.onGet(licenseOverrideUrl).reply(200, { licenseOverridesByOwner: [] });

      const storeState = {
        sbomComponentDetailsPage: {
          loading: false,
          loadError: null,
          componentDetails: {},
          componentDetailsPaginationData: null,
        },
        router: { currentParams: { componentHash: 'abc123' } },
      };
      const store = SpecUtil.mockReduxStore(storeState);

      store.dispatch(actions.loadComponentLicenses({ applicationPublicId, componentIdentifier, internalAppId, sbomVersion })).then((result) => {
        expect(result.payload.declaredLicenses).toBeNull();
        done();
      });
    });
  });

  describe('sbomComponentDetailsPage/updateCurrentPage', function () {
    it('updates currentPage when hash is found', () => {
      const state = {
        componentDetailsPaginationData: {
          pagesData: {
            1: [{ hash: 'abc', name: 'Component 1' }],
            2: [{ hash: 'def', name: 'Component 2' }],
          },
          pagination: {
            currentPage: 1,
          },
        },
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/updateCurrentPage',
        payload: 'def',
      });

      expect(newState.componentDetailsPaginationData.pagination.currentPage).toBe(2);
    });

    it('does not update currentPage when hash is not found', () => {
      const state = {
        componentDetailsPaginationData: {
          pagesData: {
            1: [{ hash: 'abc', name: 'Component 1' }],
            2: [{ hash: 'def', name: 'Component 2' }],
          },
          pagination: {
            currentPage: 1,
          },
        },
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/updateCurrentPage',
        payload: 'ghi',
      });

      expect(newState.componentDetailsPaginationData.pagination.currentPage).toBe(1);
    });

    it('does not update currentPage when componentDetailsPaginationData is null', () => {
      const state = {
        componentDetailsPaginationData: null,
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/updateCurrentPage',
        payload: 'abc',
      });

      expect(newState.componentDetailsPaginationData).toBe(null);
    });
  });
});
