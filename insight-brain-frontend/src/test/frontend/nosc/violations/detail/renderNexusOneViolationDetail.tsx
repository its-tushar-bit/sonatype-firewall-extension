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
import {
  NEXUS_ONE_VIOLATION_DETAIL_OVERVIEW_STATE,
  nexusOneViolationDetailStates,
} from 'MainRoot/nexus-one/nexusOneViolationDetailStates';

/**
 * Router-aware harness for the Violation Detail nested-view tree.
 * Mirrors renderNexusOneApplicationDetail: production states + UIView.
 */
export function createNexusOneViolationDetailRouter(violationId: string): UIRouterReact {
  const router = new UIRouterReact();
  router.plugin(servicesPlugin);
  router.plugin(memoryLocationPlugin);
  router.urlService.config.strictMode(false);
  router.stateRegistry.register({ name: 'nexusOneViolations', url: '/violations' });
  router.stateRegistry.register({
    name: 'nexusOneWaiverDetail',
    url: '/waivers/{ownerType}/{ownerId}/{waiverId}?from&type',
  });
  nexusOneViolationDetailStates().forEach((state) => router.stateRegistry.register(state));
  router.urlService.rules.initial({
    state: NEXUS_ONE_VIOLATION_DETAIL_OVERVIEW_STATE,
    params: { id: violationId },
  });
  return router;
}

export function renderNexusOneViolationDetail(
  violationId: string,
  renderOptions?: Parameters<typeof render>[1],
) {
  const router = createNexusOneViolationDetailRouter(violationId);
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
