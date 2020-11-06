/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import MaximizedContainer from '../react/MaximizedContainer';
import LoadWrapper from '../react/LoadWrapper';
import {
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow
} from '@sonatype/react-shared-components';
import { getLicenseLegalApplicationReportUrl } from '../util/CLMLocation';

export default function AdvancedLegalApplicationsPage(props) {
  const {
    // actions
    loadApplications,
    // state
    viewStateApplications,
    applications,
    $state
  } = props;

  useEffect(() => {
    loadApplications();
  }, []);

  return (
    <MaximizedContainer id="advanced-legal-applications">
      <div className="nx-page">
        <div className="nx-page-content">
          <main className="nx-page-main">
            <LoadWrapper loading={viewStateApplications.loading}
                         error={viewStateApplications.error}
                         retryHandler={loadApplications}>
              <NxTable>
                <NxTableHead>
                  <NxTableRow>
                    <NxTableCell>Application</NxTableCell>
                    <NxTableCell>Attribution Report</NxTableCell>
                    <NxTableCell>Download Raw Legal Data</NxTableCell>
                  </NxTableRow>
                </NxTableHead>
                <NxTableBody emptyMessage="No applications found">
                  {applications.map(application =>
                    <NxTableRow key={application.id}>
                      <NxTableCell>{application.name}</NxTableCell>
                      <NxTableCell>
                        <a href={$state.href('advancedLegalApplication', { publicId: application.publicId })}
                           target="_blank" rel="noopener noreferrer" >
                          View Attribution Report
                        </a>
                      </NxTableCell>
                      <NxTableCell>
                        <a href={getLicenseLegalApplicationReportUrl(application.publicId)}
                           target="_blank" rel="noopener noreferrer" >
                          Download Application Legal Data
                        </a>
                      </NxTableCell>
                    </NxTableRow>
                  )}
                </NxTableBody>
              </NxTable>
            </LoadWrapper>
          </main>
        </div>
      </div>
    </MaximizedContainer>
  );
}

AdvancedLegalApplicationsPage.propTypes = {
  loadApplications: PropTypes.func.isRequired,
  viewStateApplications: PropTypes.shape({
    loading: PropTypes.bool.isRequired,
    error: PropTypes.string
  }),
  applications: PropTypes.array.isRequired,
  $state: PropTypes.object.isRequired
};
