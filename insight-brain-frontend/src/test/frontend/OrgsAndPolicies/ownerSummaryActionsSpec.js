/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { actions } from 'MainRoot/OrgsAndPolicies/ownerSummarySlice';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { getPermissionContextTestUrl } from 'MainRoot/util/CLMLocation';

describe('ownerSummarySlice actions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, mockOwnerId, mockOwnerType;

  beforeEach(function () {
    store = SpecUtil.mockReduxStore({});
    mockOwnerType = 'ownerType';
    mockOwnerId = 'ownerId';
    spyOn(routerSelectors, 'selectOwnerInfo').and.returnValue({
      ownerType: mockOwnerType,
    });
    spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').and.returnValue({
      id: mockOwnerId,
    });
  });

  describe('checkEditIqPermission', () => {
    it('dispatches fulfilled action if the permission check passes', (done) => {
      mockAxiosCalls({
        put: {
          [getPermissionContextTestUrl(mockOwnerType, mockOwnerId)]: Promise.resolve({ data: ['WRITE'] }),
        },
      });

      store.dispatch(actions.checkEditIqPermission()).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);

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
      mockAxiosCalls({
        put: {
          [getPermissionContextTestUrl(mockOwnerType, mockOwnerId)]: Promise.reject('something went wrong'),
        },
      });

      store.dispatch(actions.checkEditIqPermission()).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);

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
