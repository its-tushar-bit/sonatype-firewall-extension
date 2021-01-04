/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Component } from 'react';
import * as PropTypes from 'prop-types';

import BackButton from '../../react/BackButton';
import LoadWrapper from '../../react/LoadWrapper';
import ApplicationReportVulnerabilitiesHeader, { metadataPropType } from './ApplicationReportVulnerabilitiesHeader';
import ApplicationReportVulnerabilitiesTable, { vulnerabilitiesPropType }
  from './ApplicationReportVulnerabilitiesTable';

export default class ApplicationReportVulnerabilitiesPage extends Component {
  componentDidMount() {
    this.props.loadReportAllData();
  }

  render() {
    const error = this.props.loadError || (!this.props.loading && !this.props.vulnerabilitiesPageEnabled &&
        'This report has not been upgraded for the new policy-vulnerability linking introduced in release 67. ' +
        'Re-evaluate in order to enable this page') || undefined;

    return (
      <div id="application-report-vulnerabilities" className="nx-page-main nx-viewport-sized">
        <BackButton stateName="applicationReport.policy" $state={this.props.$state} />
        <LoadWrapper loading={!this.props.metadata || this.props.loading}
                     error={error}
                     retryHandler={this.props.loadReportAllData}>
          {() =>
            <div className="nx-tile nx-viewport-sized__container">
              <ApplicationReportVulnerabilitiesHeader metadata={this.props.metadata} />
              <ApplicationReportVulnerabilitiesTable vulnerabilities={this.props.vulnerabilities}
                                                     $state = {this.props.$state} />
            </div>
          }
        </LoadWrapper>
      </div>
    );
  }
}

ApplicationReportVulnerabilitiesPage.propTypes = {
  loadReportAllData: PropTypes.func.isRequired,
  metadata: metadataPropType,
  vulnerabilities: vulnerabilitiesPropType.isRequired,
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  vulnerabilitiesPageEnabled: PropTypes.bool.isRequired,
  $state: BackButton.propTypes.$state
};
