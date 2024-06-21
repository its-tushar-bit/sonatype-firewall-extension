/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { always, ascend, cond, descend, equals, findIndex, inc, prop, sortWith, T, values } from 'ramda';

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';
import { getRecentlyImportedSbomsUrl } from 'MainRoot/util/CLMLocation';

const REDUCER_NAME = 'recentlyImportedSbomsTile';

export const SORT_DIRECTION = Object.freeze({
  ASC: 'asc',
  DESC: 'desc',
  UNSORTED: null,
});

const DEFAULT_SORT_DIRECTION = SORT_DIRECTION.UNSORTED;

export const initialState = Object.freeze({
  loading: true,
  loadingErrorMessage: null,
  sboms: null,
  sortDirection: DEFAULT_SORT_DIRECTION,
});

const loadRecentlyImportedSbomsRequested = (state) => {
  state.loading = true;
  state.loadingErrorMessage = null;
  state.sboms = null;
};

const loadRecentlyImportedSbomsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadingErrorMessage = null;
  state.sboms = payload;
};

const loadRecentlyImportedSbomsFailed = (state, { payload }) => {
  state.loading = false;
  state.loadingErrorMessage = Messages.getHttpErrorMessage(payload);
  state.sboms = null;
};

const loadRecentlyImportedSboms = createAsyncThunk(
  `${REDUCER_NAME}/loadRecentlyImportedSboms`,
  async (_, { rejectWithValue }) =>
    axios
      .get(getRecentlyImportedSbomsUrl())
      .then((response) => response.data)
      .catch((err) => rejectWithValue(err))
);

const cycleNextSortDirection = (state) => {
  const cycleList = (list, current) => {
    const index = findIndex((item) => item === current, list);
    return list[inc(index) % list.length];
  };
  const nextSortDirection = cycleList(values(SORT_DIRECTION), state.sortDirection);
  state.sortDirection = nextSortDirection;
};

const sortSboms = (state) => {
  const sortConfig = cond([
    [equals(SORT_DIRECTION.ASC), always([ascend(prop('applicationName'))])],
    [equals(SORT_DIRECTION.DESC), always([descend(prop('applicationName'))])],
    [T, always([descend(prop('importDate'))])],
  ])(state.sortDirection);
  state.sboms = sortWith(sortConfig)(state.sboms);
};

const recentlyImportedSbomsTileSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    cycleNextSortDirection,
    sortSboms,
  },
  extraReducers: {
    [loadRecentlyImportedSboms.pending]: loadRecentlyImportedSbomsRequested,
    [loadRecentlyImportedSboms.fulfilled]: loadRecentlyImportedSbomsFulfilled,
    [loadRecentlyImportedSboms.rejected]: loadRecentlyImportedSbomsFailed,
    [UI_ROUTER_ON_FINISH]: always(initialState),
  },
});

export const actions = {
  ...recentlyImportedSbomsTileSlice.actions,
  loadRecentlyImportedSboms,
};

export default recentlyImportedSbomsTileSlice.reducer;
