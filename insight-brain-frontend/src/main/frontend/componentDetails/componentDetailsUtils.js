/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { isNil, join, map, pick, pipe, prop, sort, startsWith, toUpper } from 'ramda';

import { isNilOrEmpty } from '../util/jsUtil';
import * as PropTypes from 'prop-types';

export const processAuditRecord = (record) => ({
  user: 'anonymous', // default value, usually overridden by the `pick` below
  action: statusToActionMap[record.status] || record.status,
  detail: record.filename === 'security.json' ? createSecurityDetails(record) : createLicenseDetails(record),

  // some audit entries use null when there isn't a comment while others use a blank string.  Normalize to prevent
  // confusing sorting
  comment: record.comment || '',
  ...pick(['time', 'user'], record),
});

/**
 * The `action` column is basically based on the `status` from the backend, but some statuses
 * get mapped to different words in the display instead of being passed straight through
 */
const statusToActionMap = {
  Open: 'Reopened',
  'Not Applicable': 'Ignored',
  Overridden: 'Overrode',
};

function createSecurityDetails(record) {
  const { source, reference } = record,
    referenceIncludesSource = !isNil(source) && startsWith(toUpper(source), toUpper(reference)),
    refString = referenceIncludesSource ? reference : `${source}-${reference}`;

  return `Vulnerability ${refString}`;
}

function createLicenseDetails(record) {
  const { overriddenLicenses } = record;

  return overriddenLicenses ? `License as ${join(', ', overriddenLicenses)}` : 'License Analysis';
}

const PATHNAME_REGEX = /^(dependency:\/)?((.*?)\/)?([^/]+)$/;

// @visibleForTesting
export function parseOccurrencePathname(pathname) {
  const [
      ,
      /* overall match */ dependency /* dirname including delimiter */,
      ,
      dirname,
      originalBasename,
    ] = PATHNAME_REGEX.exec(pathname),
    isDependency = !!dependency;

  // component names which contains '/' are replaced with '\' by the Occurrence pathnames string in the backend. This is
  // to avoid considering them as part of basename. Replacing them back as how it should be after resolving base name -
  // CLM-12606
  const basename = originalBasename.replace(/\\/g, '/');

  return { isDependency, dirname, basename };
}

const sortAlerts = sort((alertA, alertB) => alertB.trigger.threatLevel - alertA.trigger.threatLevel);

const sortVulnerabilities = sort((a, b) => {
  if (a.severity === b.severity) {
    return 0;
  } else if (a.severity === null) {
    return 1;
  } else if (b.severity === null) {
    return -1;
  }
  return b.severity - a.severity;
});

/**
 * @param componentDetails as returned by /rest/ci/componentDetails/ endpoint
 * @returns versionComparisonInfoPropType to be populated in Compare Versions table in CompareVersions component
 */
export function getComponentVersionComparisonInfo(componentDetails) {
  if (componentDetails == null) {
    return {};
  }

  const sortedAlerts = sortAlerts(componentDetails.policyAlerts);

  const highestPolicyThreat = sortedAlerts.length > 0 ? sortedAlerts[0].trigger.threatLevel : 'None';
  const numberOfViolatedPolicies = componentDetails.policyAlerts.length;

  function getHighestCVSSScore() {
    if (componentDetails.identificationSource === 'Manual') {
      return 'Unavailable, Claimed Component';
    }

    const sortedVulnerabilities = sortVulnerabilities(componentDetails.securityVulnerabilities);

    return sortedVulnerabilities.length === 0
      ? 'None'
      : sortedVulnerabilities[0].severity === null
      ? 'Unscored'
      : sortedVulnerabilities[0].severity;
  }

  const effectiveLicenses = isNilOrEmpty(componentDetails.effectiveLicenses)
    ? null
    : pipe(map(prop('licenseName')), join(', '))(componentDetails.effectiveLicenses);

  const version = componentDetails.componentIdentifier?.coordinates?.version || componentDetails.version;

  return {
    version,
    highestPolicyThreat,
    numberOfViolatedPolicies,
    highestCVSSScore: getHighestCVSSScore(),
    effectiveLicenseStatus: componentDetails.effectiveLicenseStatus,
    effectiveLicenses,
    integrityRating: componentDetails.integrityRating,
    hygieneRating: componentDetails.hygieneRating,
    policyMaxThreatLevelsByCategory: componentDetails.policyMaxThreatLevelsByCategory,
    catalogDate: componentDetails.catalogDate,
    policyEvaluationTimestamps: componentDetails.policyEvaluationTimestamps,
  };
}

export const createTabConfiguration = (tabId, title, component) => ({
  tabId,
  title,
  component,
});

export const isUnknownComponent = (componentDetails) => componentDetails?.matchState === 'unknown';
export const isExactComponent = (componentDetails) => componentDetails?.matchState === 'exact';
export const isClaimedComponent = (componentDetails) => componentDetails?.identificationSource === 'Manual';

export const versionComparisonInfoPropType = PropTypes.shape({
  version: PropTypes.string,
  highestPolicyThreat: PropTypes.oneOfType([PropTypes.number, PropTypes.oneOf(['None'])]),
  numberOfViolatedPolicies: PropTypes.number,
  highestCVSSScore: PropTypes.oneOfType([
    PropTypes.number,
    PropTypes.oneOf(['None', 'Unscored', 'Unavailable, Claimed Component']),
  ]),
  effectiveLicenseStatus: PropTypes.string,
  effectiveLicenses: PropTypes.string,
  integrityRating: PropTypes.shape({ id: PropTypes.number, label: PropTypes.string }),
  hygieneRating: PropTypes.shape({ id: PropTypes.number, label: PropTypes.string }),
});
