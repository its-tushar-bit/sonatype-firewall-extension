/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { NxH2, NxTile, NxList, NxLoadWrapper } from '@sonatype/react-shared-components';
import { selectApplicableAutoWaivers } from './../autoWaiversSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/applicableAutoWaiversSlice';
import {
  selectIsAutoWaiversEnabled,
  selectIsDeveloperDashboardEnabled,
  selectHasAutoWaiverManagement,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { EnterpriseFullWidthBanner } from 'MainRoot/shared/enterpriseTier';
import { deriveEditRoute } from 'MainRoot/OrgsAndPolicies/utility/util';
import { selectIsSbomManager, selectRouterSlice } from 'MainRoot/reduxUiRouter/routerSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectIsRootOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';

export default function AutoWaiversTile() {
  const dispatch = useDispatch();
  const isDeveloperEnabled = useSelector(selectIsDeveloperDashboardEnabled);
  const isAutoWaiversEnabled = useSelector(selectIsAutoWaiversEnabled);
  const hasAutoWaiverManagement = useSelector(selectHasAutoWaiverManagement);
  const uiStateRouter = useRouterState();
  const router = useSelector(selectRouterSlice);
  const { to, params } = deriveEditRoute(router, 'auto-waivers-config');
  const href = uiStateRouter.href(to, params);
  const isSbomManager = useSelector(selectIsSbomManager);
  const isRootOrg = useSelector(selectIsRootOrganization);

  const applicableAutoWaivers = useSelector(selectApplicableAutoWaivers);
  const { loading, loadError, data } = applicableAutoWaivers || {};

  const getMessage = () => {
    const { localWaiversCount, inheritedWaiversCount } = data?.reduce(
      (counts, waiver) => {
        if (waiver.isAutoWaiverEnabled) {
          waiver.isInherited ? counts.inheritedWaiversCount++ : counts.localWaiversCount++;
        }
        return counts;
      },
      { localWaiversCount: 0, inheritedWaiversCount: 0 }
    );

    return isRootOrg ? `${localWaiversCount} local` : `${localWaiversCount} local, ${inheritedWaiversCount} inherited`;
  };

  const doLoad = () => dispatch(actions.loadApplicableAutoWaivers());

  useEffect(() => {
    doLoad();
  }, []);

  if (!isDeveloperEnabled || !isAutoWaiversEnabled || isSbomManager) {
    return null;
  }

  if (!hasAutoWaiverManagement) {
    return (
      <NxTile
        id="owner-pill-auto-waivers-configuration"
        data-testid="iq-auto-waivers-tile"
        className="iq-banner-flush-top"
      >
        <EnterpriseFullWidthBanner
          title="Auto-Waivers"
          description="Automatically apply waivers to low-risk, non-reachable or known issues so teams can stay unblocked."
        />
      </NxTile>
    );
  }

  return (
    <NxTile id="owner-pill-auto-waivers-configuration" data-testid="iq-auto-waivers-tile">
      <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>Auto-Waivers</NxH2>
          </NxTile.HeaderTitle>
        </NxTile.Header>
        <NxTile.Content>
          <NxList>
            <NxList.LinkItem href={href}>{getMessage()}</NxList.LinkItem>
          </NxList>
        </NxTile.Content>
      </NxLoadWrapper>
    </NxTile>
  );
}
