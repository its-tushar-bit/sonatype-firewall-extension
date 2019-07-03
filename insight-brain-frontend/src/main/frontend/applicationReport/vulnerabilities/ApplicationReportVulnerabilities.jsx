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
  $ngRedux: Provider.propTypes.store,
  $state: ApplicationReportVulnerabilitiesPage.propTypes.$state,
  applicationReportActions: PropTypes.shape({
    loadReportAllData: PropTypes.func.isRequired
  })
};
