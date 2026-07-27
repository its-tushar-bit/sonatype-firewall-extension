/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Single source of truth for the Nexus One Legal list route (LEGAL_VIOLATION license-risk triage).
 * Shared by route registration, the LegalList URL bridge, and tests.
 *
 * {@code category} carries selected License Threat Group (LTG) names — not Policy Type enums.
 * State / waiver URL tokens are not used (LEGAL_VIOLATION findings have no OPEN/WAIVED/waiver status).
 */
export const NEXUS_ONE_LEGAL_STATE_NAME = 'nexusOneLegal';

export const NEXUS_ONE_LEGAL_URL = '/legal?q&page&category&stage&org&app&threat';
