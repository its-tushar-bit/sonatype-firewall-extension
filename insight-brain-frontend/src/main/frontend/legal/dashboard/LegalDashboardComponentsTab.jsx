/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import { slice } from 'ramda';
import {
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxPagination,
} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import LegalDashboardComponentRow from './LegalDashboardComponentRow';
import { DASHBOARD } from '../advancedLegalConstants';
import { isNilOrEmpty } from '../../util/jsUtil';

export default function LegalDashboardComponentsTab({ components, fetchBackendPage, stateGo }) {
  const [page, setPage] = useState(components.backendPage - 1 || 0);
  const { itemsPerPage, pagesToFill } = DASHBOARD.components;
  const previousResultsBackend = (components.backendPage - 1) * pagesToFill * itemsPerPage;
  const rows = slice(
    page * itemsPerPage - previousResultsBackend,
    (page + 1) * itemsPerPage - previousResultsBackend,
    components.results
  );

  const emptyMessage = 'No components found given the applied filters and permissions.';

  function onPageChange(newPage) {
    setPage(newPage);
    const backendPageNeeded = Math.ceil((newPage + 1) / pagesToFill);
    if (backendPageNeeded !== components.backendPage) {
      fetchBackendPage('components', backendPageNeeded);
    }
  }

  return (
    <div className="nx-scrollable nx-table-container nx-viewport-sized__scrollable">
      <NxTable id="legal-dashboard-components-table" className="legal-dashboard-table">
        <NxTableHead>
          <NxTableRow>
            <NxTableCell>Component</NxTableCell>
            <NxTableCell>Licenses</NxTableCell>
            <NxTableCell>Applications</NxTableCell>
            <NxTableCell>Component Obligations</NxTableCell>
            <NxTableCell></NxTableCell>
          </NxTableRow>
        </NxTableHead>
        <NxTableBody isLoading={components.loading} emptyMessage={emptyMessage}>
          {rows.map((row, index) => (
            <LegalDashboardComponentRow key={index} row={row} stateGo={stateGo} />
          ))}
        </NxTableBody>
      </NxTable>
      {components && !isNilOrEmpty(components.results) && (
        <div className="nx-table-container__footer">
          <NxPagination
            pageCount={Math.ceil(components.totalResultsCount / itemsPerPage)}
            currentPage={page}
            onChange={onPageChange}
          />
        </div>
      )}
    </div>
  );
}

LegalDashboardComponentsTab.propTypes = {
  components: PropTypes.any,
  fetchBackendPage: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
};
