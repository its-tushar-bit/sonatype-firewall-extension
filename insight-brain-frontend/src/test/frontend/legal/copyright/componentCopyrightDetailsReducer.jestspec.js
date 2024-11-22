/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reduce from '../../../../main/frontend/legal/copyright/componentCopyrightDetailsReducer';
import {
  COPYRIGHT_CONTEXT_FAILED,
  COPYRIGHT_CONTEXT_FULFILLED,
  COPYRIGHT_CONTEXT_REQUEST,
  COPYRIGHT_DETAILS_FAILED,
  COPYRIGHT_DETAILS_FULFILLED,
  COPYRIGHT_DETAILS_REQUEST,
  COPYRIGHT_FILE_PATHS_FAILED,
  COPYRIGHT_FILE_PATHS_FULFILLED,
  COPYRIGHT_FILE_PATHS_REQUEST,
} from '../../../../main/frontend/legal/copyright/componentCopyrightDetailsActions';

describe('componentCopyrightDetailsReducer', function () {
  describe('initial state', function () {
    it('is used if no state is provided', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reduce(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reduce(undefined, action);

      expect(newState.selectedCopyright).toBeNull();
      expect(newState.filePathsPage).toEqual(0);
      expect(newState.filePaths).toEqual([]);
      expect(newState.copyrightContexts).toEqual([]);
      expect(newState.copyrightFileCounts).toEqual({});
      expect(newState.totalFileMatches).toEqual(0);
      expect(newState.loadingCopyrightFileCounts).toBeFalsy();
      expect(newState.loadingFilePaths).toBeFalsy();
      expect(newState.loadingCopyrightContext).toBeFalsy();
      expect(newState.errorCopyrightFileCounts).toBeNull();
      expect(newState.errorCopyrightContext).toBeNull();
      expect(newState.errorFilePaths).toBeNull();
    });
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

  describe('Copyright Details action', function () {
    const originalState = {
      loadingCopyrightContext: false,
      filePathsPage: 10,
      copyrightContexts: [
        {
          contexts: ['context1', 'context2'],
          filePath: 'filePath1',
        },
        {
          contexts: ['context1', 'context2'],
          filePath: 'filePath2',
        },
      ],
      copyrightFileCounts: { 1: 2 },
      filePaths: ['path1', 'path2'],
      totalFileMatches: 5,
    };

    it('COPYRIGHT_DETAILS_REQUEST sets loading flags to true', function () {
      const newState = reduce(originalState, {
        type: COPYRIGHT_DETAILS_REQUEST,
        payload: {
          copyright: { content: 'copyright' },
          loadingCopyrightFileCounts: true,
          loadingFilePaths: true,
        },
      });
      expect(newState.loadingCopyrightFileCounts).toBeTruthy();
      expect(newState.selectedCopyright).toEqual({ content: 'copyright' });
      expect(newState.loadingFilePaths).toBeTruthy();
      expect(newState.filePathsPage).toEqual(0);

      expect(newState.copyrightFileCounts).toEqual({ 1: 2 });
      expect(newState.totalFileMatches).toEqual(5);
      expect(newState.selectedFilePaths).toEqual([]);
      expect(newState.copyrightContexts).toEqual([]);
      expect(newState.filePaths).toEqual(['path1', 'path2']);
    });

    it('COPYRIGHT_DETAILS_FULFILLED sets copyright details in the state', function () {
      const newState = reduce(originalState, {
        type: COPYRIGHT_DETAILS_FULFILLED,
        payload: {
          copyright: 'copyright',
          filePaths: { filePaths: ['path1', 'path2'], totalFileMatches: 2 },
          copyrightFileCounts: { 2: 10 },
        },
      });

      expect(newState.loadingCopyrightFileCounts).toBeFalsy();
      expect(newState.loadingFilePaths).toBeFalsy();
      expect(newState.errorCopyrightFileCounts).toBeNull();
      expect(newState.errorFilePaths).toBeNull();
      expect(newState.selectedCopyright).toEqual('copyright');
      expect(newState.filePaths).toEqual(['path1', 'path2']);
      expect(newState.totalFileMatches).toEqual(2);
      expect(newState.copyrightFileCounts).toEqual({ 2: 10 });
    });

    it('COPYRIGHT_DETAILS_FAILED sets corresponding error', function () {
      const newState = reduce(originalState, {
        type: COPYRIGHT_DETAILS_FAILED,
        payload: { value: 'Error' },
      });

      expect(newState.errorCopyrightFileCounts).toEqual('Error');

      expect(newState.filePathsPage).toEqual(10);
      expect(newState.filePaths).toEqual(['path1', 'path2']);
      expect(newState.totalFileMatches).toEqual(5);
      expect(newState.copyrightFileCounts).toEqual({ 1: 2 });
      expect(newState.copyrightContexts).toEqual([
        {
          contexts: ['context1', 'context2'],
          filePath: 'filePath1',
        },
        {
          contexts: ['context1', 'context2'],
          filePath: 'filePath2',
        },
      ]);
    });
  });

  describe('Copyright file paths action', function () {
    const originalState = {
      filePathsPage: 10,
      filePaths: ['path'],
      totalFileMatches: 5,
      copyrightContexts: [
        {
          contexts: ['context1', 'context2'],
          filePath: 'filePath',
        },
      ],
      copyrightFileCounts: { 1: 2 },
    };

    it('COPYRIGHT_FILE_PATHS_REQUEST sets loading flag to true', function () {
      const newState = reduce(originalState, {
        type: COPYRIGHT_FILE_PATHS_REQUEST,
        payload: { filePathsPage: 1 },
      });

      expect(newState.loadingFilePaths).toBeFalsy();
      expect(newState.errorFilePaths).toBeFalsy();
      expect(newState.filePathsPage).toEqual(1);

      expect(newState.filePaths).toEqual(['path']);
      expect(newState.copyrightContexts).toEqual([
        {
          contexts: ['context1', 'context2'],
          filePath: 'filePath',
        },
      ]);
      expect(newState.copyrightFileCounts).toEqual({ 1: 2 });
      expect(newState.totalFileMatches).toEqual(5);
    });

    it('COPYRIGHT_FILE_PATHS_FULFILLED sets file paths', function () {
      const newState = reduce(originalState, {
        type: COPYRIGHT_FILE_PATHS_FULFILLED,
        payload: {
          filePaths: { filePaths: ['path1', 'path2'], totalFileMatches: 2 },
        },
      });

      expect(newState.loadingFilePaths).toBeFalsy();
      expect(newState.errorFilePaths).toBeFalsy();
      expect(newState.filePaths).toEqual(['path1', 'path2']);
      expect(newState.totalFileMatches).toEqual(2);

      expect(newState.filePathsPage).toEqual(10);
      expect(newState.copyrightContexts).toEqual([]);
      expect(newState.copyrightFileCounts).toEqual({ 1: 2 });
    });

    it('COPYRIGHT_FILE_PATHS_FAILED sets corresponding error', function () {
      const newState = reduce(originalState, {
        type: COPYRIGHT_FILE_PATHS_FAILED,
        payload: {
          value: 'Error',
        },
      });

      expect(newState.loadingFilePaths).toBeFalsy();
      expect(newState.errorFilePaths).toEqual('Error');

      expect(newState.filePaths).toEqual(['path']);
      expect(newState.filePathsPage).toEqual(10);
      expect(newState.copyrightContexts).toEqual([
        {
          contexts: ['context1', 'context2'],
          filePath: 'filePath',
        },
      ]);
      expect(newState.copyrightFileCounts).toEqual({ 1: 2 });
      expect(newState.totalFileMatches).toEqual(5);
    });
  });

  describe('Copyright context action', function () {
    const originalState = {
      filePaths: ['path'],
      filePathsPage: 3,
      totalFileMatches: 5,
      copyrightContexts: [
        {
          contexts: ['context1', 'context2'],
          filePath: 'filePath',
        },
      ],
      copyrightFileCounts: { 1: 2 },
    };

    it('COPYRIGHT_CONTEXT_REQUEST sets loading flag to true', function () {
      const newState = reduce(originalState, {
        type: COPYRIGHT_CONTEXT_REQUEST,
        payload: { selectedFilePaths: ['filePath'] },
      });

      expect(newState.loadingCopyrightContext).toBeTruthy();
      expect(newState.errorCopyrightFileCounts).toBeFalsy();
      expect(newState.copyrightContexts).toEqual([
        {
          contexts: ['context1', 'context2'],
          filePath: 'filePath',
        },
      ]);
      expect(newState.selectedFilePaths).toEqual(['filePath']);

      expect(newState.copyrightFileCounts).toEqual({ 1: 2 });
      expect(newState.totalFileMatches).toEqual(5);
      expect(newState.filePathsPage).toEqual(3);
      expect(newState.filePaths).toEqual(['path']);
    });

    it('COPYRIGHT_CONTEXT_FULFILLED sets copyright contexts in the state', function () {
      const newState = reduce(originalState, {
        type: COPYRIGHT_CONTEXT_FULFILLED,
        payload: { filePath: 'filePath2', copyrightContexts: ['context1', 'context2'] },
      });

      expect(newState.loadingCopyrightContext).toBeFalsy();
      expect(newState.errorCopyrightFileCounts).toBeFalsy();
      expect(newState.copyrightContexts).toEqual([
        {
          contexts: ['context1', 'context2'],
          filePath: 'filePath',
        },
        {
          contexts: ['context1', 'context2'],
          filePath: 'filePath2',
        },
      ]);

      expect(newState.copyrightFileCounts).toEqual({ 1: 2 });
      expect(newState.totalFileMatches).toEqual(5);
      expect(newState.filePathsPage).toEqual(3);
      expect(newState.filePaths).toEqual(['path']);
    });

    it('COPYRIGHT_CONTEXT_FAILED sets corresponding error', function () {
      const newState = reduce(originalState, {
        type: COPYRIGHT_CONTEXT_FAILED,
        payload: { value: 'Error' },
      });

      expect(newState.loadingCopyrightContext).toBeFalsy();
      expect(newState.errorCopyrightContext).toEqual('Error');

      expect(newState.copyrightContexts).toEqual([
        {
          contexts: ['context1', 'context2'],
          filePath: 'filePath',
        },
      ]);
      expect(newState.copyrightFileCounts).toEqual({ 1: 2 });
      expect(newState.totalFileMatches).toEqual(5);
      expect(newState.filePathsPage).toEqual(3);
      expect(newState.filePaths).toEqual(['path']);
    });
  });
});
