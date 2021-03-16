/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { partial } from 'ramda';

import DashboardApplicationsTable from './DashboardApplicationsTable';
import { heatMapColorStylerPropTypes } from '../DashboardHeatMapCell';
import DashboardMask from '../dashboardMask/DashboardMask';

export const APPLICATIONS_RESULTS_TYPE = 'applications';

export default function DashboardApplications(props) {
  const {
    applicationResults,
    filterLoading,
    needsAcknowledgement,
    filtersAreDirty,
    loadResults,
    sortResults
  } = props;

  const doLoad = () => {
    loadResults(APPLICATIONS_RESULTS_TYPE);
  };

  useEffect(() => {
    if (!filterLoading && !needsAcknowledgement) {
      doLoad();
    }
  }, [filterLoading, needsAcknowledgement]);

  const tableProps = {
    reload: doLoad,
    colorStyler: applicationResults && applicationResults.classyBrew,
    sortApplications: partial(sortResults, [APPLICATIONS_RESULTS_TYPE]),
    applicationResults,
    needsAcknowledgement
  };

  return (
    <div id="dashboard-applications" className="iq-dashboard-applications nx-viewport-sized__container">
      {filtersAreDirty && <DashboardMask />}
      <DashboardApplicationsTable {...tableProps}/>
    </div>
  );
}

const dashboardResultsShape = PropTypes.shape({
  results: PropTypes.array,
  sortFields: PropTypes.arrayOf(PropTypes.string),
  error: PropTypes.string,
  classyBrew: heatMapColorStylerPropTypes
});

DashboardApplications.propTypes = {
  applicationResults: dashboardResultsShape,
  filterLoading: PropTypes.bool.isRequired,
  needsAcknowledgement: PropTypes.bool.isRequired,
  filtersAreDirty: PropTypes.bool.isRequired,
  loadResults: PropTypes.func.isRequired,
  sortResults: PropTypes.func.isRequired
};
