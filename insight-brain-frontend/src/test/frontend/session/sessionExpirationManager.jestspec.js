/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import Cookies from 'js-cookie';
import { sessionExpired, checkSessionExpiredLater, setServerDate } from 'MainRoot/session/sessionExpirationManager';
import { render, screen } from 'TestRoot/SpecUtil';
import LogoutWarningModal from 'MainRoot/modals/logoutWarningModal/LogoutWarningModal';
import React from 'react';

describe('sessionExpirationManager', () => {
  let mockXhr, defaultPreloadedState, renderComponent;

  beforeEach(() => {
    window.unloadListener = jest.fn();
    mockXhr = {
      open: jest.fn(),
      send: jest.fn(),
      getResponseHeader: jest.fn(),
      onload: null,
    };
    jest.spyOn(window, 'XMLHttpRequest').mockImplementation(() => mockXhr);

    defaultPreloadedState = {
      logoutWarningModal: {
        open: false,
        secondsLeft: null,
        intervalId: null,
      },
      userSession: {
        data: {
          sessionTimeoutMilliseconds: 30 * 60 * 1000,
        },
        loading: false,
        error: null,
        shouldDisplayPasswordWarning: false,
      },
    };
    const mockNotification = jest.fn();
    mockNotification.permission = 'granted';
    global.Notification = mockNotification;
    renderComponent = (preloadedState) =>
      render(<LogoutWarningModal />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  afterEach(() => {
    delete window.unloadListener;
    jest.restoreAllMocks();
  });

  describe('sessionExpired', () => {
    it('should remove beforeunload event listener', () => {
      const spyRemoveEventListener = jest.spyOn(window, 'removeEventListener');

      sessionExpired();

      expect(spyRemoveEventListener).toHaveBeenCalledWith('beforeunload', window.unloadListener);
    });

    it('should create an XMLHttpRequest and send a DELETE request', () => {
      sessionExpired();

      expect(global.XMLHttpRequest).toHaveBeenCalled();
      expect(mockXhr.open).toHaveBeenCalledWith('DELETE', '/rest/user/session/logout');
      expect(mockXhr.send).toHaveBeenCalled();
    });

    it('should redirect to location from header if present', () => {
      jest.spyOn(window, 'location', 'get').mockReturnValue({
        href: 'http://iq-server',
        reload: jest.fn(),
      });

      sessionExpired();

      mockXhr.getResponseHeader.mockReturnValue('https://logout-url.com');
      mockXhr.onload();
      expect(window.location.href).toBe('https://logout-url.com');
      expect(window.location.reload).not.toHaveBeenCalled();
    });

    it('should reload the page if no location header is present', () => {
      jest.spyOn(window, 'location', 'get').mockReturnValue({
        href: 'http://iq-server',
        reload: jest.fn(),
      });

      sessionExpired();

      mockXhr.getResponseHeader.mockReturnValue(null);
      mockXhr.onload();
      expect(window.location.href).toBe('http://iq-server');
      expect(window.location.reload).toHaveBeenCalled();
    });
  });

  describe('checkSessionExpiredLater', () => {
    beforeEach(() => {
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
      Cookies.remove('IQ-SESSION-EXPIRATION-TIMESTAMP');
    });

    it('should check session expiration periodically and show the logout warning when needed', () => {
      const { store } = renderComponent();
      Cookies.set('IQ-SESSION-EXPIRATION-TIMESTAMP', Date.now() + 5 * 60 * 1000);

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

      checkSessionExpiredLater(store, 'MyAwesomeProduct');

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

      jest.advanceTimersByTime(3 * 60 * 1000);

      expect(screen.getByRole('dialog')).toBeVisible();
      expect(screen.getByText('Session Timeout Warning')).toBeVisible();
      expect(window.Notification).toHaveBeenCalledWith('Session Timeout Warning', {
        body: 'Your MyAwesomeProduct session will expire in 2 minutes due to inactivity.',
      });
    });

    it('should log a warning when cookie is missing', () => {
      const spyConsoleWarn = jest.spyOn(console, 'warn').mockImplementation(() => {});

      checkSessionExpiredLater({ dispatch: jest.fn() }, 'IQ Server');

      expect(spyConsoleWarn).toHaveBeenCalledWith(
        'IQ-SESSION-EXPIRATION-TIMESTAMP cookie is missing. Session timeout detection will be disabled'
      );
    });
  });

  describe('setServerDate', () => {
    beforeEach(() => {
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('should set the server date difference', () => {
      const { store } = renderComponent();
      const date = new Date();
      jest.setSystemTime(date);

      // Server time is 5 minutes behind the client time
      const serverDate = new Date(date.getTime() - 5 * 60 * 1000);
      setServerDate(serverDate);

      // Server thinks the client has 5 minutes left
      // Client would think it has no time left if we didn't adjust the session expiration timestamp
      Cookies.set('IQ-SESSION-EXPIRATION-TIMESTAMP', date.getTime());

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

      checkSessionExpiredLater(store, 'MyAwesomeProduct');

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

      jest.advanceTimersByTime(3 * 60 * 1000);

      expect(screen.getByRole('dialog')).toBeVisible();
      expect(screen.getByText('Session Timeout Warning')).toBeVisible();
      expect(window.Notification).toHaveBeenCalledWith('Session Timeout Warning', {
        body: 'Your MyAwesomeProduct session will expire in 2 minutes due to inactivity.',
      });
    });
  });
});
