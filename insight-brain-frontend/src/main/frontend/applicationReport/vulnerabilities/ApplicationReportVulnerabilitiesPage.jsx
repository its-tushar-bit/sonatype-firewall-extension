/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';

import BackButton from 'MainRoot/applicationReport/BackButton';
import LoadWrapper from '../../react/LoadWrapper';
import ApplicationReportVulnerabilitiesHeader, { metadataPropType } from './ApplicationReportVulnerabilitiesHeader';
import ApplicationReportVulnerabilitiesTable, {
  vulnerabilitiesPropType,
} from './ApplicationReportVulnerabilitiesTable';

const ApplicationReportVulnerabilitiesPage = ({
  loadError,
  loading,
  vulnerabilitiesPageEnabled,
  metadata,
  vulnerabilities,
  loadReportAllData,
}) => {
  useEffect(() => {
    loadReportAllData();
  }, []);

  const error =
    loadError ||
    (!loading &&
      !vulnerabilitiesPageEnabled &&
      'This report has not been upgraded for the new policy-vulnerability linking introduced in release 67. ' +
        'Re-evaluate in order to enable this page') ||
    undefined;

  return (
    <div id="application-report-vulnerabilities" className="nx-page-main nx-viewport-sized">
      <BackButton />
      <LoadWrapper loading={!metadata || loading} error={error} retryHandler={loadReportAllData}>
        {() => (
          <div className="nx-tile nx-viewport-sized__container">
            <ApplicationReportVulnerabilitiesHeader metadata={metadata} />
            <ApplicationReportVulnerabilitiesTable vulnerabilities={vulnerabilities} />
          </div>
        )}
      </LoadWrapper>
    </div>
  );
};

export default ApplicationReportVulnerabilitiesPage;

ApplicationReportVulnerabilitiesPage.propTypes = {
  loadReportAllData: PropTypes.func.isRequired,
  metadata: metadataPropType,
  vulnerabilities: vulnerabilitiesPropType.isRequired,
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  vulnerabilitiesPageEnabled: PropTypes.bool.isRequired,
};
