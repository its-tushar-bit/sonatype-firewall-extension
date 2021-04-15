/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import {
  NxPagination,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxOverflowTooltip,
} from '@sonatype/react-shared-components';

export default function FirewallUnquarantineTable(props) {
  // Actions
  const {
    loadReleaseQuarantineList,
    loadAutoUnquarantineGridData,
    setAutoUnquarantineGridPage,
    setAutoUnquarantineGridSorting,
    setAutoUnquarantineGridPolicyFilter,
  } = props;

  // autoUnquarantineState.autoUnquarantineGridState
  const {
    loadedReleaseQuarantineList,
    loadAutoUnquarantineGridError,
    releaseQuarantinePageCount,
    releaseQuarantineList,
    policies,
    currentPage,
    sortDir,
    sortField,
    filterPolicyId,
  } = props;

  useEffect(() => {
    // Load first page with no sorting or filtering and the policies for filtering.
    loadAutoUnquarantineGridData();
  }, []);

  function setCurrentPage(newPage) {
    setAutoUnquarantineGridPage(newPage);
    loadReleaseQuarantineList();
  }

  function sortPage(columnId) {
    let nextSortDir = sortField === columnId ? getNextSortDir(sortDir) : getNextSortDir(null);

    setAutoUnquarantineGridSorting(nextSortDir, columnId);
    loadReleaseQuarantineList();
  }

  function setPolicyIdFilter(event) {
    let policyId = event.currentTarget.value;

    setAutoUnquarantineGridPolicyFilter(policyId);
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

  function getHighestPolicyViolationName(policyViolations) {
    const reducer = (policy, currentValue) =>
      policy.threatLevel > currentValue.threatLevel || policy.policyId === filterPolicyId ? policy : currentValue;

    return policyViolations.length === 0 ? '' : policyViolations.reduce(reducer).policyName;
  }

  return (
    <div className="nx-table-container iq-firewall-auto-unquarantine-table">
      <NxTable id="pagination-filter-table">
        <NxTableHead>
          <NxTableRow>
            <NxTableCell>Component</NxTableCell>
            <NxTableCell>Policy Type</NxTableCell>
            <NxTableCell
              id="quarantineTime-header"
              isSortable
              sortDir={sortField === 'quarantineTime' ? sortDir : null}
              onClick={() => sortPage('quarantineTime')}
            >
              Quarantine Date
            </NxTableCell>
            <NxTableCell>Repository</NxTableCell>
            <NxTableCell
              id="releaseQuarantineTime-header"
              isSortable
              sortDir={sortField === 'releaseQuarantineTime' ? sortDir : null}
              onClick={() => sortPage('releaseQuarantineTime')}
            >
              Date Cleared
            </NxTableCell>
          </NxTableRow>

          <NxTableRow isFilterHeader>
            <NxTableCell />
            <NxTableCell>
              <select className="nx-form-select" onChange={setPolicyIdFilter} value={filterPolicyId}>
                {/* Effectively clears the filter. */}
                <option value={''}></option>

                {policies &&
                  policies.map((policy) => (
                    <option key={policy.id} value={policy.id}>
                      {policy.name}
                    </option>
                  ))}
              </select>
            </NxTableCell>
            <NxTableCell />
            <NxTableCell />
            <NxTableCell />
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
              let policyViolationName = getHighestPolicyViolationName(row.policyViolations);

              return (
                <NxTableRow key={index}>
                  <NxTableCell className="iq-firewall-grid-component">
                    <NxOverflowTooltip title={row.displayName}>
                      <div className="nx-truncate-ellipsis">{row.displayName}</div>
                    </NxOverflowTooltip>
                  </NxTableCell>
                  <NxTableCell>
                    <NxOverflowTooltip title={policyViolationName}>
                      <div className="nx-truncate-ellipsis">{policyViolationName}</div>
                    </NxOverflowTooltip>
                  </NxTableCell>
                  <NxTableCell>{new Date(row.quarantineDate).toLocaleDateString()}</NxTableCell>
                  <NxTableCell>
                    <NxOverflowTooltip title={row.repository}>
                      <div className="nx-truncate-ellipsis">{row.repository}</div>
                    </NxOverflowTooltip>
                  </NxTableCell>
                  <NxTableCell>{new Date(row.dateCleared).toLocaleDateString()}</NxTableCell>
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
  loadAutoUnquarantineGridData: PropTypes.func.isRequired,
  loadReleaseQuarantineList: PropTypes.func.isRequired,
  loadAutoUnquarantineGridError: PropTypes.string.isRequired,
  setAutoUnquarantineGridPage: PropTypes.func.isRequired,
  setAutoUnquarantineGridSorting: PropTypes.func.isRequired,
  setAutoUnquarantineGridPolicyFilter: PropTypes.func.isRequired,
  loadedReleaseQuarantineList: PropTypes.bool.isRequired,
  loadedPolicies: PropTypes.bool.isRequired,
  releaseQuarantinePageCount: PropTypes.number.isRequired,
  releaseQuarantineList: PropTypes.array.isRequired,
  policies: PropTypes.array.isRequired,
  pageSize: PropTypes.number.isRequired,
  currentPage: PropTypes.number,
  sortDir: PropTypes.string,
  sortField: PropTypes.string,
  filterPolicyId: PropTypes.string,
};
