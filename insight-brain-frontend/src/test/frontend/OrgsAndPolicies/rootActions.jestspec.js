/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  getApplicablePolicies,
  getRepositoryManagerById,
  getApplicationSummaryUrl,
  getOrganizationUrl,
  getRepositoryInfoUrl,
} from 'MainRoot/util/CLMLocation';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { axiosMockAdapter, mockInterceptionObserver } from 'TestRoot/SpecUtil';
import { actions } from 'MainRoot/OrgsAndPolicies/rootSlice';

describe('rootSlice actions', () => {
  let store, mockOwnerId, mockOwnerType, axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    mockInterceptionObserver();
  });

  beforeEach(function () {
    store = SpecUtil.mockReduxStore({});
    mockOwnerId = 'ownerId';
    mockOwnerType = 'ownerType';
    jest.spyOn(orgsAndPoliciesSelectors, 'selectOwnerProperties').mockReturnValue({
      ownerId: mockOwnerId,
      ownerType: mockOwnerType,
    });
  });
  describe('loadApplicablePoliciesByOwner', () => {
    it('loads policy tags successfully', (done) => {
      axiosMock.onGet(getApplicablePolicies(mockOwnerType, mockOwnerId)).reply(200, { data: 'content' });

      store.dispatch(actions.loadApplicablePoliciesByOwner()).then(() => {
        expect(axiosMock.history.get.length).toBe(1);

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
      axiosMock.onGet(getApplicablePolicies(mockOwnerId)).reply(500, 'something went wrong');

      store.dispatch(actions.loadApplicablePoliciesByOwner()).then(() => {
        expect(axiosMock.history.get.length).toBe(1);

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
      repositoryId = 'repositoryId',
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
            repositoryId,
          },
        },
      };
      store = SpecUtil.mockReduxStore(mockState);
    });

    it('loads selected repository manager owner successfully', (done) => {
      axiosMock.onGet(getRepositoryManagerById(repositoryManagerId)).reply(200, {
        data: 'repo manager info',
      });

      store.dispatch(actions.loadSelectedOwner()).then(() => {
        expect(axiosMock.history.get.length).toBe(1);

        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPolicies/loadSelectedOwner/pending',
          'orgsAndPolicies/setShowLimitedFirewallAccessAlert',
          'orgsAndPolicies/loadSelectedOwner/fulfilled',
        ]);

        done();
      });
    });

    it('loads selected repository owner successfully', (done) => {
      mockState.router.currentState.name = 'management.view.repository';
      axiosMock.onGet(getRepositoryInfoUrl(repositoryId)).reply(200, {
        data: 'repository info',
      });

      store.dispatch(actions.loadSelectedOwner()).then(() => {
        expect(axiosMock.history.get.length).toBe(1);

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
      axiosMock.onGet(getApplicationSummaryUrl(applicationPublicId)).reply(200, {
        data: 'application info',
      });

      store.dispatch(actions.loadSelectedOwner()).then(() => {
        expect(axiosMock.history.get.length).toBe(1);

        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPolicies/loadSelectedOwner/pending',
          'orgsAndPolicies/setShowLimitedFirewallAccessAlert',
          'orgsAndPolicies/loadSelectedOwner/fulfilled',
        ]);

        done();
      });
    });

    it('loads selected organization owner successfully', (done) => {
      mockState.router.currentState.name = 'management.view.organization';
      store = SpecUtil.mockReduxStore(mockState);
      axiosMock.onGet(getOrganizationUrl(organizationId)).reply(200, {
        data: 'org info',
      });

      store.dispatch(actions.loadSelectedOwner()).then(() => {
        expect(axiosMock.history.get.length).toBe(1);

        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPolicies/loadSelectedOwner/pending',
          'orgsAndPolicies/setShowLimitedFirewallAccessAlert',
          'orgsAndPolicies/loadSelectedOwner/fulfilled',
        ]);

        done();
      });
    });

    it('dispatches rejected action if loadApplicablePoliciesByOwner request fails', (done) => {
      axiosMock.onGet(getRepositoryManagerById(repositoryManagerId)).reply(500, 'something went wrong');

      store.dispatch(actions.loadSelectedOwner()).then(() => {
        expect(axiosMock.history.get.length).toBe(1);

        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'orgsAndPolicies/loadSelectedOwner/pending',
          'orgsAndPolicies/setShowLimitedFirewallAccessAlert',
          'orgsAndPolicies/loadSelectedOwner/rejected',
        ]);
        done();
      });
    });
  });
});
