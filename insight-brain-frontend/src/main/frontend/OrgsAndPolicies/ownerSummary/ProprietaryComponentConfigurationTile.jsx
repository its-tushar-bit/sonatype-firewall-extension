/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxH2, NxTile, NxList } from '@sonatype/react-shared-components';
import { useSelector } from 'react-redux';

import { useRouterState } from 'MainRoot/react/RouterStateContext';
import {
  selectProprietaryConfigInheritedMatchersCount,
  selectProprietaryConfigLocalMatchersCount,
} from '../proprietarySelectors';
import { selectIsRootOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectRouterSlice } from '../../reduxUiRouter/routerSelectors';
import { deriveEditRoute } from '../utility/util';

export default function ProprietaryComponentConfigurationTile() {
  const router = useSelector(selectRouterSlice());
  const { to, params } = deriveEditRoute(router, 'proprietary-config-policy');
  const uiStateRouter = useRouterState();
  const href = uiStateRouter.href(to, params);

  const isRootOrg = useSelector(selectIsRootOrganization);
  const inheritedProprietaryCount = useSelector(selectProprietaryConfigInheritedMatchersCount);
  const localProprietaryCount = useSelector(selectProprietaryConfigLocalMatchersCount);

  const inheritedProprietaryText = `, ${inheritedProprietaryCount} inherited`;
  const localProprietaryText = `${localProprietaryCount} local`;

  return (
    <NxTile id="owner-pill-component-configuration">
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
    </NxTile>
  );
}
