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
} from './transitiveViolationsActions';
import { actions } from '../componentDetails/violations/PolicyViolationsSlice';

function mapStateToProps({ router, transitiveViolations, componentDetailsPolicyViolations }) {
  return {
    ...pick(['ownerType', 'ownerId', 'scanId', 'hash'], router.currentParams),
    ...pick(
      [
        'availableScopes',
        'reportMetadata',
        'componentTransitivePolicyViolations',
        'isRequestWaiveTransitiveViolationsOpen',
        'isWaiveTransitiveViolationsOpen',
      ],
      transitiveViolations
    ),
    showViolationsDetailPopover: componentDetailsPolicyViolations.showViolationsDetailPopover,
  };
}

const mapDispatchToProps = {
  loadAvailableScopes,
  loadReportMetadata,
  loadTransitiveViolations,
  setSortingParameters,
  setFilteringParameters,
  toggleRequestWaiveTransitiveViolations,
  toggleWaiveTransitiveViolations,
  setSelectedPolicyViolationId: actions.setSelectedPolicyViolationId,
  toggleShowViolationsDetailPopover: actions.toggleShowViolationsDetailPopover,
};

const TransitiveViolationsPageContainer = connect(mapStateToProps, mapDispatchToProps)(TransitiveViolationsPage);
export default TransitiveViolationsPageContainer;
