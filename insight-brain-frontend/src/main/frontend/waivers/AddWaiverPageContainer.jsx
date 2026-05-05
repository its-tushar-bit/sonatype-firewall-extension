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
  setWaiverReason,
  setCustomExpiryTime,
  returnToAddOrRequestWaiverOriginPage,
} from './waiverActions';
import {
  openVulnerabilityDetailsModal,
  closeVulnerabilityDetailsModal,
} from '../vulnerabilityDetails/vulnerabilityDetailsModalActions';
import {
  selectIsFirewall,
  selectIsFirewallOrRepository,
  selectIsStandaloneDeveloper,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectFirewallComponentDetailsPageRouteParams } from 'MainRoot/firewall/firewallSelectors';
import {
  selectIsExpireWhenRemediationAvailableWaiversEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectCurrentUser } from 'MainRoot/user/userSessionSelectors';

function mapStateToProps(state) {
  const { addWaiver, violation, router, waivers } = state;
  const isFirewall = selectIsFirewall(state);
  const isFirewallOrRepositoryComponent = selectIsFirewallOrRepository(state);
  const isStandaloneDeveloper = selectIsStandaloneDeveloper(state);
  const isExpireWhenRemediationAvailable = selectIsExpireWhenRemediationAvailableWaiversEnabled(state);
  const {
    repositoryId,
    componentIdentifier,
    componentHash,
    matchState,
    proprietary,
    identificationSource,
    pathname,
    tabId,
    componentDisplayName,
  } = selectFirewallComponentDetailsPageRouteParams(state);
  return {
    ...addWaiver,
    ...pick(['violationDetails'], violation),
    ...pick(['violationId'], router.currentParams),
    loading: addWaiver.loading || waivers.waiverReasons.loading,
    loadError: addWaiver.loadError || waivers.waiverReasons.loadError,
    prevStateName: router.prevState.name,
    prevParams: router.prevParams,
    currentUser: selectCurrentUser(state)?.displayName,
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
    componentDisplayName,
    isStandaloneDeveloper,
    waiverReasons: waivers.waiverReasons.data,
    isExpireWhenRemediationAvailable,
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
  setWaiverReason,
  setCustomExpiryTime,
  cancelAction: returnToAddOrRequestWaiverOriginPage,
};

const AddWaiverPageContainer = connect(mapStateToProps, mapDispatchToProps)(AddWaiverPage);
export default AddWaiverPageContainer;
