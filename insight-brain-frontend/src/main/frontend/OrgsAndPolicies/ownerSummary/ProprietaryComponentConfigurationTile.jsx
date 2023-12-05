/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { NxH2, NxTile, NxList, NxLoadWrapper } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';

import { useRouterState } from 'MainRoot/react/RouterStateContext';
import {
  selectProprietaryConfigInheritedMatchersCount,
  selectProprietaryConfigLocalMatchersCount,
} from '../proprietarySelectors';
import { selectIsRootOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectRouterSlice } from '../../reduxUiRouter/routerSelectors';
import { deriveEditRoute } from '../utility/util';
import { actions as proprietaryConfigActions } from 'MainRoot/OrgsAndPolicies/proprietarySlice';
import { selectIsLoading, selectLoadError } from 'MainRoot/OrgsAndPolicies/proprietarySelectors';
import { selectIsProprietaryComponentsEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';

export default function ProprietaryComponentConfigurationTile() {
  const dispatch = useDispatch();
  const loadData = () => {
    dispatch(proprietaryConfigActions.loadProprietaryConfig());
  };

  useEffect(() => {
    loadData();
  }, []);

  const isLoading = useSelector(selectIsLoading);
  const loadError = useSelector(selectLoadError);

  const router = useSelector(selectRouterSlice());
  const { to, params } = deriveEditRoute(router, 'proprietary-config-policy');
  const uiStateRouter = useRouterState();
  const href = uiStateRouter.href(to, params);

  const isProprietaryComponentsEnabled = useSelector(selectIsProprietaryComponentsEnabled);

  const isRootOrg = useSelector(selectIsRootOrganization);
  const inheritedProprietaryCount = useSelector(selectProprietaryConfigInheritedMatchersCount);
  const localProprietaryCount = useSelector(selectProprietaryConfigLocalMatchersCount);
  const inheritedProprietaryText = `, ${inheritedProprietaryCount} inherited`;
  const localProprietaryText = `${localProprietaryCount} local`;

  return (
    isProprietaryComponentsEnabled && (
      <NxTile id="owner-pill-component-configuration">
        <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={loadData}>
          <NxTile.Header>
            <NxTile.HeaderTitle>
              <NxH2>Proprietary Component Configuration</NxH2>
            </NxTile.HeaderTitle>
          </NxTile.Header>
          <NxTile.Content>
            <NxList id="proprietary-component-matchers">
              <NxList.LinkItem href={href}>
                <NxList.Text>
                  {localProprietaryText}
                  {!isRootOrg && inheritedProprietaryText}
                </NxList.Text>
              </NxList.LinkItem>
            </NxList>
          </NxTile.Content>
        </NxLoadWrapper>
      </NxTile>
    )
  );
}
