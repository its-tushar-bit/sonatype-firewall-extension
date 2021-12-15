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
  NxTextInput,
  NxButton,
} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import LegalDashboardComponentRow from './LegalDashboardComponentRow';
import { DASHBOARD } from '../advancedLegalConstants';
import { isNilOrEmpty } from '../../util/jsUtil';

export default function LegalDashboardComponentsTab({
  components,
  fetchBackendPage,
  changeSortField,
  stateGo,
  searchByComponentName,
  setComponentSearchInputValue,
}) {
  const [page, setPage] = useState(components.backendPage - 1 || 0);
  const { itemsPerPage, pagesToFill } = DASHBOARD.components;
  const previousResultsBackend = (components.backendPage - 1) * pagesToFill * itemsPerPage;
  const rows = slice(
    page * itemsPerPage - previousResultsBackend,
    (page + 1) * itemsPerPage - previousResultsBackend,
    components.results
  );

  function getSortDir(fieldName) {
    const { sortField } = components;
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
    changeSortField('components', newSortField);
    setPage(0);
  }

  const emptyMessage = 'No components found given the applied filters and permissions.';

  function onPageChange(newPage) {
    setPage(newPage);
    const backendPageNeeded = Math.ceil((newPage + 1) / pagesToFill);
    if (backendPageNeeded !== components.backendPage) {
      fetchBackendPage('components', backendPageNeeded);
    }
  }

  function handleSubmit(e) {
    e.preventDefault();
    searchByComponentName();
    setPage(0);
  }

  return (
    <>
      <form id="legal-dashboard-component-searchbox-container" onSubmit={handleSubmit}>
        <NxTextInput validatable onChange={setComponentSearchInputValue} {...components.componentSearchInput} />
        <NxButton variant="primary" disabled={components.loading || components.componentSearchInput.validationErrors}>
          Search
        </NxButton>
      </form>
      <div className="nx-scrollable nx-table-container nx-viewport-sized__scrollable">
        <NxTable id="legal-dashboard-components-table" className="legal-dashboard-table">
          <NxTableHead>
            <NxTableRow>
              <NxTableCell
                id="component-component-name-header"
                isSortable
                sortDir={getSortDir('COMPONENT_NAME')}
                onClick={() => sort('COMPONENT_NAME')}
              >
                Component
              </NxTableCell>
              <NxTableCell
                id="component-license-name-header"
                isSortable
                sortDir={getSortDir('LICENSE_NAME')}
                onClick={() => sort('LICENSE_NAME')}
              >
                Licenses
              </NxTableCell>
              <NxTableCell
                id="component-application-count-header"
                isSortable
                sortDir={getSortDir('APPLICATION_COUNT')}
                onClick={() => sort('APPLICATION_COUNT')}
              >
                Applications
              </NxTableCell>
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
    </>
  );
}

LegalDashboardComponentsTab.propTypes = {
  components: PropTypes.any,
  searchByComponentName: PropTypes.func.isRequired,
  fetchBackendPage: PropTypes.func.isRequired,
  changeSortField: PropTypes.func.isRequired,
  loadResults: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  setComponentSearchInputValue: PropTypes.func,
};
