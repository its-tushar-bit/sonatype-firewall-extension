/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';
import { selectRouterCurrentParams } from '../../reduxUiRouter/routerSelectors';

import { selectSelectedComponent } from '../../applicationReport/applicationReportSelectors';

export const selectComponentDetailsViolationsSlice = prop('componentDetailsPolicyViolations');

const violationsSlice = createSelector(selectComponentDetailsViolationsSlice, prop('violations'));
const violationTypeSlice = createSelector(selectComponentDetailsViolationsSlice, prop('violationType'));
export const selectComponentViolations = createSelector(
  violationsSlice,
  violationTypeSlice,
  (violations, violationType) => {
    if (violations && violationType) {
      return violations.filter((violation) => violation.policyThreatCategory === violationType);
    } else {
      return violations;
    }
  }
);

export const selectComponentWaivers = createSelector(selectComponentDetailsViolationsSlice, prop('waivers'));

export const selectSelectedViolationId = createSelector(
  selectComponentDetailsViolationsSlice,
  selectRouterCurrentParams,
  (componentDetailsPolicyViolations, routerCurrentParams) => {
    return routerCurrentParams.id || componentDetailsPolicyViolations.selectedPolicyViolationId;
  }
);

export const selectSelectedComponentPolicyViolationId = createSelector(
  selectComponentDetailsViolationsSlice,
  prop('selectedPolicyViolationId')
);

export const selectSelectedViolationDetail = createSelector(
  selectSelectedComponentPolicyViolationId,
  selectSelectedComponent,
  selectComponentViolations,
  (selectedPolicyViolationId, selectedComponent, violations = []) => {
    if (!selectedPolicyViolationId) {
      return null;
    }

    return violationToWaiverOperationViolationDetailAdapter(
      violations.find((violation) => violation.policyViolationId === selectedPolicyViolationId),
      selectedComponent.derivedComponentName
    );
  }
);

const violationToWaiverOperationViolationDetailAdapter = (violation, derivedComponentName) => {
  if (!violation) {
    return null;
  }

  const { policyViolationId, policyName, policyThreatLevel, constraints } = violation;
  const { constraintName, conditions = [] } = constraints[0];
  const violationVulnerabilityId =
    conditions.length && conditions[0].conditionTriggerReference ? conditions[0].conditionTriggerReference.value : null;
  const reasons = conditions.map((condition) => ({ reason: condition.conditionReason }));

  return {
    threatLevel: policyThreatLevel,
    constraintViolations: [{ constraintName, reasons }],
    policyViolationId,
    policyName,
    derivedComponentName,
    violationVulnerabilityId,
  };
};
