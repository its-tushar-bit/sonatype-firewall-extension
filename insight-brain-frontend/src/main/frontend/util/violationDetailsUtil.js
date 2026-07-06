/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as PropTypes from 'prop-types';
import { map, path, prop } from 'ramda';
import { categoryByPolicyThreatLevel } from '@sonatype/react-shared-components/util/threatLevels';
import { getArtifactName, getComponentName, getComponentNameWithoutVersion } from './componentNameUtils';

export const extractViolationDetails = (violationDetails) => {
  if (!violationDetails) {
    return {};
  }

  const {
    componentIdentifier,
    constraintViolations,
    policyName,
    policyViolationId,
    threatLevel,
    derivedComponentName,
    violationVulnerabilityId,
  } = violationDetails;

  // A waiver can outlive the violation data it was requested against: if the underlying CVE is later
  // reclassified, the violation still resolves by id but with an empty (or missing) constraintViolations
  // array. Guard the access so the waiver pages render instead of crashing (CLM-41118).
  const { constraintName, reasons = [] } = constraintViolations?.[0] ?? {},
    vulnerabilityId = violationVulnerabilityId || path([0, 'reference', 'value'], reasons),
    threatLevelCategory = categoryByPolicyThreatLevel[threatLevel],
    componentName = derivedComponentName || getComponentName(violationDetails),
    allVersionsComponentName = getComponentNameWithoutVersion(violationDetails),
    artifactName = getArtifactName(violationDetails);

  return {
    componentIdentifier,
    artifactName,
    componentName,
    allVersionsComponentName,
    constraintName,
    policyName,
    policyViolationId,
    reasons: map(prop('reason'), reasons),
    threatLevelCategory,
    vulnerabilityId,
  };
};

export const violationDetailsPropTypes = PropTypes.shape({
  policyViolationId: PropTypes.string,
  policyName: PropTypes.string,
  componentName: PropTypes.string,
  constraintName: PropTypes.string,
  reasons: PropTypes.arrayOf(
    PropTypes.shape({
      reason: PropTypes.string,
    })
  ),
});
