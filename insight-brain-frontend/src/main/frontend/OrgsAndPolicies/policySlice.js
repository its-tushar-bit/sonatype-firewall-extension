/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  always,
  includes,
  curryN,
  findIndex,
  isNil,
  lensProp,
  lensPath,
  map,
  not,
  omit,
  over,
  pick,
  prop,
  propEq,
  without,
  sortWith,
  reverse,
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
} from 'MainRoot/util/validationUtil';

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import {
  selectIsOrganization,
  selectIsRootOrganization,
  selectRouterSlice,
  selectRouterCurrentParams,
  selectOwnerInfo,
  selectIsRepositoriesRelated,
  selectIsRepositoryManager,
  selectIsRepositoryContainer,
  selectIsRepository,
  selectIsSbomManager,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  getNotificationWebhooksUrl,
  getPolicyOverridesUrl,
  getPolicyCRUDUrl,
  getPolicyTagUrl,
  getPolicyUrl,
  getRoleMappingForCurrentOwnerUrl,
  getJiraProjectsUrl,
  getIsJiraEnabledUrl,
  getPolicyNotificationsUrl,
} from '../util/CLMLocation';
import {
  selectActionsOverrideNeedsToBeAdded,
  selectActionsOverrideNeedsToBeRemoved,
  selectActionsOverrideNeedsToBeUpdated,
  selectCategories,
  selectCurrentPolicy,
  selectHasPolicyCategories,
  selectIsEditMode,
  selectIsOrgOwner,
  selectNotificationsOverrideNeedsToBeAdded,
  selectNotificationsOverrideNeedsToBeRemoved,
  selectNotificationsOverrideNeedsToBeUpdated,
  selectOriginalPolicyName,
  selectRolesForCurrentOwner,
} from './policySelectors';
import { actions as applicationCategoriesActions } from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/createEditApplicationCategoriesSlice';
import {
  deriveEditRoute,
  policiesComparator,
  getActionsOverride,
  getNotificationsOverride,
} from 'MainRoot/OrgsAndPolicies/utility/util';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { propSet, pathSet, allEqual } from 'MainRoot/util/jsUtil';
import { propSet as reduxPropSet } from 'MainRoot/util/reduxToolkitUtil';
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
  withDefaultValue,
  constraintNameValidator,
} from 'MainRoot/OrgsAndPolicies/utility/constraintUtil';
import { loadActionStages, loadSbomStages } from './stagesSlice';
import { startSaveMaskSuccessTimer, toggleBooleanProp } from 'MainRoot/util/reduxUtil';

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
  overrideNotificationsFlag: null,
  originalOverrideNotificationsFlag: null,
  showActionsOverridesConfirmationModal: false,
  showNotificationsOverridesConfirmationModal: false,
  currentPolicy: {
    id: undefined,
    name: initUserInput('', policyNameValidator([], '')),
    threatLevel: 5,
    legacyViolationAllowed: null,
    policyActionsOverrideAllowed: null,
    actions: {},
    policyActionsOverrides: null,
    policyNotificationsOverrideAllowed: null,
    notifications: {
      userNotifications: [],
      roleNotifications: [],
      jiraNotifications: [],
      webhookNotifications: [],
    },
    policyNotificationsOverrides: null,
    constraints: [
      {
        id: '' + new Date().getTime(),
        name: initUserInput('', constraintNameValidator([], '')),
        conditions: [
          {
            conditionTypeId: 'AgeInDays',
            operator: 'older than',
            value: initUserInput('', ageValidator),
          },
        ],
        operator: 'OR',
      },
    ],
  },
  currentPolicyOwner: null,
  originalPolicy: null,
  originalCategories: null,
  categories: null,
  hasPolicyCategories: false,
  originalHasPolicyCategories: false,
  siblings: [],
  isInherited: undefined,
  isOrgOwner: false,
  isRepositoryContainerOwner: false,
  isRepositoryManagerOwner: false,
  isRepositoryOwner: false,
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
  submitMaskState: null,
  policyTile: {
    loading: false,
    loadError: null,
    policiesByOwner: null,
    collapsibleSorting: {
      key: null,
      dir: null,
    },
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
  state.policyTile.collapsibleSorting = setInitialSorting();
  state.policyTile.policiesByOwner = sortCollapsibleItemsByField(
    cleanPoliciesByOwner(payload),
    state.policyTile.collapsibleSorting
  );
};

