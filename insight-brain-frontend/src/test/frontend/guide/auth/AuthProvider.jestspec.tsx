/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor, act } from '@testing-library/react';
import { AuthProvider, useAuth } from 'GuideRoot/auth/AuthProvider';
import * as loginApi from 'GuideRoot/auth/loginApi';
import type { SessionResponse } from 'GuideRoot/auth/loginApi';
import * as guideReturnTo from 'GuideRoot/auth/guideReturnTo';

function mockWindowLocation(pathname = '/') {
  const assignSpy = jest.fn();
  Object.defineProperty(window, 'location', {
    value: {
      ...window.location,
      pathname,
      assign: assignSpy,
      origin: 'http://localhost',
      href: `http://localhost${pathname}`,
    },
    writable: true,
    configurable: true,
  });
  return assignSpy;
}

function AuthConsumer() {
  const { status, user, authFetch } = useAuth();
  return (
    <div>
      <span data-testid="status">{status}</span>
      <span data-testid="username">{user?.username ?? 'none'}</span>
      <button onClick={() => authFetch('/api/test')}>FetchData</button>
    </div>
  );
}

const authenticatedSession: SessionResponse = {
  authenticated: true,
  user: { username: 'admin', displayName: 'Administrator', groups: ['Administrators'] },
  sessionTimeoutMs: 1800000,
};

const unauthenticatedSession: SessionResponse = {
  authenticated: false,
  user: null,
  sessionTimeoutMs: null,
};

