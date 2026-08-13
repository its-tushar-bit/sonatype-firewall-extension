/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { waitFor } from '@testing-library/react';
import { fetchUser, waitForLogin } from 'MainRoot/user/userSessionUtils';
import { axiosMockAdapter, configureStore } from 'TestRoot/SpecUtil';
import {
  getSessionUrl,
  getSessionLogoutUrl,
  getProductFeaturesUrl,
  getShouldDisplayDefaultPasswordWarning,
} from 'MainRoot/util/CLMLocation';
import { getGlobalPermissionTestUrl } from 'MainRoot/util/CLMContextLocation';
import { actions } from 'MainRoot/user/userSessionSlice';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import defaultStore from 'MainRoot/reduxConfig/store';
import reducers from 'MainRoot/reduxConfig/reducers';
import { recentSearchesStorageKey } from 'MainRoot/nosc/search/useRecentSearches';

// The pendo singleton is only built during real app init, so the logout thunk's flush
// call has nothing to invoke under jsdom. Stub the module so logout runs end to end.
jest.mock('MainRoot/pendo/mainBundlePendoService', () => ({
  __esModule: true,
  default: { flush: () => Promise.resolve() },
}));

// submitTelemetryData uses a raw XMLHttpRequest, which axios-mock-adapter does not
// intercept and jsdom cannot service. telemetryUtils exports nothing else.
jest.mock('MainRoot/util/telemetryUtils', () => ({
  __esModule: true,
  submitTelemetryData: jest.fn(),
}));

