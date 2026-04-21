/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import FirewallViolationPage from './FirewallViolationPage';
import { loadFirewallPolicyVulnerabilityDetails, setFilterIdsSimilarWaivers } from 'MainRoot/violation/violationActions';
import { loadFirewallViolationDetails } from 'MainRoot/firewall/firewallActions';
import { selectSelectedPolicyViolation } from 'MainRoot/componentDetails/ViolationsTableTile/PolicyViolationsSelectors';
import {
  selectFirewallComponentDetailsPage,
  selectFirewallComponentDetailsPageRouteParams,
  selectFirewallIsLoading,
} from 'MainRoot/firewall/firewallSelectors';
import {
  selectIsFirewall,
  selectRouterCurrentParams,
  selectIsSbomManager,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectIsContainerImagesEvaluationEnabledAndProxyStage } from 'MainRoot/applicationReport/applicationReportSelectors';
import { actions as policyViolationsActions } from 'MainRoot/componentDetails/ViolationsTableTile/policyViolationsSlice';

const EMPTY_POLICY_VIOLATION = {};
const selectViolationState = (state) => state.violation ?? EMPTY_POLICY_VIOLATION;

export default function FirewallViolationPageContainer({ selectPolicyId, isFromPolicyViolations = true }) {
  const dispatch = useDispatch();
  const selectedPolicyViolation = useSelector(selectSelectedPolicyViolation);
  const firewallComponentDetailsPage = useSelector(selectFirewallComponentDetailsPage) || {};
  const firewallRouteParams = useSelector(selectFirewallComponentDetailsPageRouteParams) || {};
  const currentParams = useSelector(selectRouterCurrentParams) || {};
  const firewallIsLoading = useSelector(selectFirewallIsLoading);
  const isContainerImagesEvaluationEnabled = useSelector(selectIsContainerImagesEvaluationEnabledAndProxyStage);
  const isFirewall = useSelector(selectIsFirewall) && !isContainerImagesEvaluationEnabled;
  const isSbomManager = useSelector(selectIsSbomManager);
  const violationState = useSelector(selectViolationState);

  const policyViolations = firewallComponentDetailsPage.policyViolations?.length > 0
    ? firewallComponentDetailsPage.policyViolations
    : selectedPolicyViolation
    ? [selectedPolicyViolation]
    : [];
  const policyDetail = selectPolicyId
    ? policyViolations.find((item) => item.policyViolationId === selectPolicyId) || selectedPolicyViolation
    : selectedPolicyViolation;
  const violationDetails = Array.isArray(firewallComponentDetailsPage.violationDetails)
    ? null
    : firewallComponentDetailsPage.violationDetails;
  const componentDetails = firewallComponentDetailsPage.componentDetails || {};

  return (
    <FirewallViolationPage
      selectPolicyId={selectPolicyId}
      policyDetail={policyDetail}
      violationDetails={violationDetails}
      violationDetailsError={violationState.violationDetailsError}
      firewallIsLoading={firewallIsLoading}
      activeWaivers={violationState.activeWaivers || []}
      vulnerabilityDetailsLoading={violationState.vulnerabilityDetailsLoading || false}
      vulnerabilityDetails={violationState.vulnerabilityDetails}
      vulnerabilityDetailsError={violationState.vulnerabilityDetailsError}
      isVulnerabilityDetailsOutdated={violationState.isVulnerabilityDetailsOutdated || false}
      loadFirewallViolationDetails={(violationId) => dispatch(loadFirewallViolationDetails(violationId))}
      loadFirewallPolicyVulnerabilityDetails={(refId, componentIdentifier) =>
        dispatch(loadFirewallPolicyVulnerabilityDetails(refId, componentIdentifier))
      }
      setSelectPolicyViolation={(payload) => dispatch(policyViolationsActions.setSelectedPolicyViolation(payload))}
      componentIdentifier={componentDetails.componentIdentifier || selectedPolicyViolation?.componentIdentifier}
      componentHash={componentDetails.hash || selectedPolicyViolation?.componentHash || selectedPolicyViolation?.hash}
      tabId={currentParams.tabId || firewallRouteParams.tabId || 'violations'}
      repositoryId={firewallRouteParams.repositoryId || selectedPolicyViolation?.repositoryId}
      matchState={firewallRouteParams.matchState || selectedPolicyViolation?.matchState || selectedPolicyViolation?.matchStateId}
      pathname={firewallRouteParams.pathname || selectedPolicyViolation?.pathname}
      componentDisplayName={
        firewallRouteParams.componentDisplayName ||
        selectedPolicyViolation?.componentDisplayName ||
        selectedPolicyViolation?.componentDisplayText
      }
      hasEditIqPermission={firewallComponentDetailsPage.hasEditIqPermission || false}
      similarWaiversFilterSelectedIds={violationState.similarWaiversFilterSelectedIds}
      setFilterIdsSimilarWaivers={(filterIds) => dispatch(setFilterIdsSimilarWaivers(filterIds))}
      isFirewall={isFirewall}
      isSbomManager={isSbomManager}
      isFromPolicyViolations={isFromPolicyViolations}
    />
  );
}
