/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  any,
  always,
  includes,
  compose,
  curryN,
  equals,
  findIndex,
  isEmpty,
  isNil,
  map,
  mapObjIndexed,
  omit,
  path,
  prop,
  propEq,
  values,
  without,
  sortWith,
  reverse,
  clone,
} from 'ramda';
import { createAsyncThunk, createSlice, unwrapResult } from '@reduxjs/toolkit';
import {
  SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS,
  nxTextInputStateHelpers,
  combineValidationErrors,
} from '@sonatype/react-shared-components';
import {
  validateDuplicatedValue,
  validateMaxLength,
  validateNameCharacters,
  validateNonEmpty,
  validateDoubleWhitespace,
  combineValidators,
  validateEmailPatternMatch,
  validateForm,
} from 'MainRoot/util/validationUtil';

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import {
  selectIsOrganization,
  selectIsRootOrganization,
  selectRouterSlice,
  selectRouterCurrentParams,
  selectOwnerInfo,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  getNotificationWebhooksUrl,
  getPolicyActionsOverridesUrl,
  getPolicyCRUDUrl,
  getPolicyTagUrl,
  getPolicyUrl,
  getRoleMappingForCurrentOwnerUrl,
  getJiraProjectsUrl,
  getIsJiraEnabledUrl,
} from '../util/CLMLocation';
import {
  selectCategories,
  selectCurrentPolicy,
  selectHasPolicyCategories,
  selectIsEditMode,
  selectIsOrgOwner,
  selectOriginalPolicyName,
  selectRolesForCurrentOwner,
} from './policySelectors';
import { actions as applicationCategoriesActions } from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/createEditApplicationCategoriesSlice';
import { deriveEditRoute, policiesComparator, getActionsOverride } from 'MainRoot/OrgsAndPolicies/utility/util';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { propSet, pathSet, allEqual, anyIndexed } from 'MainRoot/util/jsUtil';
import { propSet as reduxPropSet, propSetConst } from 'MainRoot/util/reduxToolkitUtil';
import { selectOwnerProperties, selectSelectedOwnerId } from './orgsAndPoliciesSelectors';
import { actions as constraintActions } from 'MainRoot/OrgsAndPolicies/constraintSlice';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';
import { actions as stagesActions } from 'MainRoot/OrgsAndPolicies/stagesSlice';
import { stateReload } from '../reduxUiRouter/routerActions';
import { checkPermissions } from '../util/authorizationUtil';
import {
  getCoordinatesValue,
  dataTypeValidatorsMap,
  valueTypeIdValidatorMap,
  ageValidator,
  getCoordinatesValidator,
  coordinatesTypes,
  coordinatesFormatOptions,
  conditionsWithoutValue,
  withDefaultValue,
} from 'MainRoot/OrgsAndPolicies/utility/constraintUtil';
import { loadActionStages } from './stagesSlice';
import { startSaveMaskSuccessTimer } from 'MainRoot/util/reduxUtil';

const { initialState: initUserInput, userInput } = nxTextInputStateHelpers;

const REDUCER_NAME = 'policy';

export const RECIPIENT_TYPES = {
  EMAIL: 'Email',
  ROLE: 'Role',
  JIRA: 'JIRA',
  WEBHOOK: 'Webhook',
};

const policyNameValidator = curryN(3, (siblings, originalPolicyName, val) =>
  combineValidationErrors(
    validateNameCharacters(val),
    validateNonEmpty(val),
    validateMaxLength(60, val),
    validateDuplicatedValue(siblings, val, originalPolicyName),
    validateDoubleWhitespace(val)
  )
);

