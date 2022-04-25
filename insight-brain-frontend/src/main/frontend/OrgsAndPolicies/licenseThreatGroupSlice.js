/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createAsyncThunk, createSlice, unwrapResult } from '@reduxjs/toolkit';
import axios from 'axios';
import { Messages } from 'MainRoot/util/CommonServices';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { deriveEditRoute, deriveViewRoute } from 'MainRoot/OrgsAndPolicies/utility/util';
import { pathSet, propSet, eqValues } from 'MainRoot/util/jsUtil';
import { getAllLicensesUrl } from 'MainRoot/util/CLMLocation';
import {
  getLicenseGroupsUrl,
  getApplicableLicenseGroupsUrl,
  getDeleteLicenseGroupUrl,
  getLicenseGroupLicensesUrl,
} from 'MainRoot/util/CLMContextLocation';
import {
  isNil,
  isEmpty,
  reject,
  any,
  map,
  filter,
  propEq,
  prop,
  clone,
  sortWith,
  descend,
  findIndex,
  concat,
  curryN,
  forEachObjIndexed,
  forEach,
} from 'ramda';

import { selectRouterSlice, selectIsOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectOwnerProperties } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectNextLicenseThreatGroup,
  selectCurrentLicenseThreatGroup,
  selectDirtyLicenseThreatGroup,
  selectLicenseThreatGroupIsEditMode,
  selectLicenseThreatGroupId,
  selectAvailableLicenses,
} from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSelectors';

const REDUCER_NAME = 'licenseThreatGroup';

export const initialState = {
  //control
  loadError: null,
  submitError: null,
  errorState: null,
  deleting: false,
  success: null,
  loading: false,
  isDirty: false,
  //information for tile display
  applicableLicenseThreatGroups: [],
  //information for create,update,delete form
  availableLicenses: [],
  currentLicenseThreatGroup: null,
  nextLicenseThreatGroup: null,
  dirtyLTG: {
    id: null,
    name: null,
    threatLevel: 5,
    licenses: [],
    pickedLicenses: [],
  },
  siblings: [],
};

const loadLicenseThreatGroupsRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};
const loadLicenseThreatGroupsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.licenseGroup = payload;
};
const loadLicenseThreatGroupsFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadLicenseThreatGroups = createAsyncThunk(
  `${REDUCER_NAME}/loadLicenseThreatGroups`,
  ($state, { rejectWithValue }) => {
    return axios.get(getLicenseGroupsUrl($state)).then(prop('data')).catch(rejectWithValue);
  }
);

const loadApplicableLicenseThreatGroupsRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};
const loadApplicableLicenseThreatGroupsFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.applicableLicenseThreatGroups = payload.licenseThreatGroupsByOwner;
};
const loadApplicableLicenseThreatGroupsFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadApplicableLicenseThreatGroupsByOwner = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicableLicenseThreatGroupsByOwner`,
  ($state, { rejectWithValue }) => {
    return axios
      .get(getApplicableLicenseGroupsUrl($state))
      .then(({ data }) => {
        const applicableLicenseThreatGroups = prop('licenseThreatGroupsByOwner', data);

        const isInherited = (applicableLicenseGroup, index) => {
          applicableLicenseGroup.inherited = index > 0;
        };
        const applicableLTGs = forEachObjIndexed(isInherited, applicableLicenseThreatGroups);

        return {
          licenseThreatGroupsByOwner: applicableLTGs,
        };
      })
      .catch(rejectWithValue);
  }
);

const loadApplicableLicenseThreatGroups = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicableLicenseThreatGroups`,
  ($state, { rejectWithValue, dispatch }) => {
    return dispatch(loadApplicableLicenseThreatGroupsByOwner($state)).then(unwrapResult).catch(rejectWithValue);
  }
);

const loadLicensesByLicenseThreatGroup = createAsyncThunk(
  `${REDUCER_NAME}/loadLicensesByLicenseThreatGroup`,
  ({ $state, licenseThreatGroupId }, { rejectWithValue }) => {
    return axios
      .get(getApplicableLicenseGroupsUrl($state, licenseThreatGroupId))
      .then((result) => result)
      .catch(rejectWithValue);
  }
);

const loadLicenseThreatGroupEditorRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadLicenseThreatGroupEditorFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.availableLicenses = payload.availableLicenses;
  state.currentLicenseThreatGroup = payload.currentLicenseThreatGroup;
  state.nextLicenseThreatGroup = payload.nextLicenseThreatGroup;
  state.siblings = payload.siblings;
  state.dirtyLTG = payload.dirtyLTG;
};

const loadLicenseThreatGroupEditorFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const loadLicenseThreatGroupEditor = createAsyncThunk(
  `${REDUCER_NAME}/loadLicenseThreatGroupEditor`,
  ($state, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const isEditMode = selectLicenseThreatGroupIsEditMode(state);
    let availableLicenses,
      dirtyPickedLicenses = null,
      currentLicenseThreatGroup = null,
      nextLicenseThreatGroup = null,
      siblings = [],
      dirtyLTG = {
        name: null,
        threatLevel: 5,
        licenses: [],
        pickedLicenses: [],
      };

    dispatch(actions.resetIsDirty());

    availableLicenses = selectAvailableLicenses(state);
    const loadAllLicensesIfNeeded = isEmpty(availableLicenses) ? dispatch(loadAllLicenses()) : Promise.resolve({});

    return Promise.all([loadAllLicensesIfNeeded, dispatch(loadApplicableLicenseThreatGroups($state))])
      .then((results) => {
        const [licensesPayload, licenseThreatGroupsPayload] = results;

        if (isEmpty(availableLicenses)) {
          const licenses = unwrapResult(licensesPayload);
          availableLicenses = licenses.map((item, index) => ({
            ...item,
            fullDisplayName: getFullDisplayName(item),
            picked: false,
            index: index,
          }));
        }
        dirtyPickedLicenses = [...availableLicenses];

        const licenseThreatGroups = unwrapResult(licenseThreatGroupsPayload);
        siblings = getSiblings(licenseThreatGroups);

        if (isEditMode) {
          const licenseThreatGroupId = selectLicenseThreatGroupId(state);
          const licenseThreatGroupsByOwner = prop('licenseThreatGroupsByOwner', licenseThreatGroups);
          const response = getCurrentAndNextLtg(licenseThreatGroupsByOwner, licenseThreatGroupId);

          if (isEmpty(response)) rejectWithValue('Unable to locate License Threat Group.');
          currentLicenseThreatGroup = prop('currentLicenseThreatGroup', response);
          nextLicenseThreatGroup = prop('nextLicenseThreatGroup', response);

          dirtyLTG = clone(currentLicenseThreatGroup);
          updateLicenseListWithPickedLicenses(dirtyPickedLicenses, currentLicenseThreatGroup.licenses);
        }
        dirtyLTG.pickedLicenses = dirtyPickedLicenses;

        return {
          availableLicenses,
          currentLicenseThreatGroup,
          nextLicenseThreatGroup,
          siblings,
          dirtyLTG,
        };
      })
      .catch(rejectWithValue);
  }
);

const getSiblings = (licenseThreatGroups) => {
  let siblings = [];
  const mapLicenseThreatGroup = (owner) => {
    const ownerLTGs = prop('licenseThreatGroups', owner);
    siblings = concat(siblings, ownerLTGs);
  };
  const licenseThreatGroupsByOwner = prop('licenseThreatGroupsByOwner', licenseThreatGroups);
  forEach(mapLicenseThreatGroup, licenseThreatGroupsByOwner);

  const sortByThreatLevel = sortWith([descend(prop('threatLevel')), descend(prop('name'))]);
  return sortByThreatLevel(siblings);
};

const getCurrentAndNextLtg = (licenseThreatGroupsByOwner, licenseThreatGroupId) => {
  let response;
  for (let owner of licenseThreatGroupsByOwner) {
    const ownerLTGs = prop('licenseThreatGroups', owner);
    response = findCurrentAndNextLtg(licenseThreatGroupId, ownerLTGs);
    if (!isEmpty(response)) return response;
  }
  return {};
};

