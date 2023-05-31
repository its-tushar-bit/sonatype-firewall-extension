/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as PropTypes from 'prop-types';
import {
  NxOverflowTooltip,
  NxPagination,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
} from '@sonatype/react-shared-components';

import { formatDate, STANDARD_DATE_FORMAT } from 'MainRoot/util/dateUtils';

export default function FirewallUnquarantineTable(props) {
  // Actions
  const {
    loadReleaseQuarantineList,
    setAutoUnquarantineGridPage,
    setAutoUnquarantineGridSorting,
    selectReleaseQuarantineComponent,
  } = props;

  // autoUnquarantineState.autoUnquarantineGridState
  const {
    loadedReleaseQuarantineList,
    loadAutoUnquarantineGridError,
    releaseQuarantinePageCount,
    releaseQuarantineList,
    currentPage,
    sortDir,
    sortField,
  } = props;

  function setCurrentPage(newPage) {
    setAutoUnquarantineGridPage(newPage);
    loadReleaseQuarantineList();
  }

  function sortPage(columnId) {
    let nextSortDir = sortField === columnId ? getNextSortDir(sortDir) : getNextSortDir(null);

    setAutoUnquarantineGridSorting(nextSortDir, nextSortDir === null ? null : columnId);
    loadReleaseQuarantineList();
  }

  /**
   * Return the next sorting direction in the following order cycle:
   * null -> asc -> desc
   *
   * @param {String} sortDir Current sorting direction.
   *
   * @returns The next sorting direction in the cycle.
   */
  function getNextSortDir(sortDir) {
    return sortDir === null ? 'asc' : sortDir === 'asc' ? 'desc' : null;
  }

  return (
    <div className="nx-table-container iq-firewall-auto-unquarantine-table">
      <NxTable id="pagination-filter-table" className="nx-table--fixed-layout">
        <NxTableHead>
          <NxTableRow>
            <NxTableCell>Component</NxTableCell>
            <NxTableCell
              id="quarantineTime-header"
              className="iq-cell--quarantine-date"
              isSortable
              sortDir={sortField === 'quarantineTime' ? sortDir : null}
              onClick={() => sortPage('quarantineTime')}
            >
              Quarantine Date
            </NxTableCell>
            <NxTableCell className="iq-cell--repository">Repository</NxTableCell>
            <NxTableCell
              id="releaseQuarantineTime-header"
              className="iq-cell--date-cleared"
              isSortable
              sortDir={sortField === 'releaseQuarantineTime' ? sortDir : null}
              onClick={() => sortPage('releaseQuarantineTime')}
            >
              Date Cleared
            </NxTableCell>
            <NxTableCell chevron />
          </NxTableRow>
        </NxTableHead>

        <NxTableBody
          id="iq-firewall-auto-unquarantine-table-body"
          emptyMessage="No data found."
          error={loadAutoUnquarantineGridError}
          isLoading={!loadedReleaseQuarantineList}
        >
          {releaseQuarantineList &&
            releaseQuarantineList.map((row, index) => {
              return (
                <NxTableRow isClickable={true} key={index} onClick={() => selectReleaseQuarantineComponent(index)}>
                  <NxTableCell>
                    <NxOverflowTooltip title={row.componentDisplayText}>
                      <div className="nx-truncate-ellipsis">{row.componentDisplayText}</div>
                    </NxOverflowTooltip>
                  </NxTableCell>
                  <NxTableCell className="visual-testing-ignore">
                    {formatDate(row.quarantineDate, STANDARD_DATE_FORMAT)}
                  </NxTableCell>
                  <NxTableCell>
                    <NxOverflowTooltip title={row.repository}>
                      <div className="nx-truncate-ellipsis">{row.repository}</div>
                    </NxOverflowTooltip>
                  </NxTableCell>
                  <NxTableCell className="visual-testing-ignore">
                    {formatDate(row.dateCleared, STANDARD_DATE_FORMAT)}
                  </NxTableCell>
                  <NxTableCell chevron />
                </NxTableRow>
              );
            })}
        </NxTableBody>
      </NxTable>

      <div className="nx-table-container__footer">
        <NxPagination
          className="iq-firewall-table__nav-bar"
          aria-controls="pagination-filter-table"
          pageCount={releaseQuarantinePageCount}
          currentPage={currentPage}
          onChange={setCurrentPage}
        />
      </div>
    </div>
  );
}

FirewallUnquarantineTable.propTypes = {
  loadReleaseQuarantineList: PropTypes.func.isRequired,
  loadAutoUnquarantineGridError: PropTypes.string,
  setAutoUnquarantineGridPage: PropTypes.func.isRequired,
  setAutoUnquarantineGridSorting: PropTypes.func.isRequired,
  loadedReleaseQuarantineList: PropTypes.bool.isRequired,
  releaseQuarantinePageCount: PropTypes.number.isRequired,
  releaseQuarantineList: PropTypes.array.isRequired,
  pageSize: PropTypes.number.isRequired,
  currentPage: PropTypes.number,
  sortDir: PropTypes.string,
  sortField: PropTypes.string,
  selectReleaseQuarantineComponent: PropTypes.func.isRequired,
};
