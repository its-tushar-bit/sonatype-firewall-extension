/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

const SESSION_URL = '/rest/user/session';

export interface SessionResponse {
  authenticated: boolean;
  user: { username: string; displayName: string; groups: string[] } | null;
  sessionTimeoutMs: number | null;
}

interface RawSessionPayload {
  authenticated: boolean;
  username: string | null;
  displayName: string | null;
  groups: string[] | null;
  sessionTimeoutMilliseconds: number | null;
}

export async function fetchSession(): Promise<SessionResponse> {
  const response = await fetch(SESSION_URL, { credentials: 'same-origin' });

  if (response.status === 401) {
    return {
      authenticated: false,
      user: null,
      sessionTimeoutMs: null,
    };
  }

  if (!response.ok) {
    throw new Error(`Session check failed (${response.status})`);
  }

  const data: RawSessionPayload = await response.json();

  if (!data.authenticated || !data.username) {
    return { authenticated: false, user: null, sessionTimeoutMs: null };
  }

  return {
    authenticated: true,
    user: {
      username: data.username,
      displayName: data.displayName ?? data.username,
      groups: data.groups ?? [],
    },
    sessionTimeoutMs: data.sessionTimeoutMilliseconds,
  };
}
