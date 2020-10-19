/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  getScmOnboardingConfigUrl,
  getOrganizationsUrl,
  getScmDefaultHostUrl,
  getScmRepositoriesUrl
} from '../../../../main/frontend/util/CLMLocation';

describe('scmOnboardingActions', function() {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios),
      manifestScanConfigUrl = getScmOnboardingConfigUrl(),
      scmOnboardingConfigPayload = {
        scmOnboardingFeatureEnabled: true
      },
      organizationsUrl = getOrganizationsUrl(),
      organizationsPayload = [{
        id: 'id',
        name: 'name'}
      ];

  let store, state, scmOnboardingActions;

  beforeEach(function() {
    state = {
      scmOnboardingFeatureEnabled: false
    };
    store = SpecUtil.mockReduxStore(state);
  });

  beforeEach(angular.mock.module('configurationModule'));

  beforeEach(inject(function(_scmOnboardingActions_) {
    scmOnboardingActions = _scmOnboardingActions_;
  }));

  describe('loadConfig', function() {

    afterEach(function() {
      expect(axios.get).toHaveBeenCalledWith(manifestScanConfigUrl);
    });

    it('dispatches a SCM_ONBOARDING_LOAD_CONFIG_REQUESTED action', function() {
      mockAxiosCalls({
        get: {
          [manifestScanConfigUrl]: Promise.resolve(scmOnboardingConfigPayload)
        }
      });

      store.dispatch(scmOnboardingActions.loadConfig());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('SCM_ONBOARDING_LOAD_CONFIG_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after successful GET call', function() {

      beforeEach(function() {
        mockAxiosCalls({
          get: {
            [manifestScanConfigUrl]: Promise.resolve(scmOnboardingConfigPayload)
          }
        });
      });

      it('dispatches SCM_ONBOARDING_LOAD_CONFIG_FULFILLED', function(done) {

        store.dispatch(scmOnboardingActions.loadConfig())
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
    var testdata = [{organizationId: 'organizationId'}, {organizationId: undefined}];

    afterEach(function() {
      expect(axios.get).toHaveBeenCalledWith(organizationsUrl);
    });

    for (let i in testdata) {
      it('dispatches a SCM_ONBOARDING_LOAD_ORGANIZATIONS_REQUESTED(' + testdata[i].organizationId + ') action',
          function() {
            mockAxiosCalls({
              get: {
                [organizationsUrl]: Promise.resolve(organizationsPayload)
              }
            });

            store.dispatch(scmOnboardingActions.loadOrganizations(testdata[i].organizationId));

            const actions = store.getActions();
            expect(actions.length).toBe(1);
            expect(actions[0].type).toBe('SCM_ONBOARDING_LOAD_ORGANIZATIONS_REQUESTED');
            expect(actions[0].payload).toBe(testdata[i].organizationId);
          });

      describe('after successful GET call (' + testdata[i].organizationId + ')', function() {

        beforeEach(function() {
          mockAxiosCalls({
            get: {
              [organizationsUrl]: Promise.resolve(organizationsPayload)
            }
          });
        });

        it('dispatches SCM_ONBOARDING_LOAD_ORGANIZATIONS_FULFILLED(' + testdata[i].organizationId + ')', done => {

          store.dispatch(scmOnboardingActions.loadOrganizations(testdata[i].organizationId))
              .then(() => {
                actions = store.getActions();
                expect(actions.length).toBe(2);
                expect(actions[1].type).toBe('SCM_ONBOARDING_LOAD_ORGANIZATIONS_FULFILLED');
                done();
              });

          let actions = store.getActions();
          expect(actions.length).toBe(1);
          expect(actions[0].type).toBe('SCM_ONBOARDING_LOAD_ORGANIZATIONS_REQUESTED');
          expect(actions[0].payload).toBe(testdata[i].organizationId);
        });
      });
    }
  });

  describe('onRepositorySelectionChanged', function() {
    it('dispatches an event', function() {
      store = SpecUtil.mockReduxStore(state);

      let repo = {isSelected: true};
      // TODO flesh this out in INT-3479. Not sure if we even want this to be an action at all
      store.dispatch(scmOnboardingActions.onRepositorySelectionChanged(repo));
      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toEqual('SCM_ONBOARDING_REPOSITORY_SELECTION_CHANGED');
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

      store.dispatch(scmOnboardingActions.loadRepositories())
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

      store.dispatch(scmOnboardingActions.loadRepositories())
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

  describe('loadOrgHostUrl', function() {
    it('requests a default host URL', function(done) {
      mockAxiosCalls({
        get: {
          [getScmDefaultHostUrl('orgId', 'github')]: Promise.resolve({defaultHostUrl: 'http://github.com/'})
        }
      });

      store = SpecUtil.mockReduxStore(state);

      store.dispatch(scmOnboardingActions.loadOrgHostUrl('orgId', 'github'))
          .then(() => {
            expect(axios.get).toHaveBeenCalledWith(getScmDefaultHostUrl('orgId', 'github'));
            done();
          });
      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0]).toEqual({type: 'SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_REQUESTED'});
    });

    it('handles errors', function(done) {
      mockAxiosCalls({
        get: {
          [getScmDefaultHostUrl('orgId', 'github')]: Promise.reject('Failed request')
        }
      });

      store = SpecUtil.mockReduxStore(state);

      store.dispatch(scmOnboardingActions.loadOrgHostUrl('orgId', 'github'))
          .then(() => {
            expect(axios.get).toHaveBeenCalledWith(getScmDefaultHostUrl('orgId', 'github'));
            expect(store.getActions().length).toBe(2);
            expect(store.getActions()[1].type).toBe('SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_FAILED');
            expect(store.getActions()[1].payload).toBe('Failed request');
            done();
          });
      expect(store.getActions().length).toBe(1);
      expect(axios.get).toHaveBeenCalledWith(getScmDefaultHostUrl('orgId', 'github'));
      expect(store.getActions()[0]).toEqual({type: 'SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_REQUESTED'});
    });
  });
});
