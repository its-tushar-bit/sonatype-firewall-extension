/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import { createAsyncThunk } from '@reduxjs/toolkit';
import { Messages } from 'MainRoot/util/CommonServices';
import {
  getListPolicyWaiverRequestsUrl,
  getViewOrUpdatePolicyWaiverRequestUrl,
  getReviewPolicyWaiverRequestUrl,
} from 'MainRoot/util/CLMLocation';
import { setShowLimitedFirewallAccessAlert } from 'MainRoot/firewall/firewallActions';

const ROOT_ORG_OWNER_TYPE = 'organization';
const ROOT_ORG_OWNER_ID = 'ROOT_ORGANIZATION_ID';
const REDUCER_NAME = 'firewallWaiverRequests';

export const initialState = Object.freeze({
  loading: false,
  error: null,
  waiverRequests: [],
  // Review page state
  reviewPage: {
    loading: false,
    error: null,
    waiverRequest: null,
    hasWaivePermission: false,
    isSubmitting: false,
    submitError: null,
    rejectionReason: '',
  },
});

// Thunks

const loadWaiverRequests = createAsyncThunk(
  `${REDUCER_NAME}/loadWaiverRequests`,
  async (_, { dispatch, rejectWithValue }) => {
    try {
      const response = await axios.get(getListPolicyWaiverRequestsUrl(ROOT_ORG_OWNER_TYPE, ROOT_ORG_OWNER_ID));
      return response.data;
    } catch (error) {
      if (error?.response?.status === 403) {
        dispatch(setShowLimitedFirewallAccessAlert(true));
        return rejectWithValue({ limitedAccess: true });
      }
      dispatch(setShowLimitedFirewallAccessAlert(false));
      return rejectWithValue(error);
    }
  }
);

const loadWaiverRequestForReview = createAsyncThunk(
  `${REDUCER_NAME}/loadWaiverRequestForReview`,
  async ({ ownerType, ownerId, policyWaiverRequestId }, { rejectWithValue }) => {
    try {
      const response = await axios.get(
        getViewOrUpdatePolicyWaiverRequestUrl(ownerType, ownerId, policyWaiverRequestId)
      );
      // canReview is computed server-side to handle container-image waivers correctly: a
      // scoped Firewall approver has WAIVE_POLICY_VIOLATIONS on a specific Docker repo but not
      // on the virtual REPOSITORY_CONTAINER_ID the request is parked under. Falling back to a
      // client-side checkPermissions on (ownerType, ownerId) would return false here even when
      // the backend would accept the approval. Older backends omit the field; treat missing as
      // false so the UI errs on the safe side.
      return { waiverRequest: response.data, hasWaivePermission: Boolean(response.data?.canReview) };
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const approveWaiverRequest = createAsyncThunk(
  `${REDUCER_NAME}/approveWaiverRequest`,
  async (
    {
      ownerType,
      ownerId,
      policyWaiverRequestId,
      matcherStrategy,
      expiryTime,
      waiverReasonId,
      comment,
      expireWhenRemediationAvailable,
    },
    { rejectWithValue }
  ) => {
    try {
      const response = await axios.post(getReviewPolicyWaiverRequestUrl(ownerType, ownerId, policyWaiverRequestId), {
        status: 'APPROVED',
        matcherStrategy,
        expiryTime,
        waiverReasonId: waiverReasonId || null,
        comment: comment || null,
        expireWhenRemediationAvailable: expireWhenRemediationAvailable || false,
      });
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const rejectWaiverRequest = createAsyncThunk(
  `${REDUCER_NAME}/rejectWaiverRequest`,
  async ({ ownerType, ownerId, policyWaiverRequestId, rejectionReason }, { rejectWithValue }) => {
    try {
      const response = await axios.post(getReviewPolicyWaiverRequestUrl(ownerType, ownerId, policyWaiverRequestId), {
        status: 'REJECTED',
        rejectionReason,
      });
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

// Reducers

const loadWaiverRequestsRequested = (state) => {
  state.loading = true;
  state.error = null;
};

const loadWaiverRequestsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.waiverRequests = payload;
};

const loadWaiverRequestsFailed = (state, { payload }) => {
  state.loading = false;
  state.error = payload?.limitedAccess ? null : Messages.getHttpErrorMessage(payload);
};

const loadWaiverRequestForReviewRequested = (state) => {
  state.reviewPage.loading = true;
  state.reviewPage.error = null;
};

const loadWaiverRequestForReviewFulfilled = (state, { payload }) => {
  state.reviewPage.loading = false;
  state.reviewPage.waiverRequest = payload.waiverRequest;
  state.reviewPage.hasWaivePermission = payload.hasWaivePermission;
};

const loadWaiverRequestForReviewFailed = (state, { payload }) => {
  state.reviewPage.loading = false;
  state.reviewPage.error = Messages.getHttpErrorMessage(payload);
};

const reviewSubmitRequested = (state) => {
  state.reviewPage.isSubmitting = true;
  state.reviewPage.submitError = null;
};

const reviewSubmitFulfilled = (state) => {
  state.reviewPage.isSubmitting = false;
};

const reviewSubmitFailed = (state, { payload }) => {
  state.reviewPage.isSubmitting = false;
  state.reviewPage.submitError = Messages.getHttpErrorMessage(payload);
};

const setRejectionReason = (state, { payload }) => {
  state.reviewPage.rejectionReason = payload;
};

const firewallWaiverRequestsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setRejectionReason,
  },
  extraReducers: {
    [loadWaiverRequests.pending]: loadWaiverRequestsRequested,
    [loadWaiverRequests.fulfilled]: loadWaiverRequestsFulfilled,
    [loadWaiverRequests.rejected]: loadWaiverRequestsFailed,
    [loadWaiverRequestForReview.pending]: loadWaiverRequestForReviewRequested,
    [loadWaiverRequestForReview.fulfilled]: loadWaiverRequestForReviewFulfilled,
    [loadWaiverRequestForReview.rejected]: loadWaiverRequestForReviewFailed,
    [approveWaiverRequest.pending]: reviewSubmitRequested,
    [approveWaiverRequest.fulfilled]: reviewSubmitFulfilled,
    [approveWaiverRequest.rejected]: reviewSubmitFailed,
    [rejectWaiverRequest.pending]: reviewSubmitRequested,
    [rejectWaiverRequest.fulfilled]: reviewSubmitFulfilled,
    [rejectWaiverRequest.rejected]: reviewSubmitFailed,
  },
});

export const actions = {
  ...firewallWaiverRequestsSlice.actions,
  loadWaiverRequests,
  loadWaiverRequestForReview,
  approveWaiverRequest,
  rejectWaiverRequest,
};

export default firewallWaiverRequestsSlice.reducer;
