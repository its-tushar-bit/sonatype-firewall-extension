/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice, unwrapResult } from '@reduxjs/toolkit';
import axios from 'axios';
import {
  any,
  compose,
  curryN,
  find,
  findIndex,
  flatten,
  isEmpty,
  isNil,
  map,
  path,
  pathOr,
  prop,
  propEq,
  reject,
} from 'ramda';

import { nxTextInputStateHelpers, combineValidationErrors } from '@sonatype/react-shared-components';

import {
  getApplicableCategoriesUrl,
  getCategoriesUrl,
  getDeleteCategoriesUrl,
  getOrganizationAppliedTagUrl,
  getOrganizationPolicyTagUrl,
} from '../../util/CLMLocation';
import { selectRouterCurrentParams, selectRouterSlice } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectIsEditMode, selectCurrentCategory } from './createEditApplicationCategoriesSelectors';
import { Messages } from 'MainRoot/utilAngular/CommonServices';
import { pathSet, propSet } from '../../util/jsUtil';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { selectOwnerProperties } from '../orgsAndPoliciesSelectors';
import { deriveEditRoute } from '../utility/util';
import { actions as rootActions } from '../rootSlice';
import {
  validateMaxLength,
  validateNameCharacters,
  validateNonEmpty,
  validateDoubleWhitespace,
} from 'MainRoot/util/validationUtil';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';
import { rscToAngularColorMap } from '../utility/util';

const { initialState: rscInitialState, userInput } = nxTextInputStateHelpers;

const REDUCER_NAME = 'applicationCategories/createEdit';

export const initialState = {
  appCategoryOwners: [],
  siblings: [],
  loading: false,
  loadError: null,
  isDirty: false,
  currentCategory: {
    id: null,
    color: Object.values(rscToAngularColorMap)[0],
    name: rscInitialState('', validateNonEmpty),
    description: rscInitialState('', validateNonEmpty),
  },
  serverCategory: null,
  deleteModal: {
    applicationTags: null,
    tagPolicyList: null,
  },
  deleteError: null,
  submitMaskState: null,
  deleteMaskState: null,
  validationError: null,
};

const loadOrganizationPolicyTags = createAsyncThunk(
  `${REDUCER_NAME}/loadOrganizationPolicyTags`,
  (_, { getState, rejectWithValue }) => {
    const { ownerId } = selectOwnerProperties(getState());
    const url = getOrganizationPolicyTagUrl(ownerId);

    return axios.get(url).then(prop('data')).catch(rejectWithValue);
  }
);

const loadOrganizationAppliedTag = createAsyncThunk(
  `${REDUCER_NAME}/loadOrganizationAppliedTag`,
  (_, { getState, rejectWithValue }) => {
    const { ownerId } = selectOwnerProperties(getState());
    const url = getOrganizationAppliedTagUrl(ownerId);

    return axios.get(url).then(prop('data')).catch(rejectWithValue);
  }
);

const loadApplicableCategoriesByOwner = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicableCategoriesByOwner`,
  (_, { getState, rejectWithValue }) => {
    const { ownerType, ownerId } = selectOwnerProperties(getState());
    const url = getApplicableCategoriesUrl(ownerType, ownerId);

    return axios.get(url).then(prop('data')).catch(rejectWithValue);
  }
);

const loadApplicableCategories = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicableCategories`,
  (_, { dispatch, rejectWithValue }) => {
    return dispatch(loadApplicableCategoriesByOwner())
      .then((applicationCategoriesByOwnerActionPayload) => {
        const { applicationCategoriesByOwner = [] } = unwrapResult(applicationCategoriesByOwnerActionPayload);

        const appCategoryOwners = applicationCategoriesByOwner.map((owner, index) => {
          const isParent = index !== 0;
          return { ...owner, parent: isParent };
        });

        return {
          appCategoryOwners,
        };
      })
      .catch(rejectWithValue);
  }
);

const loadApplicableCategoriesRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadApplicableCategoriesFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.appCategoryOwners = payload.appCategoryOwners || [];
};

const loadApplicableCategoriesFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const clearDeleteError = (state) => {
  state.deleteError = null;
};

const loadCategoryEditorRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadCategoryEditorFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.submitError = null;
  state.deleteError = null;
  state.isDirty = false;

  const { applicationTags, siblings, currentCategory: currentCategoryPayload, tagPolicyList } = payload;
  state.currentCategory = {
    ...currentCategoryPayload,
    name: rscInitialState(currentCategoryPayload?.name ?? '', validateNonEmpty),
    description: rscInitialState(currentCategoryPayload?.description ?? '', validateNonEmpty),
  };
  state.serverCategory = currentCategoryPayload;
  state.siblings = siblings || [];
  state.deleteModal.applicationTags = applicationTags;
  state.deleteModal.tagPolicyList = tagPolicyList;
};

const loadCategoryEditorFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const getAllApplicationCategories = compose(flatten, map(prop('applicationCategories')));
const findCategoryToEdit = (id, categoriesByOwner) => {
  const matchesId = propEq('id');
  const findById = find(matchesId(id));
  // the first owner is the local one
  const localApplicationCategory = pathOr([], [0, 'applicationCategories'], categoriesByOwner);
  return findById(localApplicationCategory);
};

const getTagPolicyList = (policyTags, policyMap, categoryId) => {
  const tagPolicyList = [];

  policyTags.forEach(({ tagId, policyId }) => {
    if (tagId === categoryId) {
      tagPolicyList.push(policyMap[policyId]);
    }
  });

  return reject(isNil, tagPolicyList);
};

const getPolicyMap = (policyHierarchy) => {
  const policyMap = {};

  policyHierarchy.forEach(function (owner) {
    owner.policies.forEach(function ({ id, name }) {
      policyMap[id] = name;
    });
  });

  return policyMap;
};

const loadCategoryEditor = createAsyncThunk(
  `${REDUCER_NAME}/loadCategoryEditor`,
  (_, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const isEditMode = selectIsEditMode(state);

    const editCategoryPromises = isEditMode
      ? [
          dispatch(loadOrganizationAppliedTag()),
          dispatch(rootActions.loadApplicablePoliciesByOwner()),
          dispatch(loadOrganizationPolicyTags()),
        ]
      : [Promise.resolve({})];
    const promises = [dispatch(loadApplicableCategoriesByOwner()), ...editCategoryPromises];

    return Promise.all(promises)
      .then((results) => {
        const [
          applicationCategoriesByOwnerActionPayload,
          applicationTagsByOwnerActionPayload,
          loadApplicablePoliciesByOwnerActionPayload,
          policyTagActionPayload,
        ] = results;
        const { applicationCategoriesByOwner = [] } = unwrapResult(applicationCategoriesByOwnerActionPayload);
        const siblingsFromAllOwners = getAllApplicationCategories(applicationCategoriesByOwner);

        if (!isEditMode) {
          return {
            siblings: siblingsFromAllOwners,
            currentCategory: {
              name: '',
              description: '',
              color: Object.values(rscToAngularColorMap)[0],
            },
          };
        }
        const { applicationTagsByOwner = [] } = unwrapResult(applicationTagsByOwnerActionPayload);
        const policyTags = unwrapResult(policyTagActionPayload);
        const { categoryId } = selectRouterCurrentParams(state);
        const categoryToEdit = findCategoryToEdit(categoryId, applicationCategoriesByOwner);
        const policiesByOwner = unwrapResult(loadApplicablePoliciesByOwnerActionPayload);
        const policyMap = getPolicyMap(policiesByOwner);
        const tagPolicyList = getTagPolicyList(policyTags, policyMap, categoryId);

        return {
          siblings: siblingsFromAllOwners,
          // the first owner is the local one
          applicationTags: path(['0', 'applicationTags'], applicationTagsByOwner),
          currentCategory: categoryToEdit,
          tagPolicyList,
        };
      })
      .catch(rejectWithValue);
  }
);

const computeIsDirty = (state) => {
  const { currentCategory, serverCategory } = state;
  const computedCurrentCategory = {
    ...currentCategory,
    description: currentCategory?.description.trimmedValue,
    name: currentCategory?.name.trimmedValue,
  };

  const isDirtyObservedProps = ['color', 'name', 'description'];
  const isDirty = isNil(serverCategory)
    ? any((prop) => !isEmpty(currentCategory[prop]), isDirtyObservedProps)
    : any((prop) => computedCurrentCategory[prop] !== serverCategory[prop], isDirtyObservedProps);

  return propSet('isDirty', isDirty, state);
};

const validateDuplicationName = (value, { siblings, currentCategory }) => {
  const isExist = any(
    (item) => item.name?.toLowerCase() === value.toLowerCase() && item.id !== currentCategory.id,
    siblings
  );
  return isExist ? 'Name is already in use' : null;
};

const categoryNameValidator = (val, state) => () =>
  combineValidationErrors(
    validateNonEmpty(val.trim()),
    validateMaxLength(60, val),
    validateDoubleWhitespace(val),
    validateDuplicationName(val.trim(), state),
    validateNameCharacters(val)
  );

const categoryDescriptionValidator = (val) => () =>
  combineValidationErrors(validateNonEmpty(val.trim()), validateMaxLength(255, val));

const setTextInput = curryN(4, function setTextInput(fieldName, validationFunc, state, { payload }) {
  return computeIsDirty(
    pathSet(['currentCategory', fieldName], userInput(validationFunc(payload, state), payload), state)
  );
});

const setColorInput = curryN(2, function setTextInput(state, { payload }) {
  return computeIsDirty(pathSet(['currentCategory', 'color'], payload, state));
});

