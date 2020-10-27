/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import ListWaiversPage from './ListWaiversPage';
import { pick } from 'ramda';
import { loadManageWaiversData } from './waiverActions';
import { setWaiverToDelete } from './waiverActions';

function mapStateToProps({ violationPage, manageWaivers, router, deleteWaiver }) {
  return {
    ...pick(['activeWaivers', 'expiredWaivers', 'violationDetails'], violationPage),
    ...pick(['violationId'], router.currentParams),
    ...pick(['loading', 'loadError', 'hasPermissionForAppWaivers'], manageWaivers),
    ...pick(['waiverToDelete'], deleteWaiver)
  };
}

const mapDispatchToProps = {
  loadManageWaiversData,
  setWaiverToDelete
};

const ListWaiversPageContainer = connect(mapStateToProps, mapDispatchToProps)(ListWaiversPage);
export default ListWaiversPageContainer;
