/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { initialState } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesLabelsSlice';

describe('orgsAndPoliciesLabels reducer', () => {
  describe('orgsAndPoliciesLabels/setLabelDescription', () => {
    it('sets description to currentLabel and isDirty property', () => {
      const state = Object.freeze({
        currentLabel: {
          color: null,
          description: null,
          label: null,
        },
        serverCurrentLabel: null,
        isDirty: false,
      });

      const { isDirty, currentLabel } = reducer(state, {
        type: 'orgsAndPoliciesLabels/setLabelDescription',
        payload: 'label description',
      });

      expect(isDirty).toBeTrue();
      expect(currentLabel.description).toBe('label description');
    });
  });

  describe('orgsAndPoliciesLabels/setLabelColor', () => {
    it('sets color to currentLabel and isDirty property', () => {
      const state = Object.freeze({
        currentLabel: {
          color: null,
          description: null,
          label: null,
        },
        serverCurrentLabel: null,
        isDirty: false,
      });

      const { isDirty, currentLabel } = reducer(state, {
        type: 'orgsAndPoliciesLabels/setLabelColor',
        payload: 'light-green',
      });

      expect(isDirty).toBeTrue();
      expect(currentLabel.color).toBe('light-green');
    });
  });

  describe('orgsAndPoliciesLabels/setLabelName', () => {
    it('sets label to currentLabel and isDirty property', () => {
      const state = Object.freeze({
        currentLabel: {
          color: null,
          description: null,
          label: null,
        },
        serverCurrentLabel: null,
        isDirty: false,
      });

      const { isDirty, currentLabel } = reducer(state, {
        type: 'orgsAndPoliciesLabels/setLabelName',
        payload: 'name',
      });

      expect(isDirty).toBeTrue();
      expect(currentLabel.label).toBe('name');
    });
  });

  describe('orgsAndPoliciesLabels/resetIsDirty', () => {
    it('resets isDirty property', () => {
      const state = Object.freeze({ isDirty: true });

      const { isDirty } = reducer(state, {
        type: 'orgsAndPoliciesLabels/resetIsDirty',
      });

      expect(isDirty).toBeFalse();
    });
  });

  describe('orgsAndPoliciesLabels/resetDeleteModalState', () => {
    it('resets delete modal related properties property', () => {
      const state = Object.freeze({
        deleting: false,
        success: false,
        errorState: 'error',
      });

      const { deleting, success, errorState } = reducer(state, {
        type: 'orgsAndPoliciesLabels/resetDeleteModalState',
      });

      expect(deleting).toBeNull();
      expect(success).toBeNull();
      expect(errorState).toBeNull();
    });
  });

  describe('orgsAndPoliciesLabels/saveLabel/pending', () => {
    it('sets submitError property to null', () => {
      const state = Object.freeze({ submitError: 'error' });

      const { submitError } = reducer(state, {
        type: 'orgsAndPoliciesLabels/saveLabel/pending',
      });

      expect(submitError).toBeNull();
    });
  });

  describe('orgsAndPoliciesLabels/saveLabel/fulfilled', () => {
    it('sets currentLabel and serverCurrentLable to payload, resets isDirty', () => {
      const state = Object.freeze({
        submitError: 'error',
        isDirty: true,
        siblings: [],
        currentLabel: null,
        serverCurrentLabel: null,
      });

      const newState = reducer(state, {
        type: 'orgsAndPoliciesLabels/saveLabel/fulfilled',
        payload: {
          isEditMode: true,
          label: {
            color: 'dark-green',
            description: null,
            label: 'label name',
            id: '1242345',
          },
        },
      });

      expect(newState.submitError).toBeNull();
      expect(newState.isDirty).toBeFalse();
      expect(newState.currentLabel).toEqual({
        color: 'dark-green',
        description: null,
        label: 'label name',
        id: '1242345',
      });
      expect(newState.serverCurrentLabel).toEqual({
        color: 'dark-green',
        description: null,
        label: 'label name',
        id: '1242345',
      });
      expect(newState.siblings).toEqual([
        {
          color: 'dark-green',
          description: null,
          label: 'label name',
          id: '1242345',
        },
      ]);
    });

    it('sets currentLabel and serverCurrentLable to initial state, resets isDirty in create mode', () => {
      const state = Object.freeze({
        submitError: 'error',
        isDirty: true,
        siblings: [],
        currentLabel: {
          color: 'foo',
          description: 'description',
          label: 'name',
        },
        serverCurrentLabel: null,
      });

      const newState = reducer(state, {
        type: 'orgsAndPoliciesLabels/saveLabel/fulfilled',
        payload: {
          isEditMode: false,
          label: {
            color: 'dark-green',
            description: null,
            label: 'label name',
            id: '1242345',
          },
        },
      });

      expect(newState.submitError).toBeNull();
      expect(newState.isDirty).toBeFalse();

      expect(newState.currentLabel).toEqual(initialState.currentLabel);
      expect(newState.serverCurrentLabel).toEqual(initialState.currentLabel);
      expect(newState.siblings).toEqual([
        {
          color: 'dark-green',
          description: null,
          label: 'label name',
          id: '1242345',
        },
      ]);
    });
  });

  describe('orgsAndPoliciesLabels/saveLabel/rejected', () => {
    it('sets submitError property to error', () => {
      const state = Object.freeze({ submitError: null });

      const { submitError } = reducer(state, {
        type: 'orgsAndPoliciesLabels/saveLabel/rejected',
        payload: 'error',
      });

      expect(submitError).toBe('error');
    });
  });

  describe('orgsAndPoliciesLabels/removeLabel/pending', () => {
    it('sets deleting property to null', () => {
      const state = Object.freeze({ deleting: false });

      const { deleting } = reducer(state, {
        type: 'orgsAndPoliciesLabels/removeLabel/pending',
      });

      expect(deleting).toBeTrue();
    });
  });

  describe('orgsAndPoliciesLabels/removeLabel/fulfilled', () => {
    it('sets currentlabel, serverCurrentLabel to initialState', () => {
      const state = Object.freeze({
        deleting: true,
        errorState: null,
        isDirty: true,
        success: null,
        currentLabel: {
          color: 'foo',
          description: 'description',
          label: 'name',
          id: 'id',
        },
        serverCurrentLabel: {
          color: 'foo',
          description: 'description',
          label: 'name',
          id: 'id',
        },
        siblings: [
          {
            color: 'foo',
            description: 'description',
            label: 'name',
            id: 'id',
          },
        ],
      });

      const newState = reducer(state, {
        type: 'orgsAndPoliciesLabels/removeLabel/fulfilled',
        payload: 'id',
      });

      expect(newState.deleting).toBeNull();
      expect(newState.errorState).toBeNull();

      expect(newState.isDirty).toBeFalse();
      expect(newState.success).toBeTrue();
      expect(newState.currentLabel).toEqual(initialState.currentLabel);
      expect(newState.serverCurrentLabel).toEqual(initialState.currentLabel);
      expect(newState.siblings).toEqual([]);
    });
  });

  describe('orgsAndPoliciesLabels/removeLabel/rejected', () => {
    it('sets errorState, deletins properties', () => {
      const state = Object.freeze({
        deleting: true,
        errorState: null,
      });

      const { deleting, errorState } = reducer(state, {
        type: 'orgsAndPoliciesLabels/removeLabel/rejected',
        payload: 'error',
      });

      expect(deleting).toBeFalse();
      expect(errorState).toBe('error');
    });
  });

  describe('orgsAndPoliciesLabels/loadApplicableLabels/pending', () => {
    it('resets loading, loadError properties', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'orgsAndPoliciesLabels/loadApplicableLabels/pending',
      });

      expect(loading).toBeTrue();
      expect(loadError).toBeNull();
    });
  });

  describe('orgsAndPoliciesLabels/loadApplicableLabels/fulfilled', () => {
    it('sets loading, loadError, applicableLabels, ownerName properties', () => {
      const state = Object.freeze({
        loading: true,
        loadError: 'error',
        applicableLabels: null,
      });

      const newStore = reducer(state, {
        type: 'orgsAndPoliciesLabels/loadApplicableLabels/fulfilled',
        payload: {
          labelsByOwner: [
            {
              ownerId: '6b365e8a8000449aa924f194a7ed0d21',
              ownerType: 'APPLICATION',
              ownerName: 'appname',
              labels: [
                {
                  color: 'light-green',
                  description: null,
                  id: 'ae63051b2e304c3bbabf94c2443b03fb',
                  label: 'n3',
                  ownerId: '6b365e8a8000449aa924f194a7ed0d21',
                  ownerType: 'APPLICATION',
                },
              ],
            },
          ],
        },
      });

      expect(newStore.loading).toBeFalse();
      expect(newStore.loadError).toBeNull();
      expect(newStore.applicableLabels).toEqual([
        {
          ownerId: '6b365e8a8000449aa924f194a7ed0d21',
          ownerType: 'APPLICATION',
          ownerName: 'appname',
          labels: [
            {
              color: 'light-green',
              description: null,
              id: 'ae63051b2e304c3bbabf94c2443b03fb',
              label: 'n3',
              ownerId: '6b365e8a8000449aa924f194a7ed0d21',
              ownerType: 'APPLICATION',
            },
          ],
        },
      ]);
    });
  });

  describe('orgsAndPoliciesLabels/loadApplicableLabels/rejected', () => {
    it('sets loading, loadError properties', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const { loading, loadError } = reducer(state, {
        type: 'orgsAndPoliciesLabels/loadApplicableLabels/rejected',
        payload: 'error',
      });

      expect(loading).toBeFalse();
      expect(loadError).toBe('error');
    });
  });

  describe('orgsAndPoliciesLabels/loadLabelsEditor/pending', () => {
    it('resets loading, loadError properties', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'orgsAndPoliciesLabels/loadLabelsEditor/pending',
      });

      expect(loading).toBeTrue();
      expect(loadError).toBeNull();
    });
  });

  describe('orgsAndPoliciesLabels/loadLabelsEditor/fulfilled', () => {
    it('sets loading, loadError, currentLabel, serverCurrentLabel, siblings properties', () => {
      const state = Object.freeze({
        loading: true,
        loadError: 'error',
        currentLabel: null,
        serverCurrentLabel: null,
        siblings: [],
      });

      const newState = reducer(state, {
        type: 'orgsAndPoliciesLabels/loadLabelsEditor/fulfilled',
        payload: {
          currentLabel: {
            color: 'light-green',
            description: null,
            id: 'ae63051b2e304c3bbabf94c2443b03fb',
            label: 'n3',
            ownerId: '6b365e8a8000449aa924f194a7ed0d21',
            ownerType: 'APPLICATION',
          },
          siblings: [
            {
              color: 'light-green',
              description: null,
              id: 'ae63051b2e304c3bbabf94c2443b03fb',
              label: 'n3',
              ownerId: '6b365e8a8000449aa924f194a7ed0d21',
              ownerType: 'APPLICATION',
            },
          ],
        },
      });

      expect(newState.loading).toBeFalse();
      expect(newState.loadError).toBeNull();
      expect(newState.currentLabel).toEqual({
        color: 'light-green',
        description: null,
        id: 'ae63051b2e304c3bbabf94c2443b03fb',
        label: 'n3',
        ownerId: '6b365e8a8000449aa924f194a7ed0d21',
        ownerType: 'APPLICATION',
      });
      expect(newState.serverCurrentLabel).toEqual({
        color: 'light-green',
        description: null,
        id: 'ae63051b2e304c3bbabf94c2443b03fb',
        label: 'n3',
        ownerId: '6b365e8a8000449aa924f194a7ed0d21',
        ownerType: 'APPLICATION',
      });
      expect(newState.siblings).toEqual([
        {
          color: 'light-green',
          description: null,
          id: 'ae63051b2e304c3bbabf94c2443b03fb',
          label: 'n3',
          ownerId: '6b365e8a8000449aa924f194a7ed0d21',
          ownerType: 'APPLICATION',
        },
      ]);
    });
  });

  describe('orgsAndPoliciesLabels/loadLabelsEditor/rejected', () => {
    it('sets loading, loadError properties', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const { loading, loadError } = reducer(state, {
        type: 'orgsAndPoliciesLabels/loadLabelsEditor/rejected',
        payload: 'error',
      });

      expect(loading).toBeFalse();
      expect(loadError).toBe('error');
    });
  });
});
