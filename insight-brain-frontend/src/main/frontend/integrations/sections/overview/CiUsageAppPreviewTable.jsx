/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxButton, NxTable } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { actions, PREVIEW_PAGE_SIZE } from '../../slices/appsWithoutRecentCiUsagePreviewSlice';
import { useEffect } from 'react';
import { selectappsWithoutRecentCiUsagePreviewSlice } from 'MainRoot/integrations/integrationsSelectors';
import { SECTIONS } from 'MainRoot/integrations/integrations.module';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { isEmpty } from 'ramda';
import IntegrationsAppRiskTooltip from '../../IntegrationsAppRiskTooltip';

export function CiUsageAppPreviewTable() {
  const dispatch = useDispatch();

  const {
    loading,
    loadError,
    applicationsWithoutRecentCiUsage,
    reload,
    totalNumberOfApplicationsWithoutRecentCiUsage,
  } = useGetAppsWithoutRecentCiUsagePreview();

  const ViewAllAppsButton = () => {
    if (
      isEmpty(applicationsWithoutRecentCiUsage) ||
      totalNumberOfApplicationsWithoutRecentCiUsage <= PREVIEW_PAGE_SIZE
    ) {
      return null;
    }
    return (
      <NxButton className="iq-integrations-ci-usage-preview-view-all" onClick={viewAllAppsClicked}>
        View all apps
      </NxButton>
    );
  };

  return (
    <>
      <NxTable data-testid="iq-integrations-apps-without-recent-usage-preview">
        <NxTable.Head>
          <NxTable.Row>
            <NxTable.Cell>Apps</NxTable.Cell>
            <NxTable.Cell isNumeric>
              <IntegrationsAppRiskTooltip />
            </NxTable.Cell>
          </NxTable.Row>
        </NxTable.Head>

        <NxTable.Body
          isLoading={loading}
          error={loadError}
          retryHandler={reload}
          emptyMessage="All of your apps are integrated with CI"
        >
          {applicationsWithoutRecentCiUsage.map(({ applicationName, totalRisk }) => (
            <NxTable.Row key={applicationName}>
              <NxTable.Cell>{applicationName}</NxTable.Cell>
              <NxTable.Cell isNumeric>{totalRisk}</NxTable.Cell>
            </NxTable.Row>
          ))}
        </NxTable.Body>
      </NxTable>

      <ViewAllAppsButton />
    </>
  );

  function viewAllAppsClicked() {
    dispatch(stateGo(`integrations.${SECTIONS.APPS_WITHOUT_CI_INTEGRATIONS}`));
  }
}

function useGetAppsWithoutRecentCiUsagePreview() {
  const dispatch = useDispatch();
  const doLoad = () => dispatch(actions.loadAppsWithoutRecentCiUsagePreview());

  useEffect(() => {
    doLoad();
  }, []);

  return { ...useSelector(selectappsWithoutRecentCiUsagePreviewSlice), reload: doLoad };
}
