/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import MaximizedContainer from '../../react/MaximizedContainer';
import ReportStatusBar from './ReportStatusBar';
import ReportContent from './ReportContent';
import ReportFilters from './ReportFilters';
import ReportTitle from './ReportTitle';
import * as PropTypes from 'prop-types';

export default function ReportPage() {

  return (
    <MaximizedContainer id="app-react-report" className="nx-page-content">
      <aside className="nx-page-sidebar" id="report-sidebar">
        <ReportFilters/>
      </aside>
      <div className="nx-page-main">
        <ReportTitle/>
        <div className="nx-tile">
          <ReportStatusBar/>
        </div>
        <div className="nx-tile iq-report-content">
          <ReportContent/>
        </div>
      </div>
    </MaximizedContainer>
  );
}

ReportPage.propTypes = {
  appId: PropTypes.string,
  scanId: PropTypes.string
};
