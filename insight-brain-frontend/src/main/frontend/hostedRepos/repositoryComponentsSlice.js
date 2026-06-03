/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import { Messages } from 'MainRoot/util/CommonServices';

const REDUCER_NAME = 'repositoryComponents';
const DEFAULT_PAGE_SIZE = 25;

const initialState = {
  components: [],
  totalCount: 0,
  currentPage: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  hasNextPage: false,
  repositoryPublicId: null,
  hasQueuedScans: false,
  filter: '',
  loading: false,
  error: null,
};

export const loadComponents = createAsyncThunk(
  `${REDUCER_NAME}/loadComponents`,
  async ({ repositoryManagerId, repositoryId, page, filter }, { rejectWithValue }) => {
    try {
      const params = { page, pageSize: DEFAULT_PAGE_SIZE };
      if (filter) params.filter = filter;
      const { data } = await axios.get(`/api/v2/repositories/${repositoryManagerId}/${repositoryId}/components`, {
        params,
      });
      return { ...data, page };
    } catch (err) {
      return rejectWithValue(err);
    }
  }
);

const repositoryComponentsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setFilter(state, { payload }) {
      state.filter = payload;
      state.currentPage = 1;
    },
    reset() {
      return initialState;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(loadComponents.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(loadComponents.fulfilled, (state, { payload }) => {
        state.loading = false;
        state.components = payload.components;
        state.totalCount = payload.totalCount;
        state.currentPage = payload.page;
        state.pageSize = payload.pageSize;
        state.hasNextPage = payload.hasNextPage;
        state.repositoryPublicId = payload.repositoryPublicId || null;
        state.hasQueuedScans = payload.hasQueuedScans || false;
      })
      .addCase(loadComponents.rejected, (state, { payload }) => {
        state.loading = false;
        state.error = Messages.getHttpErrorMessage(payload);
      });
  },
});

export const { setFilter, reset } = repositoryComponentsSlice.actions;
export default repositoryComponentsSlice.reducer;
