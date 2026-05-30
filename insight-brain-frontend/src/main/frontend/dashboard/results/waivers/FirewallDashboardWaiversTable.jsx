/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useMemo } from 'react';
import * as PropTypes from 'prop-types';
import { NxTable, NxIndeterminatePagination, NxTableContainer, NxFilterInput } from '@sonatype/react-shared-components';
import { equals } from 'ramda';
import { useSelector, useDispatch } from 'react-redux';
import { debounce } from 'debounce';
import { Messages } from 'MainRoot/util/CommonServices';
import DashboardWaiversTableRow, { waiverPropTypes } from './DashboardWaiversTableRow';
import NeedsAcknowledgementInfoRow from '../NeedsAcknowledgementInfoRow';
import FirewallWaiverExpirationFilter from 'MainRoot/firewall/waivers/FirewallWaiverExpirationFilter';
import {
  selectExpirationDate,
  firewallApplyFilter,
  setComponentNameFilter,
  setRepositoryFilter,
} from 'MainRoot/dashboard/filter/dashboardFilterActions';
import { setPage } from 'MainRoot/dashboard/results/dashboardResultsActions';
import { WAIVERS_RESULTS_TYPE } from 'MainRoot/dashboard/results/dashboardResultsTypes';
import { selectDashboardFilter } from 'MainRoot/dashboard/dashboardSelectors';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { extractSortFieldName } from 'MainRoot/util/sortUtils';

const DEFAULT_SORT_FIELDS = [['-threatLevel'], ['createTime'], ['expiryTime'], ['policyName'], ['scope'], ['component']];

