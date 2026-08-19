/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { prop } from 'ramda';
import { createAsyncThunk } from '@reduxjs/toolkit';

import createSlice from 'MainRoot/reduxConfig/createSlice';
import { getAnnouncementBannerFetchUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';
import { actions as userLoginActions } from 'MainRoot/user/LoginModal/userLoginSlice';
import { logout as userSessionLogoutThunk } from 'MainRoot/user/userSessionSlice';

const REDUCER_NAME = 'announcementBanner';

export const DISMISS_STORAGE_KEY = 'announcement-banner-dismissed';

export const initialState = Object.freeze({
  loading: false,
  loadError: null,
  banner: null,
  dismissedWindowId: null,
  // Set on logout/pending and cleared on login; prevents the banner from flashing back into view during the
  // brief window between server-side logout completing and the browser navigating away.
  suppressedByLogout: false,
});

/**
 * Async thunk: fetch the current announcement banner from the backend. The slice does not dispatch this
 * itself — the consuming component is responsible for dispatching both `hydrateDismissed` (to read any
 * previous dismissal from sessionStorage) and then this thunk on mount, and for polling on an interval.
 */
const loadAnnouncementBanner = createAsyncThunk(`${REDUCER_NAME}/load`, (_, { rejectWithValue }) =>
  axios.get(getAnnouncementBannerFetchUrl()).then(prop('data')).catch(rejectWithValue)
);

const loadPending = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadFulfilled = (state, { payload }) => {
  state.loading = false;
  state.banner = payload;
};

const loadRejected = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

/** User dismissed the banner in this session. Persist in sessionStorage so in-session navigation keeps it hidden. */
const dismiss = (state, { payload: windowId }) => {
  // Coerce null/undefined to null and skip the write; sessionStorage would otherwise stringify undefined to
  // the literal "undefined", producing a dismissedWindowId that never matches a real banner.
  if (windowId == null) {
    state.dismissedWindowId = null;
    return;
  }
  state.dismissedWindowId = windowId;
  try {
    sessionStorage.setItem(DISMISS_STORAGE_KEY, windowId);
  } catch {
    // sessionStorage unavailable; in-memory state still works.
  }
};

/** Hydrate dismissedWindowId from sessionStorage on app mount. */
const hydrateDismissed = (state) => {
  try {
    const stored = sessionStorage.getItem(DISMISS_STORAGE_KEY);
    if (stored) {
      state.dismissedWindowId = stored;
    }
  } catch {
    // sessionStorage unavailable
  }
};

/** Clear any prior dismissal — fired by login transitions so the banner reappears on next login. */
const clearDismissal = (state) => {
  state.dismissedWindowId = null;
  state.suppressedByLogout = false;
  try {
    sessionStorage.removeItem(DISMISS_STORAGE_KEY);
  } catch {
    // sessionStorage unavailable
  }
};

/** Like {@link clearDismissal} but leaves `suppressedByLogout` on to prevent a banner flash before navigation. */
const clearDismissalKeepSuppression = (state) => {
  state.dismissedWindowId = null;
  try {
    sessionStorage.removeItem(DISMISS_STORAGE_KEY);
  } catch {
    // sessionStorage unavailable
  }
};

/** Hide the banner while logout is in progress so it doesn't flash before navigation. */
const suppressForLogout = (state) => {
  state.suppressedByLogout = true;
};

/** Clear suppression if logout fails so the banner reappears. */
const clearLogoutSuppression = (state) => {
  state.suppressedByLogout = false;
};

const announcementBannerSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    dismiss,
    hydrateDismissed,
    clearDismissal,
  },
  extraReducers: {
    [loadAnnouncementBanner.pending]: loadPending,
    [loadAnnouncementBanner.fulfilled]: loadFulfilled,
    [loadAnnouncementBanner.rejected]: loadRejected,
    [userLoginActions.submitUserLogin.fulfilled]: clearDismissal,
    [userSessionLogoutThunk.pending]: suppressForLogout,
    [userSessionLogoutThunk.fulfilled]: clearDismissalKeepSuppression,
    [userSessionLogoutThunk.rejected]: clearLogoutSuppression,
  },
});

export default announcementBannerSlice.reducer;

export const actions = {
  ...announcementBannerSlice.actions,
  loadAnnouncementBanner,
};
