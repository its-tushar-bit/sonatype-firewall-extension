/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';
import { UIView } from '@uirouter/react';
import { selectRouterState } from 'MainRoot/reduxUiRouter/routerSelectors';
import MenuBarStatefulBreadcrumb from 'MainRoot/mainHeader/MenuBar/MenuBarStatefulBreadcrumb';
import OwnerSideNav from 'MainRoot/OrgsAndPolicies/ownerSideNav/OwnerSideNav';

export function OwnerManagerViewWrapper() {
  const currentState = useSelector(selectRouterState);
  const currentStateData = currentState?.data || {};
  const noSidebar = currentStateData.noSidebar;
  const viewportSized = currentStateData.viewportSized;
  const hideOverflowY = currentStateData.hideOverflowY;

  const mainClassName = `nx-page-main ${viewportSized ? 'nx-viewport-sized__container' : ''} ${
    noSidebar ? 'nx-page-main--no-sidebar' : ''
  }`.trim();

  const uiViewClassName = viewportSized ? 'nx-viewport-sized__container' : '';

  return (
    <>
      {!noSidebar && <OwnerSideNav className="nx-viewport-sized__container" />}
      <div
        id="owner-manager-main"
        className={mainClassName}
        style={{ overflowY: hideOverflowY ? 'hidden' : undefined }}
      >
        <MenuBarStatefulBreadcrumb />
        <UIView className={uiViewClassName} />
      </div>
    </>
  );
}
