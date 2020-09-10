/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../../main/frontend/configuration/scmOnboarding/scmOnboardingReducer';

describe('scmOnboardingReducer', function() {
  let otherObject;

  beforeEach(function() {
    otherObject = {value: 'test value'};
  });

  describe('unknown action', function() {
    it('returns original state', function() {
      const state = Object.freeze({foo: 'bar'});
      const action = {
        type: 'UNKNOWN'
      };
      const newState = reduce(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('SCM_ONBOARDING_LOAD_FULFILLED action', function() {
    it('populates state from configuration', function() {
      // given SCM configuration from IQ server
      const state = Object.freeze({
        other: otherObject,
        manifestScanFeatureEnabled: false
      });

      // when reduce is invoked
      const newState = reduce(state, {
        type: 'SCM_ONBOARDING_LOAD_FULFILLED',
        payload: {
          manifestScanFeatureEnabled: true
        }
      });

      // then state is updated
      expect(newState.isManifestScanFeatureEnabled).toBe(true);

      // and other properties are not modified
      expect(newState.other).toBe(otherObject);
    });
  });
});
