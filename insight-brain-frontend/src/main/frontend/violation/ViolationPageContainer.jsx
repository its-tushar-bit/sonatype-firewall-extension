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
import { selectSelectedViolationId } from '../componentDetails/ViolationsTableTile/PolicyViolationsSelectors';
import { actions } from '../componentDetails/ViolationsTableTile/policyViolationsSlice';
import { onGoToFirewallWaiversPage, loadFirewallViolationDetails } from '../firewall/firewallActions';

function mapStateToProps(state, showViolationsDetailPopover) {
  const { stages, violation, firewall } = state;
  const { componentDetailsPage } = firewall;
  const stageData = stages.dashboard;
  const isShowViolationsDetailPopover = showViolationsDetailPopover.showViolationsDetailPopover;
  const selectPolicyId = showViolationsDetailPopover.selectPolicyId;
  const policyViolations = componentDetailsPage.policyViolations;

  return {
    ...pick(
      [
        'loading',
        'violationDetailsError',
        'violationDetails',
        'vulnerabilityDetailsLoading',
        'vulnerabilityDetails',
        'vulnerabilityDetailsError',
        'activeWaivers',
      ],
      violation
    ),
    stageTypes: stageData.stageTypes,
    stageTypesError: stageData.error,
    selectedViolationId: selectSelectedViolationId(state),
    isPolicyPopoverShown: isShowViolationsDetailPopover,
    policyViolations: policyViolations,
    selectPolicyId: selectPolicyId,
  };
}

const mapDispatchToProps = {
  loadViolation,
  loadVulnerabilityDetails,
  loadFirewallPolicyVulnerabilityDetails,
  fetchStageTypes,
  stateGo,
  goToWaivers: actions.goToWaivers,
  onGoToFirewallWaiversPage: onGoToFirewallWaiversPage,
  loadFirewallViolationDetails: loadFirewallViolationDetails,
};

const ViolationPageContainer = connect(mapStateToProps, mapDispatchToProps)(ViolationPage);
export default ViolationPageContainer;

ViolationPageContainer.propTypes = pick(['$state'], ViolationPage.propTypes);
