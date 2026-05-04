/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export type AuthFetch = (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>;

export function createAuthFetch(onUnauthorized: () => void): AuthFetch {
  return async (input, init) => {
    const response = await fetch(input, init);
    if (response.status === 401) {
      onUnauthorized();
    }
    return response;
  };
}
