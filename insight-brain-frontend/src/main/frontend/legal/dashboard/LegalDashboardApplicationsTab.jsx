/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import {
  NxPagination,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow
} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { slice } from 'ramda';
import LegalDashboardApplicationRow from './LegalDashboardApplicationRow';
import { applicationPropType } from '../advancedLegalPropTypes';
import { DASHBOARD } from '../advancedLegalConstants';
import { isNilOrEmpty } from '../../util/jsUtil';
import LoadWrapper from '../../react/LoadWrapper';
import { Messages } from '../../util/CommonServices';

export default function LegalDashboardApplicationsTab({ applications, filtersAreDirty, fetchBackendPage }) {
  const [page, setPage] = useState(0);
  const { itemsPerPage, pagesToFill } = DASHBOARD.applications;
  const previousResultsBackend = (applications.backendPage - 1) * pagesToFill * itemsPerPage;
  const rows = slice(
      (page * itemsPerPage) - previousResultsBackend,
      ((page + 1) * itemsPerPage) - previousResultsBackend,
      applications.results);

  function onPageChange(newPage) {
    setPage(newPage);
    const backendPageNeeded = Math.ceil((newPage + 1) / pagesToFill);
    if (backendPageNeeded !== applications.backendPage) {
      fetchBackendPage('applications', backendPageNeeded);
    }
  }

  return (
    <div className="nx-scrollable nx-table-container nx-viewport-sized__scrollable">
      { filtersAreDirty && <div className="form-mask" /> }
      <NxTable id="legal-dashboard-applications-table" className="legal-dashboard-table">
        <NxTableHead>
          <NxTableRow>
            <NxTableCell>Application</NxTableCell>
            <NxTableCell>Last Scan</NxTableCell>
            <NxTableCell>App Categories</NxTableCell>
            <NxTableCell>Components Reviewed</NxTableCell>
          </NxTableRow>
        </NxTableHead>
        <NxTableBody emptyMessage="No applications found"
                     isLoading={applications.loading}
                     error={Messages.getHttpErrorMessage(applications.error)}>
          { rows.map((row, index) => <LegalDashboardApplicationRow key={ index } row={ row } />) }
        </NxTableBody>
      </NxTable>
      { applications && !isNilOrEmpty(applications.results) &&
        <div className="nx-table-container__footer">
          <NxPagination pageCount={ Math.ceil(applications.totalResultsCount / itemsPerPage) }
                        currentPage={ page }
                        onChange={ onPageChange } />
        </div>
      }
    </div>
  );
}

LegalDashboardApplicationsTab.propTypes = {
  applications: PropTypes.shape({
    results: PropTypes.arrayOf(applicationPropType).isRequired,
    totalResultsCount: PropTypes.number.isRequired,
    backendPage: PropTypes.number.isRequired,
    error: LoadWrapper.propTypes.error,
    loading: PropTypes.bool,
    sortFields: PropTypes.arrayOf(applicationPropType)
  }),
  fetchBackendPage: PropTypes.func.isRequired,
  filtersAreDirty: PropTypes.bool
};
