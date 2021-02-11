/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  getScmOnboardingConfigUrl,
  getScmOrganizationsUrl,
  getScmDefaultHostUrl,
  getScmRepositoriesUrl,
  getCompositeSourceControlUrl,
  getValidateScmConfigUrl
} from '../../../../main/frontend/util/CLMLocation';

describe('scmOnboardingActions', function() {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios),
      scmOnboardingConfigUrl = getScmOnboardingConfigUrl(),
      validateScmHostUrl = getValidateScmConfigUrl('provider', 'http://host/'),
      scmOnboardingConfigPayload = {
        scmOnboardingFeatureEnabled: true
      },
      scmOrganizationsUrl = getScmOrganizationsUrl(),
      defaultScmHostUrl = getScmDefaultHostUrl('ownerId', 'github');

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

  describe('loadPage', function() {
    const compositeSourceControlUrl = getCompositeSourceControlUrl('organization', 'ownerId'),
        compositeSourceControlPayload = {provider: 'github', token: {value: 'token'}},
        unconfiguredCompositeSourceControlPayload = {provider: undefined, token: undefined},
        scmDefaultHostPayload = {defaultHostUrl: 'http://localhost/'},
        orgResults = [
          {organization: {id: 'id1', name: 'org 1'}, sourceControl: {}},
          {organization: {id: 'id2', name: 'org 2'}, sourceControl: {}},
          {organization: {id: 'id3', name: 'org 3'}, sourceControl: {}},
          {organization: {id: 'id4', name: 'org 4'}, sourceControl: {}}
        ];

    describe('loads data from IQ', () => {

      beforeEach(() => {
        mockAxiosCalls({
          get: {
            [scmOnboardingConfigUrl]: Promise.resolve({data: scmOnboardingConfigPayload}),
            [getCompositeSourceControlUrl('organization', 'id1')]: Promise.resolve(
                {data: compositeSourceControlPayload}),
            [scmOrganizationsUrl]: Promise.resolve({data: orgResults}),
            [getScmDefaultHostUrl('id1', 'github')]: Promise.resolve({data: scmDefaultHostPayload}),
            [getCompositeSourceControlUrl('organization', 'id2')]: Promise.resolve(
                {data: unconfiguredCompositeSourceControlPayload})
          }
        });
      });

      it('always loads the feature flag and org list', () => {
        // when loadPage action is dispatched
        return store.dispatch(scmOnboardingActions.loadPage()).then(() => {

          // then the SCM_ONBOARDING_LOAD_PAGE_REQUESTED action is created with the expected payload
          let actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe('SCM_ONBOARDING_LOAD_PAGE_REQUESTED');
          expect(actions[0].payload).toBeUndefined();

          // and the SCM_ONBOARDING_LOAD_PAGE_FULFILLED action is created with the expected payload
          expect(actions[1].type).toBe('SCM_ONBOARDING_LOAD_PAGE_FULFILLED');
          expect(actions[1].payload.configResults).toEqual(scmOnboardingConfigPayload);
          expect(actions[1].payload.organizationsResults).toEqual(orgResults);
          expect(actions[1].payload.compositeSourceControlResults).toBeNull();
          expect(actions[1].payload.hostUrlResult).toBeNull();
        });
      });

      it('loads the sourceControl and hostUrl config when orgId is given', () => {
        return store.dispatch(scmOnboardingActions.loadPage('id1')).then(() => {

          // then the SCM_ONBOARDING_LOAD_PAGE_REQUESTED action is created with the expected payload
          let actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe('SCM_ONBOARDING_LOAD_PAGE_REQUESTED');
          expect(actions[0].payload).toEqual('id1');

          // and the SCM_ONBOARDING_LOAD_PAGE_FULFILLED action is created with the expected payload
          expect(actions[1].type).toBe('SCM_ONBOARDING_LOAD_PAGE_FULFILLED');
          expect(actions[1].payload.configResults).toEqual(scmOnboardingConfigPayload);
          expect(actions[1].payload.organizationsResults).toEqual(orgResults);
          expect(actions[1].payload.compositeSourceControlResults).toEqual(compositeSourceControlPayload);
          expect(actions[1].payload.hostUrlResult).toEqual(scmDefaultHostPayload);
        });
      });

      it('when organisation has no SCM configuration', () => {
        return store.dispatch(scmOnboardingActions.loadPage('id2')).then(() => {

          // then the SCM_ONBOARDING_LOAD_PAGE_REQUESTED action is created with the expected payload
          let actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe('SCM_ONBOARDING_LOAD_PAGE_REQUESTED');
          expect(actions[0].payload).toEqual('id2');

          // and the SCM_ONBOARDING_LOAD_PAGE_FULFILLED action is created with the expected payload
          expect(actions[1].type).toBe('SCM_ONBOARDING_LOAD_PAGE_FULFILLED');
          expect(actions[1].payload.configResults).toEqual(scmOnboardingConfigPayload);
          expect(actions[1].payload.organizationsResults).toEqual(orgResults);
          expect(actions[1].payload.compositeSourceControlResults).toEqual(unconfiguredCompositeSourceControlPayload);
          expect(actions[1].payload.hostUrlResult).toEqual(null);
        });
      });
    });

    describe('handles errors', function() {
      const organizationsPayload = [{id: 'ownerId'}, {id: undefined}];

      function testFailure(testLabel, responsesSupplier) {
        it(`fails properly when it calls ${testLabel}`, function() {
          mockAxiosCalls(responsesSupplier());

          return store.dispatch(scmOnboardingActions.loadPage('ownerId')).then(() => {

            // then SCM_ONBOARDING_LOAD_PAGE_REQUESTED action is created
            let actions = store.getActions();
            expect(actions.length).toBe(2);
            expect(actions[0].type).toBe('SCM_ONBOARDING_LOAD_PAGE_REQUESTED');
            expect(actions[0].payload).toEqual('ownerId');

            // and SCM_ONBOARDING_LOAD_PAGE_FAILED action is created
            expect(actions[1].type).toBe('SCM_ONBOARDING_LOAD_PAGE_FAILED');
            expect(actions[1].payload).toEqual('failed call');
          });
        });
      }

      testFailure('organizationsUrl', () => {
        return {
          get: {
            [scmOnboardingConfigUrl]: Promise.resolve({data: scmOnboardingConfigPayload}),
            [compositeSourceControlUrl]: Promise.resolve({data: compositeSourceControlPayload}),
            [scmOrganizationsUrl]: Promise.reject('failed call'),
            [defaultScmHostUrl]: Promise.resolve({data: scmDefaultHostPayload})
          }
        };
      });
      testFailure('scmOnboardingConfig', () => {
        return {
          get: {
            [scmOnboardingConfigUrl]: Promise.reject('failed call'),
            [compositeSourceControlUrl]: Promise.resolve({data: compositeSourceControlPayload}),
            [scmOrganizationsUrl]: Promise.resolve({data: organizationsPayload}),
            [defaultScmHostUrl]: Promise.resolve({data: scmDefaultHostPayload})
          }
        };
      });
      testFailure('compositeSourceControlUrl', () => {
        return {
          get: {
            [scmOnboardingConfigUrl]: Promise.resolve({data: scmOnboardingConfigPayload}),
            [compositeSourceControlUrl]: Promise.reject('failed call'),
            [scmOrganizationsUrl]: Promise.resolve({data: organizationsPayload}),
            [defaultScmHostUrl]: Promise.resolve({data: scmDefaultHostPayload})
          }
        };
      });
      testFailure('defaultScmHostUrl', () => {
        return {
          get: {
            [scmOnboardingConfigUrl]: Promise.resolve({data: scmOnboardingConfigPayload}),
            [compositeSourceControlUrl]: Promise.resolve({data: compositeSourceControlPayload}),
            [scmOrganizationsUrl]: Promise.resolve({data: organizationsPayload}),
            [defaultScmHostUrl]: Promise.reject('failed call')
          }
        };
      });
    });
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
          [getScmRepositoriesUrl()]: Promise.resolve([{httpCloneUrl: 'http://localhost/my/repo.git', isPrivate: true}])
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

  describe('setCurrentHostUrl', function() {
    it('dispatches an event', function() {
      store = SpecUtil.mockReduxStore(state);

      let hostUrlValue = 'https://localhost';
      store.dispatch(scmOnboardingActions.setCurrentHostUrl(hostUrlValue));
      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0]).toEqual({type: 'SCM_ONBOARDING_SET_CURRENT_HOST_URL', payload: hostUrlValue});
    });
  });

  describe('setSelectedOrganization', function() {
    const prevOrg = {
      organization: {id: 'prevId'},
      sourceControl: {token: {value: null}}
    };

    it('dispatches an org change', function() {
      store = SpecUtil.mockReduxStore(state);
      const selectedOrg = {
        organization: {id: 'id1'},
        sourceControl: {token: {value: null}}
      };

      // no axios calls
      expect(store.dispatch(scmOnboardingActions.setSelectedOrganization(selectedOrg, false, prevOrg))).toBeUndefined();

      const actions = store.getActions();
      expect(actions).toEqual([
        {type: 'SCM_ONBOARDING_SET_TARGET_ORGANIZATION', payload: selectedOrg}
      ]);
    });

    it('dispatches loadRepositoriesRequested when the new token is overridden', function() {
      store = SpecUtil.mockReduxStore(state);
      const selectedOrg = {
        organization: {id: 'id1'},
        sourceControl: {token: {value: null}}
      };

      // triggers an attempt to get new default host URL
      expect(store.dispatch(scmOnboardingActions.setSelectedOrganization(selectedOrg, true, prevOrg))).toBeDefined();

      const actions = store.getActions();
      expect(actions).toEqual([
        {type: 'SCM_ONBOARDING_SET_TARGET_ORGANIZATION', payload: selectedOrg}
      ]);
    });

    it('dispatches loadRepositoriesRequested when the old token was overridden', function() {
      store = SpecUtil.mockReduxStore(state);
      const selectedOrg = {
        organization: {id: 'id1'},
        sourceControl: {token: {value: 'redacted'}}
      };

      // attempts to check if default host URL changed
      expect(store.dispatch(scmOnboardingActions.setSelectedOrganization(selectedOrg, false, prevOrg))).toBeDefined();

      const actions = store.getActions();
      expect(actions).toEqual([
        {type: 'SCM_ONBOARDING_SET_TARGET_ORGANIZATION', payload: selectedOrg}
      ]);
    });
    it('dispatches loadRepositoriesRequested when the both the new & old tokens are overridden', function() {
      store = SpecUtil.mockReduxStore(state);
      const selectedOrg = {
        organization: {id: 'id1'},
        sourceControl: {token: {value: 'redacted'}}
      };

      store.dispatch(scmOnboardingActions.setSelectedOrganization(selectedOrg, true, prevOrg));
      const actions = store.getActions();
      expect(actions).toEqual([
        {type: 'SCM_ONBOARDING_SET_TARGET_ORGANIZATION', payload: selectedOrg}
      ]);
    });

    it('dispatches loadRepositories and all further actions when the token is overridden', function(done) {
      const scmDefaultHostPayload = {defaultHostUrl: 'http://localhost/'};
      mockAxiosCalls({
        get: {
          [getScmDefaultHostUrl('id1', 'github')]: Promise.resolve({data: scmDefaultHostPayload})
        }
      });
      store = SpecUtil.mockReduxStore(state);
      const selectedOrg = {
        organization: {id: 'id1'},
        sourceControl: {token: {value: 'redacted'}, provider: 'github'}
      };

      store.dispatch(scmOnboardingActions.setSelectedOrganization(selectedOrg, true, prevOrg))
          .then(() => {
            let actions = store.getActions();
            expect(actions.map(a => a.type)).toEqual([
              'SCM_ONBOARDING_SET_TARGET_ORGANIZATION',
              'SCM_ONBOARDING_SET_CURRENT_HOST_URL',
              'SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED',
              'SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED' // didn't mock the load repo endpoint
            ]);
            done();
          });
    });

    it('dispatches loadRepositories when prev org is undefined', function(done) {
      const scmDefaultHostPayload = {defaultHostUrl: 'http://localhost/'};
      mockAxiosCalls({
        get: {
          [getScmDefaultHostUrl('id1', 'github')]: Promise.resolve({data: scmDefaultHostPayload})
        }
      });
      store = SpecUtil.mockReduxStore(state);
      const selectedOrg = {
        organization: {id: 'id1'},
        sourceControl: {token: {value: null}, provider: 'github'}
      };

      store.dispatch(scmOnboardingActions.setSelectedOrganization(selectedOrg, false, undefined))
          .then(() => {
            let actions = store.getActions();
            expect(actions.map(a => a.type)).toEqual([
              'SCM_ONBOARDING_SET_TARGET_ORGANIZATION',
              'SCM_ONBOARDING_SET_CURRENT_HOST_URL',
              'SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED',
              'SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED' // didn't mock the load repo endpoint
            ]);
            done();
          });
    });

    it('does not dispatch loadRepositories when token is unchanged', function() {
      store = SpecUtil.mockReduxStore(state);
      const selectedOrg = {
        organization: {id: 'id1'},
        sourceControl: {token: {value: null}, provider: 'github'}
      };

      // undefined because it does not make any axios calls
      expect(store.dispatch(scmOnboardingActions.setSelectedOrganization(selectedOrg, false, prevOrg))).toBeUndefined();

      const actions = store.getActions();
      expect(actions).toEqual([
        {type: 'SCM_ONBOARDING_SET_TARGET_ORGANIZATION', payload: selectedOrg}
      ]);

    });
  });

  describe('validateScmHostUrl', function() {

    const validateScmHostUrlPayload = {
      scmUrlIsValid: false,
      scmUrlErrorMessage: 'BOOM'
    };

    // Mock clock to test debounce
    beforeAll(() => jasmine.clock().install());
    afterAll(() => jasmine.clock().uninstall());

    afterEach(function() {
      expect(axios.get).toHaveBeenCalledWith(validateScmHostUrl);
    });

    it('dispatches a SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_REQUESTED action', function() {
      mockAxiosCalls({
        get: {
          [validateScmHostUrl]: Promise.resolve(validateScmHostUrlPayload)
        }
      });

      jasmine.clock().mockDate();

      store.dispatch(scmOnboardingActions.validateScmHostUrl('provider', 'http://host/'));

      // dispatches no actions until debounce timeout has passed
      expect(store.getActions().length).toBe(0);

      // turn forward the time
      jasmine.clock().mockDate(new Date.now() + 3000);
      jasmine.clock().tick(3000);

      // after the debounce timeout the request is dispatched
      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after successful GET call', function() {

      beforeEach(function() {
        mockAxiosCalls({
          get: {
            [validateScmHostUrl]: Promise.resolve(validateScmHostUrlPayload)
          }
        });
      });

      it('dispatches SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FULFILLED', function(done) {

        store.dispatch(scmOnboardingActions.validateScmHostUrl('provider', 'http://host/'))
            .then(() => {
              actions = store.getActions();
              expect(actions.length).toBe(1);
              expect(actions[0].type).toBe('SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_REQUESTED');
              done();
            })
            .then(() => {
              actions = store.getActions();
              expect(actions.length).toBe(2);
              expect(actions[1].type).toBe('SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FULFILLED');
              done();
            });

        let actions = store.getActions();
        expect(actions.length).toBe(0);

        // turn time forwards to allow request to continue
        jasmine.clock().mockDate(new Date.now() + 3000);
        jasmine.clock().tick(3000);
      });
    });
  });
});
