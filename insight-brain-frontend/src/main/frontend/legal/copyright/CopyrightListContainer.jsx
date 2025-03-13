/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import { connect } from 'react-redux';
import CopyrightList from './CopyrightList';
import { isCopyrightDetailsState } from './copyrightDetailsUtils';
import { selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';

function mapStateToProps({ advancedLegal, componentCopyrightDetails, router }) {
  const component = advancedLegal.component || {};
  const availableScopes = advancedLegal.availableScopes || {};
  const isSbomManager = selectIsSbomManager({ router });

  let routerParams = router.currentParams;
  if (!isCopyrightDetailsState(router.currentState.name) && isCopyrightDetailsState(router.prevState.name)) {
    routerParams = router.prevParams;
  }
  return {
    isSbomManager,
    loading: component.loading || availableScopes.loading || componentCopyrightDetails.loadingCopyrightFileCounts,
    error: component.error || availableScopes.error || componentCopyrightDetails.errorCopyrightFileCounts,
    componentCopyrightDetails,
    ...pick(['component'], component),
    ...pick(['hash', 'componentIdentifier', 'ownerType', 'ownerId', 'copyrightIndex', 'stageTypeId'], routerParams),
  };
}

const CopyrightListContainer = connect(mapStateToProps)(CopyrightList);
export default CopyrightListContainer;
