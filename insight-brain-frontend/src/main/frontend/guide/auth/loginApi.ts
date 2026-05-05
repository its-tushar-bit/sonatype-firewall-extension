/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

const SESSION_URL = '/rest/user/session';

export interface SsoConfig {
  type: 'SAML' | 'OIDC';
  loginUrl: string;
}

export interface SessionResponse {
  authenticated: boolean;
  user: { username: string; displayName: string; groups: string[] } | null;
  sessionTimeoutMs: number | null;
  ssoConfig: SsoConfig | null;
}

interface RawSessionPayload {
  authenticated: boolean;
  username: string | null;
  displayName: string | null;
  groups: string[] | null;
  sessionTimeoutMilliseconds: number | null;
}

const SSO_TYPES = new Set<SsoConfig['type']>(['SAML', 'OIDC']);

function parseSsoHeaders(headers: Headers): SsoConfig | null {
  const wwwAuth = headers.get('WWW-Authenticate');
  const loginUrl = headers.get('X-SSO-Login-URL');
  if (wwwAuth && loginUrl && SSO_TYPES.has(wwwAuth as SsoConfig['type'])) {
    return { type: wwwAuth as SsoConfig['type'], loginUrl };
  }
  return null;
}

export async function fetchSession(): Promise<SessionResponse> {
  const response = await fetch(SESSION_URL, { credentials: 'same-origin' });

  if (response.status === 401) {
    return {
      authenticated: false,
      user: null,
      sessionTimeoutMs: null,
      ssoConfig: parseSsoHeaders(response.headers),
    };
  }

  if (!response.ok) {
    throw new Error(`Session check failed (${response.status})`);
  }

  const data: RawSessionPayload = await response.json();

  if (!data.authenticated || !data.username) {
    return { authenticated: false, user: null, sessionTimeoutMs: null, ssoConfig: null };
  }

  return {
    authenticated: true,
    user: {
      username: data.username,
      displayName: data.displayName ?? data.username,
      groups: data.groups ?? [],
    },
    sessionTimeoutMs: data.sessionTimeoutMilliseconds,
    ssoConfig: null,
  };
}

export async function submitLogin(username: string, password: string): Promise<void> {
  const encoded = btoa(String.fromCharCode(
    ...new TextEncoder().encode(`${username}:${password}`)
  ));

  const response = await fetch(SESSION_URL, {
    method: 'POST',
    credentials: 'same-origin',
    headers: { Authorization: `Basic ${encoded}` },
  });

  if (!response.ok) {
    throw new Error(await extractErrorMessage(response));
  }
}

const MAX_ERROR_LENGTH = 200;

async function extractErrorMessage(response: Response): Promise<string> {
  try {
    const body = await response.clone().json();
    if (typeof body?.message === 'string') {
      return truncate(body.message);
    }
  } catch {
    // JSON parse failed, try text
  }

  try {
    const text = await response.text();
    if (text.trim()) {
      return truncate(text.trim());
    }
  } catch {
    // text extraction failed
  }

  return response.status === 401 ? 'Invalid username or password' : `Login failed (${response.status})`;
}

function truncate(value: string): string {
  return value.length > MAX_ERROR_LENGTH ? value.slice(0, MAX_ERROR_LENGTH) + '…' : value;
}
