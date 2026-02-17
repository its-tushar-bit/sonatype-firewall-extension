/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  actions,
  initialState,
} from 'MainRoot/sbomManager/features/billOfMaterials/originalBom/originalBomViewerSlice';

describe('originalBomViewerSlice', () => {
  describe('Initial State', () => {
    it('should have correct initial state', () => {
      expect(initialState).toEqual({
        loading: false,
        error: null,
        treeData: [],
        openNodes: {},
        nodeChildren: {},
        visibleCounts: {},
        componentNotFound: false,
        searchValue: '',
        debouncedSearchValue: '',
      });
    });

    it('should return initial state when undefined state is passed', () => {
      expect(reducer(undefined, { type: 'unknown' })).toEqual(initialState);
    });
  });

  describe('Reducers', () => {
    describe('setSearchValue', () => {
      it('should update searchValue', () => {
        const state = { ...initialState };
        const action = actions.setSearchValue('test');

        const newState = reducer(state, action);

        expect(newState.searchValue).toBe('test');
      });

      it('should clear searchValue when empty string is passed', () => {
        const state = { ...initialState, searchValue: 'test' };
        const action = actions.setSearchValue('');

        const newState = reducer(state, action);

        expect(newState.searchValue).toBe('');
      });

      it('should not modify other state properties', () => {
        const state = {
          ...initialState,
          treeData: [{ id: '1', name: 'test' }],
          openNodes: { 1: true },
        };
        const action = actions.setSearchValue('search');

        const newState = reducer(state, action);

        expect(newState.treeData).toEqual(state.treeData);
        expect(newState.openNodes).toEqual(state.openNodes);
      });
    });

    describe('setDebouncedSearchValue', () => {
      it('should update debouncedSearchValue', () => {
        const state = { ...initialState };
        const action = actions.setDebouncedSearchValue('debounced');

        const newState = reducer(state, action);

        expect(newState.debouncedSearchValue).toBe('debounced');
      });

      it('should clear debouncedSearchValue when empty string is passed', () => {
        const state = { ...initialState, debouncedSearchValue: 'test' };
        const action = actions.setDebouncedSearchValue('');

        const newState = reducer(state, action);

        expect(newState.debouncedSearchValue).toBe('');
      });

      it('should not modify searchValue', () => {
        const state = { ...initialState, searchValue: 'test' };
        const action = actions.setDebouncedSearchValue('debounced');

        const newState = reducer(state, action);

        expect(newState.searchValue).toBe('test');
        expect(newState.debouncedSearchValue).toBe('debounced');
      });

      it('should not modify other state properties', () => {
        const state = {
          ...initialState,
          treeData: [{ id: '1', name: 'test' }],
          loading: true,
        };
        const action = actions.setDebouncedSearchValue('debounced');

        const newState = reducer(state, action);

        expect(newState.treeData).toEqual(state.treeData);
        expect(newState.loading).toBe(true);
      });
    });

    describe('loadMoreChildren', () => {
      it('should update visibleCounts for node', () => {
        const state = { ...initialState };
        const action = actions.loadMoreChildren({ nodeId: 'node1', batchSize: 50 });

        const newState = reducer(state, action);

        expect(newState.visibleCounts['node1']).toBe(100);
      });

      it('should increment existing visibleCount', () => {
        const state = { ...initialState, visibleCounts: { node1: 50 } };
        const action = actions.loadMoreChildren({ nodeId: 'node1', batchSize: 50 });

        const newState = reducer(state, action);

        expect(newState.visibleCounts['node1']).toBe(100);
      });

      it('should not affect other nodes visibleCounts', () => {
        const state = { ...initialState, visibleCounts: { node1: 50, node2: 75 } };
        const action = actions.loadMoreChildren({ nodeId: 'node1', batchSize: 50 });

        const newState = reducer(state, action);

        expect(newState.visibleCounts['node1']).toBe(100);
        expect(newState.visibleCounts['node2']).toBe(75);
      });
    });

    describe('toggleNode', () => {
      it('should open a closed node', () => {
        const state = { ...initialState };
        const node = { id: 'node1', rawData: { key: 'value' } };
        const action = actions.toggleNode({ nodeId: 'node1', node });

        const newState = reducer(state, action);

        expect(newState.openNodes['node1']).toBe(true);
      });

      it('should close an open node', () => {
        const state = { ...initialState, openNodes: { node1: true } };
        const node = { id: 'node1', rawData: { key: 'value' } };
        const action = actions.toggleNode({ nodeId: 'node1', node });

        const newState = reducer(state, action);

        expect(newState.openNodes['node1']).toBe(false);
      });

      it('should expand children when opening node with rawData', () => {
        const state = { ...initialState };
        const node = { id: 'node1', rawData: { child1: 'value1', child2: 'value2' } };
        const action = actions.toggleNode({ nodeId: 'node1', node });

        const newState = reducer(state, action);

        expect(newState.openNodes['node1']).toBe(true);
        expect(newState.nodeChildren['node1']).toBeDefined();
        expect(newState.nodeChildren['node1'].length).toBe(2);
      });

      it('should not expand children if already loaded', () => {
        const existingChildren = [{ id: 'child1', name: 'child1' }];
        const state = { ...initialState, nodeChildren: { node1: existingChildren } };
        const node = { id: 'node1', rawData: { child1: 'value1', child2: 'value2' } };
        const action = actions.toggleNode({ nodeId: 'node1', node });

        const newState = reducer(state, action);

        expect(newState.nodeChildren['node1']).toEqual(existingChildren);
      });
    });

    describe('resetState', () => {
      it('should reset to initial state', () => {
        const state = {
          loading: true,
          error: 'error',
          treeData: [{ id: '1' }],
          openNodes: { 1: true },
          searchValue: 'test',
          debouncedSearchValue: 'test',
        };
        const action = actions.resetState();

        const newState = reducer(state, action);

        expect(newState).toEqual(initialState);
      });
    });
  });
});
