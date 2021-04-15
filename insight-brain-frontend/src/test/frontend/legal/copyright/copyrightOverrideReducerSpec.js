/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../../main/frontend/legal/copyright/copyrightOverrideReducer';
import {
  COPYRIGHT_OVERRIDE_FAILED,
  COPYRIGHT_OVERRIDE_SAVE_REQUESTED,
  COPYRIGHT_OVERRIDE_SUBMIT_MASK_DONE,
  SET_DISPLAY_COPYRIGHT_OVERRIDE_MODAL,
} from '../../../../main/frontend/legal/copyright/copyrightOverrideFormActions';

describe('copyrightOverrideReducer', function () {
  describe('initial state', function () {
    it('is used if no state is provided', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reduce(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reduce(undefined, action);

      expect(newState.saveCopyrightError).toBeNull();
      expect(newState.submitMaskState).toBeNull();
      expect(newState.showEditCopyrightOverrideModal).toBeFalsy();
    });

    describe('unknown action', function () {
      it('returns original state', function () {
        const state = { foo: 'bar' };
        const action = {
          type: 'UNKNOWN',
        };
        const newState = reduce(state, action);
        expect(newState).toBe(state);
      });
    });

    describe('SET_DISPLAY_COPYRIGHT_OVERRIDE_MODAL action', function () {
      it('sets to true when false', function () {
        const newState = reduce(undefined, {
          type: SET_DISPLAY_COPYRIGHT_OVERRIDE_MODAL,
          payload: true,
        });
        expect(newState.showEditCopyrightOverrideModal).toBeTruthy();
        expect(newState.saveCopyrightError).toBeNull();
        expect(newState.submitMaskState).toBeNull();
      });

      it('sets to false when true', function () {
        const state = {
          showEditCopyrightOverrideModal: true,
          saveCopyrightError: 'some error',
          submitMaskState: true,
        };
        const newState = reduce(state, {
          type: SET_DISPLAY_COPYRIGHT_OVERRIDE_MODAL,
          payload: false,
        });
        expect(newState.showEditCopyrightOverrideModal).toBeFalsy();
        expect(newState.saveCopyrightError).toBeNull();
        expect(newState.submitMaskState).toBeNull();
      });
    });

    describe('COPYRIGHT_OVERRIDE_FAILED action', function () {
      it('sets error message', function () {
        const state = {
          showEditCopyrightOverrideModal: true,
        };
        const newState = reduce(state, {
          type: COPYRIGHT_OVERRIDE_FAILED,
          payload: 'some error message',
        });
        expect(newState.showEditCopyrightOverrideModal).toBeTruthy();
        expect(newState.saveCopyrightError).toBe('some error message');
        expect(newState.submitMaskState).toBeNull();
      });
    });

    describe('COPYRIGHT_OVERRIDE_SAVE_REQUESTED action', function () {
      it('sets submitMaskState to false', function () {
        const state = {
          showEditCopyrightOverrideModal: true,
        };
        const newState = reduce(state, {
          type: COPYRIGHT_OVERRIDE_SAVE_REQUESTED,
        });
        expect(newState.showEditCopyrightOverrideModal).toBeTruthy();
        expect(newState.submitMaskState).toBeFalsy();
      });
    });

    describe('COPYRIGHT_OVERRIDE_SAVE_FULFILLED action', function () {
      it('clears properties', function () {
        const state = {
          showEditCopyrightOverrideModal: true,
          submitMaskState: true,
        };
        const newState = reduce(state, {
          type: COPYRIGHT_OVERRIDE_SAVE_REQUESTED,
        });
        expect(newState.showEditCopyrightOverrideModal).toBeTruthy();
        expect(newState.submitMaskState).toBeFalsy();
      });

      describe('COPYRIGHT_OVERRIDE_SUBMIT_MASK_DONE action', function () {
        it('success displayed', function () {
          const state = {
            saveCopyrightError: null,
            submitMaskState: true,
            showEditCopyrightOverrideModal: false,
          };
          const newState = reduce(state, {
            type: COPYRIGHT_OVERRIDE_SUBMIT_MASK_DONE,
          });
          expect(newState.submitMaskState).toBeNull();
          expect(newState.showEditCopyrightOverrideModal).toBeFalsy();
        });
      });
    });
  });
});
