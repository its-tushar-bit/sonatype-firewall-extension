/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import LoadWrapper from '../react/LoadWrapper';
import { formatDate } from '../util/dateUtils';

import QuarantineComponentOverviewTile from './componentOverviewTile/QuarantineComponentOverviewTile';
import QuarantineComponentOverviewDescriptionTile from './componentOverviewTile/QuarantinedComponentOverviewDescriptionTile';
import PolicyViolationsTile from 'MainRoot/quarantinedComponentReport/policyViolationsTile/PolicyViolationsTile';

export default function QuarantinedComponentReport(props) {
  // Url parameter
  const { token } = props;

  // Actions
  const { loadQuarantineReportData } = props;

  // viewState
  const { loadError, componentOverview, violations, violationsLoading, violationsLoadError } = props;

  const dataLoading = componentOverview.componentOverviewLoading || !componentOverview.componentDisplayName;

  useEffect(() => {
    loadQuarantineReportData(token);
  }, [token]);

  return (
    <main id="quarantined-component-report" className="nx-page-main">
      <div class="nx-page-title">
        <h1 class="nx-h1">Quarantine Report</h1>
        <div class="nx-page-title__description">{formatDate(new Date())}</div>
      </div>
      <QuarantineComponentOverviewDescriptionTile />
      <LoadWrapper retryHandler={() => loadQuarantineReportData(token)} error={loadError} loading={dataLoading}>
        <QuarantineComponentOverviewTile componentOverview={componentOverview} />
      </LoadWrapper>
      <LoadWrapper
        retryHandler={() => loadQuarantineReportData(token)}
        error={violationsLoadError}
        loading={violationsLoading}
      >
        <PolicyViolationsTile violations={violations} />
      </LoadWrapper>
    </main>
  );
}

QuarantinedComponentReport.propTypes = {
  token: PropTypes.string.isRequired,
  loadQuarantineReportData: PropTypes.func.isRequired,
  loadError: PropTypes.string,
  violationsLoading: PropTypes.bool,
  violationsLoadError: PropTypes.string,
};
