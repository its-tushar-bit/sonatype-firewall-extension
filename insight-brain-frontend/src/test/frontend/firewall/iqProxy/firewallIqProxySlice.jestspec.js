/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { initialState, actions } from 'MainRoot/firewall/iqProxy/firewallIqProxySlice';

describe('firewallIqProxySlice', () => {
  describe('initial state', () => {
    it('returns the correct initial state', () => {
      const state = reducer(undefined, { type: '@@INIT' });

      expect(state).toEqual({ saving: false, saveError: null, saveErrorId: 0, creatingManager: false, createManagerError: null });
    });
  });

  describe('reset action', () => {
    it('resets state back to initial state', () => {
      const dirtyState = { saving: true, saveError: 'some error' };

      const newState = reducer(dirtyState, actions.reset());

      expect(newState).toEqual(initialState);
    });
  });

  describe('firewallIqProxy/saveRepository', () => {
    it('/pending sets saving to true and clears saveError', () => {
      const state = { saving: false, saveError: 'previous error' };

      const newState = reducer(state, { type: 'firewallIqProxy/saveRepository/pending' });

      expect(newState.saving).toBe(true);
      expect(newState.saveError).toBeNull();
    });

    it('/fulfilled sets saving to false', () => {
      const state = { saving: true, saveError: null };

      const newState = reducer(state, { type: 'firewallIqProxy/saveRepository/fulfilled' });

      expect(newState.saving).toBe(false);
      expect(newState.saveError).toBeNull();
    });

    it('/rejected sets saving to false and stores error from payload response data', () => {
      const state = { saving: true, saveError: null };
      const payload = { response: { data: 'Repository already exists.' } };

      const newState = reducer(state, { type: 'firewallIqProxy/saveRepository/rejected', payload });

      expect(newState.saving).toBe(false);
      expect(newState.saveError).toBe('Repository already exists.');
    });

    it('/rejected uses fallback message when payload has no response data', () => {
      const state = { saving: true, saveError: null };

      const newState = reducer(state, { type: 'firewallIqProxy/saveRepository/rejected', payload: {} });

      expect(newState.saving).toBe(false);
      expect(newState.saveError).toBe('An error occurred while saving.');
    });

    it('/rejected uses fallback message when payload is undefined', () => {
      const state = { saving: true, saveError: null };

      const newState = reducer(state, { type: 'firewallIqProxy/saveRepository/rejected', payload: undefined });

      expect(newState.saving).toBe(false);
      expect(newState.saveError).toBe('An error occurred while saving.');
    });
  });

  describe('firewallIqProxy/createVirtualRepositoryManager', () => {
    it('/pending sets creatingManager to true and clears createManagerError', () => {
      const state = { creatingManager: false, createManagerError: 'previous error' };

      const newState = reducer(state, { type: 'firewallIqProxy/createVirtualRepositoryManager/pending' });

      expect(newState.creatingManager).toBe(true);
      expect(newState.createManagerError).toBeNull();
    });

    it('/fulfilled sets creatingManager to false', () => {
      const state = { creatingManager: true, createManagerError: null };

      const newState = reducer(state, { type: 'firewallIqProxy/createVirtualRepositoryManager/fulfilled' });

      expect(newState.creatingManager).toBe(false);
    });

    it('/rejected sets creatingManager to false and stores error message', () => {
      const state = { creatingManager: true, createManagerError: null };
      const payload = { response: { data: { message: 'Name already exists.' } } };

      const newState = reducer(state, { type: 'firewallIqProxy/createVirtualRepositoryManager/rejected', payload });

      expect(newState.creatingManager).toBe(false);
      expect(newState.createManagerError).toBe('Name already exists.');
    });

    it('/rejected falls back to response data string when no message field', () => {
      const state = { creatingManager: true, createManagerError: null };
      const payload = { response: { data: 'Server error' } };

      const newState = reducer(state, { type: 'firewallIqProxy/createVirtualRepositoryManager/rejected', payload });

      expect(newState.creatingManager).toBe(false);
      expect(newState.createManagerError).toBe('Server error');
    });

    it('/rejected uses fallback message when payload is undefined', () => {
      const state = { creatingManager: true, createManagerError: null };

      const newState = reducer(state, { type: 'firewallIqProxy/createVirtualRepositoryManager/rejected', payload: undefined });

      expect(newState.creatingManager).toBe(false);
      expect(newState.createManagerError).toBe('An error occurred while creating the repository manager.');
    });
  });
});
