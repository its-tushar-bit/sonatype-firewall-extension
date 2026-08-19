/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';

import DashboardApplicationsTable from './DashboardApplicationsTable';
import { heatMapColorStylerPropTypes } from '../DashboardHeatMapCell';
import DashboardMask from '../dashboardMask/DashboardMask';

export default function DashboardApplications(props) {
  const {
      applicationResults,
      filterLoading,
      needsAcknowledgement,
      filtersAreDirty,
      loadApplicationResults,
      sortApplications,
      setNextApplicationsPage,
      setPreviousApplicationsPage,
    } = props,
    isLoading = !applicationResults.results && !applicationResults.error;

  useEffect(() => {
    if (!filterLoading && !needsAcknowledgement) {
      loadApplicationResults();
    }
  }, [filterLoading, needsAcknowledgement]);

  const tableProps = {
    reload: loadApplicationResults,
    colorStyler: applicationResults && applicationResults.classyBrew,
    sortApplications,
    applicationResults,
    needsAcknowledgement,
    setNextApplicationsPage,
    setPreviousApplicationsPage,
  };

  return (
    <div id="dashboard-applications" className="iq-dashboard-applications">
      {filtersAreDirty && !needsAcknowledgement && !isLoading && <DashboardMask />}
      <DashboardApplicationsTable {...tableProps} />
    </div>
  );
}

const dashboardResultsShape = PropTypes.shape({
  results: PropTypes.array,
  sortFields: PropTypes.arrayOf(PropTypes.string),
  hasNextPage: PropTypes.bool,
  error: PropTypes.string,
  classyBrew: heatMapColorStylerPropTypes,
});

DashboardApplications.propTypes = {
  applicationResults: dashboardResultsShape,
  filterLoading: PropTypes.bool.isRequired,
  needsAcknowledgement: PropTypes.bool.isRequired,
  filtersAreDirty: PropTypes.bool.isRequired,
  loadApplicationResults: PropTypes.func.isRequired,
  sortApplications: PropTypes.func.isRequired,
  setNextApplicationsPage: PropTypes.func.isRequired,
  setPreviousApplicationsPage: PropTypes.func.isRequired,
};
