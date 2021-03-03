/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../../main/frontend/configuration/scmOnboarding/scmOnboardingReducer';
import { initialState } from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import * as textInputStateHelpers from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import {UI_ROUTER_ON_FINISH} from '../../../../main/frontend/reduxUiRouter/routerActions';
import {
  SCM_ONBOARDING_IS_GIT_HOST_NEEDED,
  SCM_ONBOARDING_LOAD_PAGE_FULFILLED,
  SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FAILED,
  SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED,
  SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED,
  SCM_ONBOARDING_SHOW_HOST_DIALOG
} from '../../../../main/frontend/configuration/scmOnboarding/scmOnboardingActions';
import ownerConstant from '../../../../main/frontend/utility/services/owner.constant';

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
    let previousState, defaultOrganizationsPayloadWithoutRoot, defaultOrganizationsPayload;
    const rootOrgPayload = {
      organization: {
        'name': 'Root Organization',
        'id': ownerConstant.ROOT_ORGANIZATION_ID
      },
      sourceControl: {provider: 'github', token: {value: 'redacted token'}}
    };

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
      defaultOrganizationsPayloadWithoutRoot = [
        {
          organization: {
            'name': 'name0',
            'id': 'id0'
          },
          sourceControl: {provider: 'github', token: {value: null}}
        },
        {
          organization: {
            'name': 'name1',
            'id': 'id1'
          },
          sourceControl: {provider: 'github', token: {value: null}}
        }
      ];
      defaultOrganizationsPayload = [...defaultOrganizationsPayloadWithoutRoot, rootOrgPayload];
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
          hostUrlResult: {defaultHostUrl: 'http://localhost/'}
        };

        // when reduce is invoked
        const newState = reduce(state, {type: SCM_ONBOARDING_LOAD_PAGE_FULFILLED, payload: payload});

        // then state is updated
        expect(newState).toEqual({
          other: otherObject,
          viewState: {
            loadingPage: false,
            isGitHostNeeded: false,
            isGitHostDialogVisible: false,
            isSelectingOrganization: false
          },
          configState: {
            isScmOnboardingFeatureEnabled: true,
            isScmTokenConfigured: true,
            isScmTokenOverridden: false,
            scmProvider: 'github'
          },
          formState: {
            selectedOrganization: defaultOrganizationsPayload[1],
            organizations: defaultOrganizationsPayloadWithoutRoot,
            defaultHostUrl: 'http://localhost/',
            currentHostUrlState: initialState('http://localhost/'),
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
          hostUrlResult: {defaultHostUrl: 'http://localhost/'}
        };

        // when reduce is invoked
        const newState = reduce(state, {type: 'SCM_ONBOARDING_LOAD_PAGE_FULFILLED', payload: payload});

        // then state is updated and the git host is still needed
        expect(newState).toEqual({
          other: otherObject,
          viewState: {
            loadingPage: false,
            isGitHostNeeded: false,
            isGitHostDialogVisible: false,
            isSelectingOrganization: false
          },
          configState: {
            isScmOnboardingFeatureEnabled: true,
            isScmTokenConfigured: true,
            isScmTokenOverridden: false,
            scmProvider: 'github'
          },
          formState: {
            selectedOrganization: defaultOrganizationsPayload[1],
            organizations: defaultOrganizationsPayloadWithoutRoot,
            defaultHostUrl: 'http://localhost/',
            currentHostUrlState: initialState('http://localhost/'),
            preselectedOrganizationId: 'id1'
          }
        });
      });

      it('does not show the host dialog when no org has been selected', () => {
        // given an initial state where there is no preselected organization
        const state = Object.freeze({
          ...previousState,
          formState: {
            ...previousState,
            preselectedOrganizationId: null
          }
        });

        // and an initial load with no host URL or selected org
        const payload = {
          configResults: { scmOnboardingFeatureEnabled: true },
          organizationsResults: defaultOrganizationsPayload,
          compositeSourceControlResults: null,
          hostUrlResult: {defaultHostUrl: ''}
        };

        // when reduce is invoked
        const newState = reduce(state, {type: 'SCM_ONBOARDING_LOAD_PAGE_FULFILLED', payload: payload});

        // then state is updated and dialog is not shown
        expect(newState.viewState).toEqual({
          loadingPage: false,
          isGitHostNeeded: false,
          isGitHostDialogVisible: false,
          isSelectingOrganization: false
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
          isSelectingOrganization: false,
          validatingCompositeSourceControl: false,
          loadRepositoriesAuthError: null,
          isNewOrganizationModalVisible: false,
          addOrganizationError: null,
          generalError: {
            status: 502
          },
          isGitHostNeeded: false,
          isGitHostDialogVisible: false
        });
      });
    });
  });

  describe('SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED action', function() {
    it('clears the error state', function() {
      // given a state with errors
      const state = Object.freeze({
        other: otherObject,
        viewState: {
          loadingRepositories: false,
          generalError: 'general error',
          loadRepositoriesAuthError: 'auth error'
        },
        formState: {
          repositories: ['a'],
          totalRepositories: 1,
          importedRepositoryCount: 1,
          selectedRepositoryCount: 1
        }
      });

      // when reduce is invoked
      const newState = reduce(state, {
        type: 'SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED',
        payload: undefined
      });

      // then state is updated
      expect(newState.formState).toEqual({
        repositories: [],
        totalRepositories: 0,
        importedRepositoryCount: 0,
        selectedRepositoryCount: 0
      });
      expect(newState.viewState.loadingRepositories).toBe(true);
      expect(newState.viewState.generalError).toBe(null);
      expect(newState.viewState.loadRepositoriesAuthError).toBe(null);

      // and other properties are not modified
      expect(newState.other).toEqual(otherObject);
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
          repositories: [{project: 'project', namespace: 'namespace', description: 'description'}]
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
          loadRepositoriesAuthError: null,
          isGitHostDialogVisible: false
        },
        formState: {
          repositories: null
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
        generalError: null,
        isGitHostDialogVisible: true
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
        generalError: null,
        isGitHostDialogVisible: true
      });
    });
  });

  describe('SCM_ONBOARDING_SET_TARGET_ORGANIZATION action', function() {
    describe('FULFILLED', () => {
      it('populates selected organization', function() {
        // given no organization is selected
        const state = Object.freeze({
          other: otherObject,
          configState: {
            isScmTokenOverridden: true
          },
          formState: {
            selectedOrganization: null
          },
          viewState: {
          }
        });

        const selectedOrganization = {
          organization: {
            'project': 'project',
            'namespace': 'namespace',
            'description': 'description'
          },
          sourceControl: {
            token: {value: 'redacted'}
          }
        };

        const defaultHostUrl = 'http://localhost:1234/';

        // when reduce is invoked
        const newState = reduce(state, {
          type: SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED,
          payload: {selectedOrganization, defaultHostUrl}
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
              viewState: {}
            },
            newState: {
              configState: {
                isScmTokenOverridden: true
              },
              viewState: {
                defaultHostUrl: ''
              },
              formState: {
                selectedOrganization: {sourceControl: {token: {value: 'redacted'}}}
              }
            },
            expectedValue: true
          },
          {
            description: 'org is defined with no custom token, but prev state had a custom token => show dialog',
            prevState: {
              configState: {
                isScmTokenOverridden: true
              },
              formState: {},
              viewState: {}
            },
            newState: {
              configState: {
                isScmTokenOverridden: false
              },
              viewState: {
                defaultHostUrl: ''
              },
              formState: {
                selectedOrganization: {sourceControl: {token: {value: null}}}
              }
            },
            expectedValue: true
          }
        ];

        for (let currTest of testData) {
          it(currTest.description, function() {
            // when reduce is invoked
            const newState = reduce(currTest.prevState, {
              type: SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED,
              payload: {
                selectedOrganization: currTest.newState.formState.selectedOrganization,
                defaultHostUrl: currTest.newState.viewState.defaultHostUrl
              }
            });

            // then state is updated
            expect(newState.formState).toEqual(jasmine.objectContaining(currTest.newState.formState));
            expect(newState.viewState).toEqual(jasmine.objectContaining({
              isGitHostNeeded: currTest.expectedValue,
              isGitHostDialogVisible: currTest.expectedValue
            }));
          });
        }
      });

      describe('calculates a suggested host URL:', () => {
        const providerData = [
          {provider: 'github', url: 'https://github.com/'},
          {provider: 'gitlab', url: 'https://gitlab.com/'},
          {provider: 'bitbucket', url: 'https://bitbucket.org/'}
        ];
        for (let testData of providerData) {
          it('defaults to ' + testData.url + ' when provider is ' + testData.provider, function() {
            // given empty repositories list
            const state = Object.freeze({
              other: otherObject,
              configState: {
                scmProvider: testData.provider
              },
              viewState: {
                isSelectingOrganization: true
              },
              formState: {
              }
            });

            // when reduce is invoked without an identified URL
            const selectedOrganization = {sourceControl: {token: {value: null}}};
            const newState = reduce(state, {
              type: SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FULFILLED,
              payload: {
                selectedOrganization: selectedOrganization,
                defaultHostUrl: ''
              }
            });

            // then current host URL state is updated to the provider defaults
            expect(newState.formState).toEqual({
              defaultHostUrl: '',
              currentHostUrlState: initialState(testData.url),
              selectedOrganization: selectedOrganization
            });
            expect(newState.viewState).toEqual({
              isSelectingOrganization: false,
              isGitHostNeeded: true,
              isGitHostDialogVisible: true
            });

            // and other properties are not modified
            expect(newState.other).toEqual(otherObject);
          });
        }
      });
    });

    describe('REQUESTED action', function() {
      it('sets the loading state', function() {
        // given a state with errors
        const state = Object.freeze({
          other: otherObject,
          viewState: {
            isSelectingOrganization: false
          }
        });

        // when reduce is invoked
        const newState = reduce(state, {
          type: SCM_ONBOARDING_SET_TARGET_ORGANIZATION_REQUESTED
        });

        // then state is updated
        expect(newState.viewState).toEqual({
          isSelectingOrganization: true
        });

        // and other properties are not modified
        expect(newState.other).toEqual(otherObject);
      });
    });

    describe('FAILED action', function() {
      it('sets generalError field to value of error', function() {
        // given an initial state
        const state = Object.freeze({
          viewState: {
            isSelectingOrganization: true,
            generalError: null
          },
          other: otherObject
        });
        const errorResponse = {response: {status: 502}};

        // when the reducer is invoked
        const newState = reduce(state, {
          type: SCM_ONBOARDING_SET_TARGET_ORGANIZATION_FAILED,
          payload: errorResponse
        });

        // the state is updated as expected
        expect(newState).toEqual({
          viewState: {
            isSelectingOrganization: false,
            generalError: errorResponse
          },
          other: otherObject
        });
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });
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

  describe('SCM_ONBOARDING_IS_GIT_HOST_NEEDED', () => {
    for (let payload in [true, false]) {
      it('sets the loading state using ' + payload, function() {
        // given a state with errors
        const state = Object.freeze({
          other: otherObject,
          viewState: {
          }
        });

        // when reduce is invoked
        const newState = reduce(state, {
          type: SCM_ONBOARDING_IS_GIT_HOST_NEEDED,
          payload
        });

        // then state is updated
        expect(newState.viewState).toEqual({
          isGitHostNeeded: payload
        });

        // and other properties are not modified
        expect(newState.other).toEqual(otherObject);
      });
    }
  });

  describe('SCM_ONBOARDING_SHOW_HOST_DIALOG', () => {
    for (let payload in [true, false]) {
      it('sets the dialog visible state ' + payload, function() {
        // given a state with errors
        const state = Object.freeze({
          other: otherObject,
          viewState: {
          }
        });

        // when reduce is invoked
        const newState = reduce(state, {
          type: SCM_ONBOARDING_SHOW_HOST_DIALOG,
          payload
        });

        // then state is updated
        expect(newState.viewState).toEqual({
          isGitHostDialogVisible: payload
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
        configState: {
          isScmOnboardingFeatureEnabled: true,
          customProp: 'configValue'
        },
        viewState: {
          customProp: 'viewValue'
        },
        formState: {
          customProp: 'formValue'
        },
        sortConfiguration: {
          customProp: 'sortValue'
        },
        rootCustomProp: {
          mykey: 'rootCustomPropValue'
        }
      });

      const newState = reduce(state, { type: UI_ROUTER_ON_FINISH });

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
        failedImportCount: 0
      });
      expect(newState.viewState).toEqual({
        loadingPage: false,
        loadingRepositories: false,
        validatingCompositeSourceControl: false,
        isGitHostNeeded: false,
        isGitHostDialogVisible: false,
        isSelectingOrganization: false,

        generalError: null,
        loadRepositoriesAuthError: null,
        isNewOrganizationModalVisible: false,
        addOrganizationError: null
      });
      expect(newState.sortConfiguration).toEqual({
        key: 'namespace',
        sortingOrder: ['namespace', 'project', 'description'],
        dir: 'asc'
      });
      expect(newState.rootCustomProp).toBeUndefined();

      // and configState is retained
      expect(newState.configState).toEqual(state.configState);
    });
  });

  describe('organization creation reducers', () => {

    it('create organization fulfilled', () => {

      const createOrgPayload = {
        organization: { 'name': 'My Organization 3', 'id': 'id3'},
        sourceControl: {provider: 'github', token: {value: 'redacted token'}}
      };

      const existingOrganizations = [
        {
          organization: { 'name': 'My Organization 1', 'id': 'id1'},
          sourceControl: {provider: 'github', token: {value: 'redacted token'}}
        },
        {
          organization: { 'name': 'My Organization 2', 'id': 'id2'},
          sourceControl: {provider: 'github', token: {value: 'redacted token'}}
        }];

      const state = Object.freeze({
        formState: {
          organizations: existingOrganizations
        },
        viewState: {
          addOrganizationError: 'crash bang',
          isNewOrganizationModalVisible: true
        }
      });

      // when reducer is invoked
      const newState = reduce(state, {type: 'SCM_ONBOARDING_ADD_ORGANIZATION_FULFILLED', payload: createOrgPayload});

      // then the list of orgs is updated
      expect(newState.formState.organizations).toEqual([...existingOrganizations, createOrgPayload]);
      // and error message is reset
      expect(newState.viewState.addOrganizationError).toBeNull();
      // and the modal dialog is closed
      expect(newState.viewState.isNewOrganizationModalVisible).toBeFalsy();
    });

    it('create organization failed', () => {
      const errorPayload = {error: 'error'};

      const state = Object.freeze({
        viewState: {
          addOrganizationError: null
        }
      });

      // when reducer is invoked
      const newState = reduce(state, {type: 'SCM_ONBOARDING_ADD_ORGANIZATION_FAILED', payload: errorPayload});

      // then error is stored
      expect(newState.viewState.addOrganizationError).toEqual(errorPayload);
    });

    it('sets new organization modal visibility', () => {

      const state = Object.freeze({
        viewState: {
          isNewOrganizationModalVisible: false
        }
      });

      // when reducer is invoked
      const newState = reduce(state, {type: 'SCM_ONBOARDING_SET_IS_NEW_ORGANIZATION_MODAL_VISIBLE', payload: true});

      // the modal visibility is updated
      expect(newState.viewState.isNewOrganizationModalVisible).toBeTruthy();
    });
  });
});
