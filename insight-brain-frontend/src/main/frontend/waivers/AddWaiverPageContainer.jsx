/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import { pick } from 'ramda';

import AddWaiverPage from './AddWaiverPage';
import {
  loadAddWaiverData,
  saveWaiverAndRedirect,
  setWaiverComment,
  setWaiverScope,
  setComponentMatcherStrategy,
  setExpiryTime,
  setCustomExpiryTime,
  returnToAddWaiverOriginPage,
} from './waiverActions';
import {
  openVulnerabilityDetailsModal,
  closeVulnerabilityDetailsModal,
} from '../vulnerabilityDetails/vulnerabilityDetailsModalActions';
import { selectIsFirewall, selectIsFirewallOrRepository } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectFirewallComponentDetailsPageRouteParams } from 'MainRoot/firewall/firewallSelectors';

function mapStateToProps(state) {
  const { addWaiver, violation, router, user } = state;
  const isFirewall = selectIsFirewall(state);
  const isFirewallOrRepositoryComponent = selectIsFirewallOrRepository(state);
  const {
    repositoryId,
    componentIdentifier,
    componentHash,
    matchState,
    proprietary,
    identificationSource,
    pathname,
    tabId,
  } = selectFirewallComponentDetailsPageRouteParams(state);
  return {
    ...addWaiver,
    ...pick(['violationDetails'], violation),
    ...pick(['violationId'], router.currentParams),
    prevStateName: router.prevState.name,
    prevParams: router.prevParams,
    currentUser: user?.currentUser?.displayName,
    isFirewall,
    isFirewallOrRepositoryComponent,
    repositoryId,
    componentIdentifier,
    componentHash,
    matchState,
    proprietary,
    identificationSource,
    pathname,
    tabId,
  };
}

const mapDispatchToProps = {
  loadAddWaiverData,
  openVulnerabilityDetailsModal,
  closeVulnerabilityDetailsModal,
  saveWaiver: saveWaiverAndRedirect,
  setWaiverComment,
  setWaiverScope,
  setComponentMatcherStrategy,
  setExpiryTime,
  setCustomExpiryTime,
  cancelAction: returnToAddWaiverOriginPage,
};

const AddWaiverPageContainer = connect(mapStateToProps, mapDispatchToProps)(AddWaiverPage);
export default AddWaiverPageContainer;
