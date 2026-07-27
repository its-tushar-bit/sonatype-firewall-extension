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

      expect(state).toEqual({
        saving: false,
        saveError: null,
        saveErrorId: 0,
        creatingManager: false,
        createManagerError: null,
        virtualRepositoryManagers: [],
        loadingVirtualRepositoryManagers: false,
        virtualRepositoryManagersLoadError: null,
      });
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

    it('/rejected uses "Error" from Messages when payload has no response data', () => {
      const state = { saving: true, saveError: null };

      const newState = reducer(state, { type: 'firewallIqProxy/saveRepository/rejected', payload: {} });

      expect(newState.saving).toBe(false);
      expect(newState.saveError).toBe('Error');
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

    it('/rejected sets creatingManager to false and stores backend error message when not a duplicate', () => {
      const state = { creatingManager: true, createManagerError: null };
      const payload = { response: { data: { message: 'Something went wrong.' } } };
      const meta = { arg: { name: 'foo' } };

      const newState = reducer(state, {
        type: 'firewallIqProxy/createVirtualRepositoryManager/rejected',
        payload,
        meta,
      });

      expect(newState.creatingManager).toBe(false);
      expect(newState.createManagerError).toBe('Something went wrong.');
    });

    it('/rejected formats the type-scoped duplicate-name error when status is 409 (FIRE-663)', () => {
      const state = { creatingManager: true, createManagerError: null };
      const payload = { response: { status: 409, data: 'Some backend text.' } };
      const meta = { arg: { name: 'Public NPM Mirror' } };

      const newState = reducer(state, {
        type: 'firewallIqProxy/createVirtualRepositoryManager/rejected',
        payload,
        meta,
      });

      expect(newState.creatingManager).toBe(false);
      expect(newState.createManagerError).toBe(
        `A Virtual Repository Manager named 'Public NPM Mirror' already exists.`
      );
    });

    it('/rejected still detects duplicate name from response text when status is missing (FIRE-663)', () => {
      const state = { creatingManager: true, createManagerError: null };
      const payload = { response: { data: `A virtual repository manager named 'man-1' already exists.` } };
      const meta = { arg: { name: 'man-1' } };

      const newState = reducer(state, {
        type: 'firewallIqProxy/createVirtualRepositoryManager/rejected',
        payload,
        meta,
      });

      expect(newState.createManagerError).toBe(`A Virtual Repository Manager named 'man-1' already exists.`);
    });

    it('/rejected uses fallback message when payload is undefined', () => {
      const state = { creatingManager: true, createManagerError: null };

      const newState = reducer(state, {
        type: 'firewallIqProxy/createVirtualRepositoryManager/rejected',
        payload: undefined,
      });

      expect(newState.creatingManager).toBe(false);
      expect(newState.createManagerError).toBe('An error occurred while creating the virtual repository manager.');
    });

    it('clearCreateManagerError resets the error to null', () => {
      const state = { ...initialState, createManagerError: `A Virtual Repository Manager named 'x' already exists.` };

      const newState = reducer(state, actions.clearCreateManagerError());

      expect(newState.createManagerError).toBeNull();
    });
  });

  describe('firewallIqProxy/fetchVirtualRepositoryManagers', () => {
    it('/pending sets loading to true and clears prior error', () => {
      const state = {
        loadingVirtualRepositoryManagers: false,
        virtualRepositoryManagersLoadError: 'previous error',
        virtualRepositoryManagers: [],
      };

      const newState = reducer(state, { type: 'firewallIqProxy/fetchVirtualRepositoryManagers/pending' });

      expect(newState.loadingVirtualRepositoryManagers).toBe(true);
      expect(newState.virtualRepositoryManagersLoadError).toBeNull();
    });

    it('/fulfilled stores the returned list and clears loading', () => {
      const state = {
        loadingVirtualRepositoryManagers: true,
        virtualRepositoryManagersLoadError: null,
        virtualRepositoryManagers: [],
      };
      const payload = [
        { id: 'vrm-1', name: 'first', childRepositoryCount: 2 },
        { id: 'vrm-2', name: 'second', childRepositoryCount: 0 },
      ];

      const newState = reducer(state, {
        type: 'firewallIqProxy/fetchVirtualRepositoryManagers/fulfilled',
        payload,
      });

      expect(newState.loadingVirtualRepositoryManagers).toBe(false);
      expect(newState.virtualRepositoryManagers).toEqual(payload);
    });

    it('/rejected stores error message and clears loading', () => {
      const state = {
        loadingVirtualRepositoryManagers: true,
        virtualRepositoryManagersLoadError: null,
        virtualRepositoryManagers: [],
      };
      const payload = { response: { data: 'Feature not supported.' } };

      const newState = reducer(state, {
        type: 'firewallIqProxy/fetchVirtualRepositoryManagers/rejected',
        payload,
      });

      expect(newState.loadingVirtualRepositoryManagers).toBe(false);
      expect(newState.virtualRepositoryManagersLoadError).toBe('Feature not supported.');
    });

    it('/rejected uses fallback message when payload is undefined', () => {
      const state = {
        loadingVirtualRepositoryManagers: true,
        virtualRepositoryManagersLoadError: null,
        virtualRepositoryManagers: [],
      };

      const newState = reducer(state, {
        type: 'firewallIqProxy/fetchVirtualRepositoryManagers/rejected',
        payload: undefined,
      });

      expect(newState.loadingVirtualRepositoryManagers).toBe(false);
      expect(newState.virtualRepositoryManagersLoadError).toBe(
        'An error occurred while loading virtual repository managers.'
      );
    });

    it('exposes fetchVirtualRepositoryManagers via actions', () => {
      expect(actions.fetchVirtualRepositoryManagers).toBeInstanceOf(Function);
    });
  });
});