const findCurrentAndNextLtg = (licenseThreatGroupId, ltgs) => {
  const index = findIndex(propEq('id', licenseThreatGroupId))(ltgs);
  if (index > -1) {
    const currentLicenseThreatGroup = ltgs[index];
    const nextLicenseThreatGroup = ltgs[index + 1] || ltgs[index - 1];
    return {
      currentLicenseThreatGroup,
      nextLicenseThreatGroup,
    };
  }
  return {};
};

const loadAllLicenses = createAsyncThunk(`${REDUCER_NAME}/loadAllLicenses`, (_, { rejectWithValue }) => {
  return axios.get(getAllLicensesUrl()).then(prop('data')).catch(rejectWithValue);
});

const saveLicenseThreatGroupFulfilled = (state, { payload }) => {
  state.submitError = null;
  state.isDirty = false;

  if (payload.isEditMode) {
    const index = findIndex(propEq('id', payload.licenseThreatGroup.id), state.siblings);
    state.siblings[index] = payload.licenseThreatGroup;
    state.currentLicenseThreatGroup = payload.licenseThreatGroup;
    state.dirtyLTG = payload.licenseThreatGroup;
  } else {
    state.dirtyLTG = {
      id: null,
      name: null,
      threatLevel: 5,
      licenses: [],
      pickedLicenses: [...state.availableLicenses],
    };
    state.currentLicenseThreatGroup = initialState.currentLicenseThreatGroup;
    state.siblings.push(payload.licenseThreatGroup);
  }
};

