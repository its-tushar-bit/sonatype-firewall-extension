/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../main/frontend/advancedLegal/advancedLegalReducer.js';
import {
  ADVANCED_LEGAL_CANCEL_ATTRIBUTION_MODAL,
  ADVANCED_LEGAL_CANCEL_OBLIGATION_MODAL,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED,
  ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED,
  ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED,
  ADVANCED_LEGAL_SET_ATTRIBUTION_SCOPE,
  ADVANCED_LEGAL_SET_ATTRIBUTION_TEXT,
  ADVANCED_LEGAL_SET_OBLIGATION_COMMENT,
  ADVANCED_LEGAL_SET_OBLIGATION_SCOPE,
  ADVANCED_LEGAL_SET_OBLIGATION_STATUS,
  ADVANCED_LEGAL_SET_SHOW_ATTRIBUTION_MODAL,
  ADVANCED_LEGAL_SET_SHOW_OBLIGATION_MODAL
} from '../../../main/frontend/legal/advancedLegalObligationActions.js';

describe('advancedLegalObligationReducer', function () {
  describe('ADVANCED_LEGAL_SET_ATTRIBUTION_TEXT action', function () {
    it('sets the content of the first attribution of the matching obligation', function () {
      const state = {
        component: {
          obligations: [{ name: 'obligation1', attributions: [{}] }, { name: 'obligation2' }]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_ATTRIBUTION_TEXT,
        payload: { name: 'obligation1', value: 'content' }
      });
      const obligation1Attribution = newState.component.obligations[0].attributions[0];
      expect(obligation1Attribution.content).toBe('content');
      expect(newState.component.obligations[1]).toEqual({ name: 'obligation2' });
    });
  });

  describe('ADVANCED_LEGAL_SET_ATTRIBUTION_SCOPE action', function () {
    it('sets the ownerId of the first attribution of the matching obligation', function () {
      const state = {
        component: {
          obligations: [{ name: 'obligation1', attributions: [{}] }, { name: 'obligation2' }]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_ATTRIBUTION_SCOPE,
        payload: { name: 'obligation1', value: 'ownerId' }
      });
      const obligation1Attribution = newState.component.obligations[0].attributions[0];
      expect(obligation1Attribution.ownerId).toBe('ownerId');
      expect(newState.component.obligations[1]).toEqual({ name: 'obligation2' });
    });
  });

  describe('ADVANCED_LEGAL_SET_SHOW_ATTRIBUTION_MODAL action', function () {
    it('sets error to null and showAttributionModal to the payload of the first attribution of the matching obligation',
        function() {
          const state = {
            component: {
              obligations: [{ name: 'obligation1', attributions: [{}] }, { name: 'obligation2' }]
            }
          };
          const newState = reduce(state, {
            type: ADVANCED_LEGAL_SET_SHOW_ATTRIBUTION_MODAL,
            payload: { name: 'obligation1', value: true }
          });
          const obligation1Attribution = newState.component.obligations[0].attributions[0];
          expect(obligation1Attribution.showAttributionModal).toBeTruthy();
          expect(obligation1Attribution.error).toBeNull();
          expect(newState.component.obligations[1]).toEqual({ name: 'obligation2' });
        });
  });

  describe('ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED action', function() {
    it('sets error and saveAttributionSubmitMask to null of the first attribution of the matching obligation',
        function() {
          const state = {
            component: {
              obligations: [{ name: 'obligation1', attributions: [{}] }, { name: 'obligation2' }]
            }
          };
          const newState = reduce(state, {
            type: ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED,
            payload: { name: 'obligation1' }
          });
          const obligation1Attribution = newState.component.obligations[0].attributions[0];
          expect(obligation1Attribution.error).toBeNull();
          expect(obligation1Attribution.saveAttributionSubmitMask).toBeFalsy();
          expect(newState.component.obligations[1]).toEqual({ name: 'obligation2' });
        });
  });

  describe('ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED action', function() {
    it('sets the matching obligation and its first attribution to the payload', function() {
      const state = {
        component: {
          obligations: [
            { name: 'obligation1', attributions: [{}] }, { name: 'obligation2' }
          ]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED,
        payload: { name: 'obligation1', value: { id: 'id', content: 'content', ownerId: 'ownerId' } }
      });
      const obligation1 = newState.component.obligations[0];
      const obligation1Attribution = obligation1.attributions[0];
      expect(obligation1Attribution.id).toBe('id');
      expect(obligation1Attribution.originalContent).toBe('content');
      expect(obligation1Attribution.content).toBe('content');
      expect(obligation1Attribution.originalOwnerId).toBe('ownerId');
      expect(obligation1Attribution.ownerId).toBe('ownerId');
      expect(obligation1Attribution.error).toBeNull();
      expect(obligation1Attribution.saveAttributionSubmitMask).toBeTruthy();
      expect(newState.component.obligations[1]).toEqual({ name: 'obligation2' });
    });
  });

  describe('ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED action', function() {
    it('sets error to payload and saveAttributionSubmitMask to false of the first attribution of the matching' +
        'obligation',
    function() {
      const state = {
        component: {
          obligations: [
            { name: 'obligation1', attributions: [{}] }, { name: 'obligation2' }
          ]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED,
        payload: { name: 'obligation1', value: 'error' }
      });
      const obligation1Attribution = newState.component.obligations[0].attributions[0];
      expect(obligation1Attribution.error).toBe('error');
      expect(obligation1Attribution.saveAttributionSubmitMask).toBeNull();
      expect(newState.component.obligations[1]).toEqual({ name: 'obligation2' });
    });
  });

  describe('ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE action', function() {
    it('sets saveAttributionSubmitMask to null and showAttributionModal to false of the first attribution of the' +
        'matching obligation',
    function() {
      const state = {
        component: {
          obligations: [
            { name: 'obligation1', attributions: [{}] }, { name: 'obligation2' }
          ]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE,
        payload: { name: 'obligation1' }
      });
      const obligation1Attribution = newState.component.obligations[0].attributions[0];
      expect(obligation1Attribution.saveAttributionSubmitMask).toBeNull();
      expect(obligation1Attribution.showAttributionModal).toBeFalsy();
      expect(newState.component.obligations[1]).toEqual({ name: 'obligation2' });
    });
  });

  describe('ADVANCED_LEGAL_CANCEL_ATTRIBUTION_MODAL action', function () {
    it('sets content and ownerId to original values and sets showAttributionModal to false for the first attribution' +
        ' of the matching obligation',
    function() {
      const state = {
        component: {
          obligations: [
            {
              name: 'obligation1',
              attributions: [{ originalContent: 'originalContent', originalOwnerId: 'originalOwnerId' }]
            }, { name: 'obligation2' }
          ]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_CANCEL_ATTRIBUTION_MODAL,
        payload: { name: 'obligation1' }
      });
      const obligation1Attribution = newState.component.obligations[0].attributions[0];
      expect(obligation1Attribution.content).toBe('originalContent');
      expect(obligation1Attribution.ownerId).toBe('originalOwnerId');
      expect(obligation1Attribution.showAttributionModal).toBeFalsy();
      expect(newState.component.obligations[1]).toEqual({ name: 'obligation2' });
    });
  });

  describe('ADVANCED_LEGAL_SET_OBLIGATION_STATUS action', function() {
    it('sets the status of the matching obligation', function() {
      const state = {
        component: {
          obligations: [
            { name: 'obligation1' }, { name: 'obligation2' }
          ]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_OBLIGATION_STATUS,
        payload: { name: 'obligation1', value: 'status' }
      });
      const obligation1 = newState.component.obligations[0];
      expect(obligation1.status).toEqual('status');
      expect(newState.component.obligations[1].status).toBeUndefined();
    });
  });

  describe('ADVANCED_LEGAL_SET_OBLIGATION_COMMENT action', function() {
    it('sets the comment of the matching obligation', function() {
      const state = {
        component: {
          obligations: [
            { name: 'obligation1' }, { name: 'obligation2' }
          ]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_OBLIGATION_COMMENT,
        payload: { name: 'obligation1', value: 'comment' }
      });
      const obligation1 = newState.component.obligations[0];
      expect(obligation1.comment).toEqual('comment');
      expect(newState.component.obligations[1].comment).toBeUndefined();
    });
  });

  describe('ADVANCED_LEGAL_SET_OBLIGATION_SCOPE action', function() {
    it('sets the ownerId of the matching obligation', function() {
      const state = {
        component: {
          obligations: [
            { name: 'obligation1' }, { name: 'obligation2' }
          ]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_OBLIGATION_SCOPE,
        payload: { name: 'obligation1', value: 'ownerId' }
      });
      const obligation1 = newState.component.obligations[0];
      expect(obligation1.ownerId).toEqual('ownerId');
      expect(newState.component.obligations[1].ownerId).toBeUndefined();
    });
  });

  describe('ADVANCED_LEGAL_SET_SHOW_OBLIGATION_MODAL action', function() {
    it('sets error to null and showObligationModal to the payload of the matching obligation', function() {
      const state = {
        component: {
          obligations: [
            { name: 'obligation1' }, { name: 'obligation2' }
          ]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_SHOW_OBLIGATION_MODAL,
        payload: { name: 'obligation1', value: true }
      });
      const obligation1 = newState.component.obligations[0];
      expect(obligation1.showObligationModal).toBeTruthy();
      expect(obligation1.error).toBeNull();
      const obligation2 = newState.component.obligations[1];
      expect(obligation2.showObligationModal).toBeUndefined();
      expect(obligation2.error).toBeUndefined();
    });
  });

  describe('ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED action', function() {
    it('sets error and saveObligationSubmitMask to null for the matching obligation', function() {
      const state = {
        component: {
          obligations: [
            { name: 'obligation1' }, { name: 'obligation2' }
          ]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED,
        payload: { name: 'obligation1' }
      });
      const obligation1 = newState.component.obligations[0];
      expect(obligation1.error).toBeNull();
      expect(obligation1.saveObligationSubmitMask).toBeFalsy();
      const obligation2 = newState.component.obligations[1];
      expect(obligation2.error).toBeUndefined();
      expect(obligation2.saveObligationSubmitMask).toBeUndefined();
    });
  });

  describe('ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED action', function() {
    it('sets error to null, saveObligationSubmitMask to true, and saved data for the matching obligation', function() {
      const state = {
        component: {
          obligations: [
            { name: 'obligation1' }, { name: 'obligation2' }
          ]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED,
        payload: { name: 'obligation1', value: { id: 'id', status: 'status', ownerId: 'ownerId', comment: 'comment' } }
      });
      const obligation1 = newState.component.obligations[0];
      expect(obligation1.id).toEqual('id');
      expect(obligation1.originalStatus).toEqual('status');
      expect(obligation1.status).toEqual('status');
      expect(obligation1.originalComment).toEqual('comment');
      expect(obligation1.comment).toEqual('comment');
      expect(obligation1.originalOwnerId).toEqual('ownerId');
      expect(obligation1.ownerId).toEqual('ownerId');
      expect(obligation1.error).toBeNull();
      expect(obligation1.saveObligationSubmitMask).toBeTruthy();
      const obligation2 = newState.component.obligations[1];
      expect(obligation2).toEqual({ name: 'obligation2' });
    });
  });

  describe('ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED action', function() {
    it('sets error to the payload and saveObligationSubmitMask to null for the matching obligation', function() {
      const state = {
        component: {
          obligations: [
            { name: 'obligation1' }, { name: 'obligation2' }
          ]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED,
        payload: { name: 'obligation1', value: 'error' }
      });
      const obligation1 = newState.component.obligations[0];
      expect(obligation1.error).toEqual('error');
      expect(obligation1.saveObligationSubmitMask).toBeNull();
      const obligation2 = newState.component.obligations[1];
      expect(obligation2.error).toBeUndefined();
      expect(obligation2.saveObligationSubmitMask).toBeUndefined();
    });
  });

  describe('ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE action', function() {
    it('sets showObligationModal to false and saveObligationSubmitMask to null for the matching obligation',
        function() {
          const state = {
            component: {
              obligations: [
                { name: 'obligation1' }, { name: 'obligation2' }
              ]
            }
          };
          const newState = reduce(state, {
            type: ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE,
            payload: { name: 'obligation1' }
          });
          const obligation1 = newState.component.obligations[0];
          expect(obligation1.showObligationModal).toBeFalsy();
          expect(obligation1.saveObligationSubmitMask).toBeNull();
          const obligation2 = newState.component.obligations[1];
          expect(obligation2.showObligationModal).toBeUndefined();
          expect(obligation2.saveObligationSubmitMask).toBeUndefined();
        });
  });

  describe('ADVANCED_LEGAL_CANCEL_OBLIGATION_MODAL action', function() {
    it('sets status, comment, and ownerId to original values and showObligationModal to false for the matching' +
        ' obligation', function() {
      const state = {
        component: {
          obligations: [
            {
              name: 'obligation1',
              originalStatus: 'originalStatus',
              originalComment: 'originalComment',
              originalOwnerId: 'originalOwnerId'
            }, { name: 'obligation2' }
          ]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_CANCEL_OBLIGATION_MODAL,
        payload: { name: 'obligation1' }
      });
      const obligation1 = newState.component.obligations[0];
      expect(obligation1.status).toBe('originalStatus');
      expect(obligation1.comment).toBe('originalComment');
      expect(obligation1.ownerId).toBe('originalOwnerId');
      expect(obligation1.showObligationModal).toBeFalsy();
      const obligation2 = newState.component.obligations[1];
      expect(obligation2).toEqual({ name: 'obligation2' });
    });
  });
});
