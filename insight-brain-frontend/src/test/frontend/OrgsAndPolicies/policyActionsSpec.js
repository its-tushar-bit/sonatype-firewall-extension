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
  getPolicyCRUDUrl,
  getPolicyTagUrl,
  getPolicyUrl,
} from 'MainRoot/util/CLMLocation';
import { omit, prop } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

describe('policy actions', () => {
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
  });

  describe('loadApplicablePoliciesByOwner', () => {
    it('loads policy tags successfully', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicablePolicies(mockOwnerType, mockOwnerId)]: Promise.resolve({
            data: 'content',
          }),
        },
      });

      store.dispatch(actions.loadApplicablePoliciesByOwner()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'policy/loadApplicablePoliciesByOwner/pending',
          'policy/loadApplicablePoliciesByOwner/fulfilled',
        ]);

        done();
      });
    });

    it('dispatches rejected action if loadApplicablePoliciesByOwner request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicablePolicies(mockOwnerId)]: () => Promise.reject('something went wrong'),
        },
      });

      store.dispatch(actions.loadApplicablePoliciesByOwner()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'policy/loadApplicablePoliciesByOwner/pending',
          'policy/loadApplicablePoliciesByOwner/rejected',
        ]);
        done();
      });
    });
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

    it('loads data for a new policy', (done) => {
      selectRouterCurrentParamsSpy.and.returnValue({});
      mockAxiosCalls({
        get: {
          [getApplicablePolicies(mockOwnerType, mockOwnerId)]: Promise.resolve({
            data: getApplicablePoliciesResponse,
          }),
        },
      });

      store.dispatch(actions.loadPolicyEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(3);

        const actions = store.getActions();

        expect(actions.length).toBe(8);
        expect(actions).toHaveActionTypesInOrder([
          'policy/loadPolicyEditor/pending',
          'policy/loadApplicablePoliciesByOwner/pending',
          'productFeatures/fetchProductFeaturesIfNeeded/pending',
          'policy/loadApplicablePoliciesByOwner/fulfilled',
          'productFeatures/fetchProductFeaturesIfNeeded/fulfilled',
          'orgsAndPoliciesConstraint/loadConstraint/pending',
          'orgsAndPolicies/updatedOwnerHandler',
          'policy/loadPolicyEditor/fulfilled',
        ]);

        expect(actions[7].payload).toEqual({
          readOnly: undefined,
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
          currentPolicy: initialState.currentPolicy,
          isOrgOwner: false,
          isRootOrg: false,
        });

        done();
      });
    });

    it('loads data for a new policy that is also the org owner', (done) => {
      selectRouterCurrentParamsSpy.and.returnValue({});
      selectIsOrganizationSpy.and.returnValue(true);
      mockAxiosCalls({
        get: {
          [getApplicablePolicies(mockOwnerType, mockOwnerId)]: Promise.resolve({
            data: getApplicablePoliciesResponse,
          }),
        },
      });

      store.dispatch(actions.loadPolicyEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(4);

        const actions = store.getActions();

        expect(actions).toHaveActionType('policy/loadCategoriesForPolicy/pending');

        expect(actions[10].payload).toEqual(jasmine.objectContaining({ isOrgOwner: true }));

        done();
      });
    });

    it('loads data for an existing policy', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicablePolicies(mockOwnerType, mockOwnerId)]: Promise.resolve({
            data: getApplicablePoliciesResponse,
          }),
        },
      });

      store.dispatch(actions.loadPolicyEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(5);

        const actions = store.getActions();

        expect(actions.length).toBe(11);
        expect(actions).toHaveActionTypesInOrder([
          'policy/loadPolicyEditor/pending',
          'policy/loadApplicablePoliciesByOwner/pending',
          'productFeatures/fetchProductFeaturesIfNeeded/pending',
          'policy/loadApplicablePoliciesByOwner/fulfilled',
          'productFeatures/fetchProductFeaturesIfNeeded/fulfilled',
          'orgsAndPoliciesConstraint/loadConstraint/pending',
          'orgsAndPolicies/updatedOwnerHandler',
          'policy/loadCategoriesForPolicy/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/rejected',
          'policy/loadPolicyEditor/fulfilled',
        ]);

        expect(actions[10].payload).toEqual({
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
          currentPolicy: {
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
          readOnly: false,
          isOrgOwner: true,
          isRootOrg: false,
          originalProxyStageAction: [{ actionTypeId: 'warn', target: null }],
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

        expect(actions.length).toBe(6);
        expect(actions).toHaveActionTypesInOrder([
          'policy/loadPolicyEditor/pending',
          'policy/loadApplicablePoliciesByOwner/pending',
          'policy/loadApplicablePoliciesByOwner/rejected',
          'productFeatures/fetchProductFeaturesIfNeeded/pending',
          'productFeatures/fetchProductFeaturesIfNeeded/fulfilled',
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
    const currentPolicy = {
      id: '89e50a2cc6174512814c89252e2ae668',
      name: 'safssss',
      ownerId: 'ROOT_ORGANIZATION_ID',
      threatLevel: 8,
      policyViolationGrandfatheringAllowed: false,
      constraints: [
        {
          id: '8080ad77e13840789d70c79e0d507172',
          name: 'fdsf',
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

    beforeEach(() => {
      spyOn(selectors, 'selectCurrentPolicy').and.returnValue(currentPolicy);
      spyOn(selectors, 'selectCategories').and.returnValue(categories);
      selectIsOrgOwnerSpy = spyOn(selectors, 'selectIsOrgOwner').and.returnValue(true);
      selectIsEditModeSpy = spyOn(selectors, 'selectIsEditMode').and.returnValue(true);
      selectHasPolicyCategoriesSpy = spyOn(selectors, 'selectHasPolicyCategories').and.returnValue(true);
      onSaveSpy = jasmine.createSpy('onSave');
    });

    it('saves an existing policy which is also the organization owner', (done) => {
      mockAxiosCalls({
        put: {
          [getPolicyUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({ data: currentPolicy }),
          [getPolicyTagUrl(currentPolicyId, mockOwnerType, mockOwnerId)]: Promise.resolve('success'),
        },
      });

      store.dispatch(actions.savePolicy({ onSaveExistingPolicy: onSaveSpy })).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(2);
        expect(axios.put.calls.argsFor(0)).toEqual([jasmine.any(String), currentPolicy]);

        const categoriesWithoutIsApplied = categories.filter(prop('isApplied')).map(omit(['isApplied']));
        expect(axios.put.calls.argsFor(1)).toEqual([jasmine.any(String), categoriesWithoutIsApplied]);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder(['policy/savePolicy/pending', 'policy/savePolicy/fulfilled']);
        expect(actions[1].payload).toEqual({ isEditMode: true });
        expect(onSaveSpy).toHaveBeenCalledTimes(1);

        done();
      });
    });

    it('saves an existing policy which is also the organization owner but has no policy categories', (done) => {
      selectHasPolicyCategoriesSpy.and.returnValue(false);
      mockAxiosCalls({
        put: {
          [getPolicyUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({ data: currentPolicy }),
          [getPolicyTagUrl(currentPolicyId, mockOwnerType, mockOwnerId)]: Promise.resolve('success'),
        },
      });

      store.dispatch(actions.savePolicy({ onSaveExistingPolicy: onSaveSpy })).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(2);
        expect(axios.put.calls.argsFor(0)).toEqual([jasmine.any(String), currentPolicy]);
        expect(axios.put.calls.argsFor(1)).toEqual([jasmine.any(String), []]);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder(['policy/savePolicy/pending', 'policy/savePolicy/fulfilled']);
        expect(actions[1].payload).toEqual({ isEditMode: true });
        expect(onSaveSpy).toHaveBeenCalledTimes(1);

        done();
      });
    });

    it('saves an existing policy which is not the organization owner', (done) => {
      selectIsOrgOwnerSpy.and.returnValue(false);
      mockAxiosCalls({
        put: {
          [getPolicyUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({ data: currentPolicy }),
        },
      });

      store.dispatch(actions.savePolicy({ onSaveExistingPolicy: onSaveSpy })).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put.calls.argsFor(0)).toEqual([jasmine.any(String), currentPolicy]);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder(['policy/savePolicy/pending', 'policy/savePolicy/fulfilled']);
        expect(actions[1].payload).toEqual({ isEditMode: true });
        expect(onSaveSpy).toHaveBeenCalledTimes(1);

        done();
      });
    });

    it('creates a new policy which is also the organization owner', (done) => {
      selectIsEditModeSpy.and.returnValue(false);
      mockAxiosCalls({
        post: {
          [getPolicyUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({ data: currentPolicy }),
        },
        put: {
          [getPolicyTagUrl(currentPolicyId, mockOwnerType, mockOwnerId)]: Promise.resolve('success'),
        },
      });
      jasmine.clock().install();

      store.dispatch(actions.savePolicy({ onSaveExistingPolicy: onSaveSpy })).then(() => {
        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        expect(axios.post).toHaveBeenCalledOnceWith(jasmine.any(String), currentPolicy);
        expect(axios.put).toHaveBeenCalledTimes(1);
        const categoriesWithoutIsApplied = categories.filter(prop('isApplied')).map(omit(['isApplied']));
        expect(axios.put).toHaveBeenCalledOnceWith(jasmine.any(String), categoriesWithoutIsApplied);
        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder(['policy/savePolicy/pending', 'policy/savePolicy/fulfilled']);
        expect(actions[1].payload).toBeUndefined();
        expect(onSaveSpy).not.toHaveBeenCalled();

        jasmine.clock().uninstall();
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

        expect(actions.length).toBe(6);
        expect(actions).toHaveActionTypesInOrder([
          'policy/removePolicy/pending',
          'policy/resetIsDirty',
          'policy/resetDeleteModalState',
          'policy/goToCreatePolicy/pending',
          'policy/goToCreatePolicy/rejected',
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
});
