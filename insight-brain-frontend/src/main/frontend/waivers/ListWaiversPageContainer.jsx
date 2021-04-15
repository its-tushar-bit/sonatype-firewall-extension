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

function mapStateToProps({ violation, manageWaivers, router, deleteWaiver }) {
  return {
    ...pick(['activeWaivers', 'expiredWaivers', 'violationDetails'], violation),
    ...pick(['violationId'], router.currentParams),
    ...pick(
      [
        'loadingManageWaiversData',
        'loadManageWaiversDataError',
        'hasPermissionForAppWaivers',
        'loadingApplicableWaivers',
        'loadApplicableWaiversError',
      ],
      manageWaivers
    ),
    ...pick(['waiverToDelete'], deleteWaiver),
  };
}

const mapDispatchToProps = {
  loadManageWaiversData,
  setWaiverToDelete,
  loadApplicableWaivers,
};

const ListWaiversPageContainer = connect(mapStateToProps, mapDispatchToProps)(ListWaiversPage);
export default ListWaiversPageContainer;
