/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { pick } from 'ramda';

import { loadViolation, loadVulnerabilityDetails, loadFirewallPolicyVulnerabilityDetails } from './violationActions';
import { stateGo } from '../reduxUiRouter/routerActions';
import { fetchStageTypes } from '../stages/stagesActions';
import ViolationPage from './ViolationPage';
import {
  selectComponentDetailsViolationsSlice,
  selectSelectedViolationId,
} from '../componentDetails/ViolationsTableTile/PolicyViolationsSelectors';
import { actions } from '../componentDetails/ViolationsTableTile/policyViolationsSlice';
import { onGoToRepositoryComponentWaiversPage, loadFirewallViolationDetails } from '../firewall/firewallActions';
import { loadApplicableWaivers } from 'MainRoot/waivers/waiverActions';
import { selectComponentDetails } from 'MainRoot/componentDetails/componentDetailsSelectors';
import {
  selectIsFirewall,
  selectRouterCurrentParams,
  selectIsFirewallOrRepository,
} from 'MainRoot/reduxUiRouter/routerSelectors';

import {
  selectFirewallComponentDetailsPage,
  selectFirewallComponentDetailsPageRouteParams,
} from 'MainRoot/firewall/firewallSelectors';

function mapStateToProps(state, showViolationsDetailPopover) {
  const { stages, violation } = state;
  const isFirewall = selectIsFirewall(state);
  const isFirewallOrRepository = selectIsFirewallOrRepository(state);
  const firewallComponentDetailsPage = selectFirewallComponentDetailsPage(state);
  const { hasEditIqPermission: firewallHasEditIqPermission } = firewallComponentDetailsPage;
  const applicationHasEditPermission = pick(['hasEditIqPermission'], violation)?.hasEditIqPermission;

  const stageData = stages.dashboard;
  const isShowViolationsDetailPopover = showViolationsDetailPopover.showViolationsDetailPopover;
  const selectPolicyId = showViolationsDetailPopover.selectPolicyId;
  const firewallPolicyViolations = firewallComponentDetailsPage.policyViolations;
  const applicationPolicyViolations = selectComponentDetailsViolationsSlice(state);
  const componentApplicationDetails = selectComponentDetails(state);
  const { tabId } = selectRouterCurrentParams(state);
  const firewallComponentDetailsPageParams = selectFirewallComponentDetailsPageRouteParams(state);

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
        'addWaiverPermission',
        'addWaiverPermissionLoading',
        'addWaiverPermissionError',
        'hasPermissionForAppWaivers',
        'isVulnerabilityDetailsOutdated',
      ],
      violation
    ),
    violationDetails,
    hasEditIqPermission: isFirewallOrRepository ? firewallHasEditIqPermission : applicationHasEditPermission,
    stageTypes: stageData.stageTypes,
    stageTypesError: stageData.error,
    selectedViolationId: selectSelectedViolationId(state),
    isFirewallContext: isShowViolationsDetailPopover,
    policyViolations: isFirewallOrRepository ? firewallPolicyViolations : applicationPolicyViolations,
    selectPolicyId: selectPolicyId,
    componentHash: isFirewallOrRepository
      ? firewallComponentDetailsPage?.componentDetails?.hash
      : componentApplicationDetails?.hash,
    tabId,
    repositoryId: isFirewallOrRepository ? firewallComponentDetailsPageParams.repositoryId : null,
    matchState: isFirewallOrRepository ? firewallComponentDetailsPageParams.matchState : null,
    pathname: isFirewallOrRepository ? firewallComponentDetailsPageParams.pathname : null,
    isFirewall,
  };
}

const mapDispatchToProps = {
  loadViolation,
  loadVulnerabilityDetails,
  loadFirewallPolicyVulnerabilityDetails,
  fetchStageTypes,
  stateGo,
  goToWaivers: actions.goToWaivers,
  onGoToRepositoryComponentWaiversPage: onGoToRepositoryComponentWaiversPage,
  loadFirewallViolationDetails: loadFirewallViolationDetails,
  loadApplicableWaivers: loadApplicableWaivers,
};

const ViolationPageContainer = connect(mapStateToProps, mapDispatchToProps)(ViolationPage);
export default ViolationPageContainer;

ViolationPageContainer.propTypes = pick(['$state'], ViolationPage.propTypes);
