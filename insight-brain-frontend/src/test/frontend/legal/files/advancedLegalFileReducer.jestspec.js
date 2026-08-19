/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reduce from '../../../../main/frontend/legal/advancedLegalReducer';
import {
  ADVANCED_LEGAL_ADD_LICENSE_FILE,
  ADVANCED_LEGAL_ADD_NOTICE,
  ADVANCED_LEGAL_CANCEL_LICENSE_FILES_MODAL,
  ADVANCED_LEGAL_CANCEL_NOTICES_MODAL,
  ADVANCED_LEGAL_LICENSE_MODAL_LOAD_FAILED,
  ADVANCED_LEGAL_LOAD_LICENSE_MODAL_ALL_LICENSES_FULFILLED,
  ADVANCED_LEGAL_LOAD_LICENSE_MODAL_HIERARCHY_FULFILLED,
  ADVANCED_LEGAL_SAVE_LICENSE_FILES_FAILED,
  ADVANCED_LEGAL_SAVE_LICENSE_FILES_REQUESTED,
  ADVANCED_LEGAL_SAVE_LICENSE_FILES_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_LICENSE_FILES_SUCCEEDED,
  ADVANCED_LEGAL_SAVE_LICENSES_FAILED,
  ADVANCED_LEGAL_SAVE_LICENSES_REQUESTED,
  ADVANCED_LEGAL_SAVE_LICENSES_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED,
  ADVANCED_LEGAL_SAVE_NOTICES_FAILED,
  ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED,
  ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED,
  ADVANCED_LEGAL_SET_LICENSE_FILE_CONTENT,
  ADVANCED_LEGAL_SET_LICENSE_FILE_STATUS,
  ADVANCED_LEGAL_SET_LICENSE_FILES_SCOPE,
  ADVANCED_LEGAL_SET_NOTICE_CONTENT,
  ADVANCED_LEGAL_SET_NOTICE_STATUS,
  ADVANCED_LEGAL_SET_NOTICES_SCOPE,
  ADVANCED_LEGAL_SET_SHOW_LICENSE_FILES_MODAL,
  ADVANCED_LEGAL_SET_SHOW_LICENSES_MODAL,
  ADVANCED_LEGAL_SET_SHOW_NOTICES_MODAL,
  ADVANCED_LEGAL_SET_SHOW_ORIGINAL_SOURCES_MODAL,
} from '../../../../main/frontend/legal/files/advancedLegalFileActions';

