/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Single source of truth for the Martha V1 Vulnerabilities list route.
 * Shared by route registration, the list container URL bridge, and tests.
 */
export const NEXUS_ONE_VULNERABILITIES_STATE_NAME = 'nexusOneVulnerabilities';

export const NEXUS_ONE_VULNERABILITIES_URL =
  '/vulnerabilities?tab&q&page&sort&severity&cvss&ecosystem';

export type VulnerabilitiesTab = 'myScanData' | 'catalog';

export const DEFAULT_VULNERABILITIES_TAB: VulnerabilitiesTab = 'myScanData';
