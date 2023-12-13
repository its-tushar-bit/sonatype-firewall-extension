/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import CiCard from 'MainRoot/integrations/sections/overview/CiCard';
import IdeIntegrationsCard from './ideIntegrationsCard/IdeIntegrationsCard';
import AppIntegrationsAndRiskTable from '../AppIntegrationsAndRiskTable/AppIntegrationsAndRiskTable';
import { actions } from 'MainRoot/integrations/slices/chartVisibilitySlice';
import { useDispatch, useSelector } from 'react-redux';
import { selectUsageOverTimeChartVisibilitySlice } from 'MainRoot/integrations/selectors/chartVisibilitySelectors';
import { NxCard, NxH2, NxTile, NxLoadWrapper } from '@sonatype/react-shared-components';
import GraphsContainer from '../Graphs/GraphsContainer';

export default function Overview() {
  const dispatch = useDispatch();

  const { loading, loadError, usageOverTimeChartsShown, uninitialized } = useSelector(
    selectUsageOverTimeChartVisibilitySlice
  );

  useEffect(() => {
    doLoad();
  }, []);

  return (
    <div id="iq-integrations-overview-section">
      <NxLoadWrapper loading={loading || uninitialized} error={loadError} retryHandler={doLoad}>
        {usageOverTimeChartsShown ? (
          <NxTile>
            <NxTile.Content>
              <GraphsContainer />
            </NxTile.Content>
          </NxTile>
        ) : null}

        <NxH2>Applications Configuration Summary</NxH2>
        <AppIntegrationsAndRiskTable />

        <NxCard.Container>
          <CiCard />
          <IdeIntegrationsCard />
        </NxCard.Container>
      </NxLoadWrapper>
    </div>
  );

  function doLoad() {
    dispatch(actions.loadChartVisiblity());
  }
}
