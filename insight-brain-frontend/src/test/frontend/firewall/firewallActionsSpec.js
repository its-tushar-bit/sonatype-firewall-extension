/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import {
  closeConfigurationModal,
  FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE,
  FIREWALL_LOAD_CONFIGURATION_FAILED,
  FIREWALL_LOAD_CONFIGURATION_FULFILLED,
  FIREWALL_LOAD_CONFIGURATION_REQUESTED,
  FIREWALL_LOAD_STATUS_FAILED,
  FIREWALL_LOAD_STATUS_FULFILLED,
  FIREWALL_LOAD_STATUS_REQUESTED,
  FIREWALL_SAVE_CONFIGURATION_FAILED,
  FIREWALL_SAVE_CONFIGURATION_FULFILLED,
  FIREWALL_SAVE_CONFIGURATION_REQUESTED,
  FIREWALL_SET_SHOW_CONFIGURATION_MODAL,
  FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED,
  FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED,
  FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED,
  FIREWALL_RELEASE_QUARANTINE_LIST_REQUESTED,
  FIREWALL_RELEASE_QUARANTINE_LIST_FULFILLED,
  FIREWALL_RELEASE_QUARANTINE_LIST_FAILED,
  FIREWALL_AUTO_UNQUARANTINE_GRID_SET_PAGE,
  FIREWALL_AUTO_UNQUARANTINE_GRID_SET_SORTING,
  FIREWALL_AUTO_UNQUARANTINE_GRID_SET_FILTER,
  FIREWALL_POLICIES_REQUESTED,
  FIREWALL_POLICIES_FULFILLED,
  FIREWALL_POLICIES_FAILED,
  FIREWALL_LOAD_DATA_REQUESTED,
  FIREWALL_QUARANTINE_SUMMARY_FAILED,
  FIREWALL_QUARANTINE_SUMMARY_FULFILLED,
  FIREWALL_QUARANTINE_SUMMARY_REQUESTED,
  loadConfiguration,
  loadReleaseQuarantineSummary,
  loadReleaseQuarantineList,
  loadStatus,
  openConfigurationModal,
  saveConfiguration,
  loadData,
  loadPolicies,
  loadQuarantineSummary,
  loadAutoUnquarantineGridData,
  setAutoUnquarantineGridPage,
  setAutoUnquarantineGridPolicyFilter,
  setAutoUnquarantineGridSorting
} from '../../../main/frontend/firewall/firewallActions';
import {
  getFirewallConfigurationUrl,
  getFirewallReleaseQuarantineSummaryUrl,
  getFirewallStatusUrl,
  getFirewallQuarantineSummaryUrl,
  getFirewallReleaseQuarantineListUrl,
  getPoliciesUrl
} from '../../../main/frontend/util/CLMLocation';
import {SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS} from '@sonatype/react-shared-components';
import {INTEGRITY_RATING_POLICY_TYPE_ID} from
  '../../../main/frontend/firewall/config/firewallConfigurationModalReducer';

