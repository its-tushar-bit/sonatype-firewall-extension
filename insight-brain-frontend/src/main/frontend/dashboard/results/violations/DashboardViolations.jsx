/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';

import DashboardViolationsTable from './DashboardViolationsTable';
import DashboardMask from '../dashboardMask/DashboardMask';

export default function DashboardViolations(props) {
  const {
      violations,
      filterLoading,
      needsAcknowledgement,
      filtersAreDirty,
      loadViolationResults,
      sortViolations,
      stateGo,
      appliedFilter: { maxDaysOld },
      prevStateName,
      setNextViolationsPage,
      setPreviousViolationsPage,
    } = props,
    isLoading = !violations.results && !violations.error;

  const doLoad = () => {
    loadViolationResults();
  };

  useEffect(() => {
    if (!filterLoading && !needsAcknowledgement) {
      // if it comes back from violation details and there are results, don't reload
      if (!(violations.results && prevStateName === 'sidebarView.violation')) {
        doLoad();
      }
    }
  }, [filterLoading, needsAcknowledgement]);

  const tableProps = {
    violations,
    sortViolations,
    stateGo,
    maxDaysOld,
    needsAcknowledgement,
    reload: doLoad,
    setNextViolationsPage,
    setPreviousViolationsPage,
  };

  return (
    <div id="dashboard-violations" className="iq-dashboard-violations">
      {filtersAreDirty && !needsAcknowledgement && !isLoading && <DashboardMask />}
      <DashboardViolationsTable {...tableProps} />
    </div>
  );
}

const dashboardResultsShape = PropTypes.shape({
  results: PropTypes.array,
  hasNextPage: PropTypes.bool,
  error: PropTypes.string,
  sortFields: PropTypes.arrayOf(PropTypes.string),
});

DashboardViolations.propTypes = {
  filterLoading: PropTypes.bool.isRequired,
  needsAcknowledgement: PropTypes.bool.isRequired,
  filtersAreDirty: PropTypes.bool.isRequired,
  loadViolationResults: PropTypes.func.isRequired,
  sortViolations: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  appliedFilter: PropTypes.shape({
    maxDaysOld: PropTypes.number,
  }).isRequired,
  violations: dashboardResultsShape,
  prevStateName: PropTypes.string,
  setNextViolationsPage: PropTypes.func.isRequired,
  setPreviousViolationsPage: PropTypes.func.isRequired,
};
