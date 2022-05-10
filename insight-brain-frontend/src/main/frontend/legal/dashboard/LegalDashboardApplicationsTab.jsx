/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  NxPagination,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { slice } from 'ramda';
import LegalDashboardApplicationRow from './LegalDashboardApplicationRow';
import { applicationsTabPropType } from '../advancedLegalPropTypes';
import { DASHBOARD } from '../advancedLegalConstants';
import { isNilOrEmpty } from '../../util/jsUtil';
import { Messages } from '../../utilAngular/CommonServices';
import DashboardMask from '../../dashboard/results/dashboardMask/DashboardMask';

export default function LegalDashboardApplicationsTab({
  applications,
  filtersAreDirty,
  fetchBackendPage,
  changeSortField,
  stateGo,
  legalDashboardSetPage,
}) {
  const page = applications.page || 0;
  const { itemsPerPage, pagesToFill } = DASHBOARD.applications;
  const previousResultsBackend = (applications.backendPage - 1) * pagesToFill * itemsPerPage;
  const rows = slice(
    page * itemsPerPage - previousResultsBackend,
    (page + 1) * itemsPerPage - previousResultsBackend,
    applications.results
  );

  function onPageChange(newPage) {
    legalDashboardSetPage({ resultsType: 'applications', page: newPage });
    const backendPageNeeded = Math.ceil((newPage + 1) / pagesToFill);
    if (backendPageNeeded !== applications.backendPage) {
      fetchBackendPage('applications', backendPageNeeded);
    }
  }

  function getSortDir(fieldName) {
    const { sortField } = applications;
    if (sortField && sortField.startsWith(fieldName)) {
      return sortField.endsWith('ASC') ? 'asc' : 'desc';
    }
    return null;
  }

  function sort(fieldName) {
    let newSortField = '';
    switch (getSortDir(fieldName)) {
      case 'asc':
        newSortField = `${fieldName}_DESC`;
        break;
      case 'desc':
        newSortField = null;
        break;
      default:
        newSortField = `${fieldName}_ASC`;
        break;
    }
    changeSortField('applications', newSortField);
    legalDashboardSetPage({ resultsType: 'applications', page: 0 });
  }

  const emptyMessage = 'No applications found given the applied filters and permissions.';

  return (
    <div className="nx-scrollable nx-table-container nx-viewport-sized__scrollable">
      {filtersAreDirty && <DashboardMask />}
      <NxTable id="legal-dashboard-applications-table" className="legal-dashboard-table">
        <NxTableHead>
          <NxTableRow>
            <NxTableCell isSortable sortDir={getSortDir('APPLICATION_NAME')} onClick={() => sort('APPLICATION_NAME')}>
              Application
            </NxTableCell>
            <NxTableCell isSortable sortDir={getSortDir('LAST_SCAN_TIME')} onClick={() => sort('LAST_SCAN_TIME')}>
              Last Scan
            </NxTableCell>
            <NxTableCell isSortable sortDir={getSortDir('TAG_NAMES')} onClick={() => sort('TAG_NAMES')}>
              App Categories
            </NxTableCell>
            <NxTableCell>Components Reviewed</NxTableCell>
            <NxTableCell chevron />
          </NxTableRow>
        </NxTableHead>
        <NxTableBody
          emptyMessage={emptyMessage}
          isLoading={applications.loading}
          error={Messages.getHttpErrorMessage(applications.error)}
        >
          {rows.map((row, index) => (
            <LegalDashboardApplicationRow key={index} row={row} stateGo={stateGo} />
          ))}
        </NxTableBody>
      </NxTable>
      {applications && !isNilOrEmpty(applications.results) && (
        <div className="nx-table-container__footer">
          <NxPagination
            pageCount={Math.ceil(applications.totalResultsCount / itemsPerPage)}
            currentPage={page}
            onChange={onPageChange}
          />
        </div>
      )}
    </div>
  );
}

LegalDashboardApplicationsTab.propTypes = {
  applications: applicationsTabPropType,
  fetchBackendPage: PropTypes.func.isRequired,
  filtersAreDirty: PropTypes.bool,
  changeSortField: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  legalDashboardSetPage: PropTypes.func.isRequired,
};
