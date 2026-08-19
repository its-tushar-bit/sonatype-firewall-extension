/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { NxH2, NxTile, NxList, NxLoadWrapper } from '@sonatype/react-shared-components';
import { selectEntityId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectRouterSlice } from 'MainRoot/reduxUiRouter/routerSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { deriveEditRoute } from 'MainRoot/OrgsAndPolicies/utility/util';
import {
  selectLoading,
  selectLoadError,
  selectNotificationDays,
  selectInheritConfig,
} from 'MainRoot/OrgsAndPolicies/waiverExpirationNotificationSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/waiverExpirationNotificationSlice';

export default function WaiverExpirationNotificationTile() {
  const dispatch = useDispatch();
  const entityId = useSelector(selectEntityId);
  const loading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);
  const notificationDays = useSelector(selectNotificationDays);
  const inheritConfig = useSelector(selectInheritConfig);

  const router = useSelector(selectRouterSlice);
  const { to, params } = deriveEditRoute(router, 'edit-waiver-expiration-notification');
  const uiStateRouter = useRouterState();
  const href = uiStateRouter.href(to, params);

  const doLoad = () => dispatch(actions.loadConfig());

  useEffect(() => {
    doLoad();
  }, [entityId]);

  function getSummaryText() {
    if (inheritConfig) {
      return 'Inherited from parent';
    }
    if (!notificationDays || notificationDays.length === 0) {
      return 'No notification schedule configured';
    }
    const days = [...notificationDays].sort((a, b) => b - a);
    return `Notify ${days.join(', ')} day(s) before expiry`;
  }

  return (
    <NxTile id="owner-pill-waiver-expiration-notification">
      <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>Waiver Expiration Notifications</NxH2>
          </NxTile.HeaderTitle>
        </NxTile.Header>
        <NxTile.Content>
          <NxList>
            <NxList.LinkItem href={href}>
              <NxList.Text>{getSummaryText()}</NxList.Text>
            </NxList.LinkItem>
          </NxList>
        </NxTile.Content>
      </NxLoadWrapper>
    </NxTile>
  );
}
