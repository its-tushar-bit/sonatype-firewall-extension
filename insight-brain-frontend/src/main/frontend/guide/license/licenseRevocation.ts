/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Bridge between the module-level Guide HTTP client ({@link import('../api/apiFetch')}) and the
 * React {@link import('./LicenseProvider').LicenseProvider}, for mid-session Guide license
 * revocation.
 *
 * <p>
 * When Sonatype Operations revokes a customer's Guide entitlement, IQ Server (see backend story
 * GUIDE-2814) tags the response to Guide data calls with a stable marker header — the immediate
 * {@code 402} from the first call after revocation and the {@code 403} from every subsequent call.
 * The SPA matches on that header rather than parsing English error text. When detected, the HTTP
 * client calls {@link notifyLicenseRevoked} so the provider can refetch the licensed solutions and
 * let the existing {@code LicenseGate} swap in the "Guide is not currently enabled" page.
 *
 * <p>
 * This mirrors the {@code activeTracker} pattern in {@code auth/sessionExpiration.ts}: a single
 * module-level handler that the provider registers on mount, so any module that fields a backend
 * response can notify it without threading it through React context. This module intentionally has
 * no dependency on {@code apiFetch} to keep the import graph acyclic.
 */

/** Response header IQ Server sets to flag Guide-license-unavailable errors for the SPA. */
export const GUIDE_LICENSE_HEADER = 'X-Sonatype-Guide-License';

/** {@link GUIDE_LICENSE_HEADER} value emitted when Guide is not currently licensed. */
export const GUIDE_LICENSE_UNAVAILABLE = 'unavailable';

/**
 * Returns {@code true} when a response carries the Guide-license-unavailable marker header.
 *
 * <p>
 * Null-safe on {@code response.headers}: some test doubles model a body-less error response with
 * no {@code headers} field, and {@code Headers.get} is case-insensitive, so this matches the
 * marker regardless of wire casing.
 */
export function isGuideLicenseUnavailable(response: Response): boolean {
  return response.headers?.get(GUIDE_LICENSE_HEADER) === GUIDE_LICENSE_UNAVAILABLE;
}

type LicenseRevocationHandler = () => void;

// Module-level handle to the LicenseProvider's refresh callback. The provider registers exactly
// one handler for the user's session (and clears it on unmount), so the HTTP client can route a
// detected revocation without React context.
let handler: LicenseRevocationHandler | null = null;

/** Registers (or clears, with {@code null}) the handler invoked on {@link notifyLicenseRevoked}. */
export function setLicenseRevocationHandler(next: LicenseRevocationHandler | null): void {
  handler = next;
}

/** Notifies the registered handler (if any) that a Guide license revocation was detected. */
export function notifyLicenseRevoked(): void {
  handler?.();
}
