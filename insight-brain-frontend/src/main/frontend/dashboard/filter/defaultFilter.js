/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { defaultMaxDaysOld, defaultMinExpiration } from './staticFilterEntries';

export default Object.freeze({
  organizations: new Set(),
  applications: new Set(),
  repositories: new Set(),
  categories: new Set(),
  stages: new Set(),
  policyTypes: new Set(),
  policyWaiverReasons: new Set(),
  policyViolationStates: new Set(['OPEN']),
  maxDaysOld: defaultMaxDaysOld,
  expirationDate: defaultMinExpiration,
  policyThreatLevels: [2, 10],
  policyWaiverReasonIds: new Set(),
});

export const DEFAULT_FILTER_NAME = 'Default';
