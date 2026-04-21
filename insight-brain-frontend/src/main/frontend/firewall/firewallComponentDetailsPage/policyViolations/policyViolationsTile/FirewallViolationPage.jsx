/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import { indexOf } from 'ramda';
import {
  NxH3,
  NxOverflowTooltip,
  NxStatefulFilterDropdown,
  NxTab,
  NxTabList,
  NxTabPanel,
  NxTabs,
} from '@sonatype/react-shared-components';
import LoadWrapper from 'MainRoot/react/LoadWrapper';
import { capitalizeFirstLetter } from 'MainRoot/util/jsUtil';
import { getComponentName } from 'MainRoot/util/componentNameUtils';
import SecurityVulnerabilityDetailsTile from 'MainRoot/violation/SecurityVulnerabilityDetailsTile';
import ListWaiversTable from 'MainRoot/waivers/ListWaiversTable';
import ListSimilarWaiversTable from 'MainRoot/waivers/ListSimilarWaiversTable';
import FirewallViolationDetailsTile from './FirewallViolationDetailsTile';

const VULNERABILITY_DETAILS = 'VULNERABILITY_DETAILS';
const APPLICABLE_WAIVERS = 'APPLICABLE_WAIVERS';
const SIMILAR_WAIVERS = 'SIMILAR_WAIVERS';

