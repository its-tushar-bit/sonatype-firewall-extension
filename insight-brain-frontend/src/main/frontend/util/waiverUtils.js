/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { map, prop, equals, comparator, tryCatch, F, cond, T, omit, isNil, any } from 'ramda';
import { getFutureDate } from './jsUtil';
import { STANDARD_DATE_FORMAT, formatDate } from './dateUtils';
import { isUnknownComponent } from 'MainRoot/util/componentNameUtils';
import * as PropTypes from 'prop-types';
import moment from 'moment';

export const waiverMatcherStrategy = {
  ALL_COMPONENTS: 'ALL_COMPONENTS',
  ALL_VERSIONS: 'ALL_VERSIONS',
  EXACT_COMPONENT: 'EXACT_COMPONENT',
};

export const waiverRequestStatus = {
  REQUESTED: 'REQUESTED',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
};

// Previous state names for the various routes to the Add Waiver
// and Request Waiver pages, used by the Add Waiver Page's
// Cancel and Save buttons.
export const originNamesForAddRequestPages = {
  // From CIP policy view
  APP_REPORT_CIP: 'applicationReport.policy',
  // App Report -> Component Details -> Policy Violations -> Violation Details Popover -> Add Waiver
  APP_REPORT_COMPONENT_DETAILS: 'applicationReport.componentDetails.violations',
  // App Report -> Component Details -> Security Violations -> Violation Details Popover -> Add Waiver
  APP_REPORT_COMPONENT_DETAILS_SECURITY: 'applicationReport.componentDetails.security',
  // App Report -> Component Details -> Legal Violations -> Violation Details Popover -> Add Waiver
  APP_REPORT_COMPONENT_DETAILS_LEGAL: 'applicationReport.componentDetails.legal',
  // Firewall -> Component Details -> Policy Violations -> Violation Details Popover -> Add Waiver
  FIREWALL_COMPONENT_DETAILS: 'firewall.componentDetailsPage.violations',
  // Firewall -> Component Details -> Security Violations -> Violation Details Popover -> Add Waiver
  FIREWALL_COMPONENT_DETAILS_SECURITY: 'firewall.componentDetailsPage.security',
  // Firewall -> Component Details -> Legal Violations -> Violation Details Popover -> Add Waiver
  FIREWALL_COMPONENT_DETAILS_LEGAL: 'firewall.componentDetailsPage.legal',
  // Firewall -> Bulk Waive -> Add Waiver (redirects to repository results page)
  FIREWALL_BULK_WAIVE: 'firewall.bulkWaive',
  // Repository -> Component Details -> Policy Violations -> Violation Details Popover -> Add Waiver
  REPOSITORY_COMPONENT_DETAILS: 'repository.componentDetailsPage.violations',
  // Repository -> Component Details -> Security Violations -> Violation Details Popover -> Add Waiver
  REPOSITORY_COMPONENT_DETAILS_SECURITY: 'repository.componentDetailsPage.security',
  // Repository -> Component Details -> Legal Violations -> Violation Details Popover -> Add Waiver
  REPOSITORY_COMPONENT_DETAILS_LEGAL: 'repository.componentDetailsPage.legal',
  // Dashboard -> Violations -> Violation Details -> Add Waiver
  DASHBOARD_VIOLATIONS_VIEW: 'sidebarView.violation',
  FIREWALL_VIOLATION_WAIVERS: 'firewall.componentDetailsPage',
  REPOSITORY_VIOLATION_WAIVERS: 'repository.componentDetailsPage',
  //PRIORITIES-PAGE ORIGINS
  DASHBOARD_PRIORITIES_PAGE: 'developer.dashboard',
  // Dashboard -> Waivers -> Requested Waivers
  DASHBOARD_WAIVERS_REQUESTS_VIEW: 'dashboard.overview.waiverRequests',
  //Policy Violations Tab
  // Side Nav -> Reports -> Priorities -> Component Details -> Policy Violations -> Violation Details Popover -> Add Waiver
  CDP_WITHIN_PRIORITIES_PAGE_FROM_REPORTS:
    'componentDetailsPageWithinPrioritiesPageContainerFromReports.componentDetails.violations',
  // Side Nav -> Developer -> View -> Component Details -> Policy Violations -> Violation Details Popover -> Add Waiver
  CDP_WITHIN_PRIORITIES_PAGE_FROM_DEVELOPER_DASHBOARD:
    'componentDetailsPageWithinPrioritiesPageContainerFromDashboard.componentDetails.violations',
  // Integrations -> View -> Component Details -> Policy Violations -> Violation Details Popover -> Add Waiver
  CDP_WITHIN_PRIORITIES_PAGE_FROM_INTEGRATIONS:
    'componentDetailsPageWithinPrioritiesPageContainerFromIntegrations.componentDetails.violations',

  //Security Tab
  // Side Nav -> Reports -> Priorities -> Component Details -> Security -> Violation Details Popover -> Add Waiver
  CDP_WITHIN_PRIORITIES_PAGE_FROM_REPORTS_SECURITY:
    'componentDetailsPageWithinPrioritiesPageContainerFromReports.componentDetails.security',
  // Side Nav -> Developer -> View -> Component Details -> Security -> Violation Details Popover -> Add Waiver
  CDP_WITHIN_PRIORITIES_PAGE_FROM_DEVELOPER_DASHBOARD_SECURITY:
    'componentDetailsPageWithinPrioritiesPageContainerFromDashboard.componentDetails.security',
  // Integrations -> View -> Component Details -> Security -> Violation Details Popover -> Add Waiver
  CDP_WITHIN_PRIORITIES_PAGE_FROM_INTEGRATIONS_SECURITY:
    'componentDetailsPageWithinPrioritiesPageContainerFromIntegrations.componentDetails.security',

  //Legal Tab
  // Side Nav -> Reports -> Priorities -> Component Details -> Legal -> Violation Details Popover -> Add Waiver
  CDP_WITHIN_PRIORITIES_PAGE_FROM_REPORTS_LEGAL:
    'componentDetailsPageWithinPrioritiesPageContainerFromReports.componentDetails.legal',
  // Side Nav -> Developer -> View -> Component Details -> Legal -> Violation Details Popover -> Add Waiver
  CDP_WITHIN_PRIORITIES_PAGE_FROM_DEVELOPER_DASHBOARD_LEGAL:
    'componentDetailsPageWithinPrioritiesPageContainerFromDashboard.componentDetails.legal',
  // Integrations -> View -> Component Details -> Legal -> Violation Details Popover -> Add Waiver
  CDP_WITHIN_PRIORITIES_PAGE_FROM_INTEGRATIONS_LEGAL:
    'componentDetailsPageWithinPrioritiesPageContainerFromIntegrations.componentDetails.legal',
};

