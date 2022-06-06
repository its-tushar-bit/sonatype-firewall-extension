/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getApplicablePolicies } from 'MainRoot/util/CLMLocation';
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
});
