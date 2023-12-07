/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import axios from 'axios';
import {
  getDestinationOrganizationsUrl,
  getMoveApplicationUrl,
  getMoveOrganizationUrl,
} from 'MainRoot/util/CLMLocation';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';
import { actions as ownerSideNavActions } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSlice';
import { actions as policyMonitoringActions } from 'MainRoot/OrgsAndPolicies/policyMonitoringSlice';
import { actions as ownerSummaryActions } from 'MainRoot/OrgsAndPolicies/ownerSummarySlice';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';
import { selectIsApplication } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectOwnersMap } from '../ownerSideNav/ownerSideNavSelectors';
const REDUCER_NAME = `${OWNER_ACTIONS}/moveOwner`;

export const initialState = {
  isMoveOwnerModalOpen: false,
  fetchOrgs: {
    organizations: [],
    loadError: null,
    loading: false,
    isShowNoAvailableOrgsWarning: false,
  },
  selectedOrganization: null,
  isDirty: false,
  isShowSuccessModal: false,
  submitError: null,
  submitMaskState: null,
  warnings: null,
};

const openMoveOwnerModal = (state) => {
  state.isMoveOwnerModalOpen = true;
};

const closeMoveOwnerModal = (state) => {
  state.isMoveOwnerModalOpen = false;
  state.isDirty = false;
  state.selectedOrganization = null;
  state.fetchOrgs.isShowNoAvailableOrgsWarning = false;
  state.submitError = null;
  state.submitMaskState = null;
};

const showSuccessModal = (state) => {
  state.isShowSuccessModal = true;
};

const closeSuccessModal = (state) => {
  state.isShowSuccessModal = false;
};

const loadAvailableToMoveOrganizations = createAsyncThunk(
  `${REDUCER_NAME}/loadAvailableToMoveOrganizations`,
  (_, { rejectWithValue, getState }) => {
    const state = getState();
    const { id, organizationId, publicId, parentOrganizationId } = selectSelectedOwner(state);
    const isApp = selectIsApplication(state);
    const parentOrganization = selectOwnersMap(state)[isApp ? organizationId : parentOrganizationId];
    const url = getDestinationOrganizationsUrl(id, isApp);
    return axios
      .get(url)
      .then(({ data }) => {
        const availableOrganizations = data.map(({ id, name }) => ({ organizationId: id, organizationName: name }));
        const selectedOrganization = {
          organizationId: isApp ? organizationId : parentOrganizationId,
          movedApplicationId: isApp ? publicId : null,
          movedOrganizationId: isApp ? null : id,
        };
        return {
          availableOrganizations: [
            { organizationId: parentOrganization.id, organizationName: parentOrganization.name },
            ...availableOrganizations,
          ],
          selectedOrganization,
        };
      })
      .catch(rejectWithValue);
  }
);

const loadAvailableToMoveOrganizationsPending = (state) => {
  state.fetchOrgs.loading = true;
  state.fetchOrgs.loadError = null;
};

const loadAvailableToMoveOrganizationsFulfilled = (state, { payload }) => {
  const { availableOrganizations, selectedOrganization } = payload;
  state.fetchOrgs.organizations = availableOrganizations;
  state.fetchOrgs.isShowNoAvailableOrgsWarning = availableOrganizations.length <= 1;
  state.fetchOrgs.loading = false;
  state.fetchOrgs.loadError = null;
  state.selectedOrganization = selectedOrganization;
};

const loadAvailableToMoveOrganizationsFailed = (state, { payload }) => {
  state.fetchOrgs.loading = false;
  state.fetchOrgs.loadError = Messages.getHttpErrorMessage(payload);
};

const setOrganization = (
  state,
  { payload: { targetParentOrganizationId, currentParentOrganizationId, movedApplicationId, movedOrganizationId } }
) => {
  const selectedOrg = state.fetchOrgs.organizations.find(
    ({ organizationId }) => organizationId === targetParentOrganizationId
  );
  state.selectedOrganization = {
    movedOrganizationId,
    movedApplicationId,
    organizationId: selectedOrg.organizationId,
    organizationName: selectedOrg.organizationName,
  };
  state.isDirty = targetParentOrganizationId !== currentParentOrganizationId;
};

const moveApplication = createAsyncThunk(
  `${REDUCER_NAME}/moveApplication`,
  ({ movedApplicationId, organizationId, organizationName }, { dispatch, rejectWithValue }) => {
    return axios
      .post(getMoveApplicationUrl(movedApplicationId, organizationId))
      .then((response) => {
        dispatch(
          rootActions.selectedOwnerParentOrganizationUpdated({
            organizationName,
            organizationId,
          })
        );
        startSaveMaskSuccessTimer(dispatch, actions.closeMoveOwnerModal).then(() => {
          dispatch(ownerSummaryActions.loadOwnerSummary());
          dispatch(actions.showSuccessModal());
          dispatch(ownerSideNavActions.load());
        });
        return response?.data?.warnings;
      })
      .catch((error) => {
        if (error.response?.status === 409 && error.response?.data?.errors?.length > 0) {
          // data.errors is an array of incompatibilities
          const incompatibilitiesError = {
            incompatibilities: error.response.data.errors,
            message: error.message,
          };
          return rejectWithValue(incompatibilitiesError);
        }
        return rejectWithValue(error);
      });
  }
);

const moveOrganization = createAsyncThunk(
  `${REDUCER_NAME}/moveOrganization`,
  ({ movedOrganizationId, organizationId }, { dispatch, rejectWithValue }) => {
    return axios
      .put(getMoveOrganizationUrl(movedOrganizationId, organizationId))
      .then((response) => {
        dispatch(rootActions.selectedOwnerParentOrganizationUpdated({ parentOrganizationId: organizationId }));
        startSaveMaskSuccessTimer(dispatch, actions.closeMoveOwnerModal).then(() => {
          dispatch(ownerSummaryActions.loadOwnerSummary());
          dispatch(actions.showSuccessModal());
          dispatch(ownerSideNavActions.load());
          dispatch(policyMonitoringActions.loadContinuousMonitoringSummaryTileInformation());
        });
        return response?.data?.warnings;
      })
      .catch(rejectWithValue);
  }
);

const moveOwnerPending = (state) => {
  state.submitMaskState = false;
  state.warnings = null;
};

const moveOwnerFulfilled = (state, { payload }) => {
  state.submitMaskState = true;
  state.submitError = null;
  state.warnings = payload;
};

const moveOwnerFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = payload;
};

const moveOwnerModal = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    openMoveOwnerModal,
    closeMoveOwnerModal,
    showSuccessModal,
    closeSuccessModal,
    setOrganization,
  },
  extraReducers: {
    [moveApplication.pending]: moveOwnerPending,
    [moveApplication.fulfilled]: moveOwnerFulfilled,
    [moveApplication.rejected]: moveOwnerFailed,
    [moveOrganization.pending]: moveOwnerPending,
    [moveOrganization.fulfilled]: moveOwnerFulfilled,
    [moveOrganization.rejected]: moveOwnerFailed,
    [loadAvailableToMoveOrganizations.pending]: loadAvailableToMoveOrganizationsPending,
    [loadAvailableToMoveOrganizations.fulfilled]: loadAvailableToMoveOrganizationsFulfilled,
    [loadAvailableToMoveOrganizations.rejected]: loadAvailableToMoveOrganizationsFailed,
  },
});

export default moveOwnerModal.reducer;

export const actions = {
  ...moveOwnerModal.actions,
  loadAvailableToMoveOrganizations,
  moveApplication,
  moveOrganization,
};
