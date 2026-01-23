/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import {
  closeConfigurationModal,
  FIREWALL_SET_SHOW_WELCOME_MODAL,
  FIREWALL_AUTO_UNQUARANTINE_DATA_REQUESTED,
  FIREWALL_AUTO_UNQUARANTINE_GRID_SET_PAGE,
  FIREWALL_AUTO_UNQUARANTINE_GRID_SET_SORTING,
  FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE,
  FIREWALL_LOAD_CONFIGURATION_FAILED,
  FIREWALL_LOAD_CONFIGURATION_FULFILLED,
  FIREWALL_LOAD_CONFIGURATION_REQUESTED,
  FIREWALL_LOAD_DATA_REQUESTED,
  FIREWALL_POLICIES_FAILED,
  FIREWALL_POLICIES_FULFILLED,
  FIREWALL_POLICIES_REQUESTED,
  FIREWALL_POLICIES_WITH_CONDITIONS_REQUESTED,
  FIREWALL_POLICIES_WITH_CONDITIONS_FULFILLED,
  FIREWALL_POLICIES_WITH_CONDITIONS_FAILED,
  FIREWALL_QUARANTINE_GRID_SET_POLICY_FILTER,
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
  FIREWALL_LOAD_TILE_METRICS_REQUESTED,
  FIREWALL_LOAD_TILE_METRICS_FAILED,
  FIREWALL_LOAD_TILE_METRICS_FULFILLED,
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
  setAutoUnquarantineGridPage,
  setAutoUnquarantineGridSorting,
  setQuarantineGridLastUpdated,
  setQuarantineGridPage,
  setQuarantineGridPolicyFilter,
  setQuarantineGridSorting,
  setQuarantineGridPolicyFilterWithProprietaryNameConflict,
  setQuarantineGridPolicyFilterWithSecurityVulnerabilityCategoryMaliciousCode,
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
  loadPoliciesWithConditionsRequested,
  loadPoliciesWithConditionsFulfilled,
  loadPoliciesWithConditionsFailed,
  onGoToRepositoryComponentWaiversPage,
  loadFirewallViolationDetails,
  FIREWALL_QUARANTINE_GRID_SET_COMPONENT_NAME_FILTER,
  setQuarantineGridComponentNameFilter,
  FIREWALL_QUARANTINE_GRID_SET_REPOSITORY_PUBLIC_ID_FILTER,
  setQuarantineGridRepositoryPublicIdFilter,
  FIREWALL_QUARANTINE_GRID_SET_QUARANTINE_TIME_FILTER,
  setQuarantineGridQuarantineTimeFilter,
  loadTileMetrics,
  loadContainerQuarantineList,
  FIREWALL_CONTAINER_QUARANTINE_LIST_REQUESTED,
  FIREWALL_CONTAINER_QUARANTINE_LIST_FULFILLED,
  FIREWALL_CONTAINER_QUARANTINE_GRID_SET_LAST_UPDATED,
  FIREWALL_CONTAINER_QUARANTINE_LIST_FAILED,
  FIREWALL_CONTAINER_WAIVER_LIST_REQUESTED,
  FIREWALL_CONTAINER_WAIVER_LIST_FULFILLED,
  FIREWALL_CONTAINER_WAIVER_LIST_FAILED,
  FIREWALL_CONTAINER_WAIVER_GRID_SET_LAST_UPDATED,
  FIREWALL_CONTAINER_WAIVER_GRID_SET_PAGE,
  loadContainerWaiverList,
  setContainerWaiverGridLastUpdated,
  setContainerWaiverGridPage,
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
  getFirewallTileMetricsUrl,
  getPoliciesWithProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCodeUrl,
  getSimilarWaiversUrl,
  getApplicableWaiversUrl,
  getFirewallContainerQuarantineListUrl,
  getFirewallContainerWaiverListUrl,
} from 'MainRoot/util/CLMLocation';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { INTEGRITY_RATING_POLICY_TYPE_ID } from '../../../main/frontend/firewall/config/firewallConfigurationModalReducer';
import { getPermissionContextTestUrl } from 'MainRoot/util/CLMContextLocation';
import {
  VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED,
  VIOLATION_FETCH_APPLICABLE_WAIVERS_REQUESTED,
} from 'MainRoot/violation/violationActions';

