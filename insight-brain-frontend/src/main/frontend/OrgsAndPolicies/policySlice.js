/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { any, curryN, equals, findIndex, isEmpty, isNil, map, omit, prop, propEq } from 'ramda';
import { createAsyncThunk, createSlice, unwrapResult } from '@reduxjs/toolkit';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import {
  selectIsOrganization,
  selectIsRootOrganization,
  selectRouterSlice,
  selectRouterCurrentParams,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { getApplicablePolicies, getPolicyCRUDUrl, getPolicyTagUrl, getPolicyUrl } from '../util/CLMLocation';
import {
  selectCategories,
  selectCurrentPolicy,
  selectHasPolicyCategories,
  selectIsEditMode,
  selectIsOrgOwner,
} from './policySelectors';
import { actions as applicationCategoriesActions } from 'MainRoot/OrgsAndPolicies/createEditApplicationCategoriesSlice';
import { deriveEditRoute } from './utility/util';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { propSet, pathSet } from 'MainRoot/util/jsUtil';
import { pathSetConst, propSet as reduxPropSet, propSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { actions as constraintActions } from 'MainRoot/OrgsAndPolicies/constraintSlice';
import { selectOwnerProperties } from './orgsAndPoliciesSelectors';
import { stateReload } from '../reduxUiRouter/routerActions';

const REDUCER_NAME = 'policy';

export const initialState = {
  loading: false,
  loadError: null,
  categoriesForPolicyLoadError: null,
  submitError: null,
  currentPolicy: {
    id: undefined,
    name: undefined,
    threatLevel: 5,
    policyViolationGrandfatheringAllowed: null,
    actions: {},
    notifications: {
      userNotifications: [],
      roleNotifications: [],
      jiraNotifications: [],
      webhookNotifications: [],
    },
    constraints: [
      {
        id: '' + new Date().getTime(),
        name: undefined,
        conditions: [
          {
            conditionTypeId: 'AgeInDays',
            operator: 'older than',
            value: null,
          },
        ],
        operator: 'OR',
      },
    ],
  },
  currentPolicyOwner: null,
  isDirty: false,
  originalPolicy: null,
  originalCategories: null,
  categories: null,
  hasPolicyCategories: false,
  originalHasPolicyCategories: false,
  siblings: [],
  readOnly: undefined,
  isOrgOwner: false,
  isRootOrg: false,
  originalProxyStageAction: null,
  deleteModal: {
    // deleting, success and errorState props are required by the Delete Modal and can't be renamed while using DeleteModalService
    deleting: null,
    success: null,
    errorState: null,
  },
};

const getCategoriesForCurrentPolicy = (categoriesByOwner, currentPolicy) => {
  let startConcat = false,
    categories = [];
  categoriesByOwner.forEach((owner) => {
    // we only want to append categories that are actually part of
    // the owner of the policy being shown.  We don't want to show tags
    // from children when showing a parent policy in read only mode
    if (currentPolicy && (!currentPolicy.ownerId || currentPolicy.ownerId === owner.ownerId)) {
      startConcat = true;
    }
    if (startConcat) {
      categories = categories.concat(owner.applicationCategories);
    }
  });

  return categories;
};

const updateCategoriesIsApplied = (categories, appliedCategoriesById) =>
  categories.map((category) => ({
    ...category,
    isApplied: appliedCategoriesById.includes(category.id),
  }));

const loadCategoriesForPolicy = createAsyncThunk(
  `${REDUCER_NAME}/loadCategoriesForPolicy`,
  (currentPolicy, { getState, dispatch, rejectWithValue }) => {
    const { ownerType, ownerId } = selectOwnerProperties(getState());
    const { policyId } = selectRouterCurrentParams(getState());

    const promises = [
      dispatch(applicationCategoriesActions.loadApplicableCategoriesByOwner()),
      policyId ? axios.get(getPolicyTagUrl(policyId, ownerType, ownerId)) : Promise.resolve({}),
    ];

    return Promise.all(promises)
      .then(([categoriesByOwnerActionPayload, { data: availableCategories = [] }]) => {
        const { applicationCategoriesByOwner } = unwrapResult(categoriesByOwnerActionPayload);

        const hasPolicyCategories = !!availableCategories.length;
        const appliedCategoriesById = hasPolicyCategories ? availableCategories.map(prop('id')) : [];

        const categories = getCategoriesForCurrentPolicy(applicationCategoriesByOwner, currentPolicy);
        const updatedCategoriesIsApplied = updateCategoriesIsApplied(categories, appliedCategoriesById);

        return {
          hasPolicyCategories,
          categories: updatedCategoriesIsApplied,
        };
      })
      .catch(rejectWithValue);
  }
);

const loadCategoriesForPolicyRequested = (state) => {
  state.loading = true;
  state.categoriesForPolicyLoadError = null;
};

const loadCategoriesForPolicyFulfilled = (state, { payload }) => {
  state.loading = false;
  state.categoriesForPolicyLoadError = null;
  const { hasPolicyCategories, categories } = payload;
  state.hasPolicyCategories = hasPolicyCategories;
  state.originalHasPolicyCategories = hasPolicyCategories;
  state.categories = categories;
  state.originalCategories = categories;
};

const loadCategoriesForPolicyFailed = (state, { payload }) => {
  state.loading = false;
  state.categoriesForPolicyLoadError = Messages.getHttpErrorMessage(payload);
  state.isDirty = false;
  state.currentPolicy = state.originalPolicy;
};

const loadApplicablePoliciesByOwner = createAsyncThunk(
  `${REDUCER_NAME}/loadApplicablePoliciesByOwner`,
  (_, { getState, rejectWithValue }) => {
    const { ownerType, ownerId } = selectOwnerProperties(getState());
    return axios.get(getApplicablePolicies(ownerType, ownerId)).then(prop('data')).catch(rejectWithValue);
  }
);

const loadPolicyEditor = createAsyncThunk(
  `${REDUCER_NAME}/loadPolicyEditor`,
  (_, { getState, rejectWithValue, dispatch }) => {
    return dispatch(actions.loadApplicablePoliciesByOwner())
      .then((loadApplicablePoliciesByOwnerAction) => {
        const { policiesByOwner } = unwrapResult(loadApplicablePoliciesByOwnerAction);
        const siblings = policiesByOwner.flatMap(prop('policies'));

        const { policyId } = selectRouterCurrentParams(getState());
        let currentPolicy = initialState.currentPolicy,
          readOnly,
          isOrgOwner,
          originalProxyStageAction,
          isRootOrg = selectIsRootOrganization(getState());
        const currentPolicyOwner = {};

        if (policyId) {
          const matchesPolicyId = propEq('id', policyId);
          currentPolicy = siblings.find(matchesPolicyId);
          originalProxyStageAction = currentPolicy.actions['proxy'];
          policiesByOwner.some(({ policies, ownerId, ownerName, ownerType }, index) => {
            if (policies.some(matchesPolicyId)) {
              readOnly = index !== 0;

              currentPolicyOwner.id = ownerId;
              currentPolicyOwner.name = ownerName;

              isOrgOwner = ownerType === 'organization';
              return true;
            }
          });

          isRootOrg = currentPolicyOwner.id === 'ROOT_ORGANIZATION_ID';
        } else {
          const localOwner = policiesByOwner[0];

          currentPolicyOwner.id = localOwner.ownerId; // remove id if not needed.
          currentPolicyOwner.name = localOwner.ownerName;
          isOrgOwner = selectIsOrganization(getState());
        }

        dispatch(
          constraintActions.loadConstraint({ isNewPolicy: isNil(policyId), constraints: currentPolicy.constraints })
        );

        if (isOrgOwner) {
          dispatch(loadCategoriesForPolicy(currentPolicy));
        }

        return {
          siblings,
          currentPolicy,
          currentPolicyOwner,
          readOnly,
          isOrgOwner,
          isRootOrg,
          originalProxyStageAction,
        };
      })
      .catch(rejectWithValue);
  }
);

const loadPolicyEditorRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const loadPolicyEditorFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.deleteModal.deleting = null;
  state.deleteModal.success = null;
  state.deleteModal.errorState = null;
  state.isDirty = false;
  const {
    siblings,
    currentPolicy,
    currentPolicyOwner,
    readOnly,
    isOrgOwner,
    isRootOrg,
    originalProxyStageAction,
  } = payload;

  state.siblings = siblings;
  state.currentPolicy = currentPolicy;
  state.originalPolicy = currentPolicy;
  state.currentPolicyOwner = currentPolicyOwner;
  state.readOnly = readOnly;
  state.isOrgOwner = isOrgOwner;
  state.isRootOrg = isRootOrg;
  state.originalProxyStageAction = originalProxyStageAction;
};

const loadPolicyEditorFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
  state.isDirty = false;
  state.currentPolicy = state.originalPolicy;
};

