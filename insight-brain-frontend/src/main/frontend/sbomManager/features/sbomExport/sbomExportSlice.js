/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { __, always, compose, gt, ifElse, includes, length, match, nth, replace, trim, values } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { getDownloadSbomFileUrl } from 'MainRoot/util/CLMLocation';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';

import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';
import { selectSbomExportSlice } from './sbomExportSelectors';

const REDUCER_NAME = 'sbomExport';

const DEFAULT_SBOM_FILENAME = 'exported_sbom';

const extractFileNameFromResponseDisposition = (disposition, defaultFileName) =>
  compose(ifElse(compose(gt(__, 1), length), nth(1), always(defaultFileName)), match(/filename="(.+)"/))(disposition);

export const exportAndDownloadSbomSubmitMaskInitialState = Object.freeze({
  showSubmitMask: false,
  success: false,
});

export const EXPORT_SBOM_FILE_FORMAT = Object.freeze({
  json: 'application/json',
  xml: 'application/xml',
});

export const EXPORT_SBOM_SPECIFICATION = Object.freeze({
  cyclonedx: 'cyclonedx1.6',
  spdx: 'spdx2.3',
});

export const EXPORT_SBOM_STATE = Object.freeze({
  original: 'original',
  current: 'current',
});

export const sbomAdditionalExportOptionsModalInitialState = Object.freeze({
  showModal: false,
  applicationId: null,
  sbomVersion: null,
  sbomSpecification: EXPORT_SBOM_SPECIFICATION.cyclonedx,
  sbomFileFormat: EXPORT_SBOM_FILE_FORMAT.json,
});

export const initialState = Object.freeze({
  ...sbomAdditionalExportOptionsModalInitialState,
  exportAndDownloadSbomSubmitMask: { ...exportAndDownloadSbomSubmitMaskInitialState },
});

const setShowSbomAdditionalExportOptionsModal = (state, { payload: { applicationId, sbomVersion } }) => {
  state.showModal = true;
  state.applicationId = applicationId;
  state.sbomVersion = sbomVersion;
};

const setExportSbomSpecification = (state, { payload }) => {
  if (includes(payload, values(EXPORT_SBOM_SPECIFICATION))) {
    state.sbomSpecification = payload;
  } else {
    throw new TypeError('Invalid SBOM specification');
  }
};

const setExportSbomFileFormat = (state, { payload }) => {
  if (includes(payload, values(EXPORT_SBOM_FILE_FORMAT))) {
    state.sbomFileFormat = payload;
  } else {
    throw new TypeError('Invalid SBOM format');
  }
};

const resetSbomAdditionalExportOptionsModal = always(initialState);

const exportAndDownloadSbomRequested = (state) => {
  state.showModal = false;
  state.exportAndDownloadSbomSubmitMask.showSubmitMask = true;
};

const exportAndDownloadSbomFulfilled = () => {
  return {
    ...sbomAdditionalExportOptionsModalInitialState,
    exportAndDownloadSbomSubmitMask: {
      showSubmitMask: true,
      success: true,
    },
  };
};

const exportAndDownloadSbomSubmitMaskTimerDone = (state) => {
  state.exportAndDownloadSbomSubmitMask = { ...exportAndDownloadSbomSubmitMaskInitialState };
};

const startExportAndDownloadSbomSubmitMaskSuccessTimer = (dispatch) => {
  setTimeout(() => dispatch(actions.exportAndDownloadSbomSubmitMaskTimerDone()), SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
};

const getErrorMessageFromResponseBlob = async (error) => {
  try {
    return await error.response.data.text();
  } catch {
    return '';
  }
};

const exportAndDownloadSbom = createAsyncThunk(
  `${REDUCER_NAME}/exportAndDownloadSbom`,
  async (options = {}, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const { sbomFileFormat, sbomSpecification, applicationId, sbomVersion } = selectSbomExportSlice(state);
    const headersAccept = options.fileFormat || sbomFileFormat;
    const downloadSbomUrl = getDownloadSbomFileUrl(
      options.applicationId || applicationId,
      options.sbomVersion || sbomVersion,
      options.state,
      options.specification || sbomSpecification
    );

    let url, link;
    try {
      const response = await axios.get(downloadSbomUrl, {
        headers: {
          Accept: headersAccept,
        },
        responseType: 'blob',
      });

      const fileExtension = replace('application/', '', headersAccept);
      const defaultFileName = DEFAULT_SBOM_FILENAME + '_' + Date.now() + '.' + fileExtension;
      const disposition = response?.headers?.get('Content-Disposition') || '';
      const fileName = extractFileNameFromResponseDisposition(disposition, defaultFileName);

      url = window.URL.createObjectURL(new Blob([response.data]));
      link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', fileName);
      document.body.appendChild(link);
      link.click();

      return startExportAndDownloadSbomSubmitMaskSuccessTimer(dispatch);
    } catch (error) {
      const errorMessageResponse = await getErrorMessageFromResponseBlob(error);
      const errorMessage = trim('SBOM export failed. ' + errorMessageResponse);
      dispatch(toastActions.addToast({ type: 'error', message: errorMessage }));

      return rejectWithValue(errorMessage);
    } finally {
      window.URL.revokeObjectURL(url);
      if (link) {
        link.remove();
      }
    }
  }
);

const sbomExportSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    closeShowSbomAdditionalExportOptionsModal: resetSbomAdditionalExportOptionsModal,
    setShowSbomAdditionalExportOptionsModal,
    setExportSbomSpecification,
    setExportSbomFileFormat,
    exportAndDownloadSbomSubmitMaskTimerDone,
  },
  extraReducers: {
    [exportAndDownloadSbom.pending]: exportAndDownloadSbomRequested,
    [exportAndDownloadSbom.fulfilled]: exportAndDownloadSbomFulfilled,
    [exportAndDownloadSbom.rejected]: resetSbomAdditionalExportOptionsModal,
    [UI_ROUTER_ON_FINISH]: resetSbomAdditionalExportOptionsModal,
  },
});

export const actions = {
  ...sbomExportSlice.actions,
  exportAndDownloadSbom,
};

export default sbomExportSlice.reducer;