const waiverExpirations = [
  { name: 'Never', value: 'never' }, // <select> doesn't handle null values, so use string instead
  { name: '7 Days', value: '7' },
  { name: '14 Days', value: '14' },
  { name: '30 Days', value: '30' },
  { name: '60 Days', value: '60' },
  { name: '90 Days', value: '90' },
  { name: '120 Days', value: '120' },
  { name: 'Custom', value: 'custom' },
];

export const useWaiverExpirations = (isExpireWhenRemediationAvailableWaiversEnabled) => {
  return isExpireWhenRemediationAvailableWaiversEnabled
    ? waiverExpirations.concat([{ name: 'When Remediation Available', value: 'remediationAvailable' }])
    : waiverExpirations;
};

export const isCustomExpiryTimeValid = (value) => {
  if (!value) {
    return false;
  }
  return new Date(value) > new Date();
};

/**
 * Returns true when the waiver expiry date is strictly before today (yesterday or earlier).
 * A waiver expiring today is NOT considered expired (canonical rule: today = 0 days remaining).
 */
export const isWaiverExpired = (expiryTime) =>
  expiryTime != null && moment(expiryTime).startOf('day').isBefore(moment().startOf('day'));

/**
 * Returns the number of whole days until expiry, using day boundaries.
 * Today → 0, tomorrow → 1, yesterday → -1.
 * Returns null when expiryTime is absent, for auto-waivers, or for remediation-available waivers.
 */
export const getWaiverDaysRemaining = (expiryTime, isAutoWaiver, isExpireWhenRemediationAvailable) => {
  if (expiryTime == null || isAutoWaiver || isExpireWhenRemediationAvailable) return null;
  return Math.floor(moment(expiryTime).startOf('day').diff(moment().startOf('day'), 'days'));
};

export const getExpiryTime = (expiration) => {
  if (!expiration) {
    return null;
  }
  return getFutureDate(expiration);
};

export const isCustomExpiryTimeSelected = (expiryTime) => expiryTime === 'custom';

export const isNeverExpiryTimeSelected = (expiryTime) => expiryTime === 'never' || expiryTime === null;

export const isExpireWhenRemediationAvailableSelected = (expiryTime) => expiryTime === 'remediationAvailable';

