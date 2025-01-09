/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { selectDashboardFilter, selectWaiversResults } from '../../dashboardSelectors';
import { selectIsStandaloneFirewall } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  loadWaiverResults,
  sortWaiversResults,
  setNextWaiversPage,
  setPreviousWaiversPage,
} from '../dashboardResultsActions';
import { stateGo as stateGoAction } from 'MainRoot/reduxUiRouter/routerActions';
import DashboardWaiversTable from './DashboardWaiversTable';
import DashboardMask from '../dashboardMask/DashboardMask';
import { NxInfoAlert, NxTextLink } from '@sonatype/react-shared-components';
import { prop } from 'ramda';

export default function DashboardWaivers() {
  const dispatch = useDispatch();
  const loadWaivers = () => dispatch(loadWaiverResults());
  const stateGo = (...params) => dispatch(stateGoAction(...params));
  const sortWaivers = (sortFields) => dispatch(sortWaiversResults(sortFields));
  const dispatchNexPage = () => dispatch(setNextWaiversPage());
  const dispatchPreviousPage = () => dispatch(setPreviousWaiversPage());

  const {
    loading: filterLoading,
    needsAcknowledgement,
    filtersAreDirty,
    appliedFilter: { maxDaysOld },
  } = useSelector(selectDashboardFilter);
  const isStandaloneFirewall = useSelector(selectIsStandaloneFirewall);
  const waivers = useSelector(selectWaiversResults);
  const waiverSelector = useSelector(prop('waivers'));

  const isLoading = (!waivers.results && !waivers.error) || waiverSelector.waiverReasons.loading;

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
    if (isStandaloneFirewall || (!filterLoading && !needsAcknowledgement)) {
      loadWaivers();
    }
  }, [filterLoading, needsAcknowledgement]);

  return (
    <>
      <NxInfoAlert>
        This list shows all existing waivers applied at the same or higher hierarchy level, based on your filter
        selections.{' '}
        <NxTextLink external href="https://links.sonatype.com/products/nxiq/doc/dashboard-waivers">
          Learn more about waivers.
        </NxTextLink>
      </NxInfoAlert>
      <div id="dashboard-waivers" className="iq-dashboard-waivers">
        {filtersAreDirty && !needsAcknowledgement && !isLoading && <DashboardMask />}
        <DashboardWaiversTable {...tableProps} />
      </div>
    </>
  );
}
