/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import RequestWaiversPopover from './RequestWaiversPopover';

function mapStateToProps(state) {
  return {
    violationDetails: state.violation.violationDetails,
  };
}

export default connect(mapStateToProps)(RequestWaiversPopover);
