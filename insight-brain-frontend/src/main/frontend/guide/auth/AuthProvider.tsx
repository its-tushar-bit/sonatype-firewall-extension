/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { fetchSession, submitLogin } from './loginApi';
import type { SsoConfig } from './loginApi';
import { createAuthFetch } from './authFetch';
import type { AuthFetch } from './authFetch';
import { RequestQueue } from './requestQueue';
import { fetchIsSsoOnlyEnabled } from './ssoOnlyMode';
import { createSessionExpirationTracker } from './sessionExpiration';
import { SessionExpirationWarning } from './SessionExpirationWarning';
import { getCsrfToken } from './csrfToken';

const SESSION_URL = '/rest/user/session';

function deleteSession(): Promise<void> {
  const token = getCsrfToken();
  const headers: HeadersInit = token ? { 'X-CSRF-TOKEN': token } : {};
  return fetch(SESSION_URL, { method: 'DELETE', credentials: 'same-origin', headers }).then(() => {}, () => {});
}

interface User {
  username: string;
  displayName: string;
  groups: string[];
}

interface AuthContextValue {
  status: 'loading' | 'authenticated' | 'unauthenticated';
  user: User | null;
  ssoConfig: SsoConfig | null;
  login: (username: string, password: string) => Promise<void>;
  authFetch: AuthFetch;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthContextValue['status']>('loading');
  const [user, setUser] = useState<User | null>(null);
  const [ssoConfig, setSsoConfig] = useState<SsoConfig | null>(null);
  const [showExpirationWarning, setShowExpirationWarning] = useState(false);

  const queueRef = useRef(new RequestQueue());
  const trackerRef = useRef<ReturnType<typeof createSessionExpirationTracker> | null>(null);

  const resetToUnauthenticated = useCallback((newSsoConfig?: SsoConfig | null) => {
    setStatus('unauthenticated');
    setUser(null);
    if (newSsoConfig !== undefined) {
      setSsoConfig(newSsoConfig);
    }
    trackerRef.current?.stop();
  }, []);

  const startExpirationTracking = useCallback(() => {
    trackerRef.current?.stop();
    const tracker = createSessionExpirationTracker({
      onWarning: () => setShowExpirationWarning(true),
      onExpired: async () => {
        setShowExpirationWarning(false);
        queueRef.current.rejectAll(new Error('Session expired'));
        await deleteSession();
        resetToUnauthenticated(null);
      },
    });
    trackerRef.current = tracker;
    tracker.start();
  }, [resetToUnauthenticated]);

  const checkSession = useCallback(async () => {
    try {
      const session = await fetchSession();
      if (session.authenticated && session.user) {
        setUser(session.user);
        setSsoConfig(null);
        setStatus('authenticated');
        startExpirationTracking();
      } else {
        resetToUnauthenticated(session.ssoConfig);

        if (session.ssoConfig) {
          const ssoOnly = await fetchIsSsoOnlyEnabled();
          if (ssoOnly && !window.location.pathname.endsWith('/backupLogin')) {
            queueRef.current.rejectAll(new Error('SSO redirect'));
            const target = new URL(session.ssoConfig.loginUrl, window.location.origin);
            if (target.origin !== window.location.origin) {
              return;
            }
            const returnTo = window.location.pathname + window.location.search + window.location.hash;
            if (returnTo !== '/') {
              target.searchParams.set('returnTo', returnTo);
            }
            window.location.assign(target.href);
          }
        }
      }
    } catch {
      resetToUnauthenticated(null);
    }
  }, [resetToUnauthenticated, startExpirationTracking]);

  useEffect(() => {
    checkSession();
    return () => {
      trackerRef.current?.stop();
    };
  }, [checkSession]);

  const login = useCallback(async (username: string, password: string) => {
    try {
      await submitLogin(username, password);
      const session = await fetchSession();
      if (session.authenticated && session.user) {
        setUser(session.user);
        setSsoConfig(null);
        setStatus('authenticated');
        startExpirationTracking();
        queueRef.current.replayAll();
      } else {
        resetToUnauthenticated(session.ssoConfig);
        queueRef.current.rejectAll();
        throw new Error('Login succeeded but session could not be established');
      }
    } catch (error) {
      queueRef.current.rejectAll(error instanceof Error ? error : new Error(String(error)));
      throw error;
    }
  }, [resetToUnauthenticated, startExpirationTracking]);

  const authFetch = useMemo(
    () => createAuthFetch(checkSession, {
      queue: queueRef.current,
      onResponse: (response) => trackerRef.current?.refreshFromResponse(response),
    }),
    [checkSession]
  );

  const handleStayLoggedIn = useCallback(async () => {
    setShowExpirationWarning(false);
    try {
      await authFetch(SESSION_URL, { credentials: 'same-origin' });
    } catch {
      // session extend failed — tracker will fire onExpired
    }
  }, [authFetch]);

  const handleLogOut = useCallback(async () => {
    setShowExpirationWarning(false);
    await deleteSession();
    resetToUnauthenticated(null);
  }, [resetToUnauthenticated]);

  const value = useMemo<AuthContextValue>(
    () => ({ status, user, ssoConfig, login, authFetch }),
    [status, user, ssoConfig, login, authFetch]
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
      <SessionExpirationWarning
        open={showExpirationWarning}
        onStayLoggedIn={handleStayLoggedIn}
        onLogOut={handleLogOut}
      />
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
