/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/organizationsSlice';

describe('organizations reducer', () => {
  const otherState = Object.freeze({
    data: 'data',
  });

  describe('organizations/loadOrganizations/fulfilled', () => {
    it('resets organizations', () => {
      const state = Object.freeze({
        otherState,
        organizations: [],
        loading: true,
      });
      const expected = [
        {
          id: '430b39e52a2e4ca48d708913f0f4b10d',
          name: 'alpine test',
        },
      ];

      const { organizations, loading } = reducer(state, {
        type: 'organizations/loadOrganizations/fulfilled',
        payload: expected,
      });

      expect(loading).toBeFalse();
      expect(organizations).toEqual(expected);
    });
  });

  describe('organizations/loadOrganizations/pending', () => {
    it('resets organizations', () => {
      const state = Object.freeze({
        otherState,
        loadError: 'some error',
        loading: false,
      });

      const { loadError, loading } = reducer(state, {
        type: 'organizations/loadOrganizations/pending',
      });

      expect(loading).toBeTrue();
      expect(loadError).toBeNull();
    });
  });

  describe('organizations/loadOrganizations/rejected', () => {
    it('resets organizations', () => {
      const state = Object.freeze({
        otherState,
        loadError: null,
        loading: true,
      });

      const { loadError, loading } = reducer(state, {
        type: 'organizations/loadOrganizations/rejected',
        payload: 'some error',
      });

      expect(loading).toBeFalse();
      expect(loadError).toBe('some error');
    });
  });

  describe('organizations/removeOrganizationFromList', () => {
    it('removes organization from the list by id', () => {
      const state = Object.freeze({
        organizations: [
          {
            id: '430b39e52a2e4ca48d708913f0f4b10d',
            name: 'alpine test',
          },
        ],
      });
      const newState = reducer(state, {
        type: 'organizations/removeOrganizationFromList',
        payload: '430b39e52a2e4ca48d708913f0f4b10d',
      });
      expect(newState.organizations).toEqual([]);
    });
  });

  describe('organizations/removeOrganizationsFromList', () => {
    it('removes organization from the list by id', () => {
      const state = Object.freeze({
        organizations: [
          { id: 'org-one', name: 'org one' },
          { id: 'org-two', name: 'org two' },
          { id: 'org-three', name: 'org three' },
        ],
      });
      const newState = reducer(state, {
        type: 'organizations/removeOrganizationsFromList',
        payload: ['org-one', 'org-three'],
      });
      expect(newState.organizations).toEqual([{ id: 'org-two', name: 'org two' }]);
    });
  });
});
