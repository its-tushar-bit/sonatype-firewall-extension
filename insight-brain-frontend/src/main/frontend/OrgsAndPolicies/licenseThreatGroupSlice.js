/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createAsyncThunk, createSlice, unwrapResult } from '@reduxjs/toolkit';
import axios from 'axios';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { deriveEditRoute, deriveViewRoute } from 'MainRoot/OrgsAndPolicies/utility/util';
import { pathSet, propSet, eqValues } from 'MainRoot/util/jsUtil';
import {
  getAllLicensesUrl,
  getLicenseGroupsUrl,
  getApplicableLicenseGroupsUrl,
  getDeleteLicenseGroupUrl,
  getLicenseGroupLicensesUrl,
} from 'MainRoot/util/CLMLocation';
import {
  isNil,
  isEmpty,
  reject,
  any,
  map,
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
  compose,
} from 'ramda';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
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
import {
  validateNonEmpty,
  combineValidators,
  validateForm,
  validateNameCharacters,
} from 'MainRoot/util/validationUtil';
import { propSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';

const { initialState: initUserInput, userInput } = nxTextInputStateHelpers;

const REDUCER_NAME = 'licenseThreatGroup';

export const initialState = {
  //control
  loadError: null,
  submitError: null,
  errorState: null,
  loading: false,
  isDirty: false,
  validationError: null,
  //information for tile display
  applicableLicenseThreatGroups: [],
  //information for create,update,delete form
  availableLicenses: [],
  currentLicenseThreatGroup: null,
  nextLicenseThreatGroup: null,
  submitMaskState: null,
  dirtyLTG: {
    id: null,
    name: initUserInput(''),
    threatLevel: 5,
    licenses: [],
    licenseIds: [],
  },
  siblings: [],
  deleteError: null,
  deleteMaskState: null,
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
  (_, { getState, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerProperties(state);
    return axios.get(getLicenseGroupsUrl(ownerType, ownerId)).then(prop('data')).catch(rejectWithValue);
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
  (_, { getState, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerProperties(state);

    return axios
      .get(getApplicableLicenseGroupsUrl(ownerType, ownerId))
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
  (_, { rejectWithValue, dispatch }) => {
    return dispatch(loadApplicableLicenseThreatGroupsByOwner()).then(unwrapResult).catch(rejectWithValue);
  }
);

const loadLicensesByLicenseThreatGroup = createAsyncThunk(
  `${REDUCER_NAME}/loadLicensesByLicenseThreatGroup`,
  ({ licenseThreatGroupId }, { getState, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerProperties(state);

    return axios
      .get(getApplicableLicenseGroupsUrl(ownerType, ownerId, licenseThreatGroupId))
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
  (_, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const isEditMode = selectLicenseThreatGroupIsEditMode(state);
    let availableLicenses,
      currentLicenseThreatGroup = null,
      nextLicenseThreatGroup = null,
      siblings = [],
      dirtyLTG = {
        name: initUserInput(''),
        threatLevel: 5,
        licenses: [],
        licenseIds: [],
      };

    dispatch(actions.resetIsDirty());

    availableLicenses = selectAvailableLicenses(state);
    const loadAllLicensesIfNeeded = isEmpty(availableLicenses) ? dispatch(loadAllLicenses()) : Promise.resolve({});

    return Promise.all([loadAllLicensesIfNeeded, dispatch(loadApplicableLicenseThreatGroups())])
      .then((results) => {
        const [licensesPayload, licenseThreatGroupsPayload] = results;

        if (isEmpty(availableLicenses)) {
          const licenses = unwrapResult(licensesPayload);
          availableLicenses = licenses.map((item) => ({
            id: item.id,
            displayName: getFullDisplayName(item),
          }));
        }

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
          dirtyLTG.name = initUserInput(currentLicenseThreatGroup.name);
          dirtyLTG.licenseIds = map(({ licenseId }) => licenseId, currentLicenseThreatGroup.licenses);
        }

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

const saveLicenseThreatGroupRequested = (state) => {
  state.submitError = null;
  state.submitMaskState = false;
};

const saveLicenseThreatGroupFulfilled = (state, { payload }) => {
  state.submitError = null;
  state.isDirty = false;
  state.submitMaskState = true;

  if (payload.isEditMode) {
    const index = findIndex(propEq('id', payload.licenseThreatGroup.id), state.siblings);
    state.siblings[index] = payload.licenseThreatGroup;
    state.currentLicenseThreatGroup = payload.licenseThreatGroup;
    state.dirtyLTG = clone(payload.licenseThreatGroup);
    state.dirtyLTG.name = initUserInput(state.dirtyLTG.name);
    state.dirtyLTG.licenseIds = payload.licenseIds;
  } else {
    state.dirtyLTG = {
      id: null,
      name: initUserInput(''),
      threatLevel: 5,
      licenses: [],
      licenseIds: [],
    };
    state.currentLicenseThreatGroup = initialState.currentLicenseThreatGroup;
    state.siblings.push(payload.licenseThreatGroup);
  }
};

const saveLicenseThreatGroupFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const saveLicenseThreatGroup = createAsyncThunk(
  `${REDUCER_NAME}/saveLicenseThreatGroup`,
  async (_, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const { id, name, threatLevel, licenseIds } = selectDirtyLicenseThreatGroup(state);

    try {
      const newLTGPayload = await dispatch(
        createUpdateLicenseThreatGroup({ name: name.trimmedValue, threatLevel, id })
      );
      const newLTG = unwrapResult(newLTGPayload);

      if (newLTG) {
        const ltgId = prop('id', newLTG);
        const updateLicensesPayload = await dispatch(
          updateLicenseThreatGroupLicenses({
            licenseThreatGroupId: ltgId,
            licenses: licenseIds,
          })
        );

        const updatedLicenses = unwrapResult(updateLicensesPayload);

        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone);

        return {
          licenseThreatGroup: {
            ...newLTG,
            licenses: updatedLicenses,
          },
          isEditMode: selectLicenseThreatGroupIsEditMode(state),
          licenseIds: map(({ licenseId }) => licenseId, updatedLicenses),
        };
      }
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const createUpdateLicenseThreatGroup = createAsyncThunk(
  `${REDUCER_NAME}/createUpdateLicenseThreatGroup`,
  ({ name, threatLevel, id = null }, { getState, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerProperties(state);
    const isEditMode = selectLicenseThreatGroupIsEditMode(state);
    const payload = {
      name: name,
      threatLevel: threatLevel,
    };

    if (isEditMode) {
      payload.id = id;
      payload.ownerId = ownerId;
    }

    return axios[isEditMode ? 'put' : 'post'](getLicenseGroupsUrl(ownerType, ownerId), payload)
      .then(prop('data'))
      .catch(rejectWithValue);
  }
);

const updateLicenseThreatGroupLicenses = createAsyncThunk(
  `${REDUCER_NAME}/updateLicenseThreatGroupLicenses`,
  ({ licenseThreatGroupId, licenses = [] }, { getState, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerProperties(state);
    return axios
      .put(getLicenseGroupLicensesUrl(ownerType, ownerId, licenseThreatGroupId), licenses)
      .then(prop('data'))
      .catch(rejectWithValue);
  }
);

const removeLicenseThreatGroupRequested = (state) => {
  state.deleteError = null;
  state.deleteMaskState = false;
};

const removeLicenseThreatGroupFulfilled = (state, { payload }) => {
  state.isDirty = false;
  state.deleteMaskState = true;
  state.deleteError = null;
  state.currentLicenseThreatGroup = initialState.currentLicenseThreatGroup;
  state.dirtyLTG = initialState.dirtyLTG;
  state.siblings = reject(propEq('id', payload.id))(state.siblings);
};

const removeLicenseThreatGroupFailed = (state, { payload }) => {
  state.deleteError = Messages.getHttpErrorMessage(payload);
  state.deleteMaskState = null;
};

const clearDeleteError = (state) => {
  state.deleteError = null;
};

const removeLicenseThreatGroup = createAsyncThunk(
  `${REDUCER_NAME}/deleteLicenseThreatGroup`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerProperties(state);
    const ltg = selectCurrentLicenseThreatGroup(state);
    const nextLTG = selectNextLicenseThreatGroup(state);
    const isOrganization = selectIsOrganization(state);

    return axios
      .delete(getDeleteLicenseGroupUrl(ownerType, ownerId, ltg.id))
      .then(() => {
        dispatch(actions.resetIsDirty());

        startSaveMaskSuccessTimer(dispatch, actions.deleteMaskTimerDone);

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

const computeLicensesIsDirty = (currentLicenseThreatGroup, dirtyLTG) => {
  const dirtyLicensesId = dirtyLTG.licenseIds;
  const originalLicensesId = !isNil(currentLicenseThreatGroup)
    ? map((license) => license.licenseId, currentLicenseThreatGroup.licenses)
    : [];

  if (originalLicensesId.length !== dirtyLicensesId.length) return true;

  return !eqValues(originalLicensesId, dirtyLicensesId);
};

const DEFAULT_THREAT_LEVEL = 5;
const computeIsDirty = (state, isLicensesListModified) => {
  const { currentLicenseThreatGroup, dirtyLTG } = state;

  const isLicensePickerDirty = isLicensesListModified
    ? computeLicensesIsDirty(currentLicenseThreatGroup, dirtyLTG)
    : false;

  if (isNil(currentLicenseThreatGroup)) {
    const validatableFieldsAreDirty =
      !isEmpty(dirtyLTG['name'].trimmedValue) || dirtyLTG['threatLevel'] !== DEFAULT_THREAT_LEVEL;
    return propSet('isDirty', validatableFieldsAreDirty || isLicensePickerDirty, state);
  }

  const isNameDirty = dirtyLTG['name'].trimmedValue !== currentLicenseThreatGroup['name'];
  const isThreatLevelDirty = dirtyLTG['threatLevel'] !== currentLicenseThreatGroup['threatLevel'];

  return propSet('isDirty', isNameDirty || isThreatLevelDirty || isLicensePickerDirty, state);
};

const getFullDisplayName = ({ shortDisplayName, longDisplayName }) => `(${shortDisplayName}) ${longDisplayName}`;

const setInput = curryN(3, function setInput(fieldName, isLicensesListModified, state, { payload }) {
  return updatedComputedProps(pathSet(['dirtyLTG', fieldName], payload, state), isLicensesListModified);
});

const computeValidationError = (state) => {
  const validationError = validateForm([state.dirtyLTG.name]);
  return propSetConst(['validationError'], validationError, state);
};

const updatedComputedProps = compose(computeValidationError, computeIsDirty);

const setLicenseThreatGroupName = (state, { payload }) => {
  const duplicationValidator = (value) => {
    const exists = any(
      (item) => item.name.toLowerCase() === value.toLowerCase() && item.id !== state.currentLicenseThreatGroup?.id,
      state.siblings
    );

    return exists ? 'Name is already in use' : null;
  };

  const validator = combineValidators([validateNonEmpty, validateNameCharacters, duplicationValidator]);
  return updatedComputedProps(pathSet(['dirtyLTG', 'name'], userInput(validator, payload), state), false);
};

const setPickedLicenses = (state, { payload }) => {
  return updatedComputedProps(
    {
      ...state,
      dirtyLTG: {
        ...state.dirtyLTG,
        licenseIds: payload,
      },
    },
    true
  );
};

const licenseThreatGroupsSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetIsDirty: propSet('isDirty', false),
    clearDeleteError,
    setLicenseThreatGroupName,
    setLicenseThreatGroupThreatLevel: setInput('threatLevel', false),
    setPickedLicenses,
    saveMaskTimerDone: propSet('submitMaskState', null),
    deleteMaskTimerDone: propSet('deleteMaskState', null),
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

    [saveLicenseThreatGroup.pending]: saveLicenseThreatGroupRequested,
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
