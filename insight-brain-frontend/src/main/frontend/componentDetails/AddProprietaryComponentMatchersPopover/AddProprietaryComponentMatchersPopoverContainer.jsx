/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import { actions } from '../componentDetailsSlice';
import {
  selectFilteredPathnames,
  selectShowMatchersPopover,
  selectApplicationInfo,
  selectSetProprietaryMatchers,
} from '../componentDetailsSelectors';
import AddProprietaryComponentMatchersPopover from './AddProprietaryComponentMatchersPopover';

function mapStateToProps(state) {
  return {
    showPopover: selectShowMatchersPopover(state),
    pathnames: selectFilteredPathnames(state),
    appInfo: selectApplicationInfo(state),
    ...selectSetProprietaryMatchers(state),
  };
}

const mapDispatchToProps = {
  onClose: actions.toggleShowMatchersPopover,
  addProprietaryMatchers: actions.addProprietaryMatchers,
  resetSubmitError: actions.resetSubmitError,
  setComponentMatchersData: actions.setComponentMatchersData,
};

export const AddProprietaryComponentMatchersPopoverContainer = connect(
  mapStateToProps,
  mapDispatchToProps
)(AddProprietaryComponentMatchersPopover);
