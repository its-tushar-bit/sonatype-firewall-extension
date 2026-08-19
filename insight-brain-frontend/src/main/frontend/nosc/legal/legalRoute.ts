/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Single source of truth for the Nexus One Legal risk-list route (LEGAL_VIOLATION triage).
 * Shared by route registration, the LegalList URL bridge, and tests.
 *
 * ALP LeftNav Legal opens Classic Obligations at {@code /legal} (CLM-44467). This native list
 * lives at {@code /legal-risk} and is the Dashboard Legal deep-dive for tenants without ALP
 * (Classic Obligations returns 402 without Advanced Legal Pack).
 *
 * {@code category} carries selected License Threat Group (LTG) names — not Policy Type enums.
 * State / waiver URL tokens are not used (LEGAL_VIOLATION findings have no OPEN/WAIVED/waiver status).
 */
export const NEXUS_ONE_LEGAL_STATE_NAME = 'nexusOneLegal';

/** Clean path for the native LEGAL_VIOLATION triage list (no query-param tokens). */
export const NEXUS_ONE_LEGAL_PATH = '/legal-risk';

export const NEXUS_ONE_LEGAL_URL = `${NEXUS_ONE_LEGAL_PATH}?q&page&category&stage&org&app&threat`;