describe('firewallActions', function() {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios),
      firewallConfigUrl = getFirewallConfigurationUrl(),
      firewallStatusUrl = getFirewallStatusUrl(),
      firewallReleaseQuarantineSummaryUrl = getFirewallReleaseQuarantineSummaryUrl(),
      firewallReleaseQuarantineListUrl = getFirewallReleaseQuarantineListUrl(),
      firewallQuarantineSummaryUrl = getFirewallQuarantineSummaryUrl(),
      policiesUrl = getPoliciesUrl();

  let store, state;

  beforeEach(function() {
    state = {
      firewall: Object.freeze({
        viewState: Object.freeze({
          loadedStatus: false,
          loadStatusError: null,
          isShowConfigurationModal: false
        }),
        configurationState: Object.freeze({
          isEnabled: false
        }),
        autoUnquarantineState: Object.freeze({
          autoUnquarantineGridState: Object.freeze({
            loadedReleaseQuarantineList: false,
            loadedPolicies: false,
            releaseQuarantineList: [],
            releaseQuarantinePageCount: 0,
            pageSize: 12,
            currentPage: null,
            sortDir: null,
            sortField: null,
            filterPolicyId: '',
            policies: []
          })
        })
      }),
      firewallConfigurationModal: Object.freeze({
        viewState: Object.freeze({
          submitMaskSuccessState: false,
          saveConfigurationError: null,
          loadedConfiguration: false,
          loadConfigurationError: null,
          enabledPolicyConditionTypesCount: 0,
          totalPolicyConditionTypesCount: 1
        }),
        serverState: Object.freeze({
          conditionTypes: [
            {'id': INTEGRITY_RATING_POLICY_TYPE_ID, 'name': 'Integrity Rating', 'autoReleaseQuarantineEnabled': false}
          ]
        }),
        formState: Object.freeze({
          conditionTypes: [
            {'id': INTEGRITY_RATING_POLICY_TYPE_ID, 'name': 'Integrity Rating', 'autoReleaseQuarantineEnabled': false}
          ]
        })
      })
    };

    store = SpecUtil.mockReduxStore(state);
  });

  describe('loadStatus', function() {

    afterEach(function() {
      expect(axios.get).toHaveBeenCalledWith(firewallStatusUrl);
    });

    it('immediately dispatches a FIREWALL_LOAD_STATUS_REQUESTED action', function() {
      mockAxiosCalls({
        get: {
          [firewallStatusUrl]: Promise.resolve({data: {experimentalFeatures: {firewallAutoUnquarantine: true}}})
        }
      });

      store.dispatch(loadStatus());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_LOAD_STATUS_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful GET call', function() {
      it('dispatches FIREWALL_LOAD_STATUS_FULFILLED action',
          function(done) {
            mockAxiosCalls({
              get: {
                [firewallStatusUrl]: Promise.resolve({data: {experimentalFeatures: {firewallAutoUnquarantine: true}}})
              }
            });

            store.dispatch(loadStatus())
                .then(() => {
                  actions = store.getActions();
                  expect(actions.length).toBe(2);
                  expect(actions[0].type).toBe(FIREWALL_LOAD_STATUS_REQUESTED);
                  expect(actions[0].payload).toBeUndefined();
                  expect(actions[1].type).toBe(FIREWALL_LOAD_STATUS_FULFILLED);
                  expect(actions[1].payload).toEqual({experimentalFeatures: {firewallAutoUnquarantine: true}});
                  done();
                });

            let actions = store.getActions();
            expect(actions.length).toBe(1);
          });
    });

    describe('after a failed GET call', function() {
      it('dispatches an FIREWALL_LOAD_STATUS_FAILED action', function(done) {
        mockAxiosCalls({
          get: {
            [firewallStatusUrl]: () => Promise.reject('error!')
          }
        });

        store.dispatch(loadStatus())
            .then(() => {
              actions = store.getActions();
              expect(actions.length).toBe(2);
              expect(actions[1].type).toBe(FIREWALL_LOAD_STATUS_FAILED);
              expect(actions[1].payload).toBe('error!');
              done();
            });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });

  describe('loadConfiguration', function() {

    afterEach(function() {
      expect(axios.get).toHaveBeenCalledWith(firewallConfigUrl);
    });

    it('immediately dispatches a FIREWALL_LOAD_CONFIGURATION_REQUESTED action', function() {
      mockAxiosCalls({
        get: {
          [firewallConfigUrl]: Promise.resolve({data: {autoUnquarantineEnabled: true}})
        }
      });

      store.dispatch(loadConfiguration());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_LOAD_CONFIGURATION_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful GET call', function() {
      it('dispatches FIREWALL_LOAD_CONFIGURATION_FULFILLED action',
          function(done) {
            mockAxiosCalls({
              get: {
                [firewallConfigUrl]: Promise.resolve({data: {autoUnquarantineEnabled: true}})
              }
            });

            store.dispatch(loadConfiguration())
                .then(() => {
                  actions = store.getActions();
                  expect(actions.length).toBe(2);
                  expect(actions[0].type).toBe(FIREWALL_LOAD_CONFIGURATION_REQUESTED);
                  expect(actions[0].payload).toBeUndefined();
                  expect(actions[1].type).toBe(FIREWALL_LOAD_CONFIGURATION_FULFILLED);
                  expect(actions[1].payload).toEqual({autoUnquarantineEnabled: true});
                  done();
                });

            let actions = store.getActions();
            expect(actions.length).toBe(1);
          });
    });

    describe('after a failed GET call', function() {
      it('dispatches an FIREWALL_LOAD_CONFIGURATION_FAILED action', function(done) {
        mockAxiosCalls({
          get: {
            [firewallConfigUrl]: () => Promise.reject('error!')
          }
        });

        store.dispatch(loadConfiguration())
            .then(() => {
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

  describe('saveConfiguration', function() {

    afterEach(function() {
      expect(axios.put)
          .toHaveBeenCalledWith(firewallConfigUrl, state.firewallConfigurationModal.formState.conditionTypes);
    });

    it('immediately dispatches a FIREWALL_SAVE_CONFIGURATION_REQUESTED action', function() {
      mockAxiosCalls({
        put: {
          [firewallConfigUrl]: Promise.resolve({})
        }
      });

      store.dispatch(saveConfiguration());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_SAVE_CONFIGURATION_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful PUT call', function() {
      it('dispatches FIREWALL_SAVE_CONFIGURATION_FULFILLED action',
          function(done) {
            mockAxiosCalls({
              put: {
                [firewallConfigUrl]: Promise.resolve({})
              }
            });

            store.dispatch(saveConfiguration())
                .then(() => {
                  actions = store.getActions();
                  expect(actions.length).toBe(2);
                  expect(actions[0].type).toBe(FIREWALL_SAVE_CONFIGURATION_REQUESTED);
                  expect(actions[0].payload).toBeUndefined();
                  expect(actions[1].type).toBe(FIREWALL_SAVE_CONFIGURATION_FULFILLED);
                  expect(actions[1].payload).toEqual([
                    {'id': INTEGRITY_RATING_POLICY_TYPE_ID,
                      'name': 'Integrity Rating',
                      'autoReleaseQuarantineEnabled': false}
                  ]);
                  done();
                });

            let actions = store.getActions();
            expect(actions.length).toBe(1);
          });

      it('dispatches FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE after timeout',
          function(done) {
            mockAxiosCalls({
              put: {
                [firewallConfigUrl]: Promise.resolve({})
              }
            });

            store.dispatch(saveConfiguration())
                .then(() => {
                  setTimeout(function() {
                    actions = store.getActions();
                    expect(actions.length).toBe(4);
                    expect(actions[2].type).toBe(FIREWALL_CONFIGURATION_SAVE_MASK_TIMER_DONE);
                    expect(actions[3].type).toBe(FIREWALL_SET_SHOW_CONFIGURATION_MODAL);
                    expect(actions[3].payload).toBe(false);
                    done();
                  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
                });

            let actions = store.getActions();
            expect(actions.length).toBe(1);
          });
    });

    describe('after a failed PUT call', function() {
      it('dispatches an FIREWALL_SAVE_CONFIGURATION_FAILED action', function(done) {
        mockAxiosCalls({
          put: {
            [firewallConfigUrl]: () => Promise.reject('error!')
          }
        });

        store.dispatch(saveConfiguration())
            .then(() => {
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

  describe('openConfigurationModal', function() {
    it('immediately dispatches loadConfiguration and setShowConfigurationModal actions', function() {
      store.dispatch(openConfigurationModal());

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe(FIREWALL_LOAD_CONFIGURATION_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
      expect(actions[1].type).toBe(FIREWALL_SET_SHOW_CONFIGURATION_MODAL);
      expect(actions[1].payload).toEqual(true);
    });
  });

  describe('closeConfigurationModal', function() {
    it('dispatches an setShowConfigurationModal action if serverState and formState is the same',
        function(done) {
          state = {
            firewallConfigurationModal: Object.freeze({
              serverState: Object.freeze({
                autoUnquarantineEnabled: false
              }),
              formState: Object.freeze({
                autoUnquarantineEnabled: false
              })
            })
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

  describe('loadReleaseQuarantineSummary', function() {
    afterEach(function() {
      expect(axios.get).toHaveBeenCalledWith(firewallReleaseQuarantineSummaryUrl);
    });

    it('immediately dispatches a FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED action', function() {
      mockAxiosCalls({
        get: {
          [firewallReleaseQuarantineSummaryUrl]: Promise.resolve({data: {'autoReleaseQuarantineCountMTD': 3}})
        }
      });

      store.dispatch(loadReleaseQuarantineSummary());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful GET call', function() {
      it('dispatches FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED action',
          function(done) {
            mockAxiosCalls({
              get: {
                [firewallReleaseQuarantineSummaryUrl]: Promise.resolve({data: {'autoReleaseQuarantineCountMTD': 3}})
              }
            });

            store.dispatch(loadReleaseQuarantineSummary())
                .then(() => {
                  actions = store.getActions();
                  expect(actions.length).toBe(2);
                  expect(actions[0].type).toBe(FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED);
                  expect(actions[0].payload).toBeUndefined();
                  expect(actions[1].type).toBe(FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED);
                  expect(actions[1].payload).toEqual({'autoReleaseQuarantineCountMTD': 3});
                  done();
                });

            let actions = store.getActions();
            expect(actions.length).toBe(1);
          });
    });

    describe('after a failed GET call', function() {
      it('dispatches an FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED action', function(done) {
        mockAxiosCalls({
          get: {
            [firewallReleaseQuarantineSummaryUrl]: () => Promise.reject('error!')
          }
        });

        store.dispatch(loadReleaseQuarantineSummary())
            .then(() => {
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

  describe('loadReleaseQuarantineList', function() {
    let payload = {'pageCount': 2, 'results': [{'test': 'testVal'}, {'test': 'testVal'}]},
        defaultParams = '?page=1&pageSize=12';

    afterEach(function() {
      expect(axios.get).toHaveBeenCalledWith(firewallReleaseQuarantineListUrl + defaultParams);
    });

    it('immediately dispatches a FIREWALL_RELEASE_QUARANTINE_LIST_REQUESTED action', function() {
      mockAxiosCalls({
        get: {
          [firewallReleaseQuarantineListUrl + defaultParams]: Promise.resolve(payload)
        }
      });

      store.dispatch(loadReleaseQuarantineList());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_RELEASE_QUARANTINE_LIST_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful GET call', function() {
      let defaultParams = '?page=1&pageSize=12';

      it('dispatches FIREWALL_RELEASE_QUARANTINE_LIST_FULFILLED action',
          function(done) {
            mockAxiosCalls({
              get: {
                [firewallReleaseQuarantineListUrl + defaultParams]: Promise.resolve({data: payload})
              }
            });

            store.dispatch(loadReleaseQuarantineList())
                .then(() => {
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

    describe('after a failed GET call', function() {
      let defaultParams = '?page=1&pageSize=12';

      it('dispatches an FIREWALL_RELEASE_QUARANTINE_LIST_FAILED action', function(done) {
        mockAxiosCalls({
          get: {
            [firewallReleaseQuarantineListUrl + defaultParams]: () => Promise.reject('error!')
          }
        });

        store.dispatch(loadReleaseQuarantineList())
            .then(() => {
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

  describe('loadPolicies', function() {
    let payload = {policies: [{'test': 'testVal'}]};

    afterEach(function() {
      expect(axios.get).toHaveBeenCalledWith(policiesUrl);
    });

    it('immediately dispatches a FIREWALL_POLICIES_REQUESTED action', function() {
      mockAxiosCalls({
        get: {
          [policiesUrl]: Promise.resolve(payload)
        }
      });

      store.dispatch(loadPolicies());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_POLICIES_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful GET call', function() {
      it('dispatches FIREWALL_POLICIES_FULFILLED action',
          function(done) {
            mockAxiosCalls({
              get: {
                [policiesUrl]: Promise.resolve({data: payload})
              }
            });

            store.dispatch(loadPolicies())
                .then(() => {
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

    describe('after a failed GET call', function() {
      it('dispatches an FIREWALL_POLICIES_FAILED action', function(done) {
        mockAxiosCalls({
          get: {
            [policiesUrl]: () => Promise.reject('error!')
          }
        });

        store.dispatch(loadPolicies())
            .then(() => {
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

  describe('loadQuarantineSummary', function() {

    afterEach(function() {
      expect(axios.get).toHaveBeenCalledWith(firewallQuarantineSummaryUrl);
    });

    it('immediately dispatches a FIREWALL_QUARANTINE_SUMMARY_REQUESTED action', function() {
      mockAxiosCalls({
        get: {
          [firewallQuarantineSummaryUrl]: Promise.resolve(
              {data: {experimentalFeatures: {firewallAutoUnquarantine: true}}})
        }
      });

      store.dispatch(loadQuarantineSummary());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_QUARANTINE_SUMMARY_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful GET call', function() {
      it('dispatches FIREWALL_QUARANTINE_SUMMARY_FULFILLED action',
          function(done) {
            mockAxiosCalls({
              get: {
                [firewallQuarantineSummaryUrl]: Promise.resolve(
                    {data: {experimentalFeatures: {firewallAutoUnquarantine: true}}})
              }
            });

            store.dispatch(loadQuarantineSummary())
                .then(() => {
                  actions = store.getActions();
                  expect(actions.length).toBe(2);
                  expect(actions[0].type).toBe(FIREWALL_QUARANTINE_SUMMARY_REQUESTED);
                  expect(actions[0].payload).toBeUndefined();
                  expect(actions[1].type).toBe(FIREWALL_QUARANTINE_SUMMARY_FULFILLED);
                  expect(actions[1].payload).toEqual({experimentalFeatures: {firewallAutoUnquarantine: true}});
                  done();
                });

            let actions = store.getActions();
            expect(actions.length).toBe(1);
          });
    });

    describe('after a failed GET call', function() {
      it('dispatches an FIREWALL_QUARANTINE_SUMMARY_FAILED action', function(done) {
        mockAxiosCalls({
          get: {
            [firewallQuarantineSummaryUrl]: () => Promise.reject('error!')
          }
        });

        store.dispatch(loadQuarantineSummary())
            .then(() => {
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

  describe('loadAutoUnquarantineGridData', function() {
    it('immediately dispatches actions to load data for the auto unquarantine grid', function() {
      store.dispatch(loadAutoUnquarantineGridData());

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe(FIREWALL_RELEASE_QUARANTINE_LIST_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
      expect(actions[1].type).toBe(FIREWALL_POLICIES_REQUESTED);
      expect(actions[1].payload).toBeUndefined();
    });
  });

  describe('setAutoUnquarantineGridPage', function() {
    it('immediately dispatches actions to set the current page for the auto unquarantine grid', function() {
      let page = 1;

      store.dispatch(setAutoUnquarantineGridPage(page));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_AUTO_UNQUARANTINE_GRID_SET_PAGE);
      expect(actions[0].payload).toEqual({ 'currentPage': page });
    });
  });

  describe('setAutoUnquarantineGridSorting', function() {
    it('immediately dispatches actions to set the sorting for the auto unquarantine grid', function() {
      let sortField = 'testField',
          sortDir = 'asc';

      store.dispatch(setAutoUnquarantineGridSorting(sortDir, sortField));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_AUTO_UNQUARANTINE_GRID_SET_SORTING);
      expect(actions[0].payload).toEqual({ 'sortDir': sortDir, 'sortField': sortField });
    });
  });

  describe('setAutoUnquarantineGridPolicyFilter', function() {
    it('immediately dispatches actions to set the policy ID filter for the auto unquarantine grid', function() {
      let policyId = 123;

      store.dispatch(setAutoUnquarantineGridPolicyFilter(policyId));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(FIREWALL_AUTO_UNQUARANTINE_GRID_SET_FILTER);
      expect(actions[0].payload).toEqual({ 'policyId': policyId });
    });
  });

  describe('loadData', function() {
    it('immediately dispatches actions to load all firewall data', function() {
      store.dispatch(loadData());

      const actions = store.getActions();
      expect(actions.length).toBe(5);
      expect(actions[0].type).toBe(FIREWALL_LOAD_DATA_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
      expect(actions[1].type).toBe(FIREWALL_LOAD_STATUS_REQUESTED);
      expect(actions[1].payload).toBeUndefined();
      expect(actions[2].type).toBe(FIREWALL_LOAD_CONFIGURATION_REQUESTED);
      expect(actions[2].payload).toBeUndefined();
      expect(actions[3].type).toBe(FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED);
      expect(actions[3].payload).toBeUndefined();
      expect(actions[4].type).toBe(FIREWALL_QUARANTINE_SUMMARY_REQUESTED);
      expect(actions[4].payload).toBeUndefined();
    });
  });
});
