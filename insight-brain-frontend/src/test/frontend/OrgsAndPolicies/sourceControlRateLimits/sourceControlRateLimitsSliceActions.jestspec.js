/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getSourceControlRateLimitsUrl } from 'MainRoot/util/CLMLocation';
import { actions } from 'MainRoot/OrgsAndPolicies/sourceControlRateLimits/sourceControlRateLimitsSlice';
import { omit } from 'ramda';
import { SOURCE_CONTROL_RATE_LIMITS_ORGANIZATION_MOCK_DATA } from 'TestRoot/OrgsAndPolicies/sourceControlRateLimits/sourceControlRateLimitsMockData';

describe('sourceControlRateLimitsSliceActions', () => {
  let axiosMock, store, state;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    state = {
      router: {
        currentParams: {
          ownerType: 'someType',
          ownerId: 'someId',
        },
      },
    };
  });

  describe('load', () => {
    it('immediately dispatches a sourceControlRateLimits/load/pending action and an appropriate request', () => {
      store = SpecUtil.mockReduxStore(state);
      axiosMock.onGet(getSourceControlRateLimitsUrl('someType', 'someId')).reply(200, {});

      store.dispatch(actions.load());

      expect(store.getActions()).toHaveAction({
        type: 'sourceControlRateLimits/load/pending',
      });
      expect(axiosMock.history.get.length).toBe(1);
      expect(axiosMock.history.get[0].url).toBe('/api/experimental/sourceControl/someType/someId/rateLimits');
    });
  });

  it('dispatches a sourceControlRateLimits/load/fulfilled action after a successful request', (done) => {
    store = SpecUtil.mockReduxStore(state);
    const payload = SOURCE_CONTROL_RATE_LIMITS_ORGANIZATION_MOCK_DATA;
    axiosMock.onGet(getSourceControlRateLimitsUrl('someType', 'someId')).reply(200, payload);

    store.dispatch(actions.load()).then(() => {
      const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
      expect(actions).toHaveActionsInOrder([
        {
          type: 'sourceControlRateLimits/load/pending',
        },
        {
          type: 'sourceControlRateLimits/load/fulfilled',
          payload,
        },
      ]);
      done();
    });
  });

  it('dispatches a sourceControlRateLimits/load/rejected action after a failed request', (done) => {
    store = SpecUtil.mockReduxStore(state);
    const payload = 'someError';
    axiosMock.onGet(getSourceControlRateLimitsUrl('someType', 'someId')).reply(500, payload);

    store.dispatch(actions.load()).then(() => {
      const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
      expect(actions).toHaveActionTypesInOrder([
        'sourceControlRateLimits/load/pending',
        'sourceControlRateLimits/load/rejected',
      ]);
      done();
    });
  });

  describe('setSort', () => {
    it('immediately dispatches a sourceControlRateLimits/setSort action', () => {
      store.dispatch(actions.setSort('user'));

      expect(store.getActions()).toHaveAction({
        type: 'sourceControlRateLimits/setSort',
        payload: 'user',
      });
    });
  });

  describe('toggleUserRateLimitsExpanded', () => {
    it('immediately dispatches a sourceControlRateLimits/toggleUserRateLimitsExpanded action', () => {
      store.dispatch(actions.toggleUserRateLimitsExpanded('user'));

      expect(store.getActions()).toHaveAction({
        type: 'sourceControlRateLimits/toggleUserRateLimitsExpanded',
        payload: 'user',
      });
    });
  });

  describe('toggleUserDefiningOwnersExpanded', () => {
    it('immediately dispatches a sourceControlRateLimits/toggleUserDefiningOwnersExpanded action', () => {
      store.dispatch(actions.toggleUserDefiningOwnersExpanded('user'));

      expect(store.getActions()).toHaveAction({
        type: 'sourceControlRateLimits/toggleUserDefiningOwnersExpanded',
        payload: 'user',
      });
    });
  });

  describe('toggleUserAssociatedApplicationsExpanded', () => {
    it('immediately dispatches a sourceControlRateLimits/toggleUserAssociatedApplicationsExpanded action', () => {
      store.dispatch(actions.toggleUserAssociatedApplicationsExpanded('user'));

      expect(store.getActions()).toHaveAction({
        type: 'sourceControlRateLimits/toggleUserAssociatedApplicationsExpanded',
        payload: 'user',
      });
    });
  });
});
