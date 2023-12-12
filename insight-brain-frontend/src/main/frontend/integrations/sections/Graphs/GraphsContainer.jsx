/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch } from 'react-redux';
import { actions as adoptionGraphActions } from '../../slices/adoptionGraphSlice';
import { actions as riskRemediationAndMttrGraphActions } from '../../slices/riskRemediationAndMttrGraphSlice';
import AdoptionGraph from './AdoptionGraph';
import RiskAndRemediationGraph from './RiskRemediationGraph';
import MTTRGraph from './MTTRGraph';

export default function GraphsContainer() {
  const dispatch = useDispatch();
  const doLoad = () => {
    dispatch(adoptionGraphActions.loadAdoptionGraphData());
    dispatch(riskRemediationAndMttrGraphActions.loadRiskRemediationAndMttrGraphData());
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
