/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import {
  selectApplicableLabelsLoadError,
  selectApplyLabelMaskState,
  selectIsApplicableLabelsLoading,
  selectLabels,
  selectApplicableLabels,
  selectComponentDetailsLoading,
  selectSelectedLabelDetails,
} from '../componentDetailsSelectors';
import { actions } from '../componentDetailsSlice';
import ManageComponentLabels from './ManageComponentLabels';

const { loadApplicableLabels, removeAppliedLabel, handleRemoveLabelTag, toggleShowRemoveLabelModal } = actions;

function mapStateToProps(state) {
  return {
    applicableLabels: selectApplicableLabels(state),
    selectedLabels: selectLabels(state),
    selectedLabelDetails: selectSelectedLabelDetails(state),
    applyLabelMaskState: selectApplyLabelMaskState(state),
    loadError: selectApplicableLabelsLoadError(state),
    loading: selectIsApplicableLabelsLoading(state) || selectComponentDetailsLoading(state),
  };
}

const mapDispatchToProps = {
  removeLabel: removeAppliedLabel,
  loadApplicableLabels,
  toggleShowRemoveLabelModal,
  handleRemoveLabelTag,
  handleAddLabelTag: actions.handleAddLabelTag,
};

export default connect(mapStateToProps, mapDispatchToProps)(ManageComponentLabels);