export default function FirewallDashboardWaiversTable(props) {
  const {
    waivers: { results, hasNextPage, sortFields, error, hasMultiplePages, page },
    sortWaivers,
    dispatchNexPage,
    dispatchPreviousPage,
    stateGo,
    maxDaysOld,
    needsAcknowledgement,
    reload,
  } = props;
  const dispatch = useDispatch();
  const dashboardFilter = useSelector(selectDashboardFilter);
  const selectedExpirationDate = dashboardFilter?.selected?.expirationDate ?? 'ALL';

  const onExpirationChange = (value) => {
    dispatch(selectExpirationDate(value));
    dispatch(firewallApplyFilter());
    dispatch(setPage(WAIVERS_RESULTS_TYPE, 0));
  };

  const componentName = dashboardFilter?.selected?.componentName ?? '';
  const repositoryPublicId = dashboardFilter?.selected?.repositoryPublicId ?? '';

  const debouncedLoadWaiverResults = useMemo(
    () => debounce(() => {
      dispatch(firewallApplyFilter());
      dispatch(setPage(WAIVERS_RESULTS_TYPE, 0));
    }, 500),
    [dispatch]
  );

  useEffect(() => () => debouncedLoadWaiverResults.clear(), [debouncedLoadWaiverResults]);

  const onComponentNameChange = (value) => {
    dispatch(setComponentNameFilter(value));
    debouncedLoadWaiverResults();
  };

  const onRepositoryChange = (value) => {
    dispatch(setRepositoryFilter(value));
    debouncedLoadWaiverResults();
  };

  const effectiveHasNextPage = hasNextPage;
  const currentPage = hasMultiplePages ? page : null;
  const isLoading = !error && !results && !needsAcknowledgement,
    sortedColumn = extractSortFieldName(sortFields[0]),
    isSortReversed = sortFields[0].includes('-'),
    emptyMessage = 'No data available for the applied filters and permissions.';
  const colSpan = 8;

  const getColumnDirection = (index, sortInverted = false) => {
    if (!results || !results.length || error) {
      return null;
    }

    const columnFields = DEFAULT_SORT_FIELDS[index],
      currentColumn = extractSortFieldName(columnFields[0]),
      isCurrentColumnSorted = sortedColumn === currentColumn,
      isUp = isCurrentColumnSorted && (sortInverted ? isSortReversed : !isSortReversed),
      isDown = isCurrentColumnSorted && (!sortInverted ? isSortReversed : !isSortReversed);

    return isUp ? 'asc' : isDown ? 'desc' : null;
  };

  const doSort = (columnIndex) => {
    const columnSortFields = DEFAULT_SORT_FIELDS[columnIndex];
    const column = extractSortFieldName(columnSortFields[0]);

    if (equals(columnSortFields, sortFields)) {
      if (sortFields[0] !== column) {
        sortWaivers([column]);
      } else {
        sortWaivers([`-${column}`]);
      }
    } else {
      sortWaivers(columnSortFields);
    }
  };

  const displayedResults = results;

  const bodyFragment = () => {
    if (!isNilOrEmpty(displayedResults)) {
      return (
        <>
          {displayedResults.map((waiver) => (
            <DashboardWaiversTableRow {...{ stateGo, waiver, page }} key={waiver.id} />
          ))}
        </>
      );
    }
    return null;
  };
  return (
    <div className="nx-table-container">
<div className="iq-waivers-table-scroll">
      <NxTable className="nx-table--fixed-layout">
        <NxTable.Head>
          <NxTable.Row className="iq-dashboard-waivers-headers">
            <NxTable.Cell
              className="iq-size-controlled-cell"
              onClick={() => doSort(0)}
              sortDir={getColumnDirection(0)}
              isSortable
            >
              Threat
            </NxTable.Cell>
            <NxTable.Cell
              className="iq-waiver-date-header"
              onClick={() => doSort(1)}
              sortDir={getColumnDirection(1)}
              isSortable
            >
              Date Created
            </NxTable.Cell>
            <NxTable.Cell
              className="iq-waiver-date-header"
              onClick={() => doSort(2)}
              sortDir={getColumnDirection(2)}
              isSortable
            >
              Expiration
            </NxTable.Cell>
            <NxTable.Cell className="iq-waiver-policy-header" onClick={() => doSort(3)} sortDir={getColumnDirection(3)} isSortable>
              Policy
            </NxTable.Cell>
            <NxTable.Cell className="iq-waiver-scope-header" onClick={() => doSort(4)} sortDir={getColumnDirection(4)} isSortable>
              Scope
            </NxTable.Cell>
            <NxTable.Cell className="iq-waiver-component-header" onClick={() => doSort(5)} sortDir={getColumnDirection(5)} isSortable>
              Components
            </NxTable.Cell>
            <NxTable.Cell className="iq-upgrade-header">Upgrade</NxTable.Cell>
            <NxTable.Cell className="iq-waiver-actions-header">Actions</NxTable.Cell>
          </NxTable.Row>
          <NxTable.Row isFilterHeader>
              <NxTable.Cell />
              <NxTable.Cell />
              <NxTable.Cell className="iq-waiver-expiration-filter-cell">
                <FirewallWaiverExpirationFilter
                  selectedId={selectedExpirationDate}
                  onChange={onExpirationChange}
                />
              </NxTable.Cell>
              <NxTable.Cell />
              <NxTable.Cell className="iq-waiver-scope-filter-cell">
                <NxFilterInput
                  placeholder="repository"
                  value={repositoryPublicId}
                  onChange={onRepositoryChange}
                />
              </NxTable.Cell>
              <NxTable.Cell className="iq-waiver-component-filter-cell">
                <NxFilterInput
                  placeholder="component name"
                  value={componentName}
                  onChange={onComponentNameChange}
                />
              </NxTable.Cell>
              <NxTable.Cell />
              <NxTable.Cell />
            </NxTable.Row>
        </NxTable.Head>
        <NxTable.Body
          className="iq-dashboard-waivers-entries"
          isLoading={isLoading}
          emptyMessage={emptyMessage}
          error={Messages.getHttpErrorMessage(error)}
          retryHandler={reload}
        >
          {needsAcknowledgement ? <NeedsAcknowledgementInfoRow colSpan={colSpan} /> : bodyFragment()}
        </NxTable.Body>
      </NxTable>
      </div>

      {!isLoading &&
        (currentPage === null || (currentPage === 0 && !effectiveHasNextPage) ? null : (
          <NxTableContainer.Footer>
            <NxIndeterminatePagination
              onPrevPageSelect={dispatchPreviousPage}
              onNextPageSelect={dispatchNexPage}
              isFirstPage={currentPage === 0}
              isLastPage={!effectiveHasNextPage}
            />
          </NxTableContainer.Footer>
        ))}
    </div>
  );
}

FirewallDashboardWaiversTable.propTypes = {
  reload: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  sortWaivers: PropTypes.func.isRequired,
  dispatchNexPage: PropTypes.func.isRequired,
  dispatchPreviousPage: PropTypes.func.isRequired,
  maxDaysOld: PropTypes.number,
  needsAcknowledgement: PropTypes.bool.isRequired,
  waivers: PropTypes.shape({
    results: PropTypes.arrayOf(waiverPropTypes),
    hasNextPage: PropTypes.bool,
    sortFields: PropTypes.arrayOf(PropTypes.string),
    error: PropTypes.oneOfType([PropTypes.string, PropTypes.instanceOf(Error), PropTypes.object]),
    hasMultiplePages: PropTypes.bool,
    page: PropTypes.number,
  }),
};
