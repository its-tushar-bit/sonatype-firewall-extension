/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/revokeLegacyViolationModal/revokeLegacyViolationModalSlice';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';

import 'TestRoot/SpecUtil';
import 'TestRoot/mock.data/sidebar.resource.mock.data';

const OWNER_ORG_NAME = 'Organization Two Name';
const REDUCER_NAME = `revokeLegacyViolationModal`;

describe('revokeLegacyViolationActions', () => {
  let store, state, axiosMock;

  beforeAll(function () {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(function () {
    jest.useFakeTimers();
  });

  afterEach(function () {
    jest.useRealTimers();
  });

  describe('revoke application', () => {
    beforeEach(() => {
      state = {
        router: {
          currentParams: {
            applicationPublicId: 'organizationTwoID',
            name: OWNER_ORG_NAME,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: 'organizationTwoID',
              name: OWNER_ORG_NAME,
              parentOrganizationId: 'ROOT_ORGANIZATION_ID',
              publicId: 'organizationTwoID',
            },
          },
          organizations: SidebarResourceMockData.getOwnerListUrl(),
        },
      };
      store = SpecUtil.mockReduxStore(state);

      jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').mockReturnValue({
        id: 'organizationTwoID',
        name: OWNER_ORG_NAME,
        parentOrganizationId: 'ROOT_ORGANIZATION_ID',
        publicId: 'organizationTwoID',
      });
    });

    it('handles revoke', (done) => {
      axiosMock.onPut(`/rest/legacyViolations/revoke/organizationTwoID`).reply(200, {});

      store.dispatch(actions.revokeLegacyViolation()).then(() => {
        expect(axiosMock.history.put.length).toBe(1);

        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          `${OWNER_ACTIONS}/${REDUCER_NAME}/revoke/pending`,
          `${OWNER_ACTIONS}/${REDUCER_NAME}/revoke/fulfilled`,
        ]);

        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          `${OWNER_ACTIONS}/${REDUCER_NAME}/revoke/pending`,
          `${OWNER_ACTIONS}/${REDUCER_NAME}/revoke/fulfilled`,
          `${OWNER_ACTIONS}/${REDUCER_NAME}/closeModal`,
        ]);

        done();
      });
    });
  });
});
