/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import LoadWrapper from '../../react/LoadWrapper';
import {
  NxButton,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow
} from '@sonatype/react-shared-components';
import LegalDashboardComponentRow from '../dashboard/LegalDashboardComponentsTab';
import NxFilterInput from '@sonatype/react-shared-components/components/NxFilterInput/NxFilterInput';

export default function LegalApplicationDetailsPage() {

  // get these from props
  const loading = false;
  const hasError = false;
  const loadResults = () => {};
  // const components = [];
  const rows = [];
  const applicationName = 'Test App';

  const onFilterChange = () => {};
  const sort = () => {};
  const sortDir = 'asc';
  const filterValue = '';

  return (
    <LoadWrapper loading={ loading } error={ hasError } retryHandler={ loadResults }>
      <aside id="legal-application-details-filter-container" className="nx-page-sidebar nx-viewport-sized">
        <div>This is the sidebar DLS-1040</div>
      </aside>
      <main id="legal-application-details-container" className="nx-page-main nx-viewport-sized">
        <div className="nx-page-title nx-page-title__actions">
          <h1 className="nx-h1">{ applicationName } Obligations</h1>
          <div className="nx-btn-bar">
            <NxButton variant="primary">Create Attribution Report</NxButton>
          </div>
        </div>
        <div className="nx-scrollable nx-table-container nx-viewport-sized__scrollable">
          <NxTable id="legal-dashboard-applications-table" className="legal-dashboard-table">
            <NxTableHead>
              <NxTableRow>
                <NxTableCell isSortable sortDir={sortDir} onClick={sort}>Component</NxTableCell>
                <NxTableCell isSortable sortDir={sortDir} onClick={sort}>Licenses</NxTableCell>
                <NxTableCell isSortable sortDir={sortDir} onClick={sort}>Completed Obligations</NxTableCell>
                <NxTableCell isSortable sortDir={sortDir} onClick={sort}>Review Status</NxTableCell>
              </NxTableRow>
              <NxTableRow isFilterHeader>
                <NxTableCell>
                  <NxFilterInput placeholder="Filter Components"
                                 onChange={onFilterChange}
                                 value={filterValue}/>
                </NxTableCell>
                <NxTableCell>
                  <NxFilterInput placeholder="Filter Licenses"
                                 onChange={onFilterChange}
                                 value={filterValue}/>
                </NxTableCell>
                <NxTableCell colspan={2}/>
              </NxTableRow>
            </NxTableHead>
            <NxTableBody emptyMessage="No components found">
              { rows.map((row, index) => <LegalDashboardComponentRow key = { index } row={ row } />) }
            </NxTableBody>
          </NxTable>
        </div>
      </main>
    </LoadWrapper>
  );
}

LegalApplicationDetailsPage.propTypes = {
};
