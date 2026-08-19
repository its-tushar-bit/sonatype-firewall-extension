/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/selectContactModal/selectContactModalSlice';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';

describe('selectContactModal reducer', () => {
  describe('selectContact (fetchusers)', () => {
    it('sets loading to true on setFetchUsersLoading', () => {
      const state = Object.freeze({
        fetchedUsers: {
          loading: null,
        },
      });

      const {
        fetchedUsers: { loading },
      } = reducer(state, {
        type: `${OWNER_ACTIONS}/selectContact/setFetchUsersLoading`,
        meta: { arg: '*' },
      });

      expect(loading).toBe(true);
    });

    describe('selectContact/loadFetchUsers', () => {
      it('sets loading to true when user specify value into query field /fulfilled', () => {
        const state = Object.freeze({
          fetchedUsers: {
            data: [],
            loadError: 'error',
            loading: true,
            partialError: 'someError',
            query: 's*',
          },
          query: 's*',
        });

        const { fetchedUsers } = reducer(state, {
          type: `${OWNER_ACTIONS}/selectContact/loadFetchUsers/fulfilled`,
          payload: {
            error: null,
            members: [
              {
                displayName: 'Sname',
                email: 'my@gmail.com',
                internalName: 'name',
                realm: 'IQ Server',
                type: 'USER',
              },
            ],
            query: 's*',
          },
        });

        expect(fetchedUsers).toEqual({
          data: [
            {
              displayName: 'Sname',
              email: 'my@gmail.com',
              internalName: 'name',
              realm: 'IQ Server',
              type: 'USER',
              id: 'nameUSER',
            },
          ],
          loadError: null,
          loading: false,
          partialError: null,
          query: 's*',
        });
      });

      it('sets loadError on /rejected', () => {
        const state = Object.freeze({
          fetchedUsers: {
            data: [],
            loadError: '',
            loading: true,
            partialError: '',
          },
        });

        const { fetchedUsers } = reducer(state, {
          type: `${OWNER_ACTIONS}/selectContact/loadFetchUsers/rejected`,
          payload: {
            error: 'error',
          },
        });

        expect(fetchedUsers).toEqual({
          data: [],
          loadError: 'Error',
          loading: false,
          partialError: null,
        });
      });
    });
  });

  describe('saveContact action', () => {
    it('sets saveContact to /pending', () => {
      const state = Object.freeze({
        submitError: 'some error',
        submitMaskState: true,
      });

      const { submitError, submitMaskState } = reducer(state, {
        type: `${OWNER_ACTIONS}/selectContact/saveContact/pending`,
      });

      expect(submitError).toBeNull();
      expect(submitMaskState).toBe(false);
    });

    it('sets saveContact to /fulfilled', () => {
      const state = Object.freeze({
        submitMaskState: false,
        submitError: 'some error',
        isDirty: true,
        query: '12 12',
      });

      const { submitMaskState, submitError, isDirty } = reducer(state, {
        type: `${OWNER_ACTIONS}/selectContact/saveContact/fulfilled`,
        payload: {
          contact: {
            displayName: '12 12',
            email: 'my@gmail.com',
            error: null,
            internalName: '12',
            realm: 'IQ Server',
          },
          id: 'a284da8572fd4e4eb2097cab965560aa',
          name: '4App',
          organizationId: 'cb53d63023c44a429d3d539821bdbd06',
          organizationName: '4 org',
          publicId: '4',
        },
      });
      expect(submitMaskState).toBe(true);
      expect(submitError).toBeNull();
      expect(isDirty).toBe(false);
    });

    it('sets saveContact to /rejected', () => {
      const state = Object.freeze({
        submitMaskState: true,
        submitError: null,
      });
      const { submitMaskState, submitError } = reducer(state, {
        type: `${OWNER_ACTIONS}/selectContact/saveContact/rejected`,
        payload: 'error',
      });
      expect(submitMaskState).toBeNull();
      expect(submitError).toBe('error');
    });
  });

  describe('saveContact action (for removing contact)', () => {
    it('sets removeContact to /pending', () => {
      const state = Object.freeze({
        submitError: 'some error',
        submitMaskState: true,
        selectedUser: {
          internalName: 'test',
          displayName: 'test test',
          email: 'my@gmail.com',
          realm: 'IQ Server',
          error: null,
        },
      });

      const { submitError, submitMaskState } = reducer(state, {
        type: `${OWNER_ACTIONS}/selectContact/saveContact/pending`,
      });

      expect(submitError).toBeNull();
      expect(submitMaskState).toBe(false);
    });

    it('sets saveContact (removing contact) to /fulfilled', () => {
      const state = Object.freeze({
        selectedUser: null,
        submitMaskState: false,
        submitError: 'some error',
        isDirty: true,
      });

      const { submitMaskState, submitError, isDirty } = reducer(state, {
        type: `${OWNER_ACTIONS}/selectContact/saveContact/fulfilled`,
        payload: {
          contact: null,
          displayName: '12 12',
          email: 'my9111@gmail.com',
          error: null,
          internalName: '12',
          realm: 'IQ Server',
        },
      });

      expect(submitMaskState).toBe(true);
      expect(submitError).toBeNull();
      expect(isDirty).toBe(false);
    });

    it('sets saveContact (removing contact to /rejected', () => {
      const state = Object.freeze({
        submitMaskState: true,
        submitError: null,
      });

      const { submitMaskState, submitError } = reducer(state, {
        type: `${OWNER_ACTIONS}/selectContact/saveContact/rejected`,
        payload: 'error',
      });

      expect(submitMaskState).toBeNull();
      expect(submitError).toBe('error');
    });
  });

  it('sets query value', () => {
    const state = Object.freeze({
      query: '',
      isDirty: false,
      isValid: true,
    });
    const { query, isDirty, isValid } = reducer(state, {
      type: `${OWNER_ACTIONS}/selectContact/setQuery`,
      payload: 'query',
    });
    expect(query).toBe('query');
    expect(isDirty).toBe(true);
    expect(isValid).toBe(false);
  });

  it('setSelectedContact action', () => {
    const state = Object.freeze({
      selectedContact: null,
      isValid: false,
    });

    const { isValid, selectedContact } = reducer(state, {
      type: `${OWNER_ACTIONS}/selectContact/setSelectedContact`,
      payload: {
        displayName: 'test my',
        email: 'my@gmail.com',
        id: 'adUSER',
        internalName: 'test_my',
        realm: 'IQ Server',
        type: 'USER',
      },
    });

    expect(isValid).toBe(true);
    expect(selectedContact).toEqual({
      displayName: 'test my',
      email: 'my@gmail.com',
      id: 'adUSER',
      internalName: 'test_my',
      realm: 'IQ Server',
      type: 'USER',
    });
  });
});
