/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { createAsyncThunk, createSlice, unwrapResult } from '@reduxjs/toolkit';
import { any, isEmpty } from 'ramda';
import {
  nxTextInputStateHelpers,
  nxFileUploadStateHelpers,
  combineValidationErrors,
} from '@sonatype/react-shared-components';
import {
  validateNonEmpty,
  validateNameCharacters,
  validateUsernameCharacters,
  validateMaxLength,
  validateDoubleWhitespace,
} from 'MainRoot/util/validationUtil';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { getAddIconUrl } from 'MainRoot/util/CLMLocation';
import { actions as ownerEditorActions } from 'MainRoot/OrgsAndPolicies/ownerEditorSlice';
import { actions as ownerSideNavActions } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSlice';
import { selectOwnerProperties, selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectIsApplication, selectIsRepository } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectOwnerById } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';
import { selectOwnerModalSlice } from './ownerModalSelectors';
import { stateGo, stateReload } from 'MainRoot/reduxUiRouter/routerActions';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { propSet } from 'MainRoot/util/jsUtil';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';
import { selectIsSbomManager, selectIsScmOnboarding } from '../../reduxUiRouter/routerSelectors';

const { initialState: rscInitialState, userInput } = nxTextInputStateHelpers;
const { initialState: rscInitialFileUploadState, userInput: userFileUploadInput } = nxFileUploadStateHelpers;

const REDUCER_NAME = `${OWNER_ACTIONS}/ownerModal`;
const NO_ICON_VALIDATION = 'No file selected';
export const iconTypes = { custom: 'custom', robot: 'robot' };

export const initialState = {
  submitError: null,
  submitMaskState: null,
  isModalOpen: false,
  isEditMode: false,
  isApplication: null,
  isDirty: false,
  ownerIconType: '',
  ownerIcon: rscInitialFileUploadState(null),
  robotHash: '',
  validationErrors: [],
  ownerName: rscInitialState('', validateNonEmpty),
  appId: rscInitialState('', validateNonEmpty),
  isUnsavedChangesModalOpen: false,
};

const appData = (name, id, currentOwner) => ({
  id: null,
  publicId: id,
  name: name,
  organizationId: currentOwner.id,
  organizationName: currentOwner.name,
  contact: null,
  isNew: true,
});

const orgData = (name, currentOwner, isRepo) => ({
  id: null,
  name: name,
  isNew: true,
  parentOrganizationId: isRepo ? null : currentOwner.id,
});

const getRandomRobotIconHash = () => Math.floor(Math.random() * 10000);

const closeModal = (state, { payload }) => {
  if (payload?.isDirty) {
    state.isUnsavedChangesModalOpen = true;
    return;
  }
  state.submitError = null;
  state.submitMaskState = null;
  state.isModalOpen = false;
  state.ownerIconType = '';
  state.isEditMode = false;
  state.isDirty = false;
  state.isApplication = null;
  state.ownerIcon = rscInitialFileUploadState(null);
  state.ownerName = rscInitialState('', validateNonEmpty);
  state.appId = rscInitialState('', validateNonEmpty);
  state.isUnsavedChangesModalOpen = false;
  state.validationErrors = [];
};

const openModal = (state, { payload }) => {
  state.isModalOpen = true;
  state.isApplication = payload.isApp;
};

const closeUnsavedChangesModal = (state) => {
  state.isUnsavedChangesModalOpen = false;
};

const openEditModal = (state, { payload }) => {
  state.isModalOpen = true;
  state.isEditMode = true;
  state.isApplication = payload.publicId ? true : false;
  state.ownerName = rscInitialState(payload.name, validateNonEmpty);
};

const requestFulfilled = (state) => {
  state.isDirty = false;
  state.submitError = null;
  state.submitMaskState = true;
};

const requestFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const computeIsDirty = (state, selectedOwner) => {
  const { ownerName, appId, ownerIconType } = state;
  // isDirty on newOwnerCreate
  if (!state.isEditMode) {
    state.isDirty = ownerName.value || appId.value ? true : !isEmpty(ownerIconType);
    return;
  }

  // isDirty on editCurrentOwner
  state.isDirty = selectedOwner.name !== ownerName.value ? true : !isEmpty(ownerIconType);
};

