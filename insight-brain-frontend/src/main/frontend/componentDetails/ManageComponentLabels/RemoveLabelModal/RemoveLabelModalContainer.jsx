/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import {
  selectRemoveAppliedLabelError,
  selectSelectedLabelDetails,
  selectShowRemoveLabelModal,
  selectRemoveLabelMaskState,
} from '../../componentDetailsSelectors';
import { actions } from '../../componentDetailsSlice';
import RemoveLabelModal from './RemoveLabelModal';

const { removeAppliedLabel, toggleShowRemoveLabelModal } = actions;

function mapStateToProps(state) {
  return {
    selectedLabelDetails: selectSelectedLabelDetails(state),
    showRemoveLabelModal: selectShowRemoveLabelModal(state),
    removeLabelError: selectRemoveAppliedLabelError(state),
    removeLabelMaskState: selectRemoveLabelMaskState(state),
  };
}

const mapDispatchToProps = {
  toggleShowRemoveLabelModal,
  removeLabel: removeAppliedLabel,
};

export default connect(mapStateToProps, mapDispatchToProps)(RemoveLabelModal);