const loadPolicyTileFailed = (state, { payload }) => {
  state.policyTile.loading = false;
  state.policyTile.loadError = Messages.getHttpErrorMessage(payload);
};

const setInitialSorting = () => ({
  key: 'threatLevel',
  dir: 'desc',
});

const checkAreStageValuesEqual = (key, policies) => {
  if (includes(key, ['name', 'threatLevel'])) {
    return false;
  }
  const values = policies.map(
    (policy) => policy[isNil(policy.hasLocalActionsOverrides) ? 'actions' : 'enforcementAction'][key]
  );
  return allEqual(values);
};

// Remove potentially large unneeded fields from policiesByOwner
const cleanPoliciesByOwner = map(
  over(
    lensProp('policies'),
    map(omit(['notifications', 'constraints', 'policyNotificationsOverrides', 'policyActionsOverrides']))
  )
);

const sortCollapsibleItemsByField = (policiesByOwner, updatedSorting) => {
  const { dir, key } = updatedSorting;

  return map((owner) => {
    let policies = owner.policies;
    const customSort = sortWith(policiesComparator(prop(key), key));
    const equalValues = checkAreStageValuesEqual(key, policies);

    if (!equalValues) {
      policies = dir === 'asc' ? customSort(policies) : reverse(customSort(policies));
    }

    return {
      ...owner,
      policies,
    };
  }, policiesByOwner);
};

const changeCollapsibleSortField = (state, { payload }) => {
  state.policyTile.collapsibleSorting = payload;
  state.policyTile.policiesByOwner = sortCollapsibleItemsByField(state.policyTile.policiesByOwner, payload);
};

const convertMatcherToUserInput = (policy) => {
  const constraints = policy.constraints.map((constraint) => {
    const conditions = constraint.conditions.map((condition) => {
      let value = initUserInput(condition.value ?? '', validateNonEmpty);

      if (condition.conditionTypeId === 'Coordinates') {
        const parts = condition.value.split(':');
        value = {
          format: parts.shift(),
        };
        parts.forEach((part, partIdx) => {
          const field = coordinatesTypes[value.format][partIdx];
          value[field] = initUserInput(part, getCoordinatesValidator(field, value.format));
        });
      } else if (condition.conditionTypeId === 'AgeInDays') {
        value = initUserInput(condition.value ?? '', ageValidator);
      }
      return { ...condition, value };
    });

    return {
      ...constraint,
      conditions,
      name: initUserInput(constraint.name, constraintNameValidator(policy.constraints, constraint.name, constraint.id)),
    };
  });

  return { ...policy, constraints };
};

