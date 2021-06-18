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
  toggleWaiveTransitiveViolations,
} from './transitiveViolationsActions';

function mapStateToProps({ router, transitiveViolations }) {
  let scanId = undefined;
  return {
    scanId,
    ...pick(['ownerType', 'ownerId', 'scanId', 'hash'], router.currentParams),
    ...pick(
      ['availableScopes', 'reportMetadata', 'componentTransitivePolicyViolations', 'isWaiveTransitiveViolationsOpen'],
      transitiveViolations
    ),
  };
}

const mapDispatchToProps = {
  loadAvailableScopes,
  loadReportMetadata,
  loadTransitiveViolations,
  setSortingParameters,
  setFilteringParameters,
  toggleWaiveTransitiveViolations,
};

const TransitiveViolationsPageContainer = connect(mapStateToProps, mapDispatchToProps)(TransitiveViolationsPage);
export default TransitiveViolationsPageContainer;
