/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  getManifestScanConfigUrl,
  getOrganizationsUrl,
  getScmRepositoriesUrl
} from '../../../../main/frontend/util/CLMLocation';
import {
  loadConfig,
  loadOrganizations,
  loadRepositories
} from '../../../../main/frontend/configuration/scmOnboarding/scmOnboardingActions';

describe('scmOnboardingActions', function() {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios),
      manifestScanConfigUrl = getManifestScanConfigUrl(),
      manifestScanConfigPayload = {
        manifestScanFeatureEnabled: true
      },
      organizationsUrl = getOrganizationsUrl(),
      organizationsPayload = [{
        id: 'id',
        name: 'name'}
      ];

  let store, state;

  beforeEach(function() {
    state = {
      manifestScanFeatureEnabled: false
    };
    store = SpecUtil.mockReduxStore(state);
  });

  describe('loadConfig', function() {

    afterEach(function() {
      expect(axios.get).toHaveBeenCalledWith(manifestScanConfigUrl);
    });

    it('dispatches a SCM_ONBOARDING_LOAD_CONFIG_REQUESTED action', function() {
      mockAxiosCalls({
        get: {
          [manifestScanConfigUrl]: Promise.resolve(manifestScanConfigPayload)
        }
      });

      store.dispatch(loadConfig());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('SCM_ONBOARDING_LOAD_CONFIG_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after successful GET call', function() {

      beforeEach(function() {
        mockAxiosCalls({
          get: {
            [manifestScanConfigUrl]: Promise.resolve(manifestScanConfigPayload)
          }
        });
      });

      it('dispatches SCM_ONBOARDING_LOAD_CONFIG_FULFILLED', function(done) {

        store.dispatch(loadConfig())
            .then(() => {
              actions = store.getActions();
              expect(actions.length).toBe(2);
              expect(actions[1].type).toBe('SCM_ONBOARDING_LOAD_CONFIG_FULFILLED');
              done();
            });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe('SCM_ONBOARDING_LOAD_CONFIG_REQUESTED');
      });
    });
  });

  describe('loadOrganizations', function() {

    afterEach(function() {
      expect(axios.get).toHaveBeenCalledWith(organizationsUrl);
    });

    it('dispatches a SCM_ONBOARDING_LOAD_ORGANIZATIONS_REQUESTED action', function() {
      mockAxiosCalls({
        get: {
          [organizationsUrl]: Promise.resolve(organizationsPayload)
        }
      });

      store.dispatch(loadOrganizations());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('SCM_ONBOARDING_LOAD_ORGANIZATIONS_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after successful GET call', function() {

      beforeEach(function() {
        mockAxiosCalls({
          get: {
            [organizationsUrl]: Promise.resolve(organizationsPayload)
          }
        });
      });

      it('dispatches SCM_ONBOARDING_LOAD_ORGANIZATIONS_FULFILLED', function(done) {

        store.dispatch(loadOrganizations())
            .then(() => {
              actions = store.getActions();
              expect(actions.length).toBe(2);
              expect(actions[1].type).toBe('SCM_ONBOARDING_LOAD_ORGANIZATIONS_FULFILLED');
              done();
            });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe('SCM_ONBOARDING_LOAD_ORGANIZATIONS_REQUESTED');
      });
    });
  });

  describe('load repositories', function() {
    it('requests a load of repositories', function(done) {
      mockAxiosCalls({
        get: {
          [getScmRepositoriesUrl()]: Promise.resolve([{httpCloneUrl: 'http://github.com/my/repo.git', isPrivate: true}])
        }
      });

      store = SpecUtil.mockReduxStore(state);

      store.dispatch(loadRepositories())
          .then(() => {
            expect(axios.get).toHaveBeenCalledWith(getScmRepositoriesUrl());

            done();
          });
      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0]).toEqual({type: 'SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED'});
    });

    it('handles errors', function(done) {
      mockAxiosCalls({
        get: {
          [getScmRepositoriesUrl()]: Promise.reject('Failed request')
        }
      });

      store = SpecUtil.mockReduxStore(state);

      store.dispatch(loadRepositories())
          .then(() => {
            expect(axios.get).toHaveBeenCalledWith(getScmRepositoriesUrl());
            expect(store.getActions().length).toBe(2);
            expect(store.getActions()[1].type).toBe('SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED');
            expect(store.getActions()[1].payload).toBe('Failed request');
            done();
          });
      expect(store.getActions().length).toBe(1);
      expect(axios.get).toHaveBeenCalledWith(getScmRepositoriesUrl());
    });
  });
});
