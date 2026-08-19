/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { pick } from 'ramda';

import {
  loadViolation,
  loadVulnerabilityDetails,
  loadFirewallPolicyVulnerabilityDetails,
  setFilterIdsSimilarWaivers,
} from './violationActions';
import { stateGo } from '../reduxUiRouter/routerActions';
import { fetchStageTypes } from '../stages/stagesActions';
import ViolationPage from './ViolationPage';
import { selectSelectedViolationId } from '../componentDetails/ViolationsTableTile/PolicyViolationsSelectors';
import { loadFirewallViolationDetails } from '../firewall/firewallActions';
import { selectComponentDetails } from 'MainRoot/componentDetails/componentDetailsSelectors';
import { getMostRecentScanId } from './violationSelectors';
import {
  selectIsFirewall,
  selectRouterCurrentParams,
  selectIsSbomManager,
} from 'MainRoot/reduxUiRouter/routerSelectors';

import { selectIsAutoWaiversEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  selectIsContainerImagesEvaluationEnabledAndProxyStage,
  selectIsFirewallOrRepositoryAndNotProxyStage,
} from 'MainRoot/applicationReport/applicationReportSelectors';

import {
  selectFirewallComponentDetailsPage,
  selectFirewallComponentDetailsPageRouteParams,
  selectFirewallIsLoading,
} from 'MainRoot/firewall/firewallSelectors';

import { actions } from 'MainRoot/componentDetails/ViolationsTableTile/policyViolationsSlice';

function mapStateToProps(state, props) {
  const isContainerImagesEvaluationEnabled = selectIsContainerImagesEvaluationEnabledAndProxyStage(state);

  const { stages, violation } = state;
  const isFirewall = selectIsFirewall(state) && !isContainerImagesEvaluationEnabled;
  const isFirewallOrRepository = selectIsFirewallOrRepositoryAndNotProxyStage(state);

  const firewallComponentDetailsPage = selectFirewallComponentDetailsPage(state);
  const { hasEditIqPermission: firewallHasEditIqPermission } = firewallComponentDetailsPage;
  const applicationHasEditPermission = pick(['hasEditIqPermission'], violation)?.hasEditIqPermission;
  const firewallIsLoading = selectFirewallIsLoading(state);

  const stageData = stages.dashboard;
  const selectPolicyId = props.selectPolicyId;
  const firewallPolicyViolations = firewallComponentDetailsPage.policyViolations;
  const componentApplicationDetails = selectComponentDetails(state);
  const { tabId, scanId } = selectRouterCurrentParams(state);
  const firewallComponentDetailsPageParams = selectFirewallComponentDetailsPageRouteParams(state);
  const isSbomManager = selectIsSbomManager(state);
  const isAutoWaiversEnabled = selectIsAutoWaiversEnabled(state);

  const getFirewallOrRepositoryViolationDetails = () =>
    !Array.isArray(firewallComponentDetailsPage?.violationDetails)
      ? firewallComponentDetailsPage?.violationDetails
      : null;
  const violationDetails = isFirewallOrRepository
    ? getFirewallOrRepositoryViolationDetails()
    : violation?.violationDetails;

  return {
    ...pick(
      [
        'loading',
        'violationDetailsError',
        'vulnerabilityDetailsLoading',
        'vulnerabilityDetails',
        'vulnerabilityDetailsError',
        'activeWaivers',
        'expiredWaivers',
        'addWaiverPermission',
        'addWaiverPermissionLoading',
        'addWaiverPermissionError',
        'hasPermissionForAppWaivers',
        'isVulnerabilityDetailsOutdated',
        'similarWaiversFilterSelectedIds',
        'autoWaiver',
      ],
      violation
    ),
    violationDetails,
    hasEditIqPermission: isFirewallOrRepository ? firewallHasEditIqPermission : applicationHasEditPermission,
    stageTypes: stageData.stageTypes,
    stageTypesError: stageData.error,
    selectedViolationId: selectSelectedViolationId(state),
    isFirewallContext: isFirewallOrRepository && !!selectPolicyId,
    policyViolations: isFirewallOrRepository ? firewallPolicyViolations : [],
    selectPolicyId: selectPolicyId,
    componentHash: isFirewallOrRepository
      ? firewallComponentDetailsPage?.componentDetails?.hash
      : componentApplicationDetails?.hash,
    tabId,
    scanId: scanId || getMostRecentScanId(violationDetails?.stageData),
    repositoryId: isFirewallOrRepository ? firewallComponentDetailsPageParams.repositoryId : null,
    matchState: isFirewallOrRepository ? firewallComponentDetailsPageParams.matchState : null,
    pathname: isFirewallOrRepository ? firewallComponentDetailsPageParams.pathname : null,
    componentDisplayName: isFirewallOrRepository ? firewallComponentDetailsPageParams.componentDisplayName : null,
    isFirewall,
    isContainerImagesEvaluationEnabled,
    firewallIsLoading,
    isSbomManager,
    isAutoWaiversEnabled,
  };
}

const mapDispatchToProps = {
  loadViolation,
  loadVulnerabilityDetails,
  loadFirewallPolicyVulnerabilityDetails,
  fetchStageTypes,
  stateGo,
  loadFirewallViolationDetails: loadFirewallViolationDetails,
  setFilterIdsSimilarWaivers,
  setSelectPolicyViolation: actions.setSelectedPolicyViolation,
};

const ViolationPageContainer = connect(mapStateToProps, mapDispatchToProps)(ViolationPage);
export default ViolationPageContainer;
