/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { getCsrfToken } from './csrfToken';
import type { RequestQueue } from './requestQueue';

export type AuthFetch = (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>;

export interface AuthFetchOptions {
  queue?: RequestQueue;
  onResponse?: (response: Response) => void;
}

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

export function createAuthFetch(
  onUnauthorized: () => void,
  options: AuthFetchOptions = {}
): AuthFetch {
  const { queue, onResponse } = options;

  return async (input, init) => {
    const method = init?.method?.toUpperCase() ?? 'GET';

    let finalInit = init;
    if (!SAFE_METHODS.has(method)) {
      const token = getCsrfToken();
      if (token) {
        const headers = new Headers(init?.headers);
        headers.set('X-CSRF-TOKEN', token);
        finalInit = { ...init, headers };
      }
    }

    const response = await fetch(input, finalInit);

    if (response.status === 401) {
      if (queue) {
        const shouldTriggerReauth = !queue.isReauthenticating;
        const resultPromise = queue.enqueue(() => fetch(input, finalInit));
        if (shouldTriggerReauth) {
          onUnauthorized();
        }
        return resultPromise;
      }
      onUnauthorized();
      return response;
    }

    if (onResponse && response.status < 400) {
      onResponse(response);
    }

    return response;
  };
}
