/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch } from 'react-redux';
import { actions as developerDashboardGraphsActions } from '../../slices/developerDashboardGraphsSlice';

import RiskAndRemediationGraph from './RiskRemediationGraph';
import MTTRGraph from './MTTRGraph';
import AdoptionGraph from 'MainRoot/development/developmentDashboard/sections/Graphs/AdoptionGraph';

export default function GraphsContainer() {
  const dispatch = useDispatch();
  const doLoad = () => {
    dispatch(developerDashboardGraphsActions.loadDeveloperDashboardGraphsData());
  };

  useEffect(() => {
    doLoad();
  }, []);

  return (
    <div className="iq-developer-dashboard-graph-container">
      <AdoptionGraph />
      <RiskAndRemediationGraph />
      <MTTRGraph />
    </div>
  );
}
