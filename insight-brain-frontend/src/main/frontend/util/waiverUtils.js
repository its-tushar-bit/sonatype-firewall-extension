/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { map, prop } from 'ramda';
import { getFutureDate } from './jsUtil';
import { STANDARD_DATE_FORMAT, formatDate } from './dateUtils';
import * as PropTypes from 'prop-types';

export const waiverMatcherStrategy = {
  ALL_COMPONENTS: 'ALL_COMPONENTS',
  ALL_VERSIONS: 'ALL_VERSIONS',
  EXACT_COMPONENT: 'EXACT_COMPONENT',
};

// Previous state names for the various routes to the Add Waiver
// and Request Waiver pages, used by the Add Waiver Page's
// Cancel and Save buttons.
export const originNamesForAddRequestPages = {
  // From CIP policy view
  APP_REPORT_CIP: 'applicationReport.policy',
  // App Report -> Component Details -> Policy Violations -> Violation Details Popover -> Manage Waivers -> Add Waiver
  APP_REPORT_VIOLATION_WAIVERS: 'applicationReport.violationWaivers',
  // App Report -> Component Details -> Policy Violations -> Violation Details Popover -> Manage Waivers dropdown, Add Waiver
  APP_REPORT_COMPONENT_DETAILS: 'applicationReport.componentDetails.violations',
  // Dashboard -> Violations -> Violation Details -> Manage Waivers dropdown -> Add Waiver
  DASHBOARD_VIOLATIONS_VIEW: 'sidebarView.violation',
  // Dashboard -> Violations -> Violation Details -> Manage Waivers -> Add Waiver
  WAIVERS_FOR_VIOLATION: 'listWaivers',
};

export const waiverExpirations = [
  { name: 'Never', value: 'never' }, // <select> doesn't handle null values, so use string instead
  { name: '7 Days', value: '7' },
  { name: '14 Days', value: '14' },
  { name: '30 Days', value: '30' },
  { name: '60 Days', value: '60' },
  { name: '90 Days', value: '90' },
  { name: '120 Days', value: '120' },
  { name: 'Custom', value: 'custom' },
];

export const getExpiryTime = (expiration) => {
  if (!expiration) {
    return null;
  }
  return getFutureDate(expiration);
};

export const displayWaiverScope = (waiver) => {
  switch (waiver.scopeOwnerType) {
    case 'root_organization': {
      return 'Root Organization';
    }
    case 'organization': {
      return `Organization - ${waiver.scopeOwnerName}`;
    }
    case 'application': {
      return `Application - ${waiver.scopeOwnerName}`;
    }
    case 'repository': {
      return `Repository - ${waiver.scopeOwnerType}`;
    }
  }
  return null;
};

export const mapWaiversScopeProp = (waiver) => ({
  ...waiver,
  scope: displayWaiverScope({
    scopeOwnerType: waiver.ownerType,
    scopeOwnerName: waiver.ownerName,
  }),
});

export const addWaiversScopeProp = (waivers) => map(mapWaiversScopeProp, waivers);

export const isWaiverAllVersions = (waiver) =>
  [waiver?.componentMatchStrategy, waiver?.matcherStrategy].some(
    (field) => field === waiverMatcherStrategy.ALL_VERSIONS
  );
export const isWaiverAllVersionsOrExact = (waiver) =>
  [waiver?.componentMatchStrategy, waiver?.matcherStrategy].some(
    (field) => field === waiverMatcherStrategy.ALL_VERSIONS || field === waiverMatcherStrategy.EXACT_COMPONENT
  );

export const convertToWaiverViolationFormat = (data) => {
  const {
    policyId,
    policyName,
    policyViolationId,
    policyThreatLevel,
    constraints,
    lastReported,
    hash,
    policyThreatCategory,
    componentDisplayName,
    componentIdentifier,
    policyOwner,
  } = data;
  return {
    ...data,
    policyId,
    policyName,
    policyViolationId,
    threatLevel: policyThreatLevel,
    constraintViolations: [
      {
        constraintId: constraints[0].constraintId,
        constraintName: constraints[0].constraintName,
        reasons: [
          {
            reason: constraints[0].conditions[0].conditionReason,
            reference: null,
          },
        ],
      },
    ],
    applicationPublicId: '',
    applicationName: '',
    organizationName: '',
    openTime: lastReported,
    fixTime: null,
    hash,
    policyThreatCategory,
    displayName: componentDisplayName,
    componentIdentifier: componentIdentifier,
    filename: null,
    stageData: {},
    policyOwner,
    waived: false,
  };
};

// Process details about a single waiver
export const formatWaiverDetails = (waiver) => {
  if (!waiver) {
    return {};
  }

  const {
    policyName,
    constraintFacts,
    expiryTime,
    creatorName,
    createTime,
    comment,
    vulnerabilityId,
    associatedPackageUrl,
    componentIdentifier,
    displayName,
    matcherStrategy,
  } = waiver;

  const { constraintName, conditionFacts } = constraintFacts[0],
    waiverScope = displayWaiverScope(waiver),
    expiration = formatDate(expiryTime, STANDARD_DATE_FORMAT) || 'Does not expire',
    dateCreated = formatDate(createTime, STANDARD_DATE_FORMAT),
    component = {
      associatedPackageUrl,
      componentIdentifier,
      displayName,
      matcherStrategy,
    };

  return {
    policyName,
    constraintName,
    reasons: map(prop('reason'), conditionFacts),
    waiverScope,
    expiration,
    comment: comment || 'None',
    creatorName,
    dateCreated,
    vulnerabilityId,
    component,
  };
};

export const waiverType = {
  policyId: PropTypes.string,
  policyName: PropTypes.string,
  policyWaiverId: PropTypes.string,
  scopeOwnerId: PropTypes.string,
  scopeOwnerName: PropTypes.string,
  scopeOwnerType: PropTypes.string,
  matcherStrategy: PropTypes.string,
  hash: PropTypes.string,
  createTime: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  creatorName: PropTypes.string,
  comment: PropTypes.string,
  constraintFacts: PropTypes.array,
  constraintFactsJson: PropTypes.string,
  componentName: PropTypes.string,
};
