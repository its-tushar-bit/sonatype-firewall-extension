/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import {
  closeConfigurationModal,
  FIREWALL_AUTO_UNQUARANTINE_DATA_REQUESTED,
  FIREWALL_AUTO_UNQUARANTINE_GRID_SET_PAGE,
  FIREWALL_AUTO_UNQUARANTINE_GRID_SET_SORTING,
  FIREWALL_CIP_MODAL_SHOW,
  FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE,
  FIREWALL_LOAD_CONFIGURATION_FAILED,
  FIREWALL_LOAD_CONFIGURATION_FULFILLED,
  FIREWALL_LOAD_CONFIGURATION_REQUESTED,
  FIREWALL_LOAD_DATA_REQUESTED,
  FIREWALL_POLICIES_FAILED,
  FIREWALL_POLICIES_FULFILLED,
  FIREWALL_POLICIES_REQUESTED,
  FIREWALL_QUARANTINE_GRID_SET_FILTER,
  FIREWALL_QUARANTINE_GRID_SET_LAST_UPDATED,
  FIREWALL_QUARANTINE_GRID_SET_PAGE,
  FIREWALL_QUARANTINE_GRID_SET_SORTING,
  FIREWALL_QUARANTINE_LIST_FAILED,
  FIREWALL_QUARANTINE_LIST_FULFILLED,
  FIREWALL_QUARANTINE_LIST_REQUESTED,
  FIREWALL_QUARANTINE_SUMMARY_FAILED,
  FIREWALL_QUARANTINE_SUMMARY_FULFILLED,
  FIREWALL_QUARANTINE_SUMMARY_REQUESTED,
  FIREWALL_RELEASE_QUARANTINE_LIST_FAILED,
  FIREWALL_RELEASE_QUARANTINE_LIST_FULFILLED,
  FIREWALL_RELEASE_QUARANTINE_LIST_REQUESTED,
  FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED,
  FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED,
  FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED,
  FIREWALL_SAVE_CONFIGURATION_FAILED,
  FIREWALL_SAVE_CONFIGURATION_FULFILLED,
  FIREWALL_SAVE_CONFIGURATION_REQUESTED,
  FIREWALL_SELECT_COMPONENT,
  FIREWALL_SET_SHOW_CONFIGURATION_MODAL,
  FIREWALL_COMPONENT_DETAILS_REQUESTED,
  FIREWALL_COMPONENT_DETAILS_FULFILLED,
  FIREWALL_COMPONENT_DETAILS_FAILED,
  FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_REQUESTED,
  FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_FULFILLED,
  FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_FAILED,
  FIREWALL_LOAD_COMPONENT_LICENSES_REQUESTED,
  FIREWALL_LOAD_COMPONENT_LICENSES_FULFILLED,
  FIREWALL_LOAD_COMPONENT_LICENSES_FAILED,
  FIREWALL_LOAD_EXISTING_WAIVERS_DATA_REQUESTED,
  FIREWALL_LOAD_EXISTING_WAIVERS_DATA_FULFILLED,
  FIREWALL_LOAD_EXISTING_WAIVERS_DATA_FAILED,
  FIREWALL_LOAD_VIOLATION_DETAIL_REQUESTED,
  FIREWALL_LOAD_VIOLATION_DETAIL_FULFILLED,
  FIREWALL_LOAD_VIOLATION_DETAIL_FAILED,
  loadAutoUnquarantineData,
  loadConfiguration,
  loadFirewallData,
  loadPolicies,
  loadQuarantineList,
  loadQuarantineSummary,
  loadReleaseQuarantineList,
  loadReleaseQuarantineSummary,
  openConfigurationModal,
  saveConfiguration,
  selectComponent,
  selectQuarantineComponent,
  selectReleaseQuarantineComponent,
  setAutoUnquarantineGridPage,
  setAutoUnquarantineGridSorting,
  setQuarantineGridLastUpdated,
  setQuarantineGridPage,
  setQuarantineGridPolicyFilter,
  setQuarantineGridSorting,
  loadComponentDetails,
  loadComponentDetailsRequested,
  loadComponentDetailsFulfilled,
  loadComponentDetailsFailed,
  onComponentDetailsPageTabChange,
  loadComponentPolicyViolationsRequested,
  loadComponentPolicyViolationsFulfilled,
  loadComponentPolicyViolationsFailed,
  loadComponentLicenses,
  loadExistingWaiversDataRequested,
  loadExistingWaiversDataFulfilled,
  loadExistingWaiversDataFailed,
  onGoToFirewallWaiversPage,
  loadFirewallViolationDetails,
} from '../../../main/frontend/firewall/firewallActions';
import {
  getFirewallConfigurationUrl,
  getFirewallQuarantineListUrl,
  getFirewallQuarantineSummaryUrl,
  getFirewallReleaseQuarantineListUrl,
  getFirewallReleaseQuarantineSummaryUrl,
  getPoliciesUrl,
  getComponentDetailsUrl,
  getRepositoryPolicyViolationUrl,
  getLicensesWithSyntheticFilterUrl,
  getComponentMultiLicensesUrl,
  getLicenseOverrideUrl,
  getComponentLabels,
} from '../../../main/frontend/util/CLMLocation';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { INTEGRITY_RATING_POLICY_TYPE_ID } from '../../../main/frontend/firewall/config/firewallConfigurationModalReducer';

