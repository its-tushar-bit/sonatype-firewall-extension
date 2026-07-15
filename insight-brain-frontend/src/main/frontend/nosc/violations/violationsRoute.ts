/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Single source of truth for the Martha V1 Violations list route. Shared by the route registration
 * ({@code nexus-one/routes.tsx}), the {@link ViolationsList} container's URL read/write bridge, and
 * the test harness so the state name and the query-param declaration can never drift apart.
 *
 * The optional query params persist search + sidebar filters + page in the hash (CLM-42260); there is
 * no {@code sort} param because the list supports only the single {@code -policyThreatLevel} order.
 */
export const NEXUS_ONE_VIOLATIONS_STATE_NAME = 'nexusOneViolations';

export const NEXUS_ONE_VIOLATIONS_URL = '/violations?q&page&state&category&stage&org&app&threat';
