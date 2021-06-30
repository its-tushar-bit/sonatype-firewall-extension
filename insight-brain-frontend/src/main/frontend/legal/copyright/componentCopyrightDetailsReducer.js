/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createReducerFromActionMap } from '../../util/reduxUtil';
import {
  COPYRIGHT_CONTEXT_FULFILLED,
  COPYRIGHT_CONTEXT_REQUEST,
  COPYRIGHT_CONTEXT_FAILED,
  COPYRIGHT_DETAILS_FAILED,
  COPYRIGHT_DETAILS_FULFILLED,
  COPYRIGHT_DETAILS_REQUEST,
  COPYRIGHT_FILE_PATHS_FAILED,
  COPYRIGHT_FILE_PATHS_FULFILLED,
  COPYRIGHT_FILE_PATHS_REQUEST,
} from './componentCopyrightDetailsActions';

const initialState = Object.freeze({
  selectedCopyright: null,
  filePathsPage: 0,
  filePaths: Object.freeze([]),
  totalFileMatches: 0,
  copyrightContexts: Object.freeze([]),
  copyrightFileCounts: Object.freeze({}),
  loadingCopyrightFileCounts: false,
  loadingFilePaths: false,
  loadingCopyrightContext: false,
  errorCopyrightFileCounts: null,
  errorCopyrightContext: null,
  errorFilePaths: null,
});

function loadCopyrightFilePaths(payload, state) {
  return {
    ...state,
    errorFilePaths: null,
    loadingFilePaths: false, // don't show loading spinner when changing pages
    filePathsPage: payload.filePathsPage,
  };
}

function updateCopyrightFilePaths(payload, state) {
  return {
    ...state,
    errorFilePaths: null,
    loadingFilePaths: false,
    filePaths: payload.filePaths.filePaths,
    totalFileMatches: payload.filePaths.totalFileMatches,
    selectedFilePaths: [],
    copyrightContexts: [],
  };
}

function requestCopyrightContext(payload, state) {
  const copyrightContexts = state.copyrightContexts;
  return {
    ...state,
    loadingCopyrightContext: true,
    errorCopyrightContext: null,
    selectedFilePaths: payload.selectedFilePaths,
    copyrightContexts,
  };
}

function updateCopyrightContext(payload, state) {
  const copyrightContexts = state.copyrightContexts;
  return {
    ...state,
    errorCopyrightContext: null,
    loadingCopyrightContext: false,
    copyrightContexts: [
      ...copyrightContexts,
      {
        filePath: payload.filePath,
        contexts: payload.copyrightContexts,
      },
    ],
  };
}

function requestCopyrightDetails(payload, state) {
  return {
    ...state,
    filePathsPage: 0,
    loadingCopyrightFileCounts: payload.loadingCopyrightFileCounts,
    loadingFilePaths: payload.loadingFilePaths,
    errorCopyrightFileCounts: null,
    copyrightIndex: parseInt(payload.copyrightIndex),
    selectedCopyright: payload.copyright,
    copyrightContexts: [],
    selectedFilePaths: [],
  };
}

function updateCopyrightDetails(payload, state) {
  return {
    ...state,
    errorCopyrightFileCounts: null,
    errorFilePaths: null,
    loadingCopyrightFileCounts: false,
    loadingFilePaths: false,
    selectedCopyright: payload.copyright,
    filePaths: payload.filePaths.filePaths,
    totalFileMatches: payload.filePaths.totalFileMatches,
    copyrightFileCounts: payload.copyrightFileCounts,
  };
}

function failureCopyrightContext(payload, state) {
  return {
    ...state,
    errorCopyrightContext: payload.value,
    loadingCopyrightContext: false,
  };
}

function failureFilePaths(payload, state) {
  return {
    ...state,
    errorFilePaths: payload.value,
    loadingFilePaths: false,
  };
}

function failureCopyrightDetails(payload, state) {
  return {
    ...state,
    errorCopyrightFileCounts: payload.value,
    loadingCopyrightFileCounts: false,
  };
}

const reducerActionMap = {
  [COPYRIGHT_FILE_PATHS_REQUEST]: loadCopyrightFilePaths,
  [COPYRIGHT_FILE_PATHS_FULFILLED]: updateCopyrightFilePaths,
  [COPYRIGHT_FILE_PATHS_FAILED]: failureFilePaths,
  [COPYRIGHT_CONTEXT_REQUEST]: requestCopyrightContext,
  [COPYRIGHT_CONTEXT_FULFILLED]: updateCopyrightContext,
  [COPYRIGHT_CONTEXT_FAILED]: failureCopyrightContext,
  [COPYRIGHT_DETAILS_REQUEST]: requestCopyrightDetails,
  [COPYRIGHT_DETAILS_FULFILLED]: updateCopyrightDetails,
  [COPYRIGHT_DETAILS_FAILED]: failureCopyrightDetails,
};

const componentCopyrightDetailsReducer = createReducerFromActionMap(reducerActionMap, initialState);
export default componentCopyrightDetailsReducer;
