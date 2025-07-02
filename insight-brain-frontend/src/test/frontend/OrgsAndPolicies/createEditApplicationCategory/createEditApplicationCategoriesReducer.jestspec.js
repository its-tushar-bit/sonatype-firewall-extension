/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  initialState,
} from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/createEditApplicationCategoriesSlice';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
const { initialState: rscInitialState } = nxTextInputStateHelpers;

describe('orgsAndPoliciesApplicationCategories reducer', () => {
  describe('applicationCategories/createEdit/setCategoryDescription', () => {
    it('sets description to currentCategory and isDirty property', () => {
      const state = Object.freeze({
        currentCategory: {
          color: 'light-purple',
          description: rscInitialState(''),
          name: rscInitialState(''),
        },
        serverCategory: null,
        isDirty: false,
      });

      const { isDirty, currentCategory } = reducer(state, {
        type: 'applicationCategories/createEdit/setCategoryDescription',
        payload: 'category description',
      });

      expect(isDirty).toBe(true);
      expect(currentCategory.description).toEqual({
        isPristine: false,
        value: 'category description',
        trimmedValue: 'category description',
        validationErrors: [],
      });
    });
  });

  describe('applicationCategories/createEdit/setCategoryColor', () => {
    it('sets color to currentCategory and isDirty property', () => {
      const state = Object.freeze({
        currentCategory: {
          color: 'light-purple',
          description: rscInitialState(''),
          name: rscInitialState(''),
        },
        serverCategory: null,
        isDirty: false,
      });

      const { isDirty, currentCategory } = reducer(state, {
        type: 'applicationCategories/createEdit/setCategoryColor',
        payload: 'light-green',
      });

      expect(isDirty).toBe(true);
      expect(currentCategory.color).toBe('light-green');
    });
  });

  describe('applicationCategories/createEdit/setCategoryName', () => {
    it('sets name to currentCategory and isDirty property', () => {
      const state = Object.freeze({
        currentCategory: {
          color: 'light-purple',
          description: rscInitialState(''),
          name: rscInitialState(''),
        },
        serverCategory: null,
        siblings: [],
        isDirty: false,
      });

      const { isDirty, currentCategory } = reducer(state, {
        type: 'applicationCategories/createEdit/setCategoryName',
        payload: 'name',
      });

      expect(isDirty).toBe(true);
      expect(currentCategory.name).toEqual({
        isPristine: false,
        value: 'name',
        trimmedValue: 'name',
        validationErrors: [],
      });
    });

    it('gets an error from using an app category with two or more spaces in a row', () => {
      const state = Object.freeze({
        submitError: 'error',
        isDirty: true,
        siblings: [
          {
            color: 'dark-blue',
            description: 'description',
            name: 'Dark Blue',
            id: '1242345',
          },
        ],
        currentCategory: {
          color: 'foo',
          description: rscInitialState(''),
          name: rscInitialState(''),
        },
        serverCategory: null,
      });

      const {
        currentCategory: { name },
      } = reducer(state, {
        type: 'applicationCategories/createEdit/setCategoryName',
        payload: ' Dark   Blue ',
      });

      expect(name.trimmedValue).toEqual('Dark   Blue');
      expect(name.validationErrors).toEqual(['No leading, trailing or double spaces or tabs']);
    });

    it('gets an error from using an app category with only spaces', () => {
      const state = Object.freeze({
        submitError: 'error',
        isDirty: true,
        siblings: [
          {
            color: 'dark-blue',
            description: 'description',
            label: 'Dark Blue',
            id: '1242345',
          },
        ],
        currentCategory: {
          color: 'foo',
          description: rscInitialState(''),
          name: rscInitialState(''),
        },
        serverCategory: null,
      });

      const {
        currentCategory: { name },
      } = reducer(state, {
        type: 'applicationCategories/createEdit/setCategoryName',
        payload: ' ',
      });

      expect(name.trimmedValue).toEqual('');
      expect(name.validationErrors).toEqual(['Must be non-empty']);
    });
  });

  describe('applicationCategories/createEdit/resetIsDirty', () => {
    it('resets isDirty property', () => {
      const state = Object.freeze({ isDirty: true });

      const { isDirty } = reducer(state, {
        type: 'applicationCategories/createEdit/resetIsDirty',
      });

      expect(isDirty).toBe(false);
    });
  });

  describe('applicationCategories/createEdit/saveApplicationCategory/pending', () => {
    it('sets submitError property to null', () => {
      const state = Object.freeze({ submitError: 'error' });

      const { submitError } = reducer(state, {
        type: 'applicationCategories/createEdit/saveApplicationCategory/pending',
      });

      expect(submitError).toBeNull();
    });
  });

  describe('applicationCategories/createEdit/saveApplicationCategory/fulfilled', () => {
    it('sets currentCategory and serverCategory to payload, resets isDirty', () => {
      const state = Object.freeze({
        submitError: 'error',
        isDirty: true,
        siblings: [],
        currentCategory: null,
        serverCategory: null,
      });

      const newState = reducer(state, {
        type: 'applicationCategories/createEdit/saveApplicationCategory/fulfilled',
        payload: {
          isEditMode: true,
          savedCategory: {
            color: 'light-brown',
            description: 'description',
            name: 'category name',
            id: '1242345',
          },
        },
      });

      expect(newState.submitError).toBeNull();
      expect(newState.isDirty).toBe(false);
      expect(newState.currentCategory).toEqual({
        color: 'light-brown',
        description: { isPristine: true, value: 'description', trimmedValue: 'description', validationErrors: null },
        name: { isPristine: true, value: 'category name', trimmedValue: 'category name', validationErrors: null },
        id: '1242345',
      });
      expect(newState.serverCategory).toEqual({
        color: 'light-brown',
        description: 'description',
        name: 'category name',
        id: '1242345',
      });
      expect(newState.siblings).toEqual([
        {
          color: 'light-brown',
          description: 'description',
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
        type: 'applicationCategories/createEdit/saveApplicationCategory/fulfilled',
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
      expect(newState.isDirty).toBe(false);

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

  describe('applicationCategories/createEdit/saveApplicationCategory/rejected', () => {
    it('sets submitError property to error', () => {
      const state = Object.freeze({ submitError: null });

      const { submitError } = reducer(state, {
        type: 'applicationCategories/createEdit/saveApplicationCategory/rejected',
        payload: 'error',
      });

      expect(submitError).toBe('error');
    });
  });

  describe('applicationCategories/createEdit/removeApplicationCategory/pending', () => {
    it('sets deleting property to null', () => {
      const state = Object.freeze({ deleteMaskState: null });
      const { deleteMaskState } = reducer(state, {
        type: 'applicationCategories/createEdit/removeApplicationCategory/pending',
      });
      expect(deleteMaskState).toBe(false);
    });
  });

  describe('applicationCategories/createEdit/removeApplicationCategory/fulfilled', () => {
    it('sets currentCategory, serverCategory to initialState', () => {
      const state = Object.freeze({
        deleteMaskState: null,
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
        type: 'applicationCategories/createEdit/removeApplicationCategory/fulfilled',
        payload: 'id',
      });

      expect(newState.deleteMaskState).toBe(true);

      expect(newState.isDirty).toBe(false);
      expect(newState.currentCategory).toEqual(initialState.currentCategory);
      expect(newState.serverCategory).toEqual(initialState.serverCategory);
      expect(newState.siblings).toEqual([]);
    });
  });

  describe('applicationCategories/createEdit/removeApplicationCategory/rejected', () => {
    it('sets errorState, deleting', () => {
      const state = Object.freeze({
        deleteError: null,
      });

      const { deleteError } = reducer(state, {
        type: 'applicationCategories/createEdit/removeApplicationCategory/rejected',
        payload: 'error',
      });

      expect(deleteError).toBe('error');
    });
  });

  describe('applicationCategories/createEdit/loadApplicableCategories/pending', () => {
    it('resets loading, loadError', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'applicationCategories/createEdit/loadApplicableCategories/pending',
      });

      expect(loading).toBe(true);
      expect(loadError).toBeNull();
    });
  });

  describe('applicationCategories/createEdit/loadApplicableCategories/fulfilled', () => {
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
        type: 'applicationCategories/createEdit/loadApplicableCategories/fulfilled',
        payload: {
          appCategoryOwners: mockAppCategoryOwners,
        },
      });

      expect(newStore.loading).toBe(false);
      expect(newStore.loadError).toBeNull();
      expect(newStore.appCategoryOwners).toEqual(mockAppCategoryOwners);
    });
  });

  describe('applicationCategories/createEdit/loadApplicableCategories/rejected', () => {
    it('sets loading, loadError', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const { loading, loadError } = reducer(state, {
        type: 'applicationCategories/createEdit/loadApplicableCategories/rejected',
        payload: 'error',
      });

      expect(loading).toBe(false);
      expect(loadError).toBe('error');
    });
  });

  describe('applicationCategories/createEdit/loadCategoryEditor/pending', () => {
    it('resets loading, loadError', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'applicationCategories/createEdit/loadCategoryEditor/pending',
      });

      expect(loading).toBe(true);
      expect(loadError).toBeNull();
    });
  });

  describe('applicationCategories/createEdit/loadCategoryEditor/fulfilled', () => {
    it('sets loading, loadError, currentCategory, serverCategory, siblings, associatedApplicationNames, tagPolicyList and resets deleteModal statuses', () => {
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
        deleteMaskState: null,
      });

      const currentCategoryDataToSave = {
        color: 'light-green',
        description: 'description',
        id: 'id',
        name: 'x3',
        ownerId: 'ownerId',
        ownerType: 'APPLICATION',
      };
      const siblings = [
        {
          color: 'light-green',
          description: 'sibling description',
          id: 'ae63051b2e304c3bbabf94c2443b03fb',
          name: 'n3',
          ownerId: '6b365e8a8000449aa924f194a7ed0d21',
          ownerType: 'APPLICATION',
        },
      ];
      const applicationTags = ['associated application name'];
      const tagPolicyList = ['associated policy name'];
      const newState = reducer(state, {
        type: 'applicationCategories/createEdit/loadCategoryEditor/fulfilled',
        payload: {
          currentCategory: currentCategoryDataToSave,
          siblings,
          applicationTags,
          tagPolicyList,
        },
      });
      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBeNull();
      expect(newState.currentCategory).toEqual({
        color: 'light-green',
        name: { isPristine: true, trimmedValue: 'x3', validationErrors: null, value: 'x3' },
        description: { isPristine: true, trimmedValue: 'description', validationErrors: null, value: 'description' },
        id: 'id',
        ownerId: 'ownerId',
        ownerType: 'APPLICATION',
      });
      expect(newState.serverCategory).toEqual(currentCategoryDataToSave);
      expect(newState.siblings).toEqual(siblings);
      expect(newState.deleteModal.applicationTags).toEqual(applicationTags);
      expect(newState.deleteModal.tagPolicyList).toEqual(tagPolicyList);
      expect(newState.deleteMaskState).toBeNull();
    });
  });

  describe('applicationCategories/createEdit/loadCategoryEditor/rejected', () => {
    it('sets loading, loadError', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const { loading, loadError } = reducer(state, {
        type: 'applicationCategories/createEdit/loadCategoryEditor/rejected',
        payload: 'error',
      });

      expect(loading).toBe(false);
      expect(loadError).toBe('error');
    });
  });
});
