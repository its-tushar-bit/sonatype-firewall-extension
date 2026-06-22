/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { getCsrfToken } from '../auth/csrfToken';
import { notifySessionResponse } from '../auth/sessionExpiration';
import { isGuideLicenseUnavailable, notifyLicenseRevoked } from '../license/licenseRevocation';

/**
 * API configuration for Guide SPA.
 *
 * The SPA is served from IQ Server and uses relative URLs. Backend endpoints
 * live under `/api/v2/guide/...` and mirror seaworthy's backend-server API.
 */

/** API prefix for all Guide endpoints. Matches JAX-RS path in insight-brain-service. */
export const API_PREFIX = '/api/v2/guide';

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

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

/**
 * Specialised {@link ApiError} thrown when a Guide data call returns a response flagged as
 * Guide-license-unavailable (the {@link isGuideLicenseUnavailable} marker header). By the time
 * this is thrown, {@link apiFetch} has already triggered a licensed-solutions refresh via
 * {@link notifyLicenseRevoked}; the {@code LicenseGate} then swaps in the learn-more page, so
 * callers generally do not need to render their own error UI for it.
 */
export class GuideLicenseRevokedError extends ApiError {
  constructor(message: string, status: number, statusText: string) {
    super(message, status, statusText);
    this.name = 'GuideLicenseRevokedError';
  }
}

export type ApiFetchOptions = RequestInit;

/**
 * Generic fetch wrapper for Guide backend endpoints.
 *
 * Sends `credentials: 'same-origin'` so the IQ session cookie travels with
 * the request, attaches a CSRF token on unsafe methods, notifies the
 * session-expiration tracker on every response so activity-driven session
 * extension is reflected client-side, and throws {@link ApiError} on non-2xx
 * responses.
 */
export async function apiFetch<T>(
  path: string,
  init?: ApiFetchOptions
): Promise<T> {
  let fetchOptions: ApiFetchOptions = init ?? {};

  // Mark every Guide request as originating from the SPA UI so the backend credit-telemetry
  // filter attributes it to the UI channel (absence of this header means an external API client).
  const headers = new Headers(fetchOptions.headers as HeadersInit | undefined);
  headers.set('X-Guide-Client', 'ui');

  const method = (fetchOptions.method ?? 'GET').toUpperCase();
  if (!SAFE_METHODS.has(method)) {
    const token = getCsrfToken();
    if (token) {
      headers.set('X-CSRF-TOKEN', token);
    }
  }
  fetchOptions = { ...fetchOptions, headers };

  const response = await fetch(path, { credentials: 'same-origin', ...fetchOptions });

  notifySessionResponse(response);

  if (!response.ok) {
    // Capture the Guide-license-revocation marker before consuming the body: a refresh must be
    // triggered whether or not the error body parses as JSON. Reading headers does not consume
    // the body.
    const licenseRevoked = isGuideLicenseUnavailable(response);

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

    if (licenseRevoked) {
      // Guide entitlement was revoked mid-session (GUIDE-2814 backend / GUIDE-2815 frontend). Ask
      // the LicenseProvider to refetch licensed solutions so the existing LicenseGate renders the
      // learn-more page, then surface a typed error to the in-flight caller.
      notifyLicenseRevoked();
      throw new GuideLicenseRevokedError(errorMessage, response.status, response.statusText);
    }

    throw new ApiError(errorMessage, response.status, response.statusText);
  }

  return response.json() as Promise<T>;
}
