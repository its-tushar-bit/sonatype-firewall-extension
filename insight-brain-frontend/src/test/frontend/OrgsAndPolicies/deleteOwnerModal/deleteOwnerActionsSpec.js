/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getOrganizationsUrl, getApplicationsUrl } from 'MainRoot/util/CLMLocation';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/deleteOwnerModal/deleteOwnerSlice';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

const OWNER_ORG_NAME = 'Organization Two Name';
const OWNER_APP_NAME = 'Application Three Name';

describe('deleteOwnerActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state;

  beforeEach(function () {
    jasmine.clock().install();
  });

  afterEach(function () {
    jasmine.clock().uninstall();
  });

  describe('removeOwner organization', () => {
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
            },
          },
          organizations: SidebarResourceMockData.getOwnerListUrl(),
          ownerSideNav: {
            ownersMap: {
              organizationTwoID: {
                id: 'organizationTwoID',
                name: OWNER_ORG_NAME,
                parentOrganizationId: 'ROOT_ORGANIZATION_ID',
                applicationIds: ['application-one', 'application-two'],
                organizationIds: ['childOrgOne', 'childOrgTwo', 'childOrgThree'],
              },
              app1: { id: 'app-one-id', publicId: 'application-one', name: 'Application one' },
              app2: { id: 'app-two-id', publicId: 'application-two', name: 'Application two' },
              childOrgOne: { id: 'childOrgOne' },
              childOrgTwo: { id: 'childOrgTwo' },
              childOrgThree: { id: 'childOrgThree' },
            },
          },
        },
      };
      store = SpecUtil.mockReduxStore(state);

      spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').and.returnValue({
        id: 'organizationTwoID',
        name: OWNER_ORG_NAME,
        parentOrganizationId: 'ROOT_ORGANIZATION_ID',
      });
      spyOn(routerSelectors, 'selectIsApplication').and.returnValue(false);
    });

    it('handles remove', (done) => {
      mockAxiosCalls({
        del: {
          [`${getOrganizationsUrl()}/organizationTwoID`]: Promise.resolve(),
        },
      });

      store.dispatch(actions.removeOwner()).then(() => {
        expect(axios.delete).toHaveBeenCalledTimes(1);

        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'ownerActions/delete/removeOwner/pending',
          'ownerActions/delete/removeOwner/fulfilled',
        ]);

        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'ownerActions/delete/removeOwner/pending',
          'ownerActions/delete/removeOwner/fulfilled',
          'ownerActions/delete/closeModal',
        ]);

        done();
      });
    });
  });

  describe('removeOwner application', () => {
    beforeEach(() => {
      state = {
        router: {
          currentParams: {
            applicationPublicId: 'applicationThreeID',
            name: OWNER_APP_NAME,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: 'applicationThreeID',
              publicId: 'applicationThreePublicID',
              organizationId: 'organizationTwoID',
              name: OWNER_APP_NAME,
            },
          },
          organizations: SidebarResourceMockData.getOwnerListUrl(),
        },
      };
      store = SpecUtil.mockReduxStore(state);

      spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').and.returnValue({
        id: 'applicationThreeID',
        name: OWNER_APP_NAME,
        publicId: 'applicationThreePublicID',
        organizationId: 'organizationTwoID',
      });
      spyOn(routerSelectors, 'selectIsApplication').and.returnValue(true);
    });

    it('handles remove', (done) => {
      mockAxiosCalls({
        del: {
          [`${getApplicationsUrl()}/applicationThreePublicID`]: Promise.resolve(),
        },
      });

      store.dispatch(actions.removeOwner()).then(() => {
        expect(axios.delete).toHaveBeenCalledTimes(1);

        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'ownerActions/delete/removeOwner/pending',
          'ownerActions/delete/removeOwner/fulfilled',
        ]);

        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'ownerActions/delete/removeOwner/pending',
          'ownerActions/delete/removeOwner/fulfilled',
          'ownerActions/delete/closeModal',
        ]);

        done();
      });
    });
  });
});
