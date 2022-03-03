/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
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

import {
  getApplicableCategoriesUrl,
  getCategoriesUrl,
  getDeleteCategoriesUrl,
  getOrganizationAppliedTagUrl,
  getOrganizationPolicyTagUrl,
} from '../util/CLMLocation';
import { selectRouterCurrentParams, selectRouterSlice } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectIsEditMode, selectCurrentCategory } from './orgsAndPoliciesApplicationCategoriesSelectors';
import { Messages } from 'MainRoot/util/CommonServices';
import { pathSet, propSet } from '../util/jsUtil';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { actions as orgsAndPoliciesRootActions } from './orgsAndPoliciesRootSlice';
import { selectOwnerProperties } from './orgsAndPoliciesSelectors';
import { deriveEditRoute } from './utility/util';

const REDUCER_NAME = 'applicationCategories';

export const initialState = {
  appCategoryOwners: [],
  siblings: [],
  loading: false,
  loadError: null,
  isDirty: false,
  currentCategory: {
    id: null,
    color: null,
    name: null,
    description: null,
  },
  serverCategory: null,
  deleteModal: {
    associatedApplicationNames: null,
    tagPolicyList: null,
    // deleting, success and errorState props are required by the Delete Modal and can't be renamed while using DeleteModalService
    deleting: false,
    success: null,
    errorState: null,
  },
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
      .then(({ payload: { applicationCategoriesByOwner = [] } }) => {
        const ownerName = path(['0', 'ownerName'], applicationCategoriesByOwner);
        dispatch(orgsAndPoliciesRootActions.updatedOwnerHandler(ownerName));

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

const loadCategoryEditorRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadCategoryEditorFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  const { associatedApplicationNames, siblings, currentCategory, tagPolicyList } = payload;
  state.currentCategory = currentCategory;
  state.serverCategory = currentCategory;
  state.siblings = siblings || [];
  state.deleteModal.associatedApplicationNames = associatedApplicationNames;
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

const getAssociatedApplicationNames = (applicationTagsByOwner, allApplication, categoryId) => {
  // the first owner is the local one
  const applicationTags = path(['0', 'applicationTags'], applicationTagsByOwner);
  const associatedApplicationNames = [];

  applicationTags.forEach((tag) => {
    if (tag.tagId === categoryId) {
      allApplication.forEach((application) => {
        if (application.id === tag.applicationId) {
          associatedApplicationNames.push(application.name);
        }
      });
    }
  });

  return associatedApplicationNames;
};

const loadCategoryEditor = createAsyncThunk(
  `${REDUCER_NAME}/loadCategoryEditor`,
  ({ categoryEditorPromises }, { getState, rejectWithValue, dispatch }) => {
    const isEditMode = selectIsEditMode(getState());

    const editCategoryPromises = isEditMode
      ? [dispatch(loadOrganizationAppliedTag()), ...categoryEditorPromises, dispatch(loadOrganizationPolicyTags())]
      : [Promise.resolve({})];
    const promises = [dispatch(loadApplicableCategoriesByOwner()), ...editCategoryPromises];

    return Promise.all(promises)
      .then((results) => {
        const [
          {
            payload: { applicationCategoriesByOwner = [] },
          },
        ] = results;

        const siblingsFromAllOwners = getAllApplicationCategories(applicationCategoriesByOwner);

        if (!isEditMode) {
          return {
            siblings: siblingsFromAllOwners,
          };
        }

        const [
          ,
          {
            payload: { applicationTagsByOwner = [] },
          },
          allApplication,
          policyHierarchy,
          { payload: policyTags = [] },
        ] = results;
        const { categoryId } = selectRouterCurrentParams(getState());
        const categoryToEdit = findCategoryToEdit(categoryId, applicationCategoriesByOwner);
        const associatedApplicationNames = getAssociatedApplicationNames(
          applicationTagsByOwner,
          allApplication,
          categoryId
        );
        const policyMap = getPolicyMap(policyHierarchy);
        const tagPolicyList = getTagPolicyList(policyTags, policyMap, categoryId);

        return {
          siblings: siblingsFromAllOwners,
          associatedApplicationNames,
          currentCategory: categoryToEdit,
          tagPolicyList,
        };
      })
      .catch(rejectWithValue);
  }
);

const computeIsDirty = (state) => {
  const { currentCategory, serverCategory } = state;

  const isDirtyObservedProps = ['color', 'name', 'description'];
  const isDirty = isNil(serverCategory)
    ? any((prop) => !isEmpty(currentCategory[prop]), isDirtyObservedProps)
    : any((prop) => currentCategory[prop] !== serverCategory[prop], isDirtyObservedProps);

  return propSet('isDirty', isDirty, state);
};

const setTextInput = curryN(3, function setTextInput(fieldName, state, { payload }) {
  return computeIsDirty(pathSet(['currentCategory', fieldName], payload, state));
});

const saveApplicationCategory = createAsyncThunk(
  `${REDUCER_NAME}/saveApplicationCategory`,
  ({ resetCategoryEditor }, { getState, rejectWithValue }) => {
    const state = getState();
    const isEditMode = selectIsEditMode(state);
    const { ownerType, ownerId } = selectOwnerProperties(state);
    const categoryToSave = selectCurrentCategory(state);

    resetCategoryEditor();

    return axios[isEditMode ? 'put' : 'post'](getCategoriesUrl(ownerType, ownerId), categoryToSave)
      .then(({ data: savedCategory }) => {
        return {
          savedCategory,
          isEditMode,
        };
      })
      .catch(rejectWithValue);
  }
);

const saveApplicationCategoryFulfilled = (state, { payload }) => {
  state.submitError = null;
  state.isDirty = false;

  const { savedCategory, isEditMode } = payload;
  if (isEditMode) {
    state.currentCategory = savedCategory;
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
};

const goToCreateCategory = createAsyncThunk(`${REDUCER_NAME}/goToCreateCategory`, (_, { getState, dispatch }) => {
  const router = selectRouterSlice(getState());
  const { to, params } = deriveEditRoute(router, 'create-category');

  dispatch(stateGo(to, params));
});

const goToEditCategory = createAsyncThunk(`${REDUCER_NAME}/goToEditCategory`, (categoryId, { getState, dispatch }) => {
  const router = selectRouterSlice(getState());
  const { to, params } = deriveEditRoute(router, 'category', { categoryId });

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
        dispatch(actions.resetIsDirty());
        dispatch(actions.goToCreateCategory());

        return categoryToRemove.id;
      })
      .catch(rejectWithValue);
  }
);

const removeApplicationCategoryFulfilled = (state, { payload }) => {
  state.isDirty = false;
  state.deleteModal.success = true;
  state.deleteModal.deleting = null;
  state.deleteModal.errorState = null;
  state.currentCategory = initialState.currentCategory;
  state.serverCategory = initialState.serverCategory;
  state.siblings = reject(propEq('id', payload), state.siblings);
};

const removeApplicationCategoryFailed = (state, { payload }) => {
  state.deleteModal.deleting = false;
  state.deleteModal.errorState = Messages.getHttpErrorMessage(payload);
};

const orgsAndPoliciesApplicationCategoriesSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setCategoryDescription: setTextInput('description'),
    setCategoryName: setTextInput('name'),
    setCategoryColor: setTextInput('color'),
    resetIsDirty: propSet('isDirty', false),
  },
  extraReducers: {
    [loadApplicableCategories.pending]: loadApplicableCategoriesRequested,
    [loadApplicableCategories.fulfilled]: loadApplicableCategoriesFulfilled,
    [loadApplicableCategories.rejected]: loadApplicableCategoriesFailed,
    [loadCategoryEditor.pending]: loadCategoryEditorRequested,
    [loadCategoryEditor.fulfilled]: loadCategoryEditorFulfilled,
    [loadCategoryEditor.rejected]: loadCategoryEditorFailed,
    [saveApplicationCategory.pending]: propSet('submitError', null),
    [saveApplicationCategory.fulfilled]: saveApplicationCategoryFulfilled,
    [saveApplicationCategory.rejected]: saveApplicationCategoryFailed,
    [removeApplicationCategory.pending]: pathSet(['deleteModal', 'deleting'], true),
    [removeApplicationCategory.fulfilled]: removeApplicationCategoryFulfilled,
    [removeApplicationCategory.rejected]: removeApplicationCategoryFailed,
  },
});

export default orgsAndPoliciesApplicationCategoriesSlice.reducer;
export const actions = {
  ...orgsAndPoliciesApplicationCategoriesSlice.actions,
  loadOrganizationAppliedTag,
  loadOrganizationPolicyTags,
  loadCategoryEditor,
  saveApplicationCategory,
  removeApplicationCategory,
  goToEditCategory,
  goToCreateCategory,
  loadApplicableCategories,
  loadApplicableCategoriesByOwner,
};
