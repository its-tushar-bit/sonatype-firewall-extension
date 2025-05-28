/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { selectDashboardFilter, selectCurrentTab, selectWaiversResults } from '../../dashboardSelectors';
import { selectIsStandaloneFirewall } from 'MainRoot/reduxUiRouter/routerSelectors';
import { WAIVERS_RESULTS_TYPE, WAIVER_REQUESTS_RESULTS_TYPE } from 'MainRoot/dashboard/results/dashboardResultsTypes';
import {
  loadWaiverResults,
  setNextWaiversPage,
  setPreviousWaiversPage,
  sortWaiversResults,
} from '../dashboardResultsActions';
import DashboardWaiversTable from './DashboardWaiversTable';
import FirewallDashboardWaiversTable from './FirewallDashboardWaiversTable';
import DashboardWaiverRequestsTable from './DashboardWaiverRequestsTable';
import { NxTabs, NxTabList, NxTab, NxTabPanel, NxTile } from '@sonatype/react-shared-components';

export default function DashboardWaivers() {
  const loadWaivers = () => dispatch(loadWaiverResults());
  const waiverTabs = [WAIVERS_RESULTS_TYPE, WAIVER_REQUESTS_RESULTS_TYPE];
  const currentTab = useSelector(selectCurrentTab);
  const dispatch = useDispatch();
  const sortWaivers = (sortFields) => dispatch(sortWaiversResults(sortFields));
  const dispatchNexPage = () => dispatch(setNextWaiversPage());
  const dispatchPreviousPage = () => dispatch(setPreviousWaiversPage());

  const {
    needsAcknowledgement,
    appliedFilter: { maxDaysOld },
  } = useSelector(selectDashboardFilter);
  const isStandaloneFirewall = useSelector(selectIsStandaloneFirewall);
  const waivers = useSelector(selectWaiversResults);

  const handleTabClick = (index) => {
    dispatch(stateGo(`dashboard.overview.${waiverTabs[index]}`));
  };

  const modifiedWaivers = {
    ...waivers,
    results: waivers?.results
      ? waivers.results.map((waiver) => ({
          ...waiver,
          expiryTime: waiver.expiryTime === null && waiver.isAutoWaiver ? -1 : waiver.expiryTime,
        }))
      : [],
  };

  const tableProps = {
    waivers: modifiedWaivers,
    sortWaivers,
    dispatchNexPage,
    dispatchPreviousPage,
    stateGo,
    maxDaysOld,
    needsAcknowledgement,
    reload: loadWaivers,
  };

  useEffect(() => {
    if (isStandaloneFirewall) {
      loadWaivers();
    }
  }, [isStandaloneFirewall]);

  return (
    <NxTile id="dashboard-waivers" className="iq-dashboard-waivers">
      {isStandaloneFirewall ? (
        <FirewallDashboardWaiversTable {...tableProps} />
      ) : (
        <NxTabs activeTab={waiverTabs.indexOf(currentTab)} onTabSelect={handleTabClick}>
          <NxTabList>
            <NxTab>Existing Waivers</NxTab>
            <NxTab>Requested Waivers</NxTab>
          </NxTabList>
          <NxTabPanel>
            <DashboardWaiversTable />
          </NxTabPanel>
          <NxTabPanel>
            <DashboardWaiverRequestsTable />
          </NxTabPanel>
        </NxTabs>
      )}
    </NxTile>
  );
}
