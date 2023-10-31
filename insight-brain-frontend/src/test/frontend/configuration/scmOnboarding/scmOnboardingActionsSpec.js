/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  getScmOrganizationsUrl,
  getScmDefaultHostUrl,
  getScmRepositoriesUrl,
  getCompositeSourceControlUrl,
  getValidateScmConfigUrl,
  getOwnerListUrl,
  getPermissionContextTestUrl,
  getRepositoriesUrl,
  getProductFeaturesUrl,
} from 'MainRoot/util/CLMLocation';
import {
  SCM_ONBOARDING_CHECK_PERMISSIONS_REQUESTED,
  SCM_ONBOARDING_CHECK_PERMISSIONS_FULFILLED,
  SCM_ONBOARDING_LOAD_PAGE_FAILED,
  SCM_ONBOARDING_LOAD_PAGE_FULFILLED,
  SCM_ONBOARDING_LOAD_PAGE_REQUESTED,
  SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED,
  SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FAILED,
  SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED,
  SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED,
} from 'MainRoot/configuration/scmOnboarding/scmOnboardingActions';
import { authErrorMessage, featureNotEnableErrorMessage } from 'MainRoot/util/authorizationUtil';
import { getOwnersMap } from 'TestRoot/OrgsAndPolicies/ownerSideNav/nLevelMockData';

