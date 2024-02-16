/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import CiCard from 'MainRoot/integrations/sections/overview/CiCard';
import IdeIntegrationsCard from './ideIntegrationsCard/IdeIntegrationsCard';
import AppIntegrationsAndRiskTable from '../AppIntegrationsAndRiskTable/AppIntegrationsAndRiskTable';
import { actions as chartActions } from 'MainRoot/integrations/slices/chartVisibilitySlice';
import { useDispatch, useSelector } from 'react-redux';
import { selectUsageOverTimeChartVisibilitySlice } from 'MainRoot/integrations/selectors/chartVisibilitySelectors';
import { selectAppIntegrationsAndRiskSlice } from 'MainRoot/integrations/selectors/appIntegrationsAndRiskSelectors';
import { NxCard, NxH2, NxTile, NxLoadWrapper, NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import GraphsContainer from '../Graphs/GraphsContainer';
import AutomatedSourceControlFeedbackCard from './AutomatedSourceControlFeedbackCard';
import { faFilter } from '@fortawesome/pro-solid-svg-icons';
import RiskTableFilter from '../AppIntegrationsAndRiskTable/filter/RiskTableFilter';
import { actions as filterActions } from 'MainRoot/integrations/slices/appIntegrationsAndRiskSlice';

export default function Overview() {
  const dispatch = useDispatch();

  const { loading, loadError, usageOverTimeChartsShown, uninitialized } = useSelector(
    selectUsageOverTimeChartVisibilitySlice
  );

  const { showFilterSideBar } = useSelector(selectAppIntegrationsAndRiskSlice);

  useEffect(() => {
    doLoad();
  }, []);

  return (
    <div id="iq-integrations-overview-section">
      <NxLoadWrapper loading={loading || uninitialized} error={loadError} retryHandler={doLoad}>
        {showFilterSideBar && <RiskTableFilter />}
        {usageOverTimeChartsShown ? (
          <NxTile>
            <NxTile.Content>
              <GraphsContainer />
            </NxTile.Content>
          </NxTile>
        ) : null}
        <div className="nx-page-title nx-page-title__actions">
          <NxH2>Applications Configuration Summary</NxH2>
          <div className="nx-btn-bar">
            <NxButton id="filter-toggle" variant="tertiary" className="btn" onClick={toggleFilterSideBar}>
              <NxFontAwesomeIcon icon={faFilter} />
              <span>Filter</span>
            </NxButton>
          </div>
        </div>

        <AppIntegrationsAndRiskTable />

        <NxCard.Container>
          <CiCard />
          <AutomatedSourceControlFeedbackCard />
          <IdeIntegrationsCard />
        </NxCard.Container>
      </NxLoadWrapper>
    </div>
  );

  function doLoad() {
    dispatch(chartActions.loadChartVisiblity());
  }

  function toggleFilterSideBar() {
    dispatch(filterActions.toggleFilterSideBar(true));
  }
}
