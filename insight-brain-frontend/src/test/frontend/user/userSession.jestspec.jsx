/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { waitFor } from '@testing-library/react';
import { fetchUser, waitForLogin } from 'MainRoot/user/userSessionUtils';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getSessionUrl } from 'MainRoot/util/CLMLocation';
import { actions } from 'MainRoot/user/userSessionSlice';
import defaultStore from 'MainRoot/reduxConfig/store';

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
});
