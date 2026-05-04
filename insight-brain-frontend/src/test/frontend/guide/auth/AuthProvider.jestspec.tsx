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

function AuthConsumer() {
  const { status, user, ssoConfig, login, authFetch } = useAuth();
  return (
    <div>
      <span data-testid="status">{status}</span>
      <span data-testid="username">{user?.username ?? 'none'}</span>
      <span data-testid="sso">{ssoConfig ? ssoConfig.type : 'none'}</span>
      <button onClick={() => login('admin', 'admin123')}>Login</button>
      <button onClick={() => authFetch('/api/test')}>FetchData</button>
    </div>
  );
}

const authenticatedSession: SessionResponse = {
  authenticated: true,
  user: { username: 'admin', displayName: 'Administrator', groups: ['Administrators'] },
  sessionTimeoutMs: 1800000,
  ssoConfig: null,
};

const unauthenticatedSession: SessionResponse = {
  authenticated: false,
  user: null,
  sessionTimeoutMs: null,
  ssoConfig: null,
};

const unauthenticatedWithSso: SessionResponse = {
  authenticated: false,
  user: null,
  sessionTimeoutMs: null,
  ssoConfig: { type: 'SAML', loginUrl: '/saml/login' },
};

describe('AuthProvider', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('starts in loading state, then transitions to authenticated', async () => {
    jest.spyOn(loginApi, 'fetchSession').mockResolvedValue(authenticatedSession);

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

  it('transitions to unauthenticated when session check fails', async () => {
    jest.spyOn(loginApi, 'fetchSession').mockResolvedValue(unauthenticatedSession);

    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated');
    });
    expect(screen.getByTestId('username')).toHaveTextContent('none');
  });

  it('exposes ssoConfig when SSO is available', async () => {
    jest.spyOn(loginApi, 'fetchSession').mockResolvedValue(unauthenticatedWithSso);

    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('sso')).toHaveTextContent('SAML');
    });
  });

  it('login() calls submitLogin then re-fetches session', async () => {
    const fetchSessionSpy = jest.spyOn(loginApi, 'fetchSession')
      .mockResolvedValueOnce(unauthenticatedSession)
      .mockResolvedValueOnce(authenticatedSession);
    const submitLoginSpy = jest.spyOn(loginApi, 'submitLogin').mockResolvedValue(undefined);

    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated');
    });

    await act(async () => {
      screen.getByRole('button', { name: 'Login' }).click();
    });

    expect(submitLoginSpy).toHaveBeenCalledWith('admin', 'admin123');

    await waitFor(() => {
      expect(screen.getByTestId('status')).toHaveTextContent('authenticated');
    });
    expect(fetchSessionSpy).toHaveBeenCalledTimes(2);
  });

  it('authFetch re-checks session on 401', async () => {
    jest.spyOn(loginApi, 'fetchSession')
      .mockResolvedValueOnce(authenticatedSession)
      .mockResolvedValueOnce(unauthenticatedSession);
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
    } finally {
      global.fetch = originalFetch;
    }
  });

  it('login() surfaces error when fetchSession fails after successful submitLogin', async () => {
    jest.spyOn(loginApi, 'fetchSession')
      .mockResolvedValueOnce(unauthenticatedSession)
      .mockRejectedValueOnce(new Error('Network error'));
    jest.spyOn(loginApi, 'submitLogin').mockResolvedValue(undefined);

    let loginError: Error | null = null;
    function ErrorCapture() {
      const { status, login } = useAuth();
      return (
        <div>
          <span data-testid="status">{status}</span>
          <span data-testid="error">{loginError?.message ?? 'none'}</span>
          <button onClick={async () => {
            try {
              await login('admin', 'admin123');
            } catch (err) {
              loginError = err as Error;
            }
          }}>Login</button>
        </div>
      );
    }

    render(
      <AuthProvider>
        <ErrorCapture />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated');
    });

    await act(async () => {
      screen.getByRole('button', { name: 'Login' }).click();
    });

    expect(loginError).not.toBeNull();
    expect(loginError!.message).toBe('Network error');
    expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated');
  });

  it('login() throws when session is not established after successful submitLogin', async () => {
    jest.spyOn(loginApi, 'fetchSession')
      .mockResolvedValueOnce(unauthenticatedSession)
      .mockResolvedValueOnce(unauthenticatedSession);
    jest.spyOn(loginApi, 'submitLogin').mockResolvedValue(undefined);

    let loginError: Error | null = null;
    function ErrorCapture() {
      const { status, login } = useAuth();
      return (
        <div>
          <span data-testid="status">{status}</span>
          <span data-testid="error">{loginError?.message ?? 'none'}</span>
          <button onClick={async () => {
            try {
              await login('admin', 'admin123');
            } catch (err) {
              loginError = err as Error;
            }
          }}>Login</button>
        </div>
      );
    }

    render(
      <AuthProvider>
        <ErrorCapture />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated');
    });

    await act(async () => {
      screen.getByRole('button', { name: 'Login' }).click();
    });

    expect(loginError).not.toBeNull();
    expect(loginError!.message).toBe('Login succeeded but session could not be established');
    expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated');
  });

  it('clears ssoConfig when checkSession encounters a network error', async () => {
    jest.spyOn(loginApi, 'fetchSession')
      .mockResolvedValueOnce(unauthenticatedWithSso)
      .mockRejectedValueOnce(new Error('Network error'));
    const originalFetch = global.fetch;
    global.fetch = jest.fn().mockResolvedValue({ status: 401, ok: false });

    try {
      render(
        <AuthProvider>
          <AuthConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('sso')).toHaveTextContent('SAML');
      });

      await act(async () => {
        screen.getByRole('button', { name: 'FetchData' }).click();
      });

      await waitFor(() => {
        expect(screen.getByTestId('sso')).toHaveTextContent('none');
      });
      expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated');
    } finally {
      global.fetch = originalFetch;
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
