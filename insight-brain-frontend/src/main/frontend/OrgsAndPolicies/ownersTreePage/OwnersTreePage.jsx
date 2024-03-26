/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { includes } from 'ramda';
import { NxPageMain, NxLoadWrapper, NxH1 } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';

import { actions } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSlice';
import { actions as ownersTreeActions } from 'MainRoot/OrgsAndPolicies/ownersTreeSlice';
import {
  selectLoading,
  selectLoadError,
  selectTopParentOrganizationId,
  selectPrevStateOwnerName,
} from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';
import { selectIsOwnerNodeExpanded } from 'MainRoot/OrgsAndPolicies/ownersTreeSelectors';
import OwnersTreeTile from './OwnerTreeTile';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import {
  selectRouterPrevState,
  selectRouterPrevParams,
  selectPrevStateIsAppOwnerManagementView,
  selectIsSbomManager,
} from 'MainRoot/reduxUiRouter/routerSelectors';

export default function OwnersTreePage() {
  const dispatch = useDispatch();
  const load = () => dispatch(actions.loadIfNeeded());
  const toogleTreeNode = (payload) => dispatch(ownersTreeActions.toogleTreeNode(payload));

  const loading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);
  const topParentOrganizationId = useSelector(selectTopParentOrganizationId);

  const uiRouterState = useRouterState();
  const prevState = useSelector(selectRouterPrevState);
  const prevParams = useSelector(selectRouterPrevParams);

  const prevStateIncludesOwnerPage = includes(prevState?.name, [
    'management.view.organization',
    'management.view.application',
    'sbomManager.management.view.organization',
    'sbomManager.management.view.application',
  ]);
  const prevStateWasApp = useSelector(selectPrevStateIsAppOwnerManagementView);
  const property = prevStateIncludesOwnerPage ? (prevStateWasApp ? 'applicationPublicId' : 'organizationId') : '';

  const isSbomManager = useSelector(selectIsSbomManager);
  const backButtonHref = uiRouterState.href(
    prevStateIncludesOwnerPage ? prevState.name : `${isSbomManager ? 'sbomManager.' : ''}management.view`,
    prevStateIncludesOwnerPage ? { [property]: prevParams[property] } : {}
  );

  const backButtonText = useSelector((state) => selectPrevStateOwnerName(state, prevParams[property]));

  useEffect(() => {
    load();
  }, []);

  return (
    <NxPageMain className="ownersTreeView">
      <MenuBarBackButton href={backButtonHref} text={backButtonText ? `Back to ${backButtonText}` : 'Back'} />
      <header className="nx-page-title">
        <NxH1>Inheritance Hierarchy</NxH1>
      </header>
      <NxLoadWrapper loading={loading} retryHandler={load} error={loadError}>
        <OwnersTreeTile
          topParentOrganizationId={topParentOrganizationId}
          isNodeOpenSelector={selectIsOwnerNodeExpanded}
          onToggleTreeNode={toogleTreeNode}
        />
      </NxLoadWrapper>
    </NxPageMain>
  );
}
