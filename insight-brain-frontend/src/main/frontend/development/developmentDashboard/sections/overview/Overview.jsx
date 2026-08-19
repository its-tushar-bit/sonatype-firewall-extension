/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import CiCard from 'MainRoot/development/developmentDashboard/sections/overview/CiCard';
import IdeIntegrationsCard from './ideIntegrationsCard/IdeIntegrationsCard';
import AppIntegrationsAndRiskTable from '../AppIntegrationsAndRiskTable/AppIntegrationsAndRiskTable';
import { useDispatch, useSelector } from 'react-redux';
import { selectAppIntegrationsAndRiskSlice } from 'MainRoot/development/developmentDashboard/selectors/appIntegrationsAndRiskSelectors';
import { NxCard, NxH2, NxTile, NxButton, NxFontAwesomeIcon, NxInfoAlert } from '@sonatype/react-shared-components';
import GraphsContainer from '../Graphs/GraphsContainer';
import AutomatedSourceControlFeedbackCard from './AutomatedSourceControlFeedbackCard';
import { faFilter } from '@fortawesome/pro-solid-svg-icons';
import RiskTableFilter from '../AppIntegrationsAndRiskTable/filter/RiskTableFilter';
import { actions as filterActions } from 'MainRoot/development/developmentDashboard/slices/appIntegrationsAndRiskSlice';
import { selectIsDeveloperSummaryTableEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';

export const DEVELOPER_SUMMARY_TABLE_DISABLED_MESSAGE =
  'Applications Configuration Build Stage Summary is not enabled.';

export default function Overview() {
  const dispatch = useDispatch();

  const { showFilterSideBar } = useSelector(selectAppIntegrationsAndRiskSlice);
  const isDeveloperSummaryTableEnabled = useSelector(selectIsDeveloperSummaryTableEnabled);

  return (
    <div id="iq-integrations-overview-section">
      {showFilterSideBar && <RiskTableFilter />}
      <NxTile>
        <NxTile.Content>
          <GraphsContainer />
        </NxTile.Content>
      </NxTile>

      {isDeveloperSummaryTableEnabled ? (
        <>
          <div className="nx-page-title nx-page-title__actions">
            <NxH2>Build Stage Risk Monitoring Summary</NxH2>
            <div className="nx-btn-bar">
              <NxButton id="filter-toggle" variant="tertiary" className="btn" onClick={toggleFilterSideBar}>
                <NxFontAwesomeIcon icon={faFilter} />
                <span>Filter</span>
              </NxButton>
            </div>
          </div>
          <AppIntegrationsAndRiskTable />
        </>
      ) : (
        <NxInfoAlert>{DEVELOPER_SUMMARY_TABLE_DISABLED_MESSAGE}</NxInfoAlert>
      )}

      <NxCard.Container>
        <CiCard />
        <AutomatedSourceControlFeedbackCard />
        <IdeIntegrationsCard />
      </NxCard.Container>
    </div>
  );

  function toggleFilterSideBar() {
    dispatch(filterActions.toggleFilterSideBar(true));
  }
}
