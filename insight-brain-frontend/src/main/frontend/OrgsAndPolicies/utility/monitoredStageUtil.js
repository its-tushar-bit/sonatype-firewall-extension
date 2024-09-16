/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export function createInheritOrNoMonitorOption(policyMonitoringByOwner, stages) {
  var inheritOrNoMonitorOption, parentsName;
  policyMonitoringByOwner.some(function (policyMonitoringOwner, ownerIndex) {
    if (ownerIndex === 0) {
      return false;
    }
    if (ownerIndex === 1) {
      parentsName = policyMonitoringOwner.ownerName;
    }
    if (policyMonitoringOwner.policyMonitorings && policyMonitoringOwner.policyMonitorings.length > 0) {
      var theStage = getMonitoredStage(policyMonitoringOwner.policyMonitorings, stages);
      if (theStage) {
        inheritOrNoMonitorOption = {
          stageName: 'Inherit from ' + parentsName + ' (' + theStage.stageName + ')',
        };
        return true;
      }
    }
  });
  if (!inheritOrNoMonitorOption) {
    if (policyMonitoringByOwner.length === 1) {
      inheritOrNoMonitorOption = { stageName: 'Do not monitor' };
    } else {
      inheritOrNoMonitorOption = {
        stageName: 'Inherit from ' + parentsName + ' (Do not monitor)',
      };
    }
  }
  return inheritOrNoMonitorOption;
}

export function getMonitoredStageFromAncestors(policyMonitoringByOwner, sbomStages) {
  let appliedStage = null;

  for (const owner of policyMonitoringByOwner) {
    appliedStage = getMonitoredStage(owner.policyMonitorings, sbomStages);
    if (appliedStage) break;
  }

  return appliedStage;
}

export function getMonitoredStage(policyMonitorings, stages = []) {
  return stages.filter(function (stage) {
    return policyMonitorings.some((policyMonitoring) =>
      policyMonitoring ? stage.stageTypeId === policyMonitoring.stageTypeId : !stage.stageTypeId
    );
  })[0];
}

export function getSbomManagerMonitoredStageDetails(policyMonitoringByOwner, sbomStages, isApplication) {
  if (!policyMonitoringByOwner || policyMonitoringByOwner.length === 0 || !sbomStages || sbomStages.length === 0) {
    return null;
  }

  const textComplement = isApplication ? 'application' : 'organization and all its dependents';
  const length = policyMonitoringByOwner.length;
  let rootStage = null;

  if (length === 1) {
    const root = policyMonitoringByOwner[0];
    if (getMonitoredStage(root.policyMonitorings, sbomStages)) {
      return { label: 'Disable continuous monitoring for SBOM Manager', toggleEnabled: true };
    } else {
      return { label: 'Enable continuous monitoring for SBOM Manager', toggleEnabled: true };
    }
  }

  for (let i = 0; i < length; i++) {
    const currentOwner = policyMonitoringByOwner[i];
    const currentStage = getMonitoredStage(currentOwner.policyMonitorings, sbomStages);

    if (i === 0) {
      if (currentStage) {
        rootStage = currentStage;
      }
    } else {
      const parent = policyMonitoringByOwner[i];
      if (getMonitoredStage(parent.policyMonitorings, sbomStages)) {
        return {
          label: `Continuous Monitoring is up and running at ${parent.ownerName}, so this means it's active for this ${textComplement}.`,
          toggleEnabled: false,
        };
      }
    }
  }

  if (rootStage) {
    return { label: 'Disable continuous monitoring for SBOM Manager', toggleEnabled: true };
  } else {
    return {
      label:
        'Continuous Monitoring is currently disabled at the root organization.' +
        ` Would you like to enable it for this ${textComplement}?`,
      toggleEnabled: true,
    };
  }
}
