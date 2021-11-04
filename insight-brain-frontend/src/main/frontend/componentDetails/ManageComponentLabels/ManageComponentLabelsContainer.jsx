/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import {
  selectApplicableLabelsLoadError,
  selectIsApplicableLabelsLoading,
  selectLabels,
  selectApplicableLabels,
  selectShowApplyLabelModal,
} from '../componentDetailsSelectors';
import { actions } from '../componentDetailsSlice';
import ManageComponentLabels from './ManageComponentLabels';

const { loadApplicableLabels } = actions;

function mapStateToProps(state) {
  return {
    applicableLabels: selectApplicableLabels(state),
    selectedLabels: selectLabels(state),
    loadError: selectApplicableLabelsLoadError(state),
    loading: selectIsApplicableLabelsLoading(state),
    showApplyLabelModal: selectShowApplyLabelModal(state),
  };
}

const mapDispatchToProps = {
  loadApplicableLabels,
  handleAddLabelTag: actions.handleAddLabelTag,
};

export default connect(mapStateToProps, mapDispatchToProps)(ManageComponentLabels);
