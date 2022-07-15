/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { reduce } from 'ramda';

export const setAppRiskAndSortViolationsByThreat = (applicationComponents = []) => {
  let components = [...applicationComponents];

  components.forEach((component) => {
    const risk = reduce((acc, viol) => acc + viol.threatLevel, 0, component.policyViolations);

    component.risk = risk;
    component.policyViolations = component.policyViolations.sort((a, b) => b.threatLevel - a.threatLevel);
  });

  return components;
};

export const formatViolationRiskPercentage = (threatLevel, totalRisk) => {
  if (totalRisk === 0 || threatLevel === 0) {
    return '0%';
  } else {
    const riskPercent = (threatLevel / totalRisk) * 100;

    return riskPercent < 1 ? '< 1%' : `${Math.round(riskPercent)}%`;
  }
};
