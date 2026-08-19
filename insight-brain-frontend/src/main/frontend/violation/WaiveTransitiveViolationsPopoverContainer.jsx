/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { pick } from 'ramda';

import { toggleWaiveTransitiveViolations } from './transitiveViolationsActions';
import { actions } from './waiveTransitiveViolationsSlice';
import WaiveTransitiveViolationsPopover from './WaiveTransitiveViolationsPopover';
import { selectIsExpireWhenRemediationAvailableWaiversEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';

function mapStateToProps(state) {
  return {
    isExpireWhenRemediationAvailable: selectIsExpireWhenRemediationAvailableWaiversEnabled(state),
    ...pick(['availableScopes', 'componentTransitivePolicyViolations'], state.transitiveViolations),
    ...pick(['scope', 'expiration', 'comments', 'submitMaskState', 'saveError'], state.waiveTransitiveViolations),
  };
}

const mapDispatchToProps = {
  toggleWaiveTransitiveViolations,
  ...actions,
};

const WaiveTransitiveViolationsPopoverContainer = connect(
  mapStateToProps,
  mapDispatchToProps
)(WaiveTransitiveViolationsPopover);
export default WaiveTransitiveViolationsPopoverContainer;
