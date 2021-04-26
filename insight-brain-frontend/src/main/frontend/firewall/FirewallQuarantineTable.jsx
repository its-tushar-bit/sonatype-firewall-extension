/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as PropTypes from 'prop-types';
import {
  NxPagination,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxOverflowTooltip,
  NxThreatIndicator,
} from '@sonatype/react-shared-components';

export default function FirewallQuarantineTable(props) {
  // actions
  const { setQuarantineGridPage, setQuarantineGridSorting, setQuarantineGridPolicyFilter } = props;

  // quarantineState.quarantineGridState
  const {
    loadedQuarantineList,
    loadQuarantineGridError,
    quarantinePageCount,
    quarantineList,
    currentPage,
    sortDir,
    sortField,
    filterPolicy,
  } = props;

  // policiesState
  const { policies } = props;

  function sortPage(columnId) {
    let nextSortDir = sortField === columnId ? getNextSortDir(sortDir) : getNextSortDir(null);

    setQuarantineGridSorting(nextSortDir, columnId);
  }

  /**
   * Return the next sorting direction in the following order cycle:
   * null -> asc -> desc
   *
   * @param {String} sortDir Current sorting direction.
   *
   * @returns The next sorting direction in the cycle.
   */
  function getNextSortDir(curSort) {
    return curSort === null ? 'asc' : curSort === 'asc' ? 'desc' : null;
  }

  function getHighestPolicyViolation(policyViolations) {
    // A quarantined component is expected to have at least one violation.
    let chosenViolation = policyViolations[0];

    // Start at 0 because the first element could match the filter criteria.
    // If not, it's a noop comparing the threat level to itself.
    for (let i = 0; i < policyViolations.length; i++) {
      let policyViolation = policyViolations[i];

      // If we match filter criteria, that takes precedence so break the loop.
      if (filterPolicy === policyViolation.policyId) {
        chosenViolation = policyViolation;
        break;
      } else if (policyViolation.threatLevel > chosenViolation.threatLevel) {
        chosenViolation = policyViolation;
      }
    }

    return chosenViolation;
  }

  return (
    <div id="firewall-quarantine-table" className="nx-table-container iq-firewall-quarantine-table">
      <NxTable id="pagination-firewall-quarantine-table" className="nx-table--fixed-layout">
        <NxTableHead>
          <NxTableRow>
            <NxTableCell isNumeric className="iq-cell--threat">
              Threat
            </NxTableCell>
            <NxTableCell className="iq-cell--policy-type">Policy Type</NxTableCell>
            <NxTableCell
              id="quarantineTime-header"
              className="iq-cell--quarantine-date"
              isSortable
              sortDir={sortField === 'quarantineTime' ? sortDir : null}
              onClick={() => sortPage('quarantineTime')}
            >
              Quarantine Date
            </NxTableCell>
            <NxTableCell>Component</NxTableCell>
            <NxTableCell className="iq-cell--repository">Repository</NxTableCell>
          </NxTableRow>

          <NxTableRow isFilterHeader>
            <NxTableCell />
            <NxTableCell>
              <select
                className="nx-form-select"
                onChange={(event) => setQuarantineGridPolicyFilter(event.currentTarget.value)}
                value={filterPolicy}
              >
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
          id="iq-firewall-quarantine-table-body"
          emptyMessage="No data found."
          error={loadQuarantineGridError}
          isLoading={!loadedQuarantineList}
        >
          {quarantineList &&
            quarantineList.map((row, index) => {
              let policyViolation = getHighestPolicyViolation(row.policyViolations);

              return (
                <NxTableRow key={index}>
                  <NxTableCell isNumeric>
                    <NxThreatIndicator policyThreatLevel={policyViolation.threatLevel} />
                    <span>{policyViolation.threatLevel}</span>
                  </NxTableCell>
                  <NxTableCell className="iq-policy-cell">
                    <NxOverflowTooltip title={policyViolation.policyName}>
                      <div className="nx-truncate-ellipsis">{policyViolation.policyName}</div>
                    </NxOverflowTooltip>
                  </NxTableCell>
                  <NxTableCell>{new Date(row.quarantineDate).toLocaleDateString()}</NxTableCell>
                  <NxTableCell>
                    <NxOverflowTooltip title={row.displayName}>
                      <div className="nx-truncate-ellipsis">{row.displayName}</div>
                    </NxOverflowTooltip>
                  </NxTableCell>
                  <NxTableCell>
                    <NxOverflowTooltip title={row.repository}>
                      <div className="nx-truncate-ellipsis">{row.repository}</div>
                    </NxOverflowTooltip>
                  </NxTableCell>
                </NxTableRow>
              );
            })}
        </NxTableBody>
      </NxTable>

      <div className="nx-table-container__footer">
        <NxPagination
          className="iq-firewall-table__nav-bar"
          aria-controls="pagination-firewall-quarantine-table"
          pageCount={quarantinePageCount}
          currentPage={currentPage}
          onChange={setQuarantineGridPage}
        />
      </div>
    </div>
  );
}

FirewallQuarantineTable.propTypes = {
  loadQuarantineList: PropTypes.func.isRequired,
  loadQuarantineGridError: PropTypes.string,
  setQuarantineGridPage: PropTypes.func.isRequired,
  setQuarantineGridSorting: PropTypes.func.isRequired,
  setQuarantineGridPolicyFilter: PropTypes.func.isRequired,
  loadedQuarantineList: PropTypes.bool.isRequired,
  quarantineList: PropTypes.array.isRequired,
  quarantinePageCount: PropTypes.number.isRequired,
  policies: PropTypes.array.isRequired,
  pageSize: PropTypes.number.isRequired,
  currentPage: PropTypes.number,
  sortDir: PropTypes.string,
  sortField: PropTypes.string,
  filterPolicy: PropTypes.string,
};
