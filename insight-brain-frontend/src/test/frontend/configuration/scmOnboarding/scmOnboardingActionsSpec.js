/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {getManifestScanConfigUrl} from '../../../../main/frontend/util/CLMLocation';
import {load} from '../../../../main/frontend/configuration/scmOnboarding/scmOnboardingActions';

describe('scmOnboardingActions', function() {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios),
      manifestScanConfigUrl = getManifestScanConfigUrl(),
      serverData = {
        manifestScanFeatureEnabled: true
      };

  let store, state;

  beforeEach(function() {
    state = {
      manifestScanFeatureEnabled: false
    };
    store = SpecUtil.mockReduxStore(state);
  });

  describe('load', function() {

    afterEach(function() {
      expect(axios.get).toHaveBeenCalledWith(manifestScanConfigUrl);
    });

    it('dispatches a SCM_ONBOARDING_LOAD_REQUESTED action', function() {
      mockAxiosCalls({
        get: {
          [manifestScanConfigUrl]: Promise.resolve(serverData)
        }
      });

      store.dispatch(load());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('SCM_ONBOARDING_LOAD_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after successful GET call', function() {

      beforeEach(function() {
        mockAxiosCalls({
          get: {
            [manifestScanConfigUrl]: Promise.resolve(serverData)
          }
        });
      });

      it('dispatches SCM_ONBOARDING_LOAD_FULFILLED', function(done) {

        store.dispatch(load())
            .then(() => {
              actions = store.getActions();
              expect(actions.length).toBe(2);
              expect(actions[1].type).toBe('SCM_ONBOARDING_LOAD_FULFILLED');
              done();
            });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe('SCM_ONBOARDING_LOAD_REQUESTED');
      });
    });
  });
});
