/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import ListWaiversPage from './ListWaiversPage';
import { pick } from 'ramda';
import { loadViolation } from '../violation/violationPageActions';
import { setWaiverToDelete } from './waiverActions';

function mapStateToProps({ violationPage, router, deleteWaiver }) {
  return {
    ...pick(['activeWaivers', 'expiredWaivers', 'loading', 'violationDetails', 'violationDetailsError'], violationPage),
    ...pick(['violationId'], router.currentParams),
    ...pick(['waiverToDelete'], deleteWaiver)
  };
}

const mapDispatchToProps = {
  loadViolation,
  setWaiverToDelete
};

const ListWaiversPageContainer = connect(mapStateToProps, mapDispatchToProps)(ListWaiversPage);
export default ListWaiversPageContainer;
