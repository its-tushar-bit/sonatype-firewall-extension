/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { selectDashboardFilter, selectWaiversResults } from '../../dashboardSelectors';
import { loadWaiverResults, sortWaiversResults } from '../dashboardResultsActions';
import { stateGo as stateGoAction } from 'MainRoot/reduxUiRouter/routerActions';
import DashboardWaiversTable from './DashboardWaiversTable';
import DashboardMask from '../dashboardMask/DashboardMask';
import { NxInfoAlert, NxTextLink } from '@sonatype/react-shared-components';

export default function DashboardWaivers() {
  const dispatch = useDispatch();
  const loadWaivers = () => dispatch(loadWaiverResults());
  const stateGo = (...params) => dispatch(stateGoAction(...params));
  const sortWaivers = (sortFields, doBackendSort) => dispatch(sortWaiversResults(sortFields, doBackendSort));

  const {
    loading: filterLoading,
    needsAcknowledgement,
    filtersAreDirty,
    appliedFilter: { maxDaysOld },
  } = useSelector(selectDashboardFilter);
  const waivers = useSelector(selectWaiversResults);

  const isLoading = !waivers.results && !waivers.error;

  const tableProps = {
    waivers,
    sortWaivers,
    stateGo,
    maxDaysOld,
    needsAcknowledgement,
    reload: loadWaivers,
  };

  useEffect(() => {
    if (!filterLoading && !needsAcknowledgement) {
      loadWaivers();
    }
  }, [filterLoading, needsAcknowledgement]);

  return (
    <div id="dashboard-waivers" className="iq-dashboard-waivers nx-viewport-sized__container">
      {filtersAreDirty && !needsAcknowledgement && !isLoading && <DashboardMask />}
      <NxInfoAlert>
        This list shows all existing waivers applied at the same or higher hierarchy level, based on your filter
        selections.{' '}
        <NxTextLink external href="https://links.sonatype.com/products/nxiq/doc/dashboard-waivers">
          Learn more about waivers.
        </NxTextLink>
      </NxInfoAlert>
      <DashboardWaiversTable {...tableProps} />
    </div>
  );
}
