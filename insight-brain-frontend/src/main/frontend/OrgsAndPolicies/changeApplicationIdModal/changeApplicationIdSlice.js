/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { any } from 'ramda';
import { nxTextInputStateHelpers, combineValidationErrors } from '@sonatype/react-shared-components';
import { validateNonEmpty, validateUsernameCharacters, validateMaxLength } from 'MainRoot/util/validationUtil';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { actions as ownerEditorActions } from 'MainRoot/OrgsAndPolicies/ownerEditorSlice';
import { actions as ownerSideNavActions } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSlice';
import { selectOwnerProperties, selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectChangeApplicationIdSlice } from './changeApplicationIdSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { propSet } from 'MainRoot/util/jsUtil';
import { selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';

const { initialState: rscInitialState, userInput } = nxTextInputStateHelpers;

const REDUCER_NAME = 'ownerActions/changeApplicationId';

export const initialState = {
  submitError: null,
  submitMaskState: null,
  isModalOpen: false,
  newPublicId: rscInitialState('', validateNonEmpty),
};

const closeModal = (state) => {
  state.submitError = null;
  state.submitMaskState = null;
  state.isModalOpen = false;
  state.newPublicId = rscInitialState('', validateNonEmpty);
};

const changeApplicationIdFulfilled = (state) => {
  state.submitError = null;
  state.submitMaskState = true;
};

const changeApplicationIdFailed = (state, { payload }) => {
  state.submitMaskState = null;
  state.submitError = Messages.getHttpErrorMessage(payload);
};

const changeApplicationId = createAsyncThunk(
  `${REDUCER_NAME}/changeApplicationId`,
  async (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const currentOwner = selectSelectedOwner(state);
    const isSbomManager = selectIsSbomManager(state);
    const { ownerType } = selectOwnerProperties(state);
    const { newPublicId } = selectChangeApplicationIdSlice(state);

    const ownerToSave = {
      ...currentOwner,
      publicId: newPublicId.trimmedValue,
    };

    try {
      await dispatch(ownerEditorActions.updateOwner({ ownerToSave, ownerType }));

      dispatch(
        ownerSideNavActions.updateOwnersMapWithNewAppId({
          currentOwner,
          newPublicId: newPublicId.trimmedValue,
        })
      );

      startSaveMaskSuccessTimer(dispatch, actions.closeModal).then(() =>
        dispatch(
          stateGo(`${isSbomManager ? 'sbomManager.' : ''}management.view.application`, {
            applicationPublicId: newPublicId.trimmedValue,
          })
        )
      );
    } catch (err) {
      rejectWithValue(err);
    }
  }
);

const duplicationValidator = (appsList, value) => {
  const exists = any(({ publicId }) => publicId?.toLowerCase() === value.toLowerCase(), appsList);

  return exists ? 'Name is already in use' : null;
};

const inputMatcherValidation = (appsList, inputValue) =>
  combineValidationErrors(
    validateUsernameCharacters(inputValue),
    validateNonEmpty(inputValue),
    validateMaxLength(200, inputValue),
    duplicationValidator(appsList, inputValue)
  );

const setNewPublicIdValue = (state, { payload: { value, appsList } }) => {
  state.newPublicId = userInput(() => inputMatcherValidation(appsList, value), value);
};

const changeAppId = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    openModal: propSet('isModalOpen', true),
    closeModal,
    setNewPublicIdValue,
  },
  extraReducers: {
    [changeApplicationId.pending]: propSet('submitMaskState', false),
    [changeApplicationId.fulfilled]: changeApplicationIdFulfilled,
    [changeApplicationId.rejected]: changeApplicationIdFailed,
  },
});

export default changeAppId.reducer;
export const actions = {
  ...changeAppId.actions,
  changeApplicationId,
};
