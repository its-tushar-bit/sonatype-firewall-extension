/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { pick } from 'ramda';
import CopyrightDetailsHeader from './CopyrightDetailsHeader';
import { loadComponentAndCopyrightDetails } from './componentCopyrightDetailsActions';
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
    availableScopes,
    ...pick(['hash', 'ownerType', 'ownerId', 'copyrightIndex'], routerParams),
  };
}

const mapDispatchToProps = {
  loadComponentAndCopyrightDetails,
};

const CopyrightDetailsHeaderContainer = connect(mapStateToProps, mapDispatchToProps)(CopyrightDetailsHeader);
export default CopyrightDetailsHeaderContainer;