export const getExpirationDaysMessage = (expiryTime, customExpiryTime) => {
  if (isCustomExpiryTimeSelected(expiryTime) && isCustomExpiryTimeValid(customExpiryTime.value)) {
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
      return `Repository - ${waiver.scopeOwnerName}`;
    }
    case 'repository_manager': {
      return `Repository Manager - ${waiver.scopeOwnerName}`;
    }
    case 'all_repositories':
    case 'repository_container': {
      return waiver.scopeOwnerName;
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
export const isWaiverExact = (waiver) =>
  [waiver?.componentMatchStrategy, waiver?.matcherStrategy].some(
    (field) => field === waiverMatcherStrategy.EXACT_COMPONENT
  );

export const shouldShowUpgradeIndicator = (upgradeAvailable, waiver) =>
  upgradeAvailable && isWaiverExact(waiver) && !isUnknownComponent(waiver);

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
    componentUpgradeAvailable,
    reasonText,
  } = waiver;

  const { constraintName, conditionFacts } = constraintFacts?.[0] ?? {},
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
    reasons: map(prop('reason'), conditionFacts || []),
    waiverScope,
    expiration,
    comment: comment || 'None',
    creatorName,
    dateCreated,
    vulnerabilityId,
    component,
    componentUpgradeAvailable,
    reasonText: reasonText || '--',
  };
};

const idComparator = comparator((a, b) => a.constraintId < b.constraintId);
const conditionTypeComparator = comparator((a, b) => a.conditionTypeId < b.conditionTypeId);
const triggerJsonComparator = comparator((a, b) => a.triggerJson < b.triggerJson);
const arraysHaveSameLength = (array1, arra2) => equals(array1?.length, arra2?.length);

// There are cases where the backend sets triggerJson = null and conditionIndex = 0 to policy violations
// However, the waiver data always includes them, which causes faulty comparisons, so we omit these from comparison
// See removeDataUnnecessaryForPolicyAlert in PolicyAlertUtil.java
const getNonMissingProps = omit(['triggerJson', 'conditionIndex']);

// If triggerJson is null, we compare the rest of the object for equality
const equalProps = (a, b) => {
  return any(isNil, [a.triggerJson, b.triggerJson])
    ? equals(getNonMissingProps(a), getNonMissingProps(b))
    : equals(a, b);
};

const sortConstraintFacts = (constraintFacts) => {
  return constraintFacts.sort(idComparator);
};

const sortConditionFacts = (conditionFacts) => {
  return conditionFacts.sort(conditionTypeComparator).sort(triggerJsonComparator);
};

const constraintFactsCondition = cond([
  [
    arraysHaveSameLength,
    (constraintFacts1, constraintFacts2) =>
      constraintFacts1.every((constraintFact, i) => {
        constraintFact.conditionFacts = sortConditionFacts(constraintFact.conditionFacts);
        constraintFacts2[i].conditionFacts = sortConditionFacts(constraintFacts2[i].conditionFacts);

        return (
          idComparator(constraintFact, constraintFacts2[i]) === 0 &&
          constraintFact?.conditionFacts.every((conditionFact, j) =>
            equalProps(conditionFact, constraintFacts2[i].conditionFacts[j])
          )
        );
      }),
  ],
  [T, F],
]);

const matchesConstraintFacts = tryCatch((waiver, violation) => {
  const violationConstraintFacts = sortConstraintFacts(JSON.parse(violation.constraintFactsJson));
  const waiverConstraintFacts = sortConstraintFacts(JSON.parse(waiver.constraintFactsJson));

  return constraintFactsCondition(violationConstraintFacts, waiverConstraintFacts);
}, F);

const matchesConstraintFactsSimple = (waiver, violation) =>
  waiver.constraintFactsJson != null && equals(waiver.constraintFactsJson, violation.constraintFactsJson);

/**
 * Populate the applicableWaivers property for all the provided violations,
 * to identify the applicable waivers first try to compare the constraint facts as plain json
 * if that fails perform a more in depth comparisson similar to the backend
 *
 * @param {*} componentWaivers
 * @param {*} allViolations
 * @param {*} expiredComponentWaivers
 * @returns A list of violations with applicable waivers
 */
export const populateViolationsWithApplicableWaivers = (
  componentWaivers,
  allViolations,
  expiredComponentWaivers = []
) => {
  // the waivers are already filtered for the component so there's no need for a hash matcher
  const matchesPolicyId = (waiver, violation) => waiver.policyId === violation.policyId;

  const waiverIsApplicableToViolation = (waiver, violation) => {
    return (
      matchesPolicyId(waiver, violation) &&
      (matchesConstraintFactsSimple(waiver, violation) || matchesConstraintFacts(waiver, violation))
    );
  };

  return allViolations?.map((violation) => ({
    ...violation,
    applicableWaivers: componentWaivers
      .filter((waiver) => waiverIsApplicableToViolation(waiver, violation))
      .map((waiver) => waiver.policyWaiverId),
    expiredWaivers: expiredComponentWaivers.filter((waiver) => waiverIsApplicableToViolation(waiver, violation)),
  }));
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
