/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import moment from 'moment';
import { getFutureDate } from 'MainRoot/util/jsUtil';

export const waiverMatcherStrategy = {
  ALL_COMPONENTS: 'ALL_COMPONENTS',
  ALL_VERSIONS: 'ALL_VERSIONS',
  EXACT_COMPONENT: 'EXACT_COMPONENT',
};

const firewallWaiverExpirations = [
  { name: 'Never', value: 'never' },
  { name: '7 Days', value: '7' },
  { name: '14 Days', value: '14' },
  { name: '30 Days', value: '30' },
  { name: '60 Days', value: '60' },
  { name: '90 Days', value: '90' },
  { name: '120 Days', value: '120' },
  { name: 'Custom', value: 'custom' },
];

export const useFirewallWaiverExpirations = (isExpireWhenRemediationAvailableEnabled) => {
  return isExpireWhenRemediationAvailableEnabled
    ? firewallWaiverExpirations.concat([{ name: 'When Remediation Available', value: 'remediationAvailable' }])
    : firewallWaiverExpirations;
};

export const isCustomExpiryTimeValid = (value) => {
  if (!value) {
    return false;
  }
  return new Date(value) > new Date();
};

export const isCustomExpiryTimeSelected = (expiryTime) => expiryTime === 'custom';

export const isNeverExpiryTimeSelected = (expiryTime) => expiryTime === 'never' || expiryTime === null;

export const isExpireWhenRemediationAvailableSelected = (expiryTime) => expiryTime === 'remediationAvailable';

export const getExpiryTime = (expiration) => {
  if (!expiration) {
    return null;
  }
  return getFutureDate(expiration);
};

export const getExpirationDaysMessage = (expiryTime, customExpiryTime) => {
  if (isCustomExpiryTimeSelected(expiryTime) && isCustomExpiryTimeValid(customExpiryTime?.value)) {
    const today = moment().startOf('day');
    const customDate = moment(customExpiryTime.value, 'YYYY-MM-DD');
    const diff = Math.floor(moment.duration(customDate.diff(today)).asDays());
    return `This waiver will expire in ${diff} days`;
  }
  if (
    !isCustomExpiryTimeSelected(expiryTime) &&
    !isNeverExpiryTimeSelected(expiryTime) &&
    !isExpireWhenRemediationAvailableSelected(expiryTime)
  ) {
    return `This waiver will expire in ${expiryTime} days`;
  }
  if (isExpireWhenRemediationAvailableSelected(expiryTime)) {
    return 'This waiver will expire when an upgrade that fixes the violation is available';
  }
  return '';
};
export const formatCustomDate = (date) => {
  if (date && moment(date).isValid()) {
    return moment(date).format('YYYY-MM-DD');
  }
  return '';
};

export const formatFirewallComponentForWaiver = (component) => {
  const {
    policyName,
    policyId,
    policyViolationId,
    threatLevel,
    pathname,
    displayName,
    componentIdentifier,
    matchState,
    constraints,
  } = component;

  return {
    policyName,
    policyId,
    policyViolationId,
    policyThreatLevel: threatLevel,
    pathname,
    displayName: displayName || componentIdentifier?.coordinates,
    componentIdentifier,
    matchState,
    constraints,
    constraintViolations: constraints?.map((constraint) => ({
      constraintId: constraint.constraintId,
      constraintName: constraint.constraintName,
      reasons: constraint.conditions?.map((condition) => ({
        reason: condition.conditionReason,
        reference: null,
      })),
    })),
  };
};

export const buildFirewallBulkWaiverPayload = ({
  selectedViolations,
  waiverReasonId,
  expiryTime,
  customExpiryTime,
  comments,
  componentMatcherStrategy,
  selectedWaiverScope,
}) => {
  let expiryTimeValue = null;
  if (isCustomExpiryTimeSelected(expiryTime)) {
    expiryTimeValue = customExpiryTime?.value || null;
  } else if (!isNeverExpiryTimeSelected(expiryTime)) {
    expiryTimeValue = getExpiryTime(parseInt(expiryTime, 10));
  }

  return {
    violations: selectedViolations.map(formatFirewallComponentForWaiver),
    waiverReasonId,
    expiryTime: expiryTimeValue,
    comment: comments,
    componentMatcherStrategy,
    scopeOwnerId: selectedWaiverScope?.id,
    scopeOwnerType: selectedWaiverScope?.label,
  };
};

export const validateFirewallBulkWaiverConfig = (config) => {
  const errors = [];

  if (!config.waiverReasonId) {
    errors.push('Waiver reason is required');
  }

  if (!config.expiryTime) {
    errors.push('Expiration time is required');
  }

  if (isCustomExpiryTimeSelected(config.expiryTime) && !isCustomExpiryTimeValid(config.customExpiryTime?.value)) {
    errors.push('Custom expiry date must be in the future');
  }

  if (!config.componentMatcherStrategy) {
    errors.push('Component matcher strategy is required');
  }

  if (!config.selectedWaiverScope) {
    errors.push('Waiver scope is required');
  }

  if (!config.selectedViolations || config.selectedViolations.length === 0) {
    errors.push('At least one violation must be selected');
  }

  return {
    isValid: errors.length === 0,
    errors,
  };
};

export const displayFirewallRepositoryScope = (scope) => {
  if (!scope) {
    return '';
  }

  switch (scope.label) {
    case 'Repository_container':
      return scope.name;
    case 'repository':
      return `Repository - ${scope.name}`;
    case 'repository_manager':
      return `Repository Manager - ${scope.name}`;
    default:
      return `${scope.label} - ${scope.name}`;
  }
};
