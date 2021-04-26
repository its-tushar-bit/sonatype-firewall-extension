/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { partial } from 'ramda';

import DashboardViolationsTable from './DashboardViolationsTable';
import DashboardMask from '../dashboardMask/DashboardMask';

export default function DashboardViolations(props) {
  const VIOLATIONS_RESULTS_TYPE = 'violations';
  const {
      results,
      filterLoading,
      needsAcknowledgement,
      filtersAreDirty,
      loadResults,
      sortResults,
      stateGo,
      appliedFilter: { maxDaysOld },
    } = props,
    violations = results && results[VIOLATIONS_RESULTS_TYPE],
    sortViolations = partial(sortResults, [VIOLATIONS_RESULTS_TYPE]);

  const doLoad = () => {
    loadResults(VIOLATIONS_RESULTS_TYPE);
  };

  useEffect(() => {
    if (!filterLoading && !needsAcknowledgement) {
      doLoad();
    }
  }, [filterLoading, needsAcknowledgement]);

  const tableProps = {
    violations,
    sortViolations,
    stateGo,
    maxDaysOld,
    needsAcknowledgement,
    reload: doLoad,
  };

  return (
    <div id="dashboard-violations" className="iq-dashboard-violations nx-viewport-sized__container">
      {filtersAreDirty && !needsAcknowledgement && <DashboardMask />}
      <DashboardViolationsTable {...tableProps} />
    </div>
  );
}

const dashboardResultsShape = PropTypes.shape({
  results: PropTypes.array,
  numResults: PropTypes.number,
  error: PropTypes.string,
  sortFields: PropTypes.arrayOf(PropTypes.string),
});

DashboardViolations.propTypes = {
  filterLoading: PropTypes.bool.isRequired,
  needsAcknowledgement: PropTypes.bool.isRequired,
  filtersAreDirty: PropTypes.bool.isRequired,
  loadResults: PropTypes.func.isRequired,
  sortResults: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  appliedFilter: PropTypes.shape({
    maxDaysOld: PropTypes.number,
  }).isRequired,
  results: PropTypes.shape({
    violations: dashboardResultsShape,
  }),
};
