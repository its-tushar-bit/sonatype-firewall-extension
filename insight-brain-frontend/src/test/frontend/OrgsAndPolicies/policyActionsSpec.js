/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { actions, initialState } from 'MainRoot/OrgsAndPolicies/policySlice';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import * as selectors from 'MainRoot/OrgsAndPolicies/policySelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import PolicyResourceMockData from 'TestRoot/owner.manager/mock.data/policy.resource.mock.data';
import TagResourceMockData from 'TestRoot/owner.manager/mock.data/tag.resource.mock.data';
import {
  getApplicableCategoriesUrl,
  getApplicablePolicies,
  getPolicyActionsOverridesUrl,
  getPolicyCRUDUrl,
  getPolicyTagUrl,
  getPolicyUrl,
} from 'MainRoot/util/CLMLocation';
import { omit, prop } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { urlsByPurpose } from 'MainRoot/OrgsAndPolicies/stagesSlice';

describe('policySlice actions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, mockOwnerId, mockOwnerType, mockOwnerName;

  beforeEach(function () {
    store = SpecUtil.mockReduxStore({});
    mockOwnerId = 'ownerId';
    mockOwnerType = 'ownerType';
    mockOwnerName = 'ownerName';
    spyOn(orgsAndPoliciesSelectors, 'selectOwnerProperties').and.returnValue({
      ownerId: mockOwnerId,
      ownerType: mockOwnerType,
    });
    spyOn(routerSelectors, 'selectIsRepositories').and.returnValue(false);
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
      selectRouterCurrentParamsSpy = spyOn(routerSelectors, 'selectRouterCurrentParams').and.returnValue({
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
      selectRouterCurrentParamsSpy.and.returnValue({});
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
      selectRouterCurrentParamsSpy = spyOn(routerSelectors, 'selectRouterCurrentParams').and.returnValue({
        policyId: mockPolicyId,
      });
      spyOn(routerSelectors, 'selectIsRootOrganization').and.returnValue(false);
      selectIsOrganizationSpy = spyOn(routerSelectors, 'selectIsOrganization').and.returnValue(false);
    });

    it('loads data for a new policy', async () => {
      selectRouterCurrentParamsSpy.and.returnValue({});
      mockAxiosCalls({
        get: {
          [urlsByPurpose.action]: Promise.resolve({ data: [] }),
          [getApplicablePolicies(mockOwnerType, mockOwnerId)]: Promise.resolve({
            data: getApplicablePoliciesResponse,
          }),
        },
      });

      await store.dispatch(actions.loadPolicyEditor());
      await Promise.resolve();

      expect(axios.get).toHaveBeenCalledTimes(4);

      const dispatchedActions = store.getActions();

      expect(dispatchedActions.length).toBe(8);
      expect(dispatchedActions).toHaveActionTypesInOrder([
        'policy/loadPolicyEditor/pending',
        'orgsAndPolicies/loadApplicablePoliciesByOwner/pending',
        'orgsAndPolicies/loadApplicablePoliciesByOwner/fulfilled',
        'constraint/loadConstraint/pending',
        'stages/loadStageTypes/pending',
        'policy/loadPolicyEditor/fulfilled',
        'stages/loadStageTypes/fulfilled',
      ]);

      expect(dispatchedActions[5].payload).toEqual({
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
        currentPolicy: initialState.currentPolicy,
        currentPolicyOwner: {
          id: 'f3cea033acf84984ae08d9250db4aa7b',
          name: 'Org1 Heh',
        },
        isOrgOwner: false,
        isRootOrg: false,
        policiesByOwner: getApplicablePoliciesResponse.policiesByOwner,
      });
    });

    it('loads data for a new policy that is also the org owner', (done) => {
      selectRouterCurrentParamsSpy.and.returnValue({});
      selectIsOrganizationSpy.and.returnValue(true);
      mockAxiosCalls({
        [urlsByPurpose.action]: Promise.resolve({ data: [] }),
        get: {
          [getApplicablePolicies(mockOwnerType, mockOwnerId)]: Promise.resolve({
            data: getApplicablePoliciesResponse,
          }),
        },
      });

      store.dispatch(actions.loadPolicyEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(5);

        const actions = store.getActions();

        expect(actions).toHaveActionType('policy/loadCategoriesForPolicy/pending');
        expect(actions[9].payload).toEqual(jasmine.objectContaining({ isOrgOwner: true }));

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
        },
      });

      store.dispatch(actions.loadPolicyEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(6);

        const actions = store.getActions();

        expect(actions.length).toBe(10);
        expect(actions).toHaveActionTypesInOrder([
          'policy/loadPolicyEditor/pending',
          'orgsAndPolicies/loadApplicablePoliciesByOwner/pending',
          'orgsAndPolicies/loadApplicablePoliciesByOwner/fulfilled',
          'constraint/loadConstraint/pending',
          'stages/loadStageTypes/pending',
          'policy/loadCategoriesForPolicy/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/rejected',
          'policy/loadPolicyEditor/fulfilled',
          'stages/loadStageTypes/fulfilled',
        ]);

        expect(actions[8].payload).toEqual({
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
                name: { isPristine: true, value: 'Unpopular', trimmedValue: 'Unpopular', validationErrors: null },
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
        },
      });

      store.dispatch(actions.loadPolicyEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(4);
        expect(actions).toHaveActionTypesInOrder([
          'policy/loadPolicyEditor/pending',
          'orgsAndPolicies/loadApplicablePoliciesByOwner/pending',
          'orgsAndPolicies/loadApplicablePoliciesByOwner/rejected',
          'policy/loadPolicyEditor/rejected',
        ]);

        done();
      });
    });
  });

  describe('savePolicy', () => {
    let selectIsOrgOwnerSpy, selectIsEditModeSpy, selectHasPolicyCategoriesSpy, onSaveSpy;
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
      policyViolationGrandfatheringAllowed: false,
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
      spyOn(selectors, 'selectCurrentPolicy').and.returnValue(currentPolicy);
      spyOn(selectors, 'selectCategories').and.returnValue(categories);
      selectIsOrgOwnerSpy = spyOn(selectors, 'selectIsOrgOwner').and.returnValue(true);
      selectIsEditModeSpy = spyOn(selectors, 'selectIsEditMode').and.returnValue(true);
      selectHasPolicyCategoriesSpy = spyOn(selectors, 'selectHasPolicyCategories').and.returnValue(true);
      onSaveSpy = jasmine.createSpy('onSave');
      jasmine.clock().install();
    });

    afterEach(() => {
      jasmine.clock().uninstall();
    });

    it('saves an existing policy which is also the organization owner', (done) => {
      mockAxiosCalls({
        put: {
          [getPolicyUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({ data: currentPolicyData }),
          [getPolicyTagUrl(currentPolicyId, mockOwnerType, mockOwnerId)]: Promise.resolve('success'),
        },
      });

      store.dispatch(actions.savePolicy()).then(() => {
        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        expect(axios.put).toHaveBeenCalledTimes(2);
        expect(axios.put.calls.argsFor(0)).toEqual([jasmine.any(String), currentPolicyData]);

        const categoriesWithoutIsApplied = categories.filter(prop('isApplied')).map(omit(['isApplied']));
        expect(axios.put.calls.argsFor(1)).toEqual([jasmine.any(String), categoriesWithoutIsApplied]);

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
      selectHasPolicyCategoriesSpy.and.returnValue(false);
      mockAxiosCalls({
        put: {
          [getPolicyUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({ data: currentPolicyData }),
          [getPolicyTagUrl(currentPolicyId, mockOwnerType, mockOwnerId)]: Promise.resolve('success'),
        },
      });

      store.dispatch(actions.savePolicy()).then(() => {
        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        expect(axios.put).toHaveBeenCalledTimes(2);
        expect(axios.put.calls.argsFor(0)).toEqual([jasmine.any(String), currentPolicyData]);
        expect(axios.put.calls.argsFor(1)).toEqual([jasmine.any(String), []]);

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
      selectIsOrgOwnerSpy.and.returnValue(false);
      mockAxiosCalls({
        put: {
          [getPolicyUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({ data: currentPolicyData }),
        },
      });

      store.dispatch(actions.savePolicy()).then(() => {
        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put.calls.argsFor(0)).toEqual([jasmine.any(String), currentPolicyData]);

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

    it('creates a new policy which is also the organization owner', (done) => {
      selectIsEditModeSpy.and.returnValue(false);
      mockAxiosCalls({
        post: {
          [getPolicyUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({ data: currentPolicyData }),
        },
        put: {
          [getPolicyTagUrl(currentPolicyId, mockOwnerType, mockOwnerId)]: Promise.resolve('success'),
        },
      });

      store.dispatch(actions.savePolicy({ onSaveExistingPolicy: onSaveSpy })).then(() => {
        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        expect(axios.post).toHaveBeenCalledOnceWith(jasmine.any(String), currentPolicyData);
        expect(axios.put).toHaveBeenCalledTimes(1);
        const categoriesWithoutIsApplied = categories.filter(prop('isApplied')).map(omit(['isApplied']));
        expect(axios.put).toHaveBeenCalledOnceWith(jasmine.any(String), categoriesWithoutIsApplied);
        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder(['policy/savePolicy/pending', 'policy/savePolicy/fulfilled']);
        expect(actions[1].payload).toBeUndefined();
        expect(onSaveSpy).not.toHaveBeenCalled();

        done();
      });
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

    it('dispatches rejected action if getOwnerDetails request fails', (done) => {
      selectIsOrgOwnerSpy.and.returnValue(false);
      mockAxiosCalls({
        put: {
          [getPolicyUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({ data: currentPolicyData }),
        },
      });

      store.dispatch(actions.savePolicy()).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put.calls.argsFor(0)).toEqual([jasmine.any(String), currentPolicyData]);

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
      spyOn(selectors, 'selectCurrentPolicy').and.returnValue({ id: currentPolicyId });
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

        expect(actions.length).toBe(5);
        expect(actions).toHaveActionTypesInOrder([
          'policy/removePolicy/pending',
          'policy/resetIsDirty',
          'policy/goToCreatePolicy/pending',
          'policy/goToCreatePolicy/rejected',
          'policy/removePolicy/fulfilled',
        ]);
        expect(actions[4].payload).toBe(currentPolicyId);

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

  describe('saveActionsOverride', () => {
    beforeEach(() => {
      jasmine.clock().install();
    });

    afterEach(() => {
      jasmine.clock().uninstall();
    });
    it('calls crud endpoint with proper parameters and returns updated policy', (done) => {
      const currentPolicy = {
        id: 'policyID',
        policyActionsOverrides: {
          currentOwnerId: { build: 'warn' },
        },
      };

      const url = getPolicyActionsOverridesUrl(mockOwnerType, mockOwnerId, 'policyID');
      spyOn(selectors, 'selectCurrentPolicy').and.returnValue(currentPolicy);
      spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwnerId').and.returnValue('currentOwnerId');

      mockAxiosCalls({
        put: {
          [url]: Promise.resolve({ data: 'updated policy placeholder' }),
        },
      });

      store.dispatch(actions.saveActionsOverride()).then(() => {
        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put.calls.argsFor(0)).toEqual([url, { build: 'warn' }]);
        const actions = store.getActions();
        expect(actions.length).toBe(3);

        expect(actions).toHaveActionTypesInOrder([
          'policy/saveActionsOverride/pending',
          'policy/saveActionsOverride/fulfilled',
          'policy/saveMaskTimerDone',
        ]);
        expect(actions[1].payload).toEqual('updated policy placeholder');
        done();
      });
    });
  });

  describe('removeActionsOverride', () => {
    beforeEach(() => {
      jasmine.clock().install();
    });

    afterEach(() => {
      jasmine.clock().uninstall();
    });
    it('calls remove override endpoint with proper parameters', (done) => {
      const currentPolicy = {
        id: 'policyID',
      };

      const url = getPolicyActionsOverridesUrl(mockOwnerType, mockOwnerId, 'policyID');
      spyOn(selectors, 'selectCurrentPolicy').and.returnValue(currentPolicy);

      mockAxiosCalls({
        del: {
          [url]: Promise.resolve({ data: 'removed policy actions overrides placeholder' }),
        },
      });

      store.dispatch(actions.removeActionsOverride()).then(() => {
        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        expect(axios.delete).toHaveBeenCalledTimes(1);
        expect(axios.delete.calls.argsFor(0)).toEqual([url]);
        const actions = store.getActions();

        expect(actions.length).toBe(3);

        expect(actions).toHaveActionTypesInOrder([
          'policy/removeActionsOverride/pending',
          'policy/removeActionsOverride/fulfilled',
          'policy/saveMaskTimerDone',
        ]);
        expect(actions[1].payload).toEqual('removed policy actions overrides placeholder');
        done();
      });
    });
  });
});
