/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { NxList, NxTile, NxH2, NxLoadWrapper } from '@sonatype/react-shared-components';
import ownerConstant from 'MainRoot/utility/services/owner.constant';

import { actions } from 'MainRoot/OrgsAndPolicies/waivedComponentUpgradesSlice';
import {
  selectWaivedComponentUpgrades,
  selectUpgradeMonitoringLinkParams,
} from 'MainRoot/OrgsAndPolicies/waivedComponentUpgradesSelectors';

import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

export default function WaivedComponentUpgradesTile() {
  const dispatch = useDispatch();
  const uiRouterState = useRouterState();
  const owner = useSelector(selectSelectedOwner);
  const { to, params } = useSelector(selectUpgradeMonitoringLinkParams);

  const href = uiRouterState.href(to, params);

  const { loading, loadError, configuredStage } = useSelector(selectWaivedComponentUpgrades);

  const renderConfiguredStage = () => (
    <span className="waived-component-upgrades__stage-name">{configuredStage.replace(/-/g, ' ')}</span>
  );

  const doLoad = () => {
    dispatch(actions.loadUpgradeStage());
  };

  useEffect(() => {
    doLoad();
  }, []);

  const renderRootOrgLinkText = () => {
    return (
      <>
        <span className="waived-component-upgrades__list-line">
          Upgrade monitoring is {configuredStage == null && 'not'} configured
        </span>
        <br />
        {configuredStage !== null && (
          <span>Inherited by all organizations and applications ({renderConfiguredStage()})</span>
        )}
      </>
    );
  };

  const renderAppOrgLinkText = () => (
    <>
      <span className="waived-component-upgrades__list-line">
        Upgrade monitoring can only be configured at the root org level
      </span>
      <br />
      <span>
        Inheriting from root organization ({configuredStage == null ? 'not configured' : renderConfiguredStage()})
      </span>
    </>
  );

  return (
    <NxTile id="owner-pill-waived-component-upgrades">
      <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>Waived Component Upgrades</NxH2>
          </NxTile.HeaderTitle>
          <NxTile.HeaderSubtitle>Indicate when a component upgrade is available</NxTile.HeaderSubtitle>
        </NxTile.Header>
        <NxTile.Content>
          <NxList id="waived-component-upgrades">
            <NxList.LinkItem href={href}>
              {owner.id == ownerConstant.ROOT_ORGANIZATION_ID ? renderRootOrgLinkText() : renderAppOrgLinkText()}
            </NxList.LinkItem>
          </NxList>
        </NxTile.Content>
      </NxLoadWrapper>
    </NxTile>
  );
}
