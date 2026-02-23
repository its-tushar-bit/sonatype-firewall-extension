/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { actions } from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/createEditApplicationCategoriesSlice';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import * as selectors from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/createEditApplicationCategoriesSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as ownerSideNavSelectors from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';
import {
  getApplicableCategoriesUrl,
  getCategoriesUrl,
  getDeleteCategoriesUrl,
  getOrganizationAppliedTagUrl,
  getOrganizationPolicyTagUrl,
  getApplicablePolicies,
} from 'MainRoot/util/CLMLocation';
import TagResourceMockData from 'TestRoot/OrgsAndPolicies/mock.data/tag.resource.mock.data';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

import 'TestRoot/SpecUtil';

const { initialState: rscInitialState } = nxTextInputStateHelpers;

describe('createEditApplicationCategoriesSlice Actions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, mockOwnerId, mockOwnerType, mockOwnerName;
  const matchingApplicationIdForTag = 'applicationId';

  const allApplications = [
    {
      id: matchingApplicationIdForTag,
      publicId: 'activemq',
      name: 'activemq',
      organizationId: '29f73564c87942a9bf929a0af5ae6f78',
      organizationName: 'org 2',
      contact: null,
    },
    {
      id: '19c2b96fdb4842d3b63c2f6251b003af',
      publicId: 'appConsumer',
      name: 'appConsumer',
      organizationId: '29f73564c87942a9bf929a0af5ae6f78',
      organizationName: 'org 2',
      contact: null,
    },
  ];

  beforeEach(function () {
    const state = {
      router: {
        currentParams: {
          applicationPublicId: 'alpine-test',
        },
        currentState: {
          name: 'application',
        },
      },
    };
    store = SpecUtil.mockReduxStore(state);
    mockOwnerId = 'ownerId';
    mockOwnerType = 'ownerType';
    mockOwnerName = 'ownerName';
    jest.spyOn(orgsAndPoliciesSelectors, 'selectOwnerProperties').mockReturnValue({
      ownerId: mockOwnerId,
      ownerType: 'ownerType',
    });
    jest.spyOn(ownerSideNavSelectors, 'selectOwnersFlattenEntries').mockReturnValue({ applications: allApplications });
  });

  describe('loadOrganizationPolicyTags', () => {
    it('loads policy tags successfully', (done) => {
      mockAxiosCalls({
        get: {
          [getOrganizationPolicyTagUrl(mockOwnerId)]: Promise.resolve({
            data: {},
          }),
        },
      });

      store.dispatch(actions.loadOrganizationPolicyTags()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/createEdit/loadOrganizationPolicyTags/pending',
          'applicationCategories/createEdit/loadOrganizationPolicyTags/fulfilled',
        ]);

        done();
      });
    });

    it('dispatches rejected action if loadOrganizationPolicyTags request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getOrganizationAppliedTagUrl(mockOwnerId)]: () => Promise.reject('something went wrong'),
        },
      });

      store.dispatch(actions.loadOrganizationPolicyTags()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/createEdit/loadOrganizationPolicyTags/pending',
          'applicationCategories/createEdit/loadOrganizationPolicyTags/rejected',
        ]);
        done();
      });
    });
  });

  describe('loadOrganizationAppliedTag', () => {
    it('loads applied tags successfully', (done) => {
      mockAxiosCalls({
        get: {
          [getOrganizationAppliedTagUrl(mockOwnerId)]: Promise.resolve({
            data: {},
          }),
        },
      });

      store.dispatch(actions.loadOrganizationAppliedTag()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/createEdit/loadOrganizationAppliedTag/pending',
          'applicationCategories/createEdit/loadOrganizationAppliedTag/fulfilled',
        ]);

        done();
      });
    });

    it('dispatches rejected action if loadOrganizationAppliedTag request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getOrganizationAppliedTagUrl(mockOwnerId)]: () => Promise.reject('something went wrong'),
        },
      });

      store.dispatch(actions.loadOrganizationAppliedTag()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/createEdit/loadOrganizationAppliedTag/pending',
          'applicationCategories/createEdit/loadOrganizationAppliedTag/rejected',
        ]);
        done();
      });
    });
  });

  describe('loadApplicableCategoriesByOwner', () => {
    it('loads applicable categories by owner successfully', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicableCategoriesUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({
            data: {},
          }),
        },
      });

      store.dispatch(actions.loadApplicableCategoriesByOwner()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/fulfilled',
        ]);

        done();
      });
    });

    it('dispatches rejected action if loadApplicableCategoriesByOwner request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicableCategoriesUrl(mockOwnerType, mockOwnerId)]: () => Promise.reject('something went wrong'),
        },
      });

      store.dispatch(actions.loadApplicableCategoriesByOwner()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/rejected',
        ]);
        done();
      });
    });
  });

  describe('loadApplicableCategories', () => {
    it('loads applicable categories successfully', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicableCategoriesUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({
            data: TagResourceMockData.getApplicationCategoriesUrl(mockOwnerType, mockOwnerId, mockOwnerName),
          }),
        },
      });

      store.dispatch(actions.loadApplicableCategories()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(4);

        const fulfilledApplicableCategoriesAction = actions.find(
          ({ type }) => type === 'applicationCategories/createEdit/loadApplicableCategories/fulfilled'
        );

        expect(fulfilledApplicableCategoriesAction.payload.appCategoryOwners).toBeDefined();

        const appCategoryOwners = fulfilledApplicableCategoriesAction.payload.appCategoryOwners;

        expect(appCategoryOwners[0]).toEqual(expect.objectContaining({ parent: false }));
        expect(appCategoryOwners[1]).toEqual(expect.objectContaining({ parent: true }));

        done();
      });
    });

    it('dispatches rejected action if loadApplicableCategories request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicableCategoriesUrl(mockOwnerType, mockOwnerId)]: () => Promise.reject('something went wrong'),
        },
      });

      store.dispatch(actions.loadApplicableCategories()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(4);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/createEdit/loadApplicableCategories/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/rejected',
          'applicationCategories/createEdit/loadApplicableCategories/rejected',
        ]);

        done();
      });
    });
  });

  describe('goToCreateCategory', () => {
    beforeEach(() => {
      jest.spyOn(routerSelectors, 'selectRouterSlice').mockReturnValue({
        currentState: {
          name: 'organization.somewhere',
        },
        currentParams: {
          organizationId: 'organizationId',
        },
      });
    });

    it('redirects to proper create category path', (done) => {
      store.dispatch(actions.goToCreateCategory()).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/createEdit/goToCreateCategory/pending',
          '@@reduxUiRouter/stateGo',
          'applicationCategories/createEdit/goToCreateCategory/fulfilled',
        ]);

        expect(actions[1].payload).toEqual({
          to: 'management.edit.organization.create-category',
          params: {
            organizationId: 'organizationId',
          },
          options: undefined,
        });

        done();
      });
    });
  });

  describe('loadCategoryEditor', () => {
    const categoryId = 'appCategoryId_1';

    const policyTags = [
      {
        id: '5c7a17c43c764cf3966a6906d8543b44',
        policyId: 'cbee1e3cdf2440db88f6c84d7d7582d4',
        tagId: categoryId,
      },
      {
        id: '1ab73ced37a6438d93251f797ea57d30',
        policyId: '39b9c27a34444fc28f3f5bac66e4622f',
        tagId: '7ce9b4fb7a47409f96a438ab7163cae8',
      },
    ];

    const policiesByOwner = [
      {
        ownerId: 'ROOT_ORGANIZATION_ID',
        ownerName: 'Root Organization',
        ownerType: 'organization',
        policies: [
          {
            id: '074e9af7b6ae4604a6df783c5452f3ef',
            name: 'Architecture-Cleanup',
          },
          {
            id: 'cbee1e3cdf2440db88f6c84d7d7582d4',
            name: 'Architecture-Quality',
          },
        ],
      },
    ];

    const applicationTagsByOwner = [
      {
        ownerId: 'ROOT_ORGANIZATION_ID',
        ownerName: 'Root Organization',
        ownerType: 'organization',
        applicationTags: [
          {
            id: 'ce37471280114bb7b3d64b30c65e6e86',
            applicationId: matchingApplicationIdForTag,
            tagId: categoryId,
          },
          {
            id: 'f47f64880c5d44b882045b564ecf3dd7',
            applicationId: 'd0fbe038085941c9a25f62bb9eb7f085',
            tagId: '34f1f000d7a54abaa2b12731cdff780f',
          },
        ],
      },
    ];

    const flattenedApplicationCategories = [
      {
        color: 'black',
        description: 'Description 1',
        id: 'appCategoryId_1',
        name: 'Category 1',
        organizationId: 'orgownerid',
      },
      {
        color: 'black',
        description: 'Description 2',
        id: 'appCategoryId_2',
        name: 'Category 2',
        organizationId: 'orgownerid',
      },
      {
        color: 'red',
        description: 'Description 3',
        id: 'appCategoryId_3',
        name: 'Category 3',
        organizationId: 'rootorgownerid',
      },
    ];

    let selectIsEditModeSpy;
    beforeEach(() => {
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({ categoryId });

      selectIsEditModeSpy = jest.spyOn(selectors, 'selectIsEditMode').mockReturnValue(true);
    });

    it('load category editor for creating new category', (done) => {
      selectIsEditModeSpy.mockReturnValue(false);

      mockAxiosCalls({
        get: {
          [getApplicableCategoriesUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({
            data: TagResourceMockData.getApplicationCategoriesUrl(mockOwnerType, mockOwnerId, mockOwnerName),
          }),
        },
      });

      store.dispatch(actions.loadCategoryEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();
        expect(actions.length).toBe(4);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/createEdit/loadCategoryEditor/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/fulfilled',
          'applicationCategories/createEdit/loadCategoryEditor/fulfilled',
        ]);

        expect(actions.length).toBe(4);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/createEdit/loadCategoryEditor/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/fulfilled',
          'applicationCategories/createEdit/loadCategoryEditor/fulfilled',
        ]);

        expect(actions[3].payload).toEqual({
          siblings: flattenedApplicationCategories,
          currentCategory: {
            name: '',
            description: '',
            color: 'light-purple',
          },
        });

        done();
      });
    });

    it('load category editor for editing an existing category', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicableCategoriesUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({
            data: TagResourceMockData.getApplicationCategoriesUrl(mockOwnerType, mockOwnerId, mockOwnerName),
          }),
          [getOrganizationAppliedTagUrl(mockOwnerId)]: Promise.resolve({
            data: { applicationTagsByOwner },
          }),
          [getOrganizationPolicyTagUrl(mockOwnerId)]: Promise.resolve({
            data: policyTags,
          }),
          [getApplicablePolicies(mockOwnerType, mockOwnerId)]: Promise.resolve({
            data: { policiesByOwner },
          }),
        },
      });

      store.dispatch(actions.loadCategoryEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(4);

        const actions = store.getActions();

        expect(actions.length).toBe(10);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/createEdit/loadCategoryEditor/pending',
          'applicationCategories/createEdit/loadOrganizationAppliedTag/pending',
          'orgsAndPolicies/loadApplicablePoliciesByOwner/pending',
          'applicationCategories/createEdit/loadOrganizationPolicyTags/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/pending',
          'applicationCategories/createEdit/loadOrganizationAppliedTag/fulfilled',
          'orgsAndPolicies/loadApplicablePoliciesByOwner/fulfilled',
          'applicationCategories/createEdit/loadOrganizationPolicyTags/fulfilled',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/fulfilled',
          'applicationCategories/createEdit/loadCategoryEditor/fulfilled',
        ]);

        expect(actions[9].payload).toEqual({
          siblings: flattenedApplicationCategories,
          applicationTags: [
            {
              id: 'ce37471280114bb7b3d64b30c65e6e86',
              applicationId: 'applicationId',
              tagId: 'appCategoryId_1',
            },
            {
              id: 'f47f64880c5d44b882045b564ecf3dd7',
              applicationId: 'd0fbe038085941c9a25f62bb9eb7f085',
              tagId: '34f1f000d7a54abaa2b12731cdff780f',
            },
          ],
          currentCategory: {
            color: 'black',
            description: 'Description 1',
            id: 'appCategoryId_1',
            name: 'Category 1',
            organizationId: 'orgownerid',
          },
          tagPolicyList: ['Architecture-Quality'],
        });

        done();
      });
    });

    it('dispatches reject action if save request fails', (done) => {
      selectIsEditModeSpy.mockReturnValue(false);

      mockAxiosCalls({
        get: {
          [getApplicableCategoriesUrl(mockOwnerType, mockOwnerId)]: Promise.resolve(
            'failed to load applicable categories'
          ),
        },
      });

      store.dispatch(actions.loadCategoryEditor()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(4);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/createEdit/loadCategoryEditor/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/pending',
          'applicationCategories/createEdit/loadApplicableCategoriesByOwner/fulfilled',
          'applicationCategories/createEdit/loadCategoryEditor/rejected',
        ]);

        done();
      });
    });
  });

  describe('saveApplicationCategory', () => {
    const currentCategoryData = { color: 'red', description: 'red is blue', name: 'old' };
    beforeEach(() => {
      jest.spyOn(selectors, 'selectCurrentCategory').mockReturnValue({
        color: 'red',
        description: rscInitialState('red is blue'),
        name: rscInitialState('old'),
      });

      jest.spyOn(selectors, 'selectIsEditMode').mockReturnValue(true);
    });

    it('updates category successfully', (done) => {
      const currentCategoryData = {
        color: 'red',
        description: 'red is blue',
        name: 'old',
      };
      const savedCategory = {
        ownerId: mockOwnerId,
        ownerType: mockOwnerType,
        ...currentCategoryData,
      };
      mockAxiosCalls({
        put: {
          [getCategoriesUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({
            data: savedCategory,
          }),
        },
      });

      store.dispatch(actions.saveApplicationCategory()).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put).toHaveBeenCalledWith(
          `/api/v2/applicationCategories/${mockOwnerType}/${mockOwnerId}`,
          currentCategoryData
        );

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/createEdit/saveApplicationCategory/pending',
          'applicationCategories/createEdit/saveApplicationCategory/fulfilled',
        ]);

        expect(actions[1].payload).toEqual({
          savedCategory: savedCategory,
          isEditMode: true,
        });

        done();
      });
    });

    it('dispatches reject action if save request fails', (done) => {
      mockAxiosCalls({
        put: {
          [getCategoriesUrl(mockOwnerType, mockOwnerId)]: () => Promise.reject('could not save category'),
        },
      });

      store.dispatch(actions.saveApplicationCategory()).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put).toHaveBeenCalledWith(
          `/api/v2/applicationCategories/${mockOwnerType}/${mockOwnerId}`,
          currentCategoryData
        );

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/createEdit/saveApplicationCategory/pending',
          'applicationCategories/createEdit/saveApplicationCategory/rejected',
        ]);

        done();
      });
    });
  });

  describe('removeApplicationCategory', () => {
    const currentCategoryData = { id: 'someId' };

    beforeEach(() => {
      jest.spyOn(selectors, 'selectCurrentCategory').mockReturnValue(currentCategoryData);
    });

    it('updates category successfully', (done) => {
      mockAxiosCalls({
        del: {
          [getDeleteCategoriesUrl(mockOwnerType, mockOwnerId, currentCategoryData.id)]: Promise.resolve('success'),
        },
      });

      store.dispatch(actions.removeApplicationCategory()).then(() => {
        expect(axios.delete).toHaveBeenCalledTimes(1);
        expect(axios.delete).toHaveBeenCalledWith(
          `/api/v2/applicationCategories/${mockOwnerType}/${mockOwnerId}/${currentCategoryData.id}`
        );

        const actions = store.getActions();

        expect(actions.length).toBe(6);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/createEdit/removeApplicationCategory/pending',
          'applicationCategories/createEdit/resetIsDirty',
          'applicationCategories/createEdit/goToCreateCategory/pending',
          '@@reduxUiRouter/stateGo',
          'applicationCategories/createEdit/goToCreateCategory/fulfilled',
          'applicationCategories/createEdit/removeApplicationCategory/fulfilled',
        ]);

        expect(actions[5].payload).toBe(currentCategoryData.id);

        done();
      });
    });

    it('dispatches rejected action if remove category request failed', (done) => {
      const removeCategorySpy = jest.fn();

      mockAxiosCalls({
        del: {
          [getDeleteCategoriesUrl(mockOwnerType, mockOwnerId, currentCategoryData.id)]: () =>
            Promise.reject('failed to delete category'),
        },
      });

      store.dispatch(actions.removeApplicationCategory()).then(() => {
        expect(axios.delete).toHaveBeenCalledTimes(1);
        expect(axios.delete).toHaveBeenCalledWith(
          `/api/v2/applicationCategories/${mockOwnerType}/${mockOwnerId}/${currentCategoryData.id}`
        );
        expect(removeCategorySpy).not.toHaveBeenCalled();

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/createEdit/removeApplicationCategory/pending',
          'applicationCategories/createEdit/removeApplicationCategory/rejected',
        ]);

        done();
      });
    });
  });
});
