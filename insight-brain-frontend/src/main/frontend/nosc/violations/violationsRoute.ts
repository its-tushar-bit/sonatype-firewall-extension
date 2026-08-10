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
 * The optional query params persist search + sidebar filters + page + sort in the hash (CLM-42260 /
 * CLM-44036). {@code sort} is {@code highest-threat} (default, omitted) or {@code lowest-threat}.
 * {@code waiver} carries the auto/manual waiver-type radio (CLM-42261).
 * {@code appCategory} carries Application Category (tag) ids (CLM-44129).
 */
export const NEXUS_ONE_VIOLATIONS_STATE_NAME = 'nexusOneViolations';

export const NEXUS_ONE_VIOLATIONS_URL =
  '/violations?q&sort&page&state&category&stage&org&app&threat&waiver&appCategory';

/**
 * All list query params are {@code dynamic} so the container's URL round-trip (search / filters / page
 * persisted via {@code stateService.go(..., { location: 'replace' })}) updates params in place instead
 * of triggering a UI-Router transition. Without this, every filter click re-instantiates the routed
 * component — the whole page (and its refetch) tears down and rebuilds, which reads as a flash and fires
 * a duplicate list request. Params still reach the container via {@code useCurrentStateAndParams}, so
 * bookmarks and back/forward keep working.
 */
export const NEXUS_ONE_VIOLATIONS_PARAMS = {
  q: { dynamic: true },
  sort: { dynamic: true },
  page: { dynamic: true },
  state: { dynamic: true },
  category: { dynamic: true },
  stage: { dynamic: true },
  org: { dynamic: true },
  app: { dynamic: true },
  threat: { dynamic: true },
  waiver: { dynamic: true },
  appCategory: { dynamic: true },
} as const;
