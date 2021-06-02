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
  loadTransitiveViolations,
  setFilteringParameters,
  setSortingParameters,
} from './transitiveViolationsActions';

function mapStateToProps({ router, transitiveViolations }) {
  let scanId = undefined;
  if (router.prevState.name === 'applicationReport.policy') {
    scanId = router.prevParams.scanId;
  }
  return {
    scanId,
    ...pick(['ownerType', 'ownerId', 'stageTypeId', 'hash'], router.currentParams),
    ...pick(['availableScopes', 'componentTransitivePolicyViolations'], transitiveViolations),
  };
}

const mapDispatchToProps = {
  loadAvailableScopes,
  loadTransitiveViolations,
  setSortingParameters,
  setFilteringParameters,
};

const TransitiveViolationsPageContainer = connect(mapStateToProps, mapDispatchToProps)(TransitiveViolationsPage);
export default TransitiveViolationsPageContainer;