function reloadPageAfterDuration(dispatch) {
  setTimeout(() => {
    dispatch(stateReload());
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

const removeHashKeys = omit(['$$hashKey']);
const removeNotificationHashKeys = map(removeHashKeys);

const savePolicy = createAsyncThunk(
  `${REDUCER_NAME}/savePolicy`,
  ({ onSaveExistingPolicy }, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const isEditMode = selectIsEditMode(state);
    const { ownerType, ownerId } = selectOwnerProperties(state);
    const currentPolicy = selectCurrentPolicy(state);
    const hasPolicyCategories = selectHasPolicyCategories(state);
    const isOrgOwner = selectIsOrgOwner(state);
    const categories = selectCategories(state);
    const appliedCategories = categories?.filter(prop('isApplied')).map(omit(['isApplied']));
    const policyToSave = {
      ...currentPolicy,
      notifications: map(removeNotificationHashKeys, currentPolicy.notifications),
    };
    const categoriesToSave = hasPolicyCategories ? appliedCategories : [];

    return axios[isEditMode ? 'put' : 'post'](getPolicyUrl(ownerType, ownerId), policyToSave)
      .then(({ data: savedPolicy }) =>
        isOrgOwner
          ? axios.put(getPolicyTagUrl(savedPolicy.id, ownerType, ownerId), categoriesToSave)
          : Promise.resolve({})
      )
      .then(() => {
        if (!isEditMode) {
          return reloadPageAfterDuration(dispatch);
        }

        onSaveExistingPolicy();
        return { isEditMode };
      })
      .catch(rejectWithValue);
  }
);

const savePolicyRequested = (state) => {
  state.loading = true;
  state.loadError = null;
};

const savePolicyFulfilled = (state, { payload }) => {
  state.loading = false;
  state.loadError = null;
  state.isDirty = false;

  if (payload?.isEditMode) {
    state.originalPolicy = state.currentPolicy;
    state.originalCategories = state.categories;
    state.originalHasPolicyCategories = state.hasPolicyCategories;
    state.originalProxyStageAction = state.currentPolicy.actions['proxy'];

    const index = findIndex(propEq('id', state.currentPolicy.id), state.siblings);
    state.siblings[index] = state.currentPolicy;
  } else {
    state.currentPolicy = initialState.currentPolicy;
  }
};

const savePolicyFailed = (state, { payload }) => {
  state.loading = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
};

const goToCreatePolicy = createAsyncThunk(`${REDUCER_NAME}/goToCreatePolicy`, (_, { getState, dispatch }) => {
  const router = selectRouterSlice(getState());
  const { to, params } = deriveEditRoute(router, 'create-policy');

  dispatch(stateGo(to, params));
});

const removePolicy = createAsyncThunk(`${REDUCER_NAME}/removePolicy`, (_, { getState, dispatch, rejectWithValue }) => {
  const state = getState();
  const { ownerType, ownerId } = selectOwnerProperties(state);
  const policyToRemove = selectCurrentPolicy(state);

  return axios
    .delete(getPolicyCRUDUrl(ownerType, ownerId, policyToRemove.id))
    .then(() => {
      dispatch(actions.resetIsDirty());
      dispatch(actions.resetDeleteModalState());
      dispatch(goToCreatePolicy());

      return policyToRemove.id;
    })
    .catch(rejectWithValue);
});

const removePolicyFulfilled = (state) => {
  state.deleteModal.success = true;
  state.deleteModal.deleting = null;
  state.deleteModal.errorState = null;
};

const removePolicyFailed = (state, { payload }) => {
  state.deleteModal.deleting = false;
  state.deleteModal.errorState = Messages.getHttpErrorMessage(payload);
};

const computeIsDirty = (state) => {
  const { currentPolicy, originalPolicy } = state;

  const isDirtyObservedProps = [
    'name',
    'threatLevel',
    'policyViolationGrandfatheringAllowed',
    'constraints',
    'notifications',
    'actions',
  ];
  const isDirty = isNil(originalPolicy)
    ? any((prop) => !isEmpty(currentPolicy[prop]), isDirtyObservedProps)
    : any((prop) => !equals(currentPolicy[prop], originalPolicy[prop]), isDirtyObservedProps);

  return propSet('isDirty', isDirty, state);
};

const setPolicyField = curryN(3, function setPolicyField(fieldName, state, { payload }) {
  return computeIsDirty(pathSet(['currentPolicy', fieldName], payload, state));
});

const toggleField = curryN(2, function toggleField(fieldName, state) {
  return computeIsDirty(pathSet(['currentPolicy', fieldName], !state.currentPolicy[fieldName], state));
});

const setConstraintField = curryN(3, function setConstraintField(fieldName, state, { payload }) {
  const { constraintIndex, value } = payload;
  return computeIsDirty(pathSet(['currentPolicy', 'constraints', constraintIndex, fieldName], value, state));
});

const setNotifications = curryN(3, function setNotifications(notificationType, state, { payload }) {
  return computeIsDirty(pathSet(['currentPolicy', 'notifications', notificationType], payload, state));
});

const setNotificationStageIds = curryN(3, function setNotificationStageIds(notificationType, state, { payload }) {
  const { index, value } = payload;
  return computeIsDirty(pathSet(['currentPolicy', 'notifications', notificationType, index, 'stageIds'], value, state));
});

const setConstraintConditionField = curryN(3, function setConstraintConditionField(fieldName, state, { payload }) {
  const { constraintIndex, conditionIndex, value } = payload;
  return computeIsDirty(
    pathSet(['currentPolicy', 'constraints', constraintIndex, 'conditions', conditionIndex, fieldName], value, state)
  );
});

const setConstraintCondition = curryN(2, function setConstraintCondition(state, { payload }) {
  const { constraintIndex, conditionIndex, value } = payload;
  return computeIsDirty(
    pathSet(['currentPolicy', 'constraints', constraintIndex, 'conditions', conditionIndex], value, state)
  );
});

const toggleCategoryIsApplied = (state, { payload: index }) => {
  state.categories[index].isApplied = !state.categories[index].isApplied;
};

const policySlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetIsDirty: propSetConst('isDirty', initialState.isDirty),
    resetDeleteModalState: propSetConst('deleteModal', initialState.deleteModal),
    setHasPolicyCategories: reduxPropSet('hasPolicyCategories'),
    toggleCategoryIsApplied,
    togglePolicyViolationGrandfatheringAllowed: toggleField('policyViolationGrandfatheringAllowed'),
    setPolicyName: setPolicyField('name'),
    setThreatLevel: setPolicyField('threatLevel'),
    setActions: setPolicyField('actions'),
    addConstraint: setPolicyField('constraints'),
    deleteConstraint: setPolicyField('constraints'),
    setUserNotifications: setNotifications('userNotifications'),
    setRoleNotifications: setNotifications('roleNotifications'),
    setJiraNotifications: setNotifications('jiraNotifications'),
    setWebhookNotifications: setNotifications('webhookNotifications'),
    setUserNotificationStageIds: setNotificationStageIds('userNotifications'),
    setRoleNotificationStageIds: setNotificationStageIds('roleNotifications'),
    setJiraNotificationStageIds: setNotificationStageIds('jiraNotifications'),
    setWebhookNotificationStageIds: setNotificationStageIds('webhookNotifications'),
    addCondition: setConstraintField('conditions'),
    deleteCondition: setConstraintField('conditions'),
    setConstraintName: setConstraintField('name'),
    setConstraintCondition: setConstraintCondition,
    setConstraintOperator: setConstraintField('operator'),
    setConditionOperator: setConstraintConditionField('operator'),
    setConditionValue: setConstraintConditionField('value'),
  },
  extraReducers: {
    [loadCategoriesForPolicy.pending]: loadCategoriesForPolicyRequested,
    [loadCategoriesForPolicy.fulfilled]: loadCategoriesForPolicyFulfilled,
    [loadCategoriesForPolicy.rejected]: loadCategoriesForPolicyFailed,
    [loadPolicyEditor.pending]: loadPolicyEditorRequested,
    [loadPolicyEditor.fulfilled]: loadPolicyEditorFulfilled,
    [loadPolicyEditor.rejected]: loadPolicyEditorFailed,
    [savePolicy.pending]: savePolicyRequested,
    [savePolicy.fulfilled]: savePolicyFulfilled,
    [savePolicy.rejected]: savePolicyFailed,
    [removePolicy.pending]: pathSetConst(['deleteModal', 'deleting'], true),
    [removePolicy.fulfilled]: removePolicyFulfilled,
    [removePolicy.rejected]: removePolicyFailed,
  },
});

export const actions = {
  ...policySlice.actions,
  loadApplicablePoliciesByOwner,
  loadCategoriesForPolicy,
  loadPolicyEditor,
  savePolicy,
  removePolicy,
};

export default policySlice.reducer;