export const initialState = {
  loadingSavePolicy: false,
  loadingCategories: false,
  loadingPolicyEditor: false,
  loadError: null,
  deleteError: null,
  categoriesForPolicyLoadError: null,
  submitError: null,
  overrideActionsFlag: null,
  originalOverrideActionsFlag: null,
  currentPolicy: {
    id: undefined,
    name: initUserInput('', policyNameValidator([], '')),
    threatLevel: 5,
    policyViolationGrandfatheringAllowed: null,
    policyActionsOverrideAllowed: null,
    actions: {},
    policyActionsOverrides: null,
    notifications: {
      userNotifications: [],
      roleNotifications: [],
      jiraNotifications: [],
      webhookNotifications: [],
    },
    constraints: [
      {
        id: '' + new Date().getTime(),
        name: initUserInput(''),
        conditions: [
          {
            conditionTypeId: 'AgeInDays',
            operator: 'older than',
            value: initUserInput(''),
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
  isInherited: undefined,
  isOrgOwner: false,
  isRootOrg: false,
  originalProxyStageAction: null,

  hasEditIqPermission: false,
  notificationsEditor: {
    loading: false,
    loadError: null,
    roles: null,
    notificationWebhooks: null,
    isJiraEnabled: false,
    jiraProjects: null,
    formState: {
      recipientType: initUserInput(RECIPIENT_TYPES.EMAIL),
      recipientEmail: initUserInput(''),
      recipientRoleId: initUserInput(''),
      recipientProjectKey: initUserInput(''),
      recipientIssueTypeId: initUserInput(''),
    },
  },
  validationError: null,
  submitMaskState: null,
  policyTile: {
    loading: false,
    loadError: null,
    policiesByOwner: null,
    sorting: {},
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
  state.loadingCategories = true;
  state.categoriesForPolicyLoadError = null;
};

const loadCategoriesForPolicyFulfilled = (state, { payload }) => {
  state.loadingCategories = false;
  state.categoriesForPolicyLoadError = null;
  const { hasPolicyCategories, categories } = payload;
  state.hasPolicyCategories = hasPolicyCategories;
  state.originalHasPolicyCategories = hasPolicyCategories;
  state.categories = categories;
  state.originalCategories = categories;
};

const loadCategoriesForPolicyFailed = (state, { payload }) => {
  state.loadingCategories = false;
  state.categoriesForPolicyLoadError = Messages.getHttpErrorMessage(payload);
  state.isDirty = false;
  state.currentPolicy = state.originalPolicy;
};

const loadPolicyTile = createAsyncThunk(`${REDUCER_NAME}/loadPolicyTile`, (_, { rejectWithValue, dispatch }) => {
  const promises = [dispatch(rootActions.loadApplicablePoliciesByOwner()), dispatch(stagesActions.loadActionStages())];
  return Promise.all(promises)
    .then(([loadApplicablePoliciesByOwnerPayload, loadActionStagesPayload]) => {
      const policiesByOwner = unwrapResult(loadApplicablePoliciesByOwnerPayload);
      const { data: actionStages } = unwrapResult(loadActionStagesPayload);

      const ownerIds = policiesByOwner.map(prop('ownerId'));

      const updatedPoliciesByOwner = policiesByOwner.map((policyOwner, index) => {
        const policies = policyOwner.policies.map((policy) => {
          const actionsOverrideInfo = getActionsOverride(ownerIds, policy);
          const actions = actionsOverrideInfo?.actionsOverride || policy.actions;

          const enforcementAction = {};
          actionStages.forEach((actionStage) => {
            if (actions[actionStage.stageTypeId]) {
              enforcementAction[actionStage.stageTypeId] = actions[actionStage.stageTypeId];
            }
          });

          return {
            ...policy,
            hasLocalActionsOverrides: actionsOverrideInfo?.isCurrentOwnerOverride,
            enforcementAction,
          };
        });

        return {
          ...policyOwner,
          inherited: index > 0,
          policies,
        };
      });

      return updatedPoliciesByOwner;
    })
    .catch(rejectWithValue);
});

const loadPolicyTileRequested = (state) => {
  state.policyTile.loading = true;
  state.policyTile.loadError = null;
};

const loadPolicyTileFulfilled = (state, { payload }) => {
  state.policyTile.loading = false;
  state.policyTile.sorting = setInitialSorting(payload);
  state.policyTile.policiesByOwner = sortItemsByField(payload, state.policyTile.sorting);
};

const loadPolicyTileFailed = (state, { payload }) => {
  state.policyTile.loading = false;
  state.policyTile.loadError = Messages.getHttpErrorMessage(payload);
};

const setInitialSorting = (policiesByOwner) => {
  const mapped = map(
    (owner) => ({
      key: 'threatLevel',
      dir: 'desc',
      ownerName: owner.ownerName,
    }),
    policiesByOwner
  );

  const options = {};
  mapped.forEach((option) => {
    options[option.ownerName] = option;
  });
  return options;
};

const checkAreStageValuesEqual = (key, policies) => {
  if (includes(key, ['name', 'threatLevel'])) {
    return false;
  }
  const values = policies.map(
    (policy) => policy[isNil(policy.hasLocalActionsOverrides) ? 'actions' : 'enforcementAction'][key]
  );
  return allEqual(values);
};

const sortItemsByField = (policiesByOwner, sortingConfig, updatedSorting = null) => {
  if (!isNil(updatedSorting)) {
    const { key, dir, ownerName } = updatedSorting;
    const customSort = sortWith(policiesComparator(prop(key), key));

    const cloned = clone(policiesByOwner);
    const index = findIndex(propEq('ownerName', ownerName), cloned);
    const equalValues = checkAreStageValuesEqual(key, cloned[index].policies);

    if (equalValues) {
      return policiesByOwner;
    }

    const sorted = customSort(cloned[index].policies);

    cloned[index].policies = dir === 'asc' ? sorted : reverse(sorted);
    return cloned;
  }

  return map((owner) => {
    const { dir, key } = sortingConfig[owner.ownerName];
    const customSort = sortWith(policiesComparator(prop(key), key));
    return {
      ...owner,
      policies: dir === 'asc' ? customSort(owner.policies) : reverse(customSort(owner.policies)),
    };
  }, policiesByOwner);
};

const changeSortField = (state, { payload }) => {
  const newSorting = {
    ...state.policyTile.sorting,
    [payload.ownerName]: payload,
  };
  state.policyTile.sorting = newSorting;
  state.policyTile.policiesByOwner = sortItemsByField(state.policyTile.policiesByOwner, newSorting, payload);
};

const convertMatcherToUserInput = (policy) => {
  const constraints = policy.constraints.map((constraint) => {
    const conditions = constraint.conditions.map((condition) => {
      let value = initUserInput(condition.value ?? '');

      if (condition.conditionTypeId === 'Coordinates') {
        const parts = condition.value.split(':');
        value = {
          format: parts.shift(),
        };
        parts.forEach((part, partIdx) => {
          value[coordinatesTypes[value.format][partIdx]] = initUserInput(part);
        });
      }
      return { ...condition, value };
    });

    return { ...constraint, conditions, name: initUserInput(constraint.name) };
  });

  return { ...policy, constraints };
};

const loadPolicyEditor = createAsyncThunk(
  `${REDUCER_NAME}/loadPolicyEditor`,
  (_, { getState, rejectWithValue, dispatch }) => {
    return dispatch(rootActions.loadApplicablePoliciesByOwner())
      .then((loadApplicablePoliciesByOwnerAction) => {
        const state = getState();
        const policiesByOwner = unwrapResult(loadApplicablePoliciesByOwnerAction);
        const siblings = policiesByOwner.flatMap(prop('policies'));

        const { policyId } = selectRouterCurrentParams(state);
        let currentPolicy = initialState.currentPolicy;
        let isInherited;
        let isOrgOwner;
        let originalProxyStageAction;
        let isRootOrg = selectIsRootOrganization(state);

        const currentPolicyOwner = {};
        if (policyId) {
          const matchesPolicyId = propEq('id', policyId);
          const currentMatchedPolicy = siblings.find(matchesPolicyId);
          currentPolicy = convertMatcherToUserInput(currentMatchedPolicy);
          originalProxyStageAction = currentPolicy.actions['proxy'];

          const originalPolicyName = selectOriginalPolicyName(state);
          const name = initUserInput(currentPolicy.name, policyNameValidator(siblings, originalPolicyName));

          originalProxyStageAction = currentPolicy.actions['proxy'];
          policiesByOwner.some(({ policies, ownerId, ownerName, ownerType }, index) => {
            if (policies.some(matchesPolicyId)) {
              isInherited = index !== 0;

              currentPolicyOwner.id = ownerId;
              currentPolicyOwner.name = ownerName;

              isOrgOwner = ownerType === 'organization';
              return true;
            }
          });
          currentPolicy = { ...currentPolicy, name };
          isRootOrg = currentPolicyOwner.id === 'ROOT_ORGANIZATION_ID';
        } else {
          const localOwner = policiesByOwner[0];

          currentPolicyOwner.id = localOwner.ownerId; // remove id if not needed.
          currentPolicyOwner.name = localOwner.ownerName;
          isOrgOwner = selectIsOrganization(state);
        }

        dispatch(
          constraintActions.loadConstraint({ isNewPolicy: isNil(policyId), constraints: currentPolicy.constraints })
        );
        dispatch(loadActionStages());

        if (isOrgOwner) {
          dispatch(loadCategoriesForPolicy(currentPolicy));
        }

        const ownerIds = policiesByOwner.map(prop('ownerId'));
        const actionsOverrideInfo = getActionsOverride(ownerIds, currentPolicy);
        const overrideActionsFlag = actionsOverrideInfo?.isCurrentOwnerOverride || false;
        return {
          siblings,
          currentPolicy,
          currentPolicyOwner,
          isInherited,
          isOrgOwner,
          isRootOrg,
          originalProxyStageAction,
          policiesByOwner,
          overrideActionsFlag,
        };
      })
      .catch(rejectWithValue);
  }
);

const loadPolicyEditorRequested = (state) => {
  state.loadingPolicyEditor = true;
  state.loadError = null;
};

const loadPolicyEditorFulfilled = (state, { payload }) => {
  state.loadingPolicyEditor = false;
  state.loadError = null;
  state.submitMaskState = null;
  state.isDirty = false;
  const {
    siblings,
    currentPolicy,
    currentPolicyOwner,
    isInherited,
    isOrgOwner,
    isRootOrg,
    originalProxyStageAction,
    overrideActionsFlag,
  } = payload;
  state.siblings = siblings;
  state.currentPolicy = currentPolicy;
  state.originalPolicy = currentPolicy;
  state.currentPolicyOwner = currentPolicyOwner;
  state.isInherited = isInherited;
  state.isOrgOwner = isOrgOwner;
  state.isRootOrg = isRootOrg;
  state.originalProxyStageAction = originalProxyStageAction;
  state.overrideActionsFlag = overrideActionsFlag;
  state.originalOverrideActionsFlag = overrideActionsFlag;
};

const loadPolicyEditorFailed = (state, { payload }) => {
  state.loadingPolicyEditor = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
  state.isDirty = false;
  state.currentPolicy = state.originalPolicy;
};

const checkEditIqPermission = createAsyncThunk(
  `${REDUCER_NAME}/checkEditIqPermission`,
  (_, { rejectWithValue, getState }) => {
    const state = getState();
    const ownerType = selectIsOrganization(state) ? 'organization' : 'application';
    const ownerId = selectSelectedOwnerId(state);
    return checkPermissions(['WRITE'], ownerType, ownerId).catch(rejectWithValue);
  }
);

const checkEditIqPermissionFulfilled = (state) => {
  state.hasEditIqPermission = true;
};

const checkEditIqPermissionFailed = (state) => {
  state.hasEditIqPermission = false;
};

function reloadPageAfterDuration(dispatch) {
  setTimeout(() => {
    dispatch(stateReload());
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

const removeHashKeys = omit(['$$hashKey']);
const removeNotificationHashKeys = map(removeHashKeys);

const extractRequestCurrentPolicy = (currentPolicy) => {
  const { constraints } = currentPolicy;
  const formattedConstraints = constraints.map((constraint) => ({
    ...constraint,
    name: constraint.name.trimmedValue,
    conditions: constraint.conditions.map((condition) => {
      return {
        ...condition,
        value: condition.value.trimmedValue
          ? condition.value.trimmedValue
          : typeof condition.value === 'string'
          ? condition.value
          : getCoordinatesValue(condition.value),
      };
    }),
  }));

  return {
    ...currentPolicy,
    constraints: formattedConstraints,
  };
};

const savePolicy = createAsyncThunk(`${REDUCER_NAME}/savePolicy`, (_, { getState, rejectWithValue, dispatch }) => {
  const state = getState();
  const isEditMode = selectIsEditMode(state);
  const { ownerType, ownerId } = selectOwnerProperties(state);
  const currentPolicy = extractRequestCurrentPolicy(selectCurrentPolicy(state));
  const hasPolicyCategories = selectHasPolicyCategories(state);
  const isOrgOwner = selectIsOrgOwner(state);
  const categories = selectCategories(state);
  const appliedCategories = categories?.filter(prop('isApplied')).map(omit(['isApplied'])) ?? [];
  const policyToSave = {
    ...currentPolicy,
    name: currentPolicy.name.trimmedValue,
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
      startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone);
      return { isEditMode };
    })
    .catch(rejectWithValue);
});

const savePolicyRequested = (state) => {
  state.loadError = null;
  state.submitMaskState = false;
};

const savePolicyFulfilled = (state, { payload }) => {
  state.loadError = null;
  state.isDirty = false;
  state.submitMaskState = true;

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
  state.loadError = Messages.getHttpErrorMessage(payload);
  state.submitMaskState = null;
};

const saveActionsOverrideFulfilled = (state, { payload }) => {
  state.loadError = null;
  state.isDirty = false;
  const policy = { ...payload, name: initUserInput(payload.name) };
  state.currentPolicy = policy;
  state.originalPolicy = policy;
  state.overrideActionsFlag = true;
  state.originalOverrideActionsFlag = true;
  state.submitMaskState = true;
};

const saveActionsOverride = createAsyncThunk(
  `${REDUCER_NAME}/saveActionsOverride`,
  (_, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const { id, policyActionsOverrides } = selectCurrentPolicy(state);
    const { ownerType, ownerId } = selectOwnerProperties(state);
    const ownerInternalId = selectSelectedOwnerId(state);
    return axios
      .put(getPolicyActionsOverridesUrl(ownerType, ownerId, id), policyActionsOverrides[ownerInternalId])
      .then(({ data: updatedPolicy }) => {
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone);
        return updatedPolicy;
      })
      .catch(rejectWithValue);
  }
);

const removeActionsOverride = createAsyncThunk(
  `${REDUCER_NAME}/removeActionsOverride`,
  (_, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const { id } = selectCurrentPolicy(state);
    const { ownerType, ownerId } = selectOwnerProperties(state);

    return axios
      .delete(getPolicyActionsOverridesUrl(ownerType, ownerId, id))
      .then(({ data: updatedPolicy }) => {
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone);
        return updatedPolicy;
      })
      .catch(rejectWithValue);
  }
);

const removeActionsOverrideFulfilled = (state, { payload }) => {
  state.loadError = null;
  state.isDirty = false;
  const policy = { ...payload, name: initUserInput(payload.name) };
  state.currentPolicy = policy;
  state.originalPolicy = policy;
  state.overrideActionsFlag = false;
  state.originalOverrideActionsFlag = false;
  state.submitMaskState = true;
};

const goToCreatePolicy = createAsyncThunk(`${REDUCER_NAME}/goToCreatePolicy`, (_, { getState, dispatch }) => {
  const router = selectRouterSlice(getState());
  const { to, params } = deriveEditRoute(router, 'create-policy');

  dispatch(stateGo(to, params));
});

const goToEditPolicy = createAsyncThunk(`${REDUCER_NAME}/goToEditPolicy`, (policyId, { getState, dispatch }) => {
  const router = selectRouterSlice(getState());
  const { to, params } = deriveEditRoute(router, 'policy', { policyId });

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
      dispatch(goToCreatePolicy());
      return policyToRemove.id;
    })
    .catch(rejectWithValue);
});

const removePolicyRequested = (state) => {
  state.deleteError = null;
  state.submitMaskState = false;
};

const removePolicyFulfilled = (state) => {
  state.deleteError = null;
  state.submitMaskState = true;
};

const removePolicyFailed = (state, { payload }) => {
  state.deleteError = Messages.getHttpErrorMessage(payload);
  state.submitMaskState = null;
};

const hasDirtyProps = (originalPolicy, currentPolicy, observedProps) => {
  return isNil(originalPolicy)
    ? any((prop) => !isEmpty(path(prop, currentPolicy)), observedProps)
    : any((prop) => !equals(path(prop, currentPolicy), path(prop, originalPolicy)), observedProps);
};

const isDirtyConstraints = (originalConstraints, currentConstraints) => {
  if (originalConstraints?.length !== currentConstraints?.length) return true;
  const dirty = anyIndexed((constrain, idx) => {
    const originalConstraint = originalConstraints?.[idx];

    const isNumberOfConditionsDifferent = originalConstraint?.conditions?.length !== constrain.conditions.length;
    if (isNumberOfConditionsDifferent) {
      return true;
    }

    const observedProps = [['name', 'trimmedValue'], ['operator']];
    const constraintHasDirtyProps = hasDirtyProps(originalConstraint, constrain, observedProps);

    const isAnyConditionDirty = anyIndexed((condition, conditionIdx) => {
      const originalCondition = originalConstraint.conditions[conditionIdx];
      const commonConditionPropDirty = any((prop) => !equals(condition[prop], originalCondition[prop]), [
        'conditionTypeId',
        'operator',
      ]);

      if (condition.conditionTypeId === 'Coordinates') {
        if (originalCondition.conditionTypeId !== 'Coordinates') {
          return true;
        }
        const currentValues = omit(['format'], mapObjIndexed(prop('trimmedValue'), condition.value));
        const originalValues = omit(['format'], mapObjIndexed(prop('trimmedValue'), originalCondition.value));

        const isValueDirty =
          !equals(currentValues, originalValues) || condition.value.format !== originalCondition.value.format;

        return commonConditionPropDirty || isValueDirty;
      }

      return commonConditionPropDirty || condition.value.trimmedValue !== originalCondition.value.trimmedValue;
    }, constrain.conditions);

    return constraintHasDirtyProps || isAnyConditionDirty;
  }, currentConstraints);

  return dirty;
};

const computeIsDirty = (state) => {
  const { currentPolicy, originalPolicy, isInherited, overrideActionsFlag, originalOverrideActionsFlag } = state;
  const isDirtyObservedProps = [
    ['name', 'value'],
    ['threatLevel'],
    ['policyViolationGrandfatheringAllowed'],
    ['policyActionsOverrideAllowed'],
    ['notifications'],
  ];
  const isDirtyActionsProps = [['actions'], ['policyActionsOverrides']];

  const isDirty = hasDirtyProps(originalPolicy, currentPolicy, isDirtyObservedProps);
  const isConstraintsDirty = isDirtyConstraints(originalPolicy?.constraints, currentPolicy.constraints);
  const isDirtyActions = hasDirtyProps(originalPolicy, currentPolicy, isDirtyActionsProps);
  const policyActionOverrideIsDirty = overrideActionsFlag !== originalOverrideActionsFlag;

  if (overrideActionsFlag && isDirtyActions) {
    return propSet('isDirty', isDirtyActions, state);
  }

  if (!policyActionOverrideIsDirty && isInherited) {
    return propSet('isDirty', isDirty, state);
  }

  const isContentDirty = isDirty || isConstraintsDirty;
  return propSet('isDirty', isContentDirty || (policyActionOverrideIsDirty && isInherited) || isDirtyActions, state);
};

const computeValidatableFieldsForCoordinates = (fields) => {
  if (fields.format === 'maven') {
    return values(omit(['format', isEmpty(fields.classifier?.trimmedValue) ? 'classifier' : null], fields));
  } else if (fields.format === 'a-name') {
    return values(omit(['format', isEmpty(fields.qualifier?.trimmedValue) ? 'qualifier' : null], fields));
  } else if (fields.format === 'pypi') {
    return values(
      omit(
        [
          'format',
          isEmpty(fields.qualifier?.trimmedValue) ? 'qualifier' : null,
          isEmpty(fields.extension?.trimmedValue) ? 'extension' : null,
        ],
        fields
      )
    );
  }
};

const computeValidationError = (state) => {
  const {
    currentPolicy: { constraints },
  } = state;

  const fields = [];
  constraints.forEach((constraint) => {
    fields.push(constraint.name);

    constraint.conditions.forEach((condition) => {
      if (!includes(condition.conditionTypeId, conditionsWithoutValue)) {
        if (condition.conditionTypeId === 'Coordinates') {
          fields.push(...computeValidatableFieldsForCoordinates(condition.value));
        } else {
          fields.push(condition.value);
        }
      }
    });
  });

  const validationError = validateForm(fields);
  return propSetConst('validationError', validationError, state);
};

const updatedComputedProps = compose(computeIsDirty, computeValidationError);

const setPolicyField = curryN(3, function setPolicyField(fieldName, state, { payload }) {
  return updatedComputedProps(pathSet(['currentPolicy', fieldName], payload, state));
});

const setPolicyNameField = curryN(2, function setPolicyField(state, { payload }) {
  const siblings = state.siblings;
  const originalPolicyName = state.originalPolicy.name.value;
  const fieldValue = userInput(policyNameValidator(siblings, originalPolicyName), payload);
  return updatedComputedProps(pathSet(['currentPolicy', 'name'], fieldValue, state));
});

const setOverrideParentActions = (state) => computeIsDirty({ ...state, overrideActionsFlag: true });

const unSetOverrideParentActions = (state, { payload }) => {
  const ownerId = payload;
  const currentActionsOverrides = state.currentPolicy.policyActionsOverrides || {};
  const updatedActionsOverrides = omit([ownerId], currentActionsOverrides);
  const newState = { ...state, overrideActionsFlag: false };
  return updatedComputedProps(pathSet(['currentPolicy', 'policyActionsOverrides'], updatedActionsOverrides, newState));
};

const toggleField = curryN(2, function toggleField(fieldName, state) {
  return updatedComputedProps(pathSet(['currentPolicy', fieldName], !state.currentPolicy[fieldName], state));
});

const setConstraintField = curryN(3, function setConstraintField(fieldName, state, { payload }) {
  const { constraintIndex, value } = payload;
  return updatedComputedProps(pathSet(['currentPolicy', 'constraints', constraintIndex, fieldName], value, state));
});

const setConstraintNameField = curryN(3, function setConstraintNameField(fieldName, state, { payload }) {
  const { constraintIndex, value, id } = payload;

  const duplicationValidator = () => {
    const exists = any(
      (item) => item.name?.trimmedValue?.toLowerCase() === value.toLowerCase() && id !== item.id,
      state.currentPolicy.constraints
    );
    return exists ? 'Name is already in use' : null;
  };

  const constraintNameValidator = combineValidators([validateNonEmpty, duplicationValidator]);
  const newValue = userInput(constraintNameValidator, value, state);

  return updatedComputedProps(pathSet(['currentPolicy', 'constraints', constraintIndex, fieldName], newValue, state));
});

const setNotifications = curryN(3, function setNotifications(notificationType, state, { payload }) {
  return updatedComputedProps(pathSet(['currentPolicy', 'notifications', notificationType], payload, state));
});

const setNotificationStageIds = curryN(3, function setNotificationStageIds(notificationType, state, { payload }) {
  const { index, value } = payload;
  return updatedComputedProps(
    pathSet(['currentPolicy', 'notifications', notificationType, index, 'stageIds'], value, state)
  );
});

const setConstraintConditionField = curryN(3, function setConstraintConditionField(fieldName, state, { payload }) {
  const { constraintIndex, conditionIndex, value } = payload;
  return updatedComputedProps(
    pathSet(['currentPolicy', 'constraints', constraintIndex, 'conditions', conditionIndex, fieldName], value, state)
  );
});

const setConstraintConditionAgeField = curryN(2, function setConstraintConditionAgeField(state, { payload }) {
  const { constraintIndex, conditionIndex, value } = payload;
  const newValue = userInput(ageValidator, value, state);

  return updatedComputedProps(
    pathSet(['currentPolicy', 'constraints', constraintIndex, 'conditions', conditionIndex, 'value'], newValue, state)
  );
});

const setConstraintConditionFieldByDataType = curryN(
  2,
  function setConstraintConditionFieldByDataType(state, { payload }) {
    const { constraintIndex, conditionIndex, value, dataType, valueTypeId } = payload;

    const validator = valueTypeIdValidatorMap.has(valueTypeId)
      ? valueTypeIdValidatorMap.get(valueTypeId)
      : dataTypeValidatorsMap.get(dataType);

    const newValue = userInput(validator, value, state);
    return updatedComputedProps(
      pathSet(['currentPolicy', 'constraints', constraintIndex, 'conditions', conditionIndex, 'value'], newValue, state)
    );
  }
);

const setConstraintCoordinatesInput = curryN(2, function setConstraintCoordinatesInput(state, { payload }) {
  const { constraintIndex, conditionIndex, value, name } = payload;
  const newValue = userInput(getCoordinatesValidator(name), value, state);
  return updatedComputedProps(
    pathSet(
      ['currentPolicy', 'constraints', constraintIndex, 'conditions', conditionIndex, 'value', name],
      newValue,
      state
    )
  );
});

const initCoordinatesFields = (value) => {
  const conditionValue = {
    format: value,
  };

  coordinatesTypes[value].forEach((field) => {
    conditionValue[field] = initUserInput(withDefaultValue.includes(field) ? '*' : '');
  });

  return conditionValue;
};

const setConstraintCoordinatesFormat = curryN(2, function setConstraintCoordinatesFormat(state, { payload }) {
  const { constraintIndex, conditionIndex, value } = payload;
  return updatedComputedProps(
    pathSet(
      ['currentPolicy', 'constraints', constraintIndex, 'conditions', conditionIndex, 'value'],
      initCoordinatesFields(value),
      state
    )
  );
});

const setConstraintCondition = curryN(2, function setConstraintCondition(state, { payload }) {
  const { constraintIndex, conditionIndex, value } = payload;
  const { conditionTypeId } = value;

  if (conditionTypeId === 'Coordinates') {
    return updatedComputedProps(
      pathSet(
        ['currentPolicy', 'constraints', constraintIndex, 'conditions', conditionIndex],
        { ...value, value: initCoordinatesFields(coordinatesFormatOptions[0]) },
        state
      )
    );
  }

  return updatedComputedProps(
    pathSet(
      ['currentPolicy', 'constraints', constraintIndex, 'conditions', conditionIndex],
      { ...value, value: initUserInput(value.value) },
      state
    )
  );
});

const toggleCategoryIsApplied = (state, { payload: index }) => {
  state.categories[index].isApplied = !state.categories[index].isApplied;
};

const togglePolicyActionsOverrideAllowed = (state) => {
  const currentPolicyActionsOverrideAllowed = state.currentPolicy.policyActionsOverrideAllowed;
  if (currentPolicyActionsOverrideAllowed) {
    const newState = {
      ...state,
      currentPolicy: {
        ...state.currentPolicy,
        policyActionsOverrideAllowed: !currentPolicyActionsOverrideAllowed,
        policyActionsOverrides: null,
      },
    };
    return updatedComputedProps(newState);
  } else {
    return toggleField('policyActionsOverrideAllowed')(state);
  }
};

const loadNotificationWebhooks = createAsyncThunk(
  `${REDUCER_NAME}/loadNotificationWebhooks`,
  (_, { getState, rejectWithValue }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerInfo(state);
    return axios.get(getNotificationWebhooksUrl(ownerType, ownerId)).then(prop('data')).catch(rejectWithValue);
  }
);

const resetNotificationsEditorFormState = (state) => {
  const { recipientType } = state.notificationsEditor.formState ?? {};
  return pathSet(
    ['notificationsEditor', 'formState'],
    { ...initialState.notificationsEditor.formState, recipientType },
    state
  );
};

const addEmailRecipient = (state, emailAddress) => {
  const { userNotifications = [] } = state.currentPolicy?.notifications ?? {};
  const emailExists = userNotifications.some((item) => item.emailAddress === emailAddress);
  if (!emailExists) {
    const newNotification = { emailAddress, stageIds: [] };
    const payload = userNotifications.concat(newNotification);
    return setNotifications('userNotifications', state, { payload });
  }
};

const addRoleRecipient = (state, roleId) => {
  const { roleNotifications = [] } = state.currentPolicy?.notifications ?? {};
  const newNotification = { roleId, stageIds: [] };
  const payload = roleNotifications.concat(newNotification);
  return setNotifications('roleNotifications', state, { payload });
};

const addWebhookRecipient = (state, webhookId) => {
  const { webhookNotifications = [] } = state.currentPolicy?.notifications ?? {};
  const newNotification = { webhookId, stageIds: [] };
  const payload = webhookNotifications.concat(newNotification);
  return setNotifications('webhookNotifications', state, { payload });
};

const addJiraRecipient = (state, projectKey, issueTypeId) => {
  const { jiraNotifications = [] } = state.currentPolicy?.notifications ?? {};
  const newNotification = { projectKey, issueTypeId, stageIds: [] };
  const payload = jiraNotifications.concat(newNotification);
  return setNotifications('jiraNotifications', state, { payload });
};

const addNotificationRecipient = (originalState) => {
  const values = map(prop('trimmedValue'), originalState?.notificationsEditor?.formState);
  const {
    recipientType,
    recipientEmail,
    recipientRoleId,
    recipientWebhookId,
    recipientProjectKey,
    recipientIssueTypeId,
  } = values ?? {};

  const state = resetNotificationsEditorFormState(originalState);

  switch (recipientType) {
    case RECIPIENT_TYPES.EMAIL:
      return addEmailRecipient(state, recipientEmail);
    case RECIPIENT_TYPES.ROLE:
      return addRoleRecipient(state, recipientRoleId);
    case RECIPIENT_TYPES.WEBHOOK:
      return addWebhookRecipient(state, recipientWebhookId);
    case RECIPIENT_TYPES.JIRA:
      return addJiraRecipient(state, recipientProjectKey, recipientIssueTypeId);
  }
};

const removeNotificationRecipient = (state, { payload }) => {
  const { recipient } = payload;
  const removeRecipientFrom = without([omit(['displayName'], recipient)]);
  const setNotificationsFor = (notificationType, payload) => setNotifications(notificationType, state, { payload });
  const { webhookNotifications = [], userNotifications = [], roleNotifications = [], jiraNotifications = [] } =
    state.currentPolicy?.notifications ?? {};

  if (recipient.roleId) {
    return setNotificationsFor('roleNotifications', removeRecipientFrom(roleNotifications));
  } else if (recipient.emailAddress) {
    return setNotificationsFor('userNotifications', removeRecipientFrom(userNotifications));
  } else if (recipient.webhookId) {
    return setNotificationsFor('webhookNotifications', removeRecipientFrom(webhookNotifications));
  } else if (recipient.projectKey) {
    return setNotificationsFor('jiraNotifications', removeRecipientFrom(jiraNotifications));
  }
};

const toggleNotificationRecipientStage = (state, { payload }) => {
  const { recipient, stageId } = payload;
  const { webhookNotifications = [], userNotifications = [], roleNotifications = [], jiraNotifications = [] } =
    state.currentPolicy?.notifications ?? {};
  const setStageIdsFor = (notificationType, payload) => setNotificationStageIds(notificationType, state, { payload });
  const updatedStageIds = recipient.stageIds.includes(stageId)
    ? without([stageId], recipient.stageIds)
    : recipient.stageIds.concat(stageId);
  if (recipient.roleId) {
    return setStageIdsFor('roleNotifications', {
      index: findIndex(propEq('roleId', recipient.roleId), roleNotifications),
      value: updatedStageIds,
    });
  } else if (recipient.emailAddress) {
    return setStageIdsFor('userNotifications', {
      index: findIndex(propEq('emailAddress', recipient.emailAddress), userNotifications),
      value: updatedStageIds,
    });
  } else if (recipient.webhookId) {
    return setStageIdsFor('webhookNotifications', {
      index: findIndex(propEq('webhookId', recipient.webhookId), webhookNotifications),
      value: updatedStageIds,
    });
  } else if (recipient.projectKey) {
    return setStageIdsFor('jiraNotifications', {
      index: findIndex(propEq('projectKey', recipient.projectKey), jiraNotifications),
      value: updatedStageIds,
    });
  }
};

export const loadRolesForCurrentOwner = createAsyncThunk(
  `${REDUCER_NAME}/loadRolesForCurrentOwner`,
  (_, { rejectWithValue, getState }) => {
    const state = getState();
    const { ownerType, ownerId } = selectOwnerInfo(state);
    const roles = selectRolesForCurrentOwner(state);

    if (roles) return { membersByRole: roles };

    return axios.get(getRoleMappingForCurrentOwnerUrl(ownerType, ownerId)).then(prop('data')).catch(rejectWithValue);
  }
);

const setNotificationsEditorFormFieldValue = (state, { payload }) => {
  const { field, value } = payload;
  const { userNotifications = [] } = state?.currentPolicy?.notifications ?? {};

  const hasSameEmailAddress = (val) => (notification) => notification.emailAddress === val;
  const isEmailAlreadyInUse = (error) => (val) =>
    userNotifications?.some(hasSameEmailAddress(val)) ? error : undefined;

  const validateEmail = combineValidators([
    isEmailAlreadyInUse('Email already exists'),
    validateEmailPatternMatch('Use valid format: abc@xyz.com'),
  ]);

  const validate = field === 'recipientEmail' ? validateEmail : always(undefined);

  return pathSet(
    ['notificationsEditor', 'formState', field],
    userInput(validate, value),
    field === 'recipientType' ? resetNotificationsEditorFormState(state) : state
  );
};

const loadJiraProjects = createAsyncThunk(`${REDUCER_NAME}/loadJiraProjects`, async (_, { rejectWithValue }) => {
  try {
    const isJiraEnabled = await axios.get(getIsJiraEnabledUrl()).then(prop('data'));

    if (!isJiraEnabled) {
      return { isJiraEnabled, projects: null };
    }

    const projects = await axios.get(getJiraProjectsUrl()).then(prop('data'));
    return { isJiraEnabled, projects };
  } catch (error) {
    return rejectWithValue(error);
  }
});

export const loadNotificationsEditor = createAsyncThunk(
  `${REDUCER_NAME}/loadNotificationsEditor`,
  (_, { dispatch, rejectWithValue }) =>
    Promise.all([
      dispatch(loadRolesForCurrentOwner()),
      dispatch(loadNotificationWebhooks()),
      dispatch(loadJiraProjects()),
    ])
      .then((results) => {
        const { membersByRole } = unwrapResult(results[0]);
        const notificationWebhooks = unwrapResult(results[1]);
        const { isJiraEnabled, projects } = unwrapResult(results[2]);

        return {
          membersByRole,
          notificationWebhooks,
          isJiraEnabled,
          projects,
        };
      })
      .catch(rejectWithValue)
);

const loadNotificationsEditorRequested = (state) => {
  state.notificationsEditor.loading = true;
  state.notificationsEditor.loadError = null;
  state.notificationsEditor.formState = initialState.notificationsEditor.formState;
};

const loadNotificationsEditorFulfilled = (state, { payload }) => {
  const { isJiraEnabled, projects: jiraProjects, notificationWebhooks, membersByRole: roles } = payload;
  state.notificationsEditor.loading = false;
  state.notificationsEditor.loadError = null;
  state.notificationsEditor.isJiraEnabled = isJiraEnabled;
  state.notificationsEditor.jiraProjects = jiraProjects;
  state.notificationsEditor.notificationWebhooks = notificationWebhooks;
  state.notificationsEditor.roles = roles;
};

const loadNotificationsEditorFailed = (state, { payload }) => {
  state.notificationsEditor.loading = false;
  state.notificationsEditor.loadError = Messages.getHttpErrorMessage(payload);
};

const policySlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    resetIsDirty: propSetConst('isDirty', initialState.isDirty),
    setHasPolicyCategories: reduxPropSet('hasPolicyCategories'),
    toggleCategoryIsApplied,
    togglePolicyViolationGrandfatheringAllowed: toggleField('policyViolationGrandfatheringAllowed'),
    togglePolicyActionsOverrideAllowed,
    setPolicyName: setPolicyNameField(),
    setThreatLevel: setPolicyField('threatLevel'),
    setActions: setPolicyField('actions'),
    setActionsOverride(state, { payload }) {
      const { ownerId, actionsOverride } = payload;
      const currentActionsOverrides = state.currentPolicy.policyActionsOverrides || {};
      const updatedActionsOverrides = { ...currentActionsOverrides, [ownerId]: actionsOverride };
      return updatedComputedProps(pathSet(['currentPolicy', 'policyActionsOverrides'], updatedActionsOverrides, state));
    },
    setConstraint: setPolicyField('constraints'),
    setUserNotifications: setNotifications('userNotifications'),
    setRoleNotifications: setNotifications('roleNotifications'),
    setJiraNotifications: setNotifications('jiraNotifications'),
    setWebhookNotifications: setNotifications('webhookNotifications'),
    setUserNotificationStageIds: setNotificationStageIds('userNotifications'),
    setRoleNotificationStageIds: setNotificationStageIds('roleNotifications'),
    setJiraNotificationStageIds: setNotificationStageIds('jiraNotifications'),
    setWebhookNotificationStageIds: setNotificationStageIds('webhookNotifications'),
    addNotificationRecipient,
    removeNotificationRecipient,
    toggleNotificationRecipientStage,
    setNotificationsEditorFormFieldValue,
    addCondition: setConstraintField('conditions'),
    deleteCondition: setConstraintField('conditions'),
    setCondition: setConstraintField('conditions'),
    setConstraintName: setConstraintNameField('name'),
    setConstraintCondition,
    setConstraintOperator: setConstraintField('operator'),
    setConditionOperator: setConstraintConditionField('operator'),
    setConditionValue: setConstraintConditionField('value'),
    setConditionAgeValue: setConstraintConditionAgeField,
    setConstraintCoordinatesFormat,
    setConstraintCoordinatesInput,
    setMultiInputConditionValue: setConstraintConditionFieldByDataType,
    setOverrideParentActions,
    unSetOverrideParentActions,
    changeSortField,
    saveMaskTimerDone: propSet('submitMaskState', null),
    clearDeleteError: propSet('deleteError', null),
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

    [loadPolicyTile.pending]: loadPolicyTileRequested,
    [loadPolicyTile.fulfilled]: loadPolicyTileFulfilled,
    [loadPolicyTile.rejected]: loadPolicyTileFailed,

    [removePolicy.pending]: removePolicyRequested,
    [removePolicy.fulfilled]: removePolicyFulfilled,
    [removePolicy.rejected]: removePolicyFailed,
    [saveActionsOverride.pending]: savePolicyRequested,
    [saveActionsOverride.fulfilled]: saveActionsOverrideFulfilled,
    [saveActionsOverride.rejected]: savePolicyFailed,

    [removeActionsOverride.pending]: savePolicyRequested,
    [removeActionsOverride.fulfilled]: removeActionsOverrideFulfilled,
    [removeActionsOverride.rejected]: savePolicyFailed,

    [checkEditIqPermission.fulfilled]: checkEditIqPermissionFulfilled,
    [checkEditIqPermission.rejected]: checkEditIqPermissionFailed,

    [loadNotificationsEditor.pending]: loadNotificationsEditorRequested,
    [loadNotificationsEditor.fulfilled]: loadNotificationsEditorFulfilled,
    [loadNotificationsEditor.rejected]: loadNotificationsEditorFailed,
  },
});

export const actions = {
  ...policySlice.actions,
  loadCategoriesForPolicy,
  loadPolicyEditor,
  loadPolicyTile,
  savePolicy,
  removePolicy,
  saveActionsOverride,
  removeActionsOverride,
  checkEditIqPermission,
  loadNotificationWebhooks,
  loadNotificationsEditor,
  goToCreatePolicy,
  goToEditPolicy,
};

export default policySlice.reducer;
