/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { NxH2, NxTile, NxList, NxLoadWrapper } from '@sonatype/react-shared-components';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectIsGrandfatheringSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  selectGrandfatheringStatusMessage,
  selectGrandfatheringLinkParams,
  selectLoadError,
  selectLoading,
} from '../policyViolationGrandfatheringSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/policyViolationGrandfatheringSlice';

export default function PolicyGrandfatheringTile() {
  const dispatch = useDispatch();
  const uiStateRouter = useRouterState();

  const isGrandfatheringSupported = useSelector(selectIsGrandfatheringSupported);
  const grandfatheringStatusMessage = useSelector(selectGrandfatheringStatusMessage);
  const isLoading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);

  const doLoad = () => dispatch(actions.loadPolicyViolationGrandfathering());

  const { to, params } = useSelector(selectGrandfatheringLinkParams);
  const href = uiStateRouter.href(to, params);

  useEffect(() => {
    doLoad();
  }, []);

  const renderContent = () => {
    if (isGrandfatheringSupported) {
      return <NxList.LinkItem href={href}>{grandfatheringStatusMessage}</NxList.LinkItem>;
    }
    return <NxList.Item>Policy Violation Grandfathering is not supported by your license</NxList.Item>;
  };

  return (
    <NxTile id="owner-pill-grandfathering">
      <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={doLoad}>
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>Policy Violation Grandfathering</NxH2>
          </NxTile.HeaderTitle>
        </NxTile.Header>
        <NxTile.Content>
          <NxList id="policy-violation-grandfathering">{renderContent()}</NxList>
        </NxTile.Content>
      </NxLoadWrapper>
    </NxTile>
  );
}
