/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { loadReport } from '../applicationReport/applicationReportActions';
import { selectComponentDetails, selectActiveTabId, selectComponentPagination } from './componentDetailsSelectors';
import { onTabChange } from './componentDetailsActions';
import ComponentDetails from './ComponentDetails';

function mapStateToProps(state, { uiRouterState }) {
  return {
    componentDetails: selectComponentDetails(state),
    activeTabId: selectActiveTabId(state),
    pagination: selectComponentPagination(state, { uiRouterState }),
    applicationReportLoadError: state.applicationReport.loadError,
  };
}

const mapDispatchToProps = {
  // we derive componentDetails from the url and the selectedReport
  // but we need to load the report if there is none loaded yet
  loadComponentDetails: () => loadReport(true),
  onTabChange,
};

export default connect(mapStateToProps, mapDispatchToProps)(ComponentDetails);
