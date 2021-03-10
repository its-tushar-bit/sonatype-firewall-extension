/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reduce from '../../../../main/frontend/advancedLegal/advancedLegalReducer';
import {
  ADVANCED_LEGAL_ADD_NOTICE,
  ADVANCED_LEGAL_CANCEL_NOTICES_MODAL,
  ADVANCED_LEGAL_SAVE_NOTICES_FAILED,
  ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED,
  ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED,
  ADVANCED_LEGAL_SET_NOTICE_CONTENT,
  ADVANCED_LEGAL_SET_NOTICE_STATUS,
  ADVANCED_LEGAL_SET_NOTICES_SCOPE,
  ADVANCED_LEGAL_SET_SHOW_NOTICES_MODAL
} from '../../../../main/frontend/legal/files/advancedLegalFileActions';

describe('advancedLegalFileReducer', function() {
  describe('ADVANCED_LEGAL_SET_SHOW_NOTICES_MODAL action', function() {
    it('sets the modal visibility to the payload', function() {
      const state = {
        component: {
          component: {
            licenseLegalData: {}
          }
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_SHOW_NOTICES_MODAL,
        payload: true
      });
      expect(newState.component.component.licenseLegalData.showNoticesModal).toBeTruthy();
    });
  });

  describe('ADVANCED_LEGAL_CANCEL_NOTICES_MODAL action', function() {
    it('closes the modal and resets any changed values', function() {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              showNoticesModal: true,
              originalComponentLegalFileScopeOwnerId: 'orgId',
              componentLegalFileScopeOwnerId: 'appId',
              noticeFiles: [
                {
                  id: 'id1',
                  originalContentHash: 'originalContentHash1',
                  originalContent: 'originalContent1',
                  content: 'content1',
                  originalStatus: 'enabled',
                  status: 'disabled'
                },
                {
                  id: 'id2',
                  originalContentHash: null,
                  originalContent: 'originalContent2',
                  content: 'content2',
                  originalStatus: 'disabled',
                  status: 'enabled'
                },
                {
                  id: null,
                  originalContentHash: 'originalContentHash2',
                  originalContent: 'originalContent3',
                  content: 'content3',
                  originalStatus: 'enabled',
                  status: 'disabled'
                },
                {
                  id: null,
                  originalContentHash: null
                }
              ],
              noticesError: 'error'
            }
          }
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_CANCEL_NOTICES_MODAL
      });
      expect(newState.component.component.licenseLegalData).toEqual({
        showNoticesModal: false,
        originalComponentLegalFileScopeOwnerId: 'orgId',
        componentLegalFileScopeOwnerId: 'orgId',
        noticeFiles: [
          {
            id: 'id1',
            originalContentHash: 'originalContentHash1',
            originalContent: 'originalContent1',
            content: 'originalContent1',
            originalStatus: 'enabled',
            status: 'enabled',
            isPristine: true
          },
          {
            id: 'id2',
            originalContentHash: null,
            originalContent: 'originalContent2',
            content: 'originalContent2',
            originalStatus: 'disabled',
            status: 'disabled',
            isPristine: true
          },
          {
            id: null,
            originalContentHash: 'originalContentHash2',
            originalContent: 'originalContent3',
            content: 'originalContent3',
            originalStatus: 'enabled',
            status: 'enabled',
            isPristine: true
          }
        ],
        noticesError: null
      });
    });
  });

  describe('ADVANCED_LEGAL_SET_NOTICE_CONTENT action', function() {
    it('sets the content of the notice at the payload index to the payload value', function() {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              noticeFiles: [
                { content: 'content1' },
                { content: 'content2' },
                { content: 'content3' }
              ]
            }
          }
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_NOTICE_CONTENT,
        payload: { index: 1, value: 'updatedContent2' }
      });
      expect(newState.component.component.licenseLegalData.noticeFiles).toEqual([
        { content: 'content1' },
        { content: 'updatedContent2', isPristine: false },
        { content: 'content3' }
      ]);
    });
  });

  describe('ADVANCED_LEGAL_SET_NOTICE_STATUS action', function() {
    it('sets the status of the notice at the payload index to the payload value', function() {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              noticeFiles: [
                { status: 'enabled' },
                { status: 'enabled' },
                { status: 'enabled' }
              ]
            }
          }
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_NOTICE_STATUS,
        payload: { index: 1, value: 'disabled' }
      });
      expect(newState.component.component.licenseLegalData.noticeFiles).toEqual([
        { status: 'enabled' },
        { status: 'disabled' },
        { status: 'enabled' }
      ]);
    });
  });

  describe('ADVANCED_LEGAL_ADD_NOTICE action', function() {
    it('adds a new notice with default values', function() {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              noticeFiles: [
                {},
                {},
                {}
              ]
            }
          }
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_ADD_NOTICE
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
          isPristine: true
        }
      ]);
    });
  });

  describe('ADVANCED_LEGAL_SET_NOTICES_SCOPE action', function() {
    it('sets the scope of the notices to the payload', function() {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              componentLegalFileScopeOwnerId: 'orgId'
            }
          }
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_NOTICES_SCOPE,
        payload: 'appId'
      });
      expect(newState.component.component.licenseLegalData.componentLegalFileScopeOwnerId).toBe('appId');
    });
  });

  describe('ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED action', function() {
    it('clears the notices error and closes the notices submit mask', function() {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              noticesError: 'error',
              saveNoticesSubmitMask: null
            }
          }
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_NOTICES_REQUESTED
      });
      expect(newState.component.component.licenseLegalData.noticesError).toBeNull();
      expect(newState.component.component.licenseLegalData.saveNoticesSubmitMask).toBeFalsy();
    });
  });

  describe('ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED action', function() {
    it('updates the license legal data for notices using the payload', function() {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              noticeFiles: [
                { content: 'content1', originalContentHash: 'originalContentHash1' },
                { content: '', originalContentHash: 'originalContentHash2' },
                { content: 'content3', originalContentHash: null },
                { content: '', originalContentHash: null }
              ]
            }
          }
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_NOTICES_SUCCEEDED,
        payload: {
          id: 'id',
          ownerId: 'ownerId',
          legalFileOverrides: [
            { id: 'id1', content: 'c1', status: 'enabled' },
            { id: 'id2', content: '', status: 'disabled' },
            { id: 'id3', content: 'c3', status: 'enabled' }
          ]
        }
      });
      expect(newState.component.component.licenseLegalData).toEqual({
        componentLegalFileId: 'id',
        originalComponentLegalFileScopeOwnerId: 'ownerId',
        componentLegalFileScopeOwnerId: 'ownerId',
        noticeFiles: [
          {
            originalContentHash: 'originalContentHash1',
            id: 'id1',
            originalContent: 'c1',
            content: 'c1',
            originalStatus: 'enabled',
            status: 'enabled'
          },
          {
            originalContentHash: 'originalContentHash2',
            id: 'id2',
            originalContent: '',
            content: '',
            originalStatus: 'disabled',
            status: 'disabled'
          },
          {
            originalContentHash: null,
            id: 'id3',
            originalContent: 'c3',
            content: 'c3',
            originalStatus: 'enabled',
            status: 'enabled'
          }
        ],
        noticesError: null,
        saveNoticesSubmitMask: true
      });
    });
  });

  describe('ADVANCED_LEGAL_SAVE_NOTICES_FAILED action', function() {
    it('sets the notices error to the payload and closes the notices submit mask', function() {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              noticesError: null,
              saveNoticesSubmitMask: false
            }
          }
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_NOTICES_FAILED,
        payload: 'error'
      });
      expect(newState.component.component.licenseLegalData.noticesError).toBe('error');
      expect(newState.component.component.licenseLegalData.saveNoticesSubmitMask).toBeNull();
    });
  });

  describe('ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE action', function() {
    it('closes the notices submit mask and modal', function() {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              saveNoticesSubmitMask: true,
              showNoticesModal: true
            }
          }
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_NOTICES_SUBMIT_MASK_DONE,
        payload: 'error'
      });
      expect(newState.component.component.licenseLegalData.saveNoticesSubmitMask).toBeNull();
      expect(newState.component.component.licenseLegalData.showNoticesModal).toBeFalsy();
    });
  });
});
