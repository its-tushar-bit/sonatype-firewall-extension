/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxButtonBar,
  NxTable,
  NxFilterInput,
  NxTile,
  NxTableContainer,
  NxThreatIndicator,
  NxIndeterminatePagination,
  NxOverflowTooltip,
  NxToggle,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faCheck, faFilter } from '@fortawesome/pro-solid-svg-icons';
import { useDispatch, useSelector } from 'react-redux';
import * as PropTypes from 'prop-types';
import { formatDate, FIREWALL_DATE_TIME_FORMAT } from 'MainRoot/util/dateUtils';
import {
  selectAggregate,
  selectCurrentPage,
  selectErrorComponentsTable,
  selectHasMoreResults,
  selectLoadingRepositoryComponents,
  selectRepositoryComponents,
  selectSearchFiltersValues,
  selectSortConfiguration,
} from '../repositoryResultsSummaryPageSelectors';
import { actions } from '../repositoryResultsSummaryPageSlice';
import { goToRepositoryComponentDetailsPage } from 'MainRoot/firewall/firewallActions';

const RepositoryResultsComponentsTable = ({ repositoryId }) => {
  const dispatch = useDispatch();

  const sortConfiguration = useSelector(selectSortConfiguration);
  const hasMoreResults = useSelector(selectHasMoreResults);
  const currentPage = useSelector(selectCurrentPage);
  const repositoryComponents = useSelector(selectRepositoryComponents);
  const searchFiltersValues = useSelector(selectSearchFiltersValues);
  const loadingRepositoryComponents = useSelector(selectLoadingRepositoryComponents);
  const errorComponentsTable = useSelector(selectErrorComponentsTable);
  const aggregate = useSelector(selectAggregate);

  const searchComponents = (value) => dispatch(actions.searchComponents(value));
  const sortComponents = (columnName) => dispatch(actions.sortComponents(columnName));
  const loadComponents = () => dispatch(actions.getRepositoryComponents(repositoryId));
  const loadPreviousPage = () => dispatch(actions.loadPreviousPage());
  const loadNextPage = () => dispatch(actions.loadNextPage());
  const openFilterPopover = () => dispatch(actions.setShowFilterPopover(true));

  const quarantineTime = (date) => formatDate(date, FIREWALL_DATE_TIME_FORMAT);
  const formatThreatLevel = (row) => (row.threatLevel === null ? 0 : row.threatLevel);
  const toggleAggregateAndGetRepositoryComponents = () => dispatch(actions.toggleAggregateAndGetRepositoryComponents());

  const aggregateByComponentToggleTooltip =
    'By default the Repository Report aggregates violations by component. ' +
    'To see all violations not Aggregated by Component, please switch the toggle off.';

  const componentsTableRows = repositoryComponents.map((row, idx) => (
    <NxTable.Row
      data-testid={row.policyName + row.componentDisplayText + idx}
      key={row.policyName + row.componentDisplayText + idx}
      isClickable
      onClick={() =>
        dispatch(
          goToRepositoryComponentDetailsPage(
            repositoryId,
            row.componentIdentifier,
            row.hash,
            row.matchStateId,
            row.pathname,
            row.componentDisplayText
          )
        )
      }
    >
      <NxTable.Cell className="iq-repository-threat-cell">
        <NxThreatIndicator policyThreatLevel={formatThreatLevel(row)} /> {formatThreatLevel(row)}
      </NxTable.Cell>
      <NxTable.Cell>{!row.policyName ? 'No Violations' : row.policyName}</NxTable.Cell>
      <NxTable.Cell>{quarantineTime(row.quarantineTime)}</NxTable.Cell>
      <NxTable.Cell className="iq-repository-component-cell">
        <NxOverflowTooltip>
          <div className="iq-repository-component-cell--text">{row.componentDisplayText}</div>
        </NxOverflowTooltip>
      </NxTable.Cell>
      <NxTable.Cell className="iq-repository-waived-cell">
        {row.waived && (
          <div>
            Waived
            <NxFontAwesomeIcon icon={faCheck} />
          </div>
        )}
      </NxTable.Cell>
      <NxTable.Cell className="iq-repository-chevron-cell" chevron />
    </NxTable.Row>
  ));

  return (
    <NxTile>
      <NxTile.Content>
        <NxButtonBar id="repository-report-button-bar">
          <NxTooltip title={aggregateByComponentToggleTooltip}>
            <NxToggle
              id="repository-report-aggregate-by-component-toggle"
              isChecked={aggregate}
              onChange={toggleAggregateAndGetRepositoryComponents}
            >
              Aggregate by component
            </NxToggle>
          </NxTooltip>
          <NxButton onClick={openFilterPopover} variant="tertiary" id="repository-filter-popover-button">
            <NxFontAwesomeIcon icon={faFilter} />
            <span>Filter</span>
          </NxButton>
        </NxButtonBar>
        <NxTableContainer id="iq-repository-summary-table">
          <NxTable data-testid="iq-repository-summary-table">
            <NxTable.Head>
              <NxTable.Row>
                <NxTable.Cell
                  isSortable
                  sortDir={
                    sortConfiguration.sortableField === 'POLICY_THREAT_LEVEL'
                      ? sortConfiguration.asc
                        ? 'asc'
                        : 'desc'
                      : null
                  }
                  onClick={() => sortComponents('POLICY_THREAT_LEVEL')}
                  className="iq-repository-column--threat"
                >
                  THREAT
                </NxTable.Cell>
                <NxTable.Cell
                  isSortable
                  sortDir={
                    sortConfiguration.sortableField === 'POLICY_NAME' ? (sortConfiguration.asc ? 'asc' : 'desc') : null
                  }
                  onClick={() => sortComponents('POLICY_NAME')}
                  className="iq-repository-column--policy-type"
                >
                  POLICY
                </NxTable.Cell>
                <NxTable.Cell
                  isSortable
                  sortDir={
                    sortConfiguration.sortableField === 'QUARANTINE_TIME'
                      ? sortConfiguration.asc
                        ? 'asc'
                        : 'desc'
                      : null
                  }
                  onClick={() => sortComponents('QUARANTINE_TIME')}
                  className="iq-repository-column--quarantine-date"
                >
                  QUARANTINE TIME
                </NxTable.Cell>
                <NxTable.Cell
                  isSortable
                  sortDir={
                    sortConfiguration.sortableField === 'COMPONENT_COORDINATES'
                      ? sortConfiguration.asc
                        ? 'asc'
                        : 'desc'
                      : null
                  }
                  onClick={() => sortComponents('COMPONENT_COORDINATES')}
                  className="iq-repository-column--component"
                >
                  COMPONENT
                </NxTable.Cell>
                <NxTable.Cell colSpan={2} />
              </NxTable.Row>
              <NxTable.Row isFilterHeader>
                <NxTable.Cell />
                <NxTable.Cell className="iq-repository-filter--policy">
                  <NxFilterInput
                    name="POLICY_NAME"
                    placeholder="policy name"
                    id="nx-repository-policy-filter"
                    onChange={(filterValue) => searchComponents({ filterValue, filterName: 'POLICY_NAME' })}
                    value={searchFiltersValues['POLICY_NAME']}
                  />
                </NxTable.Cell>
                <NxTable.Cell className="iq-repository-filter--quarantine">
                  <NxFilterInput
                    name="QUARANTINE_TIME"
                    placeholder="date"
                    id="nx-repository-quarantine-filter"
                    onChange={(filterValue) => searchComponents({ filterValue, filterName: 'QUARANTINE_TIME' })}
                    value={searchFiltersValues['QUARANTINE_TIME']}
                  />
                </NxTable.Cell>
                <NxTable.Cell className="iq-repository-filter--component" colSpan={3}>
                  <NxFilterInput
                    name="COMPONENT_COORDINATES"
                    placeholder="component name"
                    id="nx-repository-component-filter"
                    onChange={(filterValue) => searchComponents({ filterValue, filterName: 'COMPONENT_COORDINATES' })}
                    value={searchFiltersValues['COMPONENT_COORDINATES']}
                  />
                </NxTable.Cell>
              </NxTable.Row>
            </NxTable.Head>
            <NxTable.Body
              retryHandler={loadComponents}
              isLoading={loadingRepositoryComponents}
              error={errorComponentsTable}
              emptyMessage="No results"
            >
              {componentsTableRows}
            </NxTable.Body>
          </NxTable>
        </NxTableContainer>
        {!loadingRepositoryComponents &&
          (currentPage === 1 && !hasMoreResults ? null : (
            <div id="iq-repository-paginator-container-with-margin">
              <NxIndeterminatePagination
                data-testid="components-table-pagination"
                onPrevPageSelect={loadPreviousPage}
                onNextPageSelect={loadNextPage}
                isFirstPage={currentPage === 1}
                isLastPage={!hasMoreResults}
              />
            </div>
          ))}
      </NxTile.Content>
    </NxTile>
  );
};

export default RepositoryResultsComponentsTable;

RepositoryResultsComponentsTable.propTypes = {
  repositoryId: PropTypes.string.isRequired,
};
