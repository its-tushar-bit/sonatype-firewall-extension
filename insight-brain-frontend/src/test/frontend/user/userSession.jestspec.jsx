/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { waitFor } from '@testing-library/react';
import { fetchUser, waitForLogin, _resetForTest } from 'MainRoot/user/userSessionUtils';
import { axiosMockAdapter, configureStore } from 'TestRoot/SpecUtil';
import { getSessionUrl } from 'MainRoot/util/CLMLocation';
import reducers from 'MainRoot/reduxConfig/reducers';

describe('userSessionUtils', () => {
  let axiosMock, store;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    // Create a new Redux store for each test
    store = configureStore({ reducer: reducers });
  });

  afterEach(() => {
    _resetForTest(store);
  });

  describe('fetchUser', () => {
    it('resolves the promise with user data when the request succeeds', async () => {
      const mockUserData = { username: 'myname' };
      axiosMock.onGet(getSessionUrl()).reply(200, mockUserData);

      fetchUser(store);

      const userData = await waitForLogin(store);
      expect(userData).toEqual(mockUserData);
    });

    it('sends the request with waitForLogin true', async () => {
      const mockUserData = { username: 'myname' };
      axiosMock.onGet(getSessionUrl()).reply(200, mockUserData);

      fetchUser(store);

      const request = axiosMock.history.get[0];
      expect(request.waitForLogin).toBeTruthy();
    });

    it('sends the request with waitForLogin false', async () => {
      const mockUserData = { username: 'myname' };
      axiosMock.onGet(getSessionUrl()).reply(200, mockUserData);

      fetchUser(store, false);

      const request = axiosMock.history.get[0];
      expect(request.waitForLogin).toBeFalsy();
    });

    it('does not reject the promise when a 401 error occurs', async () => {
      axiosMock.onGet(getSessionUrl()).replyOnce(401).onGet(getSessionUrl()).reply(200, { username: 'myname' });
      let isPending = true;
      waitForLogin(store)
        .then(() => {
          isPending = false;
        })
        .catch(() => {
          isPending = false;
        });

      fetchUser(store);

      await waitFor(() => {
        expect(axiosMock.history.get.length).toEqual(1);
      });

      expect(isPending).toBeTruthy();

      fetchUser(store);

      await waitForLogin(store);

      expect(isPending).toBeFalsy();
    });

    it('rejects the promise when a non-401 error occurs', async () => {
      axiosMock.onGet(getSessionUrl()).reply(500);

      fetchUser(store);

      try {
        await waitForLogin(store);
        fail('Promise should have been rejected');
      } catch (e) {
        expect(e.status).toEqual(500);
      }
    });
  });
});
