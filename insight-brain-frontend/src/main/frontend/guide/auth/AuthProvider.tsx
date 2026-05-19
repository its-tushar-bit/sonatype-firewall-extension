/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { fetchSession } from './loginApi';
import { createAuthFetch } from './authFetch';
import type { AuthFetch } from './authFetch';
import { RequestQueue } from './requestQueue';
import { createSessionExpirationTracker } from './sessionExpiration';
import { SessionExpirationWarning } from './SessionExpirationWarning';
import { getCsrfToken } from './csrfToken';
import { captureGuideReturnTo } from './guideReturnTo';

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
  logout: () => Promise<void>;
  authFetch: AuthFetch;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthContextValue['status']>('loading');
  const [user, setUser] = useState<User | null>(null);
  const [showExpirationWarning, setShowExpirationWarning] = useState(false);

  const queueRef = useRef(new RequestQueue());
  const trackerRef = useRef<ReturnType<typeof createSessionExpirationTracker> | null>(null);

  const resetToUnauthenticated = useCallback(() => {
    setStatus('unauthenticated');
    setUser(null);
    trackerRef.current?.stop();
  }, []);

  const startExpirationTracking = useCallback(() => {
    trackerRef.current?.stop();
    const tracker = createSessionExpirationTracker({
      onWarning: () => setShowExpirationWarning(true),
      onExpired: async () => {
        setShowExpirationWarning(false);
        queueRef.current.rejectAll(new Error('Session expired'));
        // Capture the current Guide URL so the legacy IQ shell can bounce
        // the user back here after they sign in again. Always capture, even
        // when an IdP SLO URL is returned — the user's eventual landing
        // point is still the legacy origin and the captured URL gets
        // consumed there.
        captureGuideReturnTo();
        const idpLogoutUrl = await logoutOnServer();
        if (idpLogoutUrl) {
          window.location.assign(idpLogoutUrl);
          return;
        }
        window.location.assign('/');
      },
    });
    trackerRef.current = tracker;
    tracker.start();
  }, []);

  const checkSession = useCallback(async () => {
    try {
      const session = await fetchSession();
      if (session.authenticated && session.user) {
        setUser(session.user);
        setStatus('authenticated');
        startExpirationTracking();
      } else {
        resetToUnauthenticated();
      }
    } catch {
      resetToUnauthenticated();
    }
  }, [resetToUnauthenticated, startExpirationTracking]);

  useEffect(() => {
    checkSession();
    return () => {
      trackerRef.current?.stop();
    };
  }, [checkSession]);

  // Once we know the user is unauthenticated, capture the Guide URL and
  // redirect to / so the legacy IQ shell can render LoginModal. We do this
  // in an effect (not during render) so it can't run twice in StrictMode.
  useEffect(() => {
    if (status === 'unauthenticated') {
      captureGuideReturnTo();
      window.location.assign('/');
    }
  }, [status]);

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
    // Stop the expiration tracker first so a near-simultaneous expiry
    // can't fire onExpired during the logoutOnServer() await and capture
    // a return-to URL that contradicts the explicit-logout intent.
    trackerRef.current?.stop();
    // Explicit logout: the user chose to leave, so we do NOT capture the
    // current Guide URL. They land on Lifecycle (or the IdP) and stay
    // there until they navigate back to Guide deliberately.
    const idpLogoutUrl = await logoutOnServer();
    if (idpLogoutUrl) {
      window.location.assign(idpLogoutUrl);
      return;
    }
    window.location.assign('/');
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ status, user, logout, authFetch }),
    [status, user, logout, authFetch]
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