describe('firewallActions', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios),
    firewallConfigUrl = getFirewallConfigurationUrl(),
    firewallReleaseQuarantineSummaryUrl = getFirewallReleaseQuarantineSummaryUrl(),
    firewallReleaseQuarantineListUrl = getFirewallReleaseQuarantineListUrl(),
    firewallTileMetricsUrl = getFirewallTileMetricsUrl(),
    firewallQuarantineSummaryUrl = getFirewallQuarantineSummaryUrl(),
    firewallQuarantineListUrl = getFirewallQuarantineListUrl(),
    firewallQuarantineListWithConditionsUrl = getPoliciesWithProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCodeUrl(),
    firewallContainerQuarantineListUrl = getFirewallContainerQuarantineListUrl(),
    firewallContainerWaiverListUrl = getFirewallContainerWaiverListUrl(),
    policiesUrl = getPoliciesUrl(),
    requestRepositoryPolicyViolationUrl = getRepositoryPolicyViolationUrl('repositoryId', 'RepolicyViolationId'),
    repositoryPolicyViolationUrl = getRepositoryPolicyViolationUrl('repositoryId', 'policyViolationId'),
    similarWaiversUrl = getSimilarWaiversUrl('policyViolationId'),
    similarWaiversUrlRe = getSimilarWaiversUrl('RepolicyViolationId'),
    similarWaiversUrlError = getSimilarWaiversUrl('ErrorpolicyViolationId'),
    applicableWaiversUrl = getApplicableWaiversUrl('policyViolationId'),
    applicableWaiversUrlRe = getApplicableWaiversUrl('RepolicyViolationId'),
    applicableWaiversUrlError = getApplicableWaiversUrl('ErrorpolicyViolationId'),
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

  let store,
    state,
    pathname = 'pathname',
    routeName = 'firewall.whatever';

  beforeEach(function () {
    state = {
      firewall: Object.freeze({
        showWelcomeModal: false,
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
          sortDir: 'desc',
          sortField: 'quarantineTime',
          filterPolicies: [],
          lastUpdated: null,
        }),
        containerQuarantineGridState: Object.freeze({
          loadContainerQuarantineGridError: null,
          loadedContainerQuarantineList: false,
          containerQuarantineList: [],
          containerQuarantinePageCount: 0,
          containerPageSize: 12,
          containerCurrentPage: null,
          containerLastUpdated: null,
        }),
        policiesState: Object.freeze({
          loadedPolicies: false,
          policies: [],
        }),
        containerWaiverGridState: Object.freeze({
          loadContainerWaiverGridError: null,
          loadingContainerWaiverList: false,
          containerWaiverList: [],
          containerWaiverPageCount: 0,
          containerWaiverPageSize: 10,
          containerWaiverCurrentPage: null,
          containerWaiverLastUpdated: null,
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
          pathname,
        }),
        currentState: {
          name: routeName,
        },
      }),
      componentDetails: {
        pendingLoads: new Set(),
      },
    };

    store = SpecUtil.mockReduxStore(state);
  });

  describe('Welcome Modal', function () {
    let getShowWelcomeModalFromStoreSpy, removeShowWelcomeModalFromStoreSpy, actionsModule;

    beforeEach(() => {
      getShowWelcomeModalFromStoreSpy = jasmine.createSpy('getShowWelcomeModalFromStore');
      removeShowWelcomeModalFromStoreSpy = jasmine.createSpy('removeShowWelcomeModalFromStore');
      actionsModule = require('inject-loader!../../../main/frontend/firewall/firewallActions')({
        './firewallWelcomeModalStore': {
          getShowWelcomeModalFromStore: getShowWelcomeModalFromStoreSpy,
          removeShowWelcomeModalFromStore: removeShowWelcomeModalFromStoreSpy,
        },
      });
    });

    describe('setShowWelcomeModal', function () {
      it('dispatches setShowWelcomeModal given boolean payload', function () {
        store.dispatch(actionsModule.setShowWelcomeModal(true));
        store.dispatch(actionsModule.setShowWelcomeModal(false));

        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe(FIREWALL_SET_SHOW_WELCOME_MODAL);
        expect(actions[0].payload).toEqual(true);
        expect(actions[1].type).toBe(FIREWALL_SET_SHOW_WELCOME_MODAL);
        expect(actions[1].payload).toEqual(false);
      });
    });

    describe('initializeWelcomeModal', function () {
      it('dispatches setShowWelcomeModal with payload boolean value from getShowWelcomeModalFromStore', function () {
        getShowWelcomeModalFromStoreSpy.and.returnValue(true);
        store.dispatch(actionsModule.initializeWelcomeModal());

        expect(getShowWelcomeModalFromStoreSpy).toHaveBeenCalled();

        getShowWelcomeModalFromStoreSpy.and.returnValue(false);
        store.dispatch(actionsModule.initializeWelcomeModal());

        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe(FIREWALL_SET_SHOW_WELCOME_MODAL);
        expect(actions[0].payload).toEqual(true);
        expect(actions[1].type).toBe(FIREWALL_SET_SHOW_WELCOME_MODAL);
        expect(actions[1].payload).toEqual(false);
      });
    });

    describe('closeWelcomeModal', function () {
      it('calls removeShowWelcomeModalFromStore and dispatches setShowWelcomeModal with payload of false', function () {
        store.dispatch(actionsModule.closeWelcomeModal());

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(FIREWALL_SET_SHOW_WELCOME_MODAL);
        expect(actions[0].payload).toEqual(false);
        expect(removeShowWelcomeModalFromStoreSpy).toHaveBeenCalled();
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
          [erroneusLicensesOverrideUrl]: () => Promise.reject('error'),
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
          expect(actions.length).toBe(3);
          expect(actions[2].type).toBe(FIREWALL_LOAD_CONFIGURATION_FAILED);
          expect(actions[2].payload).toBe('error!');
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

  describe('loadTileMetrics', function () {
    afterEach(function () {
      expect(axios.get).toHaveBeenCalledWith(firewallTileMetricsUrl);
    });

    it('immediately dispatches a FIREWALL_LOAD_TILE_METRICS_REQUESTED action', function () {
      mockAxiosCalls({
        get: {
          [firewallTileMetricsUrl]: Promise.resolve({
            data: {},
          }),
        },
      });

      store.dispatch(loadTileMetrics());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_LOAD_TILE_METRICS_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful GET call', function () {
      it('dispatches FIREWALL_LOAD_TILE_METRICS_FULFILLED action', function (done) {
        mockAxiosCalls({
          get: {
            [firewallTileMetricsUrl]: Promise.resolve({
              data: { test: 'test' },
            }),
          },
        });

        store.dispatch(loadTileMetrics()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe(FIREWALL_LOAD_TILE_METRICS_REQUESTED);
          expect(actions[0].payload).toBeUndefined();
          expect(actions[1].type).toBe(FIREWALL_LOAD_TILE_METRICS_FULFILLED);
          expect(actions[1].payload).toEqual({ test: 'test' });
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after a failed GET call', function () {
      it('dispatches an FIREWALL_LOAD_TILE_METRICS_FAILED action', function (done) {
        mockAxiosCalls({
          get: {
            [firewallTileMetricsUrl]: () => Promise.reject('error!'),
          },
        });

        store.dispatch(loadTileMetrics()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe(FIREWALL_LOAD_TILE_METRICS_FAILED);
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
    it('immediately dispatches a FIREWALL_QUARANTINE_LIST_REQUESTED action with the correct parameters', function () {
      state = {
        ...state,
        firewall: {
          ...state.firewall,
          quarantineGridState: {
            pageSize: 1000,
            sortField: 'field',
            currentPage: 100,
            filterPolicies: ['id'],
            filterComponentName: 'name',
            filterRepositoryPublicId: 'repositoryPublicId',
            sortDir: 'desc',
          },
        },
      };
      store = SpecUtil.mockReduxStore(state);
      const expectedParams =
        '?page=101&pageSize=1000&sortBy=field&asc=false&policyId=id&componentName=name&repositoryPublicId=repositoryPublicId';
      const payload = { pageCount: 2, results: [{ test: 'testVal' }, { test: 'testVal' }] };
      mockAxiosCalls({
        get: {
          [firewallQuarantineListUrl + expectedParams]: Promise.resolve(payload),
        },
      });

      store.dispatch(loadQuarantineList());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_QUARANTINE_LIST_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
      expect(axios.get).toHaveBeenCalledWith(firewallQuarantineListUrl + expectedParams);
    });
  });

  describe('loadQuarantineList', function () {
    let payload = { pageCount: 2, results: [{ test: 'testVal' }, { test: 'testVal' }] },
      defaultParams = '?page=1&pageSize=12&sortBy=quarantineTime&asc=false';

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
      let defaultParams = '?page=1&pageSize=12&sortBy=quarantineTime&asc=false',
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
      let defaultParams = '?page=1&pageSize=12&sortBy=quarantineTime&asc=false';

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
      let policy = { policies: '123' };

      store.dispatch(setQuarantineGridPolicyFilter(policy.policies));

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe(FIREWALL_QUARANTINE_GRID_SET_POLICY_FILTER);
      expect(actions[0].payload).toEqual(policy);
      expect(actions[0].payload.policies).toEqual(jasmine.any(String));
      expect(actions[1].type).toBe(FIREWALL_QUARANTINE_LIST_REQUESTED);
      expect(actions[1].payload).toBeUndefined();
    });
  });

  describe('setQuarantineGridPolicyFilterWithProprietaryNameConflict', function () {
    it('immediately dispatches actions and filters policies based on ProprietaryNameConflict', function (done) {
      state = {
        ...state,
        firewall: {
          ...state.firewall,
          policiesState: {
            loadedPolicies: true,
            policies: [{ id: 'a' }, { id: 'b' }, { id: 'c' }, { id: 'd' }],
          },
        },
      };
      store = SpecUtil.mockReduxStore(state);
      const data = {
        proprietaryNameConflictPolicies: [{ id: 'a' }, { id: 'b' }],
        securityVulnerabilityCategoryMaliciousCodePolicies: [{ id: 'c' }, { id: 'd' }],
      };
      mockAxiosCalls({
        get: {
          [firewallQuarantineListWithConditionsUrl]: Promise.resolve({ data }),
        },
      });
      store.dispatch(setQuarantineGridPolicyFilterWithProprietaryNameConflict()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe(FIREWALL_POLICIES_WITH_CONDITIONS_REQUESTED);
        expect(actions[1].type).toBe(FIREWALL_POLICIES_WITH_CONDITIONS_FULFILLED);
        expect(actions[2].type).toBe(FIREWALL_QUARANTINE_GRID_SET_POLICY_FILTER);
        expect(actions[2].payload).toEqual({ policies: ['a', 'b'] });
        expect(actions[3].type).toBe(FIREWALL_QUARANTINE_LIST_REQUESTED);
        expect(actions[3].payload).toBeUndefined();
        done();
      });
    });
  });

  describe('setQuarantineGridPolicyFilterWithSecurityVulnerabilityCategoryMaliciousCode', function () {
    it('immediately dispatches actions and filters policies based on SecurityVulnerabilityCategoryMaliciousCode', function (done) {
      state = {
        ...state,
        firewall: {
          ...state.firewall,
          policiesState: {
            loadedPolicies: true,
            policies: [{ id: 'a' }, { id: 'b' }, { id: 'c' }, { id: 'd' }],
          },
        },
      };
      store = SpecUtil.mockReduxStore(state);
      const data = {
        proprietaryNameConflictPolicies: [{ id: 'a' }, { id: 'b' }],
        securityVulnerabilityCategoryMaliciousCodePolicies: [{ id: 'c' }, { id: 'd' }],
      };
      mockAxiosCalls({
        get: {
          [firewallQuarantineListWithConditionsUrl]: Promise.resolve({ data }),
        },
      });
      store.dispatch(setQuarantineGridPolicyFilterWithSecurityVulnerabilityCategoryMaliciousCode()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe(FIREWALL_POLICIES_WITH_CONDITIONS_REQUESTED);
        expect(actions[1].type).toBe(FIREWALL_POLICIES_WITH_CONDITIONS_FULFILLED);
        expect(actions[2].type).toBe(FIREWALL_QUARANTINE_GRID_SET_POLICY_FILTER);
        expect(actions[2].payload).toEqual({ policies: ['c', 'd'] });
        expect(actions[3].type).toBe(FIREWALL_QUARANTINE_LIST_REQUESTED);
        expect(actions[3].payload).toBeUndefined();
        done();
      });
    });
  });

  describe('loadPoliciesWithConditionsRequested', () => {
    it('dispatches an action to indicate the request is being solved but not completed yet', () => {
      store.dispatch(loadPoliciesWithConditionsRequested());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_POLICIES_WITH_CONDITIONS_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });
  });

  describe('loadPoliciesWithConditionsFulfilled', () => {
    it('dispatches an action to indicate the request was solved successfully', () => {
      store.dispatch(loadPoliciesWithConditionsFulfilled());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_POLICIES_WITH_CONDITIONS_FULFILLED);
      expect(actions[0].payload).toBeUndefined();
    });
  });

  describe('loadPoliciesWithConditionsFailed', () => {
    it('dispatches an action to indicate the request failed', () => {
      const mockResponse = 'error!';
      store.dispatch(loadPoliciesWithConditionsFailed(mockResponse));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_POLICIES_WITH_CONDITIONS_FAILED);
      expect(actions[0].payload).toBe(mockResponse);
    });
  });

  describe('setQuarantineGridComponentNameFilter', function () {
    it('immediately dispatches actions to set the component name filter for the quarantine grid', function () {
      const componentName = { componentName: 'name' };
      const currentPage = { currentPage: null };

      store.dispatch(setQuarantineGridComponentNameFilter(componentName.componentName));

      const actions = store.getActions();
      expect(actions.length).toBe(3);
      expect(actions[0].type).toBe(FIREWALL_QUARANTINE_GRID_SET_COMPONENT_NAME_FILTER);
      expect(actions[0].payload).toEqual(componentName);
      expect(actions[0].payload.componentName).toEqual('name');
      expect(actions[1].type).toBe(FIREWALL_QUARANTINE_GRID_SET_PAGE);
      expect(actions[1].payload).toEqual(currentPage);
      expect(actions[1].payload.currentPage).toBeNull();
      expect(actions[2].type).toBe(FIREWALL_QUARANTINE_LIST_REQUESTED);
      expect(actions[2].payload).toBeUndefined();
    });

    it('does not load the quarantine list if the component name is only 1 character', function () {
      const componentName = { componentName: 'n' };

      store.dispatch(setQuarantineGridComponentNameFilter(componentName.componentName));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_QUARANTINE_GRID_SET_COMPONENT_NAME_FILTER);
      expect(actions[0].payload).toEqual(componentName);
      expect(actions[0].payload.componentName).toEqual(jasmine.any(String));
    });
  });

  describe('setQuarantineGridRepositoryPublicIdFilter', function () {
    it('immediately dispatches actions to set the repository public id filter for the quarantine grid', function () {
      const repositoryPublicId = { repositoryPublicId: 'publicId' };
      const currentPage = { currentPage: null };

      store.dispatch(setQuarantineGridRepositoryPublicIdFilter(repositoryPublicId.repositoryPublicId));

      const actions = store.getActions();
      expect(actions.length).toBe(3);
      expect(actions[0].type).toBe(FIREWALL_QUARANTINE_GRID_SET_REPOSITORY_PUBLIC_ID_FILTER);
      expect(actions[0].payload).toEqual(repositoryPublicId);
      expect(actions[0].payload.repositoryPublicId).toEqual('publicId');
      expect(actions[1].type).toBe(FIREWALL_QUARANTINE_GRID_SET_PAGE);
      expect(actions[1].payload).toEqual(currentPage);
      expect(actions[1].payload.currentPage).toBeNull();
      expect(actions[2].type).toBe(FIREWALL_QUARANTINE_LIST_REQUESTED);
      expect(actions[2].payload).toBeUndefined();
    });

    it('does not load the quarantine list if the repository public id is only 1 character', function () {
      const repositoryPublicId = { repositoryPublicId: 'n' };

      store.dispatch(setQuarantineGridRepositoryPublicIdFilter(repositoryPublicId.repositoryPublicId));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_QUARANTINE_GRID_SET_REPOSITORY_PUBLIC_ID_FILTER);
      expect(actions[0].payload).toEqual(repositoryPublicId);
      expect(actions[0].payload.repositoryPublicId).toEqual(jasmine.any(String));
    });
  });

  describe('setQuarantineGridQuarantineTimeFilter', function () {
    it('immediately dispatches actions to set quarantine time filter for the quarantine grid', function () {
      const currentPage = { currentPage: null };

      store.dispatch(setQuarantineGridQuarantineTimeFilter(1));

      const actions = store.getActions();
      expect(actions.length).toBe(3);
      expect(actions[0].type).toBe(FIREWALL_QUARANTINE_GRID_SET_QUARANTINE_TIME_FILTER);
      expect(actions[1].payload).toEqual(currentPage);
      expect(actions[1].payload.currentPage).toBeNull();
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
      expect(actions.length).toBe(9);
      expect(actions[0].type).toBe(FIREWALL_LOAD_DATA_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
      expect(actions[1].type).toBe(FIREWALL_LOAD_CONFIGURATION_REQUESTED);
      expect(actions[1].payload).toBeUndefined();
      expect(actions[2].type).toBe(FIREWALL_LOAD_TILE_METRICS_REQUESTED);
      expect(actions[2].payload).toBeUndefined();
      expect(actions[3].type).toBe(FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED);
      expect(actions[3].payload).toBeUndefined();
      expect(actions[4].type).toBe(FIREWALL_QUARANTINE_SUMMARY_REQUESTED);
      expect(actions[4].payload).toBeUndefined();
      expect(actions[5].type).toBe(FIREWALL_QUARANTINE_LIST_REQUESTED);
      expect(actions[5].payload).toBeUndefined();
      expect(actions[6].type).toBe(FIREWALL_CONTAINER_QUARANTINE_LIST_REQUESTED);
      expect(actions[6].payload).toBeUndefined();
      expect(actions[7].type).toBe(FIREWALL_POLICIES_REQUESTED);
      expect(actions[7].payload).toBeUndefined();
      expect(actions[8].type).toBe(FIREWALL_CONTAINER_WAIVER_LIST_REQUESTED);
      expect(actions[8].payload).toBeUndefined();
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
    it('immediately dispatches actions to set the selected component', function () {
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
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_SELECT_COMPONENT);
      expect(actions[0].payload).toEqual({
        component: components[0],
        componentIndex: 0,
        components: components,
      });
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
          [componentDetailsUrl]: () => Promise.reject(mockResponse),
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

  describe('onGoToRepositoryComponentWaiversPage', () => {
    it('calls stateGo with the appropriate parameters', () => {
      store.dispatch(onGoToRepositoryComponentWaiversPage('policyViolationId'));

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
          pathname,
          tabId: undefined,
          componentDisplayName: undefined,
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
          [similarWaiversUrl]: () => Promise.resolve(),
          [similarWaiversUrlError]: () => Promise.resolve(),
          [similarWaiversUrlRe]: () => Promise.resolve(),
          [applicableWaiversUrl]: () => Promise.resolve(),
          [applicableWaiversUrlRe]: () => Promise.resolve(),
          [applicableWaiversUrlError]: () => Promise.resolve(),
        },
      });
    });

    it('dispatch a FIREWALL_LOAD_VIOLATION_DETAIL_REQUESTED action', () => {
      store.dispatch(loadFirewallViolationDetails('RepolicyViolationId'));

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe(FIREWALL_LOAD_VIOLATION_DETAIL_REQUESTED);
      expect(actions[1].type).toBe(VIOLATION_FETCH_APPLICABLE_WAIVERS_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    it('dispatches FIREWALL_LOAD_VIOLATION_DETAIL_FULFILLED action after successfully requests', (done) => {
      store.dispatch(loadFirewallViolationDetails('policyViolationId')).then(() => {
        actions = store.getActions();
        expect(actions.length).toBe(4);
        expect(actions[0].type).toBe(FIREWALL_LOAD_VIOLATION_DETAIL_REQUESTED);
        expect(actions[0].payload).toBeUndefined();
        expect(actions[1].type).toBe(VIOLATION_FETCH_APPLICABLE_WAIVERS_REQUESTED);
        expect(actions[2].type).toBe(VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED);
        expect(actions[3].type).toBe(FIREWALL_LOAD_VIOLATION_DETAIL_FULFILLED);
        expect(actions[3].payload).toEqual(mockData);
        done();
      });

      let actions = store.getActions();
      expect(actions.length).toBe(2);
    });

    it('dispatches FIREWALL_LOAD_VIOLATION_DETAIL_FAILED action after one of all of the requests failed', () => {
      store.dispatch(loadFirewallViolationDetails('ErrorpolicyViolationId')).then(() => {
        actions = store.getActions();
        expect(actions.length).toBe(4);
        expect(actions[0].type).toBe(FIREWALL_LOAD_VIOLATION_DETAIL_REQUESTED);
        expect(actions[0].payload).toBeUndefined();
        expect(actions[1].type).toBe(VIOLATION_FETCH_APPLICABLE_WAIVERS_REQUESTED);
        expect(actions[2].type).toBe(VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED);
        expect(actions[3].type).toBe(FIREWALL_LOAD_VIOLATION_DETAIL_FAILED);
        expect(actions[3].payload).toBe('error');
      });
      let actions = store.getActions();
      expect(actions.length).toBe(2);
    });

    it('dispatches FIREWALL_LOAD_VIOLATION_DETAIL_FULFILLED action after successfully requests with hasWaivePermission=true', (done) => {
      const urlPermissionRequest = getPermissionContextTestUrl('repository', state.router.currentParams.repositoryId);
      mockAxiosCalls({
        put: {
          [urlPermissionRequest]: Promise.resolve({
            data: ['WAIVE_POLICY_VIOLATIONS'],
          }),
        },
      });

      store.dispatch(loadFirewallViolationDetails('policyViolationId')).then(() => {
        actions = store.getActions();
        expect(actions.length).toBe(4);
        expect(actions[0].type).toBe(FIREWALL_LOAD_VIOLATION_DETAIL_REQUESTED);
        expect(actions[0].payload).toBeUndefined();
        expect(actions[1].type).toBe(VIOLATION_FETCH_APPLICABLE_WAIVERS_REQUESTED);
        expect(actions[2].type).toBe(VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED);
        expect(actions[3].type).toBe(FIREWALL_LOAD_VIOLATION_DETAIL_FULFILLED);
        expect(actions[3].payload.hasWaivePermission).toEqual(true);
        done();
      });

      let actions = store.getActions();
      expect(actions.length).toBe(2);
    });
  });

  describe('loadContainerQuarantineList', function () {
    it('immediately dispatches a FIREWALL_CONTAINER_QUARANTINE_LIST_REQUESTED action with the correct parameters', function () {
      state = {
        ...state,
        firewall: {
          ...state.firewall,
          containerQuarantineGridState: {
            containerPageSize: 1000,
            containerCurrentPage: 100,
          },
        },
      };
      store = SpecUtil.mockReduxStore(state);
      const expectedParams = '?page=101&pageSize=1000';
      const payload = { pageCount: 2, results: [{ test: 'testVal' }, { test: 'testVal' }] };
      mockAxiosCalls({
        get: {
          [firewallContainerQuarantineListUrl + expectedParams]: Promise.resolve(payload),
        },
      });

      store.dispatch(loadContainerQuarantineList());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_CONTAINER_QUARANTINE_LIST_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
      expect(axios.get).toHaveBeenCalledWith(firewallContainerQuarantineListUrl + expectedParams);
    });
  });

  describe('loadContainerQuarantineList', function () {
    let payload = { pageCount: 2, results: [{ test: 'testVal' }, { test: 'testVal' }] },
      defaultParams = '?page=1&pageSize=12';

    afterEach(function () {
      expect(axios.get).toHaveBeenCalledWith(firewallContainerQuarantineListUrl + defaultParams);
    });

    it('immediately dispatches a FIREWALL_CONTAINER_QUARANTINE_LIST_REQUESTED action', function () {
      mockAxiosCalls({
        get: {
          [firewallContainerQuarantineListUrl + defaultParams]: Promise.resolve(payload),
        },
      });

      store.dispatch(loadContainerQuarantineList());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_CONTAINER_QUARANTINE_LIST_REQUESTED);
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

      it('dispatches FIREWALL_CONTAINER_QUARANTINE_LIST_FULFILLED action', function (done) {
        mockAxiosCalls({
          get: {
            [firewallContainerQuarantineListUrl + defaultParams]: Promise.resolve({ data: payload }),
          },
        });

        jasmine.clock().mockDate(lastUpdated);

        store.dispatch(loadContainerQuarantineList()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(3);
          expect(actions[0].type).toBe(FIREWALL_CONTAINER_QUARANTINE_LIST_REQUESTED);
          expect(actions[0].payload).toBeUndefined();
          expect(actions[1].type).toBe(FIREWALL_CONTAINER_QUARANTINE_LIST_FULFILLED);
          expect(actions[1].payload).toEqual(payload);
          expect(actions[2].type).toBe(FIREWALL_CONTAINER_QUARANTINE_GRID_SET_LAST_UPDATED);
          expect(actions[2].payload).toEqual({ containerLastUpdated: lastUpdated });
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    describe('after a failed GET call', function () {
      let defaultParams = '?page=1&pageSize=12';

      it('dispatches an FIREWALL_CONTAINER_QUARANTINE_LIST_FAILED action', function (done) {
        mockAxiosCalls({
          get: {
            [firewallContainerQuarantineListUrl + defaultParams]: () => Promise.reject('error!'),
          },
        });

        store.dispatch(loadContainerQuarantineList()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe(FIREWALL_CONTAINER_QUARANTINE_LIST_FAILED);
          expect(actions[1].payload).toBe('error!');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });

  describe('loadContainerWaiverList', () => {
    const payload = {
        total: 2,
        page: 1,
        pageSize: 10,
        pageCount: 1,
        results: [{ 'policy-1': 'policy-1-val' }, { 'policy-2': 'policy-2-val' }],
      },
      defaultParams = '?page=1&pageSize=10';

    it('immediately dispatches a FIREWALL_CONTAINER_WAIVER_LIST_REQUESTED action', () => {
      mockAxiosCalls({
        get: {
          [firewallContainerWaiverListUrl + defaultParams]: Promise.resolve({}),
        },
      });
      store.dispatch(loadContainerWaiverList());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_CONTAINER_WAIVER_LIST_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful GET call', () => {
      beforeEach(function () {
        jasmine.clock().install();
      });

      afterEach(function () {
        jasmine.clock().uninstall();
      });

      it('dispatches a FIREWALL_CONTAINER_WAIVER_LIST_FULFILLED action', (done) => {
        const lastUpdated = new Date();
        mockAxiosCalls({
          get: {
            [firewallContainerWaiverListUrl + defaultParams]: Promise.resolve({ data: payload }),
          },
        });
        jasmine.clock().mockDate(lastUpdated);

        store.dispatch(loadContainerWaiverList()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(3);
          expect(actions[0].type).toBe(FIREWALL_CONTAINER_WAIVER_LIST_REQUESTED);
          expect(actions[0].payload).toBeUndefined();
          expect(actions[1].type).toBe(FIREWALL_CONTAINER_WAIVER_LIST_FULFILLED);
          expect(actions[1].payload).toEqual(payload);
          expect(actions[2].type).toBe(FIREWALL_CONTAINER_WAIVER_GRID_SET_LAST_UPDATED);
          expect(actions[2].payload).toEqual({ containerWaiverLastUpdated: lastUpdated });
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });

    it('dispatches a FIREWALL_CONTAINER_WAIVER_LIST_FAILED action after a failed GET call', (done) => {
      mockAxiosCalls({
        get: {
          [firewallContainerWaiverListUrl + defaultParams]: () => Promise.reject('something wrong'),
        },
      });

      store.dispatch(loadContainerWaiverList()).then(() => {
        actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe(FIREWALL_CONTAINER_WAIVER_LIST_REQUESTED);
        expect(actions[0].payload).toBeUndefined();
        expect(actions[1].type).toBe(FIREWALL_CONTAINER_WAIVER_LIST_FAILED);
        expect(actions[1].payload).toBe('something wrong');
        done();
      });

      let actions = store.getActions();
      expect(actions.length).toBe(1);
    });
  });

  describe('setContainerWaiverGridLastUpdated', function () {
    it('immediately dispatches actions to set the last updated timestamp for the container waiver grid', () => {
      let lastUpdated = new Date();
      store.dispatch(setContainerWaiverGridLastUpdated(lastUpdated));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_CONTAINER_WAIVER_GRID_SET_LAST_UPDATED);
      expect(actions[0].payload).toEqual({ containerWaiverLastUpdated: lastUpdated });
    });
  });

  describe('setContainerWaiverGridPage', function () {
    it('immediately dispatches actions to set container waiver grid page', () => {
      const page = 2;
      store.dispatch(setContainerWaiverGridPage(page));

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe(FIREWALL_CONTAINER_WAIVER_GRID_SET_PAGE);
      expect(actions[0].payload).toEqual({ containerWaiverCurrentPage: page });
    });
  });
});
