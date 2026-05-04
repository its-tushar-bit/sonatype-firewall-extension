/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { fetchSession, submitLogin } from './loginApi';
import type { SsoConfig } from './loginApi';
import { createAuthFetch } from './authFetch';
import type { AuthFetch } from './authFetch';

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

  const resetToUnauthenticated = useCallback(() => {
    setStatus('unauthenticated');
    setUser(null);
  }, []);

  const checkSession = useCallback(async () => {
    try {
      const session = await fetchSession();
      if (session.authenticated && session.user) {
        setUser(session.user);
        setSsoConfig(null);
        setStatus('authenticated');
      } else {
        setSsoConfig(session.ssoConfig);
        resetToUnauthenticated();
      }
    } catch {
      setSsoConfig(null);
      resetToUnauthenticated();
    }
  }, [resetToUnauthenticated]);

  useEffect(() => {
    checkSession();
  }, [checkSession]);

  const login = useCallback(async (username: string, password: string) => {
    await submitLogin(username, password);
    const session = await fetchSession();
    if (session.authenticated && session.user) {
      setUser(session.user);
      setSsoConfig(null);
      setStatus('authenticated');
    } else {
      setSsoConfig(session.ssoConfig);
      resetToUnauthenticated();
      throw new Error('Login succeeded but session could not be established');
    }
  }, [resetToUnauthenticated]);

  const authFetch = useMemo(
    () => createAuthFetch(checkSession),
    [checkSession]
  );

  const value = useMemo<AuthContextValue>(
    () => ({ status, user, ssoConfig, login, authFetch }),
    [status, user, ssoConfig, login, authFetch]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
