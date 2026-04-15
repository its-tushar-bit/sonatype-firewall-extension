/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { faFilter } from '@fortawesome/pro-solid-svg-icons';
import {
  NxButton,
  NxButtonBar,
  NxFilterInput,
  NxFontAwesomeIcon,
  NxIndeterminatePagination,
  NxOverflowTooltip,
  NxTable,
  NxTableContainer,
  NxThreatIndicator,
  NxTile,
} from '@sonatype/react-shared-components';
import * as R from 'ramda';
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';

import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { FIREWALL_CONTAINER_REPOSITORY_RESULTS } from 'MainRoot/constants/states/firewall';
import { FIREWALL_DATE_TIME_FORMAT, formatDate } from 'MainRoot/util/dateUtils';

import selectContainerRepositoryResultsPage from '../containerRepositoryResultsPageSelectors';
import { actions } from '../containerRepositoryResultsPageSlice';

const ContainerRepositoryResultsTable = () => {
  const dispatch = useDispatch();

  const { loading, errorMessage, results, columnFilters, sortConfiguration, pagination } = useSelector(
    selectContainerRepositoryResultsPage
  );

  const setShowFilterDrawer = (value) => dispatch(actions.setShowFilterDrawer(value));
  const searchFilterColumn = (column, value) => dispatch(actions.searchFilterColumn({ column, value }));
  const sortColumn = (column) => {
    dispatch(actions.setLoading(true));
    dispatch(actions.sortColumn(column));
    dispatch(actions.loadTable());
    dispatch(actions.setLoading(false));
  };
  const loadPreviousPage = () => {
    dispatch(actions.setLoading(true));
    dispatch(actions.loadPreviousPage());
    dispatch(actions.setLoading(false));
  };
  const loadNextPage = () => {
    dispatch(actions.setLoading(true));
    dispatch(actions.loadNextPage());
    dispatch(actions.setLoading(false));
  };
  const loadTable = async () => {
    dispatch(actions.setLoading(true));
    await dispatch(actions.loadTable());
    dispatch(actions.setLoading(false));
  };

  // formatters
  const quarantineTime = (date) => formatDate(date, FIREWALL_DATE_TIME_FORMAT);
  const formatThreatLevel = (row) => R.when(R.isNil, R.always(0))(row.threatLevel);

  const tableRows = (results || []).map((row, index) => (
    <NxTable.Row
      data-testid={row.policyName + row.applicationPublicId + index}
      key={index}
      isClickable
      onClick={() =>
        dispatch(
          stateGo('firewall.containerReport', {
            origin: FIREWALL_CONTAINER_REPOSITORY_RESULTS,
            publicId: row.applicationPublicId,
            scanId: row.scanId,
          })
        )
      }
    >
      <NxTable.Cell className="container-repository-results-table__threat-level-cell">
        <NxThreatIndicator policyThreatLevel={formatThreatLevel(row)} /> {formatThreatLevel(row)}
      </NxTable.Cell>

      <NxTable.Cell className="container-repository-results-table__policy-name">
        Multiple-Policy-Types ({row.violationCount})
      </NxTable.Cell>

      <NxTable.Cell className="container-repository-results-table__quarantine-time">
        {quarantineTime(row.quarantineTime)}
      </NxTable.Cell>

      <NxTable.Cell className="container-repository-results-table__object-name-cell">
        <NxOverflowTooltip>
          <span>{row.objectName}</span>
        </NxOverflowTooltip>
      </NxTable.Cell>

      <NxTable.Cell chevron />
    </NxTable.Row>
  ));

  const getSortDirection = (column) => {
    const prioritySortConfig = sortConfiguration?.[0];
    return R.equals(prioritySortConfig?.sortableField, column) ? (prioritySortConfig?.asc ? 'asc' : 'desc') : null;
  };

  const getColumnFilterValue = (column) => {
    const result = (columnFilters || []).find((filter) => filter.filterableField === column);
    return result ? result.value : '';
  };

  return (
    <NxTile id="container-repository-results-table">
      <NxTile.Content>
        <NxButtonBar>
          <NxButton
            id="container-repository-results-table__open-filter-drawer-button"
            onClick={() => setShowFilterDrawer(true)}
            variant="tertiary"
          >
            <NxFontAwesomeIcon icon={faFilter} />
            <span>Filter</span>
          </NxButton>
        </NxButtonBar>

        <NxTableContainer id="container-repository-results-table__table-container">
          <NxTable
            className="container-repository-results-table__table"
            data-testid="container-repository-results-table__table"
          >
            <NxTable.Head>
              <NxTable.Row>
                <NxTable.Cell
                  className="container-repository-results-table__threat"
                  isSortable
                  sortDir={getSortDirection('POLICY_THREAT_LEVEL')}
                  onClick={() => sortColumn('POLICY_THREAT_LEVEL')}
                >
                  THREAT
                </NxTable.Cell>

                <NxTable.Cell
                  className="container-repository-results-table__policy-name"
                  //
                  // isSortable
                  // sortDir={getSortDirection('POLICY_NAME')}
                  // onClick={() => sortColumn('POLICY_NAME')}
                >
                  POLICY
                </NxTable.Cell>

                <NxTable.Cell
                  className="container-repository-results-table__quarantine-time"
                  isSortable
                  sortDir={getSortDirection('QUARANTINE_TIME')}
                  onClick={() => sortColumn('QUARANTINE_TIME')}
                >
                  QUARANTINE TIME
                </NxTable.Cell>

                <NxTable.Cell
                  className="container-repository-results-table__object-name"
                  isSortable
                  sortDir={getSortDirection('OBJECT_NAME')}
                  onClick={() => sortColumn('OBJECT_NAME')}
                >
                  OBJECT
                </NxTable.Cell>
                <NxTable.Cell colSpan={2} />
              </NxTable.Row>

              <NxTable.Row isFilterHeader>
                <NxTable.Cell />
                <NxTable.Cell />
                <NxTable.Cell className="container-repository-results-table__cell__column-filter">
                  <NxFilterInput
                    placeholder="Filter"
                    name="QUARANTINE_TIME"
                    id="container-repository-results-table__column-filter__quarantine-time"
                    onChange={(value) => searchFilterColumn('QUARANTINE_TIME', value)}
                    value={getColumnFilterValue('QUARANTINE_TIME')}
                  />
                </NxTable.Cell>

                <NxTable.Cell className="container-repository-results-table__cell__column-filter" colSpan={3}>
                  <NxFilterInput
                    placeholder="Filter"
                    name="OBJECT_NAME"
                    id="container-repository-results-table__column-filter__object-name"
                    onChange={(value) => searchFilterColumn('OBJECT_NAME', value)}
                    value={getColumnFilterValue('OBJECT_NAME')}
                  />
                </NxTable.Cell>
              </NxTable.Row>
            </NxTable.Head>

            <NxTable.Body retryHandler={loadTable} isLoading={loading} error={errorMessage} emptyMessage="No results">
              {tableRows}
            </NxTable.Body>
          </NxTable>
        </NxTableContainer>

        {!loading && pagination?.page === 1 && !pagination?.hasNextPage ? null : (
          <NxIndeterminatePagination
            data-testid="container-repository-results-table__pagination"
            onPrevPageSelect={loadPreviousPage}
            onNextPageSelect={loadNextPage}
            isFirstPage={pagination?.page === 1}
            isLastPage={!pagination?.hasNextPage}
          />
        )}
      </NxTile.Content>
    </NxTile>
  );
};

export default ContainerRepositoryResultsTable;
