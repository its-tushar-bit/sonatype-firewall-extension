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
import {
  selectComponentDetailsViolationsSlice,
  selectSelectedViolationId,
} from '../componentDetails/ViolationsTableTile/PolicyViolationsSelectors';
import { loadFirewallViolationDetails } from '../firewall/firewallActions';
import { selectComponentDetails } from 'MainRoot/componentDetails/componentDetailsSelectors';
import {
  selectIsFirewall,
  selectRouterCurrentParams,
  selectIsFirewallOrRepository,
  selectIsSbomManager,
} from 'MainRoot/reduxUiRouter/routerSelectors';

import { selectIsAutoWaiversEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';

import {
  selectFirewallComponentDetailsPage,
  selectFirewallComponentDetailsPageRouteParams,
  selectFirewallIsLoading,
} from 'MainRoot/firewall/firewallSelectors';
import { actions } from 'MainRoot/componentDetails/ViolationsTableTile/policyViolationsSlice';

function mapStateToProps(state, props) {
  const { stages, violation } = state;
  const isFirewall = selectIsFirewall(state);
  const isFirewallOrRepository = selectIsFirewallOrRepository(state);
  const firewallComponentDetailsPage = selectFirewallComponentDetailsPage(state);
  const { hasEditIqPermission: firewallHasEditIqPermission } = firewallComponentDetailsPage;
  const applicationHasEditPermission = pick(['hasEditIqPermission'], violation)?.hasEditIqPermission;
  const firewallIsLoading = selectFirewallIsLoading(state);

  const stageData = stages.dashboard;
  const selectPolicyId = props.selectPolicyId;
  const firewallPolicyViolations = firewallComponentDetailsPage.policyViolations;
  const applicationPolicyViolations = selectComponentDetailsViolationsSlice(state);
  const componentApplicationDetails = selectComponentDetails(state);
  const { tabId } = selectRouterCurrentParams(state);
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
    policyViolations: isFirewallOrRepository ? firewallPolicyViolations : applicationPolicyViolations,
    selectPolicyId: selectPolicyId,
    componentHash: isFirewallOrRepository
      ? firewallComponentDetailsPage?.componentDetails?.hash
      : componentApplicationDetails?.hash,
    tabId,
    repositoryId: isFirewallOrRepository ? firewallComponentDetailsPageParams.repositoryId : null,
    matchState: isFirewallOrRepository ? firewallComponentDetailsPageParams.matchState : null,
    pathname: isFirewallOrRepository ? firewallComponentDetailsPageParams.pathname : null,
    componentDisplayName: isFirewallOrRepository ? firewallComponentDetailsPageParams.componentDisplayName : null,
    isFirewall,
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

ViolationPageContainer.propTypes = pick(['$state'], ViolationPage.propTypes);
