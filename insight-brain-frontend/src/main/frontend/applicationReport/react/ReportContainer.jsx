/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import { loadReportMetadata } from './reportActions';
import { connect } from 'react-redux';
import ReportPage from './ReportPage';

function mapStateToProps({appReport, router}) {
  return {
    ...pick(['metadataDetails'], appReport),
    ...pick(['appId', 'scanId'], router.currentParams)
  };
}

const mapDispatchToProps = { loadReportMetadata };

const ReportPageContainer = connect(mapStateToProps, mapDispatchToProps)(ReportPage);
export default ReportPageContainer;
