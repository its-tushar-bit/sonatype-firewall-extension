/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { map, path, prop } from 'ramda';
import { categoryByPolicyThreatLevel } from '@sonatype/react-shared-components/util/threatLevels';
import { getArtifactName, getComponentName } from './componentNameUtils';

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

  const { constraintName, reasons } = constraintViolations[0],
    vulnerabilityId = violationVulnerabilityId || path([0, 'reference', 'value'], reasons),
    threatLevelCategory = categoryByPolicyThreatLevel[threatLevel],
    componentName = derivedComponentName || getComponentName(violationDetails),
    artifactName = getArtifactName(violationDetails);

  return {
    componentIdentifier,
    artifactName,
    componentName,
    constraintName,
    policyName,
    policyViolationId,
    reasons: map(prop('reason'), reasons),
    threatLevelCategory,
    vulnerabilityId,
  };
};