describe('userSessionUtils', () => {
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  afterEach(() => {
    defaultStore.dispatch(actions.resetUserSession());
  });

  describe('fetchUser', () => {
    it('resolves the promise with user data when the request succeeds', async () => {
      const mockUserData = { username: 'myname' };
      axiosMock.onGet(getSessionUrl()).reply(200, mockUserData);

      fetchUser();

      const userData = await waitForLogin();
      expect(userData).toEqual(mockUserData);
    });

    it('sends the request with waitForLogin true', async () => {
      const mockUserData = { username: 'myname' };
      axiosMock.onGet(getSessionUrl()).reply(200, mockUserData);

      fetchUser();

      const request = axiosMock.history.get[0];
      expect(request.waitForLogin).toBeTruthy();
    });

    it('sends the request with waitForLogin false', async () => {
      const mockUserData = { username: 'myname' };
      axiosMock.onGet(getSessionUrl()).reply(200, mockUserData);

      fetchUser(false);

      const request = axiosMock.history.get[0];
      expect(request.waitForLogin).toBeFalsy();
    });

    it('does not reject the promise when a 401 error occurs', async () => {
      axiosMock.onGet(getSessionUrl()).replyOnce(401).onGet(getSessionUrl()).reply(200, { username: 'myname' });
      let isPending = true;
      waitForLogin()
        .then(() => {
          isPending = false;
        })
        .catch(() => {
          isPending = false;
        });

      fetchUser();

      await waitFor(() => {
        expect(axiosMock.history.get.length).toEqual(1);
      });

      expect(isPending).toBeTruthy();

      fetchUser();

      await waitForLogin();

      expect(isPending).toBeFalsy();
    });

    it('rejects the promise when a non-401 error occurs', async () => {
      axiosMock.onGet(getSessionUrl()).reply(500);

      fetchUser();

      await expect(waitForLogin()).rejects.toMatchObject({ status: 500 });
    });
  });

  describe('return-to-Guide', () => {
    let assignSpy;
    let originalLocation;

    beforeEach(() => {
      sessionStorage.clear();
      originalLocation = window.location;
      assignSpy = jest.fn();
      Object.defineProperty(window, 'location', {
        value: { ...originalLocation, origin: 'http://localhost', assign: assignSpy },
        writable: true,
        configurable: true,
      });
    });

    afterEach(() => {
      sessionStorage.clear();
      Object.defineProperty(window, 'location', {
        value: originalLocation,
        writable: true,
        configurable: true,
      });
    });

    it('redirects to the captured Guide URL after a successful authenticated session fetch', async () => {
      sessionStorage.setItem('iqGuideReturnTo', 'http://localhost/assets/guide/index.html#/components');
      axiosMock.onGet(getSessionUrl()).reply(200, { username: 'admin' });

      fetchUser();

      await waitFor(() => {
        expect(assignSpy).toHaveBeenCalledWith('http://localhost/assets/guide/index.html#/components');
      });
      expect(sessionStorage.getItem('iqGuideReturnTo')).toBeNull();
    });

    it('does not redirect when sessionStorage entry is missing', async () => {
      axiosMock.onGet(getSessionUrl()).reply(200, { username: 'admin' });

      fetchUser();

      await waitForLogin();
      expect(assignSpy).not.toHaveBeenCalled();
    });

    it('does not redirect when payload has no username', async () => {
      sessionStorage.setItem('iqGuideReturnTo', 'http://localhost/assets/guide/index.html#/components');
      axiosMock.onGet(getSessionUrl()).reply(200, { username: null });

      fetchUser();

      // Give the thunk a tick to resolve.
      await new Promise((resolve) => setTimeout(resolve, 0));

      expect(assignSpy).not.toHaveBeenCalled();
      // Entry remains untouched so a subsequent successful fetch can consume it.
      expect(sessionStorage.getItem('iqGuideReturnTo')).toBe('http://localhost/assets/guide/index.html#/components');
    });

    it('clears entry without redirect when stored value fails validation', async () => {
      sessionStorage.setItem('iqGuideReturnTo', 'https://evil.example.com/assets/guide/index.html');
      axiosMock.onGet(getSessionUrl()).reply(200, { username: 'admin' });

      fetchUser();

      await waitForLogin();
      expect(assignSpy).not.toHaveBeenCalled();
      expect(sessionStorage.getItem('iqGuideReturnTo')).toBeNull();
    });
  });

  describe('logout', () => {
    it('clears stored global-search history so it does not carry into the next session', async () => {
      // Search terms name real applications and vulnerabilities, so they must not survive
      // logout on a shared browser profile.
      const adaKey = recentSearchesStorageKey('ada');
      const graceKey = recentSearchesStorageKey('grace');
      window.localStorage.setItem(adaKey, JSON.stringify([{ q: 'acme-payments-service', ts: 1 }]));
      window.localStorage.setItem(graceKey, JSON.stringify([{ q: 'CVE-2021-44228', ts: 1 }]));
      axiosMock.onDelete(getSessionLogoutUrl()).reply(200, null, {});

      await defaultStore.dispatch(actions.logout());

      expect(window.localStorage.getItem(adaKey)).toBeNull();
      expect(window.localStorage.getItem(graceKey)).toBeNull();
    });
  });

  describe('fetchPasswordWarning', () => {
    // A fresh store per test: the product-features map must differ between cases, and
    // axios-mock-adapter matches handlers in registration order, so the
    // /rest/product/features mock has to be registered per test rather than shared.
    function storeWithFeatures(features) {
      axiosMock.onGet(getProductFeaturesUrl()).reply(200, features);
      axiosMock.onPut(getGlobalPermissionTestUrl()).reply(200, ['CONFIGURE_SYSTEM']);
      axiosMock.onGet(getShouldDisplayDefaultPasswordWarning()).reply(200, true);
      return configureStore({ reducer: reducers });
    }

    it('does not request the password warning when user management pages are unsupported', async () => {
      const store = storeWithFeatures([]);

      await store.dispatch(actions.fetchPasswordWarning());

      expect(axiosMock.history.get.map(({ url }) => url)).not.toContain(getShouldDisplayDefaultPasswordWarning());
      expect(axiosMock.history.put).toEqual([]);
      expect(store.getState().userSession.shouldDisplayPasswordWarning).toBe(false);
    });

    it('requests the password warning when user management pages are supported', async () => {
      const store = storeWithFeatures(['user-management-pages']);

      await store.dispatch(actions.fetchPasswordWarning());

      expect(axiosMock.history.get.map(({ url }) => url)).toContain(getShouldDisplayDefaultPasswordWarning());
      expect(store.getState().userSession.shouldDisplayPasswordWarning).toBe(true);
    });

    it('leaves the product-features slice untouched when the map is already populated', async () => {
      const store = storeWithFeatures(['user-management-pages']);
      // Populate the map the way the bootstrap does, then forget the resulting requests.
      await store.dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded());
      axiosMock.resetHistory();

      const featuresLoadingSeen = [];
      const unsubscribe = store.subscribe(() => featuresLoadingSeen.push(store.getState().productFeatures.loading));
      await store.dispatch(actions.fetchPasswordWarning());
      unsubscribe();

      // No repeat GET, and `loading` never flips: consumers gated on it blank themselves
      // while it is true (IqSidebarNav renders DefaultEmptyIqSidebar), so a gate that only
      // reads a feature flag must not touch it.
      expect(axiosMock.history.get.map(({ url }) => url)).not.toContain(getProductFeaturesUrl());
      expect(featuresLoadingSeen).not.toContain(true);
    });

    it('adds no loading transition when it races an in-flight bootstrap features fetch', async () => {
      // The nexus-one bootstrap dispatches fetchProductFeaturesIfNeeded and fetchUser in the
      // same tick, so the session can resolve while the features GET is still in flight. The
      // map is empty at that point, so loadProductFeaturesOnce dispatches a second
      // fetchProductFeaturesIfNeeded. That costs a redundant GET, but must not add a `loading`
      // transition: IqSidebarNav renders DefaultEmptyIqSidebar while the flag is true.
      //
      // Registered here rather than via storeWithFeatures because the features response has to
      // stay pending until both callers have tested the map.
      let releaseFeatures;
      const featuresResponse = new Promise((resolve) => {
        releaseFeatures = () => resolve([200, ['user-management-pages']]);
      });
      axiosMock.onGet(getProductFeaturesUrl()).reply(() => featuresResponse);
      axiosMock.onPut(getGlobalPermissionTestUrl()).reply(200, ['CONFIGURE_SYSTEM']);
      axiosMock.onGet(getShouldDisplayDefaultPasswordWarning()).reply(200, true);
      const store = configureStore({ reducer: reducers });

      // Only value changes are recorded: a pending action rewriting `true` over `true` is
      // invisible to consumers, since useSelector bails on an unchanged value.
      const featuresLoadingSeen = [];
      const unsubscribe = store.subscribe(() => {
        const { loading } = store.getState().productFeatures;
        if (featuresLoadingSeen[featuresLoadingSeen.length - 1] !== loading) {
          featuresLoadingSeen.push(loading);
        }
      });

      // Neither is awaited, so both test the map while it is still empty.
      const bootstrap = store.dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded());
      const passwordWarning = store.dispatch(actions.fetchPasswordWarning());

      releaseFeatures();
      await Promise.all([bootstrap, passwordWarning]);
      unsubscribe();

      // Both callers did observe an empty map and fetch, so the interleaving under test was
      // actually reached — without this the assertion below would hold vacuously.
      expect(axiosMock.history.get.filter(({ url }) => url === getProductFeaturesUrl())).toHaveLength(2);
      expect(featuresLoadingSeen).toEqual([true, false]);
    });
  });
});
