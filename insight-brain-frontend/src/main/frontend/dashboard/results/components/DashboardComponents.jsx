/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';

import DashboardComponentsTable from './DashboardComponentsTable';
import { heatMapColorStylerPropTypes } from '../DashboardHeatMapCell';
import DashboardMask from '../dashboardMask/DashboardMask';

export default function DashboardComponents(props) {
  const {
      componentResults,
      filterLoading,
      needsAcknowledgement,
      filtersAreDirty,
      loadComponentResults,
      sortComponents,
      stateGo,
      setNextComponentsPage,
      setPreviousComponentsPage,
    } = props,
    isLoading = !componentResults.results && !componentResults.error;

  useEffect(() => {
    if (!filterLoading && !needsAcknowledgement) {
      loadComponentResults();
    }
  }, [filterLoading, needsAcknowledgement]);

  const tableProps = {
    reload: loadComponentResults,
    colorStyler: componentResults && componentResults.classyBrew,
    componentResults,
    needsAcknowledgement,
    sortComponents,
    stateGo,
    setNextComponentsPage,
    setPreviousComponentsPage,
  };

  return (
    <div id="dashboard-components" className="iq-dashboard-components">
      {filtersAreDirty && !needsAcknowledgement && !isLoading && <DashboardMask />}
      <DashboardComponentsTable {...tableProps} />
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

DashboardComponents.propTypes = {
  componentResults: dashboardResultsShape,
  filterLoading: PropTypes.bool.isRequired,
  needsAcknowledgement: PropTypes.bool.isRequired,
  filtersAreDirty: PropTypes.bool.isRequired,
  loadComponentResults: PropTypes.func.isRequired,
  sortComponents: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  setNextComponentsPage: PropTypes.func.isRequired,
  setPreviousComponentsPage: PropTypes.func.isRequired,
};
