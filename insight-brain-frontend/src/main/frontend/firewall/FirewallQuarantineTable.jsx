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
  NxTextLink,
  NxThreatIndicator,
  NxFilterInput,
  NxStatefulFilterDropdown,
} from '@sonatype/react-shared-components';

import { faSync } from '@fortawesome/pro-solid-svg-icons';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { formatDate, FIREWALL_TIME_DATE_FORMAT, FIREWALL_DATE_TIME_FORMAT } from 'MainRoot/util/dateUtils';

import './_firewall.scss';
import QuarantineTimeFilter from 'MainRoot/firewall/quarantineTable/QuarantineTimeFilter';

export default function FirewallQuarantineTable(props) {
  // actions
  const {
    loadQuarantineList,
    setQuarantineGridPage,
    setQuarantineGridSorting,
    setQuarantineGridPolicyFilter,
    setQuarantineGridComponentNameFilter,
    setQuarantineGridRepositoryPublicIdFilter,
    setQuarantineGridQuarantineTimeFilter,
    goToRepositoryComponentDetailsPage,
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
    filterPolicies,
    filterComponentName,
    filterRepositoryPublicId,
    filterQuarantineTime,
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
    return curSort === null || curSort === 'asc' ? 'desc' : 'asc';
  }

  const uiRouterState = useRouterState();

  const options = policies ? policies.map(({ id, name: displayName }) => ({ id, displayName })) : [];

  return (
    <section id="firewall-quarantine-table">
      <header className="iq-firewall-table-header nx-page-title">
        <h2 className="nx-h2 iq-firewall-table-label">Components Actively in Quarantine</h2>
        <div className="iq-firewall-table__time visual-testing-ignore">
          {lastUpdated && 'Updated ' + formatDate(lastUpdated, FIREWALL_TIME_DATE_FORMAT)}
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
              <NxTableCell
                id="policyName-header"
                className="iq-cell--policy-type"
                isSortable
                sortDir={sortField === 'policyName' ? sortDir : null}
                onClick={() => sortPage('policyName')}
              >
                Policy Name
              </NxTableCell>
              <NxTableCell
                id="quarantineTime-header"
                className="iq-cell--quarantine-date"
                isSortable
                sortDir={sortField === 'quarantineTime' ? sortDir : null}
                onClick={() => sortPage('quarantineTime')}
              >
                Quarantine Time
              </NxTableCell>
              <NxTableCell
                id="component-header"
                className="iq-cell--component"
                isSortable
                sortDir={sortField === 'componentDisplayName' ? sortDir : null}
                onClick={() => sortPage('componentDisplayName')}
              >
                Component
              </NxTableCell>
              <NxTableCell
                id="repository-header"
                className="iq-cell--repository"
                isSortable
                sortDir={sortField === 'repositoryPublicId' ? sortDir : null}
                onClick={() => sortPage('repositoryPublicId')}
              >
                Repository
              </NxTableCell>
            </NxTableRow>

            <NxTableRow isFilterHeader>
              <NxTableCell />
              <NxTableCell>
                <NxStatefulFilterDropdown
                  id="firewall-quarantine-table--select-policy"
                  options={options}
                  onChange={(selectedIds) => setQuarantineGridPolicyFilter(Array.from(selectedIds))}
                  // Only set selected ids when options is population,
                  // this is to prevent race condition.
                  selectedIds={new Set(options.length ? filterPolicies : [])}
                />
              </NxTableCell>
              <NxTableCell>
                <QuarantineTimeFilter
                  setQuarantineGridQuarantineTimeFilter={setQuarantineGridQuarantineTimeFilter}
                  filterQuarantineTime={filterQuarantineTime}
                />
              </NxTableCell>
              <NxTableCell>
                <NxFilterInput
                  id="firewall-quarantine-table--component-name"
                  placeholder="component name"
                  onChange={(value) => setQuarantineGridComponentNameFilter(value)}
                  value={filterComponentName}
                />
              </NxTableCell>
              <NxTableCell>
                <NxFilterInput
                  id="firewall-quarantine-table--repository-public-id"
                  placeholder="repository"
                  onChange={(value) => setQuarantineGridRepositoryPublicIdFilter(value)}
                  value={filterRepositoryPublicId}
                />
              </NxTableCell>
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
                return (
                  <NxTableRow key={index}>
                    <NxTableCell isNumeric>
                      <NxThreatIndicator policyThreatLevel={row.threatLevel === null ? 0 : row.threatLevel} />
                      <span>{row.threatLevel === null ? 0 : row.threatLevel}</span>
                    </NxTableCell>
                    <NxTableCell className="iq-policy-cell">
                      <NxOverflowTooltip title={!row.policyName ? 'None' : row.policyName}>
                        <div className="nx-truncate-ellipsis">{!row.policyName ? 'None' : row.policyName}</div>
                      </NxOverflowTooltip>
                    </NxTableCell>
                    <NxTableCell className="visual-testing-ignore">
                      {formatDate(row.quarantineDate, FIREWALL_DATE_TIME_FORMAT)}
                    </NxTableCell>
                    <NxTableCell className="iq-firewall-quarantine-table__component-cell">
                      <NxOverflowTooltip title={row.componentDisplayText}>
                        <div className="nx-truncate-ellipsis">
                          <button
                            className="nx-text-link"
                            id="iq-firewall-quarantine-table--component-details-page"
                            onClick={() =>
                              goToRepositoryComponentDetailsPage(
                                row.repositoryId,
                                row.componentIdentifier,
                                row.hash,
                                row.matchState,
                                row.pathname,
                                row.componentDisplayText
                              )
                            }
                          >
                            {row.componentDisplayText}
                          </button>
                        </div>
                      </NxOverflowTooltip>
                    </NxTableCell>
                    <NxTableCell>
                      <NxOverflowTooltip title={row.repositoryName}>
                        <div className="nx-truncate-ellipsis">
                          <NxTextLink
                            id="iq-firewall-quarantine-table--repo-view-link"
                            href={uiRouterState.href('repository-report', { repositoryId: row.repositoryId })}
                            truncate
                          >
                            {row.repositoryName}
                          </NxTextLink>
                        </div>
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
    </section>
  );
}

FirewallQuarantineTable.propTypes = {
  loadQuarantineList: PropTypes.func.isRequired,
  loadQuarantineGridError: PropTypes.string,
  setQuarantineGridPage: PropTypes.func.isRequired,
  setQuarantineGridSorting: PropTypes.func.isRequired,
  setQuarantineGridPolicyFilter: PropTypes.func.isRequired,
  setQuarantineGridComponentNameFilter: PropTypes.func.isRequired,
  setQuarantineGridRepositoryPublicIdFilter: PropTypes.func.isRequired,
  setQuarantineGridQuarantineTimeFilter: PropTypes.func.isRequired,
  loadedQuarantineList: PropTypes.bool.isRequired,
  quarantineList: PropTypes.array.isRequired,
  quarantinePageCount: PropTypes.number.isRequired,
  policies: PropTypes.array.isRequired,
  pageSize: PropTypes.number.isRequired,
  currentPage: PropTypes.number,
  sortDir: PropTypes.string,
  sortField: PropTypes.string,
  filterPolicies: PropTypes.array,
  filterComponentName: PropTypes.string,
  filterRepositoryPublicId: PropTypes.string,
  lastUpdated: PropTypes.object,
  goToRepositoryComponentDetailsPage: PropTypes.func,
};
