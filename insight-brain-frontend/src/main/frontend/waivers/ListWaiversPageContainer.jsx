/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import ListWaiversPage from './ListWaiversPage';
import { pick } from 'ramda';
import { loadManageWaiversData } from './waiverActions';
import { setWaiverToDelete, loadApplicableWaivers } from './waiverActions';
import { stateGo } from '../reduxUiRouter/routerActions';

function mapStateToProps(state) {
  const { violation, manageWaivers, router, deleteWaiver } = state;
  return {
    ...pick(['activeWaivers', 'expiredWaivers', 'violationDetails'], violation),
    ...pick(['violationId', 'sidebarReference', 'type', 'hash', 'scanId', 'publicId'], router.currentParams),
    ...manageWaivers,
    ...pick(['waiverToDelete'], deleteWaiver),
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