const setOwnerIconType = (state, { payload: { value, selectedOwner } }) => {
  state.ownerIconType = value;

  state.isDirty = !isEmpty(value) ? true : computeIsDirty(state, selectedOwner);

  if (value === iconTypes.robot && state.robotHash === '') {
    state.robotHash = getRandomRobotIconHash();
  }

  if (value === iconTypes.robot || value === '') {
    state.validationErrors = state.validationErrors.filter((e) => e !== NO_ICON_VALIDATION);

    return;
  }

  if (value === iconTypes.custom && !state.ownerIcon.files) {
    state.validationErrors = [...state.validationErrors, NO_ICON_VALIDATION];
  }
};

const updateRobotIcon = (state) => {
  state.robotHash = getRandomRobotIconHash();
};

const setCustomIcon = (state, { payload }) => {
  state.ownerIcon = userFileUploadInput(payload);

  state.validationErrors = payload
    ? state.validationErrors.filter((e) => e !== NO_ICON_VALIDATION)
    : [...state.validationErrors, NO_ICON_VALIDATION];
};

const createNewOwner = createAsyncThunk(
  `${REDUCER_NAME}/createOwner`,
  async (shouldRedirectToNewOrg, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const { ownerName, appId, isApplication, ownerIconType, robotHash, ownerIcon } = selectOwnerModalSlice(state);
    const isCurrentOwnerAnApp = selectIsApplication(state);
    const isRepo = selectIsRepository(state);
    const isSbomManager = selectIsSbomManager(state);
    let currentOwner = selectSelectedOwner(state);

    if (isCurrentOwnerAnApp) {
      currentOwner = selectOwnerById(state, currentOwner.organizationId);
    }

    const ownerToSave = isApplication
      ? appData(ownerName.trimmedValue, appId.trimmedValue, currentOwner)
      : orgData(ownerName.trimmedValue, currentOwner, isRepo);
    try {
      const updatedOwnerAction = await dispatch(
        ownerEditorActions.updateOwner({ ownerToSave, ownerType: isApplication ? 'application' : 'organization' })
      );
      const payload = unwrapResult(updatedOwnerAction);
      if (payload) {
        dispatch(
          ownerSideNavActions.updateOwnersMapWithNewEntry({
            isApp: isApplication,
            entry: isApplication ? payload.application : payload.organization,
          })
        );
      }

      if (ownerIconType !== '') {
        const iconUrlOwnerType = isApplication ? 'application' : 'organization';
        const iconUrlId = isApplication ? payload.application.id : payload.organization.id;
        const iconUrl = getAddIconUrl(iconUrlOwnerType, iconUrlId);
        const formData = new FormData();

        if (ownerIconType === iconTypes.robot) {
          formData.append('hasRobotSource', true);
          formData.append('hashcode', robotHash);
        }

        if (ownerIconType === iconTypes.custom) {
          formData.append('hasRobotSource', false);
          formData.append('file', ownerIcon.files[0]);
        }

        await axios.post(iconUrl, formData);
      }

      startSaveMaskSuccessTimer(dispatch, actions.closeModal).then(() => {
        if (isApplication) {
          dispatch(
            stateGo(`${isSbomManager ? 'sbomManager.' : ''}management.view.application`, {
              applicationPublicId: payload.application.publicId,
            })
          );
        }
        if (shouldRedirectToNewOrg) {
          dispatch(
            stateGo(isSbomManager ? 'management.view.organization' : 'scmOnboardingOrg', {
              organizationId: payload.organization.id,
            })
          );
        }
      });
    } catch (err) {
      return rejectWithValue(err);
    }
  }
);

const editCurrentOwner = createAsyncThunk(
  `${REDUCER_NAME}/editCurrentOwner`,
  async (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const currentOwner = selectSelectedOwner(state);
    const { ownerName, ownerIconType, robotHash, ownerIcon } = selectOwnerModalSlice(state);
    const isApp = selectIsApplication(state);
    const isScmOnboarding = selectIsScmOnboarding(state);
    const { ownerType } = selectOwnerProperties(state);

    try {
      if (ownerName.trimmedValue.toLowerCase() !== currentOwner.name.toLowerCase()) {
        const ownerToSave = { ...currentOwner, name: ownerName.trimmedValue };

        if (isApp && currentOwner.contact) {
          ownerToSave.contactInternalName = currentOwner.contact.internalName;
        }

        await dispatch(ownerEditorActions.updateOwner({ ownerToSave, ownerType }));
      }

      if (
        ownerType === 'repository_manager' ||
        ownerType === 'application' ||
        ownerType === 'organization' ||
        isScmOnboarding
      ) {
        const iconUrl = getAddIconUrl(ownerType, currentOwner.id);
        const formData = new FormData();

        if (ownerIconType === '') {
          const file = new File([new Blob()], '', {
            type: 'application/octet-stream',
          });
          formData.append('hasRobotSource', false);
          formData.append('file', file);
        }

        if (ownerIconType === iconTypes.robot) {
          formData.append('hasRobotSource', true);
          formData.append('hashcode', robotHash);
        }

        if (ownerIconType === iconTypes.custom) {
          formData.append('hasRobotSource', false);
          formData.append('file', ownerIcon.files[0]);
        }

        await axios.post(iconUrl, formData);
      }

      startSaveMaskSuccessTimer(dispatch, actions.closeModal).then(() => dispatch(stateReload()));
    } catch (err) {
      return rejectWithValue(err);
    }
  }
);