const loadPolicyEditor = createAsyncThunk(
  `${REDUCER_NAME}/loadPolicyEditor`,
  async (_, { getState, rejectWithValue, dispatch }) => {
    await dispatch(rootActions.loadSelectedOwner());
    dispatch(actions.checkEditIqPermission());
    return dispatch(rootActions.loadApplicablePoliciesByOwner())
      .then((loadApplicablePoliciesByOwnerAction) => {
        const state = getState();
        const isRepositories = selectIsRepositoriesRelated(state);
        const isSbomManager = selectIsSbomManager(state);
        const policiesByOwner = unwrapResult(loadApplicablePoliciesByOwnerAction);
        const siblings = policiesByOwner.flatMap(prop('policies'));

        const { policyId } = selectRouterCurrentParams(state);
        let currentPolicy = initialState.currentPolicy;
        let isInherited;
        let isOrgOwner;
        let isRepositoryContainerOwner;
        let isRepositoryManagerOwner;
        let isRepositoryOwner;
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
              isRepositoryContainerOwner = ownerType === 'repository_container';
              isRepositoryManagerOwner = ownerType === 'repository_manager';
              isRepositoryOwner = ownerType === 'repository';
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
          isRepositoryContainerOwner = selectIsRepositoryContainer(state);
          isRepositoryManagerOwner = selectIsRepositoryManager(state);
          isRepositoryOwner = selectIsRepository(state);
        }

        dispatch(
          constraintActions.loadConstraint({ isNewPolicy: isNil(policyId), constraints: currentPolicy.constraints })
        );

        dispatch(isSbomManager ? loadSbomStages() : loadActionStages());

        if (isOrgOwner && !isRepositories) {
          dispatch(loadCategoriesForPolicy(currentPolicy));
        }

        const ownerIds = policiesByOwner.map(prop('ownerId'));
        const actionsOverrideInfo = getActionsOverride(ownerIds, currentPolicy);
        const overrideActionsFlag = actionsOverrideInfo?.isCurrentOwnerOverride || false;
        const notificationsOverrideInfo = getNotificationsOverride(ownerIds, currentPolicy);
        const overrideNotificationsFlag = notificationsOverrideInfo?.isCurrentOwnerOverride || false;
        return {
          siblings,
          currentPolicy,
          currentPolicyOwner,
          isInherited,
          isOrgOwner,
          isRepositoryContainerOwner,
          isRepositoryManagerOwner,
          isRepositoryOwner,
          isRootOrg,
          originalProxyStageAction,
          policiesByOwner,
          overrideActionsFlag,
          overrideNotificationsFlag,
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
  const {
    siblings,
    currentPolicy,
    currentPolicyOwner,
    isInherited,
    isOrgOwner,
    isRepositoryContainerOwner,
    isRepositoryManagerOwner,
    isRepositoryOwner,
    isRootOrg,
    originalProxyStageAction,
    overrideActionsFlag,
    overrideNotificationsFlag,
  } = payload;
  state.siblings = siblings;
  state.currentPolicy = currentPolicy;
  state.originalPolicy = currentPolicy;
  state.currentPolicyOwner = currentPolicyOwner;
  state.isInherited = isInherited;
  state.isOrgOwner = isOrgOwner;
  state.isRepositoryContainerOwner = isRepositoryContainerOwner;
  state.isRepositoryManagerOwner = isRepositoryManagerOwner;
  state.isRepositoryOwner = isRepositoryOwner;
  state.isRootOrg = isRootOrg;
  state.originalProxyStageAction = originalProxyStageAction;
  state.overrideActionsFlag = overrideActionsFlag;
  state.originalOverrideActionsFlag = overrideActionsFlag;
  state.overrideNotificationsFlag = overrideNotificationsFlag;
  state.originalOverrideNotificationsFlag = overrideNotificationsFlag;
};

const loadPolicyEditorFailed = (state, { payload }) => {
  state.loadingPolicyEditor = false;
  state.loadError = Messages.getHttpErrorMessage(payload);
  state.currentPolicy = state.originalPolicy ?? initialState.currentPolicy;
};

const checkEditIqPermission = createAsyncThunk(
  `${REDUCER_NAME}/checkEditIqPermission`,
  (_, { rejectWithValue, getState }) => {
    const state = getState();
    const isRepositories = selectIsRepositoriesRelated(state);
    const isRepositoryManager = selectIsRepositoryManager(state);
    const ownerType = getOwnerType(state);
    const ownerId = isRepositories && !isRepositoryManager ? 'REPOSITORY_CONTAINER_ID' : selectSelectedOwnerId(state);
    return checkPermissions(['WRITE'], ownerType, ownerId).catch(rejectWithValue);
  }
);

const getOwnerType = (state) => {
  const isRepositories = selectIsRepositoriesRelated(state);
  if (isRepositories) {
    return 'repository_container';
  }
  return selectIsOrganization(state) ? 'organization' : 'application';
};

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
  const isSbomManager = selectIsSbomManager(state);
  const appliedCategories = categories?.filter(prop('isApplied')).map(omit(['isApplied'])) ?? [];
  const policyToSave = {
    ...currentPolicy,
    name: currentPolicy.name.trimmedValue,
    notifications: map(removeNotificationHashKeys, currentPolicy.notifications),
  };
  const categoriesToSave = hasPolicyCategories ? appliedCategories : [];
  const apiUrlToCall = isSbomManager ? getPolicyNotificationsUrl(ownerType, ownerId) : getPolicyUrl(ownerType, ownerId);

  return axios[isEditMode ? 'put' : 'post'](apiUrlToCall, policyToSave)
    .then(({ data: savedPolicy }) =>
      isOrgOwner && !isSbomManager
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

const updateOverrides = createAsyncThunk(
  `${REDUCER_NAME}/updateOverrides`,
  (_, { getState, rejectWithValue, dispatch }) => {
    const state = getState();
    const actionsOverrideNeedsToBeAdded = selectActionsOverrideNeedsToBeAdded(state);
    const actionsOverrideNeedsToBeRemoved = selectActionsOverrideNeedsToBeRemoved(state);
    const actionsOverrideNeedsToBeUpdated = selectActionsOverrideNeedsToBeUpdated(state);
    const notificationsOverrideNeedsToBeAdded = selectNotificationsOverrideNeedsToBeAdded(state);
    const notificationsOverrideNeedsToBeRemoved = selectNotificationsOverrideNeedsToBeRemoved(state);
    const notificationsOverrideNeedsToBeUpdated = selectNotificationsOverrideNeedsToBeUpdated(state);
    const { id, policyActionsOverrides, policyNotificationsOverrides } = selectCurrentPolicy(state);
    const { ownerType, ownerId } = selectOwnerProperties(state);
    const ownerInternalId = selectSelectedOwnerId(state);

    const payload = {};
    // check if the overrides need to be added/removed/updated and set the data appropriately, check update last
    // because it could be true along with one of the other flags, but addition/removal should take precedence
    if (actionsOverrideNeedsToBeAdded) {
      payload.actions = policyActionsOverrides ? policyActionsOverrides[ownerInternalId] : null;
    } else if (actionsOverrideNeedsToBeRemoved) {
      payload.actions = null;
    } else if (actionsOverrideNeedsToBeUpdated) {
      payload.actions = policyActionsOverrides ? policyActionsOverrides[ownerInternalId] : null;
    }
    if (notificationsOverrideNeedsToBeAdded) {
      payload.notifications = policyNotificationsOverrides ? policyNotificationsOverrides[ownerInternalId] : null;
    } else if (notificationsOverrideNeedsToBeRemoved) {
      payload.notifications = null;
    } else if (notificationsOverrideNeedsToBeUpdated) {
      payload.notifications = policyNotificationsOverrides ? policyNotificationsOverrides[ownerInternalId] : null;
    }

    return axios
      .put(getPolicyOverridesUrl(ownerType, ownerId, id), payload)
      .then(({ data: updatedPolicy }) => {
        startSaveMaskSuccessTimer(dispatch, actions.saveMaskTimerDone);
        return updatedPolicy;
      })
      .catch(rejectWithValue);
  }
);

const updateOverridesFulfilled = (state, { payload }) => {
  const stateToSelectFrom = { orgsAndPolicies: { policy: state } };
  const actionsOverrideNeedsToBeAdded = selectActionsOverrideNeedsToBeAdded(stateToSelectFrom);
  const actionsOverrideNeedsToBeRemoved = selectActionsOverrideNeedsToBeRemoved(stateToSelectFrom);
  const notificationsOverrideNeedsToBeAdded = selectNotificationsOverrideNeedsToBeAdded(stateToSelectFrom);
  const notificationsOverrideNeedsToBeRemoved = selectNotificationsOverrideNeedsToBeRemoved(stateToSelectFrom);

  if (actionsOverrideNeedsToBeAdded) {
    state.overrideActionsFlag = true;
    state.originalOverrideActionsFlag = true;
  } else if (actionsOverrideNeedsToBeRemoved) {
    state.overrideActionsFlag = false;
    state.originalOverrideActionsFlag = false;
  }

  if (notificationsOverrideNeedsToBeAdded) {
    state.overrideNotificationsFlag = true;
    state.originalOverrideNotificationsFlag = true;
  } else if (notificationsOverrideNeedsToBeRemoved) {
    state.overrideNotificationsFlag = false;
    state.originalOverrideNotificationsFlag = false;
  }

  state.loadError = null;

  // note: the server response (payload) mangles the values of coordinate policy conditions. So we can't just
  // take the full payload as the policy object
  const policy = { ...payload, name: initUserInput(payload.name, policyNameValidator([], '')) };
  Object.assign(state.currentPolicy, pick(['policyActionsOverrides', 'policyNotificationsOverrides', 'name'], policy));
  Object.assign(state.originalPolicy, pick(['policyActionsOverrides', 'policyNotificationsOverrides', 'name'], policy));

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
      dispatch(policySlice.actions.resetState());
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

const setPolicyField = curryN(3, function setPolicyField(fieldName, state, { payload }) {
  return pathSet(['currentPolicy', fieldName], payload, state);
});

const setPolicyNameField = curryN(2, function setPolicyField(state, { payload }) {
  const siblings = state.siblings;
  const originalPolicyName = state.originalPolicy.name.value;
  const fieldValue = userInput(policyNameValidator(siblings, originalPolicyName), payload);
  return pathSet(['currentPolicy', 'name'], fieldValue, state);
});

const setOverrideParentActions = (state) => ({ ...state, overrideActionsFlag: true });

const setOverrideParentNotifications = (state) => ({ ...state, overrideNotificationsFlag: true });

const unSetOverrideParentActions = (state, { payload }) => {
  const ownerId = payload;
  const currentActionsOverrides = state.currentPolicy.policyActionsOverrides || {};
  const updatedActionsOverrides = omit([ownerId], currentActionsOverrides);
  const newState = { ...state, overrideActionsFlag: false };
  return pathSet(['currentPolicy', 'policyActionsOverrides'], updatedActionsOverrides, newState);
};

const unSetOverrideParentNotifications = (state, { payload }) => {
  const ownerId = payload;
  const currentNotificationsOverrides = state.currentPolicy.policyNotificationsOverrides || {};
  const updatedNotificationsOverrides = omit([ownerId], currentNotificationsOverrides);
  const newState = { ...state, overrideNotificationsFlag: false };
  return pathSet(['currentPolicy', 'policyNotificationsOverrides'], updatedNotificationsOverrides, newState);
};

const toggleField = curryN(2, function toggleField(fieldName, state) {
  return pathSet(['currentPolicy', fieldName], !state.currentPolicy[fieldName], state);
});

const setConstraintField = curryN(3, function setConstraintField(fieldName, state, { payload }) {
  const { constraintIndex, value } = payload;
  return pathSet(['currentPolicy', 'constraints', constraintIndex, fieldName], value, state);
});

const setConstraintNameField = curryN(3, function setConstraintNameField(fieldName, state, { payload }) {
  const { constraintIndex, value, id } = payload;
  const newValue = userInput(constraintNameValidator(state.currentPolicy.constraints, value, id), value, state);

  return pathSet(['currentPolicy', 'constraints', constraintIndex, fieldName], newValue, state);
});

const setNotifications = curryN(3, function setNotifications(notificationType, state, { payload }) {
  const notificationsPath = isNotificationOverrideEnabled(state)
    ? ['policyNotificationsOverrides', payload.ownerId]
    : ['notifications'];
  return pathSet(['currentPolicy', ...notificationsPath, notificationType], payload.notifications, state);
});

const setNotificationStageIds = curryN(3, function setNotificationStageIds(notificationType, state, { payload }) {
  const { index, value, ownerId } = payload;
  const notificationsPath = isNotificationOverrideEnabled(state)
    ? ['policyNotificationsOverrides', ownerId]
    : ['notifications'];
  return pathSet(['currentPolicy', ...notificationsPath, notificationType, index, 'stageIds'], value, state);
});

const setConstraintConditionField = curryN(3, function setConstraintConditionField(fieldName, state, { payload }) {
  const { constraintIndex, conditionIndex, value } = payload;
  return pathSet(
    ['currentPolicy', 'constraints', constraintIndex, 'conditions', conditionIndex, fieldName],
    value,
    state
  );
});

const setConstraintConditionAgeField = curryN(2, function setConstraintConditionAgeField(state, { payload }) {
  const { constraintIndex, conditionIndex, value } = payload;
  const newValue = userInput(ageValidator, value, state);

  return pathSet(
    ['currentPolicy', 'constraints', constraintIndex, 'conditions', conditionIndex, 'value'],
    newValue,
    state
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
    return pathSet(
      ['currentPolicy', 'constraints', constraintIndex, 'conditions', conditionIndex, 'value'],
      newValue,
      state
    );
  }
);

const setConstraintCoordinatesInput = curryN(2, function setConstraintCoordinatesInput(state, { payload }) {
  const { constraintIndex, conditionIndex, value, name, format } = payload;
  const newValue = userInput(getCoordinatesValidator(name, format), value, state);
  return pathSet(
    ['currentPolicy', 'constraints', constraintIndex, 'conditions', conditionIndex, 'value', name],
    newValue,
    state
  );
});

const initCoordinatesFields = (type) => {
  const conditionValue = {
    format: type,
  };

  coordinatesTypes[type].forEach((field) => {
    conditionValue[field] = initUserInput(
      withDefaultValue[type].includes(field) ? '*' : '',
      getCoordinatesValidator(field, type)
    );
  });

  return conditionValue;
};

const setConstraintCoordinatesFormat = curryN(2, function setConstraintCoordinatesFormat(state, { payload }) {
  const { constraintIndex, conditionIndex, value } = payload;
  return pathSet(
    ['currentPolicy', 'constraints', constraintIndex, 'conditions', conditionIndex, 'value'],
    initCoordinatesFields(value),
    state
  );
});

const setConstraintCondition = curryN(2, function setConstraintCondition(state, { payload }) {
  const { constraintIndex, conditionIndex, value } = payload;
  const { conditionTypeId } = value;

  let returnValue = { ...value, value: initUserInput(value.value, validateNonEmpty) };
  if (conditionTypeId === 'Coordinates') {
    returnValue = { ...value, value: initCoordinatesFields(coordinatesFormatOptions[0]) };
  } else if (conditionTypeId === 'AgeInDays') {
    returnValue = { ...value, value: initUserInput(value.value, ageValidator) };
  }

  return pathSet(['currentPolicy', 'constraints', constraintIndex, 'conditions', conditionIndex], returnValue, state);
});

const toggleCategoryIsApplied = (state, { payload: index }) => {
  const newState = over(lensPath(['categories', index, 'isApplied']), not, state);
  return newState;
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
    return newState;
  } else {
    return toggleField('policyActionsOverrideAllowed')(state);
  }
};

const togglePolicyNotificationsOverrideAllowed = (state) => {
  const currentPolicyNotificationsOverrideAllowed = state.currentPolicy.policyNotificationsOverrideAllowed;
  if (currentPolicyNotificationsOverrideAllowed) {
    const newState = {
      ...state,
      currentPolicy: {
        ...state.currentPolicy,
        policyNotificationsOverrideAllowed: !currentPolicyNotificationsOverrideAllowed,
        policyNotificationsOverrides: null,
      },
    };
    return newState;
  } else {
    return toggleField('policyNotificationsOverrideAllowed')(state);
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

const isNotificationOverrideEnabled = (state) => {
  return state?.isInherited && state?.currentPolicy?.policyNotificationsOverrideAllowed;
};

const getNotifications = (state, ownerId) => {
  if (isNotificationOverrideEnabled(state)) {
    return state?.currentPolicy?.policyNotificationsOverrides[ownerId] ?? {};
  }
  return state?.currentPolicy?.notifications ?? {};
};

const addEmailRecipient = (state, ownerId, emailAddress) => {
  const { userNotifications = [] } = getNotifications(state, ownerId);
  const emailExists = userNotifications.some((item) => item.emailAddress === emailAddress);
  if (!emailExists) {
    const newNotification = { emailAddress, stageIds: [] };
    const payload = { notifications: userNotifications.concat(newNotification), ownerId: ownerId };
    return setNotifications('userNotifications', state, { payload });
  }
};

const addRoleRecipient = (state, ownerId, roleId) => {
  const { roleNotifications = [] } = getNotifications(state, ownerId);
  const newNotification = { roleId, stageIds: [] };
  const payload = { notifications: roleNotifications.concat(newNotification), ownerId: ownerId };
  return setNotifications('roleNotifications', state, { payload });
};

const addWebhookRecipient = (state, ownerId, webhookId) => {
  const { webhookNotifications = [] } = getNotifications(state, ownerId);
  const newNotification = { webhookId, stageIds: [] };
  const payload = { notifications: webhookNotifications.concat(newNotification), ownerId: ownerId };
  return setNotifications('webhookNotifications', state, { payload });
};

const addJiraRecipient = (state, ownerId, projectKey, issueTypeId) => {
  const { jiraNotifications = [] } = getNotifications(state, ownerId);
  const newNotification = { projectKey, issueTypeId, stageIds: [] };
  const payload = { notifications: jiraNotifications.concat(newNotification), ownerId: ownerId };
  return setNotifications('jiraNotifications', state, { payload });
};

const addNotificationRecipient = (originalState, { payload }) => {
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
      return addEmailRecipient(state, payload, recipientEmail);
    case RECIPIENT_TYPES.ROLE:
      return addRoleRecipient(state, payload, recipientRoleId);
    case RECIPIENT_TYPES.WEBHOOK:
      return addWebhookRecipient(state, payload, recipientWebhookId);
    case RECIPIENT_TYPES.JIRA:
      return addJiraRecipient(state, payload, recipientProjectKey, recipientIssueTypeId);
  }
};

const removeNotificationRecipient = (state, { payload }) => {
  const { ownerId, recipient } = payload;
  const removeRecipientFrom = without([omit(['displayName'], recipient)]);
  const setNotificationsFor = (notificationType, payload) => setNotifications(notificationType, state, { payload });
  const {
    webhookNotifications = [],
    userNotifications = [],
    roleNotifications = [],
    jiraNotifications = [],
  } = getNotifications(state, ownerId);

  if (recipient.roleId) {
    return setNotificationsFor('roleNotifications', {
      notifications: removeRecipientFrom(roleNotifications),
      ownerId: ownerId,
    });
  } else if (recipient.emailAddress) {
    return setNotificationsFor('userNotifications', {
      notifications: removeRecipientFrom(userNotifications),
      ownerId: ownerId,
    });
  } else if (recipient.webhookId) {
    return setNotificationsFor('webhookNotifications', {
      notifications: removeRecipientFrom(webhookNotifications),
      ownerId: ownerId,
    });
  } else if (recipient.projectKey) {
    return setNotificationsFor('jiraNotifications', {
      notifications: removeRecipientFrom(jiraNotifications),
      ownerId: ownerId,
    });
  }
};

const toggleNotificationRecipientStage = (state, { payload }) => {
  const { ownerId, recipient, stageId } = payload;
  const notifications = getNotifications(state, ownerId);
  const {
    webhookNotifications = [],
    userNotifications = [],
    roleNotifications = [],
    jiraNotifications = [],
  } = notifications;
  const setStageIdsFor = (notificationType, payload) => setNotificationStageIds(notificationType, state, { payload });
  const updatedStageIds = recipient.stageIds.includes(stageId)
    ? without([stageId], recipient.stageIds)
    : recipient.stageIds.concat(stageId);
  if (recipient.roleId) {
    return setStageIdsFor('roleNotifications', {
      ownerId: ownerId,
      index: findIndex(propEq('roleId', recipient.roleId), roleNotifications),
      value: updatedStageIds,
    });
  } else if (recipient.emailAddress) {
    return setStageIdsFor('userNotifications', {
      ownerId: ownerId,
      index: findIndex(propEq('emailAddress', recipient.emailAddress), userNotifications),
      value: updatedStageIds,
    });
  } else if (recipient.webhookId) {
    return setStageIdsFor('webhookNotifications', {
      ownerId: ownerId,
      index: findIndex(propEq('webhookId', recipient.webhookId), webhookNotifications),
      value: updatedStageIds,
    });
  } else if (recipient.projectKey) {
    return setStageIdsFor('jiraNotifications', {
      ownerId: ownerId,
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
  const { field, value, ownerId } = payload;
  const { userNotifications = [] } = getNotifications(state, ownerId);

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
  (_, { getState, dispatch, rejectWithValue }) => {
    const state = getState();
    let notificationPromises = [dispatch(loadRolesForCurrentOwner()), dispatch(loadJiraProjects())];
    const isRepositoriesRelated = selectIsRepositoriesRelated(state);
    if (!isRepositoriesRelated) {
      notificationPromises = [...notificationPromises, dispatch(loadNotificationWebhooks())];
    }
    return Promise.all(notificationPromises)
      .then((results) => {
        const { membersByRole } = unwrapResult(results[0]);
        const { isJiraEnabled, projects } = unwrapResult(results[1]);
        const notificationWebhooks = isRepositoriesRelated
          ? initialState.notificationsEditor.notificationWebhooks
          : unwrapResult(results[2]);

        return {
          membersByRole,
          notificationWebhooks,
          isJiraEnabled,
          projects,
        };
      })
      .catch(rejectWithValue);
  }
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
    setHasPolicyCategories(state, payload) {
      return reduxPropSet('hasPolicyCategories', state, payload);
    },
    toggleCategoryIsApplied,
    toggleLegacyViolationAllowed: toggleField('legacyViolationAllowed'),
    togglePolicyActionsOverrideAllowed,
    togglePolicyNotificationsOverrideAllowed,
    setPolicyName: setPolicyNameField(),
    setThreatLevel: setPolicyField('threatLevel'),
    setActions: setPolicyField('actions'),
    setActionsOverride(state, { payload }) {
      const { ownerId, actionsOverride } = payload;
      const currentActionsOverrides = state.currentPolicy.policyActionsOverrides || {};
      const updatedActionsOverrides = { ...currentActionsOverrides, [ownerId]: actionsOverride };
      return pathSet(['currentPolicy', 'policyActionsOverrides'], updatedActionsOverrides, state);
    },
    setConstraint: setPolicyField('constraints'),
    setNotificationsOverride(state, { payload }) {
      const { ownerId, notificationsOverride } = payload;
      const currentNotificationsOverrides = state.currentPolicy.policyNotificationsOverrides || {};
      const updatedNotificationsOverrides = { ...currentNotificationsOverrides, [ownerId]: notificationsOverride };
      return pathSet(['currentPolicy', 'policyNotificationsOverrides'], updatedNotificationsOverrides, state);
    },
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
    setOverrideParentNotifications,
    unSetOverrideParentNotifications,
    changeCollapsibleSortField,
    saveMaskTimerDone: propSet('submitMaskState', null),
    clearDeleteError: propSet('deleteError', null),
    toggleShowActionsOverridesConfirmationModal: toggleBooleanProp('showActionsOverridesConfirmationModal'),
    toggleShowNotificationsOverridesConfirmationModal: toggleBooleanProp('showNotificationsOverridesConfirmationModal'),
    resetState: always(initialState),
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

    [updateOverrides.pending]: savePolicyRequested,
    [updateOverrides.fulfilled]: updateOverridesFulfilled,
    [updateOverrides.rejected]: savePolicyFailed,

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
  updateOverrides,
  checkEditIqPermission,
  loadNotificationWebhooks,
  loadNotificationsEditor,
  goToCreatePolicy,
  goToEditPolicy,
};

export default policySlice.reducer;
