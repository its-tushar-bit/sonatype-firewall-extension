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
  selectComponentDetailsLoadErrors,
  selectComponentDetailsLoading,
  selectIsProprietary,
  selectFilteredPathnames,
} from './componentDetailsSelectors';
import { actions } from './componentDetailsSlice';
import ComponentDetails from './ComponentDetails';

const { onTabChange, loadComponentDetails, backToOffspringAction, toggleShowMatchersPopover } = actions;

function mapStateToProps(state, { uiRouterState }) {
  return {
    componentDetails: selectComponentDetails(state),
    activeTabId: selectActiveTabId(state),
    pagination: selectComponentPagination(state, { uiRouterState }),
    loadError: selectComponentDetailsLoadErrors(state),
    loading: selectComponentDetailsLoading(state),
    isProprietary: selectIsProprietary(state),
    pathnames: selectFilteredPathnames(state),
  };
}

const mapDispatchToProps = {
  // we derive componentDetails from the url and the selectedReport
  // but we need to load the report if there is none loaded yet
  loadComponentDetails,
  onTabChange,
  backToOffspringOnClick: backToOffspringAction,
  toggleShowMatchersPopover,
};

export default connect(mapStateToProps, mapDispatchToProps)(ComponentDetails);
