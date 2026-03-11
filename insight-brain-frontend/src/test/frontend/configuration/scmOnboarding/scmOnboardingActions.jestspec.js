/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import '../../SpecUtil';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import {
  getScmOrganizationsUrl,
  getScmDefaultHostUrl,
  getScmRepositoriesUrl,
  getCompositeSourceControlUrl,
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
  SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED,
  SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED,
  loadPage,
  setSelectedOrganization,
  validateScmHostUrl,
  loadRepositories,
  onRepositorySelectionChanged,
  setCurrentHostUrl,
  SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED,
} from 'MainRoot/configuration/scmOnboarding/scmOnboardingActions';
import { authErrorMessage, featureNotEnableErrorMessage } from 'MainRoot/util/authorizationUtil';
import { getOwnersMap } from 'TestRoot/OrgsAndPolicies/ownerSideNav/nLevelMockData';

describe('scmOnboardingActions', function () {
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  const scmOrganizationsUrl = getScmOrganizationsUrl(),
    defaultScmHostUrl = getScmDefaultHostUrl('ownerId', 'github');

  let store, state;

  beforeEach(function () {
    // Clear all mocks before each test
    jest.clearAllMocks();
    store = SpecUtil.mockReduxStore();
  });

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
        axiosMock.onGet(getRepositoriesUrl()).reply(200, repositoriesList);
        axiosMock.onGet(getOwnerListUrl()).reply(200, ownerListPayload);
        axiosMock.onGet(getCompositeSourceControlUrl('organization', 'id1')).reply(200, compositeSourceControlPayload);
        axiosMock.onGet(scmOrganizationsUrl).reply(200, orgResults);
        axiosMock.onGet(getScmDefaultHostUrl('id1', 'github')).reply(200, scmDefaultHostPayload);
        axiosMock.onGet(getScmDefaultHostUrl('provider-org', 'gitlab')).reply(200, gitlabDefaultHostPayload);
        axiosMock
          .onGet(getCompositeSourceControlUrl('organization', 'id2'))
          .reply(200, unconfiguredCompositeSourceControlPayload);
        axiosMock
          .onGet(getCompositeSourceControlUrl('organization', 'provider-org'))
          .reply(200, providerOverriddenCompositeSourceControlPayload);
        axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['automation']);
        axiosMock.onPut(getPermissionContextTestUrl('repository_container')).reply(200, []);
        axiosMock.onPut(getPermissionContextTestUrl('global', 'global')).reply(200, ['ADD_APPLICATION']);
        axiosMock.onPut(getPermissionContextTestUrl('organization', 'id1')).reply(200, ['ADD_APPLICATION']);
        axiosMock.onPut(getPermissionContextTestUrl('organization', 'id2')).reply(200, ['ADD_APPLICATION']);
        axiosMock.onPut(getPermissionContextTestUrl('organization', 'provider-org')).reply(200, ['ADD_APPLICATION']);
        axiosMock.onPut(getPermissionContextTestUrl('organization', 'ownerId')).reply(200, ['ADD_APPLICATION']);
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
        return store.dispatch(loadPage()).then(() => {
          // then the SCM_ONBOARDING_LOAD_PAGE_REQUESTED action is created with the expected payload
          let actions = store.getActions();
          expect(actions.length).toBe(11);
          expect(actions[0].type).toBe(SCM_ONBOARDING_CHECK_PERMISSIONS_REQUESTED);
          expect(actions[0].payload).toBeUndefined();
          expect(actions[1].type).toBe(SCM_ONBOARDING_CHECK_PERMISSIONS_FULFILLED);
          expect(actions[1].payload).toBeUndefined();
          expect(actions[2].type).toBe(SCM_ONBOARDING_LOAD_PAGE_REQUESTED);
          expect(actions[2].payload).toBeUndefined();

          // and the SCM_ONBOARDING_LOAD_PAGE_FULFILLED action is created with the expected payload
          expect(actions[10].type).toBe(SCM_ONBOARDING_LOAD_PAGE_FULFILLED);
          expect(actions[10].payload.organizationsResults).toEqual(orgResults);
          expect(actions[10].payload.compositeSourceControlResults).toBeNull();
          expect(actions[10].payload.hostUrlResult).toBeNull();
        });
      });

      it('loads the sourceControl and hostUrl config when orgId is given', () => {
        return store.dispatch(loadPage('id1')).then(() => {
          // then the SCM_ONBOARDING_LOAD_PAGE_REQUESTED action is created with the expected payload
          let actions = store.getActions();
          expect(actions.map((a) => a.type)).toEqual([
            SCM_ONBOARDING_CHECK_PERMISSIONS_REQUESTED,
            SCM_ONBOARDING_CHECK_PERMISSIONS_FULFILLED,
            SCM_ONBOARDING_LOAD_PAGE_REQUESTED,
            'ownerSideNav/load/pending',
            'ownerSideNav/loadOwnerList/pending',
            'repositories/loadRepositories/pending',
            'ownerSideNav/setDisplayedOrganization',
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
          expect(actions[10].type).toBe('SCM_ONBOARDING_LOAD_PAGE_FULFILLED');
          expect(actions[10].payload.organizationsResults).toEqual(orgResults);
          expect(actions[10].payload.compositeSourceControlResults).toEqual(compositeSourceControlPayload);
          expect(actions[10].payload.hostUrlResult).toEqual(scmDefaultHostPayload);
        });
      });

      it('when organization has no SCM configuration', () => {
        return store.dispatch(loadPage('id2')).then(() => {
          // then the SCM_ONBOARDING_LOAD_PAGE_REQUESTED action is created with the expected payload
          let actions = store.getActions();
          expect(actions.map((a) => a.type)).toEqual([
            SCM_ONBOARDING_CHECK_PERMISSIONS_REQUESTED,
            SCM_ONBOARDING_CHECK_PERMISSIONS_FULFILLED,
            SCM_ONBOARDING_LOAD_PAGE_REQUESTED,
            'ownerSideNav/load/pending',
            'ownerSideNav/loadOwnerList/pending',
            'repositories/loadRepositories/pending',
            'ownerSideNav/setDisplayedOrganization',
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
          expect(actions[10].payload.organizationsResults).toEqual(orgResults);
          expect(actions[10].payload.compositeSourceControlResults).toEqual(unconfiguredCompositeSourceControlPayload);
          expect(actions[10].payload.hostUrlResult).toEqual(null);
        });
      });

      it('uses org provider when one is available', () => {
        return store.dispatch(loadPage('provider-org')).then(() => {
          // then the SCM_ONBOARDING_LOAD_PAGE_REQUESTED action is created with the expected payload
          let actions = store.getActions();
          expect(actions.map((a) => a.type)).toEqual([
            SCM_ONBOARDING_CHECK_PERMISSIONS_REQUESTED,
            SCM_ONBOARDING_CHECK_PERMISSIONS_FULFILLED,
            SCM_ONBOARDING_LOAD_PAGE_REQUESTED,
            'ownerSideNav/load/pending',
            'ownerSideNav/loadOwnerList/pending',
            'repositories/loadRepositories/pending',
            'ownerSideNav/setDisplayedOrganization',
            'ownerSideNav/loadOwnerList/fulfilled',
            'repositories/loadRepositories/fulfilled',
            'ownerSideNav/load/fulfilled',
            SCM_ONBOARDING_LOAD_PAGE_FULFILLED,
          ]);
          expect(actions[2].payload).toEqual('provider-org');

          // and the SCM_ONBOARDING_LOAD_PAGE_FULFILLED action is created using the gitlab provider
          // rather than the parent provider
          expect(actions[10].payload.organizationsResults).toEqual(orgResults);
          expect(actions[10].payload.compositeSourceControlResults).toEqual(
            providerOverriddenCompositeSourceControlPayload
          );
          expect(actions[10].payload.hostUrlResult).toEqual(gitlabDefaultHostPayload);
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
          const responses = authResponsesSupplier();

          // Setup axiosMock based on responses - handle auth failures properly
          Object.entries(responses.get || {}).forEach(([url, response]) => {
            if (typeof response === 'function') {
              // For functions that should reject, mock as 500 error
              axiosMock.onGet(url).reply(() =>
                response()
                  .then((data) => [200, data.data || data])
                  .catch((err) => [500, err])
              );
            } else if (response && response.then) {
              // For Promise objects
              axiosMock
                .onGet(url)
                .reply(() => response.then((data) => [200, data.data || data]).catch((err) => [500, err]));
            } else {
              // For direct data objects
              axiosMock.onGet(url).reply(200, response.data);
            }
          });

          Object.entries(responses.put || {}).forEach(([url, response]) => {
            if (typeof response === 'function') {
              axiosMock.onPut(url).reply(() =>
                response()
                  .then((data) => [200, data.data || data])
                  .catch((err) => [500, err])
              );
            } else if (response && response.then) {
              axiosMock
                .onPut(url)
                .reply(() => response.then((data) => [200, data.data || data]).catch((err) => [500, err]));
            } else {
              axiosMock.onPut(url).reply(200, response.data);
            }
          });

          try {
            await store.dispatch(loadPage('ownerId'));
          } catch (error) {
            // Some errors might be thrown, that's okay for auth errors
          }

          let actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[0].type).toBe('SCM_ONBOARDING_CHECK_PERMISSIONS_REQUESTED');
          expect(actions[0].payload).toBeUndefined();

          // and SCM_ONBOARDING_CHECK_PERMISSIONS_FAILED action is created
          expect(actions[1].type).toBe('SCM_ONBOARDING_CHECK_PERMISSIONS_FAILED');
          // For network errors, expect the AxiosError object
          if (authTestLabel.includes('networkError')) {
            expect(actions[1].payload.message).toBe('Request failed with status code 500');
          } else {
            expect(actions[1].payload).toEqual(errorMessage);
          }
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

        it(`fails properly when it calls ${testLabel}`, async function () {
          const responses = responsesSupplier();

          // Setup axiosMock based on responses
          Object.entries(responses.get || {}).forEach(([url, response]) => {
            if (typeof response === 'function') {
              axiosMock.onGet(url).reply(() =>
                response()
                  .then((data) => [200, data.data || data])
                  .catch((err) => [500, err])
              );
            } else if (response && response.then) {
              // Handle Promise objects
              axiosMock
                .onGet(url)
                .reply(() => response.then((data) => [200, data.data || data]).catch((err) => [500, err]));
            } else {
              axiosMock.onGet(url).reply(200, response.data);
            }
          });

          Object.entries(responses.put || {}).forEach(([url, response]) => {
            if (typeof response === 'function') {
              axiosMock.onPut(url).reply(() =>
                response()
                  .then((data) => [200, data.data || data])
                  .catch((err) => [500, err])
              );
            } else if (response && response.then) {
              // Handle Promise objects
              axiosMock
                .onPut(url)
                .reply(() => response.then((data) => [200, data.data || data]).catch((err) => [500, err]));
            } else {
              axiosMock.onPut(url).reply(200, response.data);
            }
          });

          try {
            await store.dispatch(loadPage('ownerId'));
          } catch (error) {
            // Some errors might be thrown
          }

          // then SCM_ONBOARDING_LOAD_PAGE_REQUESTED action is created
          let actions = store.getActions();
          expect(actions.length).toBe(9);
          expect(actions[2].type).toBe('SCM_ONBOARDING_LOAD_PAGE_REQUESTED');
          expect(actions[2].payload).toEqual('ownerId');

          // and SCM_ONBOARDING_LOAD_PAGE_FAILED action is created
          const failureAction = actions.find((action) => {
            return action.type === 'SCM_ONBOARDING_LOAD_PAGE_FAILED';
          });
          expect(failureAction).not.toBeNull();
          expect(failureAction.type).toBe('SCM_ONBOARDING_LOAD_PAGE_FAILED');
          // For network errors, expect the AxiosError message
          expect(failureAction.payload.message).toBe('Request failed with status code 500');
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
      store.dispatch(onRepositorySelectionChanged(repo));
      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toEqual('SCM_ONBOARDING_REPOSITORY_SELECTION_CHANGED');
    });
  });

  describe('load repositories', function () {
    it('requests a load of repositories', function (done) {
      const orgId = 'orgid';
      const scmUrl = 'http://localhost:1234';
      axiosMock
        .onGet(getScmRepositoriesUrl(orgId, scmUrl))
        .reply(200, [{ httpCloneUrl: 'http://localhost/my/repo.git', isPrivate: true }]);

      store = SpecUtil.mockReduxStore(state);

      store.dispatch(loadRepositories(orgId, scmUrl)).then(() => {
        // Check that the request was made
        expect(axiosMock.history.get.length).toBe(1);
        expect(axiosMock.history.get[0].url).toBe(getScmRepositoriesUrl(orgId, scmUrl));
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
      axiosMock.onGet(getScmRepositoriesUrl(orgId, scmUrl)).reply(500, 'Failed request');

      store = SpecUtil.mockReduxStore(state);

      store.dispatch(loadRepositories(orgId, scmUrl)).then(() => {
        expect(axiosMock.history.get.length).toBe(1);
        expect(axiosMock.history.get[0].url).toBe(getScmRepositoriesUrl(orgId, scmUrl));
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[1].type).toBe('SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED');
        expect(store.getActions()[1].payload.message).toBe('Request failed with status code 500');
        done();
      });
      expect(store.getActions().length).toBe(1);
    });
  });

  describe('setCurrentHostUrl', function () {
    it('dispatches an event', function () {
      store = SpecUtil.mockReduxStore(state);

      let hostUrlValue = 'https://localhost';
      store.dispatch(setCurrentHostUrl(hostUrlValue));
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
      sourceControl: { token: { value: null }, authenticationType: { value: null, parentValue: null } },
    };

    // selected org action creator retrieves top-level state, so need to mock that instead of the narrow state
    function mockReduxStoreForSelectedOrg(isScmTokenOverridden, previousOrg) {
      return SpecUtil.mockReduxStore({
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: previousOrg?.organization?.id,
              type: 'organization',
            },
          },
        },
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
        sourceControl: {
          token: { value: null },
          provider: { value: null, parentValue: 'github' },
          authenticationType: { value: null, parentValue: null },
        },
      };

      // no axios calls
      expect(store.dispatch(setSelectedOrganization(selectedOrg))).resolves.toMatchObject({
        payload: { id: 'prevId', type: 'organization' },
      });

      const actions = store.getActions();
      expect(actions.length).toBe(4);
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
          authenticationType: { value: null, parentValue: null },
        },
      };

      // triggers an attempt to get new default host URL
      expect(store.dispatch(setSelectedOrganization(selectedOrg))).toBeDefined();

      const actions = store.getActions();
      expect(actions).toEqual([{ type: SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED }]);
    });

    it('dispatches loadRepositoriesRequested when the old token was overridden', function () {
      store = mockReduxStoreForSelectedOrg(false, prevOrg);
      const selectedOrg = {
        organization: { id: 'id1' },
        sourceControl: {
          token: { value: 'redacted' },
          provider: { value: 'github' },
          authenticationType: { value: null, parentValue: null },
        },
      };

      // attempts to check if default host URL changed
      expect(store.dispatch(setSelectedOrganization(selectedOrg))).toBeDefined();

      const actions = store.getActions();
      expect(actions).toEqual([{ type: SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED }]);
    });
    it('dispatches loadRepositoriesRequested when the both the new & old tokens are overridden', function () {
      store = mockReduxStoreForSelectedOrg(true, prevOrg);
      const selectedOrg = {
        organization: { id: 'id1' },
        sourceControl: {
          token: { value: 'redacted' },
          provider: { value: 'github' },
          authenticationType: { value: null, parentValue: null },
        },
      };

      store.dispatch(setSelectedOrganization(selectedOrg));
      const actions = store.getActions();
      expect(actions).toEqual([{ type: SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED }]);
    });

    it('dispatches loadRepositories and all further actions when the token is overridden', function (done) {
      const scmDefaultHostPayload = { defaultHostUrl: 'http://localhost/' };
      axiosMock.onGet(getScmDefaultHostUrl('id1', 'github')).reply(200, scmDefaultHostPayload);
      store = mockReduxStoreForSelectedOrg(true, prevOrg);
      const selectedOrg = {
        organization: { id: 'id1' },
        sourceControl: {
          token: { value: 'redacted' },
          provider: { value: 'github' },
          authenticationType: { value: null, parentValue: null },
        },
      };

      store.dispatch(setSelectedOrganization(selectedOrg)).then(() => {
        let actions = store.getActions();
        expect(actions.map((a) => a.type)).toEqual([
          SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED,
          SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED,
          SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED,
          'ownerSideNav/setDisplayedOrganization',
          'orgsAndPolicies/loadSelectedOwner/pending',
          'orgsAndPolicies/loadSelectedOwner/fulfilled',
          SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED,
        ]);
        done();
      });
    });

    it('dispatches loadRepositories when prev org is undefined', function (done) {
      const scmDefaultHostPayload = { defaultHostUrl: 'http://localhost/' };
      axiosMock.onGet(getScmDefaultHostUrl('id1', 'github')).reply(200, scmDefaultHostPayload);
      store = mockReduxStoreForSelectedOrg(false, undefined);
      const selectedOrg = {
        organization: { id: 'id1' },
        sourceControl: {
          token: { value: null, parentValue: 'redacted' },
          provider: { value: null, parentValue: 'github' },
          authenticationType: { value: null, parentValue: null },
        },
      };

      store.dispatch(setSelectedOrganization(selectedOrg)).then(() => {
        let actions = store.getActions();
        expect(actions.map((a) => a.type)).toEqual([
          SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED,
          SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED,
          SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED,
          'ownerSideNav/setDisplayedOrganization',
          'orgsAndPolicies/loadSelectedOwner/pending',
          'orgsAndPolicies/loadSelectedOwner/fulfilled',
          SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED,
        ]);
        done();
      });
    });

    it('does not dispatch loadRepositories when token is unchanged', function () {
      store = mockReduxStoreForSelectedOrg(false, prevOrg);
      const selectedOrg = {
        organization: { id: 'id1' },
        sourceControl: {
          token: { value: null },
          provider: { value: null, parentValue: 'github' },
          authenticationType: { value: null, parentValue: null },
        },
      };

      // undefined because it does not make any axios calls
      expect(store.dispatch(setSelectedOrganization(selectedOrg))).resolves.toMatchObject({
        payload: { id: 'prevId', type: 'organization' },
      });

      const actions = store.getActions();
      expect(actions.length).toBe(4);
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
    beforeAll(() => jest.useFakeTimers());
    afterAll(() => jest.useRealTimers());

    // afterEach(function () {
    //   expect(axiosMock.history.get.length).toBe(1);
    //   expect(axiosMock.history.get[0].url).toBe('/rest/onboarding/validate/provider?scmHostUrl=http%3A%2F%2Fhost%2F');
    // });

    it('dispatches a SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_REQUESTED action', function () {
      axiosMock.onGet(/\/rest\/onboarding\/validate\//).reply(200, validateScmHostUrlPayload);

      store.dispatch(validateScmHostUrl('provider', 'http://host/'));

      // dispatches no actions until debounce timeout has passed
      expect(store.getActions().length).toBe(0);

      // Advance all timers to trigger debounced action
      jest.advanceTimersByTime(3000);

      // after the debounce timeout the request is dispatched
      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe('SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_REQUESTED');
      expect(actions[0].payload).toBeUndefined();
    });

    describe('after successful GET call', function () {
      beforeEach(function () {
        jest.useFakeTimers();
        axiosMock.onGet(/\/rest\/onboarding\/validate\//).reply(200, validateScmHostUrlPayload);
      });

      afterEach(function () {
        jest.useRealTimers();
      });

      it('dispatches SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FULFILLED', function (done) {
        // Dispatch the action
        store.dispatch(validateScmHostUrl('provider', 'http://host/'));

        let actions = store.getActions();
        expect(actions.length).toBe(0);

        // Advance timers to trigger debounced action
        jest.advanceTimersByTime(3000);

        // after the debounce timeout the request is dispatched
        actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe('SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_REQUESTED');

        // Switch to real timers for axios promise resolution
        jest.useRealTimers();

        // Wait for axios promise to resolve
        setTimeout(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FULFILLED');
          done();
        }, 10);
      });
    });
  });
});
