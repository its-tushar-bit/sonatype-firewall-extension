/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/moveOwner/moveOwnerSlice';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';

describe('moveOwner reducer', () => {
  describe('moveOwner/loadAvailableToMoveOrganizations', () => {
    it('sets loading to true on /pending', () => {
      const state = Object.freeze({
        fetchOrgs: {
          loading: false,
          loadError: 'error',
        },
      });

      const {
        fetchOrgs: { loading, loadError },
      } = reducer(state, {
        type: `${OWNER_ACTIONS}/moveOwner/loadAvailableToMoveOrganizations/pending`,
      });

      expect(loading).toBe(true);
      expect(loadError).toBeNull();
    });

    it('sets organizations on /fulfilled', () => {
      const state = Object.freeze({
        fetchOrgs: {
          organizations: [],
          loadError: null,
          loading: false,
          isShowNoAvailableOrgsWarning: false,
        },
        selectedOrganization: null,
      });

      const { fetchOrgs, selectedOrganization } = reducer(state, {
        type: `${OWNER_ACTIONS}/moveOwner/loadAvailableToMoveOrganizations/fulfilled`,
        payload: {
          availableOrganizations: [
            { organizationId: '5f2a22b9360b4626985e706ed3502e1f', organizationName: 'Awesome org' },
            { organizationId: '0aba574e1c634737b7affd87a8789a0a', organizationName: 'New org' },
          ],
          selectedOrganization: { organizationId: '5f2a22b9360b4626985e706ed3502e1f', organizationName: 'Awesome org' },
        },
      });
      expect(selectedOrganization).toEqual({
        organizationId: '5f2a22b9360b4626985e706ed3502e1f',
        organizationName: 'Awesome org',
      });
      expect(fetchOrgs).toEqual({
        organizations: [
          { organizationId: '5f2a22b9360b4626985e706ed3502e1f', organizationName: 'Awesome org' },
          { organizationId: '0aba574e1c634737b7affd87a8789a0a', organizationName: 'New org' },
        ],
        loadError: null,
        loading: false,
        isShowNoAvailableOrgsWarning: false,
      });
    });

    it('sets isShowNoAvailableOrgsWarning to true and shows error message if there"re no Available organizations /fulfilled', () => {
      const state = Object.freeze({
        fetchOrgs: {
          organizations: [],
          loadError: null,
          loading: false,
          isShowNoAvailableOrgsWarning: false,
        },
        selectedOrganization: null,
      });

      const { fetchOrgs } = reducer(state, {
        type: `${OWNER_ACTIONS}/moveOwner/loadAvailableToMoveOrganizations/fulfilled`,
        payload: {
          availableOrganizations: [],
          selectedOrganization: { organizationId: '5f2a22b9360b4626985e706ed3502e1f', organizationName: 'Awesome org' },
        },
      });

      expect(fetchOrgs).toEqual({
        organizations: [],
        loadError: null,
        loading: false,
        isShowNoAvailableOrgsWarning: true,
      });
    });

    it('sets loadError on /rejected', () => {
      const state = Object.freeze({
        fetchOrgs: {
          loading: true,
          loadError: null,
        },
      });

      const {
        fetchOrgs: { loading, loadError },
      } = reducer(state, {
        type: `${OWNER_ACTIONS}/moveOwner/loadAvailableToMoveOrganizations/rejected`,
        payload: 'error',
      });

      expect(loading).toBe(false);
      expect(loadError).toBe('error');
    });
  });

  describe('moveOwner/moveApplication', () => {
    it('sets submitMaskState to false on /pending', () => {
      const state = Object.freeze({
        submitMaskState: null,
        warnings: null,
      });

      const action = {
        type: `${OWNER_ACTIONS}/moveOwner/moveApplication/pending`,
      };

      const newState = reducer(state, action);
      expect(newState).toEqual({
        submitMaskState: false,
        warnings: null,
      });
    });

    it('sets submitMaskState to true on /fulfilled', () => {
      const state = Object.freeze({
        submitError: null,
        submitMaskState: null,
        warnings: null,
      });

      const action = {
        type: `${OWNER_ACTIONS}/moveOwner/moveApplication/fulfilled`,
      };

      const newState = reducer(state, action);
      expect(newState).toEqual({
        submitError: null,
        submitMaskState: true,
        warnings: undefined,
      });
    });

    it('sets warnings if move application from organization with continuous monitoring settings to organization without any config on /fulfilled', () => {
      const state = Object.freeze({
        submitError: null,
        submitMaskState: null,
        warnings: null,
      });

      const action = {
        type: `${OWNER_ACTIONS}/moveOwner/moveApplication/fulfilled`,
        payload: ['Warning 1', 'Warning 2'],
      };

      const newState = reducer(state, action);
      expect(newState).toEqual({
        submitError: null,
        submitMaskState: true,
        warnings: ['Warning 1', 'Warning 2'],
      });
    });

    it('sets submitError on /rejected', () => {
      const state = Object.freeze({
        submitError: null,
        submitMaskState: null,
      });

      const action = {
        type: `${OWNER_ACTIONS}/moveOwner/moveApplication/rejected`,
        payload: 'Error',
      };

      const newState = reducer(state, action);
      expect(newState).toEqual({
        submitError: 'Error',
        submitMaskState: null,
      });
    });
  });

  describe('moveOwner/moveApplication', () => {
    it('sets submitMaskState to false on /pending', () => {
      const state = Object.freeze({
        submitMaskState: null,
        warnings: null,
      });

      const action = {
        type: `${OWNER_ACTIONS}/moveOwner/moveApplication/pending`,
      };

      const newState = reducer(state, action);
      expect(newState).toEqual({
        submitMaskState: false,
        warnings: null,
      });
    });

    it('sets submitMaskState to true on /fulfilled', () => {
      const state = Object.freeze({
        submitError: null,
        submitMaskState: null,
        warnings: null,
      });

      const action = {
        type: `${OWNER_ACTIONS}/moveOwner/moveApplication/fulfilled`,
      };

      const newState = reducer(state, action);
      expect(newState).toEqual({
        submitError: null,
        submitMaskState: true,
        warnings: undefined,
      });
    });

    it('sets warnings if move application form organization with continuous monitoring settings to organization without any config on /fulfilled', () => {
      const state = Object.freeze({
        submitError: null,
        submitMaskState: null,
        warnings: null,
      });

      const action = {
        type: `${OWNER_ACTIONS}/moveOwner/moveApplication/fulfilled`,
        payload: ['Warning 1', 'Warning 2'],
      };

      const newState = reducer(state, action);
      expect(newState).toEqual({
        submitError: null,
        submitMaskState: true,
        warnings: ['Warning 1', 'Warning 2'],
      });
    });

    it('sets submitError on /rejected', () => {
      const state = Object.freeze({
        submitError: null,
        submitMaskState: null,
      });

      const action = {
        type: `${OWNER_ACTIONS}/moveOwner/moveApplication/rejected`,
        payload: 'Error',
      };

      const newState = reducer(state, action);
      expect(newState).toEqual({
        submitError: 'Error',
        submitMaskState: null,
      });
    });
  });

  describe('moveOwner/setOrganization', () => {
    it('sets selected organization when choosing another organization in selector and enables Submit button', () => {
      const state = Object.freeze({
        selectedOrganization: null,
        isDirty: false,
        fetchOrgs: {
          organizations: [
            { organizationId: '5f2a22b9360b4626985e706ed3502e1f', organizationName: 'Awesome org' },
            { organizationId: '0aba574e1c634737b7affd87a8789a0a', organizationName: 'test' },
          ],
        },
      });

      const action = {
        type: `${OWNER_ACTIONS}/moveOwner/setOrganization`,
        payload: {
          movedApplicationId: '53425b02cc4d44adac5c63716e0f3e78',
          currentParentOrganizationId: '5f2a22b9360b4626985e706ed3502e1f',
          targetParentOrganizationId: '0aba574e1c634737b7affd87a8789a0a',
          movedOrganizationId: 'someOrgID',
        },
      };

      const newState = reducer(state, action);
      expect(newState).toEqual({
        selectedOrganization: {
          movedOrganizationId: 'someOrgID',
          movedApplicationId: '53425b02cc4d44adac5c63716e0f3e78',
          organizationId: '0aba574e1c634737b7affd87a8789a0a',
          organizationName: 'test',
        },
        isDirty: true,
        fetchOrgs: {
          organizations: [
            { organizationId: '5f2a22b9360b4626985e706ed3502e1f', organizationName: 'Awesome org' },
            { organizationId: '0aba574e1c634737b7affd87a8789a0a', organizationName: 'test' },
          ],
        },
      });
    });

    it('sets isDirty to false and disables Submit button if we select current parent organization', () => {
      const state = Object.freeze({
        selectedOrganization: {
          movedApplicationId: '53425b02cc4d44adac5c63716e0f3e78',
          organizationId: '0aba574e1c634737b7affd87a8789a0a',
          organizationName: 'test',
        },
        isDirty: true,
        fetchOrgs: {
          organizations: [
            { organizationId: '5f2a22b9360b4626985e706ed3502e1f', organizationName: 'Awesome org' },
            { organizationId: '0aba574e1c634737b7affd87a8789a0a', organizationName: 'test' },
          ],
        },
      });

      const action = {
        type: `${OWNER_ACTIONS}/moveOwner/setOrganization`,
        payload: {
          movedApplicationId: '53425b02cc4d44adac5c63716e0f3e78',
          currentParentOrganizationId: '5f2a22b9360b4626985e706ed3502e1f',
          targetParentOrganizationId: '5f2a22b9360b4626985e706ed3502e1f',
        },
      };

      const newState = reducer(state, action);

      expect(newState).toEqual({
        selectedOrganization: {
          movedOrganizationId: undefined,
          movedApplicationId: '53425b02cc4d44adac5c63716e0f3e78',
          organizationId: '5f2a22b9360b4626985e706ed3502e1f',
          organizationName: 'Awesome org',
        },
        isDirty: false,
        fetchOrgs: {
          organizations: [
            { organizationId: '5f2a22b9360b4626985e706ed3502e1f', organizationName: 'Awesome org' },
            { organizationId: '0aba574e1c634737b7affd87a8789a0a', organizationName: 'test' },
          ],
        },
      });
    });
  });
});
