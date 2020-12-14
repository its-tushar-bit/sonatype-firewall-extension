/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../../main/frontend/configuration/scmOnboarding/scmOnboardingReducer';
import { initialState } from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';

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

  describe('SCM_ONBOARDING_LOAD_CONFIG_FULFILLED action', function() {
    it('populates state from configuration', function() {
      // given SCM configuration from IQ server
      const state = Object.freeze({
        other: otherObject,
        loadingConfig: true,
        scmOnboardingFeatureEnabled: false
      });

      // when reduce is invoked
      const newState = reduce(state, {
        type: 'SCM_ONBOARDING_LOAD_CONFIG_FULFILLED',
        payload: {
          scmOnboardingFeatureEnabled: true
        }
      });

      // then state is updated
      expect(newState.configState.isScmOnboardingFeatureEnabled).toBe(true);
      expect(newState.viewState.loadingConfig).toBe(false);

      // and other properties are not modified
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('SCM_ONBOARDING_LOAD_CONFIG_FAILED action', function() {
    it('sets lastErrorMessage field to value of Error.message', function() {
      const state = Object.freeze({
        viewState: {
          loadingConfig: true,
          lastErrorMessage: null
        },
        configState: {
          isScmOnboardingFeatureEnabled: true
        },
        other: otherObject
      });

      const newState = reduce(state, {type: 'SCM_ONBOARDING_CONFIG_LOAD_FAILED', payload: {status: 502}});

      expect(newState).toEqual({
        viewState: {
          loadingConfig: false,
          lastErrorMessage: 'Bad Gateway'
        },
        configState: {
          isScmOnboardingFeatureEnabled: null
        },
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
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
    it('sets lastErrorMessage field to value of Error.message', function() {
      // given an initial state
      const state = Object.freeze({
        viewState: {
          loadingRepositories: true,
          lastErrorMessage: null
        },
        formState: {
          repositories: [{project: 'project', namespace: 'namespace', description: 'description'}],
          totalRepositories: 1,
          importedRepositoryCount: 1,
          selectedRepositoryCount: 1
        },
        other: otherObject
      });

      // when the reducer is invoked
      const newState = reduce(state, {type: 'SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED', payload: {status: 502}});

      // the state is updated as expected
      expect(newState).toEqual({
        viewState: {
          loadingRepositories: false,
          lastErrorMessage: 'Bad Gateway'
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
  });

  describe('SCM_ONBOARDING_LOAD_ORGANIZATIONS_FULFILLED action', function() {
    const organizationsPayload = [{
      'name': 'name0',
      'id': 'id0'
    }, {
      'name': 'name1',
      'id': 'id1'
    }];

    let testdata = [{
      preselectedOrganizationId: 'id1',
      expectedOrg: organizationsPayload[1]
    }, {
      preselectedOrganizationId: undefined,
      expectedOrg: undefined
    }];

    for (let i in testdata) {
      it('populates state organizations list (' + testdata[i].preselectedOrganizationId + ')', function() {
        // given empty organizations list
        const state = Object.freeze({
          other: otherObject,
          viewState: {
            loadingOrganizations: true
          },
          formState: {
            organizations: [],
            preselectedOrganizationId: testdata[i].preselectedOrganizationId
          }
        });

        // when reduce is invoked
        const newState = reduce(state, {
          type: 'SCM_ONBOARDING_LOAD_ORGANIZATIONS_FULFILLED',
          payload: organizationsPayload
        });

        // then state is updated
        expect(newState.formState.organizations).toBe(organizationsPayload);
        expect(newState.viewState.loadingOrganizations).toBe(false);
        expect(newState.formState.selectedOrganization).toBe(testdata[i].expectedOrg);

        // and other properties are not modified
        expect(newState.other).toBe(otherObject);
      });
    }
  });

  describe('SCM_ONBOARDING_LOAD_ORGANIZATIONS_FAILED action', function() {
    it('sets lastErrorMessage field to value of Error.message', function() {
      // given an initial state
      const state = Object.freeze({
        viewState: {
          loadingOrganizations: true,
          lastErrorMessage: null
        },
        formState: {
          organizations: [{name: 'name', id: 'id'}],
          selectedOrganization: 'id'
        },
        other: otherObject
      });

      // when the reducer is invoked
      const newState = reduce(state, {type: 'SCM_ONBOARDING_CONFIG_ORGS_FAILED', payload: {status: 502}});

      // the state is updated as expected
      expect(newState).toEqual({
        viewState: {
          loadingOrganizations: false,
          lastErrorMessage: 'Bad Gateway'
        },
        formState: {
          organizations: null,
          selectedOrganization: null
        },
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
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

  describe('SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_FULFILLED action', function() {
    it('sets the default host url state', function() {
      // given a clean host URL state
      const state = Object.freeze({
        other: otherObject,
        formState: {
          defaultHostUrl: '',
          currentHostUrlState: initialState('')
        }
      });

      const defaultHostPayload = {
        'defaultHostUrl': 'https://github.com/'
      };

      // when reduce is invoked
      const newState = reduce(state, {
        type: 'SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_FULFILLED',
        payload: defaultHostPayload
      });

      // then state is updated
      expect(newState.formState.defaultHostUrl).toEqual('https://github.com/');
      expect(newState.formState.currentHostUrlState).toEqual(initialState('https://github.com/'));

      // and other properties are not modified
      expect(newState.other).toBe(otherObject);
    });

    it('doesn\'t override currentHostUrlState', function() {
      // given a dirty host URL state
      const state = Object.freeze({
        other: otherObject,
        formState: {
          defaultHostUrl: '',
          currentHostUrlState: initialState('http://example.com')
        }
      });

      const defaultHostPayload = {
        'defaultHostUrl': 'https://github.com/'
      };

      // when reduce is invoked
      const newState = reduce(state, {
        type: 'SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_FULFILLED',
        payload: defaultHostPayload
      });

      // then default host url is updated
      expect(newState.formState.defaultHostUrl).toEqual('https://github.com/');
      expect(newState.formState.currentHostUrlState).toEqual(initialState('http://example.com'));

      // and other properties are not modified
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('SCM_ONBOARDING_LOAD_ORGANIZATIONS_FAILED action', function() {
    it('sets lastErrorMessage field to value of Error.message', function() {
      // given an initial state
      const state = Object.freeze({
        viewState: {
          lastErrorMessage: null
        },
        formState: {
          defaultHostUrl: 'http://example.com/',
          currentHostUrlState: 'http://example.org/'
        },
        other: otherObject
      });

      // when the reducer is invoked
      const newState = reduce(state, {type: 'SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_FAILED', payload: {status: 502}});

      // the state is updated as expected
      expect(newState).toEqual({
        viewState: {
          lastErrorMessage: 'Bad Gateway'
        },
        formState: {
          defaultHostUrl: '',
          currentHostUrlState: initialState('')
        },
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
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

  describe('SCM_ONBOARDING_LOAD_COMPOSITE_SCM_FULFILLED action', function() {

    dataDrivenCompositeSourceControlTest({provider: 'scmProvider', token: {value: 'token'}}, true);
    dataDrivenCompositeSourceControlTest({provider: 'scmProvider', token: {value: null}}, false);
    dataDrivenCompositeSourceControlTest({provider: 'scmProvider', token: {parentValue: 'token'}}, true);

    function dataDrivenCompositeSourceControlTest(compositeSourceControlPayload, expectedValue) {
      it(`sets scmTokenConfigured field to ${expectedValue}`, function() {
        // given previous state with token
        const state = Object.freeze({
          other: otherObject,
          viewState: {
            loadingCompositeSourceControl: true
          },
          configState: {
            isScmTokenConfigured: false,
            scmProvider: ''
          }
        });

        // when reduce is invoked
        const newState = reduce(state, {
          type: 'SCM_ONBOARDING_LOAD_COMPOSITE_SCM_FULFILLED',
          payload: compositeSourceControlPayload
        });

        // then SCM token presence value is updated
        expect(newState.configState.isScmTokenConfigured).toBe(expectedValue);

        // and loading state is updated
        expect(newState.viewState.loadingCompositeSourceControl).toBe(false);

        // and provider is set
        expect(newState.configState.scmProvider).toBe('scmProvider');

        // and other properties are not modified
        expect(newState.other).toBe(otherObject);
      });
    }
  });

  describe('SCM_ONBOARDING_LOAD_COMPOSITE_SCM_FAILED action', function() {
    it('sets lastErrorMessage field to value of Error.message', function() {
      const state = Object.freeze({
        viewState: {
          loadingCompositeSourceControl: null,
          lastErrorMessage: null
        },
        configState: {
          isScmTokenConfigured: null
        },
        other: otherObject
      });

      const newState = reduce(state, {type: 'SCM_ONBOARDING_LOAD_COMPOSITE_SCM_FAILED', payload: {status: 502}});

      expect(newState).toEqual({
        viewState: {
          loadingCompositeSourceControl: false,
          lastErrorMessage: 'Bad Gateway'
        },
        configState: {
          isScmTokenConfigured: null,
          scmProvider: null
        },
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
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
      it('sets lastErrorMessage field to value of Error.message', function() {
        const state = Object.freeze({
          viewState: {
            lastErrorMessage: null
          },
          other: otherObject
        });

        const newState = reduce(state, {type: 'SCM_ONBOARDING_IMPORT_REPOS_FAILED', payload: {status: 502}});

        expect(newState).toEqual({
          viewState: {
            lastErrorMessage: 'Bad Gateway'
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
      it('sets lastErrorMessage field to value of Error.message', function() {
        const state = Object.freeze({
          viewState: {
            validatingCompositeSourceControl: true,
            lastErrorMessage: null
          },
          other: otherObject
        });

        const newState = reduce(state, {
          type: 'SCM_ONBOARDING_VALIDATE_SCM_HOST_URL_FAILED',
          payload: {status: 404}
        });

        expect(newState).toEqual({
          viewState: {
            validatingCompositeSourceControl: false,
            lastErrorMessage: 'Error 404'
          },
          other: otherObject
        });
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });
    });
  });
});
