/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { configureStore } from '@reduxjs/toolkit';
import containerImageWaiversReducer, {
  actions,
  initialState,
} from 'MainRoot/firewall/waiverRequests/containerImageWaiversSlice';
import { getContainerImageAllRepositoriesWaiversUrl } from 'MainRoot/util/CLMLocation';

const mockWaiver = {
  policyWaiverId: 'b57049bf6e41424ebfa2002e06f955e5',
  scopeOwnerId: 'f63ae7c6a97745cba6f1a99975e47dd1',
  scopeOwnerType: 'application',
  scopeOwnerName: 'my-docker-app',
  createTime: '2025-06-19T10:00:00.000Z',
  expiryTime: null,
  threatLevel: 7,
  policyName: 'Docker Security Policy',
  matcherStrategy: 'ALL_COMPONENTS',
  forContainerImage: true,
};

describe('containerImageWaiversSlice', () => {
  let axiosMock;
  let store;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.reset();
    store = configureStore({
      reducer: { containerImageWaivers: containerImageWaiversReducer },
    });
  });

  describe('initialState', () => {
    it('has the correct initial state', () => {
      expect(initialState).toEqual({
        loading: false,
        error: null,
        waivers: [],
      });
    });
  });

  describe('loadContainerImageWaivers thunk', () => {
    it('sets loading to true while pending', async () => {
      axiosMock.onGet(getContainerImageAllRepositoriesWaiversUrl()).reply(() => new Promise(() => {}));

      store.dispatch(actions.loadContainerImageWaivers());

      expect(store.getState().containerImageWaivers.loading).toBe(true);
      expect(store.getState().containerImageWaivers.error).toBeNull();
    });

    it('stores fetched waivers on success', async () => {
      axiosMock.onGet(getContainerImageAllRepositoriesWaiversUrl()).reply(200, [mockWaiver]);

      await store.dispatch(actions.loadContainerImageWaivers());

      const state = store.getState().containerImageWaivers;
      expect(state.loading).toBe(false);
      expect(state.error).toBeNull();
      expect(state.waivers).toEqual([mockWaiver]);
    });

    it('stores empty array when API returns empty list', async () => {
      axiosMock.onGet(getContainerImageAllRepositoriesWaiversUrl()).reply(200, []);

      await store.dispatch(actions.loadContainerImageWaivers());

      const state = store.getState().containerImageWaivers;
      expect(state.loading).toBe(false);
      expect(state.waivers).toEqual([]);
    });

    it('stores multiple waivers on success', async () => {
      const anotherWaiver = { ...mockWaiver, policyWaiverId: 'aabbccdd11223344556677889900aabb' };
      axiosMock.onGet(getContainerImageAllRepositoriesWaiversUrl()).reply(200, [mockWaiver, anotherWaiver]);

      await store.dispatch(actions.loadContainerImageWaivers());

      expect(store.getState().containerImageWaivers.waivers).toHaveLength(2);
    });

    it('sets error message and clears loading on failure', async () => {
      axiosMock.onGet(getContainerImageAllRepositoriesWaiversUrl()).reply(500, { message: 'Internal Server Error' });

      await store.dispatch(actions.loadContainerImageWaivers());

      const state = store.getState().containerImageWaivers;
      expect(state.loading).toBe(false);
      expect(state.error).toBeTruthy();
      expect(state.waivers).toEqual([]);
    });

    it('sets error message on network failure', async () => {
      axiosMock.onGet(getContainerImageAllRepositoriesWaiversUrl()).networkError();

      await store.dispatch(actions.loadContainerImageWaivers());

      const state = store.getState().containerImageWaivers;
      expect(state.loading).toBe(false);
      expect(state.error).toBeTruthy();
    });

    it('calls the correct URL', async () => {
      axiosMock.onGet(getContainerImageAllRepositoriesWaiversUrl()).reply(200, []);

      await store.dispatch(actions.loadContainerImageWaivers());

      const getRequests = axiosMock.history.get.filter(
        (req) => req.url === getContainerImageAllRepositoriesWaiversUrl()
      );
      expect(getRequests).toHaveLength(1);
    });

    it('on 403 leaves error null and emits FIREWALL_SET_SHOW_LIMITED_FIREWALL_ACCESS_ALERT with true', async () => {
      axiosMock.onGet(getContainerImageAllRepositoriesWaiversUrl()).reply(403, { message: 'Forbidden' });
      const dispatched = [];
      const captureMiddleware = () => (next) => (action) => {
        dispatched.push(action);
        return next(action);
      };
      const localStore = configureStore({
        reducer: { containerImageWaivers: containerImageWaiversReducer },
        middleware: (getDefault) => getDefault().concat(captureMiddleware),
      });

      await localStore.dispatch(actions.loadContainerImageWaivers());

      const state = localStore.getState().containerImageWaivers;
      expect(state.loading).toBe(false);
      expect(state.error).toBeNull();
      expect(state.waivers).toEqual([]);
      expect(
        dispatched.some(
          (a) => a?.type === 'FIREWALL_SET_SHOW_LIMITED_FIREWALL_ACCESS_ALERT' && a.payload === true
        )
      ).toBe(true);
    });

    it('clears previous error on retry', async () => {
      axiosMock.onGet(getContainerImageAllRepositoriesWaiversUrl()).replyOnce(500, { message: 'Error' });
      await store.dispatch(actions.loadContainerImageWaivers());
      expect(store.getState().containerImageWaivers.error).toBeTruthy();

      axiosMock.onGet(getContainerImageAllRepositoriesWaiversUrl()).reply(200, [mockWaiver]);
      await store.dispatch(actions.loadContainerImageWaivers());

      const state = store.getState().containerImageWaivers;
      expect(state.error).toBeNull();
      expect(state.waivers).toEqual([mockWaiver]);
    });
  });
});
