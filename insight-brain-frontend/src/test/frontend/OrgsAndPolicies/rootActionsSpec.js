/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  getApplicablePolicies,
  getRepositoryManagerById,
  getApplicationSummaryUrl,
  getOrganizationUrl,
} from 'MainRoot/util/CLMLocation';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/rootSlice';

describe('rootSlice actions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  let store, mockOwnerId, mockOwnerType;

  beforeEach(function () {
    store = SpecUtil.mockReduxStore({});
    mockOwnerId = 'ownerId';
    mockOwnerType = 'ownerType';
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
          'orgsAndPolicies/loadApplicablePoliciesByOwner/pending',
          'orgsAndPolicies/loadApplicablePoliciesByOwner/fulfilled',
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
          'orgsAndPolicies/loadApplicablePoliciesByOwner/pending',
          'orgsAndPolicies/loadApplicablePoliciesByOwner/rejected',
        ]);
        done();
      });
    });
  });

  describe('loadSelectedOwner', () => {
    const organizationId = 'orgId',
      applicationPublicId = 'appId',
      repositoryManagerId = 'repoManagerId';

    let mockState;

    beforeEach(function () {
      mockState = {
        orgsAndPolicies: {
          root: {
            selectedOwner: {},
          },
        },
        router: {
          currentState: {
            name: 'management.view.repository_manager',
          },
          currentParams: {
            organizationId,
            applicationPublicId,
            repositoryManagerId,
          },
        },
      };
      store = SpecUtil.mockReduxStore(mockState);
    });

    it('loads selected repository manager owner successfully', (done) => {
      mockAxiosCalls({
        get: {
          [getRepositoryManagerById(repositoryManagerId)]: Promise.resolve({
            data: 'repo manager info',
          }),
        },
      });

      store.dispatch(actions.loadSelectedOwner()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPolicies/loadSelectedOwner/pending',
          'orgsAndPolicies/loadSelectedOwner/fulfilled',
        ]);

        done();
      });
    });

    it('loads selected application owner successfully', (done) => {
      mockState.router.currentState.name = 'management.view.application';
      store = SpecUtil.mockReduxStore(mockState);
      mockAxiosCalls({
        get: {
          [getApplicationSummaryUrl(applicationPublicId)]: Promise.resolve({
            data: 'application info',
          }),
        },
      });

      store.dispatch(actions.loadSelectedOwner()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPolicies/loadSelectedOwner/pending',
          'orgsAndPolicies/loadSelectedOwner/fulfilled',
        ]);

        done();
      });
    });

    it('loads selected organization owner successfully', (done) => {
      mockState.router.currentState.name = 'management.view.organization';
      store = SpecUtil.mockReduxStore(mockState);
      mockAxiosCalls({
        get: {
          [getOrganizationUrl(organizationId)]: Promise.resolve({
            data: 'org info',
          }),
        },
      });

      store.dispatch(actions.loadSelectedOwner()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPolicies/loadSelectedOwner/pending',
          'orgsAndPolicies/loadSelectedOwner/fulfilled',
        ]);

        done();
      });
    });

    it('dispatches rejected action if loadApplicablePoliciesByOwner request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getRepositoryManagerById(repositoryManagerId)]: () => Promise.reject('something went wrong'),
        },
      });

      store.dispatch(actions.loadSelectedOwner()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPolicies/loadSelectedOwner/pending',
          'orgsAndPolicies/loadSelectedOwner/rejected',
        ]);
        done();
      });
    });
  });
});