const saveApplicationCategory = createAsyncThunk(
  `${REDUCER_NAME}/saveApplicationCategory`,
  (_, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const isEditMode = selectIsEditMode(state);
    const { ownerType, ownerId } = selectOwnerProperties(state);
    const categoryToSave = selectCurrentCategory(state);
    const newCategory = {
      ...categoryToSave,
      description: categoryToSave.description.trimmedValue,
      name: categoryToSave.name.trimmedValue,
    };
    return axios[isEditMode ? 'put' : 'post'](getCategoriesUrl(ownerType, ownerId), newCategory)
      .then(({ data: savedCategory }) => {
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone);
        return {
          savedCategory,
          isEditMode,
        };
      })
      .catch(rejectWithValue);
  }
);

const saveCategoryPending = (state) => {
  state.submitError = null;
  state.submitMaskState = false;
};

const saveApplicationCategoryFulfilled = (state, { payload }) => {
  state.submitError = null;
  state.isDirty = false;
  state.submitMaskState = true;

  const { savedCategory, isEditMode } = payload;
  if (isEditMode) {
    state.currentCategory = {
      ...savedCategory,
      name: rscInitialState(savedCategory?.name ?? '', validateNonEmpty),
      description: rscInitialState(savedCategory?.description ?? '', validateNonEmpty),
    };
    state.serverCategory = savedCategory;
  } else {
    state.currentCategory = initialState.currentCategory;
    state.serverCategory = initialState.currentCategory;
  }

  const index = findIndex(propEq('id', savedCategory.id), state.siblings);
  if (index === -1) {
    state.siblings.push(savedCategory);
  } else {
    state.siblings[index] = savedCategory;
  }
};

const saveApplicationCategoryFailed = (state, { payload }) => {
  state.submitError = Messages.getHttpErrorMessage(payload);
  state.submitMaskState = null;
};

const goToCreateCategory = createAsyncThunk(`${REDUCER_NAME}/goToCreateCategory`, (_, { getState, dispatch }) => {
  const router = selectRouterSlice(getState());
  const { to, params } = deriveEditRoute(router, 'create-category');

  dispatch(stateGo(to, params));
});

const removeApplicationCategory = createAsyncThunk(
  `${REDUCER_NAME}/removeApplicationCategory`,
  (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerProperties(state);
    const categoryToRemove = selectCurrentCategory(state);

    return axios
      .delete(getDeleteCategoriesUrl(ownerType, ownerId, categoryToRemove.id))
      .then(() => {
        startSaveMaskSuccessTimer(dispatch, actions.deleteMaskTimerDone);
        dispatch(actions.resetIsDirty());
        dispatch(actions.goToCreateCategory());
        return categoryToRemove.id;
      })
      .catch(rejectWithValue);
  }
);

const removeCategoryPending = (state) => {
  state.submitError = null;
  state.deleteMaskState = false;
};

const removeApplicationCategoryFulfilled = (state, { payload }) => {
  state.isDirty = false;
  state.deleteMaskState = true;
  state.deleteError = null;
  state.currentCategory = initialState.currentCategory;
  state.serverCategory = initialState.serverCategory;
  state.siblings = reject(propEq('id', payload), state.siblings);
};

const removeApplicationCategoryFailed = (state, { payload }) => {
  state.deleteMaskState = null;
  state.deleteError = Messages.getHttpErrorMessage(payload);
};

const createEditApplicationCategoriesSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setCategoryName: setTextInput('name', categoryNameValidator),
    setCategoryDescription: setTextInput('description', categoryDescriptionValidator),
    setCategoryColor: setColorInput,
    resetIsDirty: propSet('isDirty', false),
    saveMaskTimerDone: propSet('submitMaskState', null),
    deleteMaskTimerDone: propSet('deleteMaskState', null),
    clearDeleteError,
  },
  extraReducers: {
    [loadApplicableCategories.pending]: loadApplicableCategoriesRequested,
    [loadApplicableCategories.fulfilled]: loadApplicableCategoriesFulfilled,
    [loadApplicableCategories.rejected]: loadApplicableCategoriesFailed,
    [loadCategoryEditor.pending]: loadCategoryEditorRequested,
    [loadCategoryEditor.fulfilled]: loadCategoryEditorFulfilled,
    [loadCategoryEditor.rejected]: loadCategoryEditorFailed,
    [saveApplicationCategory.pending]: saveCategoryPending,
    [saveApplicationCategory.fulfilled]: saveApplicationCategoryFulfilled,
    [saveApplicationCategory.rejected]: saveApplicationCategoryFailed,
    [removeApplicationCategory.pending]: removeCategoryPending,
    [removeApplicationCategory.fulfilled]: removeApplicationCategoryFulfilled,
    [removeApplicationCategory.rejected]: removeApplicationCategoryFailed,
  },
});

export default createEditApplicationCategoriesSlice.reducer;
export const actions = {
  ...createEditApplicationCategoriesSlice.actions,
  loadOrganizationAppliedTag,
  loadOrganizationPolicyTags,
  loadCategoryEditor,
  saveApplicationCategory,
  removeApplicationCategory,
  goToCreateCategory,
  loadApplicableCategories,
  loadApplicableCategoriesByOwner,
};
