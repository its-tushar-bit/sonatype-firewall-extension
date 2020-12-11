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
    identificationSource,
    stageData,
    policyOwner
  } = violationDetails;

  const { constraintName, reasons } = constraintViolations[0],
      { ownerId, ownerType } = policyOwner,
      vulnerabilityId = path([0, 'reference', 'value'], reasons),
      threatLevelCategory = categoryByPolicyThreatLevel[threatLevel],
      componentName = getComponentName(violationDetails),
      artifactName = getArtifactName(violationDetails),
      scanId = Object.values(stageData)[0].mostRecentScanId,
      thirdPartyScanParameters = {
        identificationSource,
        ownerId,
        ownerType,
        scanId
      };

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
    thirdPartyScanParameters
  };
};
