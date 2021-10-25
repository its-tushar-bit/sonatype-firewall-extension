/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import reducer, { initialState } from 'MainRoot/componentDetails/claim/claimSlice';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('claim reducer', () => {
  describe('componentDetailsClaim/loadComponentIdentified/pending', () => {
    it('sets loading property to true', () => {
      const state = Object.freeze({
        loading: false,
      });

      const { loading } = reducer(state, {
        type: 'componentDetailsClaim/loadComponentIdentified/pending',
      });

      expect(loading).toBe(true);
    });
  });

  describe('componentDetailsClaim/loadComponentIdentified/fulfilled', () => {
    it('sets loading property to true, serverData, inputFields and loadError', () => {
      const state = Object.freeze({
        loading: true,
        loadError: 'error',
        serverData: null,
        inputFields: {},
      });

      const payload = {
        componentIdentifier: {
          coordinates: {
            artifactId: 'artifactId',
            classifier: 'classifier',
            extension: 'extension',
            groupId: 'groupId',
            version: 'version2',
          },
          format: 'maven',
        },
        createTime: null,
        id: '303',
        comment: 'text',
      };

      const { loading, loadError, serverData, inputFields } = reducer(state, {
        type: 'componentDetailsClaim/loadComponentIdentified/fulfilled',
        payload,
      });

      expect(loading).toBe(false);
      expect(loadError).toBeNull();
      expect(serverData).toEqual(payload);
      expect(inputFields).toEqual({
        artifactId: initUserInput('artifactId'),
        classifier: initUserInput('classifier'),
        extension: initUserInput('extension'),
        groupId: initUserInput('groupId'),
        version: initUserInput('version2'),
        comment: initUserInput('text'),
        createTime: initUserInput(''),
      });
    });
  });

  describe('componentDetailsClaim/loadComponentIdentified/rejected', () => {
    it('sets loading property to false, sets loadError to null if rejected due to 404 error', () => {
      const state = Object.freeze({
        loading: true,
        loadError: 'some error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'componentDetailsClaim/loadComponentIdentified/rejected',
        payload: {
          response: { status: 404 },
        },
      });

      expect(loading).toBe(false);
      expect(loadError).toBeNull();
    });

    it('sets loading property to false, sets loadError to value', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const { loading, loadError } = reducer(state, {
        type: 'componentDetailsClaim/loadComponentIdentified/rejected',
        payload: 'some error',
      });

      expect(loading).toBe(false);
      expect(loadError).toBe('some error');
    });
  });

  describe('componentDetailsClaim/claim/pending', () => {
    it('sets claimMaskState property to false, claimError to null', () => {
      const state = Object.freeze({
        claimError: 'error',
        claimMaskState: null,
      });

      const { claimError, claimMaskState } = reducer(state, {
        type: 'componentDetailsClaim/claim/pending',
      });

      expect(claimError).toBeNull();
      expect(claimMaskState).toBe(false);
    });
  });

  describe('componentDetailsClaim/claim/fulfilled', () => {
    it('sets claimMaskState, claimError, serverData, loading, isDirty, inputFields and loadError', () => {
      const state = Object.freeze({
        loading: true,
        loadError: 'error',
        serverData: null,
        inputFields: {},
      });

      const payload = {
        componentIdentifier: {
          coordinates: {
            artifactId: 'artifactId2',
            classifier: 'classifier2',
            extension: 'extension2',
            groupId: 'groupId2',
            version: 'version3',
          },
          format: 'maven',
        },
        createTime: null,
        comment: 'text',
      };

      const { claimMaskState, claimError, serverData, loading, isDirty, inputFields } = reducer(state, {
        type: 'componentDetailsClaim/claim/fulfilled',
        payload,
      });

      expect(loading).toBe(false);
      expect(claimMaskState).toBe(true);
      expect(claimError).toBeNull();
      expect(serverData).toEqual(payload);
      expect(isDirty).toEqual(false);

      expect(inputFields).toEqual({
        artifactId: initUserInput('artifactId2'),
        classifier: initUserInput('classifier2'),
        extension: initUserInput('extension2'),
        groupId: initUserInput('groupId2'),
        version: initUserInput('version3'),
        comment: initUserInput('text'),
        createTime: initUserInput(''),
      });
    });
  });

  describe('componentDetailsClaim/claim/rejected', () => {
    it('sets claimMaskState, loading and claimError', () => {
      const state = Object.freeze({
        claimMaskState: true,
        loading: true,
        claimError: null,
      });

      const { claimMaskState, loading, claimError } = reducer(state, {
        type: 'componentDetailsClaim/claim/rejected',
        payload: 'some error',
      });

      expect(loading).toBe(false);
      expect(claimError).toBe('some error');
      expect(claimMaskState).toBeNull();
    });
  });

  describe('componentDetailsClaim/resetForm', () => {
    it('sets inputFields, isDirty, claimError', () => {
      const state = Object.freeze({
        inputFields: {},
        isDirty: true,
        claimError: 'error',
        serverData: null,
      });

      const { inputFields, isDirty, claimError } = reducer(state, {
        type: 'componentDetailsClaim/resetForm',
      });

      expect(isDirty).toBe(false);
      expect(claimError).toBeNull();
      expect(inputFields).toEqual(initialState.inputFields);
    });

    it('sets inputFields to initial state inputFields, isDirty, claimError', () => {
      const state = Object.freeze({
        inputFields: {},
        isDirty: true,
        claimError: 'error',
        serverData: {
          componentIdentifier: {
            coordinates: {
              artifactId: 'artifactId2',
              classifier: 'classifier2',
              extension: 'extension2',
              groupId: 'groupId2',
              version: 'version3',
            },
            format: 'maven',
          },
          createTime: null,
          comment: 'text',
        },
      });

      const { inputFields, isDirty, claimError } = reducer(state, {
        type: 'componentDetailsClaim/resetForm',
      });

      expect(isDirty).toBe(false);
      expect(claimError).toBeNull();
      expect(inputFields).toEqual({
        artifactId: initUserInput('artifactId2'),
        classifier: initUserInput('classifier2'),
        extension: initUserInput('extension2'),
        groupId: initUserInput('groupId2'),
        version: initUserInput('version3'),
        comment: initUserInput('text'),
        createTime: initUserInput(''),
      });
    });
  });

  describe('input field actions', () => {
    let defaultState = {
      inputFields: {
        artifactId: initUserInput('artifactId'),
        classifier: initUserInput('classifier'),
        extension: initUserInput('extension'),
        groupId: initUserInput('groupId'),
        version: initUserInput('version'),
        comment: initUserInput('text'),
        createTime: initUserInput('2021-01-21'),
      },
      isDirty: false,
      serverData: {
        componentIdentifier: {
          coordinates: {
            artifactId: 'artifactId',
            classifier: 'classifier',
            extension: 'extension',
            groupId: 'groupId',
            version: 'version',
          },
          format: 'maven',
        },
        createTime: 1611180000000,
        id: '303',
        comment: 'text',
      },
    };

    describe('componentDetailsClaim/setGroupId', () => {
      it('updates groupId and isDirty values', () => {
        const state = Object.freeze({
          ...defaultState,
        });

        const { inputFields, isDirty } = reducer(state, {
          type: 'componentDetailsClaim/setGroupId',
          payload: 'id',
        });

        expect(isDirty).toBe(true);
        expect(inputFields.groupId.trimmedValue).toEqual('id');
      });

      it('updates groupId, isDirty and validationErrors', () => {
        const state = Object.freeze({
          ...defaultState,
        });

        const { inputFields, isDirty } = reducer(state, {
          type: 'componentDetailsClaim/setGroupId',
          payload: '',
        });

        expect(isDirty).toBe(true);
        expect(inputFields.groupId.trimmedValue).toEqual('');
        expect(inputFields.groupId.validationErrors).toBeTruthy();
      });
    });

    describe('componentDetailsClaim/setExtension', () => {
      it('updates extension and isDirty values', () => {
        const state = Object.freeze({
          ...defaultState,
        });

        const { inputFields, isDirty } = reducer(state, {
          type: 'componentDetailsClaim/setExtension',
          payload: 'ext',
        });

        expect(isDirty).toBe(true);
        expect(inputFields.extension.trimmedValue).toEqual('ext');
      });

      it('updates extension, isDirty and validationErrors', () => {
        const state = Object.freeze({
          ...defaultState,
        });

        const { inputFields, isDirty } = reducer(state, {
          type: 'componentDetailsClaim/setExtension',
          payload: '',
        });

        expect(isDirty).toBe(true);
        expect(inputFields.extension.trimmedValue).toEqual('');
        expect(inputFields.extension.validationErrors).toBeTruthy();
      });
    });

    describe('componentDetailsClaim/setArtifactId', () => {
      it('updates artifactId and isDirty values', () => {
        const state = Object.freeze({
          ...defaultState,
        });

        const { inputFields, isDirty } = reducer(state, {
          type: 'componentDetailsClaim/setArtifactId',
          payload: 'artifactId new',
        });

        expect(isDirty).toBe(true);
        expect(inputFields.artifactId.trimmedValue).toEqual('artifactId new');
      });

      it('updates artifactId, isDirty and validationErrors', () => {
        const state = Object.freeze({
          ...defaultState,
        });

        const { inputFields, isDirty } = reducer(state, {
          type: 'componentDetailsClaim/setArtifactId',
          payload: '',
        });

        expect(isDirty).toBe(true);
        expect(inputFields.artifactId.trimmedValue).toEqual('');
        expect(inputFields.artifactId.validationErrors).toBeTruthy();
      });
    });

    describe('componentDetailsClaim/setVersion', () => {
      it('updates version and isDirty values', () => {
        const state = Object.freeze({
          ...defaultState,
        });

        const { inputFields, isDirty } = reducer(state, {
          type: 'componentDetailsClaim/setVersion',
          payload: 'version new',
        });

        expect(isDirty).toBe(true);
        expect(inputFields.version.trimmedValue).toEqual('version new');
      });

      it('updates version, isDirty and validationErrors', () => {
        const state = Object.freeze({
          ...defaultState,
        });

        const { inputFields, isDirty } = reducer(state, {
          type: 'componentDetailsClaim/setVersion',
          payload: '',
        });

        expect(isDirty).toBe(true);
        expect(inputFields.version.trimmedValue).toEqual('');
        expect(inputFields.version.validationErrors).toBeTruthy();
      });
    });

    describe('componentDetailsClaim/setClassifier', () => {
      it('updates classifier and isDirty values', () => {
        const state = Object.freeze({
          ...defaultState,
        });

        const { inputFields, isDirty } = reducer(state, {
          type: 'componentDetailsClaim/setClassifier',
          payload: 'classifier new',
        });

        expect(isDirty).toBe(true);
        expect(inputFields.classifier.trimmedValue).toEqual('classifier new');
      });
    });

    describe('componentDetailsClaim/setComment', () => {
      it('updates comment and isDirty values', () => {
        const state = Object.freeze({
          ...defaultState,
        });

        const { inputFields, isDirty } = reducer(state, {
          type: 'componentDetailsClaim/setComment',
          payload: 'comment new',
        });

        expect(isDirty).toBe(true);
        expect(inputFields.comment.trimmedValue).toEqual('comment new');
      });
    });

    describe('componentDetailsClaim/setCreatedTime', () => {
      it('updates comment and isDirty values', () => {
        const state = Object.freeze({
          ...defaultState,
        });

        const { inputFields, isDirty } = reducer(state, {
          type: 'componentDetailsClaim/setCreatedTime',
          payload: '2021-10-21',
        });

        expect(isDirty).toBe(true);
        expect(inputFields.createTime.trimmedValue).toEqual('2021-10-21');
      });

      it('updates artifactId, isDirty and validationErrors', () => {
        const state = Object.freeze({
          ...defaultState,
        });

        const { inputFields, isDirty } = reducer(state, {
          type: 'componentDetailsClaim/setCreatedTime',
          payload: '1989-10-21',
        });

        expect(isDirty).toBe(true);
        expect(inputFields.createTime.trimmedValue).toEqual('1989-10-21');
        expect(inputFields.createTime.validationErrors).toBeTruthy();
      });
    });
  });

  describe('componentDetailsClaim/claimMaskTimerDone', () => {
    it('updates claimMaskState to null', () => {
      const state = Object.freeze({
        claimMaskState: true,
      });

      const { claimMaskState } = reducer(state, {
        type: 'componentDetailsClaim/claimMaskTimerDone',
      });

      expect(claimMaskState).toBeNull();
    });
  });

  describe('componentDetailsClaim/revokeMaskTimerDone', () => {
    it('updates revokeMaskState to null', () => {
      const state = Object.freeze({
        revokeMaskState: true,
      });

      const { revokeMaskState } = reducer(state, {
        type: 'componentDetailsClaim/revokeMaskTimerDone',
      });

      expect(revokeMaskState).toBeNull();
    });
  });

  describe('componentDetailsClaim/resetRevokeError', () => {
    it('updates revokeError to null', () => {
      const state = Object.freeze({
        revokeError: 'error',
      });

      const { revokeError } = reducer(state, {
        type: 'componentDetailsClaim/resetRevokeError',
      });

      expect(revokeError).toBeNull();
    });
  });

  describe('componentDetailsClaim/revoke/pending', () => {
    it('sets revokeMaskState property to false, revokeError to null', () => {
      const state = Object.freeze({
        revokeError: 'error',
        revokeMaskState: null,
      });

      const { revokeError, revokeMaskState } = reducer(state, {
        type: 'componentDetailsClaim/revoke/pending',
      });

      expect(revokeError).toBeNull();
      expect(revokeMaskState).toBe(false);
    });
  });

  describe('componentDetailsClaim/revoke/fulfilled', () => {
    it('sets revokeMaskState, revokeError, serverData, loading, isDirty, inputFields and loadError', () => {
      const state = Object.freeze({
        loading: true,
        loadError: 'error',
        serverData: {
          data: 42,
        },
        inputFields: {},
        revokeError: 'error',
        revokeMaskState: false,
      });

      const { revokeMaskState, revokeError, serverData, loading, isDirty, inputFields } = reducer(state, {
        type: 'componentDetailsClaim/revoke/fulfilled',
      });

      expect(loading).toBe(false);
      expect(revokeMaskState).toBe(true);
      expect(revokeError).toBeNull();
      expect(serverData).toBeNull();
      expect(isDirty).toEqual(false);

      expect(inputFields).toEqual(initialState.inputFields);
    });
  });

  describe('componentDetailsClaim/revoke/rejected', () => {
    it('sets revokeMaskState, loading and revokeError', () => {
      const state = Object.freeze({
        revokeMaskState: true,
        loading: true,
        revokeError: null,
      });

      const { revokeMaskState, loading, revokeError } = reducer(state, {
        type: 'componentDetailsClaim/revoke/rejected',
        payload: 'some error',
      });

      expect(loading).toBe(false);
      expect(revokeError).toBe('some error');
      expect(revokeMaskState).toBeNull();
    });
  });

  describe('componentDetailsClaim/toggleShowRevokeModal', () => {
    it('toggles showRevokeModal value', () => {
      const state = Object.freeze({
        showRevokeModal: false,
      });

      const { showRevokeModal } = reducer(state, {
        type: 'componentDetailsClaim/toggleShowRevokeModal',
      });

      expect(showRevokeModal).toBe(true);
    });
  });
});
