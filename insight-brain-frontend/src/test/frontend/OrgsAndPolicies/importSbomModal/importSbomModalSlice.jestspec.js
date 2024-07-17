/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';
import reducer, { IMPORT_STATE } from 'MainRoot/OrgsAndPolicies/importSbomModal/importSbomModalSlice';

describe('importSbomModalSlice', function () {
  describe('setIsModalOpen', () => {
    it('sets the correct setIsModalOpen value', () => {
      const state = {
        isModalOpen: false,
      };

      const newState = reducer(state, {
        type: `${OWNER_ACTIONS}/importSbomModal/setIsModalOpen`,
        payload: true,
      });

      expect(newState.isModalOpen).toBe(true);
    });
  });

  describe('setUploadProgress', () => {
    it('sets the correct uploadProgress value', () => {
      const state = {
        uploadProgress: 0,
      };

      const newState = reducer(state, {
        type: `${OWNER_ACTIONS}/importSbomModal/setUploadProgress`,
        payload: 50,
      });

      expect(newState.uploadProgress).toBe(50);
    });
  });

  describe('uploadFile', () => {
    describe('pending', () => {
      it('sets the correct value for importState', () => {
        const state = {
          importState: IMPORT_STATE.INITIAL,
        };

        const newState = reducer(state, {
          type: `${OWNER_ACTIONS}/importSbomModal/uploadFile/pending`,
        });

        expect(newState.importState).toBe(IMPORT_STATE.UPLOADING_COMMITTING);
      });
    });

    describe('fulfilled', () => {
      it('sets the correct importState and sbomSummary values when applicationVersion is null', () => {
        const responseWithNullCreationDetails = Object.freeze({
          requestId: 'REQUEST-ID',
          sbomSummary: {
            specification: 'CycloneDx',
            format: 'json',
            version: '1.4',
            componentCount: 1,
            vulnerabilityCount: 2,
            applicationName: null,
            applicationVersion: null,
            serialNumber: 'urn:uuid:123-123-123-123-12345',
            creationDetails: null,
          },
          errorMessage: null,
        });

        const state = {
          importState: IMPORT_STATE.UPLOADING_COMMITTING,
          sbomSummary: {
            versionId: null,
            totalComponents: null,
            totalVulnerabilities: null,
          },
        };

        const newState = reducer(state, {
          type: `${OWNER_ACTIONS}/importSbomModal/uploadFile/fulfilled`,
          payload: responseWithNullCreationDetails,
        });

        expect(newState.importState).toBe(IMPORT_STATE.UPLOADING_COMMITTING);
        expect(newState.sbomSummary).toEqual({
          versionId: null,
          totalComponents: 1,
          totalVulnerabilities: 2,
        });
      });

      it('sets the correct importState and sbomSummary values when applicationVersion is specified', () => {
        const responseWithCreationDetails = Object.freeze({
          requestId: 'REQUEST-ID',
          sbomSummary: {
            specification: 'CycloneDx',
            format: 'json',
            version: '1.4',
            componentCount: 2,
            vulnerabilityCount: 3,
            applicationName: null,
            applicationVersion: '1.2.3',
            serialNumber: 'urn:uuid:123-123-123-123-12345',
            creationDetails: null,
          },
          errorMessage: null,
        });

        const state = {
          importState: IMPORT_STATE.UPLOADING_COMMITTING,
          sbomSummary: {
            versionId: null,
            totalComponents: null,
            totalVulnerabilities: null,
          },
        };

        const newState = reducer(state, {
          type: `${OWNER_ACTIONS}/importSbomModal/uploadFile/fulfilled`,
          payload: responseWithCreationDetails,
        });

        expect(newState.importState).toBe(IMPORT_STATE.UPLOADING_COMMITTING);
        expect(newState.sbomSummary).toEqual({
          versionId: '1.2.3',
          totalComponents: 2,
          totalVulnerabilities: 3,
        });
      });
    });
  });

  describe('uploadOrCommitFileFailed', () => {
    describe('uploadFile', () => {
      it('sets the correct importState and errorMessage values', () => {
        const state = {
          importState: null,
          errorMessage: null,
        };

        const newState = reducer(state, {
          type: `${OWNER_ACTIONS}/importSbomModal/uploadFile/rejected`,
          payload: {
            message: 'Correct error message',
          },
        });

        expect(newState).toEqual({
          importState: IMPORT_STATE.ERROR,
          errorMessage: 'Correct error message',
        });
      });

      it('sets the correct importState and default errorMessage value when payload is empty', () => {
        const state = {
          importState: null,
          errorMessage: null,
        };

        const newState = reducer(state, {
          type: `${OWNER_ACTIONS}/importSbomModal/uploadFile/rejected`,
          payload: {
            message: '',
          },
        });

        expect(newState).toEqual({
          importState: IMPORT_STATE.ERROR,
          errorMessage: 'Encountered unexpected error while attempting to upload.',
        });
      });
    });

    describe('commitFile', () => {
      it('sets the correct importState and errorMessage values', () => {
        const state = {
          importState: null,
          errorMessage: null,
        };

        const newState = reducer(state, {
          type: `${OWNER_ACTIONS}/importSbomModal/commitFile/rejected`,
          payload: {
            message: 'Correct error message',
          },
        });

        expect(newState).toEqual({
          importState: IMPORT_STATE.ERROR,
          errorMessage: 'Correct error message',
        });
      });

      it('sets the correct importState and default errorMessage value when payload is empty', () => {
        const state = {
          importState: null,
          errorMessage: null,
        };

        const newState = reducer(state, {
          type: `${OWNER_ACTIONS}/importSbomModal/commitFile/rejected`,
          payload: {
            message: '',
          },
        });

        expect(newState).toEqual({
          importState: IMPORT_STATE.ERROR,
          errorMessage: 'Encountered unexpected error while attempting to upload.',
        });
      });
    });
  });

  describe('commitFile', () => {
    describe('pending', () => {
      it('sets the correct value for importState', () => {
        const state = {
          importState: IMPORT_STATE.INITIAL,
        };

        const newState = reducer(state, {
          type: `${OWNER_ACTIONS}/importSbomModal/commitFile/pending`,
        });

        expect(newState.importState).toBe(0);
      });
    });

    describe('fulfilled', () => {
      it('sets the correct value for importState and sbomSummary', () => {
        const state = {
          importState: IMPORT_STATE.INITIAL,
        };

        const newState = reducer(state, {
          type: `${OWNER_ACTIONS}/importSbomModal/commitFile/fulfilled`,
        });

        expect(newState.importState).toBe(IMPORT_STATE.SUMMARY);
      });
    });
  });
});
