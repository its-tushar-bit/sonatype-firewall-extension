/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { ReactStateDeclaration } from '@uirouter/react';
import router from 'MainRoot/router/routerInstance';
import { PlatformHome } from 'MainRoot/nosc/platformHome/PlatformHome';
import {
  COMING_SOON_MODULE_ORDER,
  comingSoonHref,
  ComingSoonRoute,
} from 'MainRoot/nosc/comingSoon';
import { PreviewPagePlaceholder } from 'MainRoot/nosc/shell/PreviewPagePlaceholder';
import { NEXUS_ONE_DEFAULT_PATH } from 'MainRoot/nosc/routing/classicPreviewMap';

function readHashPath(): string {
  const rawHash = typeof window !== 'undefined' ? window.location.hash : '';
  return rawHash.startsWith('#') ? rawHash.slice(1) : rawHash;
}

function NexusOneRoutePlaceholder(): JSX.Element {
  return <PreviewPagePlaceholder route={readHashPath()} />;
}

router.stateRegistry.register({
  name: 'root',
  url: '^',
  redirectTo: 'platformHome',
});

router.stateRegistry.register({
  name: 'home',
  url: '/',
  redirectTo: 'platformHome',
});

router.stateRegistry.register({
  name: 'platformHome',
  url: '/home',
  component: PlatformHome,
  data: {
    title: 'Nexus One',
  },
} as ReactStateDeclaration);

const PLACEHOLDER_ROUTES: ReadonlyArray<{ readonly name: string; readonly url: string }> = [
  { name: 'nexusOneDashboard', url: '/dashboard' },
  { name: 'nexusOneDashboardViolations', url: '/dashboard/violations' },
  { name: 'nexusOneDashboardComponents', url: '/dashboard/components' },
  { name: 'nexusOneDashboardApplications', url: '/dashboard/applications' },
  { name: 'nexusOneDashboardWaivers', url: '/dashboard/waivers' },
  { name: 'nexusOneApplications', url: '/applications' },
  { name: 'nexusOneApplicationsDetail', url: '/applications/{publicId}' },
  { name: 'nexusOneSearch', url: '/search' },
  { name: 'nexusOneWaivers', url: '/waivers' },
];

PLACEHOLDER_ROUTES.forEach(({ name, url }) => {
  router.stateRegistry.register({
    name,
    url,
    component: NexusOneRoutePlaceholder,
  } as ReactStateDeclaration);
});

function slugToStateName(slug: string): string {
  const pascal = slug
    .split('-')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join('');
  return 'nexusOneComingSoon' + pascal;
}

COMING_SOON_MODULE_ORDER.forEach((slug) => {
  router.stateRegistry.register({
    name: slugToStateName(slug),
    url: comingSoonHref(slug),
    component: ComingSoonRoute,
    data: {
      title: 'Nexus One',
    },
  } as ReactStateDeclaration);
});

router.urlService.rules.otherwise(NEXUS_ONE_DEFAULT_PATH);
