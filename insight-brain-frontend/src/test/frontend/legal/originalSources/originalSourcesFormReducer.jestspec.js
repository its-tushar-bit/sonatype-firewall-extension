/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../../main/frontend/legal/originalSources/originalSourcesFormReducer';
import {
  ORIGINAL_SOURCES_OVERRIDE_FAILED,
  ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED,
  ORIGINAL_SOURCES_OVERRIDE_SUBMIT_MASK_DONE,
  SET_DISPLAY_ORIGINAL_SOURCES_OVERRIDE_MODAL,
} from '../../../../main/frontend/legal/originalSources/originalSourcesFormActions';

describe('originalSourcesFormReducer', function () {
  describe('initial state', function () {
    it('is used if no state is provided', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reduce(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reduce(undefined, action);

      expect(newState.saveOriginalSourceError).toBeNull();
      expect(newState.submitMaskState).toBeNull();
      expect(newState.showOriginalSourcesModal).toBeFalsy();
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

    describe('SET_DISPLAY_ORIGINAL_SOURCES_OVERRIDE_MODAL action', function () {
      it('sets to true when false', function () {
        const newState = reduce(undefined, {
          type: SET_DISPLAY_ORIGINAL_SOURCES_OVERRIDE_MODAL,
          payload: true,
        });
        expect(newState.showOriginalSourcesModal).toBeTruthy();
        expect(newState.saveOriginalSourceError).toBeNull();
        expect(newState.submitMaskState).toBeNull();
      });

      it('sets to false when true', function () {
        const state = {
          showOriginalSourcesModal: true,
          saveOriginalSourceError: 'some error',
          submitMaskState: true,
        };
        const newState = reduce(state, {
          type: SET_DISPLAY_ORIGINAL_SOURCES_OVERRIDE_MODAL,
          payload: false,
        });
        expect(newState.showOriginalSourcesModal).toBeFalsy();
        expect(newState.saveOriginalSourceError).toBeNull();
        expect(newState.submitMaskState).toBeNull();
      });
    });

    describe('ORIGINAL_SOURCES_OVERRIDE_FAILED action', function () {
      it('sets error message', function () {
        const state = {
          showOriginalSourcesModal: true,
        };
        const newState = reduce(state, {
          type: ORIGINAL_SOURCES_OVERRIDE_FAILED,
          payload: 'some error message',
        });
        expect(newState.showOriginalSourcesModal).toBeTruthy();
        expect(newState.saveOriginalSourceError).toBe('some error message');
        expect(newState.submitMaskState).toBeNull();
      });
    });

    describe('ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED action', function () {
      it('sets submitMaskState to false', function () {
        const state = {
          showOriginalSourcesModal: true,
        };
        const newState = reduce(state, {
          type: ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED,
        });
        expect(newState.showOriginalSourcesModal).toBeTruthy();
        expect(newState.submitMaskState).toBeFalsy();
      });
    });

    describe('ORIGINAL_SOURCES_OVERRIDE_SAVE_FULFILLED action', function () {
      it('clears properties', function () {
        const state = {
          showOriginalSourcesModal: true,
          submitMaskState: true,
        };
        const newState = reduce(state, {
          type: ORIGINAL_SOURCES_OVERRIDE_SAVE_REQUESTED,
        });
        expect(newState.showOriginalSourcesModal).toBeTruthy();
        expect(newState.submitMaskState).toBeFalsy();
      });

      describe('ORIGINAL_SOURCES_OVERRIDE_SUBMIT_MASK_DONE action', function () {
        it('success displayed', function () {
          const state = {
            saveOriginalSourceError: null,
            submitMaskState: true,
            showOriginalSourcesModal: false,
          };
          const newState = reduce(state, {
            type: ORIGINAL_SOURCES_OVERRIDE_SUBMIT_MASK_DONE,
          });
          expect(newState.submitMaskState).toBeNull();
          expect(newState.showOriginalSourcesModal).toBeFalsy();
        });
      });
    });
  });
});
