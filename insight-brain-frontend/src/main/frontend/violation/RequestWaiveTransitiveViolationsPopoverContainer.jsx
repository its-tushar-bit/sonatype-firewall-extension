/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { pick } from 'ramda';

import { toggleRequestWaiveTransitiveViolations } from './transitiveViolationsActions';
import RequestWaiveTransitiveViolationsPopover from './RequestWaiveTransitiveViolationsPopover';

function mapStateToProps({ router, transitiveViolations }) {
  return {
    ...pick(['scanId', 'hash'], router.currentParams),
    ...pick(['availableScopes', 'componentTransitivePolicyViolations'], transitiveViolations),
  };
}

const mapDispatchToProps = {
  toggleRequestWaiveTransitiveViolations,
};

const RequestWaiveTransitiveViolationsPopoverContainer = connect(
  mapStateToProps,
  mapDispatchToProps
)(RequestWaiveTransitiveViolationsPopover);
export default RequestWaiveTransitiveViolationsPopoverContainer;
