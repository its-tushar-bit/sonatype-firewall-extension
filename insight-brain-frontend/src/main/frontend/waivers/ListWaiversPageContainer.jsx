/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import ListWaiversPage from './ListWaiversPage';
import { pick } from 'ramda';
import { setWaiverToDelete, loadApplicableWaivers, loadManageWaiversData } from './waiverActions';
import { stateGo } from '../reduxUiRouter/routerActions';
import { selectIsFirewall } from '../reduxUiRouter/routerSelectors';
import { stringifyPathName, stringifyComponentIdentifier } from 'MainRoot/util/componentIdentifierUtils';

function mapStateToProps(state) {
  const { violation, manageWaivers, router, deleteWaiver, firewall } = state;
  const { showManageWaiverPage, componentDetails } = firewall.componentDetailsPage;
  const selectViolationDetails = showManageWaiverPage ? firewall.componentDetailsPage : violation;
  const matchState = showManageWaiverPage ? componentDetails.matchState : null;
  const pathname = showManageWaiverPage
    ? stringifyPathName(firewall.componentDetailsPage.componentDetails.componentIdentifier)
    : null;
  const componentIdentifier = showManageWaiverPage
    ? stringifyComponentIdentifier(componentDetails.componentIdentifier, componentDetails.matchState)
    : null;

  return {
    ...pick(['activeWaivers', 'expiredWaivers'], violation),
    ...pick(['violationDetails'], selectViolationDetails),
    ...pick(['violationId', 'sidebarReference', 'type', 'hash', 'scanId', 'publicId'], router.currentParams),
    ...manageWaivers,
    ...pick(['waiverToDelete'], deleteWaiver),
    ...pick(['showManageWaiverPage'], firewall.componentDetailsPage),
    matchState,
    isCurrentRouteName: selectIsFirewall(state),
    repositoryPolicyId: router.currentParams.repositoryPolicyId,
    pathname,
    componentIdentifier,
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
