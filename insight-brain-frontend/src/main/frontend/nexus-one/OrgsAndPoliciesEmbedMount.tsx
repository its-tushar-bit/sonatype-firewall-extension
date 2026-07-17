/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useLayoutEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import classnames from 'classnames';
import { ClassicComponentMount } from 'MainRoot/nexus-one/ClassicComponentMount';
import { selectRouterState } from 'MainRoot/reduxUiRouter/routerSelectors';
import { IQ_SIDEBAR_CONTAINER_ID } from 'MainRoot/util/constants';
// The Classic bundle pulls every management.* stylesheet through scss/scss.scss; the Nexus One
// bundle does not, so load them here so the embedded pages render with parity. CLM-42161.
import 'MainRoot/scss/orgsAndPoliciesEmbed.scss';

/**
 * Chrome mount for the embedded Orgs and Policies (`management.*`) pages.
 *
 * Unlike the other Classic embeds, the owner-management pages render their tree/detail sidebar
 * (OwnerSideNav / OwnerDetailSidebar) via `ReactDOM.createPortal(..., #iq-sidebar-container)`. That
 * portal host, and the `#iq-content.nx-page-content` grid that lays the sidebar out beside the page
 * (see Classic App.jsx and react/iqSidebarNav/_iqSidebarNav.scss), are rendered by the Classic
 * bundle's App.jsx, which the Nexus One shell never loads. Without the host, createPortal is handed
 * a null container and throws, blanking the whole page. So reproduce Classic's `#iq-content` subtree
 * here: the empty `#iq-sidebar-container` (`display: contents`, so its portaled `.nx-page-sidebar`
 * child participates directly in the grid) and the page in `#iq-footer-container` (grid-area: content).
 *
 * OwnerSideNav / OwnerDetailSidebar resolve their portal host with `document.getElementById` during
 * render, so `#iq-sidebar-container` must already be in the committed DOM before the page component
 * renders. In Classic that ordering is free (App.jsx mounts the host long before any owner route);
 * here host and page would otherwise mount in the same commit, handing createPortal a null target.
 * Gate the page on a post-commit flag so the first commit lays down only the host, and the page
 * (and its portal) renders on the next.
 *
 * `#iq-footer-container` gets `nx-viewport-sized` when the current state is viewport-sized (owner
 * summary pages), exactly as Classic App.jsx does. That class flips the footer to `overflow: hidden`
 * and lets `.nx-page-main` shrink, so the page's own `.nx-viewport-sized__scrollable` region (e.g.
 * `#owner-summary-sections`) scrolls internally with a sticky header; without it the whole mount
 * scrolls and the summary's nav-pill jump-links (which scroll that region) do nothing.
 */
export function mountOrgsAndPoliciesChrome(Component: React.ComponentType): React.ComponentType {
  return function MountedOrgsAndPoliciesChrome() {
    const [hostReady, setHostReady] = useState(false);
    useLayoutEffect(() => setHostReady(true), []);

    const currentState = useSelector(selectRouterState);
    const viewportSized = Boolean(currentState?.data?.viewportSized);

    return (
      <ClassicComponentMount>
        <div id="iq-content" className="nx-page-content nx-page-content--full-width">
          <div id={IQ_SIDEBAR_CONTAINER_ID} />
          <div
            id="iq-footer-container"
            className={classnames('nx-global-footer-2-container', { 'nx-viewport-sized': viewportSized })}
          >
            {hostReady && <Component />}
          </div>
        </div>
      </ClassicComponentMount>
    );
  };
}
