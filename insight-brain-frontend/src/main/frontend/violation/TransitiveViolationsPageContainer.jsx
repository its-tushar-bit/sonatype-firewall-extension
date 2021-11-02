/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { pick } from 'ramda';

import TransitiveViolationsPage from './TransitiveViolationsPage';
import {
  loadAvailableScopes,
  loadReportMetadata,
  loadTransitiveViolations,
  setFilteringParameters,
  setSortingParameters,
  toggleRequestWaiveTransitiveViolations,
  toggleWaiveTransitiveViolations,
  toggleViewTransitiveViolationWaivers,
  loadTransitiveViolationWaivers,
} from './transitiveViolationsActions';
import { actions } from '../componentDetails/ViolationsTableTile/policyViolationsSlice';
import { setWaiverToDelete } from '../waivers/waiverActions';

function mapStateToProps({ router, transitiveViolations, componentDetailsPolicyViolations, deleteWaiver }) {
  return {
    ...pick(['ownerType', 'ownerId', 'scanId', 'hash'], router.currentParams),
    ...pick(
      [
        'availableScopes',
        'reportMetadata',
        'componentTransitivePolicyViolations',
        'transitiveViolationWaivers',
        'isRequestWaiveTransitiveViolationsOpen',
        'isWaiveTransitiveViolationsOpen',
        'isViewTransitiveViolationWaiversOpen',
      ],
      transitiveViolations
    ),
    showViolationsDetailPopover: componentDetailsPolicyViolations.showViolationsDetailPopover,
    ...pick(['waiverToDelete'], deleteWaiver),
    shouldGoBackToComponentDetails: 'applicationReport.policy' !== router.prevState.name,
  };
}

const mapDispatchToProps = {
  loadAvailableScopes,
  loadReportMetadata,
  loadTransitiveViolations,
  loadTransitiveViolationWaivers,
  setSortingParameters,
  setFilteringParameters,
  toggleRequestWaiveTransitiveViolations,
  toggleWaiveTransitiveViolations,
  toggleViewTransitiveViolationWaivers,
  setSelectedPolicyViolationId: actions.setSelectedPolicyViolationId,
  toggleShowViolationsDetailPopover: actions.toggleShowViolationsDetailPopover,
  setWaiverToDelete,
};

const TransitiveViolationsPageContainer = connect(mapStateToProps, mapDispatchToProps)(TransitiveViolationsPage);
export default TransitiveViolationsPageContainer;
