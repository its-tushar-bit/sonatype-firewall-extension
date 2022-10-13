/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/toastContainer/toastSlice';

describe('toast reducer', () => {
  describe('addToast', () => {
    it('adds toast to empty toasts array', () => {
      const state = Object.freeze({
        toastIdInc: 0,
        toasts: [],
      });

      const { toasts, toastIdInc } = reducer(state, {
        type: 'toast/addToast',
        payload: { type: 'success', message: 'Success Toast' },
      });

      expect(toastIdInc).toBe(1);
      expect(toasts).toEqual([
        {
          id: 1,
          message: 'Success Toast',
          type: 'success',
        },
      ]);
    });

    it('adds toast to existing toasts array', () => {
      const state = Object.freeze({
        toastIdInc: 1,
        toasts: [
          {
            id: 1,
            message: 'Success Toast',
            type: 'success',
          },
        ],
      });

      const { toasts, toastIdInc } = reducer(state, {
        type: 'toast/addToast',
        payload: { type: 'error', message: 'Error Toast' },
      });

      expect(toastIdInc).toBe(2);
      expect(toasts).toEqual([
        {
          id: 2,
          message: 'Error Toast',
          type: 'error',
        },
        {
          id: 1,
          message: 'Success Toast',
          type: 'success',
        },
      ]);
    });
  });

  describe('removeToast', () => {
    it('removes toast from toasts array', () => {
      const state = Object.freeze({
        toastIdInc: 1,
        toasts: [
          {
            id: 1,
            message: 'Success Toast',
            type: 'success',
          },
        ],
      });

      const { toasts } = reducer(state, {
        type: 'toast/removeToast',
        payload: 1,
      });

      expect(toasts).toEqual([]);
    });
  });

  describe('removeAllToasts', () => {
    it('removes all toasts from toasts array', () => {
      const state = Object.freeze({
        toastIdInc: 3,
        toasts: [
          {
            id: 1,
            message: 'Success Toast',
            type: 'success',
          },
          {
            id: 2,
            message: 'Error Toast',
            type: 'error',
          },
          {
            id: 3,
            message: 'Warning Toast',
            type: 'warning',
          },
        ],
      });

      const { toasts } = reducer(state, {
        type: 'toast/removeAllToasts',
      });

      expect(toasts).toEqual([]);
    });
  });
});
