/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as PropTypes from 'prop-types';

const discoveredCountsShape = PropTypes.shape({
  LOW: PropTypes.number,
  MODERATE: PropTypes.number,
  SEVERE: PropTypes.number,
  CRITICAL: PropTypes.number,
});
const countsTypeShape = PropTypes.shape({
  SECURITY: discoveredCountsShape,
  LICENSE: discoveredCountsShape,
  QUALITY: discoveredCountsShape,
  OTHER: discoveredCountsShape,
});

export const violationCountsShape = PropTypes.shape({
  timePeriodName: PropTypes.string,
  discoveredCounts: countsTypeShape,
  waivedCounts: countsTypeShape,
  fixedCounts: countsTypeShape,
});

export const violationsByCategoryShape = PropTypes.shape({
  timePeriodName: PropTypes.string,
  security: PropTypes.number,
  license: PropTypes.number,
  quality: PropTypes.number,
  other: PropTypes.number,
});

const violationsAverageShape = PropTypes.shape({
  averageDiscovered: PropTypes.number,
  averageDiscoveredCritical: PropTypes.number,
});

export const averagesShape = PropTypes.shape({
  evaluationCount: PropTypes.number,
  totalViolations: violationsAverageShape,
  securityViolations: violationsAverageShape,
  licenseViolations: violationsAverageShape,
  qualityViolations: violationsAverageShape,
  otherViolations: violationsAverageShape,
});

const applicationCountsTypeShape = PropTypes.shape({
  applicationsWithViolations: PropTypes.number,
  applicationsWithCriticalViolations: PropTypes.number,
});

export const applicationCountsShape = PropTypes.shape({
  totalApplications: PropTypes.number,
  activeApplications: PropTypes.number,
  total: applicationCountsTypeShape,
  security: applicationCountsTypeShape,
  license: applicationCountsTypeShape,
  quality: applicationCountsTypeShape,
  other: applicationCountsTypeShape,
});

export const mttrShape = PropTypes.shape({
  timePeriodName: PropTypes.string,
  mttrInSeconds: PropTypes.number,
  criticalMttrInSeconds: PropTypes.number,
});

const componentShape = PropTypes.shape({
  componentDisplayName: PropTypes.string,
  hash: PropTypes.string,
  count: PropTypes.number,
});

export const componentCountsShape = PropTypes.shape({
  componentsPerApplication: PropTypes.number,
  componentsInTheMostApplications: PropTypes.arrayOf(componentShape),
  componentsWithTheMostViolations: PropTypes.arrayOf(componentShape),
});
