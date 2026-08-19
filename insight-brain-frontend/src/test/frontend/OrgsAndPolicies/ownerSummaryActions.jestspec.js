/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { actions } from 'MainRoot/OrgsAndPolicies/ownerSummarySlice';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { axiosMockAdapter, mockInterceptionObserver } from 'TestRoot/SpecUtil';
import { getPermissionContextTestUrl } from 'MainRoot/util/CLMLocation';

describe('ownerSummarySlice actions', () => {
  let store, mockOwnerId, mockOwnerType, axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    mockInterceptionObserver();
  });

  beforeEach(function () {
    store = SpecUtil.mockReduxStore({});
    mockOwnerType = 'ownerType';
    mockOwnerId = 'ownerId';
    jest.spyOn(routerSelectors, 'selectOwnerInfo').mockReturnValue({
      ownerType: mockOwnerType,
    });
    jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').mockReturnValue({
      id: mockOwnerId,
    });
  });

  describe('checkEditIqPermission', () => {
    it('dispatches fulfilled action if the permission check passes', (done) => {
      axiosMock.onPut(getPermissionContextTestUrl(mockOwnerType, mockOwnerId)).reply(200, ['WRITE']);

      store.dispatch(actions.checkEditIqPermission()).then(() => {
        expect(axiosMock.history.put.length).toBe(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'ownerSummary/checkEditIqPermission/pending',
          'ownerSummary/checkEditIqPermission/fulfilled',
        ]);
        done();
      });
    });

    it('dispatches fulfilled action if the permission check passes for repository', (done) => {
      jest.spyOn(routerSelectors, 'selectOwnerInfo').mockReturnValue({
        ownerType: 'repository',
      });

      axiosMock.onPut(getPermissionContextTestUrl('repository', mockOwnerId)).reply(200, ['WRITE']);

      store.dispatch(actions.checkEditIqPermission()).then(() => {
        expect(axiosMock.history.put.length).toBe(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'ownerSummary/checkEditIqPermission/pending',
          'ownerSummary/checkEditIqPermission/fulfilled',
        ]);
        done();
      });
    });

    it('dispatches fulfilled action if the permission check passes for repository container', (done) => {
      jest.spyOn(routerSelectors, 'selectOwnerInfo').mockReturnValue({
        ownerType: 'repository_container',
      });

      axiosMock.onPut(getPermissionContextTestUrl('repository_container')).reply(200, ['WRITE']);

      store.dispatch(actions.checkEditIqPermission()).then(() => {
        expect(axiosMock.history.put.length).toBe(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'ownerSummary/checkEditIqPermission/pending',
          'ownerSummary/checkEditIqPermission/fulfilled',
        ]);
        done();
      });
    });

    it('dispatches rejected action if the permission check fails', (done) => {
      axiosMock.onPut(getPermissionContextTestUrl(mockOwnerType, mockOwnerId)).reply(500, 'something went wrong');

      store.dispatch(actions.checkEditIqPermission()).then(() => {
        expect(axiosMock.history.put.length).toBe(1);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'ownerSummary/checkEditIqPermission/pending',
          'ownerSummary/checkEditIqPermission/rejected',
        ]);
        done();
      });
    });
  });
});
