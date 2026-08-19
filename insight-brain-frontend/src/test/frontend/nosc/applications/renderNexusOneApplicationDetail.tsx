/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  UIRouter,
  UIRouterReact,
  UIView,
  memoryLocationPlugin,
  servicesPlugin,
} from '@uirouter/react';
import { Theme } from '@radix-ui/themes';
import { render } from 'TestRoot/SpecUtil';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';
import { nexusOneApplicationDetailStates } from 'MainRoot/nexus-one/nexusOneApplicationDetailStates';
import { tabFromSlug, TAB_TO_URL } from 'MainRoot/nosc/applications/applicationDetailUtils';
import type { TabId } from 'MainRoot/nosc/applications/applicationDetailTypes';

/**
 * Router-aware harness for the Application Detail nested-view tree (CLM-40901).
 * Mirrors {@link renderNexusOneDashboard}: production state declarations + UIView.
 */
export function createNexusOneApplicationDetailRouter(
  publicId: string,
  tabSlug?: string,
): UIRouterReact {
  const router = new UIRouterReact();
  router.plugin(servicesPlugin);
  router.plugin(memoryLocationPlugin);
  router.urlService.config.strictMode(false);
  router.stateRegistry.register({ name: 'nexusOneApplications', url: '/applications' });
  nexusOneApplicationDetailStates().forEach((state) => router.stateRegistry.register(state));
  router.stateRegistry.register({
    name: 'nexusOneWaivers',
    url: '/waivers',
  });
  router.stateRegistry.register({
    name: 'nexusOneWaiverDetail',
    url: '/waivers/{ownerType}/{ownerId}/{waiverId}?from&type',
  });
  const tabId: TabId = tabFromSlug(tabSlug);
  const suffix = TAB_TO_URL[tabId];
  router.urlService.rules.initial({
    state: `nexusOneApplicationsDetail.${suffix}`,
    params: { publicId },
  });
  return router;
}

export function renderNexusOneApplicationDetail(
  publicId: string,
  tabSlug?: string,
  renderOptions?: Parameters<typeof render>[1],
) {
  const router = createNexusOneApplicationDetailRouter(publicId, tabSlug);
  const result = render(
    <Theme accentColor={BRAND_ACCENT} grayColor="slate" radius="medium" scaling="100%">
      <UIRouter router={router}>
        <UIView />
      </UIRouter>
    </Theme>,
    renderOptions,
  );
  return { router, ...result };
}
