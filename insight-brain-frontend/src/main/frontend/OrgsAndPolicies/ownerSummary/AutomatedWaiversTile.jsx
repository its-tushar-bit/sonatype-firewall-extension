/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { NxH2, NxTile, NxList, NxLoadWrapper, NxSmallTag } from '@sonatype/react-shared-components';
import { selectWaiversStatusMessage, selectWaiversSlice } from './../automatedWaiversSelectors';
import { actions } from '../automatedWaiversSlice';
import {
  selectIsAutoWaiversEnabled,
  selectIsDeveloperDashboardEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { deriveEditRoute } from 'MainRoot/OrgsAndPolicies/utility/util';
import { selectIsSbomManager, selectRouterSlice } from 'MainRoot/reduxUiRouter/routerSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

export default function AutomatedWaiversTile() {
  const dispatch = useDispatch();
  const waiversConfigurationStatusMessage = useSelector(selectWaiversStatusMessage);
  const { loading, loadError } = useSelector(selectWaiversSlice);
  const isDeveloperEnabled = useSelector(selectIsDeveloperDashboardEnabled);
  const isAutoWaiversEnabled = useSelector(selectIsAutoWaiversEnabled);
  const uiStateRouter = useRouterState();
  const router = useSelector(selectRouterSlice());
  const { to, params } = deriveEditRoute(router, 'edit-waivers');
  const href = uiStateRouter.href(to, params);
  const isSbomManager = useSelector(selectIsSbomManager);

  const doLoad = () => dispatch(actions.loadAutoWaiversConfiguration());
  useEffect(() => {
    doLoad();
  }, []);

  if (!isDeveloperEnabled || !isAutoWaiversEnabled || isSbomManager) {
    return null;
  }

  const waiversConfigurationEnabled = waiversConfigurationStatusMessage.includes('enabled');

  return (
    <NxTile id="owner-pill-waivers-configuration">
      <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>Waivers</NxH2>
          </NxTile.HeaderTitle>
        </NxTile.Header>
        <NxTile.Content>
          <NxList id="waivers-configuration">
            <NxList.LinkItem href={href}>
              {waiversConfigurationStatusMessage}
              {waiversConfigurationEnabled && <NxSmallTag color="green">Auto</NxSmallTag>}
            </NxList.LinkItem>
          </NxList>
        </NxTile.Content>
      </NxLoadWrapper>
    </NxTile>
  );
}
