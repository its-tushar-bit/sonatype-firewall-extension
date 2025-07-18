/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reduce from 'MainRoot/configuration/administrators/administratorsSlice';

describe('administratorsConfigSliceSpec', function () {
  let otherObject;

  beforeEach(() => {
    otherObject = { value: 'Other value' };
  });

  describe('administratorsConfig/load/fulfilled action', () => {
    it('sets loading to false and clears errors', () => {
      const state = Object.freeze({
        other: otherObject,
        loading: true,
        loadError: 'Some error',
      });

      const { loading, loadError, other } = reduce(state, {
        type: 'administratorsConfig/load/fulfilled',
      });

      expect(loading).toBe(false);
      expect(loadError).toBe(null);
      expect(other).toBe(otherObject);
    });
  });

  describe('administratorsConfig/load/rejected action', () => {
    it('sets loading to false and sets loadError to payload', () => {
      const state = Object.freeze({
        other: otherObject,
        loading: true,
        loadError: 'error',
      });

      const { loading, loadError, other } = reduce(state, {
        type: 'administratorsConfig/load/rejected',
        payload: 'error',
      });

      expect(loading).toBe(false);
      expect(loadError).toBe('error');
      expect(other).toBe(otherObject);
    });
  });

  describe('administratorsConfig/load/pending action', () => {
    it('sets loading to true', () => {
      const state = Object.freeze({
        loading: false,
      });

      const { loading } = reduce(state, {
        type: 'administratorsConfig/load/pending',
      });

      expect(loading).toBe(true);
    });
  });

  describe('administratorsConfig/loadFetchUsers/fulfilled action', () => {
    it('sets loading to false, clears errors and set the data', () => {
      const state = Object.freeze({
        other: otherObject,
        fetchUsers: { data: [], loading: true, loadError: 'some error' },
      });

      const {
        fetchUsers: { loading, loadError, data },
        other,
      } = reduce(state, {
        type: 'administratorsConfig/loadFetchUsers/fulfilled',
        payload: { members: [{ internalName: 'name', type: 'USER' }] },
      });

      expect(loading).toBe(false);
      expect(loadError).toBeNull();
      expect(data).toEqual([{ internalName: 'name', id: 'nameUSER', type: 'USER' }]);
      expect(other).toBe(otherObject);
    });

    it('sets loading to false and set partialError when there is an error', () => {
      const state = Object.freeze({
        other: otherObject,
        fetchUsers: { data: [], loading: true, loadError: 'some error', partialError: null },
      });

      const {
        fetchUsers: { loading, loadError, data, partialError },
        other,
      } = reduce(state, {
        type: 'administratorsConfig/loadFetchUsers/fulfilled',
        payload: { members: [{ internalName: 'name', type: 'USER' }], error: 'there was an error' },
      });

      expect(loading).toBe(false);
      expect(partialError).toBe('there was an error');
      expect(loadError).toBe(null);
      expect(data).toEqual([{ internalName: 'name', type: 'USER', id: 'nameUSER' }]);
      expect(other).toBe(otherObject);
    });
  });

  describe('administratorsConfig/loadFetchUsers/rejected action', () => {
    it('sets loading to false and sets loadError to payload', () => {
      const state = Object.freeze({
        other: otherObject,
        fetchUsers: { data: [], loading: true, loadError: 'some error' },
      });

      const {
        fetchUsers: { loading, loadError },
        other,
      } = reduce(state, {
        type: 'administratorsConfig/loadFetchUsers/rejected',
        payload: 'other error',
      });

      expect(loading).toBe(false);
      expect(loadError).toBe('other error');
      expect(other).toBe(otherObject);
    });
  });

  describe('administratorsConfig/loadFetchUsers/pending action', () => {
    it('sets loading to true', () => {
      const state = Object.freeze({
        other: otherObject,
        fetchUsers: { data: [], loading: false, loadError: 'some error' },
      });

      const {
        fetchUsers: { loading },
        other,
      } = reduce(state, {
        type: 'administratorsConfig/loadFetchUsers/pending',
      });

      expect(loading).toBe(true);
      expect(other).toBe(otherObject);
    });
  });

  describe('administratorsConfig/saveMembers/fulfilled action', () => {
    it('sets submitMaskState to true', () => {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: null,
        submitError: null,
      });

      const { submitMaskState, other } = reduce(state, {
        type: 'administratorsConfig/saveMembers/fulfilled',
      });

      expect(submitMaskState).toBe(true);
      expect(other).toBe(otherObject);
    });
  });

  describe('administratorsConfig/saveMembers/rejected action', () => {
    it('sets submitMaskState to null and sets submitError to payload', () => {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: true,
        submitError: null,
      });

      const { submitMaskState, submitError, other } = reduce(state, {
        type: 'administratorsConfig/saveMembers/rejected',
        payload: 'other error',
      });

      expect(submitMaskState).toBeNull();
      expect(submitError).toBe('other error');
      expect(other).toBe(otherObject);
    });
  });

  describe('administratorsConfig/saveMembers/pending action', () => {
    it('sets loading to true', () => {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: true,
        submitError: null,
      });

      const { submitMaskState, other } = reduce(state, {
        type: 'administratorsConfig/saveMembers/pending',
      });

      expect(submitMaskState).toBe(false);
      expect(other).toBe(otherObject);
    });
  });
});