const saveLicenseThreatGroupFailed = (state, { payload }) => {
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const saveLicenseThreatGroup = createAsyncThunk(
  `${REDUCER_NAME}/saveLicenseThreatGroup`,
  async ({ setPristine, $state }, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const { id, name, threatLevel, pickedLicenses } = selectDirtyLicenseThreatGroup(state);

    try {
      const newLTGPayload = await dispatch(createUpdateLicenseThreatGroup({ $state, name, threatLevel, id }));
      const newLTG = unwrapResult(newLTGPayload);

      if (newLTG) {
        const updateLTGLicensesPayload = pickedLicenses
          .filter((license) => license.picked)
          .map((license) => license.id);
        const updateLicensesPayload = await dispatch(
          updateLicenseThreatGroupLicenses({
            $state: $state,
            licenseThreatGroupId: newLTG.id,
            licenses: updateLTGLicensesPayload,
          })
        );

        const updatedLicenses = unwrapResult(updateLicensesPayload);
        let pickedLicensesList = [...selectAvailableLicenses(state)];
        updateLicenseListWithPickedLicenses(pickedLicensesList, updatedLicenses);
        newLTG.pickedLicenses = pickedLicensesList;

        setPristine();
        return {
          licenseThreatGroup: newLTG,
          isEditMode: selectLicenseThreatGroupIsEditMode(state),
        };
      }
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const updateLicenseListWithPickedLicenses = (licensesList, pickedLicenses) => {
  const findId = (id) => findIndex(propEq('id', id));
  const updatePickedLicensesList = (pickedLicense) => {
    const id = pickedLicense.licenseId;
    const licenseIndex = findId(id)(licensesList);
    if (licenseIndex > -1) {
      const element = {
        ...licensesList[licenseIndex],
        picked: true,
      };
      licensesList[licenseIndex] = element;
    }
  };
  forEach(updatePickedLicensesList, pickedLicenses);
};

const createUpdateLicenseThreatGroup = createAsyncThunk(
  `${REDUCER_NAME}/createUpdateLicenseThreatGroup`,
  ({ $state, name, threatLevel, id = null }, { getState, rejectWithValue }) => {
    const state = getState();
    const { ownerId } = selectOwnerProperties(state);
    const isEditMode = selectLicenseThreatGroupIsEditMode(state);
    const payload = {
      name: name,
      threatLevel: threatLevel,
    };

    if (isEditMode) {
      payload.id = id;
      payload.ownerId = ownerId;
    }

    return axios[isEditMode ? 'put' : 'post'](getLicenseGroupsUrl($state), payload)
      .then(prop('data'))
      .catch(rejectWithValue);
  }
);

const updateLicenseThreatGroupLicenses = createAsyncThunk(
  `${REDUCER_NAME}/updateLicenseThreatGroupLicenses`,
  ({ $state, licenseThreatGroupId, licenses = [] }, { rejectWithValue }) => {
    return axios
      .put(getLicenseGroupLicensesUrl($state, licenseThreatGroupId), licenses)
      .then(prop('data'))
      .catch(rejectWithValue);
  }
);

const removeLicenseThreatGroupRequested = (state) => {
  state.deleting = true;
  state.errorState = null;
};

const removeLicenseThreatGroupFulfilled = (state, { payload }) => {
  state.isDirty = false;
  state.success = true;
  state.deleting = null;
  state.errorState = null;
  state.currentLicenseThreatGroup = initialState.currentLicenseThreatGroup;
  state.dirtyLTG = initialState.dirtyLTG;
  state.siblings = reject(propEq('id', payload.id))(state.siblings);
};

const removeLicenseThreatGroupFailed = (state, { payload }) => {
  state.deleting = false;
  state.errorState = Messages.getHttpErrorMessage(payload);
};

const resetDeleteModalState = (state) => {
  state.deleting = null;
  state.success = null;
  state.errorState = null;
};

const removeLicenseThreatGroup = createAsyncThunk(
  `${REDUCER_NAME}/deleteLicenseThreatGroup`,
  ($state, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const ltg = selectCurrentLicenseThreatGroup(state);
    const nextLTG = selectNextLicenseThreatGroup(state);
    const isOrganization = selectIsOrganization(state);

    return axios
      .delete(getDeleteLicenseGroupUrl($state, ltg.id))
      .then(() => {
        dispatch(actions.resetIsDirty());

        if (isOrganization) {
          dispatch(goToCreateLTG());
        } else {
          if (nextLTG) {
            dispatch(goToNextLTG());
          } else {
            dispatch(goToViewManagement());
          }
        }

        return {
          id: ltg.id,
        };
      })
      .catch(rejectWithValue);
  }
);

const goToCreateLTG = createAsyncThunk(`${REDUCER_NAME}/goToCreateLTG`, (_, { getState, dispatch }) => {
  const router = selectRouterSlice(getState());
  const { to, params } = deriveEditRoute(router, 'create-license-threat-group');

  dispatch(stateGo(to, params));
});

const goToEditLTG = createAsyncThunk(`${REDUCER_NAME}/goToEditLTG`, (licenseThreatGroupId, { getState, dispatch }) => {
  const router = selectRouterSlice(getState());
  const { to, params } = deriveEditRoute(router, 'edit-license-threat-group', { licenseThreatGroupId });

  dispatch(stateGo(to, params));
});

const goToNextLTG = createAsyncThunk(`${REDUCER_NAME}/goToNextLTG`, (_, { getState, dispatch }) => {
  const state = getState();
  const router = selectRouterSlice(state);
  const nextLTG = selectNextLicenseThreatGroup(state);
  const { to, params } = deriveEditRoute(router, 'edit-license-threat-group', {
    licenseThreatGroupId: nextLTG.id,
  });

  dispatch(stateGo(to, params));
});

const goToViewManagement = createAsyncThunk(
  `${REDUCER_NAME}/goToViewManagementFromLTG`,
  (_, { getState, dispatch }) => {
    const state = getState();
    const router = selectRouterSlice(state);
    const { to, params } = deriveViewRoute(router, _);

    dispatch(stateGo(to, params));
  }
);

const computePickedLicensesIsDirty = (currentLicenseThreatGroup, dirtyLTG) => {
  const getLicensesId = (license) => license.licenseId;
  const getPickedLicenseId = (pickedLicense) => pickedLicense.id;
  const isPicked = (pickedLicense) => pickedLicense.picked;

  const originalLicensesId = !isNil(currentLicenseThreatGroup)
    ? map(getLicensesId, currentLicenseThreatGroup.licenses)
    : [];
  const dirtyLicensesId = map(getPickedLicenseId, filter(isPicked, dirtyLTG.pickedLicenses));

  if (originalLicensesId.length !== dirtyLicensesId.length) return true;

  return !eqValues(originalLicensesId, dirtyLicensesId);
};

const computeIsDirty = (state, isLicensesListModified) => {
  const { currentLicenseThreatGroup, dirtyLTG } = state;
  const validatableFields = ['name', 'threatLevel'];
  const isLicensePickerDirty = isLicensesListModified
    ? computePickedLicensesIsDirty(currentLicenseThreatGroup, dirtyLTG)
    : false;

  const isDirty = isNil(currentLicenseThreatGroup)
    ? any((propertyName) => !isEmpty(dirtyLTG[propertyName]), validatableFields)
    : any((propertyName) => dirtyLTG[propertyName] !== currentLicenseThreatGroup[propertyName], validatableFields) ||
      isLicensePickerDirty;

  return propSet('isDirty', isDirty, state);
};

const getFullDisplayName = ({ shortDisplayName, longDisplayName }) => `(${shortDisplayName}) ${longDisplayName}`;

const setInput = curryN(3, function setInput(fieldName, isLicensesListModified, state, { payload }) {
  return computeIsDirty(pathSet(['dirtyLTG', fieldName], payload, state), isLicensesListModified);
});

const licenseThreatGroupsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetIsDirty: propSet('isDirty', false),
    resetDeleteModalState,
    setLicenseThreatGroupName: setInput('name', false),
    setLicenseThreatGroupThreatLevel: setInput('threatLevel', false),
    setPickedLicenses: setInput('pickedLicenses', true),
  },
  extraReducers: {
    [loadLicenseThreatGroups.pending]: loadLicenseThreatGroupsRequested,
    [loadLicenseThreatGroups.fulfilled]: loadLicenseThreatGroupsFulfilled,
    [loadLicenseThreatGroups.rejected]: loadLicenseThreatGroupsFailed,

    [loadApplicableLicenseThreatGroups.pending]: loadApplicableLicenseThreatGroupsRequested,
    [loadApplicableLicenseThreatGroups.fulfilled]: loadApplicableLicenseThreatGroupsFulfilled,
    [loadApplicableLicenseThreatGroups.rejected]: loadApplicableLicenseThreatGroupsFailed,

    [loadLicensesByLicenseThreatGroup.pending]: loadApplicableLicenseThreatGroupsRequested,
    [loadLicensesByLicenseThreatGroup.fulfilled]: loadApplicableLicenseThreatGroupsFulfilled,
    [loadLicensesByLicenseThreatGroup.rejected]: loadApplicableLicenseThreatGroupsFailed,

    [loadLicenseThreatGroupEditor.pending]: loadLicenseThreatGroupEditorRequested,
    [loadLicenseThreatGroupEditor.fulfilled]: loadLicenseThreatGroupEditorFulfilled,
    [loadLicenseThreatGroupEditor.rejected]: loadLicenseThreatGroupEditorFailed,

    [removeLicenseThreatGroup.pending]: removeLicenseThreatGroupRequested,
    [removeLicenseThreatGroup.fulfilled]: removeLicenseThreatGroupFulfilled,
    [removeLicenseThreatGroup.rejected]: removeLicenseThreatGroupFailed,

    [saveLicenseThreatGroup.pending]: propSet('submitError', null),
    [saveLicenseThreatGroup.fulfilled]: saveLicenseThreatGroupFulfilled,
    [saveLicenseThreatGroup.rejected]: saveLicenseThreatGroupFailed,
  },
});

export default licenseThreatGroupsSlice.reducer;
export const actions = {
  ...licenseThreatGroupsSlice.actions,
  loadLicenseThreatGroups,
  loadApplicableLicenseThreatGroups,
  loadLicensesByLicenseThreatGroup,
  loadLicenseThreatGroupEditor,
  loadAllLicenses,
  removeLicenseThreatGroup,
  saveLicenseThreatGroup,
  goToCreateLTG,
  goToEditLTG,
  goToNextLTG,
  goToViewManagement,
  computeIsDirty,
};
