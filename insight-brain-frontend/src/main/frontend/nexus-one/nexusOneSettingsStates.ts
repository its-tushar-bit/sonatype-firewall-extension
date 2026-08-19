/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ReactStateDeclaration } from '@uirouter/react';
import SettingsPage from 'MainRoot/nosc/settings/SettingsPage';

const SETTINGS_TITLE = 'Settings';

/**
 * Native Nexus One Settings hub. Visible to all authenticated users; the
 * Admin Console section inside the page self-hides for users without
 * CONFIGURE_SYSTEM, so no route-level redirect guard is needed here.
 */
export function nexusOneSettingsStates(): ReactStateDeclaration[] {
  return [
    {
      name: 'nexusOneSettings',
      url: '/settings',
      component: SettingsPage,
      data: { title: SETTINGS_TITLE },
    },
  ];
}
