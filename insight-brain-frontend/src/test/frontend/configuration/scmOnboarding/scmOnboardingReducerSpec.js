/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../../main/frontend/configuration/scmOnboarding/scmOnboardingReducer';
import { initialState } from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import * as textInputStateHelpers from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';

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

  describe('SCM_ONBOARDING_LOAD_PAGE', function() {
    let previousState, defaultOrganizationsPayload;

    beforeEach(() => {
      previousState = {
        other: otherObject,
        viewState: {
          loadingPage: true
        },
        configState: {
          isScmTokenConfigured: null,
          scmProvider: null
        },
        formState: {
          selectedOrganization: null,
          organizations: null,
          defaultHostUrl: null,
          currentHostUrlState: textInputStateHelpers.initialState(''),
          preselectedOrganizationId: 'id1'
        }
      };
      defaultOrganizationsPayload = [{
        'name': 'name0',
        'id': 'id0'
      }, {
        'name': 'name1',
        'id': 'id1'
      }];
    });

    describe('FULFILLED', function() {
      it('updates the state with the data loaded from IQ', function() {
        // given an initial state
        const state = Object.freeze(previousState);

        // and several orgs returned in the payload
        const payload = {
          configResults: { scmOnboardingFeatureEnabled: true },
          organizationsResults: defaultOrganizationsPayload,
          compositeSourceControlResults: {provider: 'github', token: {value: 'token'}},
          hostUrlResult: {defaultHostUrl: 'http://github.com/'}
        };

        // when reduce is invoked
        const newState = reduce(state, {type: 'SCM_ONBOARDING_LOAD_PAGE_FULFILLED', payload: payload});

        // then state is updated
        expect(newState).toEqual({
          other: otherObject,
          viewState: {
            loadingPage: false
          },
          configState: {
            isScmOnboardingFeatureEnabled: true,
            isScmTokenConfigured: true,
            isScmTokenOverridden: true,
            scmProvider: 'github'
          },
          formState: {
            selectedOrganization: defaultOrganizationsPayload[1],
            organizations: defaultOrganizationsPayload,
            defaultHostUrl: 'http://github.com/',
            currentHostUrlState: initialState('http://github.com/'),
            preselectedOrganizationId: 'id1'
          }
        });
      });

      it('updates the state with the data loaded from IQ with token in root organization', () => {
        // given an initial state
        const state = Object.freeze(previousState);

        // and the token is configured in the root organization
        const payload = {
          configResults: { scmOnboardingFeatureEnabled: true },
          organizationsResults: defaultOrganizationsPayload,
          compositeSourceControlResults: {provider: 'github', token: {parentValue: 'token'}},
          hostUrlResult: {defaultHostUrl: 'http://github.com/'}
        };

        // when reduce is invoked
        const newState = reduce(state, {type: 'SCM_ONBOARDING_LOAD_PAGE_FULFILLED', payload: payload});

        // then state is updated
        expect(newState).toEqual({
          other: otherObject,
          viewState: {
            loadingPage: false
          },
          configState: {
            isScmOnboardingFeatureEnabled: true,
            isScmTokenConfigured: true,
            isScmTokenOverridden: false,
            scmProvider: 'github'
          },
          formState: {
            selectedOrganization: defaultOrganizationsPayload[1],
            organizations: defaultOrganizationsPayload,
            defaultHostUrl: 'http://github.com/',
            currentHostUrlState: initialState('http://github.com/'),
            preselectedOrganizationId: 'id1'
          }
        });
      });
    });

    describe('FAILED', function() {
      it('sets generalError field to value of error', function() {
        const state = Object.freeze({
          viewState: {
            loadingPage: true,
            generalError: null
          },
          configState: {
            isScmOnboardingFeatureEnabled: null
          }
        });

        const newState = reduce(state, {type: 'SCM_ONBOARDING_LOAD_PAGE_FAILED', payload: {status: 502}});

        expect(newState.viewState).toEqual({
          loadingPage: false,
          loadingRepositories: false,
          validatingCompositeSourceControl: false,
          loadRepositoriesAuthError: null,
          generalError: {
            status: 502
          }
        });
      });
    });
  });

  describe('SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED action', function() {
    it('populates state repositories list', function() {
      // given empty repositories list
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          loadingRepositories: true
        },
        formState: {
          repositories: []
        },
        sortConfiguration: {
          key: 'namespace',
          sortingOrder: ['namespace'],
          dir: 'asc'
        }
      });

      const repositoriesPayload = {
        'totalRepositories': 1,
        'status': 'SUCCESS',
        'availableRepositories': [
          {
            'project': 'project',
            'namespace': 'namespace',
            'description': 'description'
          }
        ]
      };

      // when reduce is invoked
      const newState = reduce(state, {
        type: 'SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED',
        payload: repositoriesPayload
      });

      // then state is updated
      expect(newState.formState.repositories).toEqual(repositoriesPayload.availableRepositories);
      expect(newState.formState.totalRepositories).toEqual(repositoriesPayload.totalRepositories);
      expect(newState.viewState.loadingRepositories).toBe(false);

      // and other properties are not modified
      expect(newState.sortConfiguration).toBe(state.sortConfiguration);
      expect(newState.other).toEqual(otherObject);
    });

    it('sorts when repositories are populated', function() {
      // given empty repositories list
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          loadingRepositories: true
        },
        formState: {
          repositories: []
        },
        sortConfiguration: {
          sortingOrder: ['namespace']
        }
      });

      const repositoriesPayload = {
        'totalRepositories': 2,
        'status': 'SUCCESS',
        'availableRepositories': [
          {
            'namespace': 'b'
          },
          {
            'namespace': 'a'
          }
        ]
      };

      // when reduce is invoked
      const newState = reduce(state, {
        type: 'SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED',
        payload: repositoriesPayload
      });

      // then state is updated
      expect(newState.formState.repositories).toEqual([
        {
          'namespace': 'a'
        },
        {
          'namespace': 'b'
        }
      ]);
      expect(newState.formState.totalRepositories).toEqual(repositoriesPayload.totalRepositories);
      expect(newState.viewState.loadingRepositories).toBe(false);

      // and other properties are not modified
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED action', function() {
    it('sets generalError field to value of error', function() {
      // given an initial state
      const state = Object.freeze({
        viewState: {
          loadingRepositories: true,
          generalError: null
        },
        formState: {
          repositories: [{project: 'project', namespace: 'namespace', description: 'description'}],
          totalRepositories: 1,
          importedRepositoryCount: 1,
          selectedRepositoryCount: 1
        },
        other: otherObject
      });
      const errorResponse = {response: {status: 502}};

      // when the reducer is invoked
      const newState = reduce(state, {type: 'SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED', payload: errorResponse});

      // the state is updated as expected
      expect(newState).toEqual({
        viewState: {
          loadingRepositories: false,
          generalError: errorResponse,
          loadRepositoriesAuthError: null
        },
        formState: {
          repositories: null,
          totalRepositories: 0,
          importedRepositoryCount: 0,
          selectedRepositoryCount: 0
        },
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('sets loadRepositoriesAuthError field to value of error', function() {
      const state = Object.freeze({
        viewState: {
          loadingRepositories: true
        },
        configState: {
          scmProvider: 'provider'
        }
      });
      const response = {
        status: 'SCM_AUTHN_FAILURE'
      };

      const newState = reduce(state, {type: 'SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED', payload: response});

      expect(newState.viewState).toEqual({
        loadingRepositories: false,
        loadRepositoriesAuthError: new Error('Authentication with provider failed'),
        generalError: null
      });
    });

    it('sets loadRepositoriesAuthError field to value of error', function() {
      const state = Object.freeze({
        viewState: {
          loadingRepositories: true
        },
        configState: {
          scmProvider: 'provider'
        }
      });
      const response = {
        status: 'SCM_AUTHZ_FAILURE'
      };

      const newState = reduce(state, {type: 'SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED', payload: response});

      expect(newState.viewState).toEqual({
        loadingRepositories: false,
        loadRepositoriesAuthError: new Error('Permission denied by provider'),
        generalError: null
      });
    });
  });

  describe('SCM_ONBOARDING_SET_TARGET_ORGANIZATION action', function() {
    it('populates selected organization', function() {
      // given no organization is selected
      const state = Object.freeze({
        other: otherObject,
        formState: {
          selectedOrganization: null
        }
      });

      const selectedOrganization = {
        'project': 'project',
        'namespace': 'namespace',
        'description': 'description'
      };

      // when reduce is invoked
      const newState = reduce(state, {
        type: 'SCM_ONBOARDING_SET_TARGET_ORGANIZATION',
        payload: selectedOrganization
      });

      // then state is updated
      expect(newState.formState.selectedOrganization).toBe(selectedOrganization);

      // and other properties are not modified
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('SCM_ONBOARDING_SET_CURRENT_HOST_URL action', function() {

    // this table is a full mesh of 3 inputs 1 output
    const testDataTable = [{
      currentValue: 'https://example.com/foo/',
      payload: 'https://example.com/bar/',
      existingValidationErrors: null,
      expectedValidationErrors: null
    }, {
      currentValue: 'https://example.com/foo/',
      payload: 'https://example.com/bar/',
      existingValidationErrors: 'CRASH',
      expectedValidationErrors: 'CRASH'
    }, {
      currentValue: 'https://example.com/',
      payload: 'invalid',
      existingValidationErrors: 'BANG',
      expectedValidationErrors: 'Not a valid URL'
    }, {
      currentValue: 'https://example.com/',
      payload: 'invalid',
      existingValidationErrors: null,
      expectedValidationErrors: 'Not a valid URL'
    }, {
      currentValue: 'invalid',
      payload: 'https://example.com/bar/',
      existingValidationErrors: null,
      expectedValidationErrors: null
    }, {
      currentValue: 'invalid',
      payload: 'https://example.com/bar/',
      existingValidationErrors: 'ZAP',
      expectedValidationErrors: 'ZAP'
    }, {
      currentValue: 'invalid',
      payload: 'invalid',
      existingValidationErrors: [],
      expectedValidationErrors: 'Not a valid URL'
    }, {
      currentValue: 'invalid',
      payload: 'invalid',
      existingValidationErrors: 'KABOOM',
      expectedValidationErrors: 'Not a valid URL'
    }, {
      currentValue: 'h',
      payload: '',
      existingValidationErrors: 'BOING',
      expectedValidationErrors: null
    }];

    describe('validation', () => {
      for (const testData of testDataTable) {
        it('sets the default host URL', function() {
          // given a clean host URL
          const state = Object.freeze({
            other: otherObject,
            formState: {
              currentHostUrlState: {
                value: testData.currentValue,
                validationErrors: testData.existingValidationErrors
              }
            }
          });

          // when reduce is invoked
          const newState = reduce(state, {
            type: 'SCM_ONBOARDING_SET_CURRENT_HOST_URL',
            payload: testData.payload
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
    describe('partial success', () => {
      it('updates the new repository list', function() {
        // given previous state with token
        let initialRepos = [
          {httpCloneUrl: 'http://host/prj/a'},
          {httpCloneUrl: 'http://host/prj/b'},
          {httpCloneUrl: 'http://host/prj/c'},
          {httpCloneUrl: 'http://host/prj/d'},
          {httpCloneUrl: 'http://host/prj/e'},
          {httpCloneUrl: 'http://host/prj/f'}
        ];
        const state = Object.freeze({
          formState: {
            repositories: initialRepos,
            importedRepositoryCount: 0,
            importedRepos: [],
            selectedRepositoryCount: 1,
            newlyImportedRepos: [],
            failedImportCount: 0
          },
          other: otherObject
        });

        // when reduce is invoked
        let importedRepos = [
          {httpCloneUrl: 'http://host/prj/a'},
          {httpCloneUrl: 'http://host/prj/b'}
        ];
        const newState = reduce(state, {
          type: 'SCM_ONBOARDING_IMPORT_REPOS_FULFILLED',
          payload: {
            importedRepositories: importedRepos,
            failedImportCount: 3
          }
        });

        // then imported state is updated
        expect(newState.formState.importedRepositoryCount).toBe(2);
        expect(newState.formState.selectedRepositoryCount).toBe(0);
        expect(newState.formState.failedImportCount).toBe(3);
        expect(newState.formState.newlyImportedRepos).toBe(importedRepos);
        expect(newState.formState.repositories).toEqual([
          {httpCloneUrl: 'http://host/prj/c'},
          {httpCloneUrl: 'http://host/prj/d'},
          {httpCloneUrl: 'http://host/prj/e'},
          {httpCloneUrl: 'http://host/prj/f'}
        ]);

        // and other properties are not modified
        expect(newState.other).toBe(otherObject);
      });
    });

    describe('HTTP request fails', () => {
      it('sets generalError field to value of error', function() {
        const state = Object.freeze({
          viewState: {
            generalError: null
          },
          other: otherObject
        });
        const errorResponse = {response: {status: 502}};

        const newState = reduce(state, {type: 'SCM_ONBOARDING_IMPORT_REPOS_FAILED', payload: errorResponse});

        expect(newState).toEqual({
          viewState: {
            generalError: errorResponse
          },
          other: otherObject
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
              validationErrors: 'BOOM'
            }
          }
        });

        // when reduce is invoked
        const newState = reduce(state, {
          type: 'SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FULFILLED',
          payload: {
            isValid: true
          }
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
            currentHostUrlState: initialState('http://example.com/')
          }
        });

        // when reduce is invoked
        const newState = reduce(state, {
          type: 'SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FULFILLED',
          payload: {
            isValid: false,
            errorMessages: 'CRASH'
          }
        });

        // then validation errors is populated with error message
        expect(newState.formState.currentHostUrlState.validationErrors).toEqual('CRASH');

        // and other objects unchanged
        expect(newState.other).toBe(otherObject);
      });
    });

    describe('fails REST call', () => {
      it('sets generalError field to value of error', function() {
        const state = Object.freeze({
          viewState: {
            validatingCompositeSourceControl: true,
            generalError: null
          },
          other: otherObject
        });
        const errorResponse = {response: {status: 404}};

        const newState = reduce(state, {
          type: 'SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FAILED',
          payload: errorResponse
        });

        expect(newState).toEqual({
          viewState: {
            validatingCompositeSourceControl: false,
            generalError: errorResponse
          },
          other: otherObject
        });
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });
    });
  });
});
