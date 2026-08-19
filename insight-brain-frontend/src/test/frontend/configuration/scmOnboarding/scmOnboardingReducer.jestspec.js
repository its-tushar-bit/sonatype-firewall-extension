/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../../main/frontend/configuration/scmOnboarding/scmOnboardingReducer';
import { initialState } from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import * as textInputStateHelpers from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import { UI_ROUTER_ON_FINISH } from '../../../../main/frontend/reduxUiRouter/routerActions';
import {
  SCM_ONBOARDING_IMPORT_REPOS_REQUESTED,
  SCM_ONBOARDING_IS_GIT_HOST_NEEDED,
  SCM_ONBOARDING_IS_IMPORT_STATUS_MODAL_VISIBLE,
  SCM_ONBOARDING_LOAD_PAGE_FULFILLED,
  SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FAILED,
  SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED,
  SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED,
  SCM_ONBOARDING_SHOW_HOST_DIALOG,
} from '../../../../main/frontend/configuration/scmOnboarding/scmOnboardingActions';
import ownerConstant from '../../../../main/frontend/utility/services/owner.constant';
import { SCM_AUTHN_FAILURE } from '../../../../main/frontend/configuration/scmOnboarding/utils/errorCodes';

describe('scmOnboardingReducer', function () {
  let otherObject;

  beforeEach(function () {
    otherObject = { value: 'test value' };
  });

  describe('unknown action', function () {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' });
      const action = {
        type: 'UNKNOWN',
      };
      const newState = reduce(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('SCM_ONBOARDING_LOAD_PAGE', function () {
    let previousState, defaultOrganizationsPayloadWithoutRoot, defaultOrganizationsPayload;
    const rootOrgPayload = {
      organization: {
        name: 'Root Organization',
        id: ownerConstant.ROOT_ORGANIZATION_ID,
      },
      sourceControl: {
        provider: { value: 'github' },
        token: { value: 'redacted token' },
      },
    };

    beforeEach(() => {
      previousState = {
        other: otherObject,
        viewState: {
          loadingPage: true,
          isGitHostNeeded: false,
          isGitHostDialogVisible: false,
          isSelectingOrganization: false,
        },
        configState: {
          isScmTokenConfigured: null,
          scmProvider: null,
          rootProvider: null,
        },
        formState: {
          selectedOrganization: null,
          organizations: null,
          defaultHostUrl: null,
          currentHostUrlState: textInputStateHelpers.initialState(''),
          preselectedOrganizationId: 'id1',
        },
      };
      defaultOrganizationsPayloadWithoutRoot = [
        {
          organization: {
            name: 'name0',
            id: 'id0',
          },
          sourceControl: {
            provider: { value: null, parentValue: 'github' },
            token: { value: null },
            authenticationType: { value: null, parentValue: null },
          },
        },
        {
          organization: {
            name: 'name1',
            id: 'id1',
          },
          sourceControl: {
            provider: { value: null, parentValue: 'github' },
            token: { value: null, parentValue: 'parentValue' },
            authenticationType: { value: null, parentValue: null },
          },
        },
        {
          organization: {
            name: 'Gitlab Org',
            id: 'gitlab-org',
          },
          sourceControl: {
            provider: { value: 'gitlab', parentValue: null },
            token: { value: 'token', parentValue: null, parentName: null },
            authenticationType: { value: null, parentValue: null },
          },
        },
      ];
      defaultOrganizationsPayload = [...defaultOrganizationsPayloadWithoutRoot, rootOrgPayload];
    });

    describe('FULFILLED', function () {
      it('updates the state with the data loaded from IQ with a selected org', function () {
        // given an initial state
        const state = Object.freeze(previousState);

        // and several orgs returned in the payload
        const payload = {
          organizationsResults: defaultOrganizationsPayload,
          compositeSourceControlResults: {
            provider: { value: null, parentValue: 'github' },
            token: { value: 'token' },
            authenticationType: { value: null, parentValue: null },
          },
          hostUrlResult: { defaultHostUrl: 'http://localhost/' },
        };

        // when reduce is invoked
        const newState = reduce(state, {
          type: SCM_ONBOARDING_LOAD_PAGE_FULFILLED,
          payload: payload,
        });

        // then state is updated
        expect(newState).toEqual({
          other: otherObject,
          viewState: {
            loadingPage: false,
            isGitHostNeeded: false,
            isGitHostDialogVisible: false,
            isSelectingOrganization: false,
          },
          configState: {
            isScmTokenConfigured: true,
            isScmTokenOverridden: false,
            isRootScmConfigured: true,
            scmProvider: 'github',
            rootProvider: 'github',
            rootOrgHasToken: true,
          },
          formState: {
            selectedOrganization: defaultOrganizationsPayload[1],
            organizations: defaultOrganizationsPayloadWithoutRoot,
            defaultHostUrl: 'http://localhost/',
            currentHostUrlState: initialState('http://localhost/'),
            preselectedOrganizationId: 'id1',
          },
        });
      });

      it('updates the state with the data loaded from IQ with no selected org', function () {
        // given an initial state
        const state = Object.freeze({
          ...previousState,
          formState: {
            ...previousState.formState,
            preselectedOrganizationId: null,
          },
        });

        // and several orgs returned in the payload but not selected details
        const payload = {
          organizationsResults: defaultOrganizationsPayload,
          compositeSourceControlResults: null,
          hostUrlResult: { defaultHostUrl: null },
        };

        // when reduce is invoked
        const newState = reduce(state, {
          type: SCM_ONBOARDING_LOAD_PAGE_FULFILLED,
          payload: payload,
        });

        // then state is updated
        expect(newState).toEqual({
          other: otherObject,
          viewState: {
            loadingPage: false,
            isGitHostNeeded: false,
            isGitHostDialogVisible: false,
            isSelectingOrganization: false,
          },
          configState: {
            isScmTokenConfigured: false,
            isRootScmConfigured: true,
            scmProvider: 'github',
            rootProvider: 'github',
            rootOrgHasToken: true,
          },
          formState: {
            selectedOrganization: null,
            organizations: defaultOrganizationsPayloadWithoutRoot,
            defaultHostUrl: null,
            currentHostUrlState: textInputStateHelpers.initialState(''),
            preselectedOrganizationId: null,
          },
        });
      });

      it('updates the state with the data loaded from IQ with token in root organization', () => {
        // given an initial state
        const state = Object.freeze(previousState);

        // and the token is configured in the root organization
        const payload = {
          organizationsResults: defaultOrganizationsPayload,
          compositeSourceControlResults: {
            provider: { value: null, parentValue: 'github' },
            token: { parentValue: 'token' },
            authenticationType: { value: null, parentValue: null },
          },
          hostUrlResult: { defaultHostUrl: 'http://localhost/' },
        };

        // when reduce is invoked
        const newState = reduce(state, {
          type: 'SCM_ONBOARDING_LOAD_PAGE_FULFILLED',
          payload: payload,
        });

        // then state is updated and the git host is still needed
        expect(newState).toEqual({
          other: otherObject,
          viewState: {
            loadingPage: false,
            isGitHostNeeded: false,
            isGitHostDialogVisible: false,
            isSelectingOrganization: false,
          },
          configState: {
            isScmTokenConfigured: true,
            isScmTokenOverridden: false,
            isRootScmConfigured: true,
            scmProvider: 'github',
            rootProvider: 'github',
            rootOrgHasToken: true,
          },
          formState: {
            selectedOrganization: defaultOrganizationsPayload[1],
            organizations: defaultOrganizationsPayloadWithoutRoot,
            defaultHostUrl: 'http://localhost/',
            currentHostUrlState: initialState('http://localhost/'),
            preselectedOrganizationId: 'id1',
          },
        });
      });

      it('updates the state with the data loaded from IQ when no root org config exists', () => {
        // given an initial state
        const state = Object.freeze(previousState);

        // and a payload where root has no source control config
        const rootOrgPayloadNoSC = {
          organization: {
            name: 'Root Organization',
            id: ownerConstant.ROOT_ORGANIZATION_ID,
          },
          sourceControl: {
            provider: {},
            token: {},
            authenticationType: {},
            id: null,
          },
        };

        const orgPayload = [...defaultOrganizationsPayloadWithoutRoot, rootOrgPayloadNoSC];

        // and the token is configured in the root organization
        const payload = {
          organizationsResults: orgPayload,
          compositeSourceControlResults: {
            provider: { value: null, parentValue: null },
            token: { parentValue: null },
            authenticationType: { value: null, parentValue: null },
          },
          hostUrlResult: { defaultHostUrl: 'http://localhost/' },
        };

        // when reduce is invoked
        const newState = reduce(state, {
          type: 'SCM_ONBOARDING_LOAD_PAGE_FULFILLED',
          payload: payload,
        });

        // then state is updated and the git host is still needed
        expect(newState).toEqual({
          other: otherObject,
          viewState: {
            loadingPage: false,
            isGitHostNeeded: false,
            isGitHostDialogVisible: false,
            isSelectingOrganization: false,
          },
          configState: {
            isScmTokenConfigured: true,
            isScmTokenOverridden: false,
            isRootScmConfigured: false,
            scmProvider: 'github',
            rootProvider: undefined,
            rootOrgHasToken: false,
          },
          formState: {
            selectedOrganization: defaultOrganizationsPayload[1],
            organizations: defaultOrganizationsPayloadWithoutRoot,
            defaultHostUrl: 'http://localhost/',
            currentHostUrlState: initialState('http://localhost/'),
            preselectedOrganizationId: 'id1',
          },
        });
      });

      it('shows the host dialog when provider has changed at org level and a token is available', () => {
        // given an initial state where there is no preselected organization
        const state = Object.freeze({
          ...previousState,
          formState: {
            ...previousState,
            preselectedOrganizationId: 'gitlab-org',
          },
        });

        // and the provider is configured in the newly selected organization
        const payload = {
          organizationsResults: defaultOrganizationsPayload,
          hostUrlResult: { defaultHostUrl: '' },
        };

        // when reduce is invoked
        const newState = reduce(state, {
          type: 'SCM_ONBOARDING_LOAD_PAGE_FULFILLED',
          payload: payload,
        });

        // then state is updated and dialog is shown
        expect(newState.configState).toEqual({
          ...previousState.configState,
          isScmTokenConfigured: true,
          isScmTokenOverridden: true,
          isRootScmConfigured: true,
          scmProvider: 'gitlab',
          rootOrgHasToken: true,
          rootProvider: 'github',
        });
        expect(newState.viewState).toEqual({
          loadingPage: false,
          isGitHostNeeded: true,
          isGitHostDialogVisible: true,
          isSelectingOrganization: false,
        });
      });

      it('does not show the host dialog when no org has been selected', () => {
        // given an initial state where there is no preselected organization
        const state = Object.freeze({
          ...previousState,
          formState: {
            ...previousState,
            preselectedOrganizationId: null,
          },
        });

        // and an initial load with no host URL or selected org
        const payload = {
          organizationsResults: defaultOrganizationsPayload,
          compositeSourceControlResults: null,
          hostUrlResult: { defaultHostUrl: '' },
        };

        // when reduce is invoked
        const newState = reduce(state, {
          type: 'SCM_ONBOARDING_LOAD_PAGE_FULFILLED',
          payload: payload,
        });

        // then state is updated and dialog is not shown
        expect(newState.viewState).toEqual({
          loadingPage: false,
          isGitHostNeeded: false,
          isGitHostDialogVisible: false,
          isSelectingOrganization: false,
        });
      });
    });

    describe('FAILED', function () {
      it('sets generalError field to value of error', function () {
        const state = Object.freeze({
          viewState: {
            loadingPage: true,
            generalError: null,
            isImporting: true,
          },
        });

        const newState = reduce(state, {
          type: 'SCM_ONBOARDING_LOAD_PAGE_FAILED',
          payload: { status: 502 },
        });

        expect(newState.viewState).toEqual({
          loadingPage: false,
          loadingRepositories: false,
          isSelectingOrganization: false,
          validatingCompositeSourceControl: false,
          loadRepositoriesErrorCode: null,
          isNewOrganizationModalVisible: false,
          addOrganizationError: null,
          generalError: {
            status: 502,
          },
          isGitHostNeeded: false,
          isGitHostDialogVisible: false,
          isImportStatusDialogVisible: false,
          isImporting: false,
        });
      });
    });
  });

  describe('SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED action', function () {
    it('clears the error state', function () {
      // given a state with errors
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          loadingRepositories: false,
          generalError: 'general error',
          loadRepositoriesErrorCode: SCM_AUTHN_FAILURE,
        },
        formState: {
          repositories: ['a'],
          totalRepositories: 1,
          importedRepositoryCount: 1,
          selectedRepositoryCount: 1,
        },
      });

      // when reduce is invoked
      const newState = reduce(state, {
        type: 'SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED',
        payload: undefined,
      });

      // then state is updated
      expect(newState.formState).toEqual({
        repositories: [],
        totalRepositories: 0,
        importedRepositoryCount: 0,
        selectedRepositoryCount: 0,
      });
      expect(newState.viewState.loadingRepositories).toBe(true);
      expect(newState.viewState.generalError).toBe(null);
      expect(newState.viewState.loadRepositoriesErrorCode).toBe(null);

      // and other properties are not modified
      expect(newState.other).toEqual(otherObject);
    });
  });

  describe('SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED action', function () {
    it('populates state repositories list', function () {
      // given empty repositories list
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          loadingRepositories: true,
        },
        formState: {
          repositories: [],
        },
        sortConfiguration: {
          key: 'namespace',
          sortingOrder: ['namespace'],
          dir: 'asc',
        },
      });

      const repositoriesPayload = {
        totalRepositories: 1,
        status: 'SUCCESS',
        availableRepositories: [
          {
            project: 'project',
            namespace: 'namespace',
            description: 'description',
          },
        ],
      };

      // when reduce is invoked
      const newState = reduce(state, {
        type: 'SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED',
        payload: repositoriesPayload,
      });

      // then state is updated
      expect(newState.formState.repositories).toEqual(repositoriesPayload.availableRepositories);
      expect(newState.formState.totalRepositories).toEqual(repositoriesPayload.totalRepositories);
      expect(newState.viewState.loadingRepositories).toBe(false);

      // and other properties are not modified
      expect(newState.sortConfiguration).toBe(state.sortConfiguration);
      expect(newState.other).toEqual(otherObject);
    });

    it('handles null lists of available repositories', function () {
      // given non-empty repositories list
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          loadingRepositories: true,
        },
        formState: {
          repositories: [{}],
        },
        sortConfiguration: {
          key: 'namespace',
          sortingOrder: ['namespace'],
          dir: 'asc',
        },
      });

      // and a payload with no available repos
      const repositoriesPayload = {
        totalRepositories: 1,
        status: 'SUCCESS',
        availableRepositories: null,
      };

      // when reduce is invoked
      const newState = reduce(state, {
        type: 'SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED',
        payload: repositoriesPayload,
      });

      // then the new list is an empty array, not null
      expect(newState.formState.repositories).toEqual([]);
    });

    it('sorts when repositories are populated', function () {
      // given empty repositories list
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          loadingRepositories: true,
        },
        formState: {
          repositories: [],
        },
        sortConfiguration: {
          sortingOrder: ['namespace'],
        },
      });

      const repositoriesPayload = {
        totalRepositories: 2,
        status: 'SUCCESS',
        availableRepositories: [
          {
            namespace: 'b',
          },
          {
            namespace: 'a',
          },
        ],
      };

      // when reduce is invoked
      const newState = reduce(state, {
        type: 'SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED',
        payload: repositoriesPayload,
      });

      // then state is updated
      expect(newState.formState.repositories).toEqual([
        {
          namespace: 'a',
        },
        {
          namespace: 'b',
        },
      ]);
      expect(newState.formState.totalRepositories).toEqual(repositoriesPayload.totalRepositories);
      expect(newState.viewState.loadingRepositories).toBe(false);

      // and other properties are not modified
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED action', function () {
    it('sets generalError field to value of error', function () {
      // given an initial state
      const state = Object.freeze({
        viewState: {
          loadingRepositories: true,
          generalError: null,
        },
        formState: {
          repositories: [
            {
              project: 'project',
              namespace: 'namespace',
              description: 'description',
            },
          ],
        },
        other: otherObject,
      });
      const errorResponse = { response: { status: 502 } };

      // when the reducer is invoked
      const newState = reduce(state, {
        type: 'SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED',
        payload: errorResponse,
      });

      // the state is updated as expected
      expect(newState).toEqual({
        viewState: {
          loadingRepositories: false,
          generalError: errorResponse,
          loadRepositoriesErrorCode: null,
          isGitHostDialogVisible: false,
        },
        formState: {
          repositories: [],
        },
        other: otherObject,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('sets loadRepositoriesErrorCode field to value of error', function () {
      const state = Object.freeze({
        viewState: {
          loadingRepositories: true,
        },
        configState: {
          scmProvider: 'provider',
        },
      });
      const response = {
        status: SCM_AUTHN_FAILURE,
      };

      const newState = reduce(state, {
        type: 'SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED',
        payload: response,
      });

      expect(newState.viewState).toEqual({
        loadingRepositories: false,
        loadRepositoriesErrorCode: SCM_AUTHN_FAILURE,
        generalError: null,
        isGitHostDialogVisible: true,
      });
    });
  });

  describe('SCM_ONBOARDING_SET_TARGET_ORGANIZATION action', function () {
    describe('FULFILLED', () => {
      it('populates selected organization', function () {
        // given no organization is selected
        const state = Object.freeze({
          other: otherObject,
          configState: {
            isScmTokenOverridden: true,
          },
          formState: {
            selectedOrganization: null,
          },
          viewState: {},
        });

        const selectedOrganization = {
          organization: {
            project: 'project',
            namespace: 'namespace',
            description: 'description',
          },
          sourceControl: {
            token: { value: 'redacted' },
            provider: { value: 'github' },
            authenticationType: { value: null, parentValue: null },
          },
        };

        const defaultHostUrl = 'http://localhost:1234/';

        // when reduce is invoked
        const newState = reduce(state, {
          type: SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED,
          payload: { selectedOrganization, defaultHostUrl },
        });

        // then state is updated
        expect(newState.formState.selectedOrganization).toBe(selectedOrganization);
        expect(newState.configState.isScmTokenOverridden).toBe(true);

        // and other properties are not modified
        expect(newState.other).toBe(otherObject);
      });

      describe('sets the show dialog state: ', () => {
        const testData = [
          {
            description: 'org is defined and overrides scm token, no default host => show dialog',
            prevState: {
              configState: {},
              formState: {},
              viewState: {},
            },
            newState: {
              configState: {
                isScmTokenOverridden: true,
              },
              viewState: {
                defaultHostUrl: '',
              },
              formState: {
                selectedOrganization: {
                  sourceControl: {
                    provider: { value: null, parentValue: 'github' },
                    token: { value: 'redacted' },
                    authenticationType: { value: null, parentValue: null },
                  },
                },
              },
            },
            expectedValue: true,
          },
          {
            description: 'org is defined with no custom token, but prev state had a custom token => show dialog',
            prevState: {
              configState: {
                isScmTokenOverridden: true,
              },
              formState: {},
              viewState: {},
            },
            newState: {
              configState: {
                isScmTokenOverridden: false,
              },
              viewState: {
                defaultHostUrl: '',
              },
              formState: {
                selectedOrganization: {
                  sourceControl: {
                    provider: { value: null, parentValue: 'github' },
                    token: { value: 'redacted' },
                    authenticationType: { value: null, parentValue: null },
                  },
                },
              },
            },
            expectedValue: true,
          },
        ];

        for (let currTest of testData) {
          it(currTest.description, function () {
            // when reduce is invoked
            const newState = reduce(currTest.prevState, {
              type: SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED,
              payload: {
                selectedOrganization: currTest.newState.formState.selectedOrganization,
                defaultHostUrl: currTest.newState.viewState.defaultHostUrl,
              },
            });

            // then state is updated
            expect(newState.formState).toEqual(expect.objectContaining(currTest.newState.formState));
            expect(newState.viewState).toEqual(
              expect.objectContaining({
                isGitHostNeeded: currTest.expectedValue,
                isGitHostDialogVisible: currTest.expectedValue,
              })
            );
          });
        }
      });

      describe('calculates a suggested host URL:', () => {
        const providerData = [
          { provider: 'github', url: 'https://github.com/' },
          { provider: 'gitlab', url: 'https://gitlab.com/' },
          { provider: 'bitbucket', url: 'https://bitbucket.org/' },
        ];
        for (let testData of providerData) {
          it('defaults to ' + testData.url + ' when provider is ' + testData.provider, function () {
            // given empty repositories list
            const state = Object.freeze({
              other: otherObject,
              configState: {
                scmProvider: testData.provider,
              },
              viewState: {
                isSelectingOrganization: true,
              },
              formState: {},
            });

            // when reduce is invoked without an identified URL
            const selectedOrganization = {
              sourceControl: {
                provider: { value: null, parentValue: testData.provider },
                token: { value: null, parentValue: 'redacted' },
                authenticationType: { value: null, parentValue: null },
              },
            };
            const newState = reduce(state, {
              type: SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED,
              payload: {
                selectedOrganization: selectedOrganization,
                defaultHostUrl: '',
              },
            });

            // then current host URL state is updated to the provider defaults
            expect(newState.formState).toEqual({
              defaultHostUrl: '',
              currentHostUrlState: initialState(testData.url),
              selectedOrganization: selectedOrganization,
            });
            expect(newState.viewState).toEqual({
              isSelectingOrganization: false,
              isGitHostNeeded: true,
              isGitHostDialogVisible: true,
            });

            // and other properties are not modified
            expect(newState.other).toEqual(otherObject);
          });
        }
      });
    });

    describe('REQUESTED action', function () {
      it('sets the loading state', function () {
        // given a state with errors
        const state = Object.freeze({
          other: otherObject,
          viewState: {
            isSelectingOrganization: false,
            loadRepositoriesErrorCode: 'preexisting-value',
          },
        });

        // when reduce is invoked
        const newState = reduce(state, {
          type: SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED,
        });

        // then state is updated
        expect(newState.viewState).toEqual({
          isSelectingOrganization: true,
          loadRepositoriesErrorCode: null,
        });

        // and other properties are not modified
        expect(newState.other).toEqual(otherObject);
      });
    });

    describe('FAILED action', function () {
      it('sets generalError field to value of error', function () {
        // given an initial state
        const state = Object.freeze({
          viewState: {
            isSelectingOrganization: true,
            generalError: null,
          },
          other: otherObject,
        });
        const errorResponse = { response: { status: 502 } };

        // when the reducer is invoked
        const newState = reduce(state, {
          type: SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FAILED,
          payload: errorResponse,
        });

        // the state is updated as expected
        expect(newState).toEqual({
          viewState: {
            isSelectingOrganization: false,
            generalError: errorResponse,
          },
          other: otherObject,
        });
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });
    });
  });

  describe('SCM_ONBOARDING_SET_CURRENT_HOST_URL action', function () {
    // this table is a full mesh of 3 inputs 1 output
    const testDataTable = [
      {
        currentValue: 'https://example.com/foo/',
        payload: 'https://example.com/bar/',
        existingValidationErrors: null,
        expectedValidationErrors: null,
      },
      {
        currentValue: 'https://example.com/foo/',
        payload: 'https://example.com/bar/',
        existingValidationErrors: 'CRASH',
        expectedValidationErrors: 'CRASH',
      },
      {
        currentValue: 'https://example.com/',
        payload: 'invalid',
        existingValidationErrors: 'BANG',
        expectedValidationErrors: 'Not a valid URL',
      },
      {
        currentValue: 'https://example.com/',
        payload: 'invalid',
        existingValidationErrors: null,
        expectedValidationErrors: 'Not a valid URL',
      },
      {
        currentValue: 'invalid',
        payload: 'https://example.com/bar/',
        existingValidationErrors: null,
        expectedValidationErrors: null,
      },
      {
        currentValue: 'invalid',
        payload: 'https://example.com/bar/',
        existingValidationErrors: 'ZAP',
        expectedValidationErrors: 'ZAP',
      },
      {
        currentValue: 'invalid',
        payload: 'invalid',
        existingValidationErrors: [],
        expectedValidationErrors: 'Not a valid URL',
      },
      {
        currentValue: 'invalid',
        payload: 'invalid',
        existingValidationErrors: 'KABOOM',
        expectedValidationErrors: 'Not a valid URL',
      },
      {
        currentValue: 'h',
        payload: '',
        existingValidationErrors: 'BOING',
        expectedValidationErrors: null,
      },
    ];

    describe('validation', () => {
      for (const testData of testDataTable) {
        it('sets the default host URL', function () {
          // given a clean host URL
          const state = Object.freeze({
            other: otherObject,
            formState: {
              currentHostUrlState: {
                value: testData.currentValue,
                validationErrors: testData.existingValidationErrors,
              },
            },
          });

          // when reduce is invoked
          const newState = reduce(state, {
            type: 'SCM_ONBOARDING_SET_CURRENT_HOST_URL',
            payload: testData.payload,
          });

          // then state is updated
          expect(newState.formState.currentHostUrlState.value).toEqual(testData.payload);

          // and no validation errors are displayed
          expect(newState.formState.currentHostUrlState.validationErrors).toEqual(testData.expectedValidationErrors);

          // and other properties are not modified
          expect(newState.other).toBe(otherObject);
        });
      }
    });
  });

  describe('import repositories', () => {
    describe('requested', () => {
      it('sets the isImporting flag', () => {
        const state = Object.freeze({
          viewState: {
            isImporting: false,
          },
          other: otherObject,
        });

        // when reduce is invoked
        const newState = reduce(state, {
          type: SCM_ONBOARDING_IMPORT_REPOS_REQUESTED,
        });

        // then importing state is updated
        expect(newState.formState).toEqual({
          selectedRepositoryCount: 0,
          failedImportCount: 0,
          failedRepos: [],
        });
        expect(newState.viewState).toEqual({
          isImporting: true,
        });

        // and other properties are not modified
        expect(newState.other).toBe(otherObject);
      });
    });

    describe('partial success', () => {
      it('updates the new repository list', function () {
        // given previous state with token
        let initialRepos = [
          { httpCloneUrl: 'http://host/prj/a' },
          { httpCloneUrl: 'http://host/prj/b' },
          { httpCloneUrl: 'http://host/prj/c' },
          { httpCloneUrl: 'http://host/prj/d' },
          { httpCloneUrl: 'http://host/prj/e' },
          { httpCloneUrl: 'http://host/prj/f' },
        ];
        const state = Object.freeze({
          formState: {
            repositories: initialRepos,
            importedRepositoryCount: 0,
            importedRepos: [],
            selectedRepositoryCount: 1,
            newlyImportedRepos: [],
            failedImportCount: 0,
          },
          viewState: {
            isImportStatusDialogVisible: false,
            isImporting: true,
          },
          other: otherObject,
        });

        // when reduce is invoked
        let importedRepos = [{ httpCloneUrl: 'http://host/prj/a' }, { httpCloneUrl: 'http://host/prj/b' }];
        const newState = reduce(state, {
          type: 'SCM_ONBOARDING_IMPORT_REPOS_FULFILLED',
          payload: {
            importedRepositories: importedRepos,
            failedImportCount: 3,
          },
        });

        // then imported state is updated
        expect(newState.formState.importedRepositoryCount).toBe(2);
        expect(newState.formState.selectedRepositoryCount).toBe(0);
        expect(newState.formState.failedImportCount).toBe(3);
        expect(newState.formState.newlyImportedRepos).toBe(importedRepos);
        expect(newState.formState.repositories).toEqual([
          { httpCloneUrl: 'http://host/prj/c' },
          { httpCloneUrl: 'http://host/prj/d' },
          { httpCloneUrl: 'http://host/prj/e' },
          { httpCloneUrl: 'http://host/prj/f' },
        ]);
        expect(newState.viewState).toEqual({
          isImportStatusDialogVisible: true,
          isImporting: false,
        });

        // and other properties are not modified
        expect(newState.other).toBe(otherObject);
      });
    });

    describe('HTTP request fails', () => {
      it('sets generalError field to value of error', function () {
        const state = Object.freeze({
          viewState: {
            generalError: null,
            isImporting: true,
          },
          other: otherObject,
        });
        const errorResponse = { response: { status: 502 } };

        const newState = reduce(state, {
          type: 'SCM_ONBOARDING_IMPORT_REPOS_FAILED',
          payload: errorResponse,
        });

        expect(newState).toEqual({
          viewState: {
            generalError: errorResponse,
            isImporting: false,
          },
          other: otherObject,
        });
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });
    });
  });

  describe('validate scm host url', () => {
    describe('succeeds', () => {
      it('clears validation errors', () => {
        // given initial state
        const state = Object.freeze({
          other: otherObject,
          formState: {
            currentHostUrlState: {
              validationErrors: 'BOOM',
            },
          },
        });

        // when reduce is invoked
        const newState = reduce(state, {
          type: 'SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FULFILLED',
          payload: {
            isValid: true,
          },
        });

        // then validation errors are clear
        expect(newState.formState.currentHostUrlState.validationErrors).toBeNull();

        // and other objects unchanged
        expect(newState.other).toBe(otherObject);
      });
    });

    describe('fails validation', () => {
      it('clears validation errors', () => {
        // given initial state
        const state = Object.freeze({
          other: otherObject,
          formState: {
            currentHostUrlState: initialState('http://example.com/'),
          },
        });

        // when reduce is invoked
        const newState = reduce(state, {
          type: 'SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FULFILLED',
          payload: {
            isValid: false,
            errorMessages: 'CRASH',
          },
        });

        // then validation errors is populated with error message
        expect(newState.formState.currentHostUrlState.validationErrors).toEqual('CRASH');

        // and other objects unchanged
        expect(newState.other).toBe(otherObject);
      });
    });

    describe('fails REST call', () => {
      it('sets generalError field to value of error', function () {
        const state = Object.freeze({
          viewState: {
            validatingCompositeSourceControl: true,
            generalError: null,
          },
          other: otherObject,
        });
        const errorResponse = { response: { status: 404 } };

        const newState = reduce(state, {
          type: 'SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FAILED',
          payload: errorResponse,
        });

        expect(newState).toEqual({
          viewState: {
            validatingCompositeSourceControl: false,
            generalError: errorResponse,
          },
          other: otherObject,
        });
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });
    });
  });

  describe('SCM_ONBOARDING_IS_GIT_HOST_NEEDED', () => {
    for (let payload in [true, false]) {
      it('sets the loading state using ' + payload, function () {
        // given a state with errors
        const state = Object.freeze({
          other: otherObject,
          viewState: {},
        });

        // when reduce is invoked
        const newState = reduce(state, {
          type: SCM_ONBOARDING_IS_GIT_HOST_NEEDED,
          payload,
        });

        // then state is updated
        expect(newState.viewState).toEqual({
          isGitHostNeeded: payload,
        });

        // and other properties are not modified
        expect(newState.other).toEqual(otherObject);
      });
    }
  });

  describe('SCM_ONBOARDING_SHOW_HOST_DIALOG', () => {
    for (let payload in [true, false]) {
      it('sets the dialog visible state ' + payload, function () {
        // given a state with errors
        const state = Object.freeze({
          other: otherObject,
          viewState: {},
        });

        // when reduce is invoked
        const newState = reduce(state, {
          type: SCM_ONBOARDING_SHOW_HOST_DIALOG,
          payload,
        });

        // then state is updated
        expect(newState.viewState).toEqual({
          isGitHostDialogVisible: payload,
        });

        // and other properties are not modified
        expect(newState.other).toEqual(otherObject);
      });
    }
  });

  describe('SCM_ONBOARDING_IS_IMPORT_STATUS_MODAL_VISIBLE', () => {
    for (let payload in [true, false]) {
      it('sets the dialog visible state ' + payload, function () {
        // given a state with errors
        const state = Object.freeze({
          other: otherObject,
          viewState: {},
        });

        // when reduce is invoked
        const newState = reduce(state, {
          type: SCM_ONBOARDING_IS_IMPORT_STATUS_MODAL_VISIBLE,
          payload,
        });

        // then state is updated
        expect(newState.viewState).toEqual({
          isImportStatusDialogVisible: payload,
        });

        // and other properties are not modified
        expect(newState.other).toEqual(otherObject);
      });
    }
  });

  describe('UI_ROUTER_ON_FINISH', () => {
    it('retains only configState', () => {
      // given a state with lots of values set
      const state = Object.freeze({
        viewState: {
          customProp: 'viewValue',
        },
        formState: {
          customProp: 'formValue',
        },
        sortConfiguration: {
          customProp: 'sortValue',
        },
        rootCustomProp: {
          mykey: 'rootCustomPropValue',
        },
      });

      const payload = {
        toState: {
          name: 'scmOnboarding',
        },
        toParams: {
          organizationId: 'org1',
        },
      };

      const newState = reduce(state, {
        type: UI_ROUTER_ON_FINISH,
        payload: payload,
      });

      // then state is reset
      expect(newState.formState).toEqual({
        organizations: [],
        selectedOrganization: null,
        preselectedOrganizationId: null,
        repositories: [],
        selectedRepositoryCount: 0,
        importedRepositoryCount: 0,
        totalRepositories: 0,
        newlyImportedRepos: [],
        defaultHostUrl: '',
        currentHostUrlState: textInputStateHelpers.initialState(''),
        failedImportCount: 0,
        failedRepos: [],
      });
      expect(newState.viewState).toEqual({
        loadingPage: false,
        loadingRepositories: false,
        validatingCompositeSourceControl: false,
        isGitHostNeeded: false,
        isGitHostDialogVisible: false,
        isSelectingOrganization: false,
        isImportStatusDialogVisible: false,
        isImporting: false,

        generalError: null,
        loadRepositoriesErrorCode: null,
        isNewOrganizationModalVisible: false,
        addOrganizationError: null,
      });
      expect(newState.sortConfiguration).toEqual({
        key: 'namespace',
        sortingOrder: ['namespace', 'project', 'description', 'defaultBranch'],
        dir: 'asc',
      });
      expect(newState.rootCustomProp).toBeUndefined();

      // and configState is retained
      expect(newState.configState).toEqual(state.configState);
    });

    it('retains all state when organizationId changes', () => {
      // given a state with lots of values set
      const state = Object.freeze({
        viewState: {
          customProp: 'viewValue',
        },
        formState: {
          customProp: 'formValue',
        },
        sortConfiguration: {
          customProp: 'sortValue',
        },
        rootCustomProp: {
          mykey: 'rootCustomPropValue',
        },
      });

      // retains state when not 'leaving' scmOnboarding
      const payload = {
        toState: {
          name: 'scmOnboardingOrg',
        },
        fromState: {
          name: 'scmOnboardingOrg',
        },
      };

      const newState = reduce(state, {
        type: UI_ROUTER_ON_FINISH,
        payload: payload,
      });

      // then state is retained
      expect(newState.formState).toEqual(state.formState);
      expect(newState.viewState).toEqual(state.viewState);
      expect(newState.sortConfiguration).toEqual(state.sortConfiguration);
      expect(newState.rootCustomProp).toEqual(state.rootCustomProp);
      expect(newState.configState).toEqual(state.configState);
    });
  });
});
