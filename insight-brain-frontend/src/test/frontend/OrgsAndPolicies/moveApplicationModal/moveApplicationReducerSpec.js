/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/moveApplicationModal/moveApplicationSlice';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';

describe('moveApplication reducer', () => {
  describe('moveApplication/loadAvailableToMoveOrganizations', () => {
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
        type: `${OWNER_ACTIONS}/moveApplication/loadAvailableToMoveOrganizations/pending`,
      });

      expect(loading).toBeTrue();
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
      });

      const { fetchOrgs } = reducer(state, {
        type: `${OWNER_ACTIONS}/moveApplication/loadAvailableToMoveOrganizations/fulfilled`,
        payload: [
          { organizationId: '5f2a22b9360b4626985e706ed3502e1f', organizationName: 'Awesome org' },
          { organizationId: '0aba574e1c634737b7affd87a8789a0a', organizationName: 'New org' },
        ],
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
      });

      const { fetchOrgs } = reducer(state, {
        type: `${OWNER_ACTIONS}/moveApplication/loadAvailableToMoveOrganizations/fulfilled`,
        payload: [],
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
        type: `${OWNER_ACTIONS}/moveApplication/loadAvailableToMoveOrganizations/rejected`,
        payload: 'error',
      });

      expect(loading).toBeFalse();
      expect(loadError).toBe('error');
    });
  });
  describe('moveApplication/moveApplication', () => {
    it('sets submitMaskState to false on /pending', () => {
      const state = Object.freeze({
        submitMaskState: null,
        warnings: null,
      });

      const action = {
        type: `${OWNER_ACTIONS}/moveApplication/moveApplication/pending`,
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
        type: `${OWNER_ACTIONS}/moveApplication/moveApplication/fulfilled`,
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
        type: `${OWNER_ACTIONS}/moveApplication/moveApplication/fulfilled`,
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
        type: `${OWNER_ACTIONS}/moveApplication/moveApplication/rejected`,
        payload: 'Error',
      };

      const newState = reducer(state, action);
      expect(newState).toEqual({
        submitError: 'Error',
        submitMaskState: null,
      });
    });
  });
  describe('moveApplication/setOrganization', () => {
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
        type: `${OWNER_ACTIONS}/moveApplication/setOrganization`,
        payload: {
          applicationId: '53425b02cc4d44adac5c63716e0f3e78',
          currentParentOrganization: '5f2a22b9360b4626985e706ed3502e1f',
          selectedOrganizationId: '0aba574e1c634737b7affd87a8789a0a',
        },
      };

      const newState = reducer(state, action);

      expect(newState).toEqual({
        selectedOrganization: {
          applicationId: '53425b02cc4d44adac5c63716e0f3e78',
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
          applicationId: '53425b02cc4d44adac5c63716e0f3e78',
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
        type: `${OWNER_ACTIONS}/moveApplication/setOrganization`,
        payload: {
          applicationId: '53425b02cc4d44adac5c63716e0f3e78',
          currentParentOrganization: '5f2a22b9360b4626985e706ed3502e1f',
          selectedOrganizationId: '5f2a22b9360b4626985e706ed3502e1f',
        },
      };

      const newState = reducer(state, action);

      expect(newState).toEqual({
        selectedOrganization: {
          applicationId: '53425b02cc4d44adac5c63716e0f3e78',
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
