/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSlice';

describe('assignApplicationCategories reducer', () => {
  const otherState = Object.freeze({
    data: 'data',
  });

  describe('applicationCategories/assign/loadApplicableCategories/pending', () => {
    it('sets loadingApplicableCategories, resets loadApplicableCategoriesError', () => {
      const state = Object.freeze({
        otherState,
        loadingApplicableCategories: false,
        loadApplicableCategoriesError: 'error',
      });

      const { loadingApplicableCategories, loadApplicableCategoriesError, otherState: expectedOtherState } = reducer(
        state,
        {
          type: 'applicationCategories/assign/loadApplicableCategories/pending',
        }
      );

      expect(loadingApplicableCategories).toBe(true);
      expect(loadApplicableCategoriesError).toBeNull();
      expect(expectedOtherState).toEqual(otherState);
    });
  });

  describe('applicationCategories/assign/loadApplicableCategories/fulfilled', () => {
    it('resets loadingApplications, sets applications', () => {
      const state = Object.freeze({
        otherState,
        loadingApplicableCategories: true,
        applicableCategories: [],
      });
      const expectedCategories = [
        {
          id: '13dfce231ca24289bec319fddf4bef88',
          organizationId: 'ROOT_ORGANIZATION_ID',
          name: 'Internal',
          description: 'Applications that are used only by your employees',
          color: 'dark-green',
        },
      ];

      const { loadingApplicableCategories, applicableCategories, otherState: expectedOtherState } = reducer(state, {
        type: 'applicationCategories/assign/loadApplicableCategories/fulfilled',
        payload: expectedCategories,
      });

      expect(loadingApplicableCategories).toBe(false);
      expect(applicableCategories).toEqual(expectedCategories);
      expect(expectedOtherState).toEqual(otherState);
    });
  });

  describe('applicationCategories/assign/loadApplicableCategories/rejected', () => {
    it('sets loadApplicableCategoriesError,  resets loadingApplicableCategories', () => {
      const state = Object.freeze({ otherState, loadingApplications: true, loadApplicationsError: null });

      const { loadingApplicableCategories, loadApplicableCategoriesError, otherState: expectedOtherState } = reducer(
        state,
        {
          type: 'applicationCategories/assign/loadApplicableCategories/rejected',
          payload: 'error',
        }
      );

      expect(loadingApplicableCategories).toBe(false);
      expect(loadApplicableCategoriesError).toBe('error');
      expect(expectedOtherState).toEqual(otherState);
    });
  });

  describe('applicationCategories/assign/loadAppliedCategories/pending', () => {
    it('sets loadingAppliedCategories, resets loadAppliedCategoriesError', () => {
      const state = Object.freeze({
        otherState,
        loadingAppliedCategories: false,
        loadAppliedCategoriesError: 'error',
      });

      const { loadingAppliedCategories, loadAppliedCategoriesError, otherState: expectedOtherState } = reducer(state, {
        type: 'applicationCategories/assign/loadAppliedCategories/pending',
      });

      expect(loadingAppliedCategories).toBe(true);
      expect(loadAppliedCategoriesError).toBeNull();
      expect(expectedOtherState).toEqual(otherState);
    });
  });

  describe('applicationCategories/assign/loadAppliedCategories/fulfilled', () => {
    it('resets loadingAppliedCategories, sets appliedCategories and originalAppliedCategories', () => {
      const state = Object.freeze({
        otherState,
        loadingAppliedCategories: true,
        appliedCategories: [],
        originalAppliedCategories: [],
      });
      const expectedCategories = [
        {
          id: '13dfce231ca24289bec319fddf4bef88',
          organizationId: 'ROOT_ORGANIZATION_ID',
          name: 'Internal',
          description: 'Applications that are used only by your employees',
          color: 'dark-green',
        },
      ];

      const {
        loadingAppliedCategories,
        appliedCategories,
        originalAppliedCategories,
        otherState: expectedOtherState,
      } = reducer(state, {
        type: 'applicationCategories/assign/loadAppliedCategories/fulfilled',
        payload: expectedCategories,
      });

      expect(loadingAppliedCategories).toBe(false);
      expect(appliedCategories).toEqual(expectedCategories);
      expect(originalAppliedCategories).toEqual(expectedCategories);
      expect(expectedOtherState).toEqual(otherState);
    });
  });

  describe('applicationCategories/assign/loadAppliedCategories/rejected', () => {
    it('sets loadAppliedCategoriesError,  resets loadingAppliedCategories', () => {
      const state = Object.freeze({ otherState, loadingAppliedCategories: true, loadAppliedCategoriesError: null });

      const { loadingAppliedCategories, loadAppliedCategoriesError, otherState: expectedOtherState } = reducer(state, {
        type: 'applicationCategories/assign/loadAppliedCategories/rejected',
        payload: 'error',
      });

      expect(loadingAppliedCategories).toBe(false);
      expect(loadAppliedCategoriesError).toBe('error');
      expect(expectedOtherState).toEqual(otherState);
    });
  });

  describe('applicationCategories/assign/saveAppliedCategories/pending', () => {
    it('sets submitMaskState, resets submitError', () => {
      const state = Object.freeze({
        otherState,
        submitMaskState: false,
        submitError: 'error',
      });

      const { submitMaskState, submitError, otherState: expectedOtherState } = reducer(state, {
        type: 'applicationCategories/assign/saveAppliedCategories/pending',
      });

      expect(submitMaskState).toBe(false);
      expect(submitError).toBeNull();
      expect(expectedOtherState).toEqual(otherState);
    });
  });

  describe('applicationCategories/assign/saveAppliedCategories/fulfilled', () => {
    it('resets submitMaskState, sets originalAppliedCategories', () => {
      const expectedCategories = [
        {
          id: '13dfce231ca24289bec319fddf4bef88',
          organizationId: 'ROOT_ORGANIZATION_ID',
          name: 'Internal',
          description: 'Applications that are used only by your employees',
          color: 'dark-green',
        },
      ];

      const state = Object.freeze({
        otherState,
        submitMaskState: true,
        appliedCategories: expectedCategories,
        originalAppliedCategories: [],
        isDirty: true,
      });

      const { submitMaskState, isDirty, originalAppliedCategories, otherState: expectedOtherState } = reducer(state, {
        type: 'applicationCategories/assign/saveAppliedCategories/fulfilled',
      });

      expect(submitMaskState).toBe(true);
      expect(isDirty).toBe(false);
      expect(originalAppliedCategories).toEqual(expectedCategories);
      expect(expectedOtherState).toEqual(otherState);
    });
  });

  describe('applicationCategories/assign/saveAppliedCategories/rejected', () => {
    it('sets submitError,  resets submitMaskState', () => {
      const state = Object.freeze({ otherState, submitMaskState: true, submitError: null });

      const { submitMaskState, submitError, otherState: expectedOtherState } = reducer(state, {
        type: 'applicationCategories/assign/saveAppliedCategories/rejected',
        payload: 'error',
      });

      expect(submitMaskState).toBeNull();
      expect(submitError).toBe('error');
      expect(expectedOtherState).toEqual(otherState);
    });
  });

  describe('applicationCategories/assign/updateAppliedCategories', () => {
    it('appends an new tag', () => {
      const existingAppliedCategories = [
        {
          id: '13dfce231ca24289bec319fddf4bef88',
          organizationId: 'ROOT_ORGANIZATION_ID',
          name: 'Internal',
          description: 'Applications that are used only by your employees',
          color: 'dark-green',
        },
      ];

      const state = Object.freeze({
        otherState,
        appliedCategories: existingAppliedCategories,
        originalAppliedCategories: existingAppliedCategories,
        isDirty: false,
      });

      const { appliedCategories, isDirty, otherState: expectedOtherState } = reducer(state, {
        type: 'applicationCategories/assign/updateAppliedCategories',
        payload: {
          id: '13dfce231ca24289bec319fddf4bef80',
          organizationId: 'ROOT_ORGANIZATION_ID',
          name: 'Internal',
          description: 'Applications that are used only',
          color: 'dark-blue',
        },
      });

      expect(isDirty).toBe(true);
      expect(appliedCategories).toEqual([
        {
          id: '13dfce231ca24289bec319fddf4bef88',
          organizationId: 'ROOT_ORGANIZATION_ID',
          name: 'Internal',
          description: 'Applications that are used only by your employees',
          color: 'dark-green',
        },
        {
          id: '13dfce231ca24289bec319fddf4bef80',
          organizationId: 'ROOT_ORGANIZATION_ID',
          name: 'Internal',
          description: 'Applications that are used only',
          color: 'dark-blue',
        },
      ]);
      expect(expectedOtherState).toEqual(otherState);
    });

    it('removes an existing tag', () => {
      const existingAppliedCategories = [
        {
          id: '13dfce231ca24289bec319fddf4bef88',
          organizationId: 'ROOT_ORGANIZATION_ID',
          name: 'Internal',
          description: 'Applications that are used only by your employees',
          color: 'dark-green',
        },
        {
          id: '13dfce231ca24289bec319fddf4bef80',
          organizationId: 'ROOT_ORGANIZATION_ID',
          name: 'Internal',
          description: 'Applications that are used only',
          color: 'dark-blue',
        },
      ];

      const state = Object.freeze({
        otherState,
        appliedCategories: existingAppliedCategories,
        originalAppliedCategories: existingAppliedCategories,
        isDirty: false,
      });

      const { appliedCategories, isDirty, otherState: expectedOtherState } = reducer(state, {
        type: 'applicationCategories/assign/updateAppliedCategories',
        payload: {
          id: '13dfce231ca24289bec319fddf4bef80',
          organizationId: 'ROOT_ORGANIZATION_ID',
          name: 'Internal',
          description: 'Applications that are used only',
          color: 'dark-blue',
        },
      });

      expect(isDirty).toBe(true);
      expect(appliedCategories).toEqual([
        {
          id: '13dfce231ca24289bec319fddf4bef88',
          organizationId: 'ROOT_ORGANIZATION_ID',
          name: 'Internal',
          description: 'Applications that are used only by your employees',
          color: 'dark-green',
        },
      ]);
      expect(expectedOtherState).toEqual(otherState);
    });
  });
});
