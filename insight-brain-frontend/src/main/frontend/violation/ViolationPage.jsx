/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import * as PropTypes from 'prop-types';

import LoadWrapper from '../react/LoadWrapper';
import ViolationDetailsTile, { violationDetailsPropTypes } from './ViolationDetailsTile';
import { constraintViolationsPropType } from './PolicyViolationConstraintInfo';
import { capitalizeFirstLetter } from '../util/jsUtil';
import { getComponentName } from 'MainRoot/util/componentNameUtils';
import {
  NxH3,
  NxOverflowTooltip,
  NxStatefulFilterDropdown,
  NxTab,
  NxTabList,
  NxTabPanel,
  NxTabs,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import classnames from 'classnames';
import { indexOf } from 'ramda';

import SecurityVulnerabilityDetailsTile from './SecurityVulnerabilityDetailsTile';
import ListWaiversTable from 'MainRoot/waivers/ListWaiversTable';
import ListSimilarWaiversTable from 'MainRoot/waivers/ListSimilarWaiversTable';

// TABS
const VULNERABILITY_DETAILS = 'VULNERABILITY_DETAILS';
const APPLICABLE_WAIVERS = 'APPLICABLE_WAIVERS';
const SIMILAR_WAIVERS = 'SIMILAR_WAIVERS';

export default function ViolationPage(props) {
  let {
    $state,
    loadViolation,
    loadVulnerabilityDetails,
    stateGo,
    fetchStageTypes,
    loading,
    violationDetails,
    stageTypes,
    vulnerabilityDetailsLoading,
    vulnerabilityDetails,
    vulnerabilityDetailsError,
    isVulnerabilityDetailsOutdated,
    activeWaivers,
    autoWaiver,
    componentDisplayName,
    selectedViolationId,
    isFromPolicyViolations,
    isFirewallContext,
    policyViolations,
    selectPolicyId,
    loadFirewallPolicyVulnerabilityDetails,
    loadFirewallViolationDetails,
    hasPermissionForAppWaivers,
    hasEditIqPermission,
    setSelectPolicyViolation,
    componentHash,
    tabId,
    repositoryId,
    matchState,
    pathname,
    isFirewall,
    firewallIsLoading,
    isSbomManager,
    similarWaiversFilterSelectedIds,
    setFilterIdsSimilarWaivers,
    isAutoWaiversEnabled,
  } = props;

  if (isAutoWaiversEnabled && autoWaiver) {
    activeWaivers = activeWaivers.concat(autoWaiver);
  }

  const violationMissingDatabaseIdentifier = !isFirewallContext && !selectedViolationId;

  const similarWaiversFilterOptions = [
    { id: 'active', displayName: 'Active (Unexpired)' },
    { id: 'exact', displayName: 'Exact Version' },
    { id: 'comment', displayName: 'With comment' },
  ];

  const [activeTabName, setActiveTabName] = useState(VULNERABILITY_DETAILS);

  const error = props.violationDetailsError || props.stageTypesError;

  const policyDetail = selectPolicyId
    ? policyViolations?.find((item) => item.policyViolationId === selectPolicyId)
    : null;

  const detailViolations = violationDetails ? violationDetails.constraintViolations : [];

  const constraintViolations = isFirewallContext ? policyDetail.constraints : detailViolations;

  const violationLoading = isFirewallContext ? firewallIsLoading : loading || !(violationDetails && stageTypes);

  const conditionTriggerReference = isFirewallContext
    ? policyDetail.constraints[0].conditions[0].conditionTriggerReference
    : null;

  const isSecurityVulnerability =
    capitalizeFirstLetter(
      isFirewallContext ? policyDetail.policyThreatCategory : violationDetails && violationDetails.policyThreatCategory
    ) === 'Security';

  const shouldShowVulnerabilityTab = isSecurityVulnerability && !!vulnerabilityDetails;

  const displayedTabs = shouldShowVulnerabilityTab
    ? [VULNERABILITY_DETAILS, APPLICABLE_WAIVERS, SIMILAR_WAIVERS]
    : [APPLICABLE_WAIVERS, SIMILAR_WAIVERS];

  const setActiveTab = (index) => setActiveTabName(displayedTabs[index]);

  const getActiveTabIndex = () => {
    const index = indexOf(activeTabName, displayedTabs);
    return index < 0 ? 0 : index;
  };

  useEffect(() => {
    load();
  }, [selectedViolationId, conditionTriggerReference, selectPolicyId]);

  useEffect(() => {
    return () => {
      setSelectPolicyViolation(null);
    };
  }, []);

  function load() {
    if (!isFirewallContext) {
      if (selectedViolationId) {
        loadViolation(selectedViolationId);
      }
    } else {
      loadFirewallViolationDetails(selectPolicyId);
      if (conditionTriggerReference) {
        loadFirewallPolicyVulnerabilityDetails(conditionTriggerReference.value);
      }
    }
    fetchStageTypes('dashboard');
  }
  const sectionClasses = classnames('iq-tabs-section', {
    'nx-tile': !isFromPolicyViolations,
    'iq-violation-details-popover-section': isFromPolicyViolations,
  });

  return (
    <div id="violation-page">
      <InvalidViolationGuard violationMissingDatabaseIdentifier={violationMissingDatabaseIdentifier}>
        <LoadWrapper error={error} loading={violationLoading} retryHandler={load}>
          <ViolationDetailsTile
            {...{
              $state,
              stageTypes,
              violationDetails,
              stateGo,
              activeWaivers,
              selectedViolationId,
              isFromPolicyViolations,
              isFirewallContext,
              policyViolations,
              policyDetail,
              hasPermissionForAppWaivers,
              constraintViolations,
              isSbomManager,
            }}
          />

          <section className={sectionClasses}>
            <NxTabs activeTab={getActiveTabIndex()} onTabSelect={setActiveTab}>
              <NxTabList>
                {shouldShowVulnerabilityTab && (
                  <NxTab id="violation-security-vulnerability-details-tab">Vulnerability Details</NxTab>
                )}
                <NxTab id="violation-applicable-waivers-tab">
                  <div className="iq-waiver-indicator-tab">
                    {activeWaivers.length > 0 && (
                      <span className="iq-waiver-indicator__counter">{activeWaivers.length}</span>
                    )}
                    <span> Applicable Waivers </span>
                  </div>
                </NxTab>
                <NxTab id="violation-similar-waivers-tab">Similar Waivers</NxTab>
              </NxTabList>
              {shouldShowVulnerabilityTab && (
                <NxTabPanel>
                  <SecurityVulnerabilityDetailsTile
                    showTitle={false}
                    vulnerabilityDetails={vulnerabilityDetails}
                    error={vulnerabilityDetailsError}
                    isVulnerabilityDetailsOutdated={isVulnerabilityDetailsOutdated}
                    loading={vulnerabilityDetailsLoading}
                    retryLoad={loadVulnerabilityDetails}
                    componentName={violationDetails ? getComponentName(violationDetails) : null}
                    componentIdentifier={violationDetails?.componentIdentifier}
                    ownerType={isFirewallContext ? 'organization' : 'application'}
                    ownerId={isFirewallContext ? 'ROOT_ORGANIZATION_ID' : violationDetails?.applicationPublicId}
                    hasEditIqPermission={hasEditIqPermission}
                    componentHash={componentHash}
                    tabId={tabId}
                    repositoryId={repositoryId}
                    matchState={matchState}
                    pathname={pathname}
                    isFirewall={isFirewall}
                  />
                </NxTabPanel>
              )}
              <NxTabPanel>
                <div id="applicable-waivers-tile">
                  <NxH3>
                    <b> Waivers applicable to this violation of {violationDetails?.policyName}</b>
                  </NxH3>
                  <ListWaiversTable violationDetails={violationDetails} unknownComponentName={componentDisplayName} />
                </div>
              </NxTabPanel>
              <NxTabPanel>
                <div id="similar-waivers-tile">
                  <div className="similar-waivers-header">
                    <NxOverflowTooltip>
                      <NxH3 className="similar-waivers-header__title">
                        Waivers for similar violations of {violationDetails?.policyName}
                      </NxH3>
                    </NxOverflowTooltip>
                    <div className="similar-waivers-header__subtitle">
                      Across all component versions
                      {isSecurityVulnerability ? ` implicated by ${vulnerabilityDetails?.identifier}` : ''}
                    </div>
                    <NxStatefulFilterDropdown
                      className="similar-waivers-header__filter"
                      options={similarWaiversFilterOptions}
                      selectedIds={similarWaiversFilterSelectedIds}
                      onChange={setFilterIdsSimilarWaivers}
                    />
                  </div>
                  <ListSimilarWaiversTable />
                </div>
              </NxTabPanel>
            </NxTabs>
          </section>
        </LoadWrapper>
      </InvalidViolationGuard>
    </div>
  );
}

function InvalidViolationGuard({ children, violationMissingDatabaseIdentifier }) {
  if (violationMissingDatabaseIdentifier) {
    return (
      <section className="iq-violation-details-popover-section">
        <NxWarningAlert>{MISSING_VIOLATION_ID_MESSAGE_TEXT}</NxWarningAlert>
      </section>
    );
  } else {
    return <>{children}</>;
  }
}

export const MISSING_VIOLATION_ID_MESSAGE_TEXT =
  'The policy violation requested does not have a valid database identifier. We can not look up details for it. ' +
  'Try evaluating your application again to produce a new report.';

export const violationPageTypes = {
  $state: PropTypes.shape({
    get: PropTypes.func.isRequired,
    href: PropTypes.func.isRequired,
  }).isRequired,
  selectedViolationId: PropTypes.string,
  loadViolation: PropTypes.func.isRequired,
  loadVulnerabilityDetails: PropTypes.func.isRequired,
  fetchStageTypes: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  violationDetailsError: LoadWrapper.propTypes.error,
  stageTypesError: LoadWrapper.propTypes.error,
  violationDetails: PropTypes.shape({
    ...violationDetailsPropTypes,
    constraintViolations: constraintViolationsPropType.isRequired,
  }),
  stageTypes: ViolationDetailsTile.propTypes.stageTypes,
  vulnerabilityDetailsLoading: PropTypes.bool.isRequired,
  vulnerabilityDetails: PropTypes.object,
  vulnerabilityDetailsError: LoadWrapper.propTypes.error,
  isVulnerabilityDetailsOutdated: PropTypes.bool.isRequired,
  activeWaivers: PropTypes.array,
  componentDisplayName: PropTypes.string,
  loadFirewallViolationDetails: PropTypes.func.isRequired,
  isFromPolicyViolations: PropTypes.bool,
  isFirewallContext: PropTypes.bool,
  policyViolations: PropTypes.array,
  selectPolicyId: PropTypes.string,
  loadFirewallPolicyVulnerabilityDetails: PropTypes.func,
  hasPermissionForAppWaivers: PropTypes.bool,
  componentHash: PropTypes.string,
  tabId: PropTypes.string,
  repositoryId: PropTypes.string,
  matchState: PropTypes.string,
  pathname: PropTypes.string,
  isFirewall: PropTypes.bool,
  hasEditIqPermission: PropTypes.bool,
  firewallIsLoading: PropTypes.bool,
  setSelectPolicyViolation: PropTypes.func,
  isSbomManager: PropTypes.bool,
  similarWaiversFilterSelectedIds: PropTypes.object,
  setFilterIdsSimilarWaivers: PropTypes.func,
};

ViolationPage.propTypes = violationPageTypes;
