/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import {
  selectLoadError,
  selectIsApplicableLabelsLoading,
  selectLabels,
  selectApplicableLabels,
} from '../componentDetailsSelectors';
import { actions } from '../componentDetailsSlice';
import ManageComponentLabels from './ManageComponentLabels';

const { loadApplicableLabels } = actions;

function mapStateToProps(state) {
  return {
    applicableLabels: selectApplicableLabels(state),
    selectedLabels: selectLabels(state),
    loadError: selectLoadError(state),
    loading: selectIsApplicableLabelsLoading(state),
  };
}

const mapDispatchToProps = {
  loadApplicableLabels,
};

export default connect(mapStateToProps, mapDispatchToProps)(ManageComponentLabels);
