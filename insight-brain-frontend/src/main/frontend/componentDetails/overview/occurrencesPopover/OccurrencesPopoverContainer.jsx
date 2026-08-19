/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import OccurrencesPopover from './OccurrencesPopover';
import { occurrencesPopoverActions } from './occurrencesPopoverSlice';

function mapStateToProps(state) {
  const { occurrencesPopover } = state;
  const { showOccurrencesPopover } = occurrencesPopover;
  return { showOccurrencesPopover };
}

const mapDispatchToProps = {
  onClose: occurrencesPopoverActions.toggleShowOccurrencesPopover,
};

export default connect(mapStateToProps, mapDispatchToProps)(OccurrencesPopover);
