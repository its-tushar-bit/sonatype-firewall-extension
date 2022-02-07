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
  const COMPONENTS_RESULTS_TYPE = 'components';
  const { results, filterLoading, needsAcknowledgement, filtersAreDirty, loadResults, sortComponents, stateGo } = props,
    componentResults = results && results[COMPONENTS_RESULTS_TYPE],
    isLoading = !componentResults.results && !componentResults.error;

  const doLoad = () => {
    loadResults(COMPONENTS_RESULTS_TYPE);
  };

  useEffect(() => {
    if (!filterLoading && !needsAcknowledgement) {
      doLoad();
    }
  }, [filterLoading, needsAcknowledgement]);

  const tableProps = {
    reload: doLoad,
    colorStyler: componentResults && componentResults.classyBrew,
    componentResults,
    needsAcknowledgement,
    sortComponents,
    stateGo,
  };

  return (
    <div id="dashboard-components" className="iq-dashboard-components nx-viewport-sized__container">
      {filtersAreDirty && !needsAcknowledgement && !isLoading && <DashboardMask />}
      <DashboardComponentsTable {...tableProps} />
    </div>
  );
}

const dashboardResultsShape = PropTypes.shape({
  results: PropTypes.array,
  sortFields: PropTypes.arrayOf(PropTypes.string),
  numResults: PropTypes.number,
  error: PropTypes.string,
  classyBrew: heatMapColorStylerPropTypes,
});

DashboardComponents.propTypes = {
  results: PropTypes.shape({
    components: dashboardResultsShape,
  }),
  filterLoading: PropTypes.bool.isRequired,
  needsAcknowledgement: PropTypes.bool.isRequired,
  filtersAreDirty: PropTypes.bool.isRequired,
  loadResults: PropTypes.func.isRequired,
  sortComponents: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
};
