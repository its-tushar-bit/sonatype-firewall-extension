/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk } from '@reduxjs/toolkit';
import createSlice from 'MainRoot/reduxConfig/createSlice';
import axios from 'axios';
import {
  length,
  map,
  pipe,
  uniqBy,
  uniq,
  sort,
  indexOf,
  reduceBy,
  inc,
  reject,
  sum,
  values,
  filter,
  pathOr,
} from 'ramda';
import { getActiveViolationsWithActionFailUrl, getAddContainerImagePolicyWaiverUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';
import {
  nxDateInputStateHelpers,
  nxTextInputStateHelpers,
  SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS,
} from '@sonatype/react-shared-components';
import { getExpiryTime, isCustomExpiryTimeValid } from 'MainRoot/util/waiverUtils';
import { actions as waiverActions } from 'MainRoot/waivers/waiverSlice';
import { propSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { getISODateFromDateInput } from 'MainRoot/util/jsUtil';

const REDUCER_NAME = 'addContainerImageWaiverPage';

export const initialState = Object.freeze({
  loading: false,
  error: null,
  isDirty: false,
  submitMaskState: null,
  submitError: null,
  containerImageName: '',
  affectedComponentsCount: 0,
  failViolationsCount: 0,
  policyNameList: [],
  threatLevelCounts: {},
  expiryTime: null,
  waiverReasonId: null,
  waiverComments: nxTextInputStateHelpers.initialState(''),
  customExpiryTime: nxDateInputStateHelpers.initialState(''),
});

const isFormDirty = (state) => {
  const { expiryTime, customExpiryTime, waiverReasonId, waiverComments } = state;

  return (
    expiryTime !== null ||
    (customExpiryTime && !customExpiryTime.isPristine) ||
    waiverReasonId !== null ||
    (waiverComments && !waiverComments.isPristine)
  );
};

const setIsDirtyFlag = (partialNewState) => ({
  ...partialNewState,
  isDirty: isFormDirty(partialNewState),
});

const load = createAsyncThunk(`${REDUCER_NAME}/load`, (publicId, { rejectWithValue, dispatch }) => {
  const promises = [
    axios.get(getActiveViolationsWithActionFailUrl(publicId, 'proxy')),
    dispatch(waiverActions.loadCachedWaiverReasons()),
  ];
  return Promise.all(promises)
    .then((res) => {
      return res[0]?.data;
    })
    .catch(rejectWithValue);
});

const save = createAsyncThunk(
  `${REDUCER_NAME}/save`,
  ({ publicId, scanId }, { getState, rejectWithValue, dispatch }) => {
    const state = getState().addContainerImageWaiverPage;
    const expiration = getExpiration(state);
    const serverData = {
      expiryTime: typeof expiration === 'string' ? getISODateFromDateInput(expiration) : getExpiryTime(expiration),
      waiverReasonId: state.waiverReasonId,
      comment: state.waiverComments.value,
    };

    return axios
      .post(getAddContainerImagePolicyWaiverUrl(publicId), serverData)
      .then(() => {
        startSubmitMaskSuccessTimer(dispatch, publicId, scanId);
      })
      .catch(rejectWithValue);
  }
);

const getExpiration = (state) => {
  const { expiryTime, customExpiryTime } = state;
  if (expiryTime === 'custom' && customExpiryTime.value) {
    return customExpiryTime.value;
  } else if (expiryTime === 'never' || expiryTime === null || expiryTime === 'remediationAvailable') {
    return null;
  } else {
    return parseInt(expiryTime, 10);
  }
};

const loadRequested = () => {
  return {
    ...initialState,
    loading: true,
  };
};

const loadFulfilled = (state, { payload: activeViolationsResult }) => {
  const affectedComponentsCount = length(
    uniqBy((activeViolation) => JSON.stringify(activeViolation.componentIdentifier), activeViolationsResult)
  );
  const containerImageName = pathOr(
    '',
    [0, 'componentIdentifier', 'coordinates', 'namespace'],
    activeViolationsResult
  ).replace(':', ' : ');
  const groupByThreatLevel = (v) => {
    switch (true) {
      case v.threatLevel >= 8:
        return 'critical';
      case v.threatLevel >= 4:
        return 'severe';
      case v.threatLevel >= 2:
        return 'moderate';
      default:
        return undefined;
    }
  };
  const reduceToCountsByThreatLevel = reduceBy(inc, 0)(groupByThreatLevel);
  const rejectIgnored = reject((v) => v.threatLevel < 2);
  const threatLevelCounts = pipe(rejectIgnored, reduceToCountsByThreatLevel)(activeViolationsResult);
  const failViolationsCount = sum(values(threatLevelCounts));
  const threatLevelOrder = ['critical', 'severe', 'moderate'];
  const policies = map(
    (activeViolation) => ({
      policyName: activeViolation.policyName,
      threatLevelCategory: groupByThreatLevel(activeViolation),
    }),
    activeViolationsResult
  );
  const policyNameList = pipe(
    uniq,
    filter((policy) => policy.threatLevelCategory !== undefined),
    sort((a, b) => indexOf(a.threatLevelCategory, threatLevelOrder) - indexOf(b.threatLevelCategory, threatLevelOrder))
  )(policies);
  return {
    ...state,
    containerImageName,
    failViolationsCount,
    affectedComponentsCount,
    policyNameList,
    threatLevelCounts,
    loading: false,
  };
};

const loadFailed = (state, { payload }) => {
  return {
    ...initialState,
    loading: false,
    error: Messages.getHttpErrorMessage(payload),
  };
};

const saveRequested = (state) => {
  return {
    ...state,
    submitMaskState: false,
  };
};

const saveFulfilled = (state) => {
  return {
    ...state,
    submitMaskState: true,
    isDirty: false,
  };
};

const saveFailed = (state, { payload }) => {
  return {
    ...state,
    submitMaskState: null,
    submitError: Messages.getHttpErrorMessage(payload),
  };
};

const setExpiryTime = (state, { payload }) =>
  setIsDirtyFlag({
    ...state,
    expiryTime: payload,
    customExpiryTime: nxDateInputStateHelpers.initialState(''),
  });

const setWaiverReason = (state, { payload }) =>
  setIsDirtyFlag({
    ...state,
    waiverReasonId: payload || null,
  });

const setWaiverComment = (state, { payload }) =>
  setIsDirtyFlag({
    ...state,
    waiverComments: nxTextInputStateHelpers.userInput(null, payload),
  });

const customDateValidator = (value) => (isCustomExpiryTimeValid(value) ? null : 'Date must be in the future');

const setCustomExpiryTime = (state, { payload }) =>
  setIsDirtyFlag({
    ...state,
    customExpiryTime: nxDateInputStateHelpers.userInput(customDateValidator, payload),
  });

const startSubmitMaskSuccessTimer = (dispatch, publicId, scanId) => {
  setTimeout(() => {
    dispatch(actions.resetSubmitMaskState());
    dispatch(actions.returnToContainerReportPage(publicId, scanId));
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
};

export const returnToContainerReportPage = (publicId, scanId) => {
  return (dispatch) => {
    dispatch(stateGo('firewall.containerReport', { publicId, scanId }));
  };
};

const addContainerImageWaiverPageSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setExpiryTime: setExpiryTime,
    setCustomExpiryTime: setCustomExpiryTime,
    setWaiverReason: setWaiverReason,
    setWaiverComment: setWaiverComment,
    resetSubmitMaskState: propSetConst('submitMaskState', null),
  },
  extraReducers: {
    [load.pending]: loadRequested,
    [load.fulfilled]: loadFulfilled,
    [load.rejected]: loadFailed,
    [save.pending]: saveRequested,
    [save.fulfilled]: saveFulfilled,
    [save.rejected]: saveFailed,
  },
});

export default addContainerImageWaiverPageSlice.reducer;

export const actions = {
  ...addContainerImageWaiverPageSlice.actions,
  load,
  save,
  returnToContainerReportPage,
};
