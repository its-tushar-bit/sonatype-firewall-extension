/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { prop } from 'ramda';

import { NxList, NxTile, NxH2, NxLoadWrapper } from '@sonatype/react-shared-components';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { actions as policyMonitoringActions } from 'MainRoot/OrgsAndPolicies/policyMonitoringSlice';

import {
  selectMonitoredStageFromActionStages,
  selectPolicyMonitoringLoading,
  selectPolicyMonitoringLoadError,
  selectPolicyMonitoringLinkParams,
  selectMonitoredStageFromSbomStages,
} from 'MainRoot/OrgsAndPolicies/policyMonitoringSelectors';
import { selectActionStagesIsLoading, selectActionStagesLoadError } from 'MainRoot/OrgsAndPolicies/stagesSelectors';
import { selectIsMonitoringSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';

export default function ContinuousMonitoringSummaryTile() {
  const uiStateRouter = useRouterState();
  const { to, params } = useSelector(selectPolicyMonitoringLinkParams);
  const href = uiStateRouter.href(to, params);

  const dispatch = useDispatch();
  const isMonitoringSupported = useSelector(selectIsMonitoringSupported);
  const isSbomManager = useSelector(selectIsSbomManager);
  const monitoredStage = useSelector(
    isSbomManager ? selectMonitoredStageFromSbomStages : selectMonitoredStageFromActionStages
  );
  const isMonitoringLoading = useSelector(selectPolicyMonitoringLoading);
  const isStagesLoading = useSelector(selectActionStagesIsLoading);
  const isLoading = isMonitoringLoading || isStagesLoading;
  const monitoringLoadError = useSelector(selectPolicyMonitoringLoadError);
  const stagesLoadError = useSelector(selectActionStagesLoadError);
  const loadError = monitoringLoadError || stagesLoadError;

  const doLoad = () => {
    dispatch(policyMonitoringActions.loadContinuousMonitoringSummaryTileInformation());
  };

  useEffect(() => {
    doLoad();
  }, []);

  const renderContent = () => {
    let stageName;
    if (!isSbomManager) {
      stageName = prop('stageName', monitoredStage);
    } else {
      stageName = `Notifications and Alerts are ${monitoredStage ? 'enabled' : 'disabled'} for Compliance stage`;
    }
    return <NxList.LinkItem href={href}>{stageName}</NxList.LinkItem>;
  };

  return (
    isMonitoringSupported && (
      <NxTile id="owner-pill-continuous-monitoring">
        <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={doLoad}>
          <NxTile.Header>
            <NxTile.HeaderTitle>
              <NxH2>Continuous monitoring</NxH2>
            </NxTile.HeaderTitle>
          </NxTile.Header>
          <NxTile.Content>
            <NxList id="continuous-monitoring">{renderContent()}</NxList>
          </NxTile.Content>
        </NxLoadWrapper>
      </NxTile>
    )
  );
}
