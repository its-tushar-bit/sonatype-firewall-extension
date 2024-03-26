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
import { NxH3, NxTab, NxTabList, NxTabPanel, NxTabs } from '@sonatype/react-shared-components';
import classnames from 'classnames';
import { indexOf } from 'ramda';

import SecurityVulnerabilityDetailsTile from './SecurityVulnerabilityDetailsTile';
import ListWaiversTable from 'MainRoot/waivers/ListWaiversTable';

// TABS
const VULNERABILITY_DETAILS = 'VULNERABILITY_DETAILS';
const APPLICABLE_WAIVERS = 'APPLICABLE_WAIVERS';

export default function ViolationPage(props) {
  const {
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
    loadApplicableWaivers,
    setSelectPolicyViolation,
    componentHash,
    tabId,
    repositoryId,
    matchState,
    pathname,
    isFirewall,
    firewallIsLoading,
    isSbomManager,
  } = props;

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

  const displayedTabs = isSecurityVulnerability ? [VULNERABILITY_DETAILS, APPLICABLE_WAIVERS] : [APPLICABLE_WAIVERS];

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
      loadApplicableWaivers(selectPolicyId);
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
              {isSecurityVulnerability && (
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
            </NxTabList>
            {isSecurityVulnerability && (
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
                  <b> Active and expired waivers applicable to this violation of {violationDetails?.policyName}</b>
                </NxH3>
                <ListWaiversTable violationDetails={violationDetails} unknownComponentName={componentDisplayName} />
              </div>
            </NxTabPanel>
          </NxTabs>
        </section>
      </LoadWrapper>
    </div>
  );
}

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
  activeWaivers: ListWaiversTable.propTypes.activeWaivers,
  componentDisplayName: PropTypes.string,
  loadFirewallViolationDetails: PropTypes.func.isRequired,
  loadApplicableWaivers: PropTypes.func.isRequired,
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
};

ViolationPage.propTypes = violationPageTypes;
