/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axios from 'axios';
import { getReact2ShellReportDownloadUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';

const REDUCER_NAME = 'react2shell';

const initialState = {
  error: null,
  downloadLoading: false,
};

export const downloadReact2ShellCSV = createAsyncThunk(
  `${REDUCER_NAME}/downloadCSV`,
  async (_, { rejectWithValue }) => {
    try {
      const response = await axios.get(getReact2ShellReportDownloadUrl(), { responseType: 'blob' });
      const contentDisposition = response.headers['content-disposition'];
      let filename = 'react2shell-report.csv';
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="(.+?)"/);
        if (filenameMatch) {
          filename = filenameMatch[1];
        }
      }

      const blob = new Blob([response.data], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();

      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);

      return null;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const downloadDataRequested = (state) => {
  state.downloadLoading = true;
  state.error = null;
};

const downloadDataFulfilled = (state) => {
  state.downloadLoading = false;
};

const downloadDataFailed = (state, { payload }) => {
  state.downloadLoading = false;
  state.error = Messages.getHttpErrorMessage(payload);
};

const react2shellSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    reset: () => initialState,
    setDownloadError: (state, action) => {
      state.error = action.payload;
    },
    clearDownloadError: (state) => {
      state.error = null;
    },
  },
  extraReducers: {
    [downloadReact2ShellCSV.pending]: downloadDataRequested,
    [downloadReact2ShellCSV.fulfilled]: downloadDataFulfilled,
    [downloadReact2ShellCSV.rejected]: downloadDataFailed,
  },
});

export const actions = {
  ...react2shellSlice.actions,
  downloadReact2ShellCSV,
};

export default react2shellSlice.reducer;
