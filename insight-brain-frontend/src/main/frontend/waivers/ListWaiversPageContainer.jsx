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

function mapStateToProps(state) {
  const { violation, manageWaivers, router, deleteWaiver, firewall } = state;
  const { showManageWaiverPage } = firewall.componentDetailsPage;
  const selectViolationDetails = showManageWaiverPage ? firewall.componentDetailsPage : violation;
  return {
    ...pick(['activeWaivers', 'expiredWaivers'], violation),
    ...pick(['violationDetails'], selectViolationDetails),
    ...pick(['violationId', 'sidebarReference', 'type', 'hash', 'scanId', 'publicId'], router.currentParams),
    ...manageWaivers,
    ...pick(['waiverToDelete'], deleteWaiver),
    ...pick(['showManageWaiverPage'], firewall.componentDetailsPage),
    isCurrentRouteName: selectIsFirewall(state),
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
