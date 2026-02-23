/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { getApplicationSummaryUrl, getApplicationsUrl } from 'MainRoot/util/CLMLocation';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import * as changeApplicationIdSelectors from 'MainRoot/OrgsAndPolicies/changeApplicationIdModal/changeApplicationIdSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/changeApplicationIdModal/changeApplicationIdSlice';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';

import 'TestRoot/SpecUtil';
import 'TestRoot/mock.data/sidebar.resource.mock.data';

const { initialState: rscInitialState } = nxTextInputStateHelpers;

const OWNER_APP_NAME = 'Application Three Name';

describe('ChangeApplicationIdModal actions', () => {
  let store, state, mock;

  beforeEach(() => {
    jest.useFakeTimers();
    mock = axiosMockAdapter();
    state = {
      router: {
        currentParams: {
          applicationPublicId: 'applicationThreePublicID',
          name: OWNER_APP_NAME,
        },
        currentState: { name: 'application' },
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

    jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').mockReturnValue({
      id: 'applicationThreeID',
      name: OWNER_APP_NAME,
      publicId: 'applicationThreePublicID',
      organizationId: 'organizationTwoID',
    });
    jest.spyOn(routerSelectors, 'selectIsApplication').mockReturnValue(true);

    jest.spyOn(changeApplicationIdSelectors, 'selectChangeApplicationIdSlice').mockReturnValue({
      newPublicId: rscInitialState('newAppPublicID'),
    });
  });

  afterEach(function () {
    jest.useRealTimers();
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
    mock.onGet(getApplicationsUrl()).reply(200, {
      data: {
        id: 'applicationThreeID',
        name: OWNER_APP_NAME,
        publicId: 'newAppPublicID',
        organizationId: 'organizationTwoID',
      },
    });

    mock.onGet(getApplicationSummaryUrl('applicationThreePublicID')).reply(200, {
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

      expect(actions.length).toBe(8);
      expect(actions).toHaveActionTypesInOrder([
        'ownerActions/changeApplicationId/changeApplicationId/pending',
        'ownerActions/updateOwner/pending',
        'orgsAndPolicies/loadSelectedOwner/pending',
        'orgsAndPolicies/setShowLimitedFirewallAccessAlert',
        'orgsAndPolicies/loadSelectedOwner/fulfilled',
        'ownerActions/updateOwner/fulfilled',
        'ownerSideNav/updateOwnersMapWithNewAppId',
        'ownerActions/changeApplicationId/changeApplicationId/fulfilled',
      ]);

      jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

      expect(actions.length).toBe(9);
      expect(actions).toHaveActionType('ownerActions/changeApplicationId/closeModal');

      done();
    });
  });
});
