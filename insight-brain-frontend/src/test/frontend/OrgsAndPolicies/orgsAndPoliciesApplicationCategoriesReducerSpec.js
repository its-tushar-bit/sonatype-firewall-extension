/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { initialState } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesApplicationCategoriesSlice';

describe('orgsAndPoliciesApplicationCategories reducer', () => {
  describe('applicationCategories/setCategoryDescription', () => {
    it('sets description to currentCategory and isDirty property', () => {
      const state = Object.freeze({
        currentCategory: {
          color: null,
          description: null,
          name: null,
        },
        serverCategory: null,
        isDirty: false,
      });

      const { isDirty, currentCategory } = reducer(state, {
        type: 'applicationCategories/setCategoryDescription',
        payload: 'category description',
      });

      expect(isDirty).toBeTrue();
      expect(currentCategory.description).toBe('category description');
    });
  });

  describe('applicationCategories/setCategoryColor', () => {
    it('sets color to currentCategory and isDirty property', () => {
      const state = Object.freeze({
        currentCategory: {
          color: null,
          description: null,
          name: null,
        },
        serverCategory: null,
        isDirty: false,
      });

      const { isDirty, currentCategory } = reducer(state, {
        type: 'applicationCategories/setCategoryColor',
        payload: 'light-green',
      });

      expect(isDirty).toBeTrue();
      expect(currentCategory.color).toBe('light-green');
    });
  });

  describe('applicationCategories/setCategoryName', () => {
    it('sets name to currentCategory and isDirty property', () => {
      const state = Object.freeze({
        currentCategory: {
          color: null,
          description: null,
          name: null,
        },
        serverCategory: null,
        isDirty: false,
      });

      const { isDirty, currentCategory } = reducer(state, {
        type: 'applicationCategories/setCategoryName',
        payload: 'name',
      });

      expect(isDirty).toBeTrue();
      expect(currentCategory.name).toBe('name');
    });
  });

  describe('applicationCategories/resetIsDirty', () => {
    it('resets isDirty property', () => {
      const state = Object.freeze({ isDirty: true });

      const { isDirty } = reducer(state, {
        type: 'applicationCategories/resetIsDirty',
      });

      expect(isDirty).toBeFalse();
    });
  });

  describe('applicationCategories/saveApplicationCategory/pending', () => {
    it('sets submitError property to null', () => {
      const state = Object.freeze({ submitError: 'error' });

      const { submitError } = reducer(state, {
        type: 'applicationCategories/saveApplicationCategory/pending',
      });

      expect(submitError).toBeNull();
    });
  });

  describe('applicationCategories/saveApplicationCategory/fulfilled', () => {
    it('sets currentCategory and serverCategory to payload, resets isDirty', () => {
      const state = Object.freeze({
        submitError: 'error',
        isDirty: true,
        siblings: [],
        currentCategory: null,
        serverCategory: null,
      });

      const newState = reducer(state, {
        type: 'applicationCategories/saveApplicationCategory/fulfilled',
        payload: {
          isEditMode: true,
          savedCategory: {
            color: 'light-brown',
            description: null,
            name: 'category name',
            id: '1242345',
          },
        },
      });

      expect(newState.submitError).toBeNull();
      expect(newState.isDirty).toBeFalse();
      expect(newState.currentCategory).toEqual({
        color: 'light-brown',
        description: null,
        name: 'category name',
        id: '1242345',
      });
      expect(newState.serverCategory).toEqual({
        color: 'light-brown',
        description: null,
        name: 'category name',
        id: '1242345',
      });
      expect(newState.siblings).toEqual([
        {
          color: 'light-brown',
          description: null,
          name: 'category name',
          id: '1242345',
        },
      ]);
    });

    it('sets currentCategory and serverCategory to initial state, resets isDirty in create mode', () => {
      const state = Object.freeze({
        submitError: 'error',
        isDirty: true,
        siblings: [],
        currentCategory: {
          color: 'foo',
          description: 'description',
          name: 'name',
        },
        serverCategory: null,
      });

      const newState = reducer(state, {
        type: 'applicationCategories/saveApplicationCategory/fulfilled',
        payload: {
          isEditMode: false,
          savedCategory: {
            color: 'light-brown',
            description: null,
            name: 'category name',
            id: '1242345',
          },
        },
      });

      expect(newState.submitError).toBeNull();
      expect(newState.isDirty).toBeFalse();

      expect(newState.currentCategory).toEqual(initialState.currentCategory);
      expect(newState.serverCategory).toEqual(initialState.currentCategory);
      expect(newState.siblings).toEqual([
        {
          color: 'light-brown',
          description: null,
          name: 'category name',
          id: '1242345',
        },
      ]);
    });
  });

  describe('applicationCategories/saveApplicationCategory/rejected', () => {
    it('sets submitError property to error', () => {
      const state = Object.freeze({ submitError: null });

      const { submitError } = reducer(state, {
        type: 'applicationCategories/saveApplicationCategory/rejected',
        payload: 'error',
      });

      expect(submitError).toBe('error');
    });
  });

  describe('applicationCategories/removeApplicationCategory/pending', () => {
    it('sets deleting property to null', () => {
      const state = Object.freeze({ deleting: false });

      const { deleteModal } = reducer(state, {
        type: 'applicationCategories/removeApplicationCategory/pending',
      });

      expect(deleteModal.deleting).toBeTrue();
    });
  });

  describe('applicationCategories/removeApplicationCategory/fulfilled', () => {
    it('sets currentCategory, serverCategory to initialState', () => {
      const state = Object.freeze({
        deleteModal: {
          deleting: true,
          errorState: null,

          success: null,
        },
        isDirty: true,
        currentCategory: {
          color: 'foo',
          description: 'description',
          name: 'name',
          id: 'id',
        },
        serverCategory: {
          color: 'foo',
          description: 'description',
          name: 'name',
          id: 'id',
        },
        siblings: [
          {
            color: 'foo',
            description: 'description',
            name: 'name',
            id: 'id',
          },
        ],
      });

      const newState = reducer(state, {
        type: 'applicationCategories/removeApplicationCategory/fulfilled',
        payload: 'id',
      });

      expect(newState.deleteModal.deleting).toBeNull();
      expect(newState.deleteModal.errorState).toBeNull();

      expect(newState.isDirty).toBeFalse();
      expect(newState.deleteModal.success).toBeTrue();
      expect(newState.currentCategory).toEqual(initialState.currentCategory);
      expect(newState.serverCategory).toEqual(initialState.serverCategory);
      expect(newState.siblings).toEqual([]);
    });
  });

  describe('applicationCategories/removeApplicationCategory/rejected', () => {
    it('sets errorState, deleting', () => {
      const state = Object.freeze({
        deleteModal: {
          deleting: true,
          errorState: null,
        },
      });

      const { deleteModal } = reducer(state, {
        type: 'applicationCategories/removeApplicationCategory/rejected',
        payload: 'error',
      });

      expect(deleteModal.deleting).toBeFalse();
      expect(deleteModal.errorState).toBe('error');
    });
  });

  describe('applicationCategories/loadApplicableCategories/pending', () => {
    it('resets loading, loadError', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'applicationCategories/loadApplicableCategories/pending',
      });

      expect(loading).toBeTrue();
      expect(loadError).toBeNull();
    });
  });

  describe('applicationCategories/loadApplicableCategories/fulfilled', () => {
    it('sets loading, loadError, appCategoryOwners, ownerName ', () => {
      const mockAppCategoryOwners = [
        {
          ownerId: 'ownerId',
          ownerType: 'APPLICATION',
          ownerName: 'someAppName',
          applicationCategories: [
            {
              color: 'light-green',
              description: null,
              id: 'ae63051b2e304c3bbabf94c2443b03fb',
              name: 'n3',
              ownerId: 'sdfds',
              ownerType: 'APPLICATION',
            },
          ],
        },
      ];

      const state = Object.freeze({
        loading: true,
        loadError: 'error',
        appCategoryOwners: [],
      });

      const newStore = reducer(state, {
        type: 'applicationCategories/loadApplicableCategories/fulfilled',
        payload: {
          appCategoryOwners: mockAppCategoryOwners,
        },
      });

      expect(newStore.loading).toBeFalse();
      expect(newStore.loadError).toBeNull();
      expect(newStore.appCategoryOwners).toEqual(mockAppCategoryOwners);
    });
  });

  describe('applicationCategories/loadApplicableCategories/rejected', () => {
    it('sets loading, loadError', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const { loading, loadError } = reducer(state, {
        type: 'applicationCategories/loadApplicableCategories/rejected',
        payload: 'error',
      });

      expect(loading).toBeFalse();
      expect(loadError).toBe('error');
    });
  });

  describe('applicationCategories/loadCategoryEditor/pending', () => {
    it('resets loading, loadError', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'applicationCategories/loadCategoryEditor/pending',
      });

      expect(loading).toBeTrue();
      expect(loadError).toBeNull();
    });
  });

  describe('applicationCategories/loadCategoryEditor/fulfilled', () => {
    it('sets loading, loadError, currentCategory, serverCategory, siblings', () => {
      const state = Object.freeze({
        loading: true,
        loadError: 'error',
        currentCategory: null,
        serverCategory: null,
        siblings: [],
        deleteModal: {
          associatedApplicationNames: null,
          tagPolicyList: null,
        },
      });

      const currentCategory = {
        color: 'light-green',
        description: null,
        id: 'id',
        name: 'x3',
        ownerId: 'ownerId',
        ownerType: 'APPLICATION',
      };
      const siblings = [
        {
          color: 'light-green',
          description: null,
          id: 'ae63051b2e304c3bbabf94c2443b03fb',
          name: 'n3',
          ownerId: '6b365e8a8000449aa924f194a7ed0d21',
          ownerType: 'APPLICATION',
        },
      ];
      const associatedApplicationNames = ['associated application name'];
      const tagPolicyList = ['associated policy name'];
      const newState = reducer(state, {
        type: 'applicationCategories/loadCategoryEditor/fulfilled',
        payload: {
          currentCategory,
          siblings,
          associatedApplicationNames,
          tagPolicyList,
        },
      });

      expect(newState.loading).toBeFalse();
      expect(newState.loadError).toBeNull();
      expect(newState.currentCategory).toEqual(currentCategory);
      expect(newState.serverCategory).toEqual(currentCategory);
      expect(newState.deleteModal.associatedApplicationNames).toEqual(associatedApplicationNames);
      expect(newState.deleteModal.tagPolicyList).toEqual(tagPolicyList);
      expect(newState.siblings).toEqual(siblings);
    });
  });

  describe('applicationCategories/loadCategoryEditor/rejected', () => {
    it('sets loading, loadError', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const { loading, loadError } = reducer(state, {
        type: 'applicationCategories/loadCategoryEditor/rejected',
        payload: 'error',
      });

      expect(loading).toBeFalse();
      expect(loadError).toBe('error');
    });
  });
});
