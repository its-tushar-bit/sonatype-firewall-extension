/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';

import { NxList, NxTile, NxH2, NxLoadWrapper } from '@sonatype/react-shared-components';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { actions } from 'MainRoot/OrgsAndPolicies/publicDataSources/publicDataSourcesSlice';
import {
  selectCpeConfiguration,
  selectPublicDatasourcesLinkParams,
  selectLoading,
  selectLoadError,
} from 'MainRoot/OrgsAndPolicies/publicDataSources/publicDataSourcesSelectors';

export default function PublicDataSourcesTile() {
  const PUBLIC_DATA_SOURCES_TILE_BASE_MSG = 'Public Data Sources are';
  const DISABLED_TILE_MESSAGE = `${PUBLIC_DATA_SOURCES_TILE_BASE_MSG} disabled`;
  const ENABLED_TILE_MESSAGE = `${PUBLIC_DATA_SOURCES_TILE_BASE_MSG} enabled`;
  const uiStateRouter = useRouterState();
  const { to, params } = useSelector(selectPublicDatasourcesLinkParams);
  const href = uiStateRouter.href(to, params);

  const dispatch = useDispatch();
  const ownerCpeMatchingConfigData = useSelector(selectCpeConfiguration);

  const isLoading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);

  const doLoad = () => {
    dispatch(actions.loadCpeConfiguration())
      .then((response) => response)
      .catch((err) => err);
  };

  useEffect(() => {
    doLoad();
  }, []);

  const renderContent = () => {
    let contentTile;

    if (!ownerCpeMatchingConfigData) {
      contentTile = DISABLED_TILE_MESSAGE;
    } else {
      contentTile = ownerCpeMatchingConfigData?.enabled ? ENABLED_TILE_MESSAGE : DISABLED_TILE_MESSAGE;
      if (ownerCpeMatchingConfigData.inheritedFromOrganizationName !== null) {
        contentTile += ' (Inherited from ' + ownerCpeMatchingConfigData.inheritedFromOrganizationName + ')';
      }
    }

    return <NxList.LinkItem href={href}>{contentTile}</NxList.LinkItem>;
  };

  return (
    <NxTile id="owner-pill-public-data-sources" data-testid="owner-pill-public-data-sources">
      <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={doLoad}>
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>Public Data Sources</NxH2>
          </NxTile.HeaderTitle>
        </NxTile.Header>
        <NxTile.Content>
          <NxList id="public-data-sources">{renderContent()}</NxList>
        </NxTile.Content>
      </NxLoadWrapper>
    </NxTile>
  );
}
