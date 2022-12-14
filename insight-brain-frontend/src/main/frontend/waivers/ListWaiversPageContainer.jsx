/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import ListWaiversPage from './ListWaiversPage';
import { pick, prop } from 'ramda';
import { setWaiverToDelete, loadApplicableWaivers, loadManageWaiversData } from './waiverActions';
import { stateGo } from '../reduxUiRouter/routerActions';
import { selectIsFirewall, selectRepositoryId } from '../reduxUiRouter/routerSelectors';
import { stringifyPathName, stringifyComponentIdentifier } from 'MainRoot/util/componentIdentifierUtils';
import { selectFirewallComponentDetailsPageRouteParams } from 'MainRoot/firewall/firewallSelectors';

function mapStateToProps(state) {
  let pathname, matchState, componentIdentifier, hash, tabId;
  const { violation, manageWaivers, router, deleteWaiver, firewall } = state;
  const { showManageWaiverPage, componentDetails } = firewall.componentDetailsPage;
  const isFirewall = selectIsFirewall(state);
  const componentDetailsPageRouteParams = selectFirewallComponentDetailsPageRouteParams(state);
  const selectViolationDetails = showManageWaiverPage ? firewall.componentDetailsPage : violation;

  if (isFirewall) {
    matchState = componentDetailsPageRouteParams.matchState;
    pathname = componentDetailsPageRouteParams.pathname;
    componentIdentifier = componentDetailsPageRouteParams.componentIdentifier;
    hash = componentDetailsPageRouteParams.componentHash;
    tabId = componentDetailsPageRouteParams.tabId;
  } else {
    matchState = showManageWaiverPage ? componentDetails.matchState : null;
    pathname = showManageWaiverPage
      ? stringifyPathName(firewall.componentDetailsPage.componentDetails.componentIdentifier)
      : null;
    componentIdentifier = showManageWaiverPage
      ? stringifyComponentIdentifier(componentDetails.componentIdentifier, componentDetails.matchState)
      : null;
    hash = prop('hash', router.currentParams);
  }

  return {
    ...pick(['activeWaivers', 'expiredWaivers'], violation),
    ...pick(['violationDetails'], selectViolationDetails),
    ...pick(['violationId', 'sidebarReference', 'type', 'scanId', 'publicId'], router.currentParams),
    ...manageWaivers,
    ...pick(['waiverToDelete'], deleteWaiver),
    ...pick(['showManageWaiverPage'], firewall.componentDetailsPage),
    hash,
    matchState,
    isCurrentRouteName: isFirewall,
    repositoryPolicyId: isFirewall ? selectRepositoryId(state) : router.currentParams.repositoryPolicyId,
    pathname,
    componentIdentifier,
    isFirewall,
    tabId,
  };
}

const mapDispatchToProps = {
  loadManageWaiversData,
  setWaiverToDelete,
  loadApplicableWaivers,
  stateGo,
};

const ListWaiversPageContainer = connect(mapStateToProps, mapDispatchToProps)(ListWaiversPage);
export default ListWaiversPageContainer;
