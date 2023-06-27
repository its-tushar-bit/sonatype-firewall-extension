/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { NxPagination, NxTable } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from './appsWithoutCiIntegrationsSlice';
import { appsWithoutCiIntegrationsSelector } from 'MainRoot/integrations/sections/AppsWithoutCiIntegrations/appsWithoutCiIntegrationsSelectors';

export default function AppsWithoutCiIntegrationsTable() {
  const appsWithoutCiIntegrationsSlice = useSelector(appsWithoutCiIntegrationsSelector);
  const { dashboardResults, loading, loadError, currentPage, pageCount } = appsWithoutCiIntegrationsSlice;

  const dispatch = useDispatch();

  const handleChange = (page) => {
    dispatch(actions.setCurrentPage({ currentPage: page }));
    dispatch(actions.loadAppsWithoutCiIntegrations());
  };

  useEffect(() => {
    dispatch(actions.loadAppsWithoutCiIntegrations());
  }, []);

  return (
    <div id="iq-integrations-apps-without-ci-integrations-section-table">
      <NxTable>
        <NxTable.Head>
          <NxTable.Row>
            <NxTable.Cell>APPLICATIONS</NxTable.Cell>
            <NxTable.Cell>TOTAL RISK</NxTable.Cell>
          </NxTable.Row>
        </NxTable.Head>
        <NxTable.Body emptyMessage="No data found." isLoading={loading} error={loadError}>
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
