/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export const violationsSortFields = {
  APPLICATION_NAME: 'APPLICATION_NAME',
  COMPONENT_NAME: 'COMPONENT_NAME',
  POLICY_NAME: 'POLICY_NAME',
  THREAT_LEVEL: 'THREAT_LEVEL',
  AGE: 'AGE',
};

export const applicationsSortFields = {
  NAME: 'NAME',
  TOTAL_RISK: 'TOTAL_RISK',
  CRITICAL_RISK: 'CRITICAL_RISK',
  SEVERE_RISK: 'SEVERE_RISK',
  MODERATE_RISK: 'MODERATE_RISK',
  LOW_RISK: 'LOW_RISK',
};

export const componentsSortFields = {
  NAME: 'NAME',
  NUMBER_OF_AFFECTED_APPS: 'NUMBER_OF_AFFECTED_APPS',
  TOTAL_RISK: 'TOTAL_RISK',
  CRITICAL_RISK: 'CRITICAL_RISK',
  SEVERE_RISK: 'SEVERE_RISK',
  MODERATE_RISK: 'MODERATE_RISK',
  LOW_RISK: 'LOW_RISK',
};

export const waiversSortFields = {
  COMPONENT_SCOPE: 'COMPONENT_SCOPE',
  CREATION_DATE: 'CREATION_DATE',
  EXPIRATION_DATE: 'EXPIRATION_DATE',
  OWNER_SCOPE: 'OWNER_SCOPE',
  POLICY_NAME: 'POLICY_NAME',
  THREAT_LEVEL: 'THREAT_LEVEL',
};

const translateSortFields = (fieldsMap) => (sortFields) => {
  return sortFields.map((field) => {
    if (field.substr(0, 1) === '-') {
      return '-' + fieldsMap[field.substr(1)];
    }
    return fieldsMap[field];
  });
};

export const translateViolationsSortFields = translateSortFields({
  applicationName: violationsSortFields.APPLICATION_NAME,
  derivedComponentName: violationsSortFields.COMPONENT_NAME,
  policyName: violationsSortFields.POLICY_NAME,
  threatLevel: violationsSortFields.THREAT_LEVEL,
  firstOccurrenceTime: violationsSortFields.AGE,
});

export const translateApplicationsSortFields = translateSortFields({
  applicationName: applicationsSortFields.NAME,
  'totalApplicationRisk.totalRisk': applicationsSortFields.TOTAL_RISK,
  'totalApplicationRisk.criticalRisk': applicationsSortFields.CRITICAL_RISK,
  'totalApplicationRisk.severeRisk': applicationsSortFields.SEVERE_RISK,
  'totalApplicationRisk.moderateRisk': applicationsSortFields.MODERATE_RISK,
  'totalApplicationRisk.lowRisk': applicationsSortFields.LOW_RISK,
});

export const translateComponentsSortFields = translateSortFields({
  derivedComponentName: componentsSortFields.NAME,
  affectedApplications: componentsSortFields.NUMBER_OF_AFFECTED_APPS,
  score: componentsSortFields.TOTAL_RISK,
  scoreCritical: componentsSortFields.CRITICAL_RISK,
  scoreSevere: componentsSortFields.SEVERE_RISK,
  scoreModerate: componentsSortFields.MODERATE_RISK,
  scoreLow: componentsSortFields.LOW_RISK,
});

export const translateWaiversSortFields = translateSortFields({
  component: waiversSortFields.COMPONENT_SCOPE,
  createTime: waiversSortFields.CREATION_DATE,
  expiryTime: waiversSortFields.EXPIRATION_DATE,
  scope: waiversSortFields.OWNER_SCOPE,
  policyName: waiversSortFields.POLICY_NAME,
  threatLevel: waiversSortFields.THREAT_LEVEL,
});
