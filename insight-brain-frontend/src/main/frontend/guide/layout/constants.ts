/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Home, Package, AlertTriangle, Zap, Shield } from 'lucide-react';
import { NavGroup } from './types';

export const SIDEBAR_STORAGE_KEY = 'guide.sidebar.collapsed';
export const SIDEBAR_WIDTH_COLLAPSED = '60px';
export const SIDEBAR_WIDTH_EXPANDED = '240px';

export const SIDEBAR_GROUPS: NavGroup[] = [
  {
    id: 'main',
    items: [
      {
        id: 'home',
        label: 'Home',
        href: '/',
        icon: Home,
      },
      {
        id: 'components',
        label: 'Components',
        href: '/components',
        icon: Package,
      },
      {
        id: 'vulnerabilities',
        label: 'Vulnerabilities',
        href: '/vulnerabilities',
        icon: AlertTriangle,
      },
      {
        id: 'security-events',
        label: 'Security Events',
        href: '/security-events',
        icon: Shield,
      },
      {
        id: 'mcp',
        label: 'MCP',
        href: '/mcp',
        icon: Zap,
      },
    ],
  },
];
