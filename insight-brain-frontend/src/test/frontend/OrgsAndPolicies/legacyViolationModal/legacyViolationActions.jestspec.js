/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/legacyViolationModal/legacyViolationModalSlice';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { OWNER_ACTIONS } from 'MainRoot/OrgsAndPolicies/utility/constants';

import 'TestRoot/SpecUtil';
import 'TestRoot/mock.data/sidebar.resource.mock.data';

const OWNER_ORG_NAME = 'Organization One';
const REDUCER_NAME = `${OWNER_ACTIONS}/legacyViolationModal`;

describe('legacyViolationActions', () => {
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

  describe('legacy violations for applications', () => {
    beforeEach(() => {
      state = {
        router: {
          currentParams: {
            applicationPublicId: 'organizationOneID',
            name: OWNER_ORG_NAME,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: 'organizationOneID',
              name: OWNER_ORG_NAME,
              parentOrganizationId: 'ROOT_ORGANIZATION_ID',
              publicId: 'organizationOneID',
            },
          },
          organizations: SidebarResourceMockData.getOwnerListUrl(),
        },
      };
      store = SpecUtil.mockReduxStore(state);

      jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').mockReturnValue({
        id: 'organizationOneID',
        name: OWNER_ORG_NAME,
        parentOrganizationId: 'ROOT_ORGANIZATION_ID',
        publicId: 'organizationOneID',
      });
    });

    it('handles legacy violations', (done) => {
      axiosMock.onPut(`/rest/legacyViolations/grant/organizationOneID`).reply(200, {});

      store.dispatch(actions.legacyViolation()).then(() => {
        expect(axiosMock.history.put.length).toBe(1);

        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([`${REDUCER_NAME}/pending`, `${REDUCER_NAME}/fulfilled`]);

        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          `${REDUCER_NAME}/pending`,
          `${REDUCER_NAME}/fulfilled`,
          `${REDUCER_NAME}/closeModal`,
        ]);

        done();
      });
    });
  });
});
