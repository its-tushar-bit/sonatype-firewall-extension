/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import { connect } from 'react-redux';
import CopyrightList from './CopyrightList';
import { copyrightDetailsStateName } from './copyrightDetailsUtils';

function mapStateToProps({ advancedLegal, componentCopyrightDetails, router }) {
  const component = advancedLegal.component || {};
  const availableScopes = advancedLegal.availableScopes || {};

  let routerParams = router.currentParams;
  if (router.currentState.name !== copyrightDetailsStateName && router.prevState.name === copyrightDetailsStateName) {
    routerParams = router.prevParams;
  }
  return {
    loading: component.loading || availableScopes.loading || componentCopyrightDetails.loadingCopyrightFileCounts,
    error: component.error || availableScopes.error || componentCopyrightDetails.errorCopyrightFileCounts,
    componentCopyrightDetails,
    ...pick(['component'], component),
    ...pick(['hash', 'ownerType', 'ownerId', 'copyrightIndex'], routerParams),
  };
}

const CopyrightListContainer = connect(mapStateToProps)(CopyrightList);
export default CopyrightListContainer;