const duplicationAppIdValidator = (appsList, value) => {
  const exists = any(({ publicId }) => publicId?.toLowerCase() === value.toLowerCase(), appsList);

  return exists ? 'ID is already in use' : null;
};

const duplicationNameValidator = (ownersList, isEditMode, ownerName, value) => {
  const exists = isEditMode
    ? any(
        ({ name }) => name?.toLowerCase() === value.toLowerCase() && value?.toLowerCase() !== ownerName.toLowerCase(),
        ownersList
      )
    : any(({ name }) => name?.toLowerCase() === value.toLowerCase(), ownersList);

  return exists ? 'Name is already in use' : null;
};

const inputOwnerNameValidation = (appsList, orgsList, isEditMode, isApp, selectedOwner, inputValue) =>
  combineValidationErrors(
    validateNameCharacters(inputValue),
    validateNonEmpty(inputValue),
    validateDoubleWhitespace(inputValue),
    validateMaxLength(200, inputValue),
    duplicationNameValidator(isApp ? appsList : orgsList, isEditMode, selectedOwner.name, inputValue)
  );

const inputOwnerAppIdValidation = (appsList, inputValue) =>
  combineValidationErrors(
    validateUsernameCharacters(inputValue),
    validateNonEmpty(inputValue),
    validateDoubleWhitespace(inputValue),
    validateMaxLength(200, inputValue),
    duplicationAppIdValidator(appsList, inputValue)
  );

const setNewOwnerName = (state, { payload: { value, appsList, orgsList, isApp, selectedOwner } }) => {
  const validation = userInput(
    () => inputOwnerNameValidation(appsList, orgsList, state.isEditMode, isApp, selectedOwner, value.trim()),
    value
  );
  state.ownerName = validation;
  computeIsDirty(state, selectedOwner);

  if (state.ownerIconType === iconTypes.custom && !state.ownerIcon.files) {
    state.validationErrors = state.isEditMode
      ? [...validation.validationErrors, NO_ICON_VALIDATION]
      : state.isApplication
      ? [...validation.validationErrors, ...state.appId.validationErrors, NO_ICON_VALIDATION]
      : [...validation.validationErrors, NO_ICON_VALIDATION];
    return;
  }

  state.validationErrors = state.isEditMode
    ? validation.validationErrors
    : state.isApplication
    ? [...validation.validationErrors, ...state.appId.validationErrors]
    : validation.validationErrors;
};

const setNewOwnerAppId = (state, { payload: { value, appsList, selectedOwner } }) => {
  const validation = userInput(() => inputOwnerAppIdValidation(appsList, value.trim()), value);
  state.appId = validation;
  computeIsDirty(state, selectedOwner);

  if (state.ownerIconType === iconTypes.custom && !state.ownerIcon.files) {
    state.validationErrors = [...validation.validationErrors, ...state.ownerName.validationErrors, NO_ICON_VALIDATION];
    return;
  }

  state.validationErrors = [...validation.validationErrors, ...state.ownerName.validationErrors];
};

const owner = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    openModal,
    openEditModal,
    closeModal,
    setNewOwnerName,
    setNewOwnerAppId,
    setOwnerIconType,
    updateRobotIcon,
    setCustomIcon,
    closeUnsavedChangesModal,
  },
  extraReducers: {
    [createNewOwner.pending]: propSet('submitMaskState', false),
    [createNewOwner.fulfilled]: requestFulfilled,
    [createNewOwner.rejected]: requestFailed,
    [editCurrentOwner.pending]: propSet('submitMaskState', false),
    [editCurrentOwner.fulfilled]: requestFulfilled,
    [editCurrentOwner.rejected]: requestFailed,
  },
});

export default owner.reducer;
export const actions = {
  ...owner.actions,
  createNewOwner,
  editCurrentOwner,
};
