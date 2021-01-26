/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import {loadStatus} from '../../../main/frontend/firewall/firewallActions';
import {getFirewallConfigUrl} from '../../../main/frontend/util/CLMLocation';

describe('firewallActions', function() {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios),
      firewallConfigUrl = getFirewallConfigUrl();

  describe('loadStatus', function() {
    let store, state;

    beforeEach(function() {
      state = {
        firewall: {
          viewState: {
            loadingStatus: true,
            loadStatusError: null
          },
          configurationState: {
            isEnabled: false
          }
        }
      };

      store = SpecUtil.mockReduxStore(state);
    });

    afterEach(function() {
      expect(axios.get).toHaveBeenCalledWith(firewallConfigUrl);
    });

    it('immediately dispatches a FIREWALL_LOAD_STATUS_REQUESTED action', function() {
      mockAxiosCalls({
        get: {
          [firewallConfigUrl]: Promise.resolve({data: {experimentalFeatures: {firewallAutoUnquarantine: true}}})
        }
      });

      store.dispatch(loadStatus());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('FIREWALL_LOAD_STATUS_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after a successful GET call', function() {
      it('dispatches FIREWALL_LOAD_STATUS_FULFILLED action',
          function(done) {
            mockAxiosCalls({
              get: {
                [firewallConfigUrl]: Promise.resolve({data: {experimentalFeatures: {firewallAutoUnquarantine: true}}})
              }
            });

            store.dispatch(loadStatus())
                .then(() => {
                  actions = store.getActions();
                  expect(actions.length).toBe(2);
                  expect(actions[0].type).toBe('FIREWALL_LOAD_STATUS_REQUESTED');
                  expect(actions[0].payload).toBeUndefined();
                  expect(actions[1].type).toBe('FIREWALL_LOAD_STATUS_FULFILLED');
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
            [firewallConfigUrl]: Promise.reject('error!')
          }
        });

        store.dispatch(loadStatus())
            .then(() => {
              actions = store.getActions();
              expect(actions.length).toBe(2);
              expect(actions[1].type).toBe('FIREWALL_LOAD_STATUS_FAILED');
              expect(actions[1].payload).toBe('error!');
              done();
            });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
      });
    });
  });
});
