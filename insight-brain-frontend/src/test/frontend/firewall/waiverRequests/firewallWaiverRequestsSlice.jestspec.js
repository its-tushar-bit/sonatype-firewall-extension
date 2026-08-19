/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { configureStore } from '@reduxjs/toolkit';
import reducer, { actions, initialState } from 'MainRoot/firewall/waiverRequests/firewallWaiverRequestsSlice';
import { getListPolicyWaiverRequestsUrl, getViewOrUpdatePolicyWaiverRequestUrl } from 'MainRoot/util/CLMLocation';

const LIST_URL = getListPolicyWaiverRequestsUrl('organization', 'ROOT_ORGANIZATION_ID');

function makeStore(preloaded = {}) {
  return configureStore({
    reducer: { firewallWaiverRequests: reducer },
    preloadedState: { firewallWaiverRequests: { ...initialState, ...preloaded } },
  });
}

describe('firewallWaiverRequestsSlice', () => {
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  afterEach(() => {
    axiosMock.reset();
    jest.clearAllMocks();
  });

  it('has expected initial state', () => {
    const store = makeStore();
    expect(store.getState().firewallWaiverRequests).toEqual(initialState);
  });

  describe('loadWaiverRequestForReview', () => {
    const ownerType = 'repository';
    const ownerId = 'npm-central';
    const policyWaiverRequestId = 'req-1';
    const REVIEW_URL = getViewOrUpdatePolicyWaiverRequestUrl(ownerType, ownerId, policyWaiverRequestId);
    const mockWaiverRequest = { id: policyWaiverRequestId, status: 'REQUESTED', policyName: 'Security-Critical' };

    it('sets reviewPage.loading true while fetching', () => {
      axiosMock.onGet(REVIEW_URL).reply(() => new Promise(() => {}));
      const store = makeStore();
      store.dispatch(actions.loadWaiverRequestForReview({ ownerType, ownerId, policyWaiverRequestId }));
      expect(store.getState().firewallWaiverRequests.reviewPage.loading).toBe(true);
    });

    it('sets hasWaivePermission true when the response has canReview=true', async () => {
      axiosMock.onGet(REVIEW_URL).reply(200, { ...mockWaiverRequest, canReview: true });
      const store = makeStore();
      await store.dispatch(actions.loadWaiverRequestForReview({ ownerType, ownerId, policyWaiverRequestId }));
      const { reviewPage } = store.getState().firewallWaiverRequests;
      expect(reviewPage.waiverRequest).toEqual({ ...mockWaiverRequest, canReview: true });
      expect(reviewPage.hasWaivePermission).toBe(true);
      expect(reviewPage.loading).toBe(false);
    });

    it('sets hasWaivePermission false when the response has canReview=false', async () => {
      axiosMock.onGet(REVIEW_URL).reply(200, { ...mockWaiverRequest, canReview: false });
      const store = makeStore();
      await store.dispatch(actions.loadWaiverRequestForReview({ ownerType, ownerId, policyWaiverRequestId }));
      const { reviewPage } = store.getState().firewallWaiverRequests;
      expect(reviewPage.hasWaivePermission).toBe(false);
    });

    it('sets hasWaivePermission false when canReview is missing from the response', async () => {
      axiosMock.onGet(REVIEW_URL).reply(200, mockWaiverRequest);
      const store = makeStore();
      await store.dispatch(actions.loadWaiverRequestForReview({ ownerType, ownerId, policyWaiverRequestId }));
      const { reviewPage } = store.getState().firewallWaiverRequests;
      expect(reviewPage.hasWaivePermission).toBe(false);
    });

    it('sets reviewPage.error on fetch failure', async () => {
      axiosMock.onGet(REVIEW_URL).reply(500, { message: 'Server error' });
      const store = makeStore();
      await store.dispatch(actions.loadWaiverRequestForReview({ ownerType, ownerId, policyWaiverRequestId }));
      const { reviewPage } = store.getState().firewallWaiverRequests;
      expect(reviewPage.loading).toBe(false);
      expect(reviewPage.error).toBeTruthy();
    });
  });

  describe('loadWaiverRequests', () => {
    it('sets loading true while fetching', () => {
      axiosMock.onGet(LIST_URL).reply(() => new Promise(() => {}));
      const store = makeStore();
      store.dispatch(actions.loadWaiverRequests());
      expect(store.getState().firewallWaiverRequests.loading).toBe(true);
    });

    it('populates waiverRequests on success', async () => {
      const mockRequests = [
        {
          policyWaiverRequestId: 'req-1',
          scopeOwnerType: 'repository',
          scopeOwnerId: 'npm-central',
          requesterName: 'john.doe',
          policyName: 'Security-Critical',
          threatLevel: 10,
          comment: 'Unblock CI pipeline',
          requestTime: '2026-05-24T10:00:00Z',
          status: 'REQUESTED',
        },
      ];
      axiosMock.onGet(LIST_URL).reply(200, mockRequests);
      const store = makeStore();
      await store.dispatch(actions.loadWaiverRequests());
      const state = store.getState().firewallWaiverRequests;
      expect(state.loading).toBe(false);
      expect(state.waiverRequests).toEqual(mockRequests);
      expect(state.error).toBeNull();
    });

    it('calls GET on the list endpoint', async () => {
      axiosMock.onGet(LIST_URL).reply(200, []);
      const store = makeStore();
      await store.dispatch(actions.loadWaiverRequests());
      expect(axiosMock.history.get.length).toBe(1);
      expect(axiosMock.history.get[0].url).toBe(LIST_URL);
    });

    it('sets error on failure', async () => {
      axiosMock.onGet(LIST_URL).reply(500, { message: 'Server error' });
      const store = makeStore();
      await store.dispatch(actions.loadWaiverRequests());
      const state = store.getState().firewallWaiverRequests;
      expect(state.loading).toBe(false);
      expect(state.error).toBeTruthy();
    });

    it('on 403 leaves error null and emits FIREWALL_SET_SHOW_LIMITED_FIREWALL_ACCESS_ALERT with true', async () => {
      axiosMock.onGet(LIST_URL).reply(403, { message: 'Forbidden' });
      const dispatched = [];
      const captureMiddleware = () => (next) => (action) => {
        dispatched.push(action);
        return next(action);
      };
      const store = configureStore({
        reducer: { firewallWaiverRequests: reducer },
        preloadedState: { firewallWaiverRequests: { ...initialState } },
        middleware: (getDefault) => getDefault().concat(captureMiddleware),
      });
      await store.dispatch(actions.loadWaiverRequests());
      const state = store.getState().firewallWaiverRequests;
      expect(state.loading).toBe(false);
      expect(state.error).toBeNull();
      expect(
        dispatched.some((a) => a?.type === 'FIREWALL_SET_SHOW_LIMITED_FIREWALL_ACCESS_ALERT' && a.payload === true)
      ).toBe(true);
    });
  });
});
