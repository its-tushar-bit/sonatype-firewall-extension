/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback, useEffect } from 'react';
import { NxFilterInput, NxPagination, NxTable, NX_STANDARD_DEBOUNCE_TIME } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { actions, COLUMNS } from './appsWithoutCiIntegrationsSlice';
import { appsWithoutCiIntegrationsSelector } from 'MainRoot/integrations/sections/AppsWithoutCiIntegrations/appsWithoutCiIntegrationsSelectors';
import { debounce } from 'debounce';

export default function AppsWithoutCiIntegrationsTable() {
  const appsWithoutCiIntegrationsSlice = useSelector(appsWithoutCiIntegrationsSelector);
  const { dashboardResults, loading, loadError, currentPage, pageCount, sort, filter } = appsWithoutCiIntegrationsSlice;

  const dispatch = useDispatch();

  const handleChange = (page) => {
    dispatch(actions.setCurrentPage({ currentPage: page }));
    dispatch(actions.loadAppsWithoutCiIntegrations());
  };

  useEffect(() => {
    dispatch(actions.loadAppsWithoutCiIntegrations());
  }, []);

  const handleSort = (name) => {
    dispatch(actions.setSort(name));
    dispatch(actions.loadAppsWithoutCiIntegrations());
  };

  const getSortDir = (name) => {
    if (!sort.includes(name)) return null;
    return sort.includes('-') ? 'desc' : 'asc';
  };

  const debouncedFilterNameChange = useCallback(
    debounce((value) => {
      dispatch(actions.loadAppsWithoutCiIntegrations(value));
    }, NX_STANDARD_DEBOUNCE_TIME),
    []
  );

  const onFilterNameChange = (filter) => {
    dispatch(actions.setFilter(filter));
    debouncedFilterNameChange(filter);
  };

  return (
    <div id="iq-integrations-apps-without-ci-integrations-section-table">
      <NxTable>
        <NxTable.Head>
          <NxTable.Row>
            <NxTable.Cell isSortable onClick={() => handleSort(COLUMNS.NAME)} sortDir={getSortDir(COLUMNS.NAME)}>
              APPLICATIONS
            </NxTable.Cell>
            <NxTable.Cell
              isSortable
              onClick={() => handleSort(COLUMNS.TOTAL_RISK)}
              sortDir={getSortDir(COLUMNS.TOTAL_RISK)}
            >
              TOTAL RISK
            </NxTable.Cell>
          </NxTable.Row>
          <NxTable.Row isFilterHeader>
            <NxTable.Cell>
              <NxFilterInput placeholder="Type a name" onChange={onFilterNameChange} value={filter} />
            </NxTable.Cell>
            <NxTable.Cell />
          </NxTable.Row>
        </NxTable.Head>
        <NxTable.Body emptyMessage="All of your apps are integrated with CI." isLoading={loading} error={loadError}>
          {dashboardResults.map(({ applicationName, totalRisk }) => {
            return (
              <NxTable.Row key={applicationName.concat(totalRisk)}>
                <NxTable.Cell className="iq-integrations-applications-table__name-cell">{applicationName}</NxTable.Cell>
                <NxTable.Cell>{totalRisk}</NxTable.Cell>
              </NxTable.Row>
            );
          })}
        </NxTable.Body>
      </NxTable>
      <div className="nx-table-container__footer">
        <NxPagination pageCount={pageCount} currentPage={getCurrentPage()} onChange={handleChange} />
      </div>
    </div>
  );

  function getCurrentPage() {
    if (pageCount === 0) {
      // NxPagination does not allow currentPage to numeric if pageCount is 0
      return null;
    } else {
      return currentPage;
    }
  }
}
