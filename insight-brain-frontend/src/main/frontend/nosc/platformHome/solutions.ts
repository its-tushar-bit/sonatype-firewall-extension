/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
// Icon URLs provided by esbuild's default file loader. Same pattern used by
// guide/layout/GuideLogo.tsx and other IQ frontend modules.
import guideIcon from './icons/sonatype-guide-icon.svg';
import lifecycleIcon from './icons/sonatype-lifecycle-icon.svg';
import repositoryIcon from './icons/sonatype-repository-icon.svg';
import firewallIcon from './icons/sonatype-firewall-icon.svg';
import sbomManagerIcon from './icons/sonatype-sbom-manager-icon.svg';

export interface Solution {
  /** Stable identifier used in URLs, test ids, and analytics. */
  id: 'guide' | 'lifecycle' | 'repository' | 'firewall' | 'sbom-manager';
  /** Customer-facing product name. */
  name: string;
  /** Single-sentence description shown under the name. */
  description: string;
  /** Path to the SVG icon (URL-loaded via esbuild). */
  icon: string;
  /**
   * If `internal` is true, `href` is a Nexus One in-bundle hash path (e.g.
   * `/dashboard`, `/coming-soon/guide`). Platform Home navigates via
   * `bundleIndexUrl('nexus-one', href)`. If false, `href` is a full URL opened
   * in a new tab.
   */
  internal: boolean;
  href: string;
  /**
   * Whether the destination has shipped today. If `internal && !inIQToday`,
   * the tile lands on `PreviewPagePlaceholder` and is labeled "Coming soon".
   * Used purely for UI affordance — does NOT gate the click.
   */
  inIQToday: boolean;
}

/**
 * The 5 Sonatype solutions surfaced on the Platform Home, in render order.
 *
 * Lifecycle is the only `inIQToday: true` entry today (it's IQ's primary
 * product, F6 dashboard ships in PR-6). Firewall and SBOM Manager already
 * live in the same JVM as IQ but don't have Preview surfaces yet — they
 * land on PreviewPagePlaceholder until each product team ships its surface.
 * Guide is `internal: true` because it will be fully integrated into IQ by
 * Phase-1 launch; until then its tile also lands on the placeholder.
 *
 * Repository is the only external link — it's a separate JVM, separate
 * codebase, separate team. Clicking opens sonatype.com in a new tab.
 */
export const SOLUTIONS: ReadonlyArray<Solution> = [
  {
    id: 'guide',
    name: 'Sonatype Guide',
    description: 'Guide AI coding assistants with open source intelligence.',
    icon: guideIcon,
    internal: true,
    href: '/coming-soon/guide',
    inIQToday: false,
  },
  {
    id: 'lifecycle',
    name: 'Sonatype Lifecycle',
    description: 'Avoid rework with automated SCA and remediation.',
    icon: lifecycleIcon,
    internal: true,
    href: '/dashboard',
    inIQToday: true,
  },
  {
    id: 'repository',
    name: 'Sonatype Nexus Repository',
    description: 'Build fast with a centralized binary repository.',
    icon: repositoryIcon,
    internal: false,
    href: 'https://www.sonatype.com/products/sonatype-nexus-repository',
    inIQToday: false,
  },
  {
    id: 'firewall',
    name: 'Sonatype Repository Firewall',
    description: 'Reduce remediation with OSS malware protection.',
    icon: firewallIcon,
    internal: true,
    href: '/coming-soon/firewall',
    inIQToday: false,
  },
  {
    id: 'sbom-manager',
    name: 'Sonatype SBOM Manager',
    description: 'Automate software compliance and reporting.',
    icon: sbomManagerIcon,
    internal: true,
    href: '/coming-soon/sbom-manager',
    inIQToday: false,
  },
];
