/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import {
  selectComponentDetails,
  selectActiveTabId,
  selectComponentPagination,
  selectIsLabelsLoading,
} from './componentDetailsSelectors';
import { actions } from './componentDetailsSlice';
import ComponentDetails from './ComponentDetails';

const { onTabChange, loadComponentDetails, backToOffspringAction } = actions;

function mapStateToProps(state, { uiRouterState }) {
  return {
    componentDetails: selectComponentDetails(state),
    activeTabId: selectActiveTabId(state),
    pagination: selectComponentPagination(state, { uiRouterState }),
    loadError: state.applicationReport.loadError || state.componentDetails.loadError,
    loading: state.applicationReport.pendingLoads.size > 0 || selectIsLabelsLoading(state),
  };
}

const mapDispatchToProps = {
  // we derive componentDetails from the url and the selectedReport
  // but we need to load the report if there is none loaded yet
  loadComponentDetails,
  onTabChange,
  backToOffspringOnClick: backToOffspringAction,
};

export default connect(mapStateToProps, mapDispatchToProps)(ComponentDetails);
