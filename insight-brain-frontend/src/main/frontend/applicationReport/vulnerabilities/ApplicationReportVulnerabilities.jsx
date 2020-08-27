/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Provider, connect } from 'react-redux';
import * as PropTypes from 'prop-types';
import { pick } from 'ramda';

import ApplicationReportVulnerabilitiesPage from './ApplicationReportVulnerabilitiesPage';

function mapStateToProps({ applicationReport }) {
  return {
    ...pick(['metadata', 'loadError', 'vulnerabilitiesPageEnabled'], applicationReport),
    vulnerabilities: applicationReport.vulnerabilities || [],
    loading: !!applicationReport.pendingLoads.size
  };
}

export default function ApplicationReportVulnerabilities({ $ngRedux, $state, applicationReportActions }) {
  const mapDispatchToProps = pick(['loadReportAllData'], applicationReportActions);

  const ConnectedApplicationReportVulnerabilitiesPage =
      connect(mapStateToProps, mapDispatchToProps)(ApplicationReportVulnerabilitiesPage);

  return (
    <Provider store={$ngRedux}>
      <ConnectedApplicationReportVulnerabilitiesPage $state={$state} />
    </Provider>
  );
}

ApplicationReportVulnerabilities.propTypes = {
  $ngRedux: PropTypes.shape({
    subscribe: PropTypes.func.isRequired,
    dispatch: PropTypes.func.isRequired,
    getState: PropTypes.func.isRequired
  }),
  $state: ApplicationReportVulnerabilitiesPage.propTypes.$state,
  applicationReportActions: PropTypes.shape({
    loadReportAllData: PropTypes.func.isRequired
  })
};
