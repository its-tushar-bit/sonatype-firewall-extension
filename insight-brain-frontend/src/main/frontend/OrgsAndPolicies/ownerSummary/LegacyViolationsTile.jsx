/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { NxH2, NxTile, NxList, NxLoadWrapper } from '@sonatype/react-shared-components';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectIsLegacyViolationSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  selectLegacyViolationsStatusMessage,
  selectLegacyViolationLinkParams,
  selectLoadError,
  selectLoading,
} from '../legacyViolationSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/legacyViolationSlice';

export default function LegacyViolationsTile() {
  const dispatch = useDispatch();
  const uiStateRouter = useRouterState();

  const isLegacyViolationsEnabled = useSelector(selectIsLegacyViolationSupported);
  const legacyViolationsStatusMessage = useSelector(selectLegacyViolationsStatusMessage);
  const isLoading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);

  const doLoad = () => dispatch(actions.loadLegacyViolation());

  const { to, params } = useSelector(selectLegacyViolationLinkParams);
  const href = uiStateRouter.href(to, params);

  useEffect(() => {
    doLoad();
  }, []);

  const renderContent = () => {
    return <NxList.LinkItem href={href}>{legacyViolationsStatusMessage}</NxList.LinkItem>;
  };

  return (
    isLegacyViolationsEnabled && (
      <NxTile id="owner-pill-legacy-violations">
        <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={doLoad}>
          <NxTile.Header>
            <NxTile.HeaderTitle>
              <NxH2>Legacy Violations</NxH2>
            </NxTile.HeaderTitle>
          </NxTile.Header>
          <NxTile.Content>
            <NxList id="legacy-violations">{renderContent()}</NxList>
          </NxTile.Content>
        </NxLoadWrapper>
      </NxTile>
    )
  );
}
