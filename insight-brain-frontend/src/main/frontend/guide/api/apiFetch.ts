/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * API configuration for Guide SPA.
 *
 * When self-hosted, the SPA is served from IQ Server and uses relative URLs.
 * Backend endpoints will be at: /api/v2/guide/...
 *
 * Real backend endpoints are being built in insight-brain-service, mirroring
 * seaworthy's backend-server API.
 */

/**
 * Feature flag to control mock data usage.
 * TODO: Set to false when real backend endpoints exist.
 */
export const USE_MOCKS = true;

/** API prefix for all Guide endpoints. Matches JAX-RS path in insight-brain-service when available. (GUIDE-2316). */
export const API_PREFIX = '/api/v2/guide';

/** Artificial latency in milliseconds for mock responses (exercises loading states) */
const MOCK_LATENCY_MS = 150;

/** Error class for API errors with status code */
export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly statusText: string
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

/** Options for apiFetch */
export interface ApiFetchOptions extends RequestInit {
  /** Mock handler to use when USE_MOCKS is true */
  mockHandler?: () => unknown;
}

/**
 * Generic fetch wrapper that handles mock data and error responses.
 *
 * When USE_MOCKS is true and a mockHandler is provided, returns the mock
 * data after a small artificial latency to exercise loading states.
 *
 * Otherwise, performs a real fetch and throws ApiError on non-2xx responses.
 *
 * @param path - The URL path to fetch
 * @param init - Standard fetch options plus mockHandler
 * @returns Parsed JSON response typed as T
 * @throws ApiError on non-2xx responses
 */
export async function apiFetch<T>(
  path: string,
  init?: ApiFetchOptions
): Promise<T> {
  const { mockHandler, ...fetchOptions } = init ?? {};

  if (USE_MOCKS && mockHandler) {
    // Simulate network latency to exercise loading states
    await new Promise((resolve) => setTimeout(resolve, MOCK_LATENCY_MS));
    return mockHandler() as T;
  }

  const response = await fetch(path, fetchOptions);

  if (!response.ok) {
    let errorMessage = `${response.status} ${response.statusText}`;
    try {
      const errorBody = await response.json();
      if (typeof errorBody?.message === 'string') {
        errorMessage = errorBody.message;
      } else if (typeof errorBody === 'string') {
        errorMessage = errorBody;
      }
    } catch {
      // Response body not JSON, use status text
    }
    throw new ApiError(errorMessage, response.status, response.statusText);
  }

  return response.json() as Promise<T>;
}
