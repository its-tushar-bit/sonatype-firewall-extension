/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { initialState } from 'MainRoot/OrgsAndPolicies/labelsSlice';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

const { initialState: rscInitialState } = nxTextInputStateHelpers;

describe('labels reducer', () => {
  describe('labels/setLabelDescription', () => {
    it('sets description to currentLabel and isDirty property', () => {
      const state = Object.freeze({
        currentLabel: {
          color: null,
          description: rscInitialState(''),
          label: rscInitialState(''),
        },
        serverCurrentLabel: null,
        isDirty: false,
      });

      const { isDirty, currentLabel } = reducer(state, {
        type: 'labels/setLabelDescription',
        payload: 'label description',
      });

      expect(isDirty).toBe(true);
      expect(currentLabel.description).toEqual({
        isPristine: false,
        value: 'label description',
        trimmedValue: 'label description',
        validationErrors: null,
      });
    });
  });

  describe('labels/setCurrentOwnerProps', () => {
    it('sets description to currentLabel and isDirty property', () => {
      const state = Object.freeze({
        ownerType: null,
        ownerId: null,
      });

      const { ownerType, ownerId } = reducer(state, {
        type: 'labels/setCurrentOwnerProps',
        payload: {
          ownerType: 'ownerType',
          ownerId: 'ownerId',
        },
      });

      expect(ownerType).toBe('ownerType');
      expect(ownerId).toBe('ownerId');
    });
  });

  describe('labels/setLabelColor', () => {
    it('sets color to currentLabel and isDirty property', () => {
      const state = Object.freeze({
        currentLabel: {
          color: null,
          description: rscInitialState(''),
          label: rscInitialState(''),
        },
        serverCurrentLabel: null,
        isDirty: false,
      });

      const { isDirty, currentLabel } = reducer(state, {
        type: 'labels/setLabelColor',
        payload: 'light-green',
      });

      expect(isDirty).toBe(true);
      expect(currentLabel.color).toBe('light-green');
    });
  });

  describe('labels/setLabelName', () => {
    it('sets label to currentLabel and isDirty property', () => {
      const state = Object.freeze({
        currentLabel: {
          color: null,
          description: '',
          label: null,
        },
        serverCurrentLabel: null,
        isDirty: false,
        siblings: [],
      });

      const { isDirty, currentLabel } = reducer(state, {
        type: 'labels/setLabelName',
        payload: 'name',
      });

      expect(isDirty).toBe(true);
      expect(currentLabel.label).toEqual({
        isPristine: false,
        value: 'name',
        trimmedValue: 'name',
        validationErrors: [],
      });
    });

    it('gets an error from using a label with two or more spaces in a row', () => {
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
        currentLabel: {
          color: 'foo',
          description: rscInitialState(''),
          label: rscInitialState(''),
        },
        serverCurrentLabel: null,
      });

      const {
        currentLabel: { label },
      } = reducer(state, {
        type: 'labels/setLabelName',
        payload: ' Dark   Blue ',
      });

      expect(label.trimmedValue).toEqual('Dark   Blue');
      expect(label.validationErrors).toEqual(['No leading, trailing or double spaces or tabs']);
    });

    it('gets an error from using a label with only spaces', () => {
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
        currentLabel: {
          color: 'foo',
          description: rscInitialState(''),
          label: rscInitialState(''),
        },
        serverCurrentLabel: null,
      });

      const {
        currentLabel: { label },
      } = reducer(state, {
        type: 'labels/setLabelName',
        payload: ' ',
      });

      expect(label.trimmedValue).toEqual('');
      expect(label.validationErrors).toEqual(['Must be non-empty']);
    });
  });

  describe('labels/resetIsDirty', () => {
    it('resets isDirty property', () => {
      const state = Object.freeze({ isDirty: true });

      const { isDirty } = reducer(state, {
        type: 'labels/resetIsDirty',
      });

      expect(isDirty).toBe(false);
    });
  });

  describe('labels/resetDeleteModalState', () => {
    it('resets delete modal related properties property', () => {
      const state = Object.freeze({
        deleting: false,
        success: false,
        errorState: 'error',
      });

      const { deleting, success, errorState } = reducer(state, {
        type: 'labels/resetDeleteModalState',
      });

      expect(deleting).toBeNull();
      expect(success).toBeNull();
      expect(errorState).toBeNull();
    });
  });

  describe('labels/saveLabel/pending', () => {
    it('sets submitError property to null', () => {
      const state = Object.freeze({ submitError: 'error' });

      const { submitError } = reducer(state, {
        type: 'labels/saveLabel/pending',
      });

      expect(submitError).toBeNull();
    });
  });

  describe('labels/saveLabel/fulfilled', () => {
    it('sets currentLabel and serverCurrentLable to payload, resets isDirty in edit mode', () => {
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
        currentLabel: null,
        serverCurrentLabel: null,
      });

      const newState = reducer(state, {
        type: 'labels/saveLabel/fulfilled',
        payload: {
          isEditMode: true,
          label: {
            color: 'dark-green',
            description: '',
            label: 'label name',
            id: '1242345',
          },
        },
      });

      expect(newState.submitError).toBeNull();
      expect(newState.isDirty).toBe(false);
      expect(newState.currentLabel).toEqual({
        color: 'dark-green',
        description: { isPristine: true, value: '', trimmedValue: '', validationErrors: null },
        label: { isPristine: true, value: 'label name', trimmedValue: 'label name', validationErrors: null },
        id: '1242345',
      });
      expect(newState.serverCurrentLabel).toEqual({
        color: 'dark-green',
        description: '',
        label: 'label name',
        id: '1242345',
      });
      expect(newState.siblings).toEqual([
        {
          color: 'dark-green',
          description: '',
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
        type: 'labels/saveLabel/fulfilled',
        payload: {
          isEditMode: false,
          label: {
            color: 'dark-green',
            description: '',
            label: 'label name',
            id: '1242345',
          },
        },
      });

      expect(newState.submitError).toBeNull();
      expect(newState.isDirty).toBe(false);

      expect(newState.currentLabel).toEqual(initialState.currentLabel);
      expect(newState.serverCurrentLabel).toEqual(initialState.currentLabel);
      expect(newState.siblings).toEqual([
        {
          color: 'dark-green',
          description: '',
          label: 'label name',
          id: '1242345',
        },
      ]);
    });
  });

  describe('labels/saveLabel/rejected', () => {
    it('sets submitError property to error', () => {
      const state = Object.freeze({ submitError: null });

      const { submitError } = reducer(state, {
        type: 'labels/saveLabel/rejected',
        payload: 'error',
      });

      expect(submitError).toBe('error');
    });
  });

  describe('labels/removeLabel/pending', () => {
    it('sets deleting property to null', () => {
      const state = Object.freeze({ deleting: false });

      const { deleting } = reducer(state, {
        type: 'labels/removeLabel/pending',
      });

      expect(deleting).toBe(false);
    });
  });

  describe('labels/removeLabel/fulfilled', () => {
    it('sets currentlabel, serverCurrentLabel to initialState', () => {
      const state = Object.freeze({
        deleteError: null,
        isDirty: true,
        deleteMaskState: null,
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
        type: 'labels/removeLabel/fulfilled',
        payload: 'id',
      });

      expect(newState.deleteError).toBeNull();

      expect(newState.isDirty).toBe(false);
      expect(newState.deleteMaskState).toBe(true);
      expect(newState.currentLabel).toEqual(initialState.currentLabel);
      expect(newState.serverCurrentLabel).toEqual(initialState.currentLabel);
      expect(newState.siblings).toEqual([]);
    });
  });

  describe('labels/removeLabel/rejected', () => {
    it('sets errorState, deletins properties', () => {
      const state = Object.freeze({
        deleteError: null,
      });

      const { deleteError } = reducer(state, {
        type: 'labels/removeLabel/rejected',
        payload: 'error',
      });

      expect(deleteError).toBe('error');
    });
  });

  describe('labels/loadApplicableLabels/pending', () => {
    it('resets loading, loadError properties', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'labels/loadApplicableLabels/pending',
      });

      expect(loading).toBe(true);
      expect(loadError).toBeNull();
    });
  });

  describe('labels/loadApplicableLabels/fulfilled', () => {
    it('sets loading, loadError, applicableLabels, ownerName properties', () => {
      const state = Object.freeze({
        loading: true,
        loadError: 'error',
        applicableLabels: null,
      });

      const newStore = reducer(state, {
        type: 'labels/loadApplicableLabels/fulfilled',
        payload: [
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
          {
            ownerId: '6b365e8a8000449aa924f194a7ed0d22',
            ownerType: 'APPLICATION',
            ownerName: 'appname2',
            labels: [
              {
                color: 'dark-green',
                description: null,
                id: 'ae63051b2e304c3bbabf94c2443b03fa',
                label: 'n4',
                ownerId: '6b365e8a8000449aa924f194a7ed0d22',
                ownerType: 'APPLICATION',
              },
            ],
          },
        ],
      });

      expect(newStore.loading).toBe(false);
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
        {
          ownerId: '6b365e8a8000449aa924f194a7ed0d22',
          ownerType: 'APPLICATION',
          ownerName: 'appname2',
          labels: [
            {
              color: 'dark-green',
              description: null,
              id: 'ae63051b2e304c3bbabf94c2443b03fa',
              label: 'n4',
              ownerId: '6b365e8a8000449aa924f194a7ed0d22',
              ownerType: 'APPLICATION',
            },
          ],
        },
      ]);
      expect(newStore.inheritedLabelsOpen).toEqual({
        '6b365e8a8000449aa924f194a7ed0d21': true,
        '6b365e8a8000449aa924f194a7ed0d22': true,
      });
    });
  });

  describe('labels/loadApplicableLabels/rejected', () => {
    it('sets loading, loadError properties', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const { loading, loadError } = reducer(state, {
        type: 'labels/loadApplicableLabels/rejected',
        payload: 'error',
      });

      expect(loading).toBe(false);
      expect(loadError).toBe('error');
    });
  });

  describe('labels/loadLabelsEditor/pending', () => {
    it('resets loading, loadError properties', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'labels/loadLabelsEditor/pending',
      });

      expect(loading).toBe(true);
      expect(loadError).toBeNull();
    });
  });

  describe('labels/loadLabelsEditor/fulfilled', () => {
    it('sets loading, loadError, currentLabel, serverCurrentLabel, siblings properties', () => {
      const state = Object.freeze({
        loading: true,
        loadError: 'error',
        currentLabel: null,
        serverCurrentLabel: null,
        siblings: [],
      });

      const newState = reducer(state, {
        type: 'labels/loadLabelsEditor/fulfilled',
        payload: {
          currentLabel: {
            color: 'light-green',
            description: '',
            id: 'ae63051b2e304c3bbabf94c2443b03fb',
            label: 'n3',
            ownerId: '6b365e8a8000449aa924f194a7ed0d21',
            ownerType: 'APPLICATION',
          },
          siblings: [
            {
              color: 'light-green',
              description: '',
              id: 'ae63051b2e304c3bbabf94c2443b03fb',
              label: 'n3',
              ownerId: '6b365e8a8000449aa924f194a7ed0d21',
              ownerType: 'APPLICATION',
            },
          ],
        },
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBeNull();
      expect(newState.currentLabel).toEqual({
        color: 'light-green',
        description: { isPristine: true, value: '', trimmedValue: '', validationErrors: null },
        id: 'ae63051b2e304c3bbabf94c2443b03fb',
        label: { isPristine: true, value: 'n3', trimmedValue: 'n3', validationErrors: null },
        ownerId: '6b365e8a8000449aa924f194a7ed0d21',
        ownerType: 'APPLICATION',
      });
      expect(newState.serverCurrentLabel).toEqual({
        color: 'light-green',
        description: '',
        id: 'ae63051b2e304c3bbabf94c2443b03fb',
        label: 'n3',
        ownerId: '6b365e8a8000449aa924f194a7ed0d21',
        ownerType: 'APPLICATION',
      });
      expect(newState.siblings).toEqual([
        {
          color: 'light-green',
          description: '',
          id: 'ae63051b2e304c3bbabf94c2443b03fb',
          label: 'n3',
          ownerId: '6b365e8a8000449aa924f194a7ed0d21',
          ownerType: 'APPLICATION',
        },
      ]);
    });

    it('sets loading, loadError, currentLabel, serverCurrentLabel and does not set siblings', () => {
      const state = Object.freeze({
        loading: true,
        loadError: 'error',
        currentLabel: null,
        serverCurrentLabel: null,
        siblings: [],
      });

      const newState = reducer(state, {
        type: 'labels/loadLabelsEditor/fulfilled',
        payload: {
          currentLabel: {
            color: 'light-green',
            description: '',
            id: 'ae63051b2e304c3bbabf94c2443b03fb',
            label: 'n3',
            ownerId: '6b365e8a8000449aa924f194a7ed0d21',
            ownerType: 'APPLICATION',
          },
          siblings: null,
        },
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBeNull();
      expect(newState.currentLabel).toEqual({
        color: 'light-green',
        description: { isPristine: true, value: '', trimmedValue: '', validationErrors: null },
        id: 'ae63051b2e304c3bbabf94c2443b03fb',
        label: { isPristine: true, value: 'n3', trimmedValue: 'n3', validationErrors: null },
        ownerId: '6b365e8a8000449aa924f194a7ed0d21',
        ownerType: 'APPLICATION',
      });
      expect(newState.serverCurrentLabel).toEqual({
        color: 'light-green',
        description: '',
        id: 'ae63051b2e304c3bbabf94c2443b03fb',
        label: 'n3',
        ownerId: '6b365e8a8000449aa924f194a7ed0d21',
        ownerType: 'APPLICATION',
      });
      expect(newState.siblings).toEqual([]);
    });
  });

  describe('labels/loadLabelsEditor/rejected', () => {
    it('sets loading, loadError properties', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const { loading, loadError } = reducer(state, {
        type: 'labels/loadLabelsEditor/rejected',
        payload: 'error',
      });

      expect(loading).toBe(false);
      expect(loadError).toBe('error');
    });
  });

  describe('labels/toggleInheritedLabelsOpen', () => {
    it('toggles inheritedLabelsOpen to true for the given ownerId', () => {
      const state = Object.freeze({
        inheritedLabelsOpen: {
          ownerId: false,
          otherOwnerId1: false,
          otherOwnerId2: true,
        },
      });

      const { inheritedLabelsOpen } = reducer(state, {
        type: 'labels/toggleInheritedLabelsOpen',
        payload: 'ownerId',
      });

      expect(inheritedLabelsOpen).toEqual({
        ownerId: true,
        otherOwnerId1: false,
        otherOwnerId2: true,
      });
    });

    it('toggles inheritedLabelsOpen to false for the given ownerId', () => {
      const state = Object.freeze({
        inheritedLabelsOpen: {
          ownerId: true,
          otherOwnerId1: false,
          otherOwnerId2: true,
        },
      });

      const { inheritedLabelsOpen } = reducer(state, {
        type: 'labels/toggleInheritedLabelsOpen',
        payload: 'ownerId',
      });

      expect(inheritedLabelsOpen).toEqual({
        ownerId: false,
        otherOwnerId1: false,
        otherOwnerId2: true,
      });
    });
  });
});
