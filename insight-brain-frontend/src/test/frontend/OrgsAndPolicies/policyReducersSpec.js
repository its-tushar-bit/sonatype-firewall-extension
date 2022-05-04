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
    it('sets loadError and loading', () => {
      const state = Object.freeze({ loadError: 'error', loading: false });

      const { loadError, loading } = reducer(state, {
        type: 'policy/loadCategoriesForPolicy/pending',
      });

      expect(loadError).toBeNull();
      expect(loading).toBeTrue();
    });
  });

  describe('policy/loadCategoriesForPolicy/fulfilled', () => {
    it('sets payload into state', () => {
      const state = Object.freeze({
        loadError: 'error',
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
        loadError,
        loading,
        hasPolicyCategories,
        originalHasPolicyCategories,
        categories,
        originalCategories,
      } = reducer(state, {
        type: 'policy/loadCategoriesForPolicy/fulfilled',
        payload: fulfilledPayload,
      });

      expect(loadError).toBeNull();
      expect(loading).toBeFalse();
      expect(hasPolicyCategories).toBeTrue();
      expect(originalHasPolicyCategories).toBeTrue();
      expect(categories).toEqual([]);
      expect(originalCategories).toEqual(categories);
    });
  });

  describe('policy/loadCategoriesForPolicy/failed', () => {
    it('sets loadError and loading', () => {
      const state = Object.freeze({ loadError: null, loading: true });

      const { loadError, loading } = reducer(state, {
        type: 'policy/loadCategoriesForPolicy/rejected',
        payload: 'error',
      });

      expect(loadError).toBe('error');
      expect(loading).toBeFalse();
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
        readOnly: false,
        isOrgOwner: false,
        isRootOrg: false,
        originalProxyStageAction: null,
      });

      const fulfilledPayload = {
        currentPolicy: { id: 'someId' },
        currentPolicyOwner: { id: 'ownerId', name: 'ownerName' },
        siblings: [{ id: 'anotherPolicyId' }],
        readOnly: true,
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
        readOnly,
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
      expect(readOnly).toBe(fulfilledPayload.readOnly);
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
});