describe('AuthProvider', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('starts in loading state, then transitions to authenticated', async () => {
    jest.spyOn(loginApi, 'fetchSession').mockResolvedValue(authenticatedSession);
    mockWindowLocation('/');

    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>
    );

    expect(screen.getByTestId('status')).toHaveTextContent('loading');

    await waitFor(() => {
      expect(screen.getByTestId('status')).toHaveTextContent('authenticated');
    });
    expect(screen.getByTestId('username')).toHaveTextContent('admin');
  });

  it('captures return-to-Guide and redirects to / when unauthenticated', async () => {
    jest.spyOn(loginApi, 'fetchSession').mockResolvedValue(unauthenticatedSession);
    const captureSpy = jest.spyOn(guideReturnTo, 'captureGuideReturnTo').mockImplementation(() => {});
    const assignSpy = mockWindowLocation('/assets/guide/index.html');

    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated');
    });

    await waitFor(() => {
      expect(captureSpy).toHaveBeenCalledTimes(1);
      expect(assignSpy).toHaveBeenCalledWith('/');
    });
  });

  it('authFetch re-checks session on 401 and triggers capture+redirect', async () => {
    jest.spyOn(loginApi, 'fetchSession')
      .mockResolvedValueOnce(authenticatedSession)
      .mockResolvedValueOnce(unauthenticatedSession);
    const captureSpy = jest.spyOn(guideReturnTo, 'captureGuideReturnTo').mockImplementation(() => {});
    const assignSpy = mockWindowLocation('/assets/guide/index.html');
    const originalFetch = global.fetch;
    global.fetch = jest.fn().mockResolvedValue({ status: 401, ok: false });

    try {
      render(
        <AuthProvider>
          <AuthConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('status')).toHaveTextContent('authenticated');
      });

      await act(async () => {
        screen.getByRole('button', { name: 'FetchData' }).click();
      });

      await waitFor(() => {
        expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated');
      });

      await waitFor(() => {
        expect(captureSpy).toHaveBeenCalled();
        expect(assignSpy).toHaveBeenCalledWith('/');
      });
    } finally {
      global.fetch = originalFetch;
    }
  });

  it('redirects to IdP SLO URL on logout when backend returns Location header (no capture)', async () => {
    jest.spyOn(loginApi, 'fetchSession').mockResolvedValue(authenticatedSession);
    const captureSpy = jest.spyOn(guideReturnTo, 'captureGuideReturnTo').mockImplementation(() => {});
    const assignSpy = mockWindowLocation('/assets/guide/index.html');
    const originalFetch = global.fetch;
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      status: 204,
      headers: new Headers({ Location: 'https://idp.example.com/logout' }),
    });

    Object.defineProperty(document, 'cookie', {
      value: `IQ-SESSION-EXPIRATION-TIMESTAMP=${Date.now() + 60_000}`,
      writable: true,
      configurable: true,
    });

    try {
      render(
        <AuthProvider>
          <AuthConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('status')).toHaveTextContent('authenticated');
      });

      const logoutButton = await screen.findByRole('button', { name: 'Log Out' });
      await act(async () => {
        logoutButton.click();
      });

      await waitFor(() => {
        expect(assignSpy).toHaveBeenCalledWith('https://idp.example.com/logout');
      });
      expect(captureSpy).not.toHaveBeenCalled();
    } finally {
      global.fetch = originalFetch;
      Object.defineProperty(document, 'cookie', { value: '', writable: true, configurable: true });
    }
  });

  it('redirects to / on logout without IdP SLO and does NOT capture', async () => {
    jest.spyOn(loginApi, 'fetchSession').mockResolvedValue(authenticatedSession);
    const captureSpy = jest.spyOn(guideReturnTo, 'captureGuideReturnTo').mockImplementation(() => {});
    const assignSpy = mockWindowLocation('/assets/guide/index.html');
    const originalFetch = global.fetch;
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      status: 204,
      headers: new Headers(),
    });

    Object.defineProperty(document, 'cookie', {
      value: `IQ-SESSION-EXPIRATION-TIMESTAMP=${Date.now() + 60_000}`,
      writable: true,
      configurable: true,
    });

    try {
      render(
        <AuthProvider>
          <AuthConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('status')).toHaveTextContent('authenticated');
      });

      const logoutButton = await screen.findByRole('button', { name: 'Log Out' });
      await act(async () => {
        logoutButton.click();
      });

      await waitFor(() => {
        expect(assignSpy).toHaveBeenCalledWith('/');
      });
      expect(captureSpy).not.toHaveBeenCalled();
    } finally {
      global.fetch = originalFetch;
      Object.defineProperty(document, 'cookie', { value: '', writable: true, configurable: true });
    }
  });

  it('captures and redirects to / when session expires (no IdP SLO URL)', async () => {
    jest.spyOn(loginApi, 'fetchSession').mockResolvedValue(authenticatedSession);
    const captureSpy = jest.spyOn(guideReturnTo, 'captureGuideReturnTo').mockImplementation(() => {});
    const assignSpy = mockWindowLocation('/assets/guide/index.html');
    const originalFetch = global.fetch;
    global.fetch = jest.fn().mockResolvedValue({ ok: true, status: 200, headers: new Headers() });

    Object.defineProperty(document, 'cookie', {
      value: `IQ-SESSION-EXPIRATION-TIMESTAMP=${Date.now() - 1000}`,
      writable: true,
      configurable: true,
    });

    try {
      render(
        <AuthProvider>
          <AuthConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(captureSpy).toHaveBeenCalled();
        expect(assignSpy).toHaveBeenCalledWith('/');
      });
    } finally {
      global.fetch = originalFetch;
      Object.defineProperty(document, 'cookie', { value: '', writable: true, configurable: true });
    }
  });

  it('captures and redirects to IdP SLO when session expires (IdP SLO URL present)', async () => {
    jest.spyOn(loginApi, 'fetchSession').mockResolvedValue(authenticatedSession);
    const captureSpy = jest.spyOn(guideReturnTo, 'captureGuideReturnTo').mockImplementation(() => {});
    const assignSpy = mockWindowLocation('/assets/guide/index.html');
    const originalFetch = global.fetch;
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers({ Location: 'https://idp.example.com/logout' }),
    });

    Object.defineProperty(document, 'cookie', {
      value: `IQ-SESSION-EXPIRATION-TIMESTAMP=${Date.now() - 1000}`,
      writable: true,
      configurable: true,
    });

    try {
      render(
        <AuthProvider>
          <AuthConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(captureSpy).toHaveBeenCalled();
        expect(assignSpy).toHaveBeenCalledWith('https://idp.example.com/logout');
      });
    } finally {
      global.fetch = originalFetch;
      Object.defineProperty(document, 'cookie', { value: '', writable: true, configurable: true });
    }
  });

  it('throws when useAuth is used outside AuthProvider', () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});

    expect(() => render(<AuthConsumer />)).toThrow(
      'useAuth must be used within an AuthProvider'
    );

    consoleSpy.mockRestore();
  });
});
