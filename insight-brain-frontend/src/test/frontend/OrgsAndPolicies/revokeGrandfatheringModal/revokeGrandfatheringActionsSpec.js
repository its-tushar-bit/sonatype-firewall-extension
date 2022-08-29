/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/revokeGrandfatheringModal/revokeGrandfatheringSlice';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { OWNER_EDITOR } from 'MainRoot/OrgsAndPolicies/utility/constants';

const OWNER_ORG_NAME = 'Organization Two Name';

describe('revokeGrandfatheringActions', () => {
  let store, state, axiosMock;

  beforeAll(function () {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(function () {
    jasmine.clock().install();
  });

  afterEach(function () {
    jasmine.clock().uninstall();
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

      spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').and.returnValue({
        id: 'organizationTwoID',
        name: OWNER_ORG_NAME,
        parentOrganizationId: 'ROOT_ORGANIZATION_ID',
        publicId: 'organizationTwoID',
      });
    });

    it('handles revoke', (done) => {
      axiosMock.onPut(`/rest/policyViolationGrandfathering/revoke/organizationTwoID`).reply(200, {});

      store.dispatch(actions.revokeGrandfathering()).then(() => {
        expect(axiosMock.history.put.length).toBe(1);

        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          `${OWNER_EDITOR}/revokeGrandfathering/revoke/pending`,
          `${OWNER_EDITOR}/revokeGrandfathering/revoke/fulfilled`,
        ]);

        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          `${OWNER_EDITOR}/revokeGrandfathering/revoke/pending`,
          `${OWNER_EDITOR}/revokeGrandfathering/revoke/fulfilled`,
          `${OWNER_EDITOR}/revokeGrandfathering/closeModal`,
        ]);

        done();
      });
    });
  });
});
