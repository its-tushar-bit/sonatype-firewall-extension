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
      expect(newState.isScmOnboardingFeatureEnabled).toBe(true);
      expect(newState.loadingConfig).toBe(false);

      // and other properties are not modified
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED action', function() {
    it('populates state repositories list', function() {
      // given empty repositories list
      const state = Object.freeze({
        other: otherObject,
        loadingRepositories: true,
        repositories: []
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
      expect(newState.repositories).toBe(repositoriesPayload.availableRepositories);
      expect(newState.totalRepositories).toBe(repositoriesPayload.totalRepositories);
      expect(newState.loadingRepositories).toBe(false);

      // and other properties are not modified
      expect(newState.other).toBe(otherObject);
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
          loadingOrganizations: true,
          organizations: [],
          preselectedOrganizationId: testdata[i].preselectedOrganizationId
        });

        // when reduce is invoked
        const newState = reduce(state, {
          type: 'SCM_ONBOARDING_LOAD_ORGANIZATIONS_FULFILLED',
          payload: organizationsPayload
        });

        // then state is updated
        expect(newState.organizations).toBe(organizationsPayload);
        expect(newState.loadingOrganizations).toBe(false);
        expect(newState.selectedOrganization).toBe(testdata[i].expectedOrg);

        // and other properties are not modified
        expect(newState.other).toBe(otherObject);
      });
    }
  });

  describe('SCM_ONBOARDING_SET_TARGET_ORGANIZATION action', function() {
    it('populates selected organization', function() {
      // given no organization is selected
      const state = Object.freeze({
        other: otherObject,
        selectedOrganization: null
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
      expect(newState.selectedOrganization).toBe(selectedOrganization);

      // and other properties are not modified
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED action', function() {
    it('populates state repositories list', function() {
      // given empty repositories list
      const state = Object.freeze({
        other: otherObject,
        repositories: []
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
      expect(newState.repositories).toBe(repositoriesPayload.availableRepositories);
      expect(newState.totalRepositories).toBe(repositoriesPayload.totalRepositories);

      // and other properties are not modified
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_FULFILLED action', function() {
    it('sets the default host url state', function() {
      // given a clean host URL state
      const state = Object.freeze({
        other: otherObject,
        defaultHostUrl: '',
        currentHostUrl: ''
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
      expect(newState.defaultHostUrl).toEqual('https://github.com/');
      expect(newState.currentHostUrl).toEqual('https://github.com/');

      // and other properties are not modified
      expect(newState.other).toBe(otherObject);
    });

    it('doesn\'t override currentHostUrl', function() {
      // given a dirty host URL state
      const state = Object.freeze({
        other: otherObject,
        defaultHostUrl: '',
        currentHostUrl: 'http://example.com'
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
      expect(newState.defaultHostUrl).toEqual('https://github.com/');
      expect(newState.currentHostUrl).toEqual('http://example.com');

      // and other properties are not modified
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('SCM_ONBOARDING_SET_CURRENT_HOST_URL action', function() {
    it('sets the default host URL', function() {
      // given a clean host URL
      const state = Object.freeze({
        other: otherObject,
        currentHostUrl: ''
      });

      const defaultHostPayload = 'https://github.com/';

      // when reduce is invoked
      const newState = reduce(state, {
        type: 'SCM_ONBOARDING_SET_CURRENT_HOST_URL',
        payload: defaultHostPayload
      });

      // then state is updated
      expect(newState.currentHostUrl).toEqual('https://github.com/');

      // and other properties are not modified
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('SCM_ONBOARDING_LOAD_COMPOSITE_SCM_FULFILLED action', function() {

    dataDrivenCompositeSourceControlTest({token: {value: 'token'}}, true);
    dataDrivenCompositeSourceControlTest({token: {value: null}}, false);
    dataDrivenCompositeSourceControlTest({token: {parentValue: 'token'}}, true);

    function dataDrivenCompositeSourceControlTest(compositeSourceControlPayload, expectedValue) {
      it(`sets scmTokenConfigured field to ${expectedValue}`, function() {
        // given previous state with token
        const state = Object.freeze({
          other: otherObject,
          scmTokenConfigured: false
        });

        // when reduce is invoked
        const newState = reduce(state, {
          type: 'SCM_ONBOARDING_LOAD_COMPOSITE_SCM_FULFILLED',
          payload: compositeSourceControlPayload
        });

        // then state is updated
        expect(newState.scmTokenConfigured).toBe(expectedValue);

        // and other properties are not modified
        expect(newState.other).toBe(otherObject);
      });
    }
  });
});
