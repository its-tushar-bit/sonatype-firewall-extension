/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import 'TestRoot/SpecUtil';

import { actions, initialState } from 'MainRoot/OrgsAndPolicies/policySlice';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import * as selectors from 'MainRoot/OrgsAndPolicies/policySelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import PolicyResourceMockData from 'TestRoot/OrgsAndPolicies/mock.data/policy.resource.mock.data';
import TagResourceMockData from 'TestRoot/OrgsAndPolicies/mock.data/tag.resource.mock.data';
import {
  getApplicableCategoriesUrl,
  getApplicablePolicies,
  getPolicyOverridesUrl,
  getPolicyCRUDUrl,
  getPolicyTagUrl,
  getPolicyUrl,
  getOrganizationUrl,
  getPolicyNotificationsUrl,
} from 'MainRoot/util/CLMLocation';
import { omit, prop } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { urlsByPurpose } from 'MainRoot/OrgsAndPolicies/stagesSlice';
import { getPermissionContextTestUrl } from '../../../main/frontend/util/CLMLocation';

describe('policySlice actions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, mockOwnerId, mockOwnerType, mockOwnerName;

  beforeEach(function () {
    store = SpecUtil.mockReduxStore({
      router: {
        currentParams: {
          organizationId: 'e270271429f747ef9bebf4ca88f5e6c0',
        },
        currentState: {
          name: 'management.view.organization',
        },
      },
      orgsAndPolicies: {
        organizations: { organizations: [] },
        root: {
          selectedOwner: {
            id: undefined,
            type: 'application',
          },
        },
      },
    });
    mockOwnerId = 'ownerId';
    mockOwnerType = 'ownerType';
    mockOwnerName = 'ownerName';
    jest.spyOn(orgsAndPoliciesSelectors, 'selectOwnerProperties').mockReturnValue({
      ownerId: mockOwnerId,
      ownerType: mockOwnerType,
    });
    jest.spyOn(routerSelectors, 'selectIsRepositories').mockReturnValue(false);
  });

  describe('loadCategoriesForPolicy', () => {
    let selectRouterCurrentParamsSpy;
    const getApplicationCategoriesUrlResponse = TagResourceMockData.getApplicationCategoriesUrl(
      mockOwnerType,
      mockOwnerId,
      mockOwnerName
    );
    const mockPolicyId = 'policy.id';
    beforeEach(() => {
      selectRouterCurrentParamsSpy = jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
        policyId: mockPolicyId,
      });
    });

    it('loads policy tags when editing an existing policy', (done) => {
      const getPolicyTagUrlResponse = TagResourceMockData.getPolicyTagUrl();
      mockAxiosCalls({
        get: {
          [getApplicableCategoriesUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({
            data: getApplicationCategoriesUrlResponse,
          }),
          [getPolicyTagUrl(mockPolicyId, mockOwnerType, mockOwnerId)]: Promise.resolve({
            data: getPolicyTagUrlResponse,
          }),
        },
      });

      const mockCurrentPolicy = { id: mockOwnerId };
      store.dispatch(actions.loadCategoriesForPolicy(mockCurrentPolicy)).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(2);

        const actions = store.getActions();

        expect(actions.length).toBe(4);
        expect(actions).toHaveActionTypesInOrder([
          'policy/loadCategoriesForPolicy/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/fulfilled',
          'policy/loadCategoriesForPolicy/fulfilled',
        ]);

        expect(actions[3].payload).toEqual({
          hasPolicyCategories: true,
          categories: [
            {
              color: 'black',
              description: 'Description 1',
              id: 'appCategoryId_1',
              name: 'Category 1',
              organizationId: 'orgownerid',
              isApplied: true,
            },
            {
              color: 'black',
              description: 'Description 2',
              id: 'appCategoryId_2',
              name: 'Category 2',
              organizationId: 'orgownerid',
              isApplied: true,
            },
            {
              color: 'red',
              description: 'Description 3',
              id: 'appCategoryId_3',
              name: 'Category 3',
              organizationId: 'rootorgownerid',
              isApplied: false,
            },
          ],
        });

        done();
      });
    });

    it('loads policy tags when for new policy', (done) => {
      selectRouterCurrentParamsSpy.mockReturnValue({});
      mockAxiosCalls({
        get: {
          [getApplicableCategoriesUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({
            data: getApplicationCategoriesUrlResponse,
          }),
        },
      });

      const mockCurrentPolicy = { id: mockOwnerId };
      store.dispatch(actions.loadCategoriesForPolicy(mockCurrentPolicy)).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(4);
        expect(actions).toHaveActionTypesInOrder([
          'policy/loadCategoriesForPolicy/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/fulfilled',
          'policy/loadCategoriesForPolicy/fulfilled',
        ]);

        expect(actions[3].payload).toEqual({
          hasPolicyCategories: false,
          categories: [
            {
              color: 'black',
              description: 'Description 1',
              id: 'appCategoryId_1',
              name: 'Category 1',
              organizationId: 'orgownerid',
              isApplied: false,
            },
            {
              color: 'black',
              description: 'Description 2',
              id: 'appCategoryId_2',
              name: 'Category 2',
              organizationId: 'orgownerid',
              isApplied: false,
            },
            {
              color: 'red',
              description: 'Description 3',
              id: 'appCategoryId_3',
              name: 'Category 3',
              organizationId: 'rootorgownerid',
              isApplied: false,
            },
          ],
        });

        done();
      });
    });

    it('dispatches rejected action if loadCategoriesForPolicy request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getPolicyTagUrl(mockPolicyId, mockOwnerType, mockOwnerId)]: () => Promise.reject('something went wrong'),
        },
      });

      store.dispatch(actions.loadCategoriesForPolicy()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(2);

        const actions = store.getActions();

        expect(actions.length).toBe(4);
        expect(actions).toHaveActionTypesInOrder([
          'policy/loadCategoriesForPolicy/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/rejected',
          'policy/loadCategoriesForPolicy/rejected',
        ]);

        done();
      });
    });
  });

  describe('loadPolicyEditor', () => {
    let selectRouterCurrentParamsSpy, selectIsOrganizationSpy;
    const mockPolicyId = '4d6b4ac75ea148b2aa6ca36e6899cc78';
    const getApplicablePoliciesResponse = PolicyResourceMockData.getApplicablePolicies(
      mockOwnerType,
      mockOwnerId,
      mockOwnerName
    );

    beforeEach(() => {
      selectRouterCurrentParamsSpy = jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
        policyId: mockPolicyId,
      });
      jest.spyOn(routerSelectors, 'selectIsRootOrganization').mockReturnValue(false);
      selectIsOrganizationSpy = jest.spyOn(routerSelectors, 'selectIsOrganization').mockReturnValue(false);
    });

    it('loads data for a new policy', async () => {
      selectRouterCurrentParamsSpy.mockReturnValue({});
      mockAxiosCalls({
        get: {
          [urlsByPurpose.action]: Promise.resolve({ data: [] }),
          [getApplicablePolicies(mockOwnerType, mockOwnerId)]: Promise.resolve({
            data: getApplicablePoliciesResponse,
          }),
          [getOrganizationUrl('e270271429f747ef9bebf4ca88f5e6c0')]: Promise.resolve({
            data: { id: 'e270271429f747ef9bebf4ca88f5e6c0', name: 'broadcast' },
          }),
        },
        put: { [getPermissionContextTestUrl('application', 'global')]: Promise.resolve({ data: ['WRITE'] }) },
      });

      await store.dispatch(actions.loadPolicyEditor());
      await Promise.resolve();

      expect(axios.get).toHaveBeenCalledTimes(5);

      const dispatchedActions = store.getActions();

      expect(dispatchedActions.length).toBe(13);
      expect(dispatchedActions).toHaveActionTypesInOrder([
        'policy/loadPolicyEditor/pending',
        'orgsAndPolicies/loadSelectedOwner/pending',
        'orgsAndPolicies/setShowLimitedFirewallAccessAlert',
        'orgsAndPolicies/loadSelectedOwner/fulfilled',
        'policy/checkEditIqPermission/pending',
        'orgsAndPolicies/loadApplicablePoliciesByOwner/pending',
        'orgsAndPolicies/loadApplicablePoliciesByOwner/fulfilled',
        'policy/checkEditIqPermission/fulfilled',
        'constraint/loadConstraint/pending',
        'stages/loadStageTypes/pending',
        'stages/loadStageTypes/fulfilled',
        'policy/loadPolicyEditor/fulfilled',
        'constraint/loadConstraint/rejected',
      ]);

      expect(dispatchedActions[11].payload).toEqual({
        isInherited: undefined,
        originalProxyStageAction: undefined,
        siblings: [
          {
            id: '4d6b4ac75ea148b2aa6ca36e6899cc78',
            name: 'Org Policy 3',
            ownerId: 'f3cea033acf84984ae08d9250db4aa7b',
            enabled: true,
            threatLevel: 0,
            constraints: [
              {
                id: 'd4fe6780471e4543bcb0e28d0e122b69',
                name: 'Unpopular',
                enabled: true,
                operator: 'OR',
                conditions: [{ conditionTypeId: 'RelativePopularity', operator: '<', value: '10' }],
              },
            ],
            actions: {
              develop: [{ actionTypeId: 'warn', target: null }],
              build: [{ actionTypeId: 'fail', target: null }],
              'stage-release': [{ actionTypeId: 'fail', target: null }],
              release: [{ actionTypeId: 'warn', target: null }],
              operate: [{ actionTypeId: 'warn', target: null }],
              proxy: [{ actionTypeId: 'warn', target: null }],
            },
            monitorNotifyActions: null,
          },
        ],
        overrideActionsFlag: false,
        overrideNotificationsFlag: false,
        currentPolicy: initialState.currentPolicy,
        currentPolicyOwner: {
          id: 'f3cea033acf84984ae08d9250db4aa7b',
          name: 'Org1 Heh',
        },
        isOrgOwner: false,
        isRepositoryContainerOwner: false,
        isRepositoryManagerOwner: false,
        isRepositoryOwner: false,
        isRootOrg: false,
        policiesByOwner: getApplicablePoliciesResponse.policiesByOwner,
      });
    });

    it('loads data for a new policy that is also the org owner', (done) => {
      selectRouterCurrentParamsSpy.mockReturnValue({});
      selectIsOrganizationSpy.mockReturnValue(true);
      mockAxiosCalls({
        [urlsByPurpose.action]: Promise.resolve({ data: [] }),
        get: {
          [getApplicablePolicies(mockOwnerType, mockOwnerId)]: Promise.resolve({
            data: getApplicablePoliciesResponse,
          }),
          [getOrganizationUrl('e270271429f747ef9bebf4ca88f5e6c0')]: Promise.resolve({
            data: { id: 'e270271429f747ef9bebf4ca88f5e6c0', name: 'broadcast' },
          }),
        },
        put: { [getPermissionContextTestUrl('application', 'global')]: Promise.resolve({ data: ['WRITE'] }) },
      });

      store.dispatch(actions.loadPolicyEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(6);

        const actions = store.getActions();
        expect(actions).toHaveActionType('policy/loadCategoriesForPolicy/pending');
        expect(actions[14].payload).toEqual(expect.objectContaining({ isOrgOwner: true }));

        done();
      });
    });

    it('loads data for an existing policy', (done) => {
      mockAxiosCalls({
        get: {
          [urlsByPurpose.action]: Promise.resolve({ data: [] }),
          [getApplicablePolicies(mockOwnerType, mockOwnerId)]: Promise.resolve({
            data: getApplicablePoliciesResponse,
          }),
          [getOrganizationUrl('e270271429f747ef9bebf4ca88f5e6c0')]: Promise.resolve({
            data: { id: 'e270271429f747ef9bebf4ca88f5e6c0', name: 'broadcast' },
          }),
        },
        put: { [getPermissionContextTestUrl('application', 'global')]: Promise.resolve({ data: ['WRITE'] }) },
      });

      store.dispatch(actions.loadPolicyEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(7);

        const actions = store.getActions();

        expect(actions.length).toBe(17);
        expect(actions).toHaveActionTypesInOrder([
          'policy/loadPolicyEditor/pending',
          'orgsAndPolicies/loadSelectedOwner/pending',
          'orgsAndPolicies/setShowLimitedFirewallAccessAlert',
          'orgsAndPolicies/loadSelectedOwner/fulfilled',
          'policy/checkEditIqPermission/pending',
          'orgsAndPolicies/loadApplicablePoliciesByOwner/pending',
          'orgsAndPolicies/loadApplicablePoliciesByOwner/fulfilled',
          'policy/checkEditIqPermission/fulfilled',
          'constraint/loadConstraint/pending',
          'stages/loadStageTypes/pending',
          'policy/loadCategoriesForPolicy/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/rejected',
          'stages/loadStageTypes/fulfilled',
          'policy/loadPolicyEditor/fulfilled',
          'constraint/loadConstraint/rejected',
          'policy/loadCategoriesForPolicy/rejected',
        ]);

        expect(actions[14].payload).toEqual({
          siblings: [
            {
              id: '4d6b4ac75ea148b2aa6ca36e6899cc78',
              name: 'Org Policy 3',
              ownerId: 'f3cea033acf84984ae08d9250db4aa7b',
              enabled: true,
              threatLevel: 0,
              constraints: [
                {
                  id: 'd4fe6780471e4543bcb0e28d0e122b69',
                  name: 'Unpopular',
                  enabled: true,
                  operator: 'OR',
                  conditions: [{ conditionTypeId: 'RelativePopularity', operator: '<', value: '10' }],
                },
              ],
              actions: {
                develop: [{ actionTypeId: 'warn', target: null }],
                build: [{ actionTypeId: 'fail', target: null }],
                'stage-release': [{ actionTypeId: 'fail', target: null }],
                release: [{ actionTypeId: 'warn', target: null }],
                operate: [{ actionTypeId: 'warn', target: null }],
                proxy: [{ actionTypeId: 'warn', target: null }],
              },
              monitorNotifyActions: null,
            },
          ],
          overrideActionsFlag: false,
          overrideNotificationsFlag: false,
          currentPolicy: {
            id: '4d6b4ac75ea148b2aa6ca36e6899cc78',
            name: {
              isPristine: true,
              value: 'Org Policy 3',
              trimmedValue: 'Org Policy 3',
              validationErrors: ['Name is already in use'],
            },
            ownerId: 'f3cea033acf84984ae08d9250db4aa7b',
            enabled: true,
            threatLevel: 0,
            constraints: [
              {
                id: 'd4fe6780471e4543bcb0e28d0e122b69',
                name: { isPristine: true, value: 'Unpopular', trimmedValue: 'Unpopular', validationErrors: [] },
                enabled: true,
                operator: 'OR',
                conditions: [
                  {
                    conditionTypeId: 'RelativePopularity',
                    operator: '<',
                    value: { isPristine: true, value: '10', trimmedValue: '10', validationErrors: null },
                  },
                ],
              },
            ],
            actions: {
              develop: [{ actionTypeId: 'warn', target: null }],
              build: [{ actionTypeId: 'fail', target: null }],
              'stage-release': [{ actionTypeId: 'fail', target: null }],
              release: [{ actionTypeId: 'warn', target: null }],
              operate: [{ actionTypeId: 'warn', target: null }],
              proxy: [{ actionTypeId: 'warn', target: null }],
            },
            monitorNotifyActions: null,
          },
          currentPolicyOwner: {
            id: 'f3cea033acf84984ae08d9250db4aa7b',
            name: 'Org1 Heh',
          },
          isInherited: false,
          isOrgOwner: true,
          isRepositoryContainerOwner: false,
          isRepositoryManagerOwner: false,
          isRepositoryOwner: false,
          isRootOrg: false,
          originalProxyStageAction: [{ actionTypeId: 'warn', target: null }],
          policiesByOwner: getApplicablePoliciesResponse.policiesByOwner,
        });

        done();
      });
    });

    it('dispatches rejected action if loadApplicablePoliciesByOwner action fails', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicablePolicies(mockOwnerId)]: () => Promise.reject('something went wrong'),
          [getOrganizationUrl('e270271429f747ef9bebf4ca88f5e6c0')]: Promise.resolve({
            data: { id: 'e270271429f747ef9bebf4ca88f5e6c0', name: 'broadcast' },
          }),
        },
        put: { [getPermissionContextTestUrl('application', 'global')]: Promise.resolve({ data: ['WRITE'] }) },
      });

      store.dispatch(actions.loadPolicyEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(2);

        const actions = store.getActions();
        expect(actions.length).toBe(9);
        expect(actions).toHaveActionTypesInOrder([
          'policy/loadPolicyEditor/pending',
          'orgsAndPolicies/loadSelectedOwner/pending',
          'orgsAndPolicies/setShowLimitedFirewallAccessAlert',
          'orgsAndPolicies/loadSelectedOwner/fulfilled',
          'policy/checkEditIqPermission/pending',
          'orgsAndPolicies/loadApplicablePoliciesByOwner/pending',
          'orgsAndPolicies/loadApplicablePoliciesByOwner/rejected',
          'policy/checkEditIqPermission/fulfilled',
          'policy/loadPolicyEditor/rejected',
        ]);

        done();
      });
    });
  });

  describe('savePolicy', () => {
    let selectIsOrgOwnerSpy, selectIsEditModeSpy, selectHasPolicyCategoriesSpy, selectIsSbomManagerSpy, onSaveSpy;
    const currentPolicyId = 'currentPolicyId';
    const categories = [
      { id: '1', isApplied: true },
      { id: '2', isApplied: false },
    ];
    const currentPolicyData = {
      id: '89e50a2cc6174512814c89252e2ae668',
      name: 'safssss',
      ownerId: 'ROOT_ORGANIZATION_ID',
      threatLevel: 8,
      legacyViolationAllowed: false,
      constraints: [
        {
          id: '8080ad77e13840789d70c79e0d507172',
          name: 'fasd',
          operator: 'OR',
          conditions: [{ conditionTypeId: 'AgeInDays', operator: 'older than', value: '730', conditionIndex: 0 }],
        },
      ],
      actions: {},
      notifications: {
        userNotifications: [{ stageIds: ['proxy', 'operate'], emailAddress: 'sdf@sd.com' }],
        roleNotifications: [{ stageIds: ['operate', 'build', 'develop'], roleId: '1da70fae1fd54d6cb7999871ebdb9a36' }],
        jiraNotifications: [],
        webhookNotifications: [],
      },
    };

    const currentPolicy = {
      ...currentPolicyData,
      name: { trimmedValue: 'safssss' },
      constraints: [
        {
          id: '8080ad77e13840789d70c79e0d507172',
          name: { trimmedValue: 'fasd' },
          operator: 'OR',
          conditions: [{ conditionTypeId: 'AgeInDays', operator: 'older than', value: '730', conditionIndex: 0 }],
        },
      ],
    };

    beforeEach(() => {
      jest.spyOn(selectors, 'selectCurrentPolicy').mockReturnValue(currentPolicy);
      jest.spyOn(selectors, 'selectCategories').mockReturnValue(categories);
      selectIsOrgOwnerSpy = jest.spyOn(selectors, 'selectIsOrgOwner').mockReturnValue(true);
      selectIsEditModeSpy = jest.spyOn(selectors, 'selectIsEditMode').mockReturnValue(true);
      selectHasPolicyCategoriesSpy = jest.spyOn(selectors, 'selectHasPolicyCategories').mockReturnValue(true);
      selectIsSbomManagerSpy = jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(false);
      onSaveSpy = jest.fn().mockName('onSave');
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('saves an existing policy which is also the organization owner', (done) => {
      mockAxiosCalls({
        put: {
          [getPolicyUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({ data: currentPolicyData }),
          [getPolicyTagUrl(currentPolicyId, mockOwnerType, mockOwnerId)]: Promise.resolve('success'),
        },
      });

      store.dispatch(actions.savePolicy()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        expect(axios.put).toHaveBeenCalledTimes(2);
        expect(axios.put.mock.calls[0]).toEqual([expect.any(String), currentPolicyData]);

        const categoriesWithoutIsApplied = categories.filter(prop('isApplied')).map(omit(['isApplied']));
        expect(axios.put.mock.calls[1]).toEqual([expect.any(String), categoriesWithoutIsApplied]);

        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'policy/savePolicy/pending',
          'policy/savePolicy/fulfilled',
          'policy/saveMaskTimerDone',
        ]);
        expect(actions[1].payload).toEqual({ isEditMode: true });

        done();
      });
    });

    it('saves an existing policy which is also the organization owner in SBOM Manager', (done) => {
      selectIsSbomManagerSpy.mockReturnValue(true);
      mockAxiosCalls({
        put: {
          [getPolicyNotificationsUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({ data: currentPolicyData }),
        },
      });

      store.dispatch(actions.savePolicy()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        expect(axios.put).toHaveBeenCalled();
        expect(axios.put.mock.calls[0]).toEqual([expect.any(String), currentPolicyData]);

        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'policy/savePolicy/pending',
          'policy/savePolicy/fulfilled',
          'policy/saveMaskTimerDone',
        ]);
        expect(actions[1].payload).toEqual({ isEditMode: true });

        done();
      });
    });

    it('saves an existing policy which is also the organization owner but has no policy categories', (done) => {
      selectHasPolicyCategoriesSpy.mockReturnValue(false);
      mockAxiosCalls({
        put: {
          [getPolicyUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({ data: currentPolicyData }),
          [getPolicyTagUrl(currentPolicyId, mockOwnerType, mockOwnerId)]: Promise.resolve('success'),
        },
      });

      store.dispatch(actions.savePolicy()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        expect(axios.put).toHaveBeenCalledTimes(2);
        expect(axios.put.mock.calls[0]).toEqual([expect.any(String), currentPolicyData]);
        expect(axios.put.mock.calls[1]).toEqual([expect.any(String), []]);

        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'policy/savePolicy/pending',
          'policy/savePolicy/fulfilled',
          'policy/saveMaskTimerDone',
        ]);
        expect(actions[1].payload).toEqual({ isEditMode: true });

        done();
      });
    });

    it('saves an existing policy which is not the organization owner', (done) => {
      selectIsOrgOwnerSpy.mockReturnValue(false);
      mockAxiosCalls({
        put: {
          [getPolicyUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({ data: currentPolicyData }),
        },
      });

      store.dispatch(actions.savePolicy()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put.mock.calls[0]).toEqual([expect.any(String), currentPolicyData]);

        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'policy/savePolicy/pending',
          'policy/savePolicy/fulfilled',
          'policy/saveMaskTimerDone',
        ]);
        expect(actions[1].payload).toEqual({ isEditMode: true });

        done();
      });
    });

    it('creates a new policy which is also the organization owner', async () => {
      selectIsEditModeSpy.mockReturnValue(false);
      mockAxiosCalls({
        post: {
          [getPolicyUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({ data: currentPolicyData }),
        },
        put: {
          [getPolicyTagUrl(currentPolicyId, mockOwnerType, mockOwnerId)]: Promise.resolve('success'),
        },
      });

      await store.dispatch(actions.savePolicy({ onSaveExistingPolicy: onSaveSpy }));

      // Advance timers and run all pending promises
      jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      await Promise.resolve(); // Let the promise chain complete

      expect(axios.post).toHaveBeenCalledTimes(1);
      expect(axios.post).toHaveBeenCalledWith(expect.any(String), currentPolicyData);
      expect(axios.put).toHaveBeenCalledTimes(1);
      const categoriesWithoutIsApplied = categories.filter(prop('isApplied')).map(omit(['isApplied']));
      expect(axios.put).toHaveBeenCalledTimes(1);
      expect(axios.put).toHaveBeenCalledWith(expect.any(String), categoriesWithoutIsApplied);
      const dispatchedActions = store.getActions();

      // Verify stateReload is dispatched after success timer (NEXUS-46170)
      expect(dispatchedActions.length).toBe(4);
      expect(dispatchedActions).toHaveActionTypesInOrder([
        'policy/savePolicy/pending',
        'policy/savePolicy/fulfilled',
        'policy/saveMaskTimerDone',
        '@@reduxUiRouter/stateReload',
      ]);
      expect(dispatchedActions[1].payload).toBeUndefined();
      expect(dispatchedActions[3].payload).toBeUndefined(); // stateReload called with no args
      expect(onSaveSpy).not.toHaveBeenCalled();
    });

    it('dispatches rejected action if savePolicy request fails', (done) => {
      mockAxiosCalls({
        put: {
          [getPolicyUrl(mockOwnerType, mockOwnerId)]: () => Promise.reject('error'),
        },
      });

      store.dispatch(actions.savePolicy({ onSaveExistingPolicy: onSaveSpy })).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder(['policy/savePolicy/pending', 'policy/savePolicy/rejected']);

        done();
      });
    });

    it('dispatches rejected action if savePolicy request fails in SBOM Manager', (done) => {
      selectIsSbomManagerSpy.mockReturnValue(true);
      mockAxiosCalls({
        put: {
          [getPolicyNotificationsUrl(mockOwnerType, mockOwnerId)]: () => Promise.reject('error'),
        },
      });

      store.dispatch(actions.savePolicy({ onSaveExistingPolicy: onSaveSpy })).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder(['policy/savePolicy/pending', 'policy/savePolicy/rejected']);

        done();
      });
    });

    it('dispatches rejected action if getOwnerDetails request fails', (done) => {
      selectIsOrgOwnerSpy.mockReturnValue(false);
      mockAxiosCalls({
        put: {
          [getPolicyUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({ data: currentPolicyData }),
        },
      });

      store.dispatch(actions.savePolicy()).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put.mock.calls[0]).toEqual([expect.any(String), currentPolicyData]);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder(['policy/savePolicy/pending', 'policy/savePolicy/fulfilled']);

        done();
      });
    });
  });

  describe('removePolicy', () => {
    const currentPolicyId = 'currentPolicyId';
    beforeEach(() => {
      jest.spyOn(selectors, 'selectCurrentPolicy').mockReturnValue({ id: currentPolicyId });
    });

    it('removes a policy', (done) => {
      mockAxiosCalls({
        del: {
          [getPolicyCRUDUrl(mockOwnerType, mockOwnerId, currentPolicyId)]: Promise.resolve('success'),
        },
      });

      store.dispatch(actions.removePolicy()).then(() => {
        expect(axios.delete).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(6);
        expect(actions).toHaveActionTypesInOrder([
          'policy/removePolicy/pending',
          'policy/resetState',
          'policy/goToCreatePolicy/pending',
          '@@reduxUiRouter/stateGo',
          'policy/goToCreatePolicy/fulfilled',
          'policy/removePolicy/fulfilled',
        ]);
        expect(actions[5].payload).toBe(currentPolicyId);

        done();
      });
    });

    it('dispatches rejected action if removePolicy request fails', (done) => {
      mockAxiosCalls({
        del: {
          [getPolicyCRUDUrl(mockOwnerType, mockOwnerId, currentPolicyId)]: () => Promise.reject('error'),
        },
      });

      store.dispatch(actions.removePolicy()).then(() => {
        expect(axios.delete).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder(['policy/removePolicy/pending', 'policy/removePolicy/rejected']);

        done();
      });
    });
  });

  describe('updateOverrides adding', () => {
    beforeEach(() => {
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
    });
    it('calls update override endpoint with proper parameters and returns updated policy', (done) => {
      const currentPolicy = {
        id: 'policyID',
        policyActionsOverrides: {
          currentOwnerId: { build: 'warn' },
        },
        policyNotificationsOverrides: {
          currentOwnerId: { userNotifications: [{ emailAddress: 'user2@email.com', stageIds: ['build', 'release'] }] },
        },
      };

      const url = getPolicyOverridesUrl(mockOwnerType, mockOwnerId, 'policyID');
      jest.spyOn(selectors, 'selectCurrentPolicy').mockReturnValue(currentPolicy);
      jest.spyOn(selectors, 'selectActionsOverrideNeedsToBeAdded').mockReturnValue(true);
      jest.spyOn(selectors, 'selectNotificationsOverrideNeedsToBeAdded').mockReturnValue(true);
      jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwnerId').mockReturnValue('currentOwnerId');

      mockAxiosCalls({
        put: {
          [url]: Promise.resolve({ data: 'updated policy placeholder' }),
        },
      });

      store.dispatch(actions.updateOverrides()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put.mock.calls[0]).toEqual([
          url,
          {
            actions: { build: 'warn' },
            notifications: { userNotifications: [{ emailAddress: 'user2@email.com', stageIds: ['build', 'release'] }] },
          },
        ]);
        const actions = store.getActions();
        expect(actions.length).toBe(3);

        expect(actions).toHaveActionTypesInOrder([
          'policy/updateOverrides/pending',
          'policy/updateOverrides/fulfilled',
          'policy/saveMaskTimerDone',
        ]);
        expect(actions[1].payload).toEqual('updated policy placeholder');
        done();
      });
    });
  });

  describe('updateOverrides removing', () => {
    beforeEach(() => {
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
    });
    it('calls update override endpoint with proper parameters and returns updated policy', (done) => {
      const currentPolicy = {
        id: 'policyID',
      };

      const url = getPolicyOverridesUrl(mockOwnerType, mockOwnerId, 'policyID');
      jest.spyOn(selectors, 'selectCurrentPolicy').mockReturnValue(currentPolicy);
      jest.spyOn(selectors, 'selectActionsOverrideNeedsToBeRemoved').mockReturnValue(true);
      jest.spyOn(selectors, 'selectNotificationsOverrideNeedsToBeRemoved').mockReturnValue(true);

      mockAxiosCalls({
        put: {
          [url]: Promise.resolve({ data: 'updated policy placeholder' }),
        },
      });

      store.dispatch(actions.updateOverrides()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put.mock.calls[0]).toEqual([
          url,
          {
            actions: null,
            notifications: null,
          },
        ]);
        const actions = store.getActions();

        expect(actions.length).toBe(3);

        expect(actions).toHaveActionTypesInOrder([
          'policy/updateOverrides/pending',
          'policy/updateOverrides/fulfilled',
          'policy/saveMaskTimerDone',
        ]);
        expect(actions[1].payload).toEqual('updated policy placeholder');
        done();
      });
    });
  });

  describe('updateOverrides updating', () => {
    beforeEach(() => {
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
    });
    it('calls update override endpoint with proper parameters and returns updated policy', (done) => {
      const currentPolicy = {
        id: 'policyID',
        policyActionsOverrides: {
          currentOwnerId: { build: 'warn' },
        },
        policyNotificationsOverrides: {
          currentOwnerId: { userNotifications: [{ emailAddress: 'user2@email.com', stageIds: ['build', 'release'] }] },
        },
      };

      const url = getPolicyOverridesUrl(mockOwnerType, mockOwnerId, 'policyID');
      jest.spyOn(selectors, 'selectCurrentPolicy').mockReturnValue(currentPolicy);
      jest.spyOn(selectors, 'selectActionsOverrideNeedsToBeUpdated').mockReturnValue(true);
      jest.spyOn(selectors, 'selectNotificationsOverrideNeedsToBeUpdated').mockReturnValue(true);
      jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwnerId').mockReturnValue('currentOwnerId');

      mockAxiosCalls({
        put: {
          [url]: Promise.resolve({ data: 'updated policy placeholder' }),
        },
      });

      store.dispatch(actions.updateOverrides()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put.mock.calls[0]).toEqual([
          url,
          {
            actions: { build: 'warn' },
            notifications: { userNotifications: [{ emailAddress: 'user2@email.com', stageIds: ['build', 'release'] }] },
          },
        ]);
        const actions = store.getActions();
        expect(actions.length).toBe(3);

        expect(actions).toHaveActionTypesInOrder([
          'policy/updateOverrides/pending',
          'policy/updateOverrides/fulfilled',
          'policy/saveMaskTimerDone',
        ]);
        expect(actions[1].payload).toEqual('updated policy placeholder');
        done();
      });
    });
  });
});
