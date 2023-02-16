/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import axios from 'axios';
import { getDestinationOrganizationsUrl, getMoveApplicationUrl } from 'MainRoot/util/CLMLocation';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';
import { actions as applicationsActions } from 'MainRoot/OrgsAndPolicies/applicationsSlice';
import { actions as ownerSideNavActions } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSlice';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';
const REDUCER_NAME = `${OWNER_ACTIONS}/moveApplication`;

export const initialState = {
  isMoveAppModalOpen: false,
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

const openMoveAppModal = (state) => {
  state.isMoveAppModalOpen = true;
};

const closeMoveAppModal = (state) => {
  state.isMoveAppModalOpen = false;
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
    const { id, organizationId, organizationName } = selectSelectedOwner(state);
    return axios
      .get(getDestinationOrganizationsUrl(id))
      .then(({ data }) => {
        const availableOrganizations = data.map(({ id, name }) => ({ organizationId: id, organizationName: name }));
        return [{ organizationId, organizationName }, ...availableOrganizations];
      })
      .catch(rejectWithValue);
  }
);

const loadAvailableToMoveOrganizationsPending = (state) => {
  state.fetchOrgs.loading = true;
  state.fetchOrgs.loadError = null;
};

const loadAvailableToMoveOrganizationsFulfilled = (state, { payload }) => {
  state.fetchOrgs.organizations = payload;
  state.fetchOrgs.isShowNoAvailableOrgsWarning = payload.length <= 1;
  state.fetchOrgs.loading = false;
  state.fetchOrgs.loadError = null;
};

const loadAvailableToMoveOrganizationsFailed = (state, { payload }) => {
  state.fetchOrgs.loading = false;
  state.fetchOrgs.loadError = Messages.getHttpErrorMessage(payload);
};

const setOrganization = (state, { payload: { selectedOrganizationId, applicationId, currentParentOrganization } }) => {
  const selectedOrg = state.fetchOrgs.organizations.find(
    ({ organizationId }) => organizationId === selectedOrganizationId
  );
  const { organizationId, organizationName } = selectedOrg;
  state.selectedOrganization = {
    applicationId: applicationId,
    organizationId,
    organizationName,
  };
  state.isDirty = selectedOrganizationId !== currentParentOrganization;
};

const moveApplication = createAsyncThunk(
  `${REDUCER_NAME}/moveApplication`,
  ({ applicationId, organizationId, organizationName }, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const currentOwner = selectSelectedOwner(state);

    return axios
      .post(getMoveApplicationUrl(applicationId, organizationId))
      .then((response) => {
        return dispatch(applicationsActions.loadApplications(true)).then(() => {
          dispatch(rootActions.selectedOwnerParentOrganizationUpdated({ organizationName, organizationId }));
          startSaveMaskSuccessTimer(dispatch, actions.closeMoveAppModal).then(() => {
            dispatch(actions.showSuccessModal());
            dispatch(
              ownerSideNavActions.moveApplication({
                currentOwner,
                newParentId: organizationId,
              })
            );
          });
          return response?.data?.warnings;
        });
      })
      .catch((error) => {
        if (error.response?.status === 409 && error.response?.data?.errors?.length) {
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

const moveApplicationPending = (state) => {
  state.submitMaskState = false;
  state.warnings = null;
};

const moveApplicationFulfilled = (state, { payload }) => {
  state.submitMaskState = true;
  state.submitError = null;
  state.warnings = payload;
};

const moveApplicationFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = payload;
};

const moveApplicationModal = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    openMoveAppModal,
    closeMoveAppModal,
    showSuccessModal,
    closeSuccessModal,
    setOrganization,
  },
  extraReducers: {
    [moveApplication.pending]: moveApplicationPending,
    [moveApplication.fulfilled]: moveApplicationFulfilled,
    [moveApplication.rejected]: moveApplicationFailed,
    [loadAvailableToMoveOrganizations.pending]: loadAvailableToMoveOrganizationsPending,
    [loadAvailableToMoveOrganizations.fulfilled]: loadAvailableToMoveOrganizationsFulfilled,
    [loadAvailableToMoveOrganizations.rejected]: loadAvailableToMoveOrganizationsFailed,
  },
});

export default moveApplicationModal.reducer;

export const actions = {
  ...moveApplicationModal.actions,
  loadAvailableToMoveOrganizations,
  moveApplication,
};
