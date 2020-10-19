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

  describe('SCM_ONBOARDING_REPOSITORY_SELECTION_CHANGED action', function() {
    it('increments after a selection changed event', function() {
      // TODO flesh this out in INT-3479. Not sure if we even want this to be in the reducer at all
      const state = Object.freeze({selectedRepositoryCount: 1});

      // when reduce is invoked
      const newState = reduce(state, {
        type: 'SCM_ONBOARDING_REPOSITORY_SELECTION_CHANGED',
        payload: {
          isSelected: true
        }
      });

      // then count is updated
      expect(newState.selectedRepositoryCount).toBe(2);
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

      const repositoriesPayload = [{
        'project': 'project',
        'namespace': 'namespace',
        'description': 'description'
      }];

      // when reduce is invoked
      const newState = reduce(state, {
        type: 'SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED',
        payload: repositoriesPayload
      });

      // then state is updated
      expect(newState.repositories).toBe(repositoriesPayload);
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

      const repositoriesPayload = [{
        'project': 'project',
        'namespace': 'namespace',
        'description': 'description'
      }];

      // when reduce is invoked
      const newState = reduce(state, {
        type: 'SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED',
        payload: repositoriesPayload
      });

      // then state is updated
      expect(newState.repositories).toBe(repositoriesPayload);

      // and other properties are not modified
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('SCM_ONBOARDING_LOAD_ORG_DEFAULT_HOST_URL_FULFILLED action', function() {
    it('sets the default host url state', function() {
      // given a clean host URL state
      const state = Object.freeze({
        other: otherObject,
        defaultHostUrlState: {isPristine: true, value: ''}
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
      expect(newState.defaultHostUrlState).toEqual({
        isPristine: false,
        value: 'https://github.com/'
      });

      // and other properties are not modified
      expect(newState.other).toBe(otherObject);
    });
  });
});