export default function FirewallViolationPage({
  selectPolicyId,
  policyDetail,
  violationDetails,
  violationDetailsError,
  firewallIsLoading,
  activeWaivers,
  vulnerabilityDetailsLoading,
  vulnerabilityDetails,
  vulnerabilityDetailsError,
  isVulnerabilityDetailsOutdated,
  loadFirewallViolationDetails,
  loadFirewallPolicyVulnerabilityDetails,
  setSelectPolicyViolation,
  componentIdentifier,
  componentHash,
  tabId,
  repositoryId,
  matchState,
  pathname,
  componentDisplayName,
  hasEditIqPermission,
  similarWaiversFilterSelectedIds,
  setFilterIdsSimilarWaivers,
  isFirewall,
  isSbomManager,
  isFromPolicyViolations = true,
}) {
  const [activeTabName, setActiveTabName] = useState(VULNERABILITY_DETAILS);

  const conditionTriggerReference = policyDetail?.constraints?.[0]?.conditions?.[0]?.conditionTriggerReference;
  const vulnerabilityComponentIdentifier = policyDetail?.componentIdentifier || componentIdentifier;
  const isSecurityVulnerability = capitalizeFirstLetter(policyDetail?.policyThreatCategory) === 'Security';
  const shouldShowVulnerabilityTab = isSecurityVulnerability && !!vulnerabilityDetails;
  const displayedTabs = shouldShowVulnerabilityTab
    ? [VULNERABILITY_DETAILS, APPLICABLE_WAIVERS, SIMILAR_WAIVERS]
    : [APPLICABLE_WAIVERS, SIMILAR_WAIVERS];
  const error = violationDetailsError;

  const load = () => {
    if (!selectPolicyId) {
      return;
    }

    loadFirewallViolationDetails(selectPolicyId);
    if (conditionTriggerReference) {
      loadFirewallPolicyVulnerabilityDetails(conditionTriggerReference.value, vulnerabilityComponentIdentifier);
    }
  };

  useEffect(() => {
    load();
  }, [selectPolicyId, conditionTriggerReference, vulnerabilityComponentIdentifier]);

  useEffect(() => {
    return () => {
      setSelectPolicyViolation(null);
    };
  }, []);

  const setActiveTab = (index) => setActiveTabName(displayedTabs[index]);
  const getActiveTabIndex = () => {
    const index = indexOf(activeTabName, displayedTabs);
    return index < 0 ? 0 : index;
  };
  const sectionClasses = classnames('iq-tabs-section', {
    'nx-tile': !isFromPolicyViolations,
    'iq-violation-details-popover-section': isFromPolicyViolations,
  });
  const retryLoadVulnerabilityDetails = () => {
    if (conditionTriggerReference) {
      loadFirewallPolicyVulnerabilityDetails(conditionTriggerReference.value, vulnerabilityComponentIdentifier);
    }
  };
  const policyName = policyDetail?.policyName || violationDetails?.policyName;

  return (
    <div id="firewall-violation-page">
      <LoadWrapper error={error} loading={firewallIsLoading} retryHandler={load}>
        <FirewallViolationDetailsTile
          policyDetail={policyDetail}
          violationDetails={violationDetails}
          isFromPolicyViolations={isFromPolicyViolations}
          isSbomManager={isSbomManager}
        />

        <section className={sectionClasses}>
          <NxTabs activeTab={getActiveTabIndex()} onTabSelect={setActiveTab}>
            <NxTabList>
              {shouldShowVulnerabilityTab && (
                <NxTab id="firewall-violation-security-vulnerability-details-tab">Vulnerability Details</NxTab>
              )}
              <NxTab id="firewall-violation-applicable-waivers-tab">
                <div className="iq-waiver-indicator-tab">
                  {activeWaivers.length > 0 && <span className="iq-waiver-indicator__counter">{activeWaivers.length}</span>}
                  <span> Applicable Waivers </span>
                </div>
              </NxTab>
              <NxTab id="firewall-violation-similar-waivers-tab">Similar Waivers</NxTab>
            </NxTabList>
            {shouldShowVulnerabilityTab && (
              <NxTabPanel>
                <SecurityVulnerabilityDetailsTile
                  showTitle={false}
                  vulnerabilityDetails={vulnerabilityDetails}
                  error={vulnerabilityDetailsError}
                  isVulnerabilityDetailsOutdated={isVulnerabilityDetailsOutdated}
                  loading={vulnerabilityDetailsLoading}
                  retryLoad={retryLoadVulnerabilityDetails}
                  componentName={violationDetails ? getComponentName(violationDetails) : null}
                  componentIdentifier={vulnerabilityComponentIdentifier}
                  ownerType="organization"
                  ownerId="ROOT_ORGANIZATION_ID"
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
              <div id="firewall-applicable-waivers-tile">
                <NxH3>
                  <b> Waivers applicable to this violation of {policyName}</b>
                </NxH3>
                <ListWaiversTable violationDetails={violationDetails} unknownComponentName={componentDisplayName} />
              </div>
            </NxTabPanel>
            <NxTabPanel>
              <div id="firewall-similar-waivers-tile">
                <div className="similar-waivers-header">
                  <NxOverflowTooltip>
                    <NxH3 className="similar-waivers-header__title">
                      Waivers for similar violations of {policyName}
                    </NxH3>
                  </NxOverflowTooltip>
                  <div className="similar-waivers-header__subtitle">
                    Across all component versions
                    {isSecurityVulnerability ? ` implicated by ${vulnerabilityDetails?.identifier}` : ''}
                  </div>
                  <NxStatefulFilterDropdown
                    className="similar-waivers-header__filter"
                    options={[
                      { id: 'active', displayName: 'Active (Unexpired)' },
                      { id: 'exact', displayName: 'Exact Version' },
                      { id: 'comment', displayName: 'With comment' },
                    ]}
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
    </div>
  );
}

FirewallViolationPage.propTypes = {
  selectPolicyId: PropTypes.string,
  policyDetail: PropTypes.object,
  violationDetails: PropTypes.object,
  violationDetailsError: LoadWrapper.propTypes.error,
  firewallIsLoading: PropTypes.bool.isRequired,
  activeWaivers: PropTypes.array,
  vulnerabilityDetailsLoading: PropTypes.bool.isRequired,
  vulnerabilityDetails: PropTypes.object,
  vulnerabilityDetailsError: LoadWrapper.propTypes.error,
  isVulnerabilityDetailsOutdated: PropTypes.bool,
  loadFirewallViolationDetails: PropTypes.func.isRequired,
  loadFirewallPolicyVulnerabilityDetails: PropTypes.func.isRequired,
  setSelectPolicyViolation: PropTypes.func.isRequired,
  componentIdentifier: PropTypes.object,
  componentHash: PropTypes.string,
  tabId: PropTypes.string,
  repositoryId: PropTypes.string,
  matchState: PropTypes.string,
  pathname: PropTypes.string,
  componentDisplayName: PropTypes.string,
  hasEditIqPermission: PropTypes.bool,
  similarWaiversFilterSelectedIds: PropTypes.object,
  setFilterIdsSimilarWaivers: PropTypes.func.isRequired,
  isFirewall: PropTypes.bool,
  isSbomManager: PropTypes.bool,
  isFromPolicyViolations: PropTypes.bool,
};

FirewallViolationPage.defaultProps = {
  activeWaivers: [],
  isVulnerabilityDetailsOutdated: false,
  hasEditIqPermission: false,
  isFirewall: false,
  isSbomManager: false,
};
