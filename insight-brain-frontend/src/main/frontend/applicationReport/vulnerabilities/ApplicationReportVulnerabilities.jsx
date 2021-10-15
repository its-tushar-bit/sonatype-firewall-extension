/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { pick } from 'ramda';
import { loadReportAllData } from '../applicationReportActions';
import ApplicationReportVulnerabilitiesPage from './ApplicationReportVulnerabilitiesPage';

function mapStateToProps({ applicationReport }) {
  return {
    ...pick(['metadata', 'loadError', 'vulnerabilitiesPageEnabled'], applicationReport),
    vulnerabilities: applicationReport.vulnerabilities || [],
    loading: !!applicationReport.pendingLoads.size,
  };
}

const mapDispatchToProps = { loadReportAllData };

export default connect(mapStateToProps, mapDispatchToProps)(ApplicationReportVulnerabilitiesPage);