describe('scmOnboardingActions', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios),
    validateScmHostUrl = getValidateScmConfigUrl('provider', 'http://host/'),
    scmOrganizationsUrl = getScmOrganizationsUrl(),
    defaultScmHostUrl = getScmDefaultHostUrl('ownerId', 'github');

  let store, state, scmOnboardingActions;

  beforeEach(function () {
    store = SpecUtil.mockReduxStore();
  });

  beforeEach(angular.mock.module('configurationModule'));

  beforeEach(inject(function (_scmOnboardingActions_) {
    scmOnboardingActions = _scmOnboardingActions_;
  }));

  describe('loadPage', function () {
    const compositeSourceControlUrl = getCompositeSourceControlUrl('organization', 'ownerId'),
      compositeSourceControlPayload = {
        provider: { value: null, parentValue: 'github', parentName: 'root org' },
        token: { value: null, parentValue: 'token', parentName: 'root org' },
      },
      unconfiguredCompositeSourceControlPayload = { provider: { value: null, parentValue: null }, token: undefined },
      providerOverriddenCompositeSourceControlPayload = {
        provider: { value: 'gitlab', parentValue: 'github', parentName: 'root org' },
        token: { value: 'token', parentValue: 'root token', parentName: 'root org' },
      },
      scmDefaultHostPayload = { defaultHostUrl: 'http://localhost/' },
      gitlabDefaultHostPayload = { defaultHostUrl: 'http://localhost:1234/' },
      orgResults = [
        { organization: { id: 'id1', name: 'org 1' }, sourceControl: {} },
        { organization: { id: 'id2', name: 'org 2' }, sourceControl: {} },
        { organization: { id: 'id3', name: 'org 3' }, sourceControl: {} },
        { organization: { id: 'id4', name: 'org 4' }, sourceControl: {} },
      ];
    const repositoriesList = {
      repositories: [
        {
          oldestEvalTimestamp: null,
          managerInstanceId: '54342D8A-8FBE62A7-98C2B285-C37C2DC0-5FDBFC12',
          repository: {
            id: 'c192fc00375948dfbe1e8702ef6a3e44',
            repositoryManagerId: '17ee89ffd86649c49ce32f7d0328072a',
            publicId: 'maven-central',
            enabled: true,
            quarantineEnabled: false,
            format: 'maven2',
          },
        },
        {
          oldestEvalTimestamp: null,
          managerInstanceId: '54342D8A-8FBE62A7-98C2B285-C37C2DC0-5FDBFC12',
          repository: {
            id: 'c192fc00375948dfbe1e8702ef6a3e45',
            repositoryManagerId: '17ee89ffd86649c49ce32f7d0328072a',
            publicId: 'maven-central',
            enabled: true,
            quarantineEnabled: false,
            format: 'maven2',
          },
        },
        {
          oldestEvalTimestamp: null,
          managerInstanceId: '54342D8A-8FBE62A7-98C2B285-C37C2DC0-5FDBFC12',
          repository: {
            id: 'c192fc00375948dfbe1e8702ef6a3e46',
            repositoryManagerId: '17ee89ffd86649c49ce32f7d0328072a',
            publicId: 'maven-central',
            enabled: true,
            quarantineEnabled: false,
            format: 'maven2',
          },
        },
      ],
    };
    const organizationsDepth = 4;
    const ownersMap = getOwnersMap(organizationsDepth);
    const topParentOrganizationId = 'ROOT_ORGANIZATION_ID';
    const ownerListPayload = { topParentOrganizationId, ownersMap };

    describe('loads data from IQ', () => {
      beforeEach(() => {
        mockAxiosCalls({
          get: {
            [getRepositoriesUrl()]: Promise.resolve({
              data: repositoriesList,
            }),
            [getOwnerListUrl()]: Promise.resolve({
              data: ownerListPayload,
            }),
            [getCompositeSourceControlUrl('organization', 'id1')]: Promise.resolve({
              data: compositeSourceControlPayload,
            }),
            [scmOrganizationsUrl]: Promise.resolve({ data: orgResults }),
            [getScmDefaultHostUrl('id1', 'github')]: Promise.resolve({
              data: scmDefaultHostPayload,
            }),
            [getScmDefaultHostUrl('provider-org', 'gitlab')]: Promise.resolve({
              data: gitlabDefaultHostPayload,
            }),
            [getCompositeSourceControlUrl('organization', 'id2')]: Promise.resolve({
              data: unconfiguredCompositeSourceControlPayload,
            }),
            [getCompositeSourceControlUrl('organization', 'provider-org')]: Promise.resolve({
              data: providerOverriddenCompositeSourceControlPayload,
            }),
            [getProductFeaturesUrl()]: Promise.resolve({
              data: ['automation'],
            }),
          },
          put: {
            [getPermissionContextTestUrl('repository_container')]: Promise.resolve({
              data: [],
            }),
            [getPermissionContextTestUrl('global', 'global')]: Promise.resolve({
              data: ['ADD_APPLICATION'],
            }),
            [getPermissionContextTestUrl('organization', 'id1')]: Promise.resolve({
              data: ['ADD_APPLICATION'],
            }),
            [getPermissionContextTestUrl('organization', 'id2')]: Promise.resolve({
              data: ['ADD_APPLICATION'],
            }),
            [getPermissionContextTestUrl('organization', 'provider-org')]: Promise.resolve({
              data: ['ADD_APPLICATION'],
            }),
            [getPermissionContextTestUrl('organization', 'ownerId')]: Promise.resolve({
              data: ['ADD_APPLICATION'],
            }),
          },
        });
      });

      beforeEach(function () {
        store = SpecUtil.mockReduxStore({
          scmOnboarding: {
            formState: {
              selectedOrganization: null,
            },
          },
          router: {
            currentState: {
              name: 'scmOnboardingOrg',
            },
            currentParams: {
              organizationId: 'ROOT_ORGANIZATION',
            },
            prevState: {
              name: null,
            },
          },
        });
      });

      it('always loads the feature flag and org list', () => {
        // when loadPage action is dispatched
        return store.dispatch(scmOnboardingActions.loadPage()).then(() => {
          // then the SCM_ONBOARDING_LOAD_PAGE_REQUESTED action is created with the expected payload
          let actions = store.getActions();
          expect(actions.length).toBe(10);
          expect(actions[0].type).toBe(SCM_ONBOARDING_CHECK_PERMISSIONS_REQUESTED);
          expect(actions[0].payload).toBeUndefined();
          expect(actions[1].type).toBe(SCM_ONBOARDING_CHECK_PERMISSIONS_FULFILLED);
          expect(actions[1].payload).toBeUndefined();
          expect(actions[2].type).toBe(SCM_ONBOARDING_LOAD_PAGE_REQUESTED);
          expect(actions[2].payload).toBeUndefined();

          // and the SCM_ONBOARDING_LOAD_PAGE_FULFILLED action is created with the expected payload
          expect(actions[9].type).toBe(SCM_ONBOARDING_LOAD_PAGE_FULFILLED);
          expect(actions[9].payload.organizationsResults).toEqual(orgResults);
          expect(actions[9].payload.compositeSourceControlResults).toBeNull();
          expect(actions[9].payload.hostUrlResult).toBeNull();
        });
      });

      it('loads the sourceControl and hostUrl config when orgId is given', () => {
        return store.dispatch(scmOnboardingActions.loadPage('id1')).then(() => {
          // then the SCM_ONBOARDING_LOAD_PAGE_REQUESTED action is created with the expected payload
          let actions = store.getActions();
          expect(actions.map((a) => a.type)).toEqual([
            SCM_ONBOARDING_CHECK_PERMISSIONS_REQUESTED,
            SCM_ONBOARDING_CHECK_PERMISSIONS_FULFILLED,
            SCM_ONBOARDING_LOAD_PAGE_REQUESTED,
            'ownerSideNav/load/pending',
            'ownerSideNav/loadOwnerList/pending',
            'repositories/loadRepositories/pending',
            'ownerSideNav/loadOwnerList/fulfilled',
            'repositories/loadRepositories/fulfilled',
            'ownerSideNav/load/fulfilled',
            // the call to load repos fails in our loadPage promise, falling into the loadPage
            // catch block - we didn't stub out the calls to load repos
            SCM_ONBOARDING_LOAD_PAGE_FULFILLED,
            SCM_ONBOARDING_LOAD_PAGE_FAILED,
          ]);

          expect(actions[0].type).toBe(SCM_ONBOARDING_CHECK_PERMISSIONS_REQUESTED);

          // and the SCM_ONBOARDING_LOAD_PAGE_FULFILLED action is created with the expected payload
          expect(actions[9].type).toBe('SCM_ONBOARDING_LOAD_PAGE_FULFILLED');
          expect(actions[9].payload.organizationsResults).toEqual(orgResults);
          expect(actions[9].payload.compositeSourceControlResults).toEqual(compositeSourceControlPayload);
          expect(actions[9].payload.hostUrlResult).toEqual(scmDefaultHostPayload);
        });
      });

      it('when organization has no SCM configuration', () => {
        return store.dispatch(scmOnboardingActions.loadPage('id2')).then(() => {
          // then the SCM_ONBOARDING_LOAD_PAGE_REQUESTED action is created with the expected payload
          let actions = store.getActions();
          expect(actions.map((a) => a.type)).toEqual([
            SCM_ONBOARDING_CHECK_PERMISSIONS_REQUESTED,
            SCM_ONBOARDING_CHECK_PERMISSIONS_FULFILLED,
            SCM_ONBOARDING_LOAD_PAGE_REQUESTED,
            'ownerSideNav/load/pending',
            'ownerSideNav/loadOwnerList/pending',
            'repositories/loadRepositories/pending',
            'ownerSideNav/loadOwnerList/fulfilled',
            'repositories/loadRepositories/fulfilled',
            'ownerSideNav/load/fulfilled',
            SCM_ONBOARDING_LOAD_PAGE_FULFILLED,
            // the call to load repos fails in our loadPage promise, falling into the loadPage
            // catch block - we didn't stub out the calls to load repos
            SCM_ONBOARDING_LOAD_PAGE_FAILED,
          ]);
          expect(actions[2].payload).toEqual('id2');

          // and the SCM_ONBOARDING_LOAD_PAGE_FULFILLED action is created with the expected payload
          expect(actions[9].payload.organizationsResults).toEqual(orgResults);
          expect(actions[9].payload.compositeSourceControlResults).toEqual(unconfiguredCompositeSourceControlPayload);
          expect(actions[9].payload.hostUrlResult).toEqual(null);
        });
      });

      it('uses org provider when one is available', () => {
        return store.dispatch(scmOnboardingActions.loadPage('provider-org')).then(() => {
          // then the SCM_ONBOARDING_LOAD_PAGE_REQUESTED action is created with the expected payload
          let actions = store.getActions();
          expect(actions.map((a) => a.type)).toEqual([
            SCM_ONBOARDING_CHECK_PERMISSIONS_REQUESTED,
            SCM_ONBOARDING_CHECK_PERMISSIONS_FULFILLED,
            SCM_ONBOARDING_LOAD_PAGE_REQUESTED,
            'ownerSideNav/load/pending',
            'ownerSideNav/loadOwnerList/pending',
            'repositories/loadRepositories/pending',
            'ownerSideNav/loadOwnerList/fulfilled',
            'repositories/loadRepositories/fulfilled',
            'ownerSideNav/load/fulfilled',
            SCM_ONBOARDING_LOAD_PAGE_FULFILLED,
          ]);
          expect(actions[2].payload).toEqual('provider-org');

          // and the SCM_ONBOARDING_LOAD_PAGE_FULFILLED action is created using the gitlab provider
          // rather than the parent provider
          expect(actions[9].payload.organizationsResults).toEqual(orgResults);
          expect(actions[9].payload.compositeSourceControlResults).toEqual(
            providerOverriddenCompositeSourceControlPayload
          );
          expect(actions[9].payload.hostUrlResult).toEqual(gitlabDefaultHostPayload);
        });
      });
    });

    describe('handle auth errors', function () {
      function authTestFailure(authTestLabel, errorMessage, authResponsesSupplier) {
        beforeEach(function () {
          store = SpecUtil.mockReduxStore({
            scmOnboarding: {
              formState: {
                selectedOrganization: orgResults[0],
              },
            },
            router: {
              currentState: {
                name: 'scmOnboardingOrg',
              },
              prevState: {
                name: null,
              },
            },
          });
        });

        it(`fails properly when authorization is perform and it calls ${authTestLabel}`, async () => {
          mockAxiosCalls(authResponsesSupplier());
          try {
            await store.dispatch(scmOnboardingActions.loadPage('ownerId'));
          } catch (error) {
            expect(error).toEqual(errorMessage);
          }
          let actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe('SCM_ONBOARDING_CHECK_PERMISSIONS_REQUESTED');
          expect(actions[0].payload).toBeUndefined();

          // and SCM_ONBOARDING_CHECK_PERMISSIONS_FAILED action is created
          expect(actions[1].type).toBe('SCM_ONBOARDING_CHECK_PERMISSIONS_FAILED');
          expect(actions[1].payload).toEqual(errorMessage);
        });
      }

      authTestFailure('permissionsUrl-networkError', 'failed call', () => {
        return {
          get: {
            [getProductFeaturesUrl()]: Promise.resolve({
              data: ['automation'],
            }),
          },
          put: {
            [getPermissionContextTestUrl('organization', 'ownerId')]: () => Promise.reject('failed call'),
          },
        };
      });
      authTestFailure('featuresUrl-networkError', 'failed call', () => {
        return {
          get: {
            [getProductFeaturesUrl()]: () => Promise.reject('failed call'),
          },
          put: {
            [getPermissionContextTestUrl('organization', 'ownerId')]: Promise.resolve({
              data: ['ADD_APPLICATION'],
            }),
          },
        };
      });
      authTestFailure('permissionsUrl-noPermission', authErrorMessage, () => {
        return {
          get: {
            [getProductFeaturesUrl()]: Promise.resolve({
              data: ['automation'],
            }),
          },
          put: {
            [getPermissionContextTestUrl('organization', 'ownerId')]: Promise.resolve({
              data: [],
            }),
          },
        };
      });
      authTestFailure('featuresUrl-featureNotEnable', featureNotEnableErrorMessage, () => {
        return {
          get: {
            [getProductFeaturesUrl()]: Promise.resolve({
              data: ['another feature'],
            }),
          },
          put: {
            [getPermissionContextTestUrl('organization', 'ownerId')]: Promise.resolve({
              data: ['ADD_APPLICATION'],
            }),
          },
        };
      });
    });

    describe('handles errors', function () {
      const organizationsPayload = [{ id: 'ownerId' }, { id: undefined }];

      function testFailure(testLabel, responsesSupplier) {
        beforeEach(function () {
          store = SpecUtil.mockReduxStore({
            scmOnboarding: {
              formState: {
                selectedOrganization: orgResults[0],
              },
            },
            router: {
              currentState: {
                name: 'scmOnboardingOrg',
              },
              prevState: {
                name: null,
              },
            },
          });
        });

        it(`fails properly when it calls ${testLabel}`, function () {
          mockAxiosCalls(responsesSupplier());

          return store.dispatch(scmOnboardingActions.loadPage('ownerId')).then(() => {
            // then SCM_ONBOARDING_LOAD_PAGE_REQUESTED action is created
            let actions = store.getActions();
            expect(actions.length).toBe(8);
            expect(actions[2].type).toBe('SCM_ONBOARDING_LOAD_PAGE_REQUESTED');
            expect(actions[2].payload).toEqual('ownerId');

            // and SCM_ONBOARDING_LOAD_PAGE_FAILED action is created
            const failureAction = actions.find((action) => {
              return action.type === 'SCM_ONBOARDING_LOAD_PAGE_FAILED';
            });
            expect(failureAction).not.toBeNull();
            expect(failureAction.type).toBe('SCM_ONBOARDING_LOAD_PAGE_FAILED');
            expect(failureAction.payload).toEqual('failed call');
          });
        });
      }

      testFailure('organizationsUrl', () => {
        return {
          get: {
            [compositeSourceControlUrl]: Promise.resolve({
              data: compositeSourceControlPayload,
            }),
            [scmOrganizationsUrl]: () => Promise.reject('failed call'),
            [defaultScmHostUrl]: Promise.resolve({
              data: scmDefaultHostPayload,
            }),
            [getProductFeaturesUrl()]: Promise.resolve({
              data: ['automation'],
            }),
          },
          put: {
            [getPermissionContextTestUrl('organization', 'ownerId')]: Promise.resolve({
              data: ['ADD_APPLICATION'],
            }),
          },
        };
      });
      testFailure('compositeSourceControlUrl', () => {
        return {
          get: {
            [compositeSourceControlUrl]: () => Promise.reject('failed call'),
            [scmOrganizationsUrl]: Promise.resolve({
              data: organizationsPayload,
            }),
            [defaultScmHostUrl]: Promise.resolve({
              data: scmDefaultHostPayload,
            }),
            [getProductFeaturesUrl()]: Promise.resolve({
              data: ['automation'],
            }),
          },
          put: {
            [getPermissionContextTestUrl('organization', 'ownerId')]: Promise.resolve({
              data: ['ADD_APPLICATION'],
            }),
          },
        };
      });
      testFailure('defaultScmHostUrl', () => {
        return {
          get: {
            [compositeSourceControlUrl]: Promise.resolve({
              data: compositeSourceControlPayload,
            }),
            [scmOrganizationsUrl]: Promise.resolve({
              data: organizationsPayload,
            }),
            [defaultScmHostUrl]: () => Promise.reject('failed call'),
            [getProductFeaturesUrl()]: Promise.resolve({
              data: ['automation'],
            }),
          },
          put: {
            [getPermissionContextTestUrl('organization', 'ownerId')]: Promise.resolve({
              data: ['ADD_APPLICATION'],
            }),
          },
        };
      });
    });
  });

  describe('onRepositorySelectionChanged', function () {
    it('dispatches an event', function () {
      store = SpecUtil.mockReduxStore(state);

      let repo = { isSelected: true };
      // TODO flesh this out in INT-3479. Not sure if we even want this to be an action at all
      store.dispatch(scmOnboardingActions.onRepositorySelectionChanged(repo));
      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toEqual('SCM_ONBOARDING_REPOSITORY_SELECTION_CHANGED');
    });
  });

  describe('load repositories', function () {
    it('requests a load of repositories', function (done) {
      const orgId = 'orgid';
      const scmUrl = 'http://localhost:1234';
      mockAxiosCalls({
        get: {
          [getScmRepositoriesUrl(orgId, scmUrl)]: Promise.resolve([
            { httpCloneUrl: 'http://localhost/my/repo.git', isPrivate: true },
          ]),
        },
      });

      store = SpecUtil.mockReduxStore(state);

      store.dispatch(scmOnboardingActions.loadRepositories(orgId, scmUrl)).then(() => {
        expect(axios.get).toHaveBeenCalledWith(getScmRepositoriesUrl(orgId, scmUrl));
        done();
      });
      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0]).toEqual({
        type: 'SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED',
      });
    });

    it('handles errors', function (done) {
      const orgId = 'orgid';
      const scmUrl = 'http://localhost:1234';
      mockAxiosCalls({
        get: {
          [getScmRepositoriesUrl(orgId, scmUrl)]: () => Promise.reject('Failed request'),
        },
      });

      store = SpecUtil.mockReduxStore(state);

      store.dispatch(scmOnboardingActions.loadRepositories(orgId, scmUrl)).then(() => {
        expect(axios.get).toHaveBeenCalledWith(getScmRepositoriesUrl(orgId, scmUrl));
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[1].type).toBe('SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED');
        expect(store.getActions()[1].payload).toBe('Failed request');
        done();
      });
      expect(store.getActions().length).toBe(1);
      expect(axios.get).toHaveBeenCalledWith(getScmRepositoriesUrl(orgId, scmUrl));
    });
  });

  describe('setCurrentHostUrl', function () {
    it('dispatches an event', function () {
      store = SpecUtil.mockReduxStore(state);

      let hostUrlValue = 'https://localhost';
      store.dispatch(scmOnboardingActions.setCurrentHostUrl(hostUrlValue));
      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0]).toEqual({
        type: 'SCM_ONBOARDING_SET_CURRENT_HOST_URL',
        payload: hostUrlValue,
      });
    });
  });

  describe('setSelectedOrganization', function () {
    const prevOrg = {
      organization: { id: 'prevId' },
      sourceControl: { token: { value: null } },
    };

    // selected org action creator retrieves top-level state, so need to mock that instead of the narrow state
    function mockReduxStoreForSelectedOrg(isScmTokenOverridden, previousOrg) {
      return SpecUtil.mockReduxStore({
        scmOnboarding: {
          ...state,
          configState: {
            isScmTokenOverridden: isScmTokenOverridden,
          },
          formState: {
            selectedOrganization: previousOrg,
          },
        },
      });
    }

    it('dispatches an org change', function () {
      store = mockReduxStoreForSelectedOrg(false, prevOrg);
      const selectedOrg = {
        organization: { id: 'id1' },
        sourceControl: { token: { value: null }, provider: { value: null, parentValue: 'github' } },
      };

      // no axios calls
      expect(store.dispatch(scmOnboardingActions.setSelectedOrganization(selectedOrg))).toBeUndefined();

      const actions = store.getActions();
      expect(actions.length).toBe(5);
      expect(actions[0].type).toBe(SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED);
      expect(actions[1].type).toBe(SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED);
      expect(actions[1].payload).toEqual({
        selectedOrganization: selectedOrg,
        defaultHostUrl: null,
      });
    });

    it('dispatches loadRepositoriesRequested when the new token is overridden', function () {
      store = mockReduxStoreForSelectedOrg(true, prevOrg);
      const selectedOrg = {
        organization: { id: 'id1' },
        sourceControl: {
          token: { value: null, parentValue: 'redacted', parentName: 'Root Organization' },
          provider: { value: null, parentValue: 'github' },
        },
      };

      // triggers an attempt to get new default host URL
      expect(store.dispatch(scmOnboardingActions.setSelectedOrganization(selectedOrg))).toBeDefined();

      const actions = store.getActions();
      expect(actions).toEqual([{ type: SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED }]);
    });

    it('dispatches loadRepositoriesRequested when the old token was overridden', function () {
      store = mockReduxStoreForSelectedOrg(false, prevOrg);
      const selectedOrg = {
        organization: { id: 'id1' },
        sourceControl: { token: { value: 'redacted' }, provider: { value: 'github' } },
      };

      // attempts to check if default host URL changed
      expect(store.dispatch(scmOnboardingActions.setSelectedOrganization(selectedOrg))).toBeDefined();

      const actions = store.getActions();
      expect(actions).toEqual([{ type: SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED }]);
    });
    it('dispatches loadRepositoriesRequested when the both the new & old tokens are overridden', function () {
      store = mockReduxStoreForSelectedOrg(true, prevOrg);
      const selectedOrg = {
        organization: { id: 'id1' },
        sourceControl: { token: { value: 'redacted' }, provider: { value: 'github' } },
      };

      store.dispatch(scmOnboardingActions.setSelectedOrganization(selectedOrg));
      const actions = store.getActions();
      expect(actions).toEqual([{ type: SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED }]);
    });

    it('dispatches loadRepositories and all further actions when the token is overridden', function (done) {
      const scmDefaultHostPayload = { defaultHostUrl: 'http://localhost/' };
      mockAxiosCalls({
        get: {
          [getScmDefaultHostUrl('id1', 'github')]: Promise.resolve({
            data: scmDefaultHostPayload,
          }),
        },
      });
      store = mockReduxStoreForSelectedOrg(true, prevOrg);
      const selectedOrg = {
        organization: { id: 'id1' },
        sourceControl: { token: { value: 'redacted' }, provider: { value: 'github' } },
      };

      store.dispatch(scmOnboardingActions.setSelectedOrganization(selectedOrg)).then(() => {
        let actions = store.getActions();
        expect(actions.map((a) => a.type)).toEqual([
          SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED,
          SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED,
          SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED,
          SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FAILED,
        ]);
        done();
      });
    });

    it('dispatches loadRepositories when prev org is undefined', function (done) {
      const scmDefaultHostPayload = { defaultHostUrl: 'http://localhost/' };
      mockAxiosCalls({
        get: {
          [getScmDefaultHostUrl('id1', 'github')]: Promise.resolve({
            data: scmDefaultHostPayload,
          }),
        },
      });
      store = mockReduxStoreForSelectedOrg(false, undefined);
      const selectedOrg = {
        organization: { id: 'id1' },
        sourceControl: {
          token: { value: null, parentValue: 'redacted' },
          provider: { value: null, parentValue: 'github' },
        },
      };

      store.dispatch(scmOnboardingActions.setSelectedOrganization(selectedOrg)).then(() => {
        let actions = store.getActions();
        expect(actions.map((a) => a.type)).toEqual([
          SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED,
          SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED,
          SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED,
          SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FAILED,
        ]);
        done();
      });
    });

    it('does not dispatch loadRepositories when token is unchanged', function () {
      store = mockReduxStoreForSelectedOrg(false, prevOrg);
      const selectedOrg = {
        organization: { id: 'id1' },
        sourceControl: { token: { value: null }, provider: { value: null, parentValue: 'github' } },
      };

      // undefined because it does not make any axios calls
      expect(store.dispatch(scmOnboardingActions.setSelectedOrganization(selectedOrg))).toBeUndefined();

      const actions = store.getActions();
      expect(actions.length).toBe(5);
      expect(actions[0].type).toBe(SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED);
      expect(actions[1].type).toBe(SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED);
      expect(actions[1].payload).toEqual({
        selectedOrganization: selectedOrg,
        defaultHostUrl: null,
      });
    });
  });

  describe('validateScmHostUrl', function () {
    const validateScmHostUrlPayload = {
      scmUrlIsValid: false,
      scmUrlErrorMessage: 'BOOM',
    };

    // Mock clock to test debounce
    beforeAll(() => jasmine.clock().install());
    afterAll(() => jasmine.clock().uninstall());

    afterEach(function () {
      expect(axios.get).toHaveBeenCalledWith(validateScmHostUrl);
    });

    it('dispatches a SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_REQUESTED action', function () {
      mockAxiosCalls({
        get: {
          [validateScmHostUrl]: Promise.resolve(validateScmHostUrlPayload),
        },
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

    describe('after successful GET call', function () {
      beforeEach(function () {
        mockAxiosCalls({
          get: {
            [validateScmHostUrl]: Promise.resolve(validateScmHostUrlPayload),
          },
        });
      });

      it('dispatches SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FULFILLED', function (done) {
        store
          .dispatch(scmOnboardingActions.validateScmHostUrl('provider', 'http://host/'))
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
