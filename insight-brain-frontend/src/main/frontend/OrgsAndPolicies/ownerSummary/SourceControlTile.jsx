/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxH2, NxTile, NxList, NxLoadWrapper, NxInfoAlert, NxH3 } from '@sonatype/react-shared-components';

import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectSelectedOwner, selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { actions as sourceControlActions } from 'MainRoot/OrgsAndPolicies/sourceControlSlice';
import { selectIsOrganization, selectIsRootOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectIsScmEnabled,
  selectIsSourceControlForSourceTileSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { deriveEditRoute } from '../utility/util';
import {
  selectSourceControl,
  selectEffectiveProvider,
  selectItemSubText,
  selectItemText,
  selectLoading,
  selectLoadError,
} from 'MainRoot/OrgsAndPolicies/sourceControlSelectors';
import { selectRouterSlice } from 'MainRoot/reduxUiRouter/routerSelectors';

export default function SourceControlTile() {
  const dispatch = useDispatch();

  const isOrganization = useSelector(selectIsOrganization);
  const isRootOrganization = useSelector(selectIsRootOrganization);
  const ownerName = useSelector(selectSelectedOwnerName);
  const isSourceControlSupported = useSelector(selectIsSourceControlForSourceTileSupported);

  const isLoading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);
  const itemText = useSelector(selectItemText);
  const itemSubText = useSelector(selectItemSubText);
  const sourceControl = useSelector(selectSourceControl);
  const effectiveProvider = useSelector(selectEffectiveProvider);

  const uiStateRouter = useRouterState();
  const router = useSelector(selectRouterSlice());
  const { to, params } = deriveEditRoute(router, 'edit-source-control');
  const href = uiStateRouter.href(to, params);
  const currentOwner = useSelector(selectSelectedOwner);

  const isScmFeatureEnabled = useSelector(selectIsScmEnabled);

  const loadSourceControl = () => dispatch(sourceControlActions.loadSourceControl());
  useEffect(() => {
    if (isSourceControlSupported) {
      loadSourceControl();
    }
  }, [currentOwner, isSourceControlSupported]);

  const subtitle = `Configures the integration with an external SCM for the ${ownerName} ${
    isOrganization ? (isRootOrganization ? '' : 'organization') : 'application'
  }`;

  const renderContent = () => {
    const showText = !!(sourceControl && effectiveProvider);
    return (
      <NxTile.Content>
        <NxH3>Configuration</NxH3>
        <NxList id="legacy-violations">
          <NxList.LinkItem href={href}>
            {showText && (
              <>
                <NxList.Text>{itemText}</NxList.Text>
                <NxList.Subtext>{itemSubText}</NxList.Subtext>
              </>
            )}
            {!showText && <NxList.Text>{itemSubText}</NxList.Text>}
          </NxList.LinkItem>
        </NxList>
      </NxTile.Content>
    );
  };

  return (
    isSourceControlSupported &&
    isScmFeatureEnabled && (
      <NxTile id="owner-pill-source-control">
        <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={loadSourceControl}>
          <NxTile.Header>
            <NxTile.Headings>
              <NxTile.HeaderTitle>
                <NxH2>Source Control</NxH2>
              </NxTile.HeaderTitle>
              <NxTile.HeaderSubtitle>{subtitle}</NxTile.HeaderSubtitle>
            </NxTile.Headings>
          </NxTile.Header>
          {renderContent()}
        </NxLoadWrapper>
      </NxTile>
    )
  );
}
