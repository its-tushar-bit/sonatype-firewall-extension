/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reducer, { initialState } from 'MainRoot/OrgsAndPolicies/policySlice';

describe('policySlice reducers', () => {
  describe('policy/resetIsDirty', () => {
    it('resets isDirty', () => {
      const state = Object.freeze({
        isDirty: true,
      });

      const { isDirty } = reducer(state, {
        type: 'policy/resetIsDirty',
      });

      expect(isDirty).toBeFalse();
    });
  });

  describe('policy/resetDeleteModalState', () => {
    it('resets isDirty', () => {
      const state = Object.freeze({
        deleteModal: { success: true, deleting: false, errorState: 'someError' },
      });

      const { deleteModal } = reducer(state, {
        type: 'policy/resetDeleteModalState',
      });

      expect(deleteModal).toEqual({ deleting: null, success: null, errorState: null });
    });
  });

  describe('policy/setPolicyName', () => {
    it('sets currentPolicy name and compute isDirty', () => {
      const state = Object.freeze({
        currentPolicy: {
          name: null,
        },

        isDirty: false,
      });

      const { currentPolicy, isDirty } = reducer(state, {
        type: 'policy/setPolicyName',
        payload: 'newName',
      });

      expect(currentPolicy.name).toBe('newName');
      expect(isDirty).toBeTrue();
    });
  });

  describe('policy/setThreatLevel', () => {
    it('sets currentPolicy threatLevel and compute isDirty', () => {
      const state = Object.freeze({
        currentPolicy: {
          threatLevel: 1,
        },

        isDirty: false,
      });

      const { currentPolicy, isDirty } = reducer(state, {
        type: 'policy/setThreatLevel',
        payload: 5,
      });

      expect(currentPolicy.threatLevel).toBe(5);
      expect(isDirty).toBeTrue();
    });
  });

  describe('policy/setActions', () => {
    it('sets actions', () => {
      const state = Object.freeze({
        currentPolicy: { actions: null },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setActions',
        payload: { proxy: 'warn' },
      });

      expect(currentPolicy.actions).toEqual({ proxy: 'warn' });
    });
  });

  describe('policy/setHasPolicyCategories', () => {
    it('sets hasPolicyCategories', () => {
      const state = Object.freeze({
        hasPolicyCategories: false,
      });

      const { hasPolicyCategories } = reducer(state, {
        type: 'policy/setHasPolicyCategories',
        payload: true,
      });

      expect(hasPolicyCategories).toBeTrue();
    });
  });

  describe('policy/togglePolicyViolationGrandfatheringAllowed', () => {
    it('toggles policyViolationGrandfatheringAllowed for a category', () => {
      const state = Object.freeze({
        currentPolicy: { policyViolationGrandfatheringAllowed: false },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/togglePolicyViolationGrandfatheringAllowed',
      });

      expect(currentPolicy.policyViolationGrandfatheringAllowed).toBeTrue();
    });
  });

  describe('policy/policyActionsOverrideAllowed', () => {
    it('toggles policyActionsOverrideAllowed for a category from false to true', () => {
      const state = Object.freeze({
        isDirty: false,
        currentPolicy: { policyActionsOverrideAllowed: false },
      });

      const { currentPolicy, isDirty } = reducer(state, {
        type: 'policy/togglePolicyActionsOverrideAllowed',
      });

      expect(currentPolicy.policyActionsOverrideAllowed).toBeTrue();
      expect(isDirty).toBeTrue();
    });

    it('toggles policyActionsOverrideAllowed for a category from true to false', () => {
      const state = Object.freeze({
        isDirty: false,
        currentPolicy: {
          policyActionsOverrideAllowed: true,
          policyActionsOverrides: {
            df9ad82193e44f4f9385e0c9e8835409: {
              source: 'warn',
              build: 'warn',
              'stage-release': 'fail',
              release: 'warn',
              operate: 'fail',
            },
          },
        },
      });

      const { currentPolicy, isDirty } = reducer(state, {
        type: 'policy/togglePolicyActionsOverrideAllowed',
      });

      expect(currentPolicy.policyActionsOverrideAllowed).toBeFalse();
      expect(currentPolicy.policyActionsOverrides).toBeNull();
      expect(isDirty).toBeTrue();
    });
  });

  describe('policy/addConstraint', () => {
    it('sets currentPolicy constraints and compute isDirty', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: null,
        },

        isDirty: false,
      });

      const constraints = [
        {
          id: 'someId',
          conditions: [
            {
              conditionTypeId: 'AgeInDays',
              operator: 'older than',
              value: null,
            },
          ],
          operator: 'OR',
        },
      ];

      const { currentPolicy, isDirty } = reducer(state, {
        type: 'policy/addConstraint',
        payload: constraints,
      });

      expect(currentPolicy.constraints).toEqual(constraints);
      expect(isDirty).toBeTrue();
    });
  });

  describe('policy/deleteConstraint', () => {
    it('sets currentPolicy constraints and compute isDirty', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [
            {
              id: 'someId',
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

        isDirty: false,
      });

      const { currentPolicy, isDirty } = reducer(state, {
        type: 'policy/deleteConstraint',
        payload: [],
      });

      expect(currentPolicy.constraints).toEqual([]);
      expect(isDirty).toBeTrue();
    });
  });

  describe('policy/setUserNotifications', () => {
    it('sets userNotifications and compute isDirty', () => {
      const state = Object.freeze({
        currentPolicy: {
          notifications: { userNotifications: null },
        },

        isDirty: false,
      });
      const userNotifications = [{ emailAddress: 'df@sd.com', stageIds: [] }];

      const { currentPolicy, isDirty } = reducer(state, {
        type: 'policy/setUserNotifications',
        payload: userNotifications,
      });

      expect(currentPolicy.notifications.userNotifications).toEqual(userNotifications);
      expect(isDirty).toBeTrue();
    });
  });

  describe('policy/setRoleNotifications', () => {
    it('sets roleNotifications and compute isDirty', () => {
      const state = Object.freeze({
        currentPolicy: {
          notifications: { roleNotifications: null },
        },

        isDirty: false,
      });
      const roleNotifications = [{ roleId: '90c7c98683b4471cb77a916744540bcc', stageIds: [] }];

      const { currentPolicy, isDirty } = reducer(state, {
        type: 'policy/setRoleNotifications',
        payload: roleNotifications,
      });

      expect(currentPolicy.notifications.roleNotifications).toEqual(roleNotifications);
      expect(isDirty).toBeTrue();
    });
  });

  describe('policy/setJiraNotifications', () => {
    it('sets jiraNotifications and compute isDirty', () => {
      const state = Object.freeze({
        currentPolicy: {
          notifications: { jiraNotifications: null },
        },

        isDirty: false,
      });

      const jiraNotifications = [{ projectKey: 'somekey', stageIds: [] }];

      const { currentPolicy, isDirty } = reducer(state, {
        type: 'policy/setJiraNotifications',
        payload: jiraNotifications,
      });

      expect(currentPolicy.notifications.jiraNotifications).toEqual(jiraNotifications);
      expect(isDirty).toBeTrue();
    });
  });

  describe('policy/setWebhookNotifications', () => {
    it('sets webhookNotifications and compute isDirty', () => {
      const state = Object.freeze({
        currentPolicy: {
          notifications: { webhookNotifications: null },
        },

        isDirty: false,
      });

      const webhookNotifications = [{ webhookId: 'someid', stageIds: [] }];

      const { currentPolicy, isDirty } = reducer(state, {
        type: 'policy/setWebhookNotifications',
        payload: webhookNotifications,
      });

      expect(currentPolicy.notifications.webhookNotifications).toEqual(webhookNotifications);
      expect(isDirty).toBeTrue();
    });
  });

  describe('policy/setUserNotificationStageIds', () => {
    it('sets stageIds in userNotifications and compute isDirty', () => {
      const state = Object.freeze({
        currentPolicy: {
          notifications: {
            userNotifications: [
              { emailAddress: 'someEmail', stageIds: [] },
              { emailAddress: 'someOtherEmail', stageIds: [] },
            ],
          },
        },

        isDirty: false,
      });

      const { currentPolicy, isDirty } = reducer(state, {
        type: 'policy/setUserNotificationStageIds',
        payload: { index: 1, value: ['develop'] },
      });

      expect(currentPolicy.notifications.userNotifications[0].stageIds).toEqual([]);
      expect(currentPolicy.notifications.userNotifications[1].stageIds).toEqual(['develop']);
      expect(isDirty).toBeTrue();
    });
  });

  describe('policy/setRoleNotificationStageIds', () => {
    it('sets stageIds in roleNotifications and compute isDirty', () => {
      const state = Object.freeze({
        currentPolicy: {
          notifications: {
            roleNotifications: [
              { roleId: 'someId', stageIds: [] },
              { roleId: 'someOtherId', stageIds: [] },
            ],
          },
        },

        isDirty: false,
      });

      const { currentPolicy, isDirty } = reducer(state, {
        type: 'policy/setRoleNotificationStageIds',
        payload: { index: 1, value: ['develop'] },
      });

      expect(currentPolicy.notifications.roleNotifications[0].stageIds).toEqual([]);
      expect(currentPolicy.notifications.roleNotifications[1].stageIds).toEqual(['develop']);
      expect(isDirty).toBeTrue();
    });
  });

  describe('policy/setJiraNotificationStageIds', () => {
    it('sets stageIds in jiraNotifications and compute isDirty', () => {
      const state = Object.freeze({
        currentPolicy: {
          notifications: {
            jiraNotifications: [
              { projectKey: 'test', stageIds: [] },
              { projectKey: 'test2', stageIds: [] },
            ],
          },
        },

        isDirty: false,
      });

      const { currentPolicy, isDirty } = reducer(state, {
        type: 'policy/setJiraNotificationStageIds',
        payload: { index: 1, value: ['develop'] },
      });

      expect(currentPolicy.notifications.jiraNotifications[0].stageIds).toEqual([]);
      expect(currentPolicy.notifications.jiraNotifications[1].stageIds).toEqual(['develop']);
      expect(isDirty).toBeTrue();
    });
  });

  describe('policy/setWebhookNotificationStageIds', () => {
    it('sets stageIds in webhookNotifications and compute isDirty', () => {
      const state = Object.freeze({
        currentPolicy: {
          notifications: {
            webhookNotifications: [
              { webhookId: 'test', stageIds: [] },
              { webhookId: 'test2', stageIds: [] },
            ],
          },
        },
        isDirty: false,
      });

      const { currentPolicy, isDirty } = reducer(state, {
        type: 'policy/setWebhookNotificationStageIds',
        payload: { index: 1, value: ['develop'] },
      });

      expect(currentPolicy.notifications.webhookNotifications[0].stageIds).toEqual([]);
      expect(currentPolicy.notifications.webhookNotifications[1].stageIds).toEqual(['develop']);
      expect(isDirty).toBeTrue();
    });
  });

  describe('policy/addCondition', () => {
    it('sets conditions for currentPolicy constraints and compute isDirty', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [
            {
              id: 'someId',
              conditions: null,
              operator: 'OR',
            },
            {
              id: 'someOtherId',
              conditions: null,
              operator: 'OR',
            },
          ],
        },
        isDirty: false,
      });
      const conditions = [{ conditionTypeId: 'AgeInDays', operator: 'older than', value: null }];

      const { currentPolicy, isDirty } = reducer(state, {
        type: 'policy/addCondition',
        payload: { constraintIndex: 1, value: conditions },
      });

      expect(currentPolicy.constraints[0].conditions).toBeNull();
      expect(currentPolicy.constraints[1].conditions).toEqual(conditions);
      expect(isDirty).toBeTrue();
    });
  });

  describe('policy/addCondition', () => {
    it('sets conditions for currentPolicy constraints and compute isDirty', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [
            {
              id: 'someId',
              conditions: null,
              operator: 'OR',
            },
            {
              id: 'someOtherId',
              conditions: null,
              operator: 'OR',
            },
          ],
        },
        isDirty: false,
      });

      const { currentPolicy, isDirty } = reducer(state, {
        type: 'policy/addCondition',
        payload: { constraintIndex: 1, value: [] },
      });

      expect(currentPolicy.constraints[0].conditions).toBeNull();
      expect(currentPolicy.constraints[1].conditions).toEqual([]);
      expect(isDirty).toBeTrue();
    });
  });

  describe('policy/setConstraintName', () => {
    it('sets name for currentPolicy constraints and compute isDirty', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [
            {
              id: 'someId',
              conditions: null,
              name: null,
              operator: 'OR',
            },
            {
              id: 'someOtherId',
              conditions: null,
              operator: 'OR',
            },
          ],
        },
        isDirty: false,
      });

      const { currentPolicy, isDirty } = reducer(state, {
        type: 'policy/setConstraintName',
        payload: { constraintIndex: 1, value: 'newName' },
      });

      expect(currentPolicy.constraints[0].name).toBeNull();
      expect(currentPolicy.constraints[1].name).toBe('newName');
      expect(isDirty).toBeTrue();
    });
  });

  describe('policy/setConstraintOperator', () => {
    it('sets operator for currentPolicy constraints and compute isDirty', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [
            {
              id: 'someId',
              operator: null,
            },
            {
              id: 'someOtherId',
              operator: 'OR',
            },
          ],
        },
        isDirty: false,
      });

      const { currentPolicy, isDirty } = reducer(state, {
        type: 'policy/setConstraintOperator',
        payload: { constraintIndex: 1, value: 'AND' },
      });

      expect(currentPolicy.constraints[0].operator).toBeNull();
      expect(currentPolicy.constraints[1].operator).toBe('AND');
      expect(isDirty).toBeTrue();
    });
  });

  describe('policy/setConditionOperator', () => {
    it('sets operator and compute isDirty', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [
            { id: 'test' },
            {
              id: 'someOtherId',
              conditions: [{ operator: null }, { operator: null }],
            },
          ],
        },
        isDirty: false,
      });

      const { currentPolicy, isDirty } = reducer(state, {
        type: 'policy/setConditionOperator',
        payload: { constraintIndex: 1, conditionIndex: 1, value: 'AND' },
      });

      expect(currentPolicy.constraints[1].conditions[0].operator).toBeNull();
      expect(currentPolicy.constraints[1].conditions[1].operator).toBe('AND');
      expect(isDirty).toBeTrue();
    });
  });

  describe('policy/setConditionValue', () => {
    it('sets value and compute isDirty', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [
            { id: 'test' },
            {
              id: 'someOtherId',
              conditions: [{ value: null }, { value: null }],
            },
          ],
        },
        isDirty: false,
      });

      const { currentPolicy, isDirty } = reducer(state, {
        type: 'policy/setConditionValue',
        payload: { constraintIndex: 1, conditionIndex: 1, value: 'someValue' },
      });

      expect(currentPolicy.constraints[1].conditions[0].value).toBeNull();
      expect(currentPolicy.constraints[1].conditions[1].value).toBe('someValue');
      expect(isDirty).toBeTrue();
    });
  });

  describe('policy/setConstraintCondition', () => {
    it('sets conditions and compute isDirty', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [
            { id: 'test', conditions: null },
            {
              id: 'someOtherId',
              conditions: [
                {
                  conditionTypeId: 'MatchState',
                  operator: 'is',
                  value: 'similar',
                  conditionIndex: 0,
                },
                {
                  conditionTypeId: 'Coordinates',
                  operator: 'do not match',
                  value: 'maven:org.eclipse.*:*:*:*:*',
                  conditionIndex: 1,
                },
              ],
            },
          ],
        },
        isDirty: false,
      });
      const newCondition = {
        conditionTypeId: 'AgeInDays',
        operator: 'older than',
        value: '730',
      };

      const { currentPolicy, isDirty } = reducer(state, {
        type: 'policy/setConstraintCondition',
        payload: { constraintIndex: 1, conditionIndex: 1, value: newCondition },
      });

      expect(currentPolicy.constraints[0].conditions).toBeNull();
      expect(currentPolicy.constraints[1].conditions[1]).toEqual(newCondition);
      expect(isDirty).toBeTrue();
    });
  });

  describe('policy/loadCategoriesForPolicy/pending', () => {
    it('sets categoriesForPolicyLoadError and loading', () => {
      const state = Object.freeze({ categoriesForPolicyLoadError: 'error', loading: false });

      const { categoriesForPolicyLoadError, loading } = reducer(state, {
        type: 'policy/loadCategoriesForPolicy/pending',
      });

      expect(categoriesForPolicyLoadError).toBeNull();
      expect(loading).toBeTrue();
    });
  });

  describe('policy/loadCategoriesForPolicy/fulfilled', () => {
    it('sets payload into state', () => {
      const state = Object.freeze({
        categoriesForPolicyLoadError: 'error',
        loading: false,
        hasPolicyCategories: false,
        originalHasPolicyCategories: false,
        categories: null,
        originalCategories: null,
      });

      const fulfilledPayload = {
        hasPolicyCategories: true,
        categories: [],
      };
      const {
        categoriesForPolicyLoadError,
        loading,
        hasPolicyCategories,
        originalHasPolicyCategories,
        categories,
        originalCategories,
      } = reducer(state, {
        type: 'policy/loadCategoriesForPolicy/fulfilled',
        payload: fulfilledPayload,
      });

      expect(categoriesForPolicyLoadError).toBeNull();
      expect(loading).toBeFalse();
      expect(hasPolicyCategories).toBeTrue();
      expect(originalHasPolicyCategories).toBeTrue();
      expect(categories).toEqual([]);
      expect(originalCategories).toEqual(categories);
    });
  });

  describe('policy/loadCategoriesForPolicy/failed', () => {
    it('sets loadError and loading', () => {
      const state = Object.freeze({ categoriesForPolicyLoadError: null, loading: true });

      const { categoriesForPolicyLoadError, loading } = reducer(state, {
        type: 'policy/loadCategoriesForPolicy/rejected',
        payload: 'error',
      });

      expect(categoriesForPolicyLoadError).toBe('error');
      expect(loading).toBeFalse();
    });
  });

  describe('policy/checkEditIqPermission/fulfilled', () => {
    it('sets hasEditIqPermission', () => {
      const state = Object.freeze({ hasEditIqPermission: null });

      const { hasEditIqPermission } = reducer(state, {
        type: 'policy/checkEditIqPermission/fulfilled',
      });

      expect(hasEditIqPermission).toBeTrue();
    });
  });

  describe('policy/checkEditIqPermission/rejected', () => {
    it('clears hasEditIqPermission', () => {
      const state = Object.freeze({ hasEditIqPermission: null });

      const { hasEditIqPermission } = reducer(state, {
        type: 'policy/checkEditIqPermission/rejected',
      });

      expect(hasEditIqPermission).toBeFalse();
    });
  });

  describe('policy/loadPolicyEditor/pending', () => {
    it('sets loadError and loading', () => {
      const state = Object.freeze({ loadError: 'error', loading: false });

      const { loadError, loading } = reducer(state, {
        type: 'policy/loadPolicyEditor/pending',
      });

      expect(loadError).toBeNull();
      expect(loading).toBeTrue();
    });
  });

  describe('policy/loadPolicyEditor/fulfilled', () => {
    it('sets payload into the state', () => {
      const state = Object.freeze({
        loadError: 'error',
        loading: true,
        deleteModal: {
          deleting: false,
          success: false,
          errorState: 'someError',
        },
        isDirty: true,
        currentPolicy: null,
        currentPolicyOwner: null,
        originalPolicy: null,
        siblings: [],
        isInherited: false,
        isOrgOwner: false,
        isRootOrg: false,
        originalProxyStageAction: null,
      });

      const fulfilledPayload = {
        currentPolicy: { id: 'someId' },
        currentPolicyOwner: { id: 'ownerId', name: 'ownerName' },
        siblings: [{ id: 'anotherPolicyId' }],
        isInherited: true,
        isOrgOwner: true,
        isRootOrg: true,
        originalProxyStageAction: 'warn',
      };

      const {
        loadError,
        loading,
        deleteModal,
        isDirty,
        currentPolicy,
        currentPolicyOwner,
        originalPolicy,
        siblings,
        isInherited,
        isOrgOwner,
        isRootOrg,
        originalProxyStageAction,
      } = reducer(state, {
        type: 'policy/loadPolicyEditor/fulfilled',
        payload: fulfilledPayload,
      });

      expect(loadError).toBeNull();
      expect(loading).toBeFalse();
      expect(deleteModal).toEqual({
        success: null,
        deleting: null,
        errorState: null,
      });
      expect(isDirty).toBeFalse();
      expect(currentPolicy).toEqual(fulfilledPayload.currentPolicy);
      expect(currentPolicyOwner).toEqual(fulfilledPayload.currentPolicyOwner);
      expect(originalPolicy).toEqual(fulfilledPayload.currentPolicy);
      expect(siblings).toEqual(fulfilledPayload.siblings);
      expect(isInherited).toBe(fulfilledPayload.isInherited);
      expect(isOrgOwner).toBe(fulfilledPayload.isOrgOwner);
      expect(isRootOrg).toBe(fulfilledPayload.isRootOrg);
      expect(originalProxyStageAction).toBe(fulfilledPayload.originalProxyStageAction);
    });
  });

  describe('policy/loadPolicyEditor/failed', () => {
    it('sets loadError and loading', () => {
      const state = Object.freeze({ loadError: null, loading: true });

      const { loadError, loading } = reducer(state, {
        type: 'policy/loadPolicyEditor/rejected',
        payload: 'error',
      });

      expect(loadError).toBe('error');
      expect(loading).toBeFalse();
    });
  });

  describe('policy/savePolicy/pending', () => {
    it('sets loadError and loading', () => {
      const state = Object.freeze({ loadError: 'error', loading: false });

      const { loadError, loading } = reducer(state, {
        type: 'policy/savePolicy/pending',
      });

      expect(loadError).toBeNull();
      expect(loading).toBeTrue();
    });
  });

  describe('policy/savePolicy/fulfilled', () => {
    it('updates originals and siblings when payload has isEditMode', () => {
      const state = Object.freeze({
        loadError: 'error',
        loading: false,
        isDirty: true,
        originalPolicy: null,
        currentPolicy: { id: 'somePolicyId', actions: { proxy: 'warn' } },
        categories: [],
        originalCategories: null,
        originalHasPolicyCategories: false,
        hasPolicyCategories: true,
        originalProxyStageAction: null,
        siblings: [{ id: 'somePolicyId' }, { id: 'someOtherPolicyId' }],
      });

      const {
        loadError,
        loading,
        isDirty,
        originalPolicy,
        originalCategories,
        originalHasPolicyCategories,
        originalProxyStageAction,
        siblings,
      } = reducer(state, {
        type: 'policy/savePolicy/fulfilled',
        payload: { isEditMode: true },
      });

      expect(loadError).toBeNull();
      expect(loading).toBeFalse();
      expect(isDirty).toBeFalse();
      expect(originalPolicy).toEqual(state.currentPolicy);
      expect(originalCategories).toEqual(state.categories);
      expect(originalHasPolicyCategories).toBe(state.hasPolicyCategories);
      expect(originalProxyStageAction).toBe(state.currentPolicy.actions['proxy']);
      expect(siblings).toEqual([{ id: 'somePolicyId', actions: { proxy: 'warn' } }, { id: 'someOtherPolicyId' }]);
    });

    it('should not update originals and siblings when payload has isEditMode', () => {
      const state = Object.freeze({
        loadError: 'error',
        loading: false,
        isDirty: true,
        originalPolicy: null,
        currentPolicy: { id: 'somePolicyId', actions: { proxy: 'warn' } },
        categories: [],
        originalCategories: null,
        originalHasPolicyCategories: false,
        hasPolicyCategories: true,
        originalProxyStageAction: null,
        siblings: [{ id: 'somePolicyId' }, { id: 'someOtherPolicyId' }],
      });

      const {
        loadError,
        loading,
        isDirty,
        originalPolicy,
        originalCategories,
        originalHasPolicyCategories,
        originalProxyStageAction,
        siblings,
        currentPolicy,
      } = reducer(state, {
        type: 'policy/savePolicy/fulfilled',
      });

      expect(loadError).toBeNull();
      expect(loading).toBeFalse();
      expect(isDirty).toBeFalse();
      expect(originalPolicy).toEqual(state.originalPolicy);
      expect(originalCategories).toEqual(state.originalCategories);
      expect(originalHasPolicyCategories).toBe(state.originalHasPolicyCategories);
      expect(originalProxyStageAction).toBe(state.originalProxyStageAction);
      expect(siblings).toEqual(state.siblings);
      expect(currentPolicy).toEqual(initialState.currentPolicy);
    });
  });

  describe('policy/savePolicy/failed', () => {
    it('sets loadError and loading', () => {
      const state = Object.freeze({ loadError: null, loading: true });

      const { loadError, loading } = reducer(state, {
        type: 'policy/savePolicy/rejected',
        payload: 'error',
      });

      expect(loadError).toBe('error');
      expect(loading).toBeFalse();
    });
  });

  describe('policy/removePolicy/pending', () => {
    it('sets deleting', () => {
      const state = Object.freeze({ deleteModal: { deleting: null } });

      const { deleteModal } = reducer(state, {
        type: 'policy/removePolicy/pending',
      });

      expect(deleteModal.deleting).toBeTrue();
    });
  });

  describe('policy/removePolicy/fulfilled', () => {
    it('sets deleteModal states, updates currentPolicy, originalPolicy and siblings', () => {
      const state = Object.freeze({ deleteModal: { deleting: true, errorState: 'someError', success: null } });

      const { deleteModal } = reducer(state, {
        type: 'policy/removePolicy/fulfilled',
      });

      expect(deleteModal.deleting).toBeNull();
      expect(deleteModal.success).toBeTrue();
      expect(deleteModal.errorState).toBeNull();
    });
  });

  describe('policy/removePolicy/failed', () => {
    it('sets deleting and errorState ', () => {
      const state = Object.freeze({ deleteModal: { deleting: true, errorState: null } });

      const { deleteModal } = reducer(state, {
        type: 'policy/removePolicy/rejected',
        payload: 'error',
      });

      expect(deleteModal.deleting).toBeFalse();
      expect(deleteModal.errorState).toBe('error');
    });
  });

  describe('policy/setActionsOverride', () => {
    it('adds provided actions override for provided owner and sets isDirty to true if override has changed', () => {
      const state = Object.freeze({
        isDirty: true,
        isInherited: true,
        overrideActionsFlag: true,
        originalOverrideActionsFlag: false,
        currentPolicy: {
          policyActionsOverrides: {
            someOwnerId: { build: 'warn' },
          },
        },
        originalPolicy: {
          policyActionsOverrides: {
            someOwnerId: { build: 'warn' },
          },
        },
      });

      const action = {
        type: 'policy/setActionsOverride',
        payload: {
          ownerId: 'currentOwnerId',
          actionsOverride: { build: 'fail', release: 'fail' },
        },
      };

      const { currentPolicy, isDirty } = reducer(state, action);

      expect(currentPolicy.policyActionsOverrides.someOwnerId).toEqual({ build: 'warn' });
      expect(currentPolicy.policyActionsOverrides.currentOwnerId).toEqual({ build: 'fail', release: 'fail' });
      expect(isDirty).toBe(true);
    });

    it('adds actions override and sets isDirty to false if override has not changed', () => {
      const state = Object.freeze({
        isDirty: true,
        isInherited: true,
        overrideActionsFlag: false,
        originalOverrideActionsFlag: false,
        currentPolicy: {
          policyActionsOverrides: {
            someOwnerId: { build: 'warn' },
          },
        },
        originalPolicy: {
          policyActionsOverrides: {
            someOwnerId: { build: 'warn' },
            currentOwnerId: { build: 'fail', release: 'fail' },
          },
        },
      });

      const action = {
        type: 'policy/setActionsOverride',
        payload: {
          ownerId: 'currentOwnerId',
          actionsOverride: { build: 'fail', release: 'fail' },
        },
      };

      const { currentPolicy, isDirty } = reducer(state, action);

      expect(currentPolicy.policyActionsOverrides).toEqual(state.originalPolicy.policyActionsOverrides);
      expect(isDirty).toBe(false);
    });
  });

  describe('policy/saveActionsOverride/fulfilled', () => {
    it('resets loading flags, resets isDirty and resets the policy from payload', () => {
      const state = Object.freeze({
        loading: true,
        loadError: 'some error',
        isDirty: true,
        currentPolicy: 'current policy',
        originalPolicy: 'original policy',
        overrideActionsFlag: false,
        originalOverrideActionsFlag: false,
      });

      const action = {
        type: 'policy/saveActionsOverride/fulfilled',
        payload: 'updated policy',
      };

      const {
        loading,
        loadError,
        isDirty,
        currentPolicy,
        originalPolicy,
        overrideActionsFlag,
        originalOverrideActionsFlag,
      } = reducer(state, action);

      expect(loading).toBe(false);
      expect(loadError).toBe(null);
      expect(isDirty).toBe(false);
      expect(currentPolicy).toBe('updated policy');
      expect(originalPolicy).toBe('updated policy');
      expect(overrideActionsFlag).toBeTrue();
      expect(originalOverrideActionsFlag).toBeTrue();
    });
  });

  describe('policy/saveActionsOverride/pending', () => {
    it('resets loadError and sets loading to true', () => {
      const state = Object.freeze({ loadError: 'error', loading: false });

      const { loadError, loading } = reducer(state, {
        type: 'policy/saveActionsOverride/pending',
      });

      expect(loadError).toBeNull();
      expect(loading).toBeTrue();
    });
  });

  describe('policy/saveActionsOverride/failed', () => {
    it('resets loading and sets loadError', () => {
      const state = Object.freeze({ loadError: null, loading: true });

      const { loadError, loading } = reducer(state, {
        type: 'policy/saveActionsOverride/rejected',
        payload: 'error',
      });

      expect(loadError).toBe('error');
      expect(loading).toBeFalse();
    });
  });

  describe('policy/removeActionsOverride/fulfilled', () => {
    it('resets loading flags, resets isDirty and resets the policy from payload', () => {
      const state = Object.freeze({
        loading: true,
        loadError: 'some error',
        isDirty: true,
        currentPolicy: 'current policy',
        originalPolicy: 'original policy',
        overrideActionsFlag: true,
        originalOverrideActionsFlag: true,
      });

      const action = {
        type: 'policy/removeActionsOverride/fulfilled',
        payload: 'removed overrides for policy',
      };

      const {
        loading,
        loadError,
        isDirty,
        currentPolicy,
        originalPolicy,
        overrideActionsFlag,
        originalOverrideActionsFlag,
      } = reducer(state, action);

      expect(loading).toBe(false);
      expect(loadError).toBe(null);
      expect(isDirty).toBe(false);
      expect(currentPolicy).toBe('removed overrides for policy');
      expect(originalPolicy).toBe('removed overrides for policy');
      expect(overrideActionsFlag).toBeFalse();
      expect(originalOverrideActionsFlag).toBeFalse();
    });
  });

  describe('policy/removeActionsOverride/pending', () => {
    it('resets loadError and sets loading to true', () => {
      const state = Object.freeze({ loadError: 'error', loading: false });

      const { loadError, loading } = reducer(state, {
        type: 'policy/removeActionsOverride/pending',
      });

      expect(loadError).toBeNull();
      expect(loading).toBeTrue();
    });
  });

  describe('policy/removeActionsOverride/failed', () => {
    it('resets loading and sets loadError', () => {
      const state = Object.freeze({ loadError: null, loading: true });

      const { loadError, loading } = reducer(state, {
        type: 'policy/removeActionsOverride/rejected',
        payload: 'error',
      });

      expect(loadError).toBe('error');
      expect(loading).toBeFalse();
    });
  });

  describe('policy/setOverrideParentActions', () => {
    it('sets overrideActionsFlag and isDirty', () => {
      const state = Object.freeze({
        overrideActionsFlag: false,
        originalOverrideActionsFlag: false,
        isDirty: false,
        isInherited: true,
        currentPolicy: {},
        originalPolicy: {
          policyActionsOverrides: {
            someOwnerId: { build: 'warn' },
            currentOwnerId: { build: 'fail', release: 'fail' },
          },
        },
      });

      const { isDirty, overrideActionsFlag } = reducer(state, {
        type: 'policy/setOverrideParentActions',
        payload: true,
      });

      expect(overrideActionsFlag).toBeTrue();
      expect(isDirty).toBeTrue();
    });
  });

  describe('policy/unSetOverrideParentActions', () => {
    it('removes override from current policy, sets overrideActionsFlag and isDirty', () => {
      const state = Object.freeze({
        overrideActionsFlag: true,
        originalOverrideActionsFlag: true,
        isDirty: false,
        isInherited: true,
        currentPolicy: {
          policyActionsOverrides: {
            id201: {
              proxy: 'fail',
            },
          },
        },
        originalPolicy: {
          policyActionsOverrides: null,
        },
      });

      const { isDirty, overrideActionsFlag, currentPolicy } = reducer(state, {
        type: 'policy/unSetOverrideParentActions',
        payload: 'id201',
      });

      expect(overrideActionsFlag).toBeFalse();
      expect(isDirty).toBeTrue();
      expect(currentPolicy.policyActionsOverrides).toEqual({});
    });
  });

  describe('policy/loadPolicyTile/pending', () => {
    it('resets loadError and sets loading to true for policy tile', () => {
      const state = Object.freeze({ policyTile: { loadError: 'error', loading: false } });

      const { policyTile } = reducer(state, {
        type: 'policy/loadPolicyTile/pending',
      });

      expect(policyTile.loadError).toBeNull();
      expect(policyTile.loading).toBeTrue();
    });
  });

  describe('policy/loadPolicyTile/fulfilled', () => {
    it('resets loading flag, sets initial sorting config and policies sorted by threat', () => {
      const state = Object.freeze({
        policyTile: {
          loading: true,
          sorting: {},
          policiesByOwner: null,
        },
      });

      const action = {
        type: 'policy/loadPolicyTile/fulfilled',
        payload: [
          {
            inherited: false,
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organizations',
            policies: [
              {
                hasLocalActionsOverrides: undefined,
                id: '787822577d384a28b59c9d06ff6d37e2',
                name: 'allowed to override',
                threatLevel: 5,
                ownerId: 'ROOT_ORGANIZATION_ID',
                actions: {},
              },
              {
                hasLocalActionsOverrides: undefined,
                id: 'e2cb70070ce540f7af0af8478b4d8bd9',
                name: 'fresh allowed to override',
                threatLevel: 5,
                ownerId: 'ROOT_ORGANIZATION_ID',
                actions: {},
              },
              {
                hasLocalActionsOverrides: undefined,
                id: '9bd6ecd914f04e41bc983a9421f31b12',
                name: 'not allowed to override',
                threatLevel: 10,
                ownerId: 'ROOT_ORGANIZATION_ID',
                actions: {},
              },
            ],
          },
        ],
      };

      const { policyTile } = reducer(state, action);

      expect(policyTile.loading).toBe(false);
      expect(policyTile.sorting).toEqual({
        'Root Organization': { key: 'threatLevel', dir: 'desc', ownerName: 'Root Organization' },
      });
      expect(policyTile.policiesByOwner[0].policies[0].threatLevel).toBe(10);
      expect(policyTile.policiesByOwner[0].policies[0].name).toBe('not allowed to override');
      expect(policyTile.policiesByOwner[0].policies[1].threatLevel).toBe(5);
      expect(policyTile.policiesByOwner[0].policies[1].name).toBe('fresh allowed to override');
      expect(policyTile.policiesByOwner[0].policies[2].threatLevel).toBe(5);
      expect(policyTile.policiesByOwner[0].policies[2].name).toBe('allowed to override');
    });
  });

  describe('policy/changeSortField', () => {
    it('sets sorting config and policies sorted by chosen payload key', () => {
      const state = Object.freeze({
        policyTile: {
          sorting: {
            'Root Organization': { key: 'threatLevel', dir: 'desc', ownerName: 'Root Organization' },
          },
          policiesByOwner: [
            {
              inherited: false,
              ownerId: 'ROOT_ORGANIZATION_ID',
              ownerName: 'Root Organization',
              ownerType: 'organizations',
              policies: [
                {
                  hasLocalActionsOverrides: undefined,
                  id: '9bd6ecd914f04e41bc983a9421f31b12',
                  name: 'not allowed to override',
                  threatLevel: 10,
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  actions: {},
                },
                {
                  hasLocalActionsOverrides: undefined,
                  id: 'e2cb70070ce540f7af0af8478b4d8bd9',
                  name: 'fresh allowed to override',
                  threatLevel: 5,
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  actions: {},
                },
                {
                  hasLocalActionsOverrides: undefined,
                  id: '787822577d384a28b59c9d06ff6d37e2',
                  name: 'allowed to override',
                  threatLevel: 5,
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  actions: {},
                },
              ],
            },
          ],
        },
      });

      const action = {
        type: 'policy/changeSortField',
        payload: { key: 'threatLevel', dir: 'asc', ownerName: 'Root Organization' },
      };

      const { policyTile } = reducer(state, action);

      expect(policyTile.sorting).toEqual({
        'Root Organization': { key: 'threatLevel', dir: 'asc', ownerName: 'Root Organization' },
      });
      expect(policyTile.policiesByOwner[0].policies[0].threatLevel).toBe(5);
      expect(policyTile.policiesByOwner[0].policies[0].name).toBe('allowed to override');
      expect(policyTile.policiesByOwner[0].policies[1].threatLevel).toBe(5);
      expect(policyTile.policiesByOwner[0].policies[1].name).toBe('fresh allowed to override');
      expect(policyTile.policiesByOwner[0].policies[2].threatLevel).toBe(10);
      expect(policyTile.policiesByOwner[0].policies[2].name).toBe('not allowed to override');
    });

    it('returns policies in preserved order if values are equal by the key', () => {
      const state = Object.freeze({
        policyTile: {
          sorting: {
            'Root Organization': { key: 'threatLevel', dir: 'desc', ownerName: 'Root Organization' },
          },
          policiesByOwner: [
            {
              inherited: false,
              ownerId: 'ROOT_ORGANIZATION_ID',
              ownerName: 'Root Organization',
              ownerType: 'organizations',
              policies: [
                {
                  hasLocalActionsOverrides: undefined,
                  id: '9bd6ecd914f04e41bc983a9421f31b12',
                  name: 'not allowed to override',
                  threatLevel: 10,
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  actions: { build: 'warn', develop: 'warn', operate: 'fail', release: 'fail', source: 'fail' },
                },
                {
                  hasLocalActionsOverrides: undefined,
                  id: 'e2cb70070ce540f7af0af8478b4d8bd9',
                  name: 'fresh allowed to override',
                  threatLevel: 5,
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  actions: { build: 'warn', develop: 'fail' },
                },
                {
                  hasLocalActionsOverrides: undefined,
                  id: '787822577d384a28b59c9d06ff6d37e2',
                  name: 'allowed to override',
                  threatLevel: 7,
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  actions: { build: 'warn', release: 'fail' },
                },
              ],
            },
          ],
        },
      });

      const action = {
        type: 'policy/changeSortField',
        payload: { key: 'build', dir: 'asc', ownerName: 'Root Organization' },
      };

      const { policyTile } = reducer(state, action);

      expect(policyTile.sorting).toEqual({
        'Root Organization': { key: 'build', dir: 'asc', ownerName: 'Root Organization' },
      });
      expect(policyTile.policiesByOwner[0].policies[0].threatLevel).toBe(10);
      expect(policyTile.policiesByOwner[0].policies[0].name).toBe('not allowed to override');
      expect(policyTile.policiesByOwner[0].policies[1].threatLevel).toBe(5);
      expect(policyTile.policiesByOwner[0].policies[1].name).toBe('fresh allowed to override');
      expect(policyTile.policiesByOwner[0].policies[2].threatLevel).toBe(7);
      expect(policyTile.policiesByOwner[0].policies[2].name).toBe('allowed to override');
    });
  });

  describe('policy/loadPolicyTile/failed', () => {
    it('resets loading and sets loadError for policy tile', () => {
      const state = Object.freeze({ policyTile: { loadError: null, loading: true } });

      const { policyTile } = reducer(state, {
        type: 'policy/loadPolicyTile/rejected',
        payload: 'error',
      });

      expect(policyTile.loadError).toBe('error');
      expect(policyTile.loading).toBeFalse();
    });
  });
});
