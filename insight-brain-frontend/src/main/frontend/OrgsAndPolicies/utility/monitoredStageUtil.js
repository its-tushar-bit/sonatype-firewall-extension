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
    if (policyMonitoringOwner.policyMonitoring) {
      var theStage = getMonitoredStage(policyMonitoringOwner.policyMonitoring, stages);
      inheritOrNoMonitorOption = {
        stageName: 'Inherit from ' + parentsName + ' (' + theStage.stageName + ')',
      };
      return true;
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

export function getMonitoredStage(policyMonitoring, stages = []) {
  return stages.filter(function (stage) {
    return policyMonitoring ? stage.stageTypeId === policyMonitoring.stageTypeId : !stage.stageTypeId;
  })[0];
}