describe('firewallActions', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios),
    firewallConfigUrl = getFirewallConfigurationUrl(),
    firewallReleaseQuarantineSummaryUrl = getFirewallReleaseQuarantineSummaryUrl(),
    firewallReleaseQuarantineListUrl = getFirewallReleaseQuarantineListUrl(),
    firewallQuarantineSummaryUrl = getFirewallQuarantineSummaryUrl(),
    firewallQuarantineListUrl = getFirewallQuarantineListUrl(),
    policiesUrl = getPoliciesUrl(),
    requestRepositoryPolicyViolationUrl = getRepositoryPolicyViolationUrl('repositoryId', 'RepolicyViolationId'),
    repositoryPolicyViolationUrl = getRepositoryPolicyViolationUrl('repositoryId', 'policyViolationId'),
    errorRepositoryPolicyViolationUrl = getRepositoryPolicyViolationUrl('repositoryId', 'ErrorpolicyViolationId'),
    allLicensesUrl = getLicensesWithSyntheticFilterUrl(),
    componentMultiLicensesUrl = getComponentMultiLicensesUrl({
      clientType: 'ci',
      ownerType: 'repository',
      ownerId: 'repositoryId',
      componentIdentifier: 'componentIdentifier',
    }),
    licensesOverrideUrl = getLicenseOverrideUrl('repository', 'repositoryId', 'componentIdentifier'),
    erroneusLicensesOverrideUrl = getLicenseOverrideUrl('repository', 'repositoryId', 'erroneusComponentIdentifier');

  let store, state;

  beforeEach(function () {
    state = {
      firewall: Object.freeze({
        cip: Object.freeze({
          showCipModal: false,
          selectedComponent: null,
          selectedComponentIndex: null,
          displayedEntries: [],
        }),
        viewState: Object.freeze({
          loadStatusError: null,
          isShowConfigurationModal: false,
        }),
        autoUnquarantineState: Object.freeze({
          autoUnquarantineGridState: Object.freeze({
            loadedReleaseQuarantineList: false,
            releaseQuarantineList: [],
            releaseQuarantinePageCount: 0,
            pageSize: 12,
            currentPage: null,
            sortDir: null,
            sortField: null,
          }),
        }),
        quarantineGridState: Object.freeze({
          loadQuarantineGridError: null,
          loadedQuarantineList: false,
          quarantineList: [],
          quarantinePageCount: 0,
          pageSize: 12,
          currentPage: null,
          sortDir: null,
          sortField: null,
          filterPolicyId: '',
          lastUpdated: null,
        }),
        policiesState: Object.freeze({
          loadedPolicies: false,
          policies: [],
        }),
      }),
      firewallConfigurationModal: Object.freeze({
        viewState: Object.freeze({
          submitMaskSuccessState: false,
          saveConfigurationError: null,
          loadedConfiguration: false,
          loadConfigurationError: null,
          enabledPolicyConditionTypesCount: 0,
          totalPolicyConditionTypesCount: 1,
        }),
        serverState: Object.freeze({
          conditionTypes: [
            {
              id: INTEGRITY_RATING_POLICY_TYPE_ID,
              name: 'Integrity Rating',
              autoReleaseQuarantineEnabled: false,
            },
          ],
        }),
        formState: Object.freeze({
          conditionTypes: [
            {
              id: INTEGRITY_RATING_POLICY_TYPE_ID,
              name: 'Integrity Rating',
              autoReleaseQuarantineEnabled: false,
            },
          ],
        }),
      }),
      router: Object.freeze({
        currentParams: Object.freeze({
          componentHash: 'componentHash',
          componentIdentifier: {
            format: 'maven',
            coordinates: {
              artifactId: 'ant',
              classifier: '',
              extension: 'jar',
              groupId: 'ant',
              version: '1.6',
            },
          },
          repositoryId: 'repositoryId',
          matchState: 'matchState',
          proprietary: 'proprietary',
          identificationSource: 'identificationSource',
          pathname: 'pathname',
        }),
      }),
      componentDetails: {
        pendingLoads: new Set(),
      },
    };

    store = SpecUtil.mockReduxStore(state);
  });

  describe('loadComponentLicenses', function () {
    beforeEach(function () {
      mockAxiosCalls({
        get: {
          [allLicensesUrl]: Promise.resolve({
            data: [],
          }),
          [componentMultiLicensesUrl]: Promise.resolve({
            data: { multiLicensesData: [] },
          }),
          [licensesOverrideUrl]: Promise.resolve({
            data: { licenseOverridesByOwner: [] },
          }),
          [erroneusLicensesOverrideUrl]: Promise.reject('error'),
        },
      });
    });

    it('immediately dispatches a FIREWALL_LOAD_COMPONENT_LICENSES_REQUESTED action', function () {
      store.dispatch(loadComponentLicenses('repositoryId', 'componentIdentifier'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_LOAD_COMPONENT_LICENSES_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    it('dispatches FIREWALL_LOAD_COMPONENT_LICENSES_FULFILLED action after succesfull requests', function (done) {
      store.dispatch(loadComponentLicenses('repositoryId', 'componentIdentifier')).then(() => {
        actions = store.getActions();
        expect(actions.length).toBe(3);
        expect(actions[0].type).toBe(FIREWALL_LOAD_COMPONENT_LICENSES_REQUESTED);
        expect(actions[0].payload).toBeUndefined();
        expect(actions[1].type).toBe(FIREWALL_LOAD_COMPONENT_LICENSES_FULFILLED);
        expect(actions[1].payload).toEqual({
          multiLicensesData: [],
          licenseOverride: [],
          allLicenses: [],
        });
        expect(actions[2].type).toBe('componentDetailsLicenseDetectionsTile/load/fulfilled');
        expect(actions[2].payload).toEqual({
          multiLicensesData: [],
          licenseOverride: [],
          allLicenses: [],
        });
        done();
      });

      let actions = store.getActions();
      expect(actions.length).toBe(1);
    });

    it('dispatches a FIREWALL_LOAD_COMPONENT_LICENSES_FAILED action after one of all of the requests failed', function (done) {
      store.dispatch(loadComponentLicenses('repositoryId', 'erroneusComponentIdentifier')).then(() => {
        actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe(FIREWALL_LOAD_COMPONENT_LICENSES_REQUESTED);
        expect(actions[0].payload).toBeUndefined();
        expect(actions[1].type).toBe(FIREWALL_LOAD_COMPONENT_LICENSES_FAILED);
        expect(actions[1].payload).toBe('error');
        done();
      });
      let actions = store.getActions();
      expect(actions.length).toBe(1);
    });
  });

  describe('loadConfiguration', function () {
    afterEach(function () {
      expect(axios.get).toHaveBeenCalledWith(firewallConfigUrl);
    });

    it('immediately dispatches a FIREWALL_LOAD_CONFIGURATION_REQUESTED action', function () {
      mockAxiosCalls({
        get: {
          [firewallConfigUrl]: Promise.resolve({
            data: { autoUnquarantineEnabled: true },
          }),
        },
      });

      store.dispatch(loadConfiguration());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_LOAD_CONFIGURATION_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful GET call', function () {
      it('dispatches FIREWALL_LOAD_CONFIGURATION_FULFILLED action', function (done) {
        mockAxiosCalls({
          get: {
            [firewallConfigUrl]: Promise.resolve({
              data: { autoUnquarantineEnabled: true },
            }),
          },
        });

        store.dispatch(loadConfiguration()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe(FIREWALL_LOAD_CONFIGURATION_REQUESTED);
          expect(actions[0].payload).toBeUndefined();
          expect(actions[1].type).toBe(FIREWALL_LOAD_CONFIGURATION_FULFILLED);
          expect(actions[1].payload).toEqual({ autoUnquarantineEnabled: true });
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after a failed GET call', function () {
      it('dispatches an FIREWALL_LOAD_CONFIGURATION_FAILED action', function (done) {
        mockAxiosCalls({
          get: {
            [firewallConfigUrl]: () => Promise.reject('error!'),
          },
        });

        store.dispatch(loadConfiguration()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe(FIREWALL_LOAD_CONFIGURATION_FAILED);
          expect(actions[1].payload).toBe('error!');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });

  describe('saveConfiguration', function () {
    afterEach(function () {
      expect(axios.put).toHaveBeenCalledWith(
        firewallConfigUrl,
        state.firewallConfigurationModal.formState.conditionTypes
      );
    });

    it('immediately dispatches a FIREWALL_SAVE_CONFIGURATION_REQUESTED action', function () {
      mockAxiosCalls({
        put: {
          [firewallConfigUrl]: Promise.resolve({}),
        },
      });

      store.dispatch(saveConfiguration());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_SAVE_CONFIGURATION_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful PUT call', function () {
      it('dispatches FIREWALL_SAVE_CONFIGURATION_FULFILLED action', function (done) {
        mockAxiosCalls({
          put: {
            [firewallConfigUrl]: Promise.resolve({}),
          },
        });

        store.dispatch(saveConfiguration()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe(FIREWALL_SAVE_CONFIGURATION_REQUESTED);
          expect(actions[0].payload).toBeUndefined();
          expect(actions[1].type).toBe(FIREWALL_SAVE_CONFIGURATION_FULFILLED);
          expect(actions[1].payload).toEqual([
            {
              id: INTEGRITY_RATING_POLICY_TYPE_ID,
              name: 'Integrity Rating',
              autoReleaseQuarantineEnabled: false,
            },
          ]);
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });

      it('dispatches FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE after timeout', function (done) {
        mockAxiosCalls({
          put: {
            [firewallConfigUrl]: Promise.resolve({}),
          },
        });
        jasmine.clock().install();

        store.dispatch(saveConfiguration()).then(() => {
          jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
          jasmine.clock().uninstall();

          actions = store.getActions();
          expect(actions.length).toBe(4);
          expect(actions[2].type).toBe(FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE);
          expect(actions[3].type).toBe(FIREWALL_SET_SHOW_CONFIGURATION_MODAL);
          expect(actions[3].payload).toBe(false);
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after a failed PUT call', function () {
      it('dispatches an FIREWALL_SAVE_CONFIGURATION_FAILED action', function (done) {
        mockAxiosCalls({
          put: {
            [firewallConfigUrl]: () => Promise.reject('error!'),
          },
        });

        store.dispatch(saveConfiguration()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe(FIREWALL_SAVE_CONFIGURATION_FAILED);
          expect(actions[1].payload).toBe('error!');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });

  describe('openConfigurationModal', function () {
    it('immediately dispatches loadConfiguration and setShowConfigurationModal actions', function () {
      store.dispatch(openConfigurationModal());

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe(FIREWALL_LOAD_CONFIGURATION_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
      expect(actions[1].type).toBe(FIREWALL_SET_SHOW_CONFIGURATION_MODAL);
      expect(actions[1].payload).toEqual(true);
    });
  });

  describe('closeConfigurationModal', function () {
    it('dispatches an setShowConfigurationModal action if serverState and formState is the same', function (done) {
      state = {
        firewallConfigurationModal: Object.freeze({
          serverState: Object.freeze({
            autoUnquarantineEnabled: false,
          }),
          formState: Object.freeze({
            autoUnquarantineEnabled: false,
          }),
        }),
      };

      store = SpecUtil.mockReduxStore(state);
      store.dispatch(closeConfigurationModal());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_SET_SHOW_CONFIGURATION_MODAL);
      expect(actions[0].payload).toBe(false);
      done();
    });
  });

  describe('loadReleaseQuarantineSummary', function () {
    afterEach(function () {
      expect(axios.get).toHaveBeenCalledWith(firewallReleaseQuarantineSummaryUrl);
    });

    it('immediately dispatches a FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED action', function () {
      mockAxiosCalls({
        get: {
          [firewallReleaseQuarantineSummaryUrl]: Promise.resolve({
            data: { autoReleaseQuarantineCountMTD: 3 },
          }),
        },
      });

      store.dispatch(loadReleaseQuarantineSummary());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful GET call', function () {
      it('dispatches FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED action', function (done) {
        mockAxiosCalls({
          get: {
            [firewallReleaseQuarantineSummaryUrl]: Promise.resolve({
              data: { autoReleaseQuarantineCountMTD: 3 },
            }),
          },
        });

        store.dispatch(loadReleaseQuarantineSummary()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe(FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED);
          expect(actions[0].payload).toBeUndefined();
          expect(actions[1].type).toBe(FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED);
          expect(actions[1].payload).toEqual({
            autoReleaseQuarantineCountMTD: 3,
          });
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after a failed GET call', function () {
      it('dispatches an FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED action', function (done) {
        mockAxiosCalls({
          get: {
            [firewallReleaseQuarantineSummaryUrl]: () => Promise.reject('error!'),
          },
        });

        store.dispatch(loadReleaseQuarantineSummary()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe(FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED);
          expect(actions[1].payload).toBe('error!');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });

  describe('loadReleaseQuarantineList', function () {
    let payload = {
        pageCount: 2,
        results: [{ test: 'testVal' }, { test: 'testVal' }],
      },
      defaultParams = '?page=1&pageSize=12';

    afterEach(function () {
      expect(axios.get).toHaveBeenCalledWith(firewallReleaseQuarantineListUrl + defaultParams);
    });

    it('immediately dispatches a FIREWALL_RELEASE_QUARANTINE_LIST_REQUESTED action', function () {
      mockAxiosCalls({
        get: {
          [firewallReleaseQuarantineListUrl + defaultParams]: Promise.resolve(payload),
        },
      });

      store.dispatch(loadReleaseQuarantineList());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_RELEASE_QUARANTINE_LIST_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful GET call', function () {
      let defaultParams = '?page=1&pageSize=12';

      it('dispatches FIREWALL_RELEASE_QUARANTINE_LIST_FULFILLED action', function (done) {
        mockAxiosCalls({
          get: {
            [firewallReleaseQuarantineListUrl + defaultParams]: Promise.resolve({ data: payload }),
          },
        });

        store.dispatch(loadReleaseQuarantineList()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe(FIREWALL_RELEASE_QUARANTINE_LIST_REQUESTED);
          expect(actions[0].payload).toBeUndefined();
          expect(actions[1].type).toBe(FIREWALL_RELEASE_QUARANTINE_LIST_FULFILLED);
          expect(actions[1].payload).toEqual(payload);
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after a failed GET call', function () {
      let defaultParams = '?page=1&pageSize=12';

      it('dispatches an FIREWALL_RELEASE_QUARANTINE_LIST_FAILED action', function (done) {
        mockAxiosCalls({
          get: {
            [firewallReleaseQuarantineListUrl + defaultParams]: () => Promise.reject('error!'),
          },
        });

        store.dispatch(loadReleaseQuarantineList()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe(FIREWALL_RELEASE_QUARANTINE_LIST_FAILED);
          expect(actions[1].payload).toBe('error!');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });

  describe('loadPolicies', function () {
    let payload = { policies: [{ test: 'testVal' }] };

    afterEach(function () {
      expect(axios.get).toHaveBeenCalledWith(policiesUrl);
    });

    it('immediately dispatches a FIREWALL_POLICIES_REQUESTED action', function () {
      mockAxiosCalls({
        get: {
          [policiesUrl]: Promise.resolve(payload),
        },
      });

      store.dispatch(loadPolicies());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_POLICIES_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful GET call', function () {
      it('dispatches FIREWALL_POLICIES_FULFILLED action', function (done) {
        mockAxiosCalls({
          get: {
            [policiesUrl]: Promise.resolve({ data: payload }),
          },
        });

        store.dispatch(loadPolicies()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe(FIREWALL_POLICIES_REQUESTED);
          expect(actions[0].payload).toBeUndefined();
          expect(actions[1].type).toBe(FIREWALL_POLICIES_FULFILLED);
          expect(actions[1].payload).toEqual(payload);
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after a failed GET call', function () {
      it('dispatches an FIREWALL_POLICIES_FAILED action', function (done) {
        mockAxiosCalls({
          get: {
            [policiesUrl]: () => Promise.reject('error!'),
          },
        });

        store.dispatch(loadPolicies()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe(FIREWALL_POLICIES_FAILED);
          expect(actions[1].payload).toBe('error!');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });

  describe('loadQuarantineSummary', function () {
    afterEach(function () {
      expect(axios.get).toHaveBeenCalledWith(firewallQuarantineSummaryUrl);
    });

    it('immediately dispatches a FIREWALL_QUARANTINE_SUMMARY_REQUESTED action', function () {
      mockAxiosCalls({
        get: {
          [firewallQuarantineSummaryUrl]: Promise.resolve({
            data: {},
          }),
        },
      });

      store.dispatch(loadQuarantineSummary());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_QUARANTINE_SUMMARY_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful GET call', function () {
      it('dispatches FIREWALL_QUARANTINE_SUMMARY_FULFILLED action', function (done) {
        mockAxiosCalls({
          get: {
            [firewallQuarantineSummaryUrl]: Promise.resolve({
              data: { test: 'test' },
            }),
          },
        });

        store.dispatch(loadQuarantineSummary()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe(FIREWALL_QUARANTINE_SUMMARY_REQUESTED);
          expect(actions[0].payload).toBeUndefined();
          expect(actions[1].type).toBe(FIREWALL_QUARANTINE_SUMMARY_FULFILLED);
          expect(actions[1].payload).toEqual({ test: 'test' });
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after a failed GET call', function () {
      it('dispatches an FIREWALL_QUARANTINE_SUMMARY_FAILED action', function (done) {
        mockAxiosCalls({
          get: {
            [firewallQuarantineSummaryUrl]: () => Promise.reject('error!'),
          },
        });

        store.dispatch(loadQuarantineSummary()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe(FIREWALL_QUARANTINE_SUMMARY_FAILED);
          expect(actions[1].payload).toBe('error!');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });

  describe('setAutoUnquarantineGridPage', function () {
    it('immediately dispatches actions to set the current page for the auto unquarantine grid', function () {
      let page = 123;

      store.dispatch(setAutoUnquarantineGridPage(page));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_AUTO_UNQUARANTINE_GRID_SET_PAGE);
      expect(actions[0].payload).toEqual({ currentPage: page });
    });
  });

  describe('setAutoUnquarantineGridSorting', function () {
    it('immediately dispatches actions to set the sorting for the auto unquarantine grid', function () {
      let sortField = 'testField',
        sortDir = 'asc';

      store.dispatch(setAutoUnquarantineGridSorting(sortDir, sortField));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_AUTO_UNQUARANTINE_GRID_SET_SORTING);
      expect(actions[0].payload).toEqual({
        sortDir: sortDir,
        sortField: sortField,
      });
    });
  });

  describe('loadQuarantineList', function () {
    let payload = { pageCount: 2, results: [{ test: 'testVal' }, { test: 'testVal' }] },
      defaultParams = '?page=1&pageSize=12';

    afterEach(function () {
      expect(axios.get).toHaveBeenCalledWith(firewallQuarantineListUrl + defaultParams);
    });

    it('immediately dispatches a FIREWALL_QUARANTINE_LIST_REQUESTED action', function () {
      mockAxiosCalls({
        get: {
          [firewallQuarantineListUrl + defaultParams]: Promise.resolve(payload),
        },
      });

      store.dispatch(loadQuarantineList());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_QUARANTINE_LIST_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful GET call', function () {
      let defaultParams = '?page=1&pageSize=12',
        lastUpdated = new Date();

      beforeEach(function () {
        jasmine.clock().install();
      });

      afterEach(function () {
        jasmine.clock().uninstall();
      });

      it('dispatches FIREWALL_QUARANTINE_LIST_FULFILLED action', function (done) {
        mockAxiosCalls({
          get: {
            [firewallQuarantineListUrl + defaultParams]: Promise.resolve({ data: payload }),
          },
        });

        jasmine.clock().mockDate(lastUpdated);

        store.dispatch(loadQuarantineList()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(3);
          expect(actions[0].type).toBe(FIREWALL_QUARANTINE_LIST_REQUESTED);
          expect(actions[0].payload).toBeUndefined();
          expect(actions[1].type).toBe(FIREWALL_QUARANTINE_LIST_FULFILLED);
          expect(actions[1].payload).toEqual(payload);
          expect(actions[2].type).toBe(FIREWALL_QUARANTINE_GRID_SET_LAST_UPDATED);
          expect(actions[2].payload).toEqual({ lastUpdated: lastUpdated });
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after a failed GET call', function () {
      let defaultParams = '?page=1&pageSize=12';

      it('dispatches an FIREWALL_QUARANTINE_LIST_FAILED action', function (done) {
        mockAxiosCalls({
          get: {
            [firewallQuarantineListUrl + defaultParams]: () => Promise.reject('error!'),
          },
        });

        store.dispatch(loadQuarantineList()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe(FIREWALL_QUARANTINE_LIST_FAILED);
          expect(actions[1].payload).toBe('error!');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });

  describe('setQuarantineGridPage', function () {
    it('immediately dispatches actions to set the current page for the quarantine grid', function () {
      let page = 1;

      store.dispatch(setQuarantineGridPage(page));

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe(FIREWALL_QUARANTINE_GRID_SET_PAGE);
      expect(actions[0].payload).toEqual({ currentPage: page });
      expect(actions[1].type).toBe(FIREWALL_QUARANTINE_LIST_REQUESTED);
      expect(actions[1].payload).toBeUndefined();
    });
  });

  describe('setQuarantineGridSorting', function () {
    it('immediately dispatches actions to set the sorting for the quarantine grid', function () {
      let sortField = 'testField',
        sortDir = 'asc';

      store.dispatch(setQuarantineGridSorting(sortDir, sortField));

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe(FIREWALL_QUARANTINE_GRID_SET_SORTING);
      expect(actions[0].payload).toEqual({ sortDir: sortDir, sortField: sortField });
      expect(actions[1].type).toBe(FIREWALL_QUARANTINE_LIST_REQUESTED);
      expect(actions[1].payload).toBeUndefined();
    });
  });

  describe('setQuarantineGridPolicyFilter', function () {
    it('immediately dispatches actions to set the policy ID filter for the quarantine grid', function () {
      let policy = { policy: '456' };

      store.dispatch(setQuarantineGridPolicyFilter(policy.policy));

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe(FIREWALL_QUARANTINE_GRID_SET_FILTER);
      expect(actions[0].payload).toEqual(policy);
      expect(actions[0].payload.policy).toEqual(jasmine.any(String));
      expect(actions[1].type).toBe(FIREWALL_QUARANTINE_LIST_REQUESTED);
      expect(actions[1].payload).toBeUndefined();
    });
  });

  describe('setQuarantineGridLastUpdated', function () {
    it('immediately dispatches actions to set the last updated timestamp for the quarantine grid', function () {
      let lastUpdated = new Date();

      store.dispatch(setQuarantineGridLastUpdated(lastUpdated));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_QUARANTINE_GRID_SET_LAST_UPDATED);
      expect(actions[0].payload).toEqual({ lastUpdated: lastUpdated });
    });
  });

  describe('loadFirewallData', function () {
    it('immediately dispatches actions to load all firewall data', function () {
      store.dispatch(loadFirewallData());

      const actions = store.getActions();
      expect(actions.length).toBe(6);
      expect(actions[0].type).toBe(FIREWALL_LOAD_DATA_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
      expect(actions[1].type).toBe(FIREWALL_LOAD_CONFIGURATION_REQUESTED);
      expect(actions[1].payload).toBeUndefined();
      expect(actions[2].type).toBe(FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED);
      expect(actions[2].payload).toBeUndefined();
      expect(actions[3].type).toBe(FIREWALL_QUARANTINE_SUMMARY_REQUESTED);
      expect(actions[3].payload).toBeUndefined();
      expect(actions[4].type).toBe(FIREWALL_QUARANTINE_LIST_REQUESTED);
      expect(actions[4].payload).toBeUndefined();
      expect(actions[5].type).toBe(FIREWALL_POLICIES_REQUESTED);
      expect(actions[5].payload).toBeUndefined();
    });
  });

  describe('loadAutoUnquarantineData', function () {
    it('immediately dispatches actions to load all firewall release quarantine data', function () {
      store.dispatch(loadAutoUnquarantineData());

      const actions = store.getActions();
      expect(actions.length).toBe(4);
      expect(actions[0].type).toBe(FIREWALL_AUTO_UNQUARANTINE_DATA_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
      expect(actions[1].type).toBe(FIREWALL_LOAD_CONFIGURATION_REQUESTED);
      expect(actions[1].payload).toBeUndefined();
      expect(actions[2].type).toBe(FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED);
      expect(actions[2].payload).toBeUndefined();
      expect(actions[3].type).toBe(FIREWALL_RELEASE_QUARANTINE_LIST_REQUESTED);
      expect(actions[3].payload).toBeUndefined();
    });
  });

  describe('selectQuarantineComponent', function () {
    it('immediately dispatches actions to set the selected component and show the CIP modal', function () {
      let components = [{ componentDisplayTex: 'text' }];
      state = {
        firewall: Object.freeze({
          quarantineGridState: Object.freeze({
            quarantineList: components,
          }),
        }),
      };

      store = SpecUtil.mockReduxStore(state);
      store.dispatch(selectQuarantineComponent(0));

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe(FIREWALL_SELECT_COMPONENT);
      expect(actions[0].payload).toEqual({
        component: components[0],
        componentIndex: 0,
        components: components,
      });
      expect(actions[1].type).toBe(FIREWALL_CIP_MODAL_SHOW);
      expect(actions[1].payload).toBeUndefined();
    });
  });

  describe('selectReleaseQuarantineComponent', function () {
    it('immediately dispatches actions to set the selected component and show the CIP modal', function () {
      let components = [{ componentDisplayTex: 'text' }];
      state = {
        firewall: Object.freeze({
          autoUnquarantineState: Object.freeze({
            autoUnquarantineGridState: Object.freeze({
              releaseQuarantineList: components,
            }),
          }),
        }),
      };

      store = SpecUtil.mockReduxStore(state);
      store.dispatch(selectReleaseQuarantineComponent(0));

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe(FIREWALL_SELECT_COMPONENT);
      expect(actions[0].payload).toEqual({
        component: components[0],
        componentIndex: 0,
        components: components,
      });
      expect(actions[1].type).toBe(FIREWALL_CIP_MODAL_SHOW);
      expect(actions[1].payload).toBeUndefined();
    });
  });

  describe('selectComponent', function () {
    it('immediately dispatches actions to set the selected component', function () {
      let components = [{ componentDisplayTex: 'text' }];
      state = {
        firewall: Object.freeze({
          cip: Object.freeze({
            showCipModal: false,
            selectedComponent: null,
            selectedComponentIndex: null,
            displayedEntries: components,
          }),
        }),
      };

      store = SpecUtil.mockReduxStore(state);
      store.dispatch(selectComponent(0));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_SELECT_COMPONENT);
      expect(actions[0].payload).toEqual({
        components: components,
        componentIndex: 0,
        component: components[0],
      });
    });
  });

  describe('loadComponentDetails', () => {
    it('immediatly dispatches actions to retrieve the component details that matches with the params', function (done) {
      const repositoryId = 'repositoryId',
        componentIdentifier =
          '{"format":"maven","coordinates":{"artifactId":"ant","classifier":"","extension":"jar","groupId":"ant","version":"1.6.3"}}',
        componentHash = 'componentHash',
        matchState = 'exact',
        proprietary = true,
        identificationSource = 'sonatype',
        scanId = 'scanId',
        ownerType = 'repository';

      const componentDetailsParams = {
        repositoryId,
        componentIdentifier,
        componentHash,
        matchState,
        proprietary,
        identificationSource,
        scanId,
      };
      const requestParams = {
        clientType: 'ci',
        ownerType,
        ownerId: repositoryId,
        componentIdentifier,
        hash: componentHash,
        matchState,
        proprietary,
        identificationSource,
        scanId,
      };
      const componentDetailsUrl = getComponentDetailsUrl(requestParams);
      const componentDetailsUrlMockResponse = {
        hash: 'b7c953dd67e01c952d79',
        matchState: 'exact',
      };
      const componentLabelsUrl = getComponentLabels(repositoryId, componentHash, ownerType);
      const componentLabelsUrlMockResponse = {
        data: { labelsByOwner: [] },
      };

      mockAxiosCalls({
        get: {
          [componentDetailsUrl]: Promise.resolve({
            data: componentDetailsUrlMockResponse,
          }),
          [componentLabelsUrl]: Promise.resolve(componentLabelsUrlMockResponse),
        },
      });

      store.dispatch(loadComponentDetails(componentDetailsParams)).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(6);
        expect(actions[0].type).toBe(FIREWALL_COMPONENT_DETAILS_REQUESTED);
        expect(actions[0].payload).toBeUndefined();
        expect(actions[1].type).toBe(FIREWALL_COMPONENT_DETAILS_FULFILLED);
        expect(actions[1].payload).toBe(componentDetailsUrlMockResponse);
        expect(actions[2].type).toBe('componentDetails/loadFirewallComponentDetailsLabels/pending');
        expect(actions[2].payload).toBeUndefined();
        expect(actions[3].type).toBe('componentDetails/loadFirewallComponentDetailsLabelsWithCancelToken/pending');
        expect(actions[3].payload).toBeUndefined();
        expect(actions[4].type).toBe('componentDetails/loadFirewallComponentDetailsLabels/fulfilled');
        expect(actions[4].payload).toBeUndefined();
        expect(actions[5].type).toBe('componentDetails/loadFirewallComponentDetailsLabelsWithCancelToken/fulfilled');
        expect(actions[5].payload).toEqual(componentLabelsUrlMockResponse);
        done();
      });
    });
    it('immediatly dispatches actions to handle a component details request error', function () {
      const repositoryId = 'repositoryId',
        componentIdentifier =
          '{"format":"maven","coordinates":{"artifactId":"ant","classifier":"","extension":"jar","groupId":"ant","version":"1.6.3"}}',
        componentHash = 'componentHash',
        matchState = 'exact',
        proprietary = true,
        identificationSource = 'sonatype',
        scanId = 'scanId';
      const componentDetailsParams = {
        repositoryId,
        componentIdentifier,
        componentHash,
        matchState,
        proprietary,
        identificationSource,
        scanId,
      };
      const requestParams = {
        clientType: 'ci',
        ownerType: 'repository',
        ownerId: repositoryId,
        componentIdentifier,
        hash: componentHash,
        matchState,
        proprietary,
        identificationSource,
        scanId,
      };
      const componentDetailsUrl = getComponentDetailsUrl(requestParams);
      const mockResponse = 'error!';

      mockAxiosCalls({
        get: {
          [componentDetailsUrl]: Promise.reject(mockResponse),
        },
      });

      store.dispatch(loadComponentDetails(componentDetailsParams)).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe(FIREWALL_COMPONENT_DETAILS_REQUESTED);
        expect(actions[0].payload).toBeUndefined();
        expect(actions[1].type).toBe(FIREWALL_COMPONENT_DETAILS_FAILED);
        expect(actions[1].payload).toBe(mockResponse);
      });
    });
  });

  describe('loadComponentDetailsRequested', () => {
    it('dispatches an action to indicate the request is being solved but not completed yet', () => {
      const customState = {
        firewall: Object.freeze({
          componentDetailsPage: Object.freeze({
            isLoadingComponentDetails: false,
            componentDetails: null,
            componentDetailsError: null,
          }),
        }),
      };

      store = SpecUtil.mockReduxStore(customState);
      store.dispatch(loadComponentDetailsRequested());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_COMPONENT_DETAILS_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });
  });

  describe('loadComponentDetailsFulfilled', () => {
    it('dispatches an action to indicate the request was solved successfully', () => {
      const customState = {
        firewall: Object.freeze({
          componentDetailsPage: Object.freeze({
            isLoadingComponentDetails: true,
            componentDetails: null,
            componentDetailsError: null,
          }),
        }),
      };

      const mockResponse = { hash: 'hash' };
      store = SpecUtil.mockReduxStore(customState);
      store.dispatch(loadComponentDetailsFulfilled(mockResponse));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_COMPONENT_DETAILS_FULFILLED);
      expect(actions[0].payload).toBe(mockResponse);
    });
  });

  describe('loadComponentDetailsFailed', () => {
    it('dispatches an action to indicate the request failed', () => {
      const customState = {
        firewall: Object.freeze({
          componentDetailsPage: Object.freeze({
            isLoadingComponentDetails: true,
            componentDetails: null,
            componentDetailsError: null,
          }),
        }),
      };

      const mockResponse = 'error!';
      store = SpecUtil.mockReduxStore(customState);
      store.dispatch(loadComponentDetailsFailed(mockResponse));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_COMPONENT_DETAILS_FAILED);
      expect(actions[0].payload).toBe(mockResponse);
    });
  });

  describe('loadComponentPolicyViolationsRequested', () => {
    it('dispatches an action to indicate the request is being solved but not completed yet', () => {
      const customState = {
        firewall: Object.freeze({
          componentDetailsPage: Object.freeze({
            policyViolations: null,
            isLoadingPolicyViolations: false,
            policyViolationsError: null,
          }),
        }),
      };

      store = SpecUtil.mockReduxStore(customState);
      store.dispatch(loadComponentPolicyViolationsRequested());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });
  });

  describe('loadComponentPolicyViolationsFulfilled', () => {
    it('dispatches an action to indicate the request was solved successfully', () => {
      const customState = {
        firewall: Object.freeze({
          componentDetailsPage: Object.freeze({
            policyViolations: null,
            isLoadingPolicyViolations: true,
            policyViolationsError: null,
          }),
        }),
      };

      const mockResponse = { hash: 'hash' };
      store = SpecUtil.mockReduxStore(customState);
      store.dispatch(loadComponentPolicyViolationsFulfilled(mockResponse));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_FULFILLED);
      expect(actions[0].payload).toBe(mockResponse);
    });
  });

  describe('loadComponentPolicyViolationsFailed', () => {
    it('dispatches an action to indicate the request failed', () => {
      const customState = {
        firewall: Object.freeze({
          componentDetailsPage: Object.freeze({
            policyViolations: null,
            isLoadingPolicyViolations: false,
            policyViolationsError: null,
          }),
        }),
      };

      const mockResponse = 'error!';
      store = SpecUtil.mockReduxStore(customState);
      store.dispatch(loadComponentPolicyViolationsFailed(mockResponse));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_FAILED);
      expect(actions[0].payload).toBe(mockResponse);
    });
  });

  describe('onComponentDetailsPageTabChange', () => {
    it('calls stateGo with the appropriate parameters', () => {
      store.dispatch(onComponentDetailsPageTabChange('labels'));
      expect(store.getActions()).toHaveAction({
        type: '@@reduxUiRouter/stateGo',
        payload: {
          to: 'firewall.componentDetailsPage.labels',
          options: undefined,
          params: undefined,
        },
      });
    });
  });

  describe('loadExistingWaiversDataRequested', () => {
    it('dispatches an action to indicate the request is being solved but not completed yet', () => {
      const customState = {
        firewall: Object.freeze({
          componentDetailsPage: Object.freeze({
            policyExistingWaivers: null,
            isLoadExistingWaivers: false,
            existingWaiversError: null,
          }),
        }),
      };

      store = SpecUtil.mockReduxStore(customState);
      store.dispatch(loadExistingWaiversDataRequested());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_LOAD_EXISTING_WAIVERS_DATA_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });
  });

  describe('loadExistingWaiversDataFulfilled', () => {
    it('dispatches an action to indicate the request was solved successfully', () => {
      const customState = {
        firewall: Object.freeze({
          componentDetailsPage: Object.freeze({
            policyExistingWaivers: null,
            isLoadExistingWaivers: true,
            existingWaiversError: null,
          }),
        }),
      };

      const mockResponse = { waivers: 'waivers' };
      store = SpecUtil.mockReduxStore(customState);
      store.dispatch(loadExistingWaiversDataFulfilled(mockResponse));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_LOAD_EXISTING_WAIVERS_DATA_FULFILLED);
      expect(actions[0].payload).toBe(mockResponse);
    });
  });

  describe('loadExistingWaiversDataFailed', () => {
    it('dispatches an action to indicate the request failed', () => {
      const customState = {
        firewall: Object.freeze({
          componentDetailsPage: Object.freeze({
            policyExistingWaivers: null,
            isLoadExistingWaivers: false,
            existingWaiversError: null,
          }),
        }),
      };

      const mockResponse = 'error!';
      store = SpecUtil.mockReduxStore(customState);
      store.dispatch(loadExistingWaiversDataFailed(mockResponse));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_LOAD_EXISTING_WAIVERS_DATA_FAILED);
      expect(actions[0].payload).toBe(mockResponse);
    });
  });

  describe('onGoToFirewallWaiversPage', () => {
    it('calls stateGo with the appropriate parameters', () => {
      store.dispatch(onGoToFirewallWaiversPage('policyViolationId'));

      const actions = store.getActions();

      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe('@@reduxUiRouter/stateGo');
      expect(actions[0].payload).toEqual({
        to: 'firewall.violationWaivers',
        params: {
          componentHash: 'componentHash',
          componentIdentifier: {
            format: 'maven',
            coordinates: {
              artifactId: 'ant',
              classifier: '',
              extension: 'jar',
              groupId: 'ant',
              version: '1.6',
            },
          },
          violationId: 'policyViolationId',
          repositoryId: 'repositoryId',
          matchState: 'matchState',
          proprietary: 'proprietary',
          identificationSource: 'identificationSource',
          pathname: 'pathname',
          tabId: undefined,
        },
        options: undefined,
      });
    });
  });

  describe('loadFirewallViolationDetails', () => {
    let mockData;
    beforeEach(function () {
      mockData = {
        policyViolationId: 'e0ecf0a629d341e88179f8d40f4675ee',
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'ant',
            classifier: '',
            extension: 'jar',
            groupId: 'ant',
            version: '1.6',
          },
        },
        componentDisplayName: {
          parts: [
            {
              field: 'Group',
              value: 'ant',
            },
            {
              value: ' : ',
            },
            {
              field: 'Artifact',
              value: 'ant',
            },
            {
              value: ' : ',
            },
            {
              field: 'Version',
              value: '1.6',
            },
          ],
          name: 'ant',
        },
        hash: '7a3c2521ae0c6f53e044',
        policyId: 'd98fb873ed1f48e5b00316d8acddbc0f',
        policyName: 'Security-Medium',
        policyOwner: {
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
        },
        policyThreatLevel: 7,
        policyThreatCategory: 'SECURITY',
        constraints: [
          {
            constraintId: 'c6436a5a051046b1ba2aa94e9fd82a51',
            constraintName: 'Medium risk CVSS score',
            constraintOperator: 'AND',
            conditions: [
              {
                conditionType: 'SecurityVulnerabilitySeverity',
                conditionSummary: 'Security Vulnerability Severity >= 4',
                conditionReason: 'Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)',
                conditionTriggerReference: {
                  value: 'CVE-2012-2098',
                  type: 'SECURITY_VULNERABILITY_REFID',
                },
              },
              {
                conditionType: 'SecurityVulnerabilitySeverity',
                conditionSummary: 'Security Vulnerability Severity < 7',
                conditionReason: 'Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)',
                conditionTriggerReference: {
                  value: 'CVE-2012-2098',
                  type: 'SECURITY_VULNERABILITY_REFID',
                },
              },
            ],
          },
        ],
        constraintFactsJson:
          '[{"constraintId":"c6436a5a051046b1ba2aa94e9fd82a51","constraintName":"Medium risk CVSS score","operatorName":"AND","conditionFacts":[{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":0,"summary":"Security Vulnerability Severity >= 4","reason":"Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)","reference":{"value":"CVE-2012-2098","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"refId\\":\\"CVE-2012-2098\\",\\"severity\\":5.0}}"},{"conditionTypeId":"SecurityVulnerabilitySeverity","conditionIndex":1,"summary":"Security Vulnerability Severity < 7","reason":"Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)","reference":{"value":"CVE-2012-2098","type":"SECURITY_VULNERABILITY_REFID"},"triggerJson":"{\\"conditionIndex\\":1,\\"trigger\\":{\\"refId\\":\\"CVE-2012-2098\\",\\"severity\\":5.0}}"}]}]',
        policyActionTypeId: null,
        lastReported: '2022-10-10T16:01:37.586+03:00',
        threatLevel: 7,
        constraintViolations: [
          {
            constraintId: 'c6436a5a051046b1ba2aa94e9fd82a51',
            constraintName: 'Medium risk CVSS score',
            reasons: [
              {
                reason: 'Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)',
                reference: null,
              },
            ],
          },
        ],
        applicationPublicId: '',
        applicationName: '',
        organizationName: '',
        openTime: '2022-10-10T16:01:37.586+03:00',
        fixTime: null,
        displayName: {
          parts: [
            {
              field: 'Group',
              value: 'ant',
            },
            {
              value: ' : ',
            },
            {
              field: 'Artifact',
              value: 'ant',
            },
            {
              value: ' : ',
            },
            {
              field: 'Version',
              value: '1.6',
            },
          ],
          name: 'ant',
        },
        filename: null,
        stageData: {},
        waived: false,
      };

      mockAxiosCalls({
        get: {
          [requestRepositoryPolicyViolationUrl]: () => Promise.resolve({}),
          [repositoryPolicyViolationUrl]: () => Promise.resolve({ data: mockData }),
          [errorRepositoryPolicyViolationUrl]: () => Promise.reject('error'),
        },
      });
    });

    it('dispatch a FIREWALL_LOAD_VIOLATION_DETAIL_REQUESTED action', () => {
      store.dispatch(loadFirewallViolationDetails('RepolicyViolationId'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_LOAD_VIOLATION_DETAIL_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    it('dispatches FIREWALL_LOAD_VIOLATION_DETAIL_FULFILLED action after successfully requests', (done) => {
      store.dispatch(loadFirewallViolationDetails('policyViolationId')).then(() => {
        actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe(FIREWALL_LOAD_VIOLATION_DETAIL_REQUESTED);
        expect(actions[0].payload).toBeUndefined();
        expect(actions[1].type).toBe(FIREWALL_LOAD_VIOLATION_DETAIL_FULFILLED);
        expect(actions[1].payload).toEqual(mockData);
        done();
      });

      let actions = store.getActions();
      expect(actions.length).toBe(1);
    });

    it('dispatches FIREWALL_LOAD_VIOLATION_DETAIL_FAILED action after one of all of the requests failed', () => {
      store.dispatch(loadFirewallViolationDetails('ErrorpolicyViolationId')).then(() => {
        actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe(FIREWALL_LOAD_VIOLATION_DETAIL_REQUESTED);
        expect(actions[0].payload).toBeUndefined();
        expect(actions[1].type).toBe(FIREWALL_LOAD_VIOLATION_DETAIL_FAILED);
        expect(actions[1].payload).toBe('error');
      });
      let actions = store.getActions();
      expect(actions.length).toBe(1);
    });
  });
});
