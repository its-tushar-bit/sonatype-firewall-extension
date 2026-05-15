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
const LOGOUT_URL = '/rest/user/session/logout';

// Calls the backend logout endpoint, which destroys the Shiro session and may
// return a `Location` header pointing at the IdP's SLO URL (e.g. Auth0). Returns
// the Location value if present so callers can hard-redirect for full SLO.
function logoutOnServer(): Promise<string | null> {
  const token = getCsrfToken();
  const headers: HeadersInit = token ? { 'X-CSRF-TOKEN': token } : {};
  return fetch(LOGOUT_URL, { method: 'DELETE', credentials: 'same-origin', headers })
    .then((response) => response.headers.get('Location'), () => null);
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
  logout: () => Promise<void>;
  authFetch: AuthFetch;
}

interface CheckSessionOptions {
  // When true, skip the SSO-only auto-redirect even if SSO-only mode is enabled.
  // Used after explicit logout so the user lands on the login page instead of being
  // silently re-authenticated by the still-active IdP session.
  skipSsoOnlyRedirect?: boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthContextValue['status']>('loading');
  const [user, setUser] = useState<User | null>(null);
  const [ssoConfig, setSsoConfig] = useState<SsoConfig | null>(null);
  const [showExpirationWarning, setShowExpirationWarning] = useState(false);

  const queueRef = useRef(new RequestQueue());
  const trackerRef = useRef<ReturnType<typeof createSessionExpirationTracker> | null>(null);
  // Ref-based indirection: onExpired needs to call checkSession, but checkSession
  // depends on startExpirationTracking which owns onExpired — using the ref breaks
  // the cycle without re-creating the tracker on every checkSession identity change.
  const checkSessionRef = useRef<(options?: CheckSessionOptions) => Promise<void>>(
    () => Promise.resolve()
  );

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
        const idpLogoutUrl = await logoutOnServer();
        if (idpLogoutUrl) {
          window.location.assign(idpLogoutUrl);
          return;
        }
        // Skip the SSO-only auto-redirect so the user lands on the login page
        // instead of being silently re-authenticated by the still-active IdP
        // session. Matches explicit-logout behavior — an expired session should
        // not bounce the user back through the IdP without their consent.
        await checkSessionRef.current({ skipSsoOnlyRedirect: true });
      },
    });
    trackerRef.current = tracker;
    tracker.start();
  }, []);

  const checkSession = useCallback(async (options?: CheckSessionOptions) => {
    try {
      const session = await fetchSession();
      if (session.authenticated && session.user) {
        setUser(session.user);
        setSsoConfig(null);
        setStatus('authenticated');
        startExpirationTracking();
      } else {
        resetToUnauthenticated(session.ssoConfig);

        if (session.ssoConfig && !options?.skipSsoOnlyRedirect) {
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
    checkSessionRef.current = checkSession;
  }, [checkSession]);

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

  const logout = useCallback(async () => {
    setShowExpirationWarning(false);
    const idpLogoutUrl = await logoutOnServer();
    if (idpLogoutUrl) {
      // The backend returned an IdP SLO URL (e.g. Auth0). Hard-redirect to it
      // so the user is also signed out of the IdP, not just the IQ session.
      window.location.assign(idpLogoutUrl);
      return;
    }
    // No IdP SLO URL — re-fetch session in place so the user lands on the
    // login page. Skip the SSO-only auto-redirect since the user explicitly
    // chose to log out.
    await checkSessionRef.current({ skipSsoOnlyRedirect: true });
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ status, user, ssoConfig, login, logout, authFetch }),
    [status, user, ssoConfig, login, logout, authFetch]
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
      <SessionExpirationWarning
        open={showExpirationWarning}
        onStayLoggedIn={handleStayLoggedIn}
        onLogOut={logout}
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
