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
  FIREWALL_QUARANTINE_SUMMARY_FAILED,
  FIREWALL_QUARANTINE_SUMMARY_FULFILLED,
  FIREWALL_QUARANTINE_SUMMARY_REQUESTED,
  loadConfiguration,
  loadReleaseQuarantineSummary,
  loadStatus,
  openConfigurationModal,
  saveConfiguration,
  loadQuarantineSummary
} from '../../../main/frontend/firewall/firewallActions';
import {getFirewallConfigurationUrl, getFirewallReleaseQuarantineSummaryUrl, getFirewallStatusUrl,
  getFirewallQuarantineSummaryUrl}
  from '../../../main/frontend/util/CLMLocation';
import {SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS} from '@sonatype/react-shared-components';

describe('firewallActions', function() {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios),
      firewallConfigUrl = getFirewallConfigurationUrl(),
      firewallStatusUrl = getFirewallStatusUrl(),
      firewallReleaseQuarantineSummaryUrl = getFirewallReleaseQuarantineSummaryUrl(),
      firewallQuarantineSummaryUrl = getFirewallQuarantineSummaryUrl();

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
        })
      }),
      firewallConfigurationModal: Object.freeze({
        viewState: Object.freeze({
          sumbitMaskSuccessState: false,
          saveConfigurationError: null,
          loadedConfiguration: false,
          loadConfigurationError: null,
          enabledPolicyConditionTypesCount: 0,
          totalPolicyConditionTypesCount: 1
        }),
        serverState: Object.freeze({
          autoUnquarantineEnabled: false
        }),
        formState: Object.freeze({
          autoUnquarantineEnabled: false
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
      expect(axios.put).toHaveBeenCalledWith(firewallConfigUrl, state.firewallConfigurationModal.formState);
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
                  expect(actions[1].payload).toEqual({autoUnquarantineEnabled: false});
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
});