describe('advancedLegalFileReducer', function () {
  describe('ADVANCED_LEGAL_SET_SHOW_LICENSES_MODAL action', function () {
    it('sets the modal visibility to the payload', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {},
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_SHOW_LICENSES_MODAL,
        payload: true,
      });
      expect(newState.component.component.licenseLegalData.showLicensesModal).toBe(true);
    });
  });
  describe('ADVANCED_LEGAL_SET_SHOW_ORIGINAL_SOURCES_MODAL action', function () {
    it('sets the modal visibility to the payload', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {},
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_SHOW_ORIGINAL_SOURCES_MODAL,
        payload: true,
      });
      expect(newState.component.component.licenseLegalData.showOriginalSourcesModal).toBe(true);
    });
  });

  describe('ADVANCED_LEGAL_SET_SHOW_NOTICES_MODAL action', function () {
    it('sets the modal visibility to the payload', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {},
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_SHOW_NOTICES_MODAL,
        payload: true,
      });
      expect(newState.component.component.licenseLegalData.showNoticesModal).toBeTruthy();
    });
  });

  describe('ADVANCED_LEGAL_CANCEL_NOTICES_MODAL action', function () {
    it('closes the modal and resets any changed values', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              showNoticesModal: true,
              originalComponentNoticesScopeOwnerId: 'orgId',
              componentNoticesScopeOwnerId: 'appId',
              noticeFiles: [
                {
                  id: 'id1',
                  originalContentHash: 'originalContentHash1',
                  originalContent: 'originalContent1',
                  content: 'content1',
                  originalStatus: 'enabled',
                  status: 'disabled',
                },
                {
                  id: 'id2',
                  originalContentHash: null,
                  originalContent: 'originalContent2',
                  content: 'content2',
                  originalStatus: 'disabled',
                  status: 'enabled',
                },
                {
                  id: null,
                  originalContentHash: 'originalContentHash2',
                  originalContent: 'originalContent3',
                  content: 'content3',
                  originalStatus: 'enabled',
                  status: 'disabled',
                },
                {
                  id: null,
                  originalContentHash: null,
                },
              ],
              noticesError: 'error',
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_CANCEL_NOTICES_MODAL,
      });
      expect(newState.component.component.licenseLegalData).toEqual({
        showNoticesModal: false,
        originalComponentNoticesScopeOwnerId: 'orgId',
        componentNoticesScopeOwnerId: 'orgId',
        noticeFiles: [
          {
            id: 'id1',
            originalContentHash: 'originalContentHash1',
            originalContent: 'originalContent1',
            content: 'originalContent1',
            originalStatus: 'enabled',
            status: 'enabled',
            isPristine: true,
          },
          {
            id: 'id2',
            originalContentHash: null,
            originalContent: 'originalContent2',
            content: 'originalContent2',
            originalStatus: 'disabled',
            status: 'disabled',
            isPristine: true,
          },
          {
            id: null,
            originalContentHash: 'originalContentHash2',
            originalContent: 'originalContent3',
            content: 'originalContent3',
            originalStatus: 'enabled',
            status: 'enabled',
            isPristine: true,
          },
        ],
        noticesError: null,
      });
    });
  });

  describe('ADVANCED_LEGAL_SET_NOTICE_CONTENT action', function () {
    it('sets the content of the notice at the payload index to the payload value', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              noticeFiles: [{ content: 'content1' }, { content: 'content2' }, { content: 'content3' }],
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_NOTICE_CONTENT,
        payload: { index: 1, value: 'updatedContent2' },
      });
      expect(newState.component.component.licenseLegalData.noticeFiles).toEqual([
        { content: 'content1' },
        { content: 'updatedContent2', isPristine: false },
        { content: 'content3' },
      ]);
    });
  });

  describe('ADVANCED_LEGAL_SET_NOTICE_STATUS action', function () {
    it('sets the status of the notice at the payload index to the payload value', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              noticeFiles: [{ status: 'enabled' }, { status: 'enabled' }, { status: 'enabled' }],
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_NOTICE_STATUS,
        payload: { index: 1, value: 'disabled' },
      });
      expect(newState.component.component.licenseLegalData.noticeFiles).toEqual([
        { status: 'enabled' },
        { status: 'disabled' },
        { status: 'enabled' },
      ]);
    });
  });

  describe('ADVANCED_LEGAL_ADD_NOTICE action', function () {
    it('adds a new notice with default values', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              noticeFiles: [{}, {}, {}],
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_ADD_NOTICE,
      });
      expect(newState.component.component.licenseLegalData.noticeFiles).toEqual([
        {},
        {},
        {},
        {
          id: null,
          originalContentHash: null,
          relPath: null,
          content: '',
          status: 'enabled',
          isPristine: true,
        },
      ]);
    });
  });

  describe('ADVANCED_LEGAL_SET_NOTICES_SCOPE action', function () {
    it('sets the scope of the notices to the payload', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              componentNoticesScopeOwnerId: 'orgId',
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_NOTICES_SCOPE,
        payload: 'appId',
      });
      expect(newState.component.component.licenseLegalData.componentNoticesScopeOwnerId).toBe('appId');
    });
  });

  describe('ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED action', function () {
    it('clears the notices error and closes the notices submit mask', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              noticesError: 'error',
              saveNoticesSubmitMask: null,
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED,
      });
      expect(newState.component.component.licenseLegalData.noticesError).toBeNull();
      expect(newState.component.component.licenseLegalData.saveNoticesSubmitMask).toBeFalsy();
    });
  });

  describe('ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED action', function () {
    it('updates the license legal data for notices using the payload', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              noticeFiles: [
                {
                  content: 'content1',
                  originalContentHash: 'originalContentHash1',
                  relPath: 'path1',
                },
                {
                  content: '',
                  originalContentHash: 'originalContentHash2',
                  relPath: null,
                },
                { content: 'content3', originalContentHash: null },
                { content: '', originalContentHash: null },
              ],
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED,
        payload: {
          id: 'id',
          ownerId: 'ownerId',
          lastUpdatedByUsername: 'admin',
          lastUpdatedAt: 1618873200000,
          legalFileOverrides: [
            {
              id: 'id1',
              legalFileType: 'notice',
              originalContentHash: 'originalContentHash1',
              content: 'c1',
              status: 'enabled',
            },
            {
              id: 'id2',
              legalFileType: 'notice',
              originalContentHash: 'originalContentHash2',
              content: '',
              status: 'disabled',
            },
            {
              id: 'id3',
              legalFileType: 'notice',
              originalContentHash: null,
              content: 'c3',
              status: 'enabled',
            },
          ],
        },
      });
      expect(newState.component.component.licenseLegalData).toEqual({
        componentNoticesId: 'id',
        originalComponentNoticesScopeOwnerId: 'ownerId',
        componentNoticesScopeOwnerId: 'ownerId',
        componentNoticesLastUpdatedByUsername: 'admin',
        componentNoticesLastUpdatedAt: 1618873200000,
        noticeFiles: [
          {
            originalContentHash: 'originalContentHash1',
            relPath: 'path1',
            id: 'id1',
            originalContent: 'c1',
            content: 'c1',
            originalStatus: 'enabled',
            status: 'enabled',
            isPristine: true,
          },
          {
            originalContentHash: 'originalContentHash2',
            relPath: null,
            id: 'id2',
            originalContent: '',
            content: '',
            originalStatus: 'disabled',
            status: 'disabled',
            isPristine: true,
          },
          {
            originalContentHash: null,
            relPath: null,
            id: 'id3',
            originalContent: 'c3',
            content: 'c3',
            originalStatus: 'enabled',
            status: 'enabled',
            isPristine: true,
          },
        ],
        noticesError: null,
        saveNoticesSubmitMask: true,
      });
    });
  });

  describe('ADVANCED_LEGAL_SAVE_NOTICES_FAILED action', function () {
    it('sets the notices error to the payload and closes the notices submit mask', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              noticesError: null,
              saveNoticesSubmitMask: false,
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_NOTICES_FAILED,
        payload: 'error',
      });
      expect(newState.component.component.licenseLegalData.noticesError).toBe('error');
      expect(newState.component.component.licenseLegalData.saveNoticesSubmitMask).toBeNull();
    });
  });

  describe('ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE action', function () {
    it('closes the notices submit mask and modal', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              saveNoticesSubmitMask: true,
              showNoticesModal: true,
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE,
        payload: 'error',
      });
      expect(newState.component.component.licenseLegalData.saveNoticesSubmitMask).toBeNull();
      expect(newState.component.component.licenseLegalData.showNoticesModal).toBeFalsy();
    });
  });

  describe('ADVANCED_LEGAL_SAVE_LICENSES_REQUESTED action', function () {
    it('clears the licenses error and closes the licenses submit mask', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              licensesError: 'error',
              saveLicensesSubmitMask: null,
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_LICENSES_REQUESTED,
      });
      expect(newState.component.component.licenseLegalData.licensesError).toBeNull();
      expect(newState.component.component.licenseLegalData.saveLicensesSubmitMask).toBeFalsy();
    });
  });

  describe('ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED action', function () {
    it('sets the licenses submit mask to  true', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              saveLicensesSubmitMask: false,
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED,
      });
      expect(newState.component.component.licenseLegalData.saveLicensesSubmitMask).toEqual(true);
    });
  });

  describe('ADVANCED_LEGAL_SAVE_LICENSES_FAILED action', function () {
    it('sets the notices error to the payload and closes the notices submit mask', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              licensesError: null,
              saveLicensesSubmitMask: true,
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_LICENSES_FAILED,
        payload: 'error',
      });
      expect(newState.component.component.licenseLegalData.licensesError).toBe('error');
      expect(newState.component.component.licenseLegalData.saveLicensesSubmitMask).toBeNull();
    });
  });

  describe('ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE action', function () {
    it('closes the notices submit mask and modal', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              saveLicensesSubmitMask: true,
              showLicensesModal: true,
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_LICENSES_SUBMIT_MASK_DONE,
        payload: 'error',
      });
      expect(newState.component.component.licenseLegalData.saveLicensesSubmitMask).toBeNull();
      expect(newState.component.component.licenseLegalData.showLicensesModal).toBeFalsy();
    });
  });

  describe('ADVANCED_LEGAL_SET_SHOW_LICENSES_MODAL action', function () {
    it('sets the modal visibility to the payload', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {},
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_SHOW_LICENSE_FILES_MODAL,
        payload: true,
      });
      expect(newState.component.component.licenseLegalData.showLicenseFilesModal).toBeTruthy();
    });
  });

  describe('ADVANCED_LEGAL_LOAD_LICENSE_MODAL_HIERARCHY_FULFILLED action', function () {
    it('sets the hierarchy to the payload', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {},
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LOAD_LICENSE_MODAL_HIERARCHY_FULFILLED,
        payload: 'payload',
      });
      expect(newState.component.component.licenseLegalData.hierarchy).toEqual('payload');
    });
  });

  describe('ADVANCED_LEGAL_LOAD_LICENSE_MODAL_ALL_LICENSES_FULFILLED action', function () {
    it('sets the allLicenses to the payload', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {},
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LOAD_LICENSE_MODAL_ALL_LICENSES_FULFILLED,
        payload: 'payload',
      });
      expect(newState.component.component.licenseLegalData.allLicenses).toEqual('payload');
    });
  });

  describe('ADVANCED_LEGAL_LICENSE_MODAL_LOAD_FAILED action', function () {
    it('sets the error to the payload', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {},
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LICENSE_MODAL_LOAD_FAILED,
        payload: 'error',
      });
      expect(newState.component.component.licenseLegalData.licensesError).toEqual('error');
    });
  });

  describe('ADVANCED_LEGAL_CANCEL_LICENSES_MODAL action', function () {
    it('closes the modal and resets any changed values', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              showLicenseFilesModal: true,
              originalComponentLicensesScopeOwnerId: 'orgId',
              componentLicensesScopeOwnerId: 'appId',
              licenseFiles: [
                {
                  id: 'id1',
                  originalContentHash: 'originalContentHash1',
                  originalContent: 'originalContent1',
                  content: 'content1',
                  originalStatus: 'enabled',
                  status: 'disabled',
                },
                {
                  id: 'id2',
                  originalContentHash: null,
                  originalContent: 'originalContent2',
                  content: 'content2',
                  originalStatus: 'disabled',
                  status: 'enabled',
                },
                {
                  id: null,
                  originalContentHash: 'originalContentHash2',
                  originalContent: 'originalContent3',
                  content: 'content3',
                  originalStatus: 'enabled',
                  status: 'disabled',
                },
                {
                  id: null,
                  originalContentHash: null,
                },
              ],
              licensesError: 'error',
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_CANCEL_LICENSE_FILES_MODAL,
      });
      expect(newState.component.component.licenseLegalData).toEqual({
        showLicenseFilesModal: false,
        originalComponentLicensesScopeOwnerId: 'orgId',
        componentLicensesScopeOwnerId: 'orgId',
        licenseFiles: [
          {
            id: 'id1',
            originalContentHash: 'originalContentHash1',
            originalContent: 'originalContent1',
            content: 'originalContent1',
            originalStatus: 'enabled',
            status: 'enabled',
            isPristine: true,
          },
          {
            id: 'id2',
            originalContentHash: null,
            originalContent: 'originalContent2',
            content: 'originalContent2',
            originalStatus: 'disabled',
            status: 'disabled',
            isPristine: true,
          },
          {
            id: null,
            originalContentHash: 'originalContentHash2',
            originalContent: 'originalContent3',
            content: 'originalContent3',
            originalStatus: 'enabled',
            status: 'enabled',
            isPristine: true,
          },
        ],
        licensesError: null,
      });
    });
  });

  describe('ADVANCED_LEGAL_SET_LICENSE_CONTENT action', function () {
    it('sets the content of the license at the payload index to the payload value', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              licenseFiles: [{ content: 'content1' }, { content: 'content2' }, { content: 'content3' }],
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_LICENSE_FILE_CONTENT,
        payload: { index: 1, value: 'updatedContent2' },
      });
      expect(newState.component.component.licenseLegalData.licenseFiles).toEqual([
        { content: 'content1' },
        { content: 'updatedContent2', isPristine: false },
        { content: 'content3' },
      ]);
    });
  });

  describe('ADVANCED_LEGAL_SET_LICENSE_STATUS action', function () {
    it('sets the status of the license at the payload index to the payload value', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              licenseFiles: [{ status: 'enabled' }, { status: 'enabled' }, { status: 'enabled' }],
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_LICENSE_FILE_STATUS,
        payload: { index: 1, value: 'disabled' },
      });
      expect(newState.component.component.licenseLegalData.licenseFiles).toEqual([
        { status: 'enabled' },
        { status: 'disabled' },
        { status: 'enabled' },
      ]);
    });
  });

  describe('ADVANCED_LEGAL_ADD_LICENSE action', function () {
    it('adds a new license with default values', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              licenseFiles: [{}, {}, {}],
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_ADD_LICENSE_FILE,
      });
      expect(newState.component.component.licenseLegalData.licenseFiles).toEqual([
        {},
        {},
        {},
        {
          id: null,
          originalContentHash: null,
          relPath: null,
          content: '',
          status: 'enabled',
          isPristine: true,
        },
      ]);
    });
  });

  describe('ADVANCED_LEGAL_SET_LICENSES_SCOPE action', function () {
    it('sets the scope of the licenses to the payload', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              componentLicensesScopeOwnerId: 'orgId',
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_LICENSE_FILES_SCOPE,
        payload: 'appId',
      });
      expect(newState.component.component.licenseLegalData.componentLicensesScopeOwnerId).toBe('appId');
    });
  });

  describe('ADVANCED_LEGAL_SAVE_LICENSES_REQUESTED action', function () {
    it('clears the licenses error and closes the licenses submit mask', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              licensesError: 'error',
              saveLicenseFilesSubmitMask: null,
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_LICENSE_FILES_REQUESTED,
      });
      expect(newState.component.component.licenseLegalData.licensesError).toBeNull();
      expect(newState.component.component.licenseLegalData.saveLicenseFilesSubmitMask).toBeFalsy();
    });
  });

  describe('ADVANCED_LEGAL_SAVE_LICENSES_SUCCEEDED action', function () {
    it('updates the license legal data for licenses using the payload', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              licenseFiles: [
                {
                  content: 'content1',
                  originalContentHash: 'originalContentHash1',
                  relPath: 'path1',
                },
                {
                  content: '',
                  originalContentHash: 'originalContentHash2',
                  relPath: null,
                },
                { content: 'content3', originalContentHash: null },
                { content: '', originalContentHash: null },
              ],
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_LICENSE_FILES_SUCCEEDED,
        payload: {
          id: 'id',
          ownerId: 'ownerId',
          lastUpdatedByUsername: 'admin',
          lastUpdatedAt: 1618873200000,
          legalFileOverrides: [
            {
              id: 'id1',
              legalFileType: 'license',
              originalContentHash: 'originalContentHash1',
              content: 'c1',
              status: 'enabled',
            },
            {
              id: 'id2',
              legalFileType: 'license',
              originalContentHash: 'originalContentHash2',
              content: '',
              status: 'disabled',
            },
            {
              id: 'id3',
              legalFileType: 'license',
              originalContentHash: null,
              content: 'c3',
              status: 'enabled',
            },
          ],
        },
      });
      expect(newState.component.component.licenseLegalData).toEqual({
        componentLicensesId: 'id',
        originalComponentLicensesScopeOwnerId: 'ownerId',
        componentLicensesScopeOwnerId: 'ownerId',
        componentLicensesLastUpdatedByUsername: 'admin',
        componentLicensesLastUpdatedAt: 1618873200000,
        licenseFiles: [
          {
            originalContentHash: 'originalContentHash1',
            relPath: 'path1',
            id: 'id1',
            originalContent: 'c1',
            content: 'c1',
            originalStatus: 'enabled',
            status: 'enabled',
            isPristine: true,
          },
          {
            originalContentHash: 'originalContentHash2',
            relPath: null,
            id: 'id2',
            originalContent: '',
            content: '',
            originalStatus: 'disabled',
            status: 'disabled',
            isPristine: true,
          },
          {
            originalContentHash: null,
            relPath: null,
            id: 'id3',
            originalContent: 'c3',
            content: 'c3',
            originalStatus: 'enabled',
            status: 'enabled',
            isPristine: true,
          },
        ],
        licensesError: null,
        saveLicenseFilesSubmitMask: true,
      });
    });
  });

  describe('ADVANCED_LEGAL_SAVE_LICENSES_FAILED action', function () {
    it('sets the licenses error to the payload and closes the licenses submit mask', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              licensesError: null,
              saveLicenseFilesSubmitMask: false,
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_LICENSE_FILES_FAILED,
        payload: 'error',
      });
      expect(newState.component.component.licenseLegalData.licensesError).toBe('error');
      expect(newState.component.component.licenseLegalData.saveLicenseFilesSubmitMask).toBeNull();
    });
  });

  describe('ADVANCED_LEGAL_SAVE_LICENSES_SUBMIT_MASK_DONE action', function () {
    it('closes the licenses submit mask and modal', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              saveLicenseFilesSubmitMask: true,
              showLicenseFilesModal: true,
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_LICENSE_FILES_SUBMIT_MASK_DONE,
        payload: 'error',
      });
      expect(newState.component.component.licenseLegalData.saveLicenseFilesSubmitMask).toBeNull();
      expect(newState.component.component.licenseLegalData.showLicenseFilesModal).toBeFalsy();
    });
  });
});
