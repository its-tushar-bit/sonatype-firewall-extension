/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import developerLight from './images/light/developer-color-icon.svg';
import developerDark from './images/dark/developer-color-reversed-icon.svg';
import lifecycleLight from './images/light/lifecycle-color-icon.svg';
import lifecycleDark from './images/dark/lifecycle-color-reversed-icon.svg';
import nxrmLight from './images/light/repository-color-icon.svg';
import nxrmDark from './images/dark/repository-color-reversed-icon.svg';
import firewallLight from './images/light/firewall-color-icon.svg';
import firewallDark from './images/dark/firewall-color-reversed-icon.svg';
import sbomLight from './images/light/sbom-color-icon.svg';
import sbomDark from './images/dark/sbom-color-reversed-icon.svg';
import guideLight from './images/light/guide-color-icon.svg';
import guideDark from './images/dark/guide-color-reversed-icon.svg';

export type SolutionId =
  | 'developer'
  | 'lifecycle'
  | 'nexusRepositoryManager'
  | 'firewall'
  | 'sbom'
  | 'guide';

export interface LicensedSolution {
  id: SolutionId;
  url: string;
}

export interface ProductMetadata {
  displayName: string;
  iconLight: string;
  iconDark: string;
  // Marketing/upsell URL used by the Explore section when the product isn't licensed.
  // Mirrors the legacy `defaultSolutionsList` URLs. Omit to exclude from Explore
  // (e.g. Developer has no marketing page yet — matches the legacy filter).
  marketingUrl?: string;
}

export type LicensedProduct =
  | {
      id: SolutionId;
      displayName: string;
      url: string;
    }
  | {
      id: SolutionId;
      displayName: string;
      instances: { url: string }[];
    };

export const PRODUCT_METADATA: Record<SolutionId, ProductMetadata> = {
  developer: {
    displayName: 'Developer',
    iconLight: developerLight,
    iconDark: developerDark,
  },
  lifecycle: {
    displayName: 'Lifecycle',
    iconLight: lifecycleLight,
    iconDark: lifecycleDark,
    marketingUrl:
      'https://www.sonatype.com/products/open-source-security-dependency-management?utm_campaign=Solution-Switcher&utm_source=product&utm_medium=lifecycle',
  },
  nexusRepositoryManager: {
    displayName: 'Nexus Repository',
    iconLight: nxrmLight,
    iconDark: nxrmDark,
    marketingUrl:
      'https://www.sonatype.com/products/sonatype-nexus-repository?utm_campaign=Solution-Switcher&utm_source=product&utm_medium=repository-manager',
  },
  firewall: {
    displayName: 'Repository Firewall',
    iconLight: firewallLight,
    iconDark: firewallDark,
    marketingUrl:
      'https://www.sonatype.com/products/sonatype-repository-firewall?utm_campaign=Solution-Switcher&utm_source=product&utm_medium=firewall',
  },
  sbom: {
    displayName: 'SBOM Manager',
    iconLight: sbomLight,
    iconDark: sbomDark,
    marketingUrl:
      'https://www.sonatype.com/products/sonatype-sbom-manager?utm_campaign=Solution-Switcher&utm_source=product&utm_medium=sbom-manager',
  },
  guide: {
    displayName: 'AI Developer',
    iconLight: guideLight,
    iconDark: guideDark,
    marketingUrl:
      'https://www.sonatype.com/products/sonatype-guide?utm_campaign=Solution-Switcher&utm_source=product&utm_medium=guide',
  },
};

export interface ExploreProduct {
  id: SolutionId;
  displayName: string;
  url: string;
}

const isKnownSolutionId = (id: string): id is SolutionId =>
  Object.prototype.hasOwnProperty.call(PRODUCT_METADATA, id);

export function groupAndSortLicensedSolutions(
  solutions: LicensedSolution[],
): LicensedProduct[] {
  const grouped = new Map<SolutionId, LicensedSolution[]>();
  for (const s of solutions) {
    if (!isKnownSolutionId(s.id)) continue;
    const list = grouped.get(s.id) ?? [];
    list.push(s);
    grouped.set(s.id, list);
  }

  const products: LicensedProduct[] = [];
  for (const [id, entries] of grouped) {
    const displayName = PRODUCT_METADATA[id].displayName;
    if (entries.length === 1) {
      products.push({ id, displayName, url: entries[0].url });
    } else {
      products.push({
        id,
        displayName,
        instances: entries.map((e) => ({ url: e.url })),
      });
    }
  }

  products.sort((a, b) => a.displayName.localeCompare(b.displayName));
  return products;
}

export function getExploreProducts(licensed: LicensedProduct[]): ExploreProduct[] {
  const licensedIds = new Set(licensed.map((p) => p.id));
  const explore: ExploreProduct[] = [];
  for (const id of Object.keys(PRODUCT_METADATA) as SolutionId[]) {
    if (licensedIds.has(id)) continue;
    const { displayName, marketingUrl } = PRODUCT_METADATA[id];
    if (!marketingUrl) continue;
    explore.push({ id, displayName, url: marketingUrl });
  }
  explore.sort((a, b) => a.displayName.localeCompare(b.displayName));
  return explore;
}
