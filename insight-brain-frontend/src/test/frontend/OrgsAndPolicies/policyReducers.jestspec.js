/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { clone, omit } from 'ramda';
import reducer, { initialState } from 'MainRoot/OrgsAndPolicies/policySlice';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { ageValidator, getCoordinatesValidator } from 'MainRoot/OrgsAndPolicies/utility/constraintUtil';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('policySlice reducers', () => {
  describe('policy/setPolicyName', () => {
    it('sets currentPolicy name', () => {
      const state = Object.freeze({
        currentPolicy: {
          name: null,
          constraints: [],
        },
        siblings: [],
        originalPolicy: { name: { value: '' } },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setPolicyName',
        payload: 'newName',
      });

      expect(currentPolicy.name).toEqual({
        isPristine: false,
        value: 'newName',
        trimmedValue: 'newName',
        validationErrors: [],
      });
    });

    it('sets currentPolicy name with validation errors', () => {
      const state = Object.freeze({
        currentPolicy: {
          name: null,
          constraints: [],
        },
        siblings: [],
        originalPolicy: { name: { value: 'newName' } },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setPolicyName',
        payload: 'newName this is a really long string for  validation...........................!',
      });

      expect(currentPolicy.name).toEqual({
        isPristine: false,
        value: 'newName this is a really long string for  validation...........................!',
        trimmedValue: 'newName this is a really long string for  validation...........................!',
        validationErrors: [
          'Use valid characters: alphanumeric, "_", ".", "-", or spaces',
          'Please enter less than 60 characters',
          'No leading, trailing or double spaces or tabs',
        ],
      });
    });

    it('sets currentPolicy name with validation empty error', () => {
      const state = Object.freeze({
        currentPolicy: {
          name: null,
          constraints: [],
        },
        siblings: [],
        originalPolicy: { name: { value: 'newName' } },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setPolicyName',
        payload: '',
      });

      expect(currentPolicy.name).toEqual({
        isPristine: false,
        value: '',
        trimmedValue: '',
        validationErrors: ['Must be non-empty'],
      });
    });

    it('sets currentPolicy name with validation duplicate error', () => {
      const state = Object.freeze({
        currentPolicy: {
          name: null,
          constraints: [],
        },
        siblings: [{ name: 'newName' }],
        originalPolicy: { name: { value: '' } },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setPolicyName',
        payload: 'newName',
      });

      expect(currentPolicy.name).toEqual({
        isPristine: false,
        value: 'newName',
        trimmedValue: 'newName',
        validationErrors: ['Name is already in use'],
      });
    });
  });

  describe('policy/setThreatLevel', () => {
    it('sets currentPolicy threatLevel', () => {
      const state = Object.freeze({
        currentPolicy: {
          threatLevel: 1,
          constraints: [],
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setThreatLevel',
        payload: 5,
      });

      expect(currentPolicy.threatLevel).toBe(5);
    });
  });

  describe('policy/setActions', () => {
    it('sets actions', () => {
      const state = Object.freeze({
        currentPolicy: {
          actions: null,
          constraints: [],
        },
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
        currentPolicy: {
          constraints: [],
        },
        hasPolicyCategories: false,
      });

      const { hasPolicyCategories } = reducer(state, {
        type: 'policy/setHasPolicyCategories',
        payload: true,
      });

      expect(hasPolicyCategories).toBe(true);
    });
  });

  describe('policy/saveMaskTimerDone', () => {
    it('nulls submitMaskState', () => {
      const state = Object.freeze({
        submitMaskState: true,
      });

      const { submitMaskState } = reducer(state, {
        type: 'policy/saveMaskTimerDone',
      });

      expect(submitMaskState).toBeNull();
    });
  });

  describe('policy/clearDeleteError', () => {
    it('clears deleteError', () => {
      const state = Object.freeze({
        deleteError: true,
      });

      const { deleteError } = reducer(state, {
        type: 'policy/clearDeleteError',
      });

      expect(deleteError).toBeNull();
    });
  });

  describe('policy/toggleLegacyViolationAllowed', () => {
    it('toggles LegacyViolationAllowed for a category', () => {
      const state = Object.freeze({
        currentPolicy: { legacyViolationAllowed: false, constraints: [] },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/toggleLegacyViolationAllowed',
      });

      expect(currentPolicy.legacyViolationAllowed).toBe(true);
    });
  });

  describe('policy/togglePolicyActionsOverrideAllowed', () => {
    it('toggles policyActionsOverrideAllowed for a category from false to true', () => {
      const state = Object.freeze({
        currentPolicy: { policyActionsOverrideAllowed: false, constraints: [] },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/togglePolicyActionsOverrideAllowed',
      });

      expect(currentPolicy.policyActionsOverrideAllowed).toBe(true);
    });

    it('toggles policyActionsOverrideAllowed for a category from true to false', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [],
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

      const { currentPolicy } = reducer(state, {
        type: 'policy/togglePolicyActionsOverrideAllowed',
      });

      expect(currentPolicy.policyActionsOverrideAllowed).toBe(false);
      expect(currentPolicy.policyActionsOverrides).toBeNull();
    });
  });

  describe('policy/togglePolicyNotificationsOverrideAllowed', () => {
    it('toggles policyNotificationsOverrideAllowed for a category from false to true', () => {
      const state = Object.freeze({
        currentPolicy: { policyNotificationsOverrideAllowed: false, constraints: [] },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/togglePolicyNotificationsOverrideAllowed',
      });

      expect(currentPolicy.policyNotificationsOverrideAllowed).toBe(true);
    });

    it('toggles policyNotificationsOverrideAllowed for a category from true to false', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [],
          policyNotificationsOverrideAllowed: true,
          policyNotificationsOverrides: {
            df9ad82193e44f4f9385e0c9e8835409: {
              userNotifications: [{ emailAddress: 'user2@email.com', stageIds: ['build', 'release'] }],
            },
          },
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/togglePolicyNotificationsOverrideAllowed',
      });

      expect(currentPolicy.policyNotificationsOverrideAllowed).toBe(false);
      expect(currentPolicy.policyNotificationsOverrides).toBeNull();
    });
  });

  describe('policy/setConstraint', () => {
    it('sets currentPolicy constraints', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [],
        },
        originalPolicy: {
          constraints: [],
        },
      });

      const constraints = [
        {
          id: 'someId',
          conditions: [
            {
              conditionTypeId: 'AgeInDays',
              operator: 'older than',
              value: {
                value: '730',
                trimmedValue: '730',
              },
            },
          ],
          operator: 'OR',
        },
      ];

      const { currentPolicy } = reducer(state, {
        type: 'policy/setConstraint',
        payload: constraints,
      });

      expect(currentPolicy.constraints).toEqual(constraints);
    });

    it('sets currentPolicy constraints', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [],
        },
        originalPolicy: {
          constraints: [],
        },
      });

      const constraints = [
        {
          id: 'someId',
          conditions: [
            {
              conditionTypeId: 'AgeInDays',
              operator: 'older than',
              value: {
                value: '',
                trimmedValue: '',
              },
            },
          ],
          operator: 'OR',
        },
      ];

      const { currentPolicy } = reducer(state, {
        type: 'policy/setConstraint',
        payload: constraints,
      });

      expect(currentPolicy.constraints).toEqual(constraints);
    });
  });

  describe('policy/setUserNotifications', () => {
    it('sets userNotifications', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [],
          notifications: { userNotifications: null },
        },
      });
      const userNotifications = [{ emailAddress: 'df@sd.com', stageIds: [] }];

      const { currentPolicy } = reducer(state, {
        type: 'policy/setUserNotifications',
        payload: { notifications: userNotifications, ownerId: null },
      });

      expect(currentPolicy.notifications.userNotifications).toEqual(userNotifications);
    });
  });

  describe('policy/setRoleNotifications', () => {
    it('sets roleNotifications', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [],
          notifications: { roleNotifications: null },
        },
      });
      const roleNotifications = [{ roleId: '90c7c98683b4471cb77a916744540bcc', stageIds: [] }];

      const { currentPolicy } = reducer(state, {
        type: 'policy/setRoleNotifications',
        payload: { notifications: roleNotifications, ownerId: null },
      });

      expect(currentPolicy.notifications.roleNotifications).toEqual(roleNotifications);
    });
  });

  describe('policy/setJiraNotifications', () => {
    it('sets jiraNotifications', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [],
          notifications: { jiraNotifications: null },
        },
      });

      const jiraNotifications = [{ projectKey: 'somekey', stageIds: [] }];

      const { currentPolicy } = reducer(state, {
        type: 'policy/setJiraNotifications',
        payload: { notifications: jiraNotifications, ownerId: null },
      });

      expect(currentPolicy.notifications.jiraNotifications).toEqual(jiraNotifications);
    });
  });

  describe('policy/setWebhookNotifications', () => {
    it('sets webhookNotifications', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [],
          notifications: { webhookNotifications: null },
        },
      });

      const webhookNotifications = [{ webhookId: 'someid', stageIds: [] }];

      const { currentPolicy } = reducer(state, {
        type: 'policy/setWebhookNotifications',
        payload: { notifications: webhookNotifications, ownerId: null },
      });

      expect(currentPolicy.notifications.webhookNotifications).toEqual(webhookNotifications);
    });
  });

  describe('policy/setUserNotificationStageIds', () => {
    it('sets stageIds in userNotifications', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [],
          notifications: {
            userNotifications: [
              { emailAddress: 'someEmail', stageIds: [] },
              { emailAddress: 'someOtherEmail', stageIds: [] },
            ],
          },
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setUserNotificationStageIds',
        payload: { index: 1, value: ['develop'] },
      });

      expect(currentPolicy.notifications.userNotifications[0].stageIds).toEqual([]);
      expect(currentPolicy.notifications.userNotifications[1].stageIds).toEqual(['develop']);
    });
  });

  describe('policy/setRoleNotificationStageIds', () => {
    it('sets stageIds in roleNotifications', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [],
          notifications: {
            roleNotifications: [
              { roleId: 'someId', stageIds: [] },
              { roleId: 'someOtherId', stageIds: [] },
            ],
          },
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setRoleNotificationStageIds',
        payload: { index: 1, value: ['develop'] },
      });

      expect(currentPolicy.notifications.roleNotifications[0].stageIds).toEqual([]);
      expect(currentPolicy.notifications.roleNotifications[1].stageIds).toEqual(['develop']);
    });
  });

  describe('policy/setJiraNotificationStageIds', () => {
    it('sets stageIds in jiraNotifications', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [],
          notifications: {
            jiraNotifications: [
              { projectKey: 'test', stageIds: [] },
              { projectKey: 'test2', stageIds: [] },
            ],
          },
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setJiraNotificationStageIds',
        payload: { index: 1, value: ['develop'] },
      });

      expect(currentPolicy.notifications.jiraNotifications[0].stageIds).toEqual([]);
      expect(currentPolicy.notifications.jiraNotifications[1].stageIds).toEqual(['develop']);
    });
  });

  describe('policy/setWebhookNotificationStageIds', () => {
    it('sets stageIds in webhookNotifications', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [],
          notifications: {
            webhookNotifications: [
              { webhookId: 'test', stageIds: [] },
              { webhookId: 'test2', stageIds: [] },
            ],
          },
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setWebhookNotificationStageIds',
        payload: { index: 1, value: ['develop'] },
      });

      expect(currentPolicy.notifications.webhookNotifications[0].stageIds).toEqual([]);
      expect(currentPolicy.notifications.webhookNotifications[1].stageIds).toEqual(['develop']);
    });
  });

  describe('policy/setCondition', () => {
    it('sets conditions for currentPolicy constraints', () => {
      const state = Object.freeze({
        originalPolicy: {
          constraints: [],
        },
        currentPolicy: {
          constraints: [
            {
              id: 'someId',
              conditions: [],
              operator: 'OR',
            },
          ],
        },
      });
      const conditions = [
        {
          conditionTypeId: 'AgeInDays',
          operator: 'older than',
          value: initUserInput(''),
        },
      ];

      const { currentPolicy } = reducer(state, {
        type: 'policy/setCondition',
        payload: { constraintIndex: 1, value: conditions },
      });

      expect(currentPolicy.constraints[0].conditions).toEqual([]);
      expect(currentPolicy.constraints[1].conditions).toEqual(conditions);
    });
  });

  describe('policy/setConstraintName', () => {
    it('sets name for currentPolicy constraints', () => {
      const state = Object.freeze({
        originalPolicy: {
          constraints: [],
        },
        currentPolicy: {
          constraints: [
            {
              id: 'someId',
              conditions: [],
              name: initUserInput(''),
              operator: 'OR',
            },
            {
              id: 'someOtherId',
              conditions: [],
              operator: 'OR',
              name: initUserInput(''),
            },
          ],
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setConstraintName',
        payload: { constraintIndex: 1, value: 'newName', id: 'someOtherId' },
      });

      expect(currentPolicy.constraints[0].name.value).toBe('');
      expect(currentPolicy.constraints[1].name.value).toBe('newName');
    });

    it('sets duplicated name for currentPolicy constraint', () => {
      const state = Object.freeze({
        originalPolicy: {
          constraints: [],
        },
        currentPolicy: {
          constraints: [
            {
              id: 'someId',
              conditions: [],
              name: initUserInput('duplicate'),
              operator: 'OR',
            },
            {
              id: 'someOtherId',
              conditions: [],
              operator: 'OR',
              name: initUserInput(''),
            },
          ],
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setConstraintName',
        payload: { constraintIndex: 1, value: 'duplicate', id: 'someOtherId' },
      });

      expect(currentPolicy.constraints[0].name.value).toBe('duplicate');
      expect(currentPolicy.constraints[1].name.value).toBe('duplicate');
    });
  });

  describe('policy/setConstraintOperator', () => {
    it('sets operator for currentPolicy constraints', () => {
      const state = Object.freeze({
        originalPolicy: {
          constraints: [],
        },
        currentPolicy: {
          constraints: [
            {
              id: 'someId',
              operator: null,
              conditions: [],
            },
            {
              id: 'someOtherId',
              operator: 'OR',
              conditions: [],
            },
          ],
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setConstraintOperator',
        payload: { constraintIndex: 1, value: 'AND' },
      });

      expect(currentPolicy.constraints[0].operator).toBeNull();
      expect(currentPolicy.constraints[1].operator).toBe('AND');
    });
  });

  describe('policy/setConditionOperator', () => {
    it('sets operator', () => {
      const state = Object.freeze({
        originalPolicy: {
          constraints: [],
        },
        currentPolicy: {
          constraints: [
            {
              id: 'test',
              conditions: [],
            },
            {
              id: 'someOtherId',
              conditions: [{ operator: null }, { operator: null }],
            },
          ],
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setConditionOperator',
        payload: { constraintIndex: 1, conditionIndex: 1, value: 'AND' },
      });

      expect(currentPolicy.constraints[1].conditions[0].operator).toBeNull();
      expect(currentPolicy.constraints[1].conditions[1].operator).toBe('AND');
    });
  });

  describe('policy/setConditionValue', () => {
    it('sets value', () => {
      const state = Object.freeze({
        originalPolicy: {
          constraints: [],
        },
        currentPolicy: {
          constraints: [
            {
              id: 'test',
              conditions: [],
            },
            {
              id: 'someOtherId',
              conditions: [{ value: null }, { value: null }],
            },
          ],
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setConditionValue',
        payload: { constraintIndex: 1, conditionIndex: 1, value: 'someValue' },
      });

      expect(currentPolicy.constraints[1].conditions[0].value).toBeNull();
      expect(currentPolicy.constraints[1].conditions[1].value).toBe('someValue');
    });
  });

  describe('policy/setConstraintCoordinatesFormat', () => {
    it('sets value to a-name', () => {
      const state = Object.freeze({
        originalPolicy: {
          conditions: [],
        },
        currentPolicy: {
          constraints: [
            {
              id: 'someId',
              conditions: [
                {
                  conditionTypeId: 'Coordinates',
                  operator: 'match',
                  value: {
                    format: 'maven',
                    groupId: initUserInput('', getCoordinatesValidator('groupId', 'maven')),
                    artifactId: initUserInput('', getCoordinatesValidator('artifactId', 'maven')),
                    version: initUserInput('', getCoordinatesValidator('version', 'maven')),
                    extension: initUserInput('*', getCoordinatesValidator('extension', 'maven')),
                    classifier: initUserInput('*', getCoordinatesValidator('classifier', 'maven')),
                  },
                },
              ],
            },
          ],
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setConstraintCoordinatesFormat',
        payload: { constraintIndex: 0, conditionIndex: 0, value: 'a-name' },
      });

      expect(currentPolicy.constraints[0].conditions[0]).toEqual({
        conditionTypeId: 'Coordinates',
        operator: 'match',
        value: {
          format: 'a-name',
          name: initUserInput('', getCoordinatesValidator('name', 'a-name')),
          qualifier: initUserInput('*', getCoordinatesValidator('qualifier', 'a-name')),
          version: initUserInput('', getCoordinatesValidator('version', 'a-name')),
        },
      });
    });

    it('sets value to pypi', () => {
      const state = Object.freeze({
        originalPolicy: {
          conditions: [],
        },
        currentPolicy: {
          constraints: [
            {
              id: 'someId',
              conditions: [
                {
                  conditionTypeId: 'Coordinates',
                  operator: 'match',
                  value: {
                    format: 'maven',
                    groupId: initUserInput('', getCoordinatesValidator('groupId', 'maven')),
                    artifactId: initUserInput('', getCoordinatesValidator('artifactId', 'maven')),
                    version: initUserInput('', getCoordinatesValidator('version', 'maven')),
                    extension: initUserInput('*', getCoordinatesValidator('extension', 'maven')),
                    classifier: initUserInput('*', getCoordinatesValidator('classifier', 'maven')),
                  },
                },
              ],
            },
          ],
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setConstraintCoordinatesFormat',
        payload: { constraintIndex: 0, conditionIndex: 0, value: 'pypi' },
      });

      expect(currentPolicy.constraints[0].conditions[0]).toEqual({
        conditionTypeId: 'Coordinates',
        operator: 'match',
        value: {
          format: 'pypi',
          name: initUserInput('', getCoordinatesValidator('name', 'pypi')),
          qualifier: initUserInput('*', getCoordinatesValidator('qualifier', 'pypi')),
          version: initUserInput('', getCoordinatesValidator('version', 'pypi')),
          extension: initUserInput('*', getCoordinatesValidator('extension', 'pypi')),
        },
      });
    });
  });

  describe('policy/setConstraintCoordinatesInput', () => {
    it('sets value', () => {
      const state = Object.freeze({
        originalPolicy: {
          conditions: [],
        },
        currentPolicy: {
          constraints: [
            {
              id: 'someId',
              conditions: [
                {
                  conditionTypeId: 'Coordinates',
                  operator: 'match',
                  value: {
                    format: 'maven',
                    groupId: initUserInput('', getCoordinatesValidator('groupId', 'maven')),
                    artifactId: initUserInput('', getCoordinatesValidator('artifactId', 'maven')),
                    version: initUserInput('', getCoordinatesValidator('version', 'maven')),
                    extension: initUserInput('*', getCoordinatesValidator('extension', 'maven')),
                    classifier: initUserInput('*', getCoordinatesValidator('classifier', 'maven')),
                  },
                },
              ],
            },
          ],
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setConstraintCoordinatesInput',
        payload: { constraintIndex: 0, conditionIndex: 0, value: 'version value', name: 'version', format: 'maven' },
      });

      expect(currentPolicy.constraints[0].conditions[0]).toEqual({
        conditionTypeId: 'Coordinates',
        operator: 'match',
        value: {
          format: 'maven',
          groupId: initUserInput('', getCoordinatesValidator('groupId', 'maven')),
          artifactId: initUserInput('', getCoordinatesValidator('artifactId', 'maven')),
          version: {
            value: 'version value',
            trimmedValue: 'version value',
            isPristine: false,
            validationErrors: [],
          },
          extension: initUserInput('*', getCoordinatesValidator('extension', 'maven')),
          classifier: initUserInput('*', getCoordinatesValidator('classifier', 'maven')),
        },
      });
    });
  });

  describe('policy/setConstraintConditionAgeField', () => {
    it('sets value, sets age field validation errors', () => {
      const state = Object.freeze({
        originalPolicy: {
          conditions: [],
        },
        currentPolicy: {
          constraints: [
            {
              id: 'someId',
              conditions: [
                {
                  conditionTypeId: 'AgeInDays',
                  operator: 'older than',
                  value: initUserInput('730'),
                },
              ],
            },
          ],
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setConditionAgeValue',
        payload: { constraintIndex: 0, conditionIndex: 0, value: '0' },
      });

      expect(currentPolicy.constraints[0].conditions[0].value).toEqual({
        value: '0',
        trimmedValue: '0',
        isPristine: false,
        validationErrors: ['Minimum allowed value is 1'],
      });
    });

    it('sets value, sets age field validation errors', () => {
      const state = Object.freeze({
        originalPolicy: {
          conditions: [],
        },
        currentPolicy: {
          constraints: [
            {
              id: 'someId',
              conditions: [
                {
                  conditionTypeId: 'AgeInDays',
                  operator: 'older than',
                  value: initUserInput('730'),
                },
              ],
            },
          ],
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setConditionAgeValue',
        payload: { constraintIndex: 0, conditionIndex: 0, value: '' },
      });

      expect(currentPolicy.constraints[0].conditions[0].value).toEqual({
        value: '',
        trimmedValue: '',
        isPristine: false,
        validationErrors: ['Must be non-empty', 'Minimum allowed value is 1'],
      });
    });
  });

  describe('setMultiInputConditionValue', () => {
    it('sets value, sets age field validation errors for PercentageValueType', () => {
      const state = Object.freeze({
        originalPolicy: {
          conditions: [],
        },
        currentPolicy: {
          constraints: [],
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setMultiInputConditionValue',
        payload: {
          constraintIndex: 0,
          conditionIndex: 0,
          value: '',
          dataType: 'Integer',
          valueTypeId: 'PercentageValueType',
        },
      });

      expect(currentPolicy.constraints[0].conditions[0].value).toEqual({
        value: '',
        trimmedValue: '',
        isPristine: false,
        validationErrors: ['Must be non-empty', 'Value must be from 0 to 100'],
      });
    });

    it('sets value, sets age field validation errors for PackageUrlValueType', () => {
      const state = Object.freeze({
        originalPolicy: {
          conditions: [],
        },
        currentPolicy: {
          constraints: [],
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/setMultiInputConditionValue',
        payload: {
          constraintIndex: 0,
          conditionIndex: 0,
          value: '',
          dataType: 'String',
          valueTypeId: 'PackageUrlValueType',
        },
      });

      expect(currentPolicy.constraints[0].conditions[0].value).toEqual({
        value: '',
        trimmedValue: '',
        isPristine: false,
        validationErrors: ['Must be non-empty', 'Value must be a valid Package URL: pkg:type/name@version'],
      });
    });
  });

  describe('policy/setConstraintCondition', () => {
    it('sets conditions', () => {
      const state = Object.freeze({
        originalPolicy: {
          conditions: [],
        },
        currentPolicy: {
          constraints: [
            { id: 'test', conditions: [] },
            {
              id: 'someOtherId',
              conditions: [
                {
                  conditionTypeId: 'MatchState',
                  operator: 'is',
                  value: initUserInput('similar'),
                  conditionIndex: 0,
                },
              ],
            },
          ],
        },
      });
      const newCondition = {
        conditionTypeId: 'AgeInDays',
        operator: 'older than',
        value: '',
      };

      const { currentPolicy } = reducer(state, {
        type: 'policy/setConstraintCondition',
        payload: { constraintIndex: 1, conditionIndex: 0, value: newCondition },
      });

      expect(currentPolicy.constraints[0].conditions).toEqual([]);
      expect(currentPolicy.constraints[1].conditions[0]).toEqual({
        conditionTypeId: 'AgeInDays',
        operator: 'older than',
        value: initUserInput('', ageValidator),
      });
    });

    it('sets conditions for coordinates case', () => {
      const state = Object.freeze({
        originalPolicy: {
          conditions: [],
        },
        currentPolicy: {
          constraints: [
            { id: 'test', conditions: [] },
            {
              id: 'someOtherId',
              conditions: [
                {
                  conditionTypeId: 'MatchState',
                  operator: 'is',
                  value: initUserInput('similar'),
                  conditionIndex: 0,
                },
              ],
            },
          ],
        },
      });
      const newCondition = {
        conditionTypeId: 'Coordinates',
        operator: 'match',
        value: '',
      };

      const { currentPolicy } = reducer(state, {
        type: 'policy/setConstraintCondition',
        payload: { constraintIndex: 1, conditionIndex: 0, value: newCondition },
      });

      expect(currentPolicy.constraints[0].conditions).toEqual([]);
      expect(currentPolicy.constraints[1].conditions[0]).toEqual({
        conditionTypeId: 'Coordinates',
        operator: 'match',
        value: {
          format: 'maven',
          groupId: initUserInput('', getCoordinatesValidator('groupId', 'maven')),
          artifactId: initUserInput('', getCoordinatesValidator('artifactId', 'maven')),
          version: initUserInput('', getCoordinatesValidator('version', 'maven')),
          extension: initUserInput('*', getCoordinatesValidator('extension', 'maven')),
          classifier: initUserInput('*', getCoordinatesValidator('classifier', 'maven')),
        },
      });
    });
  });

  describe('policy/loadCategoriesForPolicy/pending', () => {
    it('sets categoriesForPolicyLoadError and loading', () => {
      const state = Object.freeze({ categoriesForPolicyLoadError: 'error', loadingCategories: false });

      const { categoriesForPolicyLoadError, loadingCategories } = reducer(state, {
        type: 'policy/loadCategoriesForPolicy/pending',
      });

      expect(categoriesForPolicyLoadError).toBeNull();
      expect(loadingCategories).toBe(true);
    });
  });

  describe('policy/loadCategoriesForPolicy/fulfilled', () => {
    it('sets payload into state', () => {
      const state = Object.freeze({
        categoriesForPolicyLoadError: 'error',
        loadingCategories: false,
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
        loadingCategories,
        hasPolicyCategories,
        originalHasPolicyCategories,
        categories,
        originalCategories,
      } = reducer(state, {
        type: 'policy/loadCategoriesForPolicy/fulfilled',
        payload: fulfilledPayload,
      });

      expect(categoriesForPolicyLoadError).toBeNull();
      expect(loadingCategories).toBe(false);
      expect(hasPolicyCategories).toBe(true);
      expect(originalHasPolicyCategories).toBe(true);
      expect(categories).toEqual([]);
      expect(originalCategories).toEqual(categories);
    });
  });

  describe('policy/loadCategoriesForPolicy/rejected', () => {
    it('sets loadError and loading', () => {
      const state = Object.freeze({
        categoriesForPolicyLoadError: null,
        loadingCategories: true,
        currentPolicy: { name: 'current' },
        originalPolicy: { name: 'initial' },
      });

      const { categoriesForPolicyLoadError, loadingCategories, currentPolicy } = reducer(state, {
        type: 'policy/loadCategoriesForPolicy/rejected',
        payload: 'error',
      });

      expect(categoriesForPolicyLoadError).toBe('error');
      expect(loadingCategories).toBe(false);
      expect(currentPolicy).toEqual(state.originalPolicy);
    });
  });

  describe('policy/checkEditIqPermission/fulfilled', () => {
    it('sets hasEditIqPermission', () => {
      const state = Object.freeze({ hasEditIqPermission: null });

      const { hasEditIqPermission } = reducer(state, {
        type: 'policy/checkEditIqPermission/fulfilled',
      });

      expect(hasEditIqPermission).toBe(true);
    });
  });

  describe('policy/checkEditIqPermission/rejected', () => {
    it('clears hasEditIqPermission', () => {
      const state = Object.freeze({ hasEditIqPermission: null });

      const { hasEditIqPermission } = reducer(state, {
        type: 'policy/checkEditIqPermission/rejected',
      });

      expect(hasEditIqPermission).toBe(false);
    });
  });

  describe('policy/loadPolicyEditor/pending', () => {
    it('sets loadError and loading', () => {
      const state = Object.freeze({ loadError: 'error', loadingPolicyEditor: false });

      const { loadError, loadingPolicyEditor } = reducer(state, {
        type: 'policy/loadPolicyEditor/pending',
      });

      expect(loadError).toBeNull();
      expect(loadingPolicyEditor).toBe(true);
    });
  });

  describe('policy/loadPolicyEditor/fulfilled', () => {
    it('sets payload into the state', () => {
      const state = Object.freeze({
        loadError: 'error',
        loadingPolicyEditor: true,
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
        loadingPolicyEditor,
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
      expect(loadingPolicyEditor).toBe(false);
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
      const state = Object.freeze({ loadError: null, loadingPolicyEditor: true });

      const { loadError, loadingPolicyEditor } = reducer(state, {
        type: 'policy/loadPolicyEditor/rejected',
        payload: 'error',
      });

      expect(loadError).toBe('error');
      expect(loadingPolicyEditor).toBe(false);
    });
  });

  describe('policy/savePolicy/pending', () => {
    it('sets loadError and loading', () => {
      const state = Object.freeze({ loadError: 'error', submitMaskState: null });

      const { loadError, submitMaskState } = reducer(state, {
        type: 'policy/savePolicy/pending',
      });

      expect(loadError).toBeNull();
      expect(submitMaskState).toBe(false);
    });
  });

  describe('policy/savePolicy/fulfilled', () => {
    it('updates originals and siblings when payload has isEditMode', () => {
      const state = Object.freeze({
        loadError: 'error',
        loadingSavePolicy: false,
        originalPolicy: null,
        currentPolicy: { id: 'somePolicyId', actions: { proxy: 'warn' } },
        categories: [],
        originalCategories: null,
        originalHasPolicyCategories: false,
        hasPolicyCategories: true,
        originalProxyStageAction: null,
        submitMaskState: false,
        siblings: [{ id: 'somePolicyId' }, { id: 'someOtherPolicyId' }],
      });

      const {
        loadError,
        loadingSavePolicy,
        originalPolicy,
        originalCategories,
        originalHasPolicyCategories,
        originalProxyStageAction,
        siblings,
        submitMaskState,
      } = reducer(state, {
        type: 'policy/savePolicy/fulfilled',
        payload: { isEditMode: true },
      });

      expect(loadError).toBeNull();
      expect(loadingSavePolicy).toBe(false);
      expect(submitMaskState).toBe(true);
      expect(originalPolicy).toEqual(state.currentPolicy);
      expect(originalCategories).toEqual(state.categories);
      expect(originalHasPolicyCategories).toBe(state.hasPolicyCategories);
      expect(originalProxyStageAction).toBe(state.currentPolicy.actions['proxy']);
      expect(siblings).toEqual([{ id: 'somePolicyId', actions: { proxy: 'warn' } }, { id: 'someOtherPolicyId' }]);
    });

    it('should not update originals and siblings when payload has isEditMode', () => {
      const state = Object.freeze({
        loadError: 'error',
        loadingSavePolicy: false,
        originalPolicy: null,
        currentPolicy: { id: 'somePolicyId', actions: { proxy: 'warn' } },
        categories: [],
        originalCategories: null,
        originalHasPolicyCategories: false,
        hasPolicyCategories: true,
        originalProxyStageAction: null,
        submitMaskState: false,
        siblings: [{ id: 'somePolicyId' }, { id: 'someOtherPolicyId' }],
      });

      const {
        loadError,
        loadingSavePolicy,
        originalPolicy,
        originalCategories,
        originalHasPolicyCategories,
        originalProxyStageAction,
        siblings,
        currentPolicy,
        submitMaskState,
      } = reducer(state, {
        type: 'policy/savePolicy/fulfilled',
      });

      expect(loadError).toBeNull();
      expect(loadingSavePolicy).toBe(false);
      expect(submitMaskState).toBe(true);
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
      const state = Object.freeze({ loadError: null, submitMaskState: true });

      const { loadError, submitMaskState } = reducer(state, {
        type: 'policy/savePolicy/rejected',
        payload: 'error',
      });

      expect(loadError).toBe('error');
      expect(submitMaskState).toBeNull();
    });
  });

  describe('policy/removePolicy/pending', () => {
    it('sets deleting', () => {
      const state = Object.freeze({ deleteError: 'error', submitMaskState: null });

      const { deleteError, submitMaskState } = reducer(state, {
        type: 'policy/removePolicy/pending',
      });

      expect(deleteError).toBeNull();
      expect(submitMaskState).toBe(false);
    });
  });

  describe('policy/removePolicy/fulfilled', () => {
    it('sets deleteModal states, updates currentPolicy, originalPolicy and siblings', () => {
      const state = Object.freeze({ deleteError: 'error', submitMaskState: false });

      const { deleteError, submitMaskState } = reducer(state, {
        type: 'policy/removePolicy/fulfilled',
      });

      expect(deleteError).toBeNull();
      expect(submitMaskState).toBe(true);
    });
  });

  describe('policy/removePolicy/failed', () => {
    it('sets deleting and errorState ', () => {
      const state = Object.freeze({ deleteError: null, submitMaskState: false });

      const { submitMaskState, deleteError } = reducer(state, {
        type: 'policy/removePolicy/rejected',
        payload: 'error',
      });

      expect(submitMaskState).toBeNull();
      expect(deleteError).toBe('error');
    });
  });

  describe('policy/setActionsOverride', () => {
    it('adds provided actions override for provided owner if override has changed', () => {
      const state = Object.freeze({
        isInherited: true,
        overrideActionsFlag: true,
        originalOverrideActionsFlag: false,
        currentPolicy: {
          constraints: [],
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

      const { currentPolicy } = reducer(state, action);

      expect(currentPolicy.policyActionsOverrides.someOwnerId).toEqual({ build: 'warn' });
      expect(currentPolicy.policyActionsOverrides.currentOwnerId).toEqual({ build: 'fail', release: 'fail' });
    });

    it('adds actions override if override has not changed', () => {
      const state = Object.freeze({
        isInherited: true,
        overrideActionsFlag: false,
        originalOverrideActionsFlag: false,
        currentPolicy: {
          constraints: [],
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

      const { currentPolicy } = reducer(state, action);

      expect(currentPolicy.policyActionsOverrides).toEqual(state.originalPolicy.policyActionsOverrides);
    });
  });

  describe('policy/setNotificationsOverride', () => {
    it('adds provided notifications override for provided owner if override has changed', () => {
      const state = Object.freeze({
        isInherited: true,
        overrideNotificationsFlag: true,
        originalOverrideNotificationsFlag: false,
        currentPolicy: {
          constraints: [],
          policyNotificationsOverrides: {
            someOwnerId: { userNotifications: [] },
          },
        },
        originalPolicy: {
          policyNotificationsOverrides: {
            someOwnerId: { userNotifications: [] },
          },
        },
      });

      const action = {
        type: 'policy/setNotificationsOverride',
        payload: {
          ownerId: 'currentOwnerId',
          notificationsOverride: { userNotifications: [{ emailAddress: 'user@email.com', stageIds: ['operate'] }] },
        },
      };

      const { currentPolicy } = reducer(state, action);

      expect(currentPolicy.policyNotificationsOverrides.someOwnerId).toEqual({ userNotifications: [] });
      expect(currentPolicy.policyNotificationsOverrides.currentOwnerId).toEqual({
        userNotifications: [{ emailAddress: 'user@email.com', stageIds: ['operate'] }],
      });
    });

    it('adds notifications override if override has not changed', () => {
      const state = Object.freeze({
        isInherited: true,
        overrideNotificationsFlag: false,
        originalOverrideNotificationsFlag: false,
        currentPolicy: {
          constraints: [],
          policyNotificationsOverrides: {
            someOwnerId: { userNotifications: [{ emailAddress: 'email@email.com', stageIds: ['build'] }] },
          },
        },
        originalPolicy: {
          policyNotificationsOverrides: {
            someOwnerId: { userNotifications: [{ emailAddress: 'email@email.com', stageIds: ['build'] }] },
            currentOwnerId: {
              userNotifications: [{ emailAddress: 'email2@email.com', stageIds: ['release', 'operate'] }],
            },
          },
        },
      });

      const action = {
        type: 'policy/setNotificationsOverride',
        payload: {
          ownerId: 'currentOwnerId',
          notificationsOverride: {
            userNotifications: [{ emailAddress: 'email2@email.com', stageIds: ['release', 'operate'] }],
          },
        },
      };

      const { currentPolicy } = reducer(state, action);

      expect(currentPolicy.policyNotificationsOverrides).toEqual(state.originalPolicy.policyNotificationsOverrides);
    });
  });

  describe('policy/updateOverrides/fulfilled', () => {
    it('resets loading flags and resets the policy from payload', () => {
      const state = Object.freeze({
        submitMaskState: false,
        loadError: 'some error',
        currentPolicy: { name: 'current policy' },
        originalPolicy: { name: 'original policy' },
      });

      const action = {
        type: 'policy/updateOverrides/fulfilled',
        payload: { name: 'updated policy' },
      };

      const { submitMaskState, loadError, currentPolicy, originalPolicy } = reducer(state, action);

      expect(submitMaskState).toBe(true);
      expect(loadError).toBe(null);
      expect(currentPolicy).toEqual({
        name: {
          isPristine: true,
          value: 'updated policy',
          trimmedValue: 'updated policy',
          validationErrors: [],
        },
      });
      expect(originalPolicy).toEqual({
        name: {
          isPristine: true,
          value: 'updated policy',
          trimmedValue: 'updated policy',
          validationErrors: [],
        },
      });
    });

    it('sets override action flags to true', () => {
      const state = Object.freeze({
        overrideActionsFlag: true,
        originalOverrideActionsFlag: false,
        currentPolicy: { name: 'current policy' },
        originalPolicy: { name: 'original policy' },
      });

      const action = {
        type: 'policy/updateOverrides/fulfilled',
        payload: { name: 'updated policy' },
      };

      const { overrideActionsFlag, originalOverrideActionsFlag } = reducer(state, action);

      expect(overrideActionsFlag).toBe(true);
      expect(originalOverrideActionsFlag).toBe(true);
    });

    it('sets override action flags to false', () => {
      const state = Object.freeze({
        overrideActionsFlag: false,
        originalOverrideActionsFlag: true,
        currentPolicy: { name: 'current policy' },
        originalPolicy: { name: 'original policy' },
      });

      const action = {
        type: 'policy/updateOverrides/fulfilled',
        payload: { name: 'updated policy' },
      };

      const { overrideActionsFlag, originalOverrideActionsFlag } = reducer(state, action);

      expect(overrideActionsFlag).toBeFalsy();
      expect(originalOverrideActionsFlag).toBeFalsy();
    });

    it('sets override notification flags to true', () => {
      const state = Object.freeze({
        overrideNotificationsFlag: true,
        originalOverrideNotificationsFlag: false,
        currentPolicy: { name: 'current policy' },
        originalPolicy: { name: 'original policy' },
      });

      const action = {
        type: 'policy/updateOverrides/fulfilled',
        payload: { name: 'updated policy' },
      };

      const { overrideNotificationsFlag, originalOverrideNotificationsFlag } = reducer(state, action);

      expect(overrideNotificationsFlag).toBe(true);
      expect(originalOverrideNotificationsFlag).toBe(true);
    });

    it('sets override notification flags to false', () => {
      const state = Object.freeze({
        overrideNotificationsFlag: false,
        originalOverrideNotificationsFlag: true,
        currentPolicy: { name: 'current policy' },
        originalPolicy: { name: 'original policy' },
      });

      const action = {
        type: 'policy/updateOverrides/fulfilled',
        payload: { name: 'updated policy' },
      };

      const { overrideNotificationsFlag, originalOverrideNotificationsFlag } = reducer(state, action);

      expect(overrideNotificationsFlag).toBeFalsy();
      expect(originalOverrideNotificationsFlag).toBeFalsy();
    });
  });

  describe('policy/updateOverrides/pending', () => {
    it('resets loadError and sets loading to true', () => {
      const state = Object.freeze({ loadError: 'error', submitMaskState: null });

      const { loadError, submitMaskState } = reducer(state, {
        type: 'policy/updateOverrides/pending',
      });

      expect(loadError).toBeNull();
      expect(submitMaskState).toBe(false);
    });
  });

  describe('policy/updateOverrides/failed', () => {
    it('resets loading and sets loadError', () => {
      const state = Object.freeze({ loadError: null, submitMaskState: false });

      const { loadError, submitMaskState } = reducer(state, {
        type: 'policy/updateOverrides/rejected',
        payload: 'error',
      });

      expect(loadError).toBe('error');
      expect(submitMaskState).toBeNull();
    });
  });

  describe('policy/setOverrideParentActions', () => {
    it('sets overrideActionsFlag ', () => {
      const state = Object.freeze({
        overrideActionsFlag: false,
        originalOverrideActionsFlag: false,
        isInherited: true,
        currentPolicy: {
          constraints: [],
        },
        originalPolicy: {
          policyActionsOverrides: {
            someOwnerId: { build: 'warn' },
            currentOwnerId: { build: 'fail', release: 'fail' },
          },
        },
      });

      const { overrideActionsFlag } = reducer(state, {
        type: 'policy/setOverrideParentActions',
        payload: true,
      });

      expect(overrideActionsFlag).toBe(true);
    });
  });

  describe('policy/setOverrideParentNotifications', () => {
    it('sets overrideNotificationsFlag', () => {
      const state = Object.freeze({
        overrideNotificationsFlag: false,
        originalOverrideNotificationsFlag: false,
        isInherited: true,
        currentPolicy: {
          constraints: [],
        },
        originalPolicy: {
          policyNotificationsOverrides: {
            someOwnerId: { userNotifications: [] },
            currentOwnerId: { userNotifications: [] },
          },
        },
      });

      const { overrideNotificationsFlag } = reducer(state, {
        type: 'policy/setOverrideParentNotifications',
        payload: true,
      });

      expect(overrideNotificationsFlag).toBe(true);
    });
  });

  describe('policy/unSetOverrideParentActions', () => {
    it('removes override from current policy, sets overrideActionsFlag', () => {
      const state = Object.freeze({
        overrideActionsFlag: true,
        originalOverrideActionsFlag: true,
        isInherited: true,
        currentPolicy: {
          constraints: [],
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

      const { overrideActionsFlag, currentPolicy } = reducer(state, {
        type: 'policy/unSetOverrideParentActions',
        payload: 'id201',
      });

      expect(overrideActionsFlag).toBe(false);
      expect(currentPolicy.policyActionsOverrides).toEqual({});
    });
  });

  describe('policy/unSetOverrideParentNotifications', () => {
    it('removes override from current policy, sets overrideNotificationsFlag', () => {
      const state = Object.freeze({
        overrideNotificationsFlag: true,
        originalOverrideNotificationsFlag: true,
        isInherited: true,
        currentPolicy: {
          constraints: [],
          policyNotificationsOverrides: {
            id201: { userNotifications: [] },
          },
        },
        originalPolicy: {
          policyNotificationsOverrides: null,
        },
      });

      const { overrideNotificationsFlag, currentPolicy } = reducer(state, {
        type: 'policy/unSetOverrideParentNotifications',
        payload: 'id201',
      });

      expect(overrideNotificationsFlag).toBe(false);
      expect(currentPolicy.policyNotificationsOverrides).toEqual({});
    });
  });

  describe('policy/addNotificationRecipient', () => {
    it('adds user notification recipient', () => {
      const state = Object.freeze({
        notificationsEditor: {
          formState: {
            recipientType: { value: 'Email', trimmedValue: 'Email' },
            recipientEmail: { value: ' nonTrimmedEmail@email.com ', trimmedValue: 'trimmedEmail@email.com' },
          },
        },
        currentPolicy: {
          constraints: [],
          notifications: {
            roleNotifications: [],
            userNotifications: [],
            webhookNotifications: [],
          },
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/addNotificationRecipient',
      });

      expect(currentPolicy.notifications).toEqual({
        roleNotifications: [],
        userNotifications: [{ emailAddress: 'trimmedEmail@email.com', stageIds: [] }],
        webhookNotifications: [],
      });
    });

    it('adds overridden user notification recipient', () => {
      const state = Object.freeze({
        isInherited: true,
        notificationsEditor: {
          formState: {
            recipientType: { value: 'Email', trimmedValue: 'Email' },
            recipientEmail: { value: ' nonTrimmedEmail@email.com ', trimmedValue: 'trimmedEmail@email.com' },
          },
        },
        currentPolicy: {
          constraints: [],
          notifications: {
            roleNotifications: [],
            userNotifications: [],
            webhookNotifications: [],
          },
          policyNotificationsOverrideAllowed: true,
          policyNotificationsOverrides: {
            orgId: {
              roleNotifications: [],
              userNotifications: [],
              webhookNotifications: [],
            },
          },
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/addNotificationRecipient',
        payload: 'orgId',
      });

      expect(currentPolicy.notifications).toEqual(state.currentPolicy.notifications);
      const expected = clone(state.currentPolicy.policyNotificationsOverrides);
      expected.orgId.userNotifications = [{ emailAddress: 'trimmedEmail@email.com', stageIds: [] }];
      expect(currentPolicy.policyNotificationsOverrides).toEqual(expected);
    });

    it('adds role notification recipient', () => {
      const state = Object.freeze({
        notificationsEditor: {
          formState: {
            recipientType: { value: 'Role', trimmedValue: 'Role' },
            recipientRoleId: { value: 'roleId', trimmedValue: 'roleId' },
          },
        },
        currentPolicy: {
          constraints: [],
          notifications: {
            roleNotifications: [],
            userNotifications: [],
            webhookNotifications: [],
          },
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/addNotificationRecipient',
      });

      expect(currentPolicy.notifications).toEqual({
        roleNotifications: [{ roleId: 'roleId', stageIds: [] }],
        userNotifications: [],
        webhookNotifications: [],
      });
    });

    it('adds overridden role notification recipient', () => {
      const state = Object.freeze({
        isInherited: true,
        notificationsEditor: {
          formState: {
            recipientType: { value: 'Role', trimmedValue: 'Role' },
            recipientRoleId: { value: 'roleId', trimmedValue: 'roleId' },
          },
        },
        currentPolicy: {
          constraints: [],
          notifications: {
            roleNotifications: [],
            userNotifications: [],
            webhookNotifications: [],
          },
          policyNotificationsOverrideAllowed: true,
          policyNotificationsOverrides: {
            orgId: {
              roleNotifications: [],
              userNotifications: [],
              webhookNotifications: [],
            },
          },
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/addNotificationRecipient',
        payload: 'orgId',
      });

      expect(currentPolicy.notifications).toEqual(state.currentPolicy.notifications);
      const expected = clone(state.currentPolicy.policyNotificationsOverrides);
      expected.orgId.roleNotifications = [{ roleId: 'roleId', stageIds: [] }];
      expect(currentPolicy.policyNotificationsOverrides).toEqual(expected);
    });

    it('adds webhook notification recipient', () => {
      const state = Object.freeze({
        notificationsEditor: {
          formState: {
            recipientType: { value: 'Webhook', trimmedValue: 'Webhook' },
            recipientWebhookId: { value: 'webhookId', trimmedValue: 'webhookId' },
          },
        },
        currentPolicy: {
          constraints: [],
          notifications: {
            roleNotifications: [],
            userNotifications: [],
            webhookNotifications: [],
          },
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/addNotificationRecipient',
      });

      expect(currentPolicy.notifications).toEqual({
        roleNotifications: [],
        userNotifications: [],
        webhookNotifications: [{ webhookId: 'webhookId', stageIds: [] }],
      });
    });
  });

  it('adds overridden webhook notification recipient', () => {
    const state = Object.freeze({
      isInherited: true,
      notificationsEditor: {
        formState: {
          recipientType: { value: 'Webhook', trimmedValue: 'Webhook' },
          recipientWebhookId: { value: 'webhookId', trimmedValue: 'webhookId' },
        },
      },
      currentPolicy: {
        constraints: [],
        notifications: {
          roleNotifications: [],
          userNotifications: [],
          webhookNotifications: [],
        },
        policyNotificationsOverrideAllowed: true,
        policyNotificationsOverrides: {
          orgId: {
            roleNotifications: [],
            userNotifications: [],
            webhookNotifications: [],
          },
        },
      },
    });

    const { currentPolicy } = reducer(state, {
      type: 'policy/addNotificationRecipient',
      payload: 'orgId',
    });

    expect(currentPolicy.notifications).toEqual(state.currentPolicy.notifications);
    const expected = clone(state.currentPolicy.policyNotificationsOverrides);
    expected.orgId.webhookNotifications = [{ webhookId: 'webhookId', stageIds: [] }];
    expect(currentPolicy.policyNotificationsOverrides).toEqual(expected);
  });

  describe('policy/removeNotificationRecipient', () => {
    it('removes user notification recipient', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [],
          notifications: {
            roleNotifications: [{ roleId: 'roleId', stageIds: [] }],
            userNotifications: [{ emailAddress: 'email@email.com', stageIds: [] }],
            webhookNotifications: [{ webhookId: 'webhookId', stageIds: [] }],
            jiraNotifications: [{ projectKey: 1, issueTypeId: 1, stageIds: [] }],
          },
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/removeNotificationRecipient',
        payload: { recipient: { emailAddress: 'email@email.com', stageIds: [] } },
      });

      expect(currentPolicy.notifications).toEqual({
        roleNotifications: [{ roleId: 'roleId', stageIds: [] }],
        userNotifications: [],
        webhookNotifications: [{ webhookId: 'webhookId', stageIds: [] }],
        jiraNotifications: [{ projectKey: 1, issueTypeId: 1, stageIds: [] }],
      });
    });

    it('removes overridden user notification recipient', () => {
      const state = Object.freeze({
        isInherited: true,
        currentPolicy: {
          constraints: [],
          notifications: {
            roleNotifications: [{ roleId: 'roleId', stageIds: [] }],
            userNotifications: [{ emailAddress: 'email@email.com', stageIds: [] }],
            webhookNotifications: [{ webhookId: 'webhookId', stageIds: [] }],
            jiraNotifications: [{ projectKey: 1, issueTypeId: 1, stageIds: [] }],
          },
          policyNotificationsOverrideAllowed: true,
          policyNotificationsOverrides: {
            orgId: {
              roleNotifications: [{ roleId: 'roleId2', stageIds: [] }],
              userNotifications: [{ emailAddress: 'email2@email.com', stageIds: [] }],
              webhookNotifications: [{ webhookId: 'webhookId2', stageIds: [] }],
              jiraNotifications: [{ projectKey: 2, issueTypeId: 2, stageIds: [] }],
            },
          },
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/removeNotificationRecipient',
        payload: { recipient: { emailAddress: 'email2@email.com', stageIds: [] }, ownerId: 'orgId' },
      });

      expect(currentPolicy.notifications).toEqual(state.currentPolicy.notifications);
      const expected = clone(state.currentPolicy.policyNotificationsOverrides);
      expected.orgId.userNotifications = [];
      expect(currentPolicy.policyNotificationsOverrides).toEqual(expected);
    });

    it('removes role notification recipient', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [],
          notifications: {
            roleNotifications: [{ roleId: 'roleId', stageIds: [] }],
            userNotifications: [{ emailAddress: 'email@email.com', stageIds: [] }],
            webhookNotifications: [{ webhookId: 'webhookId', stageIds: [] }],
            jiraNotifications: [{ projectKey: 1, issueTypeId: 1, stageIds: [] }],
          },
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/removeNotificationRecipient',
        payload: { recipient: { roleId: 'roleId', stageIds: [] } },
      });

      expect(currentPolicy.notifications).toEqual({
        roleNotifications: [],
        userNotifications: [{ emailAddress: 'email@email.com', stageIds: [] }],
        webhookNotifications: [{ webhookId: 'webhookId', stageIds: [] }],
        jiraNotifications: [{ projectKey: 1, issueTypeId: 1, stageIds: [] }],
      });
    });

    it('removes overridden role notification recipient', () => {
      const state = Object.freeze({
        isInherited: true,
        currentPolicy: {
          constraints: [],
          notifications: {
            roleNotifications: [{ roleId: 'roleId', stageIds: [] }],
            userNotifications: [{ emailAddress: 'email@email.com', stageIds: [] }],
            webhookNotifications: [{ webhookId: 'webhookId', stageIds: [] }],
            jiraNotifications: [{ projectKey: 1, issueTypeId: 1, stageIds: [] }],
          },
          policyNotificationsOverrideAllowed: true,
          policyNotificationsOverrides: {
            orgId: {
              roleNotifications: [{ roleId: 'roleId2', stageIds: [] }],
              userNotifications: [{ emailAddress: 'email2@email.com', stageIds: [] }],
              webhookNotifications: [{ webhookId: 'webhookId2', stageIds: [] }],
              jiraNotifications: [{ projectKey: 2, issueTypeId: 2, stageIds: [] }],
            },
          },
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/removeNotificationRecipient',
        payload: { recipient: { roleId: 'roleId2', stageIds: [] }, ownerId: 'orgId' },
      });

      expect(currentPolicy.notifications).toEqual(state.currentPolicy.notifications);
      const expected = clone(state.currentPolicy.policyNotificationsOverrides);
      expected.orgId.roleNotifications = [];
      expect(currentPolicy.policyNotificationsOverrides).toEqual(expected);
    });

    it('removes webhook notification recipient', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [],
          notifications: {
            roleNotifications: [{ roleId: 'roleId', stageIds: [] }],
            userNotifications: [{ emailAddress: 'email@email.com', stageIds: [] }],
            webhookNotifications: [{ webhookId: 'webhookId', stageIds: [] }],
            jiraNotifications: [{ projectKey: 1, issueTypeId: 1, stageIds: [] }],
          },
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/removeNotificationRecipient',
        payload: { recipient: { webhookId: 'webhookId', stageIds: [] } },
      });

      expect(currentPolicy.notifications).toEqual({
        roleNotifications: [{ roleId: 'roleId', stageIds: [] }],
        userNotifications: [{ emailAddress: 'email@email.com', stageIds: [] }],
        webhookNotifications: [],
        jiraNotifications: [{ projectKey: 1, issueTypeId: 1, stageIds: [] }],
      });
    });

    it('removes overridden webhook notification recipient', () => {
      const state = Object.freeze({
        isInherited: true,
        currentPolicy: {
          constraints: [],
          notifications: {
            roleNotifications: [{ roleId: 'roleId', stageIds: [] }],
            userNotifications: [{ emailAddress: 'email@email.com', stageIds: [] }],
            webhookNotifications: [{ webhookId: 'webhookId', stageIds: [] }],
            jiraNotifications: [{ projectKey: 1, issueTypeId: 1, stageIds: [] }],
          },
          policyNotificationsOverrideAllowed: true,
          policyNotificationsOverrides: {
            orgId: {
              roleNotifications: [{ roleId: 'roleId2', stageIds: [] }],
              userNotifications: [{ emailAddress: 'email2@email.com', stageIds: [] }],
              webhookNotifications: [{ webhookId: 'webhookId2', stageIds: [] }],
              jiraNotifications: [{ projectKey: 2, issueTypeId: 2, stageIds: [] }],
            },
          },
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/removeNotificationRecipient',
        payload: { recipient: { webhookId: 'webhookId2', stageIds: [] }, ownerId: 'orgId' },
      });

      expect(currentPolicy.notifications).toEqual(state.currentPolicy.notifications);
      const expected = clone(state.currentPolicy.policyNotificationsOverrides);
      expected.orgId.webhookNotifications = [];
      expect(currentPolicy.policyNotificationsOverrides).toEqual(expected);
    });

    it('removes jira notification recipient', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [],
          notifications: {
            roleNotifications: [{ roleId: 'roleId', stageIds: [] }],
            userNotifications: [{ emailAddress: 'email@email.com', stageIds: [] }],
            webhookNotifications: [{ webhookId: 'webhookId', stageIds: [] }],
            jiraNotifications: [{ projectKey: 1, issueTypeId: 1, stageIds: [] }],
          },
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/removeNotificationRecipient',
        payload: { recipient: { projectKey: 1, issueTypeId: 1, stageIds: [] } },
      });

      expect(currentPolicy.notifications).toEqual({
        roleNotifications: [{ roleId: 'roleId', stageIds: [] }],
        userNotifications: [{ emailAddress: 'email@email.com', stageIds: [] }],
        webhookNotifications: [{ webhookId: 'webhookId', stageIds: [] }],
        jiraNotifications: [],
      });
    });
  });

  it('removes overridden jira notification recipient', () => {
    const state = Object.freeze({
      isInherited: true,
      currentPolicy: {
        constraints: [],
        notifications: {
          roleNotifications: [{ roleId: 'roleId', stageIds: [] }],
          userNotifications: [{ emailAddress: 'email@email.com', stageIds: [] }],
          webhookNotifications: [{ webhookId: 'webhookId', stageIds: [] }],
          jiraNotifications: [{ projectKey: 1, issueTypeId: 1, stageIds: [] }],
        },
        policyNotificationsOverrideAllowed: true,
        policyNotificationsOverrides: {
          orgId: {
            roleNotifications: [{ roleId: 'roleId2', stageIds: [] }],
            userNotifications: [{ emailAddress: 'email2@email.com', stageIds: [] }],
            webhookNotifications: [{ webhookId: 'webhookId2', stageIds: [] }],
            jiraNotifications: [{ projectKey: 2, issueTypeId: 2, stageIds: [] }],
          },
        },
      },
    });

    const { currentPolicy } = reducer(state, {
      type: 'policy/removeNotificationRecipient',
      payload: { recipient: { projectKey: 2, issueTypeId: 2, stageIds: [] }, ownerId: 'orgId' },
    });

    expect(currentPolicy.notifications).toEqual(state.currentPolicy.notifications);
    const expected = clone(state.currentPolicy.policyNotificationsOverrides);
    expected.orgId.jiraNotifications = [];
    expect(currentPolicy.policyNotificationsOverrides).toEqual(expected);
  });

  describe('policy/toggleNotificationRecipientStage', () => {
    it('toogles off notification recipient stage', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [],
          notifications: {
            roleNotifications: [{ roleId: 'roleId', stageIds: [] }],
            userNotifications: [{ emailAddress: 'email@email.com', stageIds: ['proxy'] }],
            webhookNotifications: [{ webhookId: 'webhookId', stageIds: [] }],
          },
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/toggleNotificationRecipientStage',
        payload: { stageId: 'proxy', recipient: { emailAddress: 'email@email.com', stageIds: ['proxy'] } },
      });

      expect(currentPolicy.notifications).toEqual({
        roleNotifications: [{ roleId: 'roleId', stageIds: [] }],
        userNotifications: [{ emailAddress: 'email@email.com', stageIds: [] }],
        webhookNotifications: [{ webhookId: 'webhookId', stageIds: [] }],
      });
    });

    it('toogles off overridden notification recipient stage', () => {
      const state = Object.freeze({
        isInherited: true,
        currentPolicy: {
          constraints: [],
          notifications: {
            roleNotifications: [{ roleId: 'roleId', stageIds: [] }],
            userNotifications: [{ emailAddress: 'email@email.com', stageIds: ['proxy'] }],
            webhookNotifications: [{ webhookId: 'webhookId', stageIds: [] }],
          },
          policyNotificationsOverrideAllowed: true,
          policyNotificationsOverrides: {
            orgId: {
              roleNotifications: [{ roleId: 'roleId2', stageIds: [] }],
              userNotifications: [{ emailAddress: 'email2@email.com', stageIds: ['proxy'] }],
              webhookNotifications: [{ webhookId: 'webhookId2', stageIds: [] }],
            },
          },
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/toggleNotificationRecipientStage',
        payload: {
          stageId: 'proxy',
          recipient: { emailAddress: 'email2@email.com', stageIds: ['proxy'] },
          ownerId: 'orgId',
        },
      });

      expect(currentPolicy.notifications).toEqual(state.currentPolicy.notifications);
      const expected = clone(state.currentPolicy.policyNotificationsOverrides);
      expected.orgId.userNotifications = [{ emailAddress: 'email2@email.com', stageIds: [] }];
      expect(currentPolicy.policyNotificationsOverrides).toEqual(expected);
    });

    it('toogles on notification recipient stage', () => {
      const state = Object.freeze({
        currentPolicy: {
          constraints: [],
          notifications: {
            roleNotifications: [{ roleId: 'roleId', stageIds: [] }],
            userNotifications: [{ emailAddress: 'email@email.com', stageIds: [] }],
            webhookNotifications: [{ webhookId: 'webhookId', stageIds: [] }],
          },
        },
      });

      const { currentPolicy } = reducer(state, {
        type: 'policy/toggleNotificationRecipientStage',
        payload: { stageId: 'proxy', recipient: { emailAddress: 'email@email.com', stageIds: [] } },
      });

      expect(currentPolicy.notifications).toEqual({
        roleNotifications: [{ roleId: 'roleId', stageIds: [] }],
        userNotifications: [{ emailAddress: 'email@email.com', stageIds: ['proxy'] }],
        webhookNotifications: [{ webhookId: 'webhookId', stageIds: [] }],
      });
    });
  });

  it('toogles on overridden notification recipient stage', () => {
    const state = Object.freeze({
      isInherited: true,
      currentPolicy: {
        constraints: [],
        notifications: {
          roleNotifications: [{ roleId: 'roleId', stageIds: [] }],
          userNotifications: [{ emailAddress: 'email@email.com', stageIds: [] }],
          webhookNotifications: [{ webhookId: 'webhookId', stageIds: [] }],
        },
        policyNotificationsOverrideAllowed: true,
        policyNotificationsOverrides: {
          orgId: {
            roleNotifications: [{ roleId: 'roleId2', stageIds: [] }],
            userNotifications: [{ emailAddress: 'email2@email.com', stageIds: [] }],
            webhookNotifications: [{ webhookId: 'webhookId2', stageIds: [] }],
          },
        },
      },
    });

    const { currentPolicy } = reducer(state, {
      type: 'policy/toggleNotificationRecipientStage',
      payload: {
        stageId: 'proxy',
        recipient: { emailAddress: 'email2@email.com', stageIds: [] },
        ownerId: 'orgId',
      },
    });

    expect(currentPolicy.notifications).toEqual(state.currentPolicy.notifications);
    const expected = clone(state.currentPolicy.policyNotificationsOverrides);
    expected.orgId.userNotifications = [{ emailAddress: 'email2@email.com', stageIds: ['proxy'] }];
    expect(currentPolicy.policyNotificationsOverrides).toEqual(expected);
  });

  describe('policy/setNotificationsEditorFormFieldValue', () => {
    ['recipientType', 'recipientEmail', 'recipientRoleId', 'recipientWebhookId'].forEach((field) => {
      it(`sets ${field} value`, () => {
        const currentFormState = {
          recipientType: { value: 'prev' },
          recipientRoleId: { value: 'prev' },
          recipientWebhookId: { value: 'prev' },
        };
        const state = Object.freeze({
          notificationsEditor: {
            formState: currentFormState,
          },
        });

        const {
          notificationsEditor: { formState },
        } = reducer(state, {
          type: 'policy/setNotificationsEditorFormFieldValue',
          payload: { field, value: 'value' },
        });

        expect(formState[field].value).toEqual('value');
        if (field === 'recipientType') {
          expect(formState).toEqual(expect.objectContaining(omit([field], initialState.notificationsEditor.formState)));
        } else {
          expect(formState).toEqual(expect.objectContaining(omit([field], currentFormState)));
        }
      });
    });

    describe('recipientEmail', () => {
      const field = 'recipientEmail';
      it(`sets value`, () => {
        const currentState = {
          recipientEmail: { value: 'prev', trimmedValue: 'prev' },
          recipientWebhookId: { value: 'prev', trimmedValue: 'prev' },
        };
        const state = Object.freeze({ notificationsEditor: { formState: currentState } });

        const {
          notificationsEditor: { formState },
        } = reducer(state, {
          type: 'policy/setNotificationsEditorFormFieldValue',
          payload: { field, value: ' value ' },
        });

        expect(formState[field].value).toEqual(' value ');
        expect(formState).toEqual(expect.objectContaining(omit([field], currentState)));
      });

      const ERROR_MESSAGE = {
        FORMAT: ['Use valid format: abc@xyz.com'],
        DUPLICATE: ['Email already exists'],
      };

      [
        { value: ' ', validationErrors: ERROR_MESSAGE.FORMAT },
        { value: 'existing@email.com', validationErrors: ERROR_MESSAGE.DUPLICATE },
        { value: ' existing@email.com', validationErrors: ERROR_MESSAGE.DUPLICATE },
        { value: 'existing@email.com ', validationErrors: ERROR_MESSAGE.DUPLICATE },
        { value: 'existing @email.com ', validationErrors: ERROR_MESSAGE.FORMAT },
      ].forEach(({ value, validationErrors }) => {
        it(`sets validation error`, () => {
          const currentState = {
            recipientEmail: { value: 'prev', trimmedValue: 'prev' },
            recipientWebhookId: { value: 'prev', trimmedValue: 'prev' },
          };
          const state = Object.freeze({
            notificationsEditor: { formState: currentState },
            currentPolicy: { notifications: { userNotifications: [{ emailAddress: 'existing@email.com' }] } },
          });

          const { notificationsEditor } = reducer(state, {
            type: 'policy/setNotificationsEditorFormFieldValue',
            payload: { field, value },
          });

          expect(notificationsEditor.formState[field].validationErrors).toEqual(validationErrors);
          expect(notificationsEditor.formState).toEqual(expect.objectContaining(omit([field], currentState)));
        });
      });
    });
  });

  describe('policy/loadNotificationsEditor/pending action', () => {
    it('sets loading to true and loadError to null', () => {
      const state = Object.freeze({
        notificationsEditor: {
          loading: false,
          loadError: 'error',
          formState: {
            recipientType: { value: 'Email' },
            recipientEmail: { value: 'email@email.com' },
          },
        },
      });

      const {
        notificationsEditor: { loading, loadError, formState },
      } = reducer(state, {
        type: 'policy/loadNotificationsEditor/pending',
      });

      expect(loading).toBe(true);
      expect(loadError).toBe(null);
      expect(formState).toBe(initialState.notificationsEditor.formState);
    });
  });

  describe('policy/loadNotificationsEditor/rejected action', () => {
    it('sets loading flag to false and sets loadError to payload', () => {
      const state = Object.freeze({
        notificationsEditor: {
          loading: true,
          loadError: null,
        },
      });

      const {
        notificationsEditor: { loading, loadError },
      } = reducer(state, {
        type: 'policy/loadNotificationsEditor/rejected',
        payload: 'error',
      });

      expect(loading).toBe(false);
      expect(loadError).toBe('error');
    });
  });

  describe('policy/loadNotificationsEditor/fulfilled action', () => {
    it('sets editor data', () => {
      const state = Object.freeze({
        notificationsEditor: {
          loading: true,
          loadError: 'error',
        },
      });
      const isJiraEnabled = true;
      const jiraProjects = ['project'];
      const notificationWebhooks = ['webhook'];
      const roles = ['role'];

      const { notificationsEditor } = reducer(state, {
        type: 'policy/loadNotificationsEditor/fulfilled',
        payload: {
          isJiraEnabled,
          projects: jiraProjects,
          notificationWebhooks,
          membersByRole: roles,
        },
      });

      expect(notificationsEditor.loading).toBe(false);
      expect(notificationsEditor.loadError).toBe(null);
      expect(notificationsEditor.isJiraEnabled).toBe(isJiraEnabled);
      expect(notificationsEditor.jiraProjects).toEqual(jiraProjects);
      expect(notificationsEditor.notificationWebhooks).toEqual(notificationWebhooks);
      expect(notificationsEditor.roles).toEqual(roles);
    });
  });

  describe('policy/loadPolicyTile/pending', () => {
    it('resets loadError and sets loading to true for policy tile', () => {
      const state = Object.freeze({ policyTile: { loadError: 'error', loading: false } });

      const { policyTile } = reducer(state, {
        type: 'policy/loadPolicyTile/pending',
      });

      expect(policyTile.loadError).toBeNull();
      expect(policyTile.loading).toBe(true);
    });
  });

  describe('policy/loadPolicyTile/fulfilled', () => {
    it('resets loading flag, sets initial sorting config and policies sorted by threat', () => {
      const state = Object.freeze({
        policyTile: {
          loading: true,
          collapsibleSorting: {},
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
      expect(policyTile.collapsibleSorting).toEqual({ key: 'threatLevel', dir: 'desc' });
      expect(policyTile.policiesByOwner[0].policies[0].threatLevel).toBe(10);
      expect(policyTile.policiesByOwner[0].policies[0].name).toBe('not allowed to override');
      expect(policyTile.policiesByOwner[0].policies[1].threatLevel).toBe(5);
      expect(policyTile.policiesByOwner[0].policies[1].name).toBe('fresh allowed to override');
      expect(policyTile.policiesByOwner[0].policies[2].threatLevel).toBe(5);
      expect(policyTile.policiesByOwner[0].policies[2].name).toBe('allowed to override');
    });
  });

  describe('policy/changeCollapsibleSortField', () => {
    it('sets sorting config and policies sorted by chosen payload key', () => {
      const state = Object.freeze({
        policyTile: {
          collapsibleSorting: {
            key: 'threatLevel',
            dir: 'desc',
          },
          policiesByOwner: [
            {
              inherited: false,
              ownerId: 'CHILD_ORG_1_1',
              ownerName: 'Child org 1.1',
              ownerType: 'organizations',
              policies: [
                {
                  hasLocalActionsOverrides: undefined,
                  id: '9bd6ecd914f04e41bc983a9421f31b12',
                  name: 'not allowed to override',
                  threatLevel: 10,
                  ownerId: 'CHILD_ORG_1_1',
                  actions: {},
                },
                {
                  hasLocalActionsOverrides: undefined,
                  id: 'e2cb70070ce540f7af0af8478b4d8bd9',
                  name: 'fresh allowed to override',
                  threatLevel: 5,
                  ownerId: 'CHILD_ORG_1_1',
                  actions: {},
                },
                {
                  hasLocalActionsOverrides: undefined,
                  id: '787822577d384a28b59c9d06ff6d37e2',
                  name: 'allowed to override',
                  threatLevel: 5,
                  ownerId: 'CHILD_ORG_1_1',
                  actions: {},
                },
              ],
            },
            {
              inherited: true,
              ownerId: 'CHILD_ORG_1',
              ownerName: 'Child Org 1',
              ownerType: 'organizations',
              policies: [
                {
                  hasLocalActionsOverrides: undefined,
                  id: '9bd6ecd914f04e41bc983a9421f31b12',
                  name: 'not allowed to override',
                  threatLevel: 5,
                  ownerId: 'CHILD_ORG_1',
                  actions: {},
                },
                {
                  hasLocalActionsOverrides: undefined,
                  id: 'e2cb70070ce540f7af0af8478b4d8bd9',
                  name: 'fresh allowed to override',
                  threatLevel: 7,
                  ownerId: 'CHILD_ORG_1',
                  actions: {},
                },
                {
                  hasLocalActionsOverrides: undefined,
                  id: '787822577d384a28b59c9d06ff6d37e2',
                  name: 'allowed to override',
                  threatLevel: 6,
                  ownerId: 'CHILD_ORG_1',
                  actions: {},
                },
              ],
            },
            {
              inherited: true,
              ownerId: 'ROOT_ORGANIZATION_ID',
              ownerName: 'Root Organization',
              ownerType: 'organizations',
              policies: [
                {
                  hasLocalActionsOverrides: undefined,
                  id: '9bd6ecd914f04e41bc983a9421f31b12',
                  name: 'not allowed to override',
                  threatLevel: 3,
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  actions: {},
                },
                {
                  hasLocalActionsOverrides: undefined,
                  id: 'e2cb70070ce540f7af0af8478b4d8bd9',
                  name: 'fresh allowed to override',
                  threatLevel: 9,
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  actions: {},
                },
                {
                  hasLocalActionsOverrides: undefined,
                  id: '787822577d384a28b59c9d06ff6d37e2',
                  name: 'allowed to override',
                  threatLevel: 6,
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  actions: {},
                },
              ],
            },
          ],
        },
      });

      const action = {
        type: 'policy/changeCollapsibleSortField',
        payload: { key: 'threatLevel', dir: 'asc' },
      };

      const { policyTile } = reducer(state, action);

      expect(policyTile.collapsibleSorting).toEqual({ key: 'threatLevel', dir: 'asc' });
      expect(policyTile.policiesByOwner[0].policies[0].threatLevel).toBe(5);
      expect(policyTile.policiesByOwner[0].policies[0].name).toBe('allowed to override');
      expect(policyTile.policiesByOwner[0].policies[1].threatLevel).toBe(5);
      expect(policyTile.policiesByOwner[0].policies[1].name).toBe('fresh allowed to override');
      expect(policyTile.policiesByOwner[0].policies[2].threatLevel).toBe(10);
      expect(policyTile.policiesByOwner[0].policies[2].name).toBe('not allowed to override');
      expect(policyTile.policiesByOwner[1].policies[0].threatLevel).toBe(5);
      expect(policyTile.policiesByOwner[1].policies[0].name).toBe('not allowed to override');
      expect(policyTile.policiesByOwner[1].policies[1].threatLevel).toBe(6);
      expect(policyTile.policiesByOwner[1].policies[1].name).toBe('allowed to override');
      expect(policyTile.policiesByOwner[1].policies[2].threatLevel).toBe(7);
      expect(policyTile.policiesByOwner[1].policies[2].name).toBe('fresh allowed to override');
      expect(policyTile.policiesByOwner[2].policies[0].threatLevel).toBe(3);
      expect(policyTile.policiesByOwner[2].policies[0].name).toBe('not allowed to override');
      expect(policyTile.policiesByOwner[2].policies[1].threatLevel).toBe(6);
      expect(policyTile.policiesByOwner[2].policies[1].name).toBe('allowed to override');
      expect(policyTile.policiesByOwner[2].policies[2].threatLevel).toBe(9);
      expect(policyTile.policiesByOwner[2].policies[2].name).toBe('fresh allowed to override');
    });

    it('returns policies in preserved order if values are equal by the key', () => {
      const state = Object.freeze({
        policyTile: {
          sorting: { key: 'threatLevel', dir: 'desc' },
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
        type: 'policy/changeCollapsibleSortField',
        payload: { key: 'build', dir: 'asc' },
      };

      const { policyTile } = reducer(state, action);

      expect(policyTile.collapsibleSorting).toEqual({ key: 'build', dir: 'asc' });
      expect(policyTile.policiesByOwner[0].policies[0].threatLevel).toBe(10);
      expect(policyTile.policiesByOwner[0].policies[0].name).toBe('not allowed to override');
      expect(policyTile.policiesByOwner[0].policies[1].threatLevel).toBe(5);
      expect(policyTile.policiesByOwner[0].policies[1].name).toBe('fresh allowed to override');
      expect(policyTile.policiesByOwner[0].policies[2].threatLevel).toBe(7);
      expect(policyTile.policiesByOwner[0].policies[2].name).toBe('allowed to override');
    });

    it('returns policies in preserved order if values are equal by the key', () => {
      const state = Object.freeze({
        policyTile: {
          sorting: { key: 'threatLevel', dir: 'desc' },
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
        type: 'policy/changeCollapsibleSortField',
        payload: { key: 'build', dir: 'asc' },
      };

      const { policyTile } = reducer(state, action);

      expect(policyTile.collapsibleSorting).toEqual({ key: 'build', dir: 'asc' });
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
      expect(policyTile.loading).toBe(false);
    });
  });

  describe('policy/toggleShowActionsOverridesConfirmationModal', () => {
    it('toggles showActionsOverridesConfirmationModal from false to true', () => {
      const state = Object.freeze({
        showActionsOverridesConfirmationModal: false,
      });

      const { showActionsOverridesConfirmationModal } = reducer(state, {
        type: 'policy/toggleShowActionsOverridesConfirmationModal',
      });

      expect(showActionsOverridesConfirmationModal).toBe(true);
    });

    it('toggles showActionsOverridesConfirmationModal from true to false', () => {
      const state = Object.freeze({
        showActionsOverridesConfirmationModal: true,
      });

      const { showActionsOverridesConfirmationModal } = reducer(state, {
        type: 'policy/toggleShowActionsOverridesConfirmationModal',
      });

      expect(showActionsOverridesConfirmationModal).toBe(false);
    });
  });

  describe('policy/toggleShowNotificationsOverridesConfirmationModal', () => {
    it('toggles showNotificationsOverridesConfirmationModal from false to true', () => {
      const state = Object.freeze({
        showNotificationsOverridesConfirmationModal: false,
      });

      const { showNotificationsOverridesConfirmationModal } = reducer(state, {
        type: 'policy/toggleShowNotificationsOverridesConfirmationModal',
      });

      expect(showNotificationsOverridesConfirmationModal).toBe(true);
    });

    it('toggles showNotificationsOverridesConfirmationModal from true to false', () => {
      const state = Object.freeze({
        showNotificationsOverridesConfirmationModal: true,
      });

      const { showNotificationsOverridesConfirmationModal } = reducer(state, {
        type: 'policy/toggleShowNotificationsOverridesConfirmationModal',
      });

      expect(showNotificationsOverridesConfirmationModal).toBe(false);
    });
  });
});
