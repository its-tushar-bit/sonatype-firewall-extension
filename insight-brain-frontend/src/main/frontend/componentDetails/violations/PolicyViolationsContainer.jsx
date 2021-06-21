/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import PolicyViolations from './PolicyViolations';
import { selectComponentDetails, selectComponentViolations } from '../componentDetailsSelectors';
import { selectApplicationReportSlice } from '../../applicationReport/applicationReportSelectors';
import { loadReport } from '../../applicationReport/applicationReportActions';

function mapStateToProps(state) {
  const { loadError } = selectApplicationReportSlice(state);

  return {
    violations: selectComponentViolations(state),
    componentDetails: selectComponentDetails(state),
    loadError,
  };
}

const mapDispatchToProps = {
  // we derive componentDetails from the url and the selectedReport
  // but we need to load the report if there is none loaded yet
  loadComponentDetails: () => loadReport(true),
};

export const PolicyViolationsContainer = connect(mapStateToProps, mapDispatchToProps)(PolicyViolations);
