/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../main/frontend/legal/advancedLegalReducer.js';
import {
  ADVANCED_LEGAL_CANCEL_ATTRIBUTION_MODAL,
  ADVANCED_LEGAL_CANCEL_OBLIGATION_MODAL,
  ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_FAILED,
  ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_REQUESTED,
  ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUCCEEDED,
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
  ADVANCED_LEGAL_SET_SHOW_ALL_OBLIGATIONS_MODAL,
  ADVANCED_LEGAL_SET_SHOW_ATTRIBUTION_MODAL,
  ADVANCED_LEGAL_SET_SHOW_OBLIGATION_MODAL,
} from '../../../main/frontend/legal/obligation/advancedLegalObligationActions.js';

describe('advancedLegalObligationReducer', function () {
  describe('ADVANCED_LEGAL_SET_ATTRIBUTION_TEXT action', function () {
    it('sets the content of the first attribution of the matching obligation', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              obligations: [{ name: 'Must State Changes' }, { name: 'obligation2' }],
              attributions: [{ obligationName: 'Must State Changes' }],
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_ATTRIBUTION_TEXT,
        payload: { name: 'Must State Changes', value: 'content' },
      });
      const attribution1 = newState.component.component.licenseLegalData.attributions[0];
      expect(attribution1.content).toBe('content');
      expect(newState.component.component.licenseLegalData.obligations[1]).toEqual({ name: 'obligation2' });
    });
  });

  describe('ADVANCED_LEGAL_SET_ATTRIBUTION_SCOPE action', function () {
    it('sets the ownerId of the first attribution of the matching obligation', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              obligations: [{ name: 'Must State Changes' }, { name: 'obligation2' }],
              attributions: [{ obligationName: 'Must State Changes' }],
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_ATTRIBUTION_SCOPE,
        payload: { name: 'Must State Changes', value: 'ownerId' },
      });
      const attribution1 = newState.component.component.licenseLegalData.attributions[0];
      expect(attribution1.ownerId).toBe('ownerId');
      expect(newState.component.component.licenseLegalData.obligations[1]).toEqual({ name: 'obligation2' });
    });
  });

  describe('ADVANCED_LEGAL_SET_SHOW_ATTRIBUTION_MODAL action', function () {
    it('sets error to null and showAttributionModal to the payload of the first attribution of the matching obligation', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              obligations: [{ name: 'Must State Changes' }, { name: 'obligation2' }],
              attributions: [],
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_SHOW_ATTRIBUTION_MODAL,
        payload: { name: 'Must State Changes', value: true },
      });
      const attribution1 = newState.component.component.licenseLegalData.attributions[0];
      expect(attribution1.showAttributionModal).toBeTruthy();
      expect(attribution1.error).toBeNull();
      expect(newState.component.component.licenseLegalData.obligations[1]).toEqual({ name: 'obligation2' });
    });
  });

  describe('ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED action', function () {
    it('sets error and saveAttributionSubmitMask to null of the first attribution of the matching obligation', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              obligations: [{ name: 'Must State Changes' }, { name: 'obligation2' }],
              attributions: [],
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED,
        payload: { name: 'Must State Changes' },
      });
      const attribution1 = newState.component.component.licenseLegalData.attributions[0];
      expect(attribution1.error).toBeNull();
      expect(attribution1.saveAttributionSubmitMask).toBeFalsy();
      expect(newState.component.component.licenseLegalData.obligations[1]).toEqual({ name: 'obligation2' });
    });
  });

  describe('ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED action', function () {
    it('sets the matching obligation and its first attribution to the payload', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              obligations: [{ name: 'Must State Changes' }, { name: 'obligation2' }],
              attributions: [],
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED,
        payload: {
          name: 'Must State Changes',
          value: { id: 'id', content: 'content', ownerId: 'ownerId' },
        },
      });
      const attribution1 = newState.component.component.licenseLegalData.attributions[0];
      expect(attribution1.id).toBe('id');
      expect(attribution1.originalContent).toBe('content');
      expect(attribution1.content).toBe('content');
      expect(attribution1.originalOwnerId).toBe('ownerId');
      expect(attribution1.ownerId).toBe('ownerId');
      expect(attribution1.error).toBeNull();
      expect(attribution1.saveAttributionSubmitMask).toBeTruthy();
      expect(newState.component.component.licenseLegalData.obligations[1]).toEqual({ name: 'obligation2' });
    });
  });

  describe('ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED action', function () {
    it(
      'sets error to payload and saveAttributionSubmitMask to false of the first attribution of the matching' +
        'obligation',
      function () {
        const state = {
          component: {
            component: {
              licenseLegalData: {
                obligations: [{ name: 'Must State Changes' }, { name: 'obligation2' }],
                attributions: [],
              },
            },
          },
        };
        const newState = reduce(state, {
          type: ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED,
          payload: { name: 'Must State Changes', value: 'error' },
        });
        const attribution1 = newState.component.component.licenseLegalData.attributions[0];
        expect(attribution1.error).toBe('error');
        expect(attribution1.saveAttributionSubmitMask).toBeNull();
        expect(newState.component.component.licenseLegalData.obligations[1]).toEqual({ name: 'obligation2' });
      }
    );
  });

  describe('ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE action', function () {
    it(
      'sets saveAttributionSubmitMask to null and showAttributionModal to false of the first attribution of the' +
        'matching obligation',
      function () {
        const state = {
          component: {
            component: {
              licenseLegalData: {
                obligations: [{ name: 'Must State Changes' }, { name: 'obligation2' }],
                attributions: [],
              },
            },
          },
        };
        const newState = reduce(state, {
          type: ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE,
          payload: { name: 'Must State Changes' },
        });
        const attribution1 = newState.component.component.licenseLegalData.attributions[0];
        expect(attribution1.saveAttributionSubmitMask).toBeNull();
        expect(attribution1.showAttributionModal).toBeFalsy();
        expect(newState.component.component.licenseLegalData.obligations[1]).toEqual({ name: 'obligation2' });
      }
    );
  });

  describe('ADVANCED_LEGAL_CANCEL_ATTRIBUTION_MODAL action', function () {
    it(
      'sets content and ownerId to original values and sets showAttributionModal to false for the first attribution' +
        ' of the matching obligation',
      function () {
        const state = {
          component: {
            component: {
              licenseLegalData: {
                obligations: [{ name: 'Must State Changes' }, { name: 'obligation2' }],
                attributions: [
                  {
                    obligationName: 'Must State Changes',
                    originalContent: 'originalContent',
                    originalOwnerId: 'originalOwnerId',
                  },
                ],
              },
            },
          },
        };
        const newState = reduce(state, {
          type: ADVANCED_LEGAL_CANCEL_ATTRIBUTION_MODAL,
          payload: { name: 'Must State Changes' },
        });
        const attribution1 = newState.component.component.licenseLegalData.attributions[0];
        expect(attribution1.content).toBe('originalContent');
        expect(attribution1.ownerId).toBe('originalOwnerId');
        expect(attribution1.showAttributionModal).toBeFalsy();
        expect(newState.component.component.licenseLegalData.obligations[1]).toEqual({ name: 'obligation2' });
      }
    );
  });

  describe('ADVANCED_LEGAL_SET_OBLIGATION_STATUS action', function () {
    it('sets the status of the matching obligation', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              obligations: [{ name: 'obligation1' }, { name: 'obligation2' }],
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_OBLIGATION_STATUS,
        payload: { name: 'obligation1', value: 'status' },
      });
      const obligation1 = newState.component.component.licenseLegalData.obligations[0];
      expect(obligation1.status).toEqual('status');
      expect(newState.component.component.licenseLegalData.obligations[1].status).toBeUndefined();
    });
  });

  describe('ADVANCED_LEGAL_SET_OBLIGATION_COMMENT action', function () {
    it('sets the comment of the matching obligation', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              obligations: [{ name: 'obligation1' }, { name: 'obligation2' }],
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_OBLIGATION_COMMENT,
        payload: { name: 'obligation1', value: 'comment' },
      });
      const obligation1 = newState.component.component.licenseLegalData.obligations[0];
      expect(obligation1.comment).toEqual('comment');
      expect(newState.component.component.licenseLegalData.obligations[1].comment).toBeUndefined();
    });
  });

  describe('ADVANCED_LEGAL_SET_OBLIGATION_SCOPE action', function () {
    it('sets the ownerId of the matching obligation', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              obligations: [{ name: 'obligation1' }, { name: 'obligation2' }],
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_OBLIGATION_SCOPE,
        payload: { name: 'obligation1', value: 'ownerId' },
      });
      const obligation1 = newState.component.component.licenseLegalData.obligations[0];
      expect(obligation1.ownerId).toEqual('ownerId');
      expect(newState.component.component.licenseLegalData.obligations[1].ownerId).toBeUndefined();
    });
  });

  describe('ADVANCED_LEGAL_SET_SHOW_OBLIGATION_MODAL action', function () {
    it('sets error to null and showObligationModal to the payload of the matching obligation', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              obligations: [{ name: 'obligation1' }, { name: 'obligation2' }],
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_SHOW_OBLIGATION_MODAL,
        payload: { name: 'obligation1', value: true },
      });
      const obligation1 = newState.component.component.licenseLegalData.obligations[0];
      expect(obligation1.showObligationModal).toBeTruthy();
      expect(obligation1.error).toBeNull();
      const obligation2 = newState.component.component.licenseLegalData.obligations[1];
      expect(obligation2.showObligationModal).toBeUndefined();
      expect(obligation2.error).toBeUndefined();
    });
  });

  describe('ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED action', function () {
    it('sets error and saveObligationSubmitMask to null for the matching obligation', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              obligations: [{ name: 'obligation1' }, { name: 'obligation2' }],
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED,
        payload: { name: 'obligation1' },
      });
      const obligation1 = newState.component.component.licenseLegalData.obligations[0];
      expect(obligation1.error).toBeNull();
      expect(obligation1.saveObligationSubmitMask).toBeFalsy();
      const obligation2 = newState.component.component.licenseLegalData.obligations[1];
      expect(obligation2.error).toBeUndefined();
      expect(obligation2.saveObligationSubmitMask).toBeUndefined();
    });
  });

  describe('ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED action', function () {
    it('sets error to null, saveObligationSubmitMask to true, and saved data for the matching obligation', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              obligations: [{ name: 'obligation1' }, { name: 'obligation2' }],
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED,
        payload: {
          name: 'obligation1',
          value: {
            id: 'id',
            status: 'status',
            ownerId: 'ownerId',
            comment: 'comment',
          },
        },
      });
      const obligation1 = newState.component.component.licenseLegalData.obligations[0];
      expect(obligation1.id).toEqual('id');
      expect(obligation1.originalStatus).toEqual('status');
      expect(obligation1.status).toEqual('status');
      expect(obligation1.originalComment).toEqual('comment');
      expect(obligation1.comment).toEqual('comment');
      expect(obligation1.originalOwnerId).toEqual('ownerId');
      expect(obligation1.ownerId).toEqual('ownerId');
      expect(obligation1.error).toBeNull();
      expect(obligation1.saveObligationSubmitMask).toBeTruthy();
      const obligation2 = newState.component.component.licenseLegalData.obligations[1];
      expect(obligation2).toEqual({ name: 'obligation2' });
    });
  });

  describe('ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED action', function () {
    it('sets error to the payload and saveObligationSubmitMask to null for the matching obligation', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              obligations: [{ name: 'obligation1' }, { name: 'obligation2' }],
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED,
        payload: { name: 'obligation1', value: 'error' },
      });
      const obligation1 = newState.component.component.licenseLegalData.obligations[0];
      expect(obligation1.error).toEqual('error');
      expect(obligation1.saveObligationSubmitMask).toBeNull();
      const obligation2 = newState.component.component.licenseLegalData.obligations[1];
      expect(obligation2.error).toBeUndefined();
      expect(obligation2.saveObligationSubmitMask).toBeUndefined();
    });
  });

  describe('ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE action', function () {
    it('sets showObligationModal to false and saveObligationSubmitMask to null for the matching obligation', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              obligations: [{ name: 'obligation1' }, { name: 'obligation2' }],
              attributions: [],
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE,
        payload: { name: 'obligation1' },
      });
      const obligation1 = newState.component.component.licenseLegalData.obligations[0];
      expect(obligation1.showObligationModal).toBeFalsy();
      expect(obligation1.saveObligationSubmitMask).toBeNull();
      const obligation2 = newState.component.component.licenseLegalData.obligations[1];
      expect(obligation2.showObligationModal).toBeUndefined();
      expect(obligation2.saveObligationSubmitMask).toBeUndefined();
    });
  });

  describe('ADVANCED_LEGAL_CANCEL_OBLIGATION_MODAL action', function () {
    it(
      'sets status, comment, and ownerId to original values and showObligationModal to false for the matching' +
        ' obligation',
      function () {
        const state = {
          component: {
            component: {
              licenseLegalData: {
                obligations: [
                  {
                    name: 'obligation1',
                    originalStatus: 'originalStatus',
                    originalComment: 'originalComment',
                    originalOwnerId: 'originalOwnerId',
                  },
                  { name: 'obligation2' },
                ],
              },
            },
          },
        };
        const newState = reduce(state, {
          type: ADVANCED_LEGAL_CANCEL_OBLIGATION_MODAL,
          payload: { name: 'obligation1' },
        });
        const obligation1 = newState.component.component.licenseLegalData.obligations[0];
        expect(obligation1.status).toBe('originalStatus');
        expect(obligation1.comment).toBe('originalComment');
        expect(obligation1.ownerId).toBe('originalOwnerId');
        expect(obligation1.showObligationModal).toBeFalsy();
        const obligation2 = newState.component.component.licenseLegalData.obligations[1];
        expect(obligation2).toEqual({ name: 'obligation2' });
      }
    );
  });

  describe('ADVANCED_LEGAL_SET_SHOW_ALL_OBLIGATIONS_MODAL action', function () {
    it('sets error to null and showObligationModal to the payload of the matching obligation', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              showAllObligationsModal: false,
              saveAllObligationsSubmitMask: null,
              saveAllObligationsError: null,
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_SHOW_ALL_OBLIGATIONS_MODAL,
        payload: true,
      });
      const licenseLegalData = newState.component.component.licenseLegalData;
      expect(licenseLegalData.showAllObligationsModal).toBeTruthy();
      expect(licenseLegalData.saveAllObligationsSubmitMask).toBeNull();
      expect(licenseLegalData.saveAllObligationsError).toBeNull();
    });
  });

  describe('ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_REQUESTED action', function () {
    it('sets error and saveAllObligationsSubmitMask to false', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              saveAllObligationsSubmitMask: null,
              saveAllObligationsError: 'some error',
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_REQUESTED,
      });

      expect(newState.component.component.licenseLegalData.saveAllObligationsError).toBeNull();
      expect(newState.component.component.licenseLegalData.saveAllObligationsSubmitMask).toBeFalsy();
    });
  });

  describe('ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUCCEEDED action', function () {
    it('sets saveAllObligationsError to null, saveAllObligationsSubmitMask to true', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              saveAllObligationsSubmitMask: false,
              saveAllObligationsError: 'some error',
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUCCEEDED,
      });
      expect(newState.component.component.licenseLegalData.saveAllObligationsSubmitMask).toBeTruthy();
      expect(newState.component.component.licenseLegalData.saveAllObligationsError).toBeNull();
    });
  });

  describe('ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_FAILED action', function () {
    it('sets saveAllObligationsError to null, saveAllObligationsSubmitMask to true', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              saveAllObligationsError: null,
              saveAllObligationsSubmitMask: false,
            },
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_FAILED,
        payload: { value: 'my error' },
      });
      expect(newState.component.component.licenseLegalData.saveAllObligationsSubmitMask).toBeNull();
      expect(newState.component.component.licenseLegalData.saveAllObligationsError).toBe('my error');
    });
  });
});
