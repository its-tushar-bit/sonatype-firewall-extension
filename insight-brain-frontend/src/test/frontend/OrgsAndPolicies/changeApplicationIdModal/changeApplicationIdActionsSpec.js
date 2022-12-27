/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { getApplicationsUrl } from 'MainRoot/util/CLMLocation';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import * as changeApplicationIdSelectors from 'MainRoot/OrgsAndPolicies/changeApplicationIdModal/changeApplicationIdSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/changeApplicationIdModal/changeApplicationIdSlice';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';

const { initialState: rscInitialState } = nxTextInputStateHelpers;

const OWNER_APP_NAME = 'Application Three Name';

describe('ChangeApplicationIdModal actions', () => {
  let store, state, mock;

  beforeEach(() => {
    jasmine.clock().install();
    mock = axiosMockAdapter();
    state = {
      router: {
        currentParams: {
          applicationPublicId: 'applicationThreePublicID',
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

    spyOn(changeApplicationIdSelectors, 'selectChangeApplicationIdSlice').and.returnValue({
      newPublicId: rscInitialState('newAppPublicID'),
    });
  });

  afterEach(function () {
    jasmine.clock().uninstall();
  });

  it('handles update of appId', (done) => {
    mock.onPut(getApplicationsUrl()).reply(200, {
      data: {
        id: 'applicationThreeID',
        name: OWNER_APP_NAME,
        publicId: 'newAppPublicID',
        organizationId: 'organizationTwoID',
      },
    });

    store.dispatch(actions.changeApplicationId()).then(() => {
      expect(mock.history.put.length).toBe(1);
      expect(mock.history.put[0].url).toBe(getApplicationsUrl());

      const actions = store.getActions();
      expect(actions.length).toBe(5);
      expect(actions).toHaveActionTypesInOrder([
        'ownerActions/changeApplicationId/changeApplicationId/pending',
        'ownerActions/updateApplication/pending',
        'applications/updateApplication',
        'ownerActions/updateApplication/fulfilled',
        'ownerActions/changeApplicationId/changeApplicationId/fulfilled',
      ]);

      jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

      expect(actions.length).toBe(6);
      expect(actions).toHaveActionTypesInOrder([
        'ownerActions/changeApplicationId/changeApplicationId/pending',
        'ownerActions/updateApplication/pending',
        'applications/updateApplication',
        'ownerActions/updateApplication/fulfilled',
        'ownerActions/changeApplicationId/changeApplicationId/fulfilled',
        'ownerActions/changeApplicationId/closeModal',
      ]);

      done();
    });
  });
});
