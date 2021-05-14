/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as PropTypes from 'prop-types';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxOverflowTooltip,
  NxPagination,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxThreatIndicator,
} from '@sonatype/react-shared-components';

import { faSync } from '@fortawesome/pro-solid-svg-icons';

export default function FirewallQuarantineTable(props) {
  // actions
  const {
    loadQuarantineList,
    setQuarantineGridPage,
    setQuarantineGridSorting,
    setQuarantineGridPolicyFilter,
    selectQuarantineComponent,
  } = props;

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
    lastUpdated,
  } = props;

  // policiesState
  const { policies } = props;

  function sortPage(columnId) {
    let nextSortDir = sortField === columnId ? getNextSortDir(sortDir) : getNextSortDir(null);

    setQuarantineGridSorting(nextSortDir, nextSortDir === null ? null : columnId);
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
    <section id="firewall-quarantine-table">
      <header className="iq-firewall-table-header nx-page-title">
        <h2 className="nx-h2 iq-firewall-table-label">Quarantine</h2>
        <div className="iq-firewall-table__time">
          {lastUpdated && 'Updated ' + lastUpdated.toLocaleTimeString() + ' ' + lastUpdated.toLocaleDateString()}
        </div>
        <div className="nx-btn-bar">
          <NxButton
            id="firewall-quarantine-table--refresh-button"
            variant="tertiary"
            onClick={() => loadQuarantineList()}
          >
            <NxFontAwesomeIcon icon={faSync} />
            <span>Refresh</span>
          </NxButton>
        </div>
      </header>

      <div className="nx-table-container iq-firewall-quarantine-table">
        <NxTable id="pagination-firewall-quarantine-table" className="nx-table--fixed-layout">
          <NxTableHead>
            <NxTableRow>
              <NxTableCell isNumeric className="iq-cell--threat">
                Threat
              </NxTableCell>
              <NxTableCell className="iq-cell--policy-type">Policy Name</NxTableCell>
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
              <NxTableCell chevron />
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
                let policyViolation = getHighestPolicyViolation(row.quarantinePolicyViolations);

                return (
                  <NxTableRow isClickable={true} key={index} onClick={() => selectQuarantineComponent(index)}>
                    <NxTableCell isNumeric>
                      <NxThreatIndicator policyThreatLevel={policyViolation ? policyViolation.threatLevel : 0} />
                      <span>{policyViolation ? policyViolation.threatLevel : 0}</span>
                    </NxTableCell>
                    <NxTableCell className="iq-policy-cell">
                      <NxOverflowTooltip title={policyViolation ? policyViolation.policyName : 'None'}>
                        <div className="nx-truncate-ellipsis">
                          {policyViolation ? policyViolation.policyName : 'None'}
                        </div>
                      </NxOverflowTooltip>
                    </NxTableCell>
                    <NxTableCell>{new Date(row.quarantineDate).toLocaleDateString()}</NxTableCell>
                    <NxTableCell>
                      <NxOverflowTooltip title={row.componentDisplayText}>
                        <div className="nx-truncate-ellipsis">{row.componentDisplayText}</div>
                      </NxOverflowTooltip>
                    </NxTableCell>
                    <NxTableCell>
                      <NxOverflowTooltip title={row.repository}>
                        <div className="nx-truncate-ellipsis">{row.repository}</div>
                      </NxOverflowTooltip>
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
            aria-controls="pagination-firewall-quarantine-table"
            pageCount={quarantinePageCount}
            currentPage={currentPage}
            onChange={setQuarantineGridPage}
          />
        </div>
      </div>
    </section>
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
  lastUpdated: PropTypes.object,
  selectQuarantineComponent: PropTypes.func.isRequired,
};
