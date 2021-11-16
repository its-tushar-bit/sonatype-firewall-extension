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
} from '../../../main/frontend/firewall/firewallActions';
import {
  getFirewallConfigurationUrl,
  getFirewallQuarantineListUrl,
  getFirewallQuarantineSummaryUrl,
  getFirewallReleaseQuarantineListUrl,
  getFirewallReleaseQuarantineSummaryUrl,
  getPoliciesUrl,
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
    policiesUrl = getPoliciesUrl();

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
    };

    store = SpecUtil.mockReduxStore(state);
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
});
