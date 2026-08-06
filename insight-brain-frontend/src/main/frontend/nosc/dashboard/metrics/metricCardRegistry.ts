/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  dashboardApplicationsHref,
  dashboardComponentsHref,
  dashboardLegalHref,
  dashboardOrgsAndPoliciesHref,
  dashboardViolationsHref,
  dashboardVulnerabilitiesHref,
  dashboardWaiversHref,
} from 'MainRoot/nosc/dashboard/dashboardBundleUrls';
import type { DashboardMetricsResponse, MetricEntry } from './dashboardMetricsTypes';
import type { DualHeroStat, RailTone, SecondaryStat, SecondaryStatPresentation, SubMetric } from './MetricCard';

/**
 * Card registry for the preview dashboard metric grid (CLM-40905).
 *
 * Ordered alphabetically by card title to match the Nexus One prototype.
 * The grid renders whatever this ordered list declares — adding a future card
 * is registration-only: append a definition here and the grid + hook need no changes.
 */

function isPresentMetric(entry: MetricEntry | undefined): boolean {
  return entry != null && entry.errorCode !== 'METRIC_UNAVAILABLE';
}

export interface MetricCardSelection {
  readonly value?: number;
  readonly subMetrics?: readonly SubMetric[];
  readonly dualHero?: readonly [DualHeroStat, DualHeroStat];
  readonly secondaryStat?: SecondaryStat;
  readonly href?: string;
}

/** Runtime context for card selection (license gates that affect deep-dive hrefs). */
export interface MetricCardSelectContext {
  readonly advancedLegalPack: boolean;
}

export interface MetricCardDefinition {
  readonly id: string;
  readonly title: string;
  readonly testId: string;
  /** Skeleton during the initial (summary) load before any metrics data arrives. */
  readonly showWhileLoading: boolean;
  /**
   * Skeleton while the heavy-tier POST is in flight after summary is ready.
   * Summary-only cards stay omitted when absent — they will not arrive on the heavy response.
   */
  readonly showWhileHeavyLoading: boolean;
  /** Domain rail tint for the card's left border (chrome, not a status signal). */
  readonly railTone?: RailTone;
  /** How to present the card's secondary stat, if any. Defaults to `text` in {@link MetricCard}. */
  readonly secondaryStatPresentation?: SecondaryStatPresentation;
  readonly isAvailable: (data: DashboardMetricsResponse) => boolean;
  readonly select: (
    data: DashboardMetricsResponse,
    context: MetricCardSelectContext,
  ) => MetricCardSelection;
}

export const METRIC_CARD_DEFINITIONS: readonly MetricCardDefinition[] = [
  {
    id: 'applications',
    title: 'Applications',
    testId: 'metric-card-applications',
    showWhileLoading: true,
    showWhileHeavyLoading: false,
    railTone: 'apps',
    secondaryStatPresentation: 'chip',
    isAvailable: (data) => isPresentMetric(data.applications),
    select: (data) => {
      const stages = data.applications?.breakdown?.stages;
      return {
        value: data.applications?.total ?? 0,
        secondaryStat: stages != null ? { value: stages, label: 'Stages' } : undefined,
        href: dashboardApplicationsHref(),
      };
    },
  },
  {
    id: 'legal',
    title: 'Legal Obligations',
    testId: 'metric-card-legal',
    showWhileLoading: false,
    showWhileHeavyLoading: true,
    railTone: 'legal',
    isAvailable: (data) => isPresentMetric(data.legal),
    select: (data, context) => {
      const b = data.legal?.breakdown;
      const dualHero: readonly [DualHeroStat, DualHeroStat] | undefined = b
        ? [
            {
              value: b.applications,
              label: b.applications === 1 ? 'Application' : 'Applications',
            },
            { value: b.components, label: b.components === 1 ? 'Component' : 'Components' },
          ]
        : undefined;
      return {
        // Dual-hero only when the countDistinct breakdown is present; otherwise
        // fall back to the headline total so the card never renders a bare "0".
        value: dualHero ? undefined : data.legal?.total ?? 0,
        dualHero,
        href: dashboardLegalHref(context.advancedLegalPack),
      };
    },
  },
  {
    id: 'orgsAndPolicies',
    title: 'Orgs and Policies',
    testId: 'metric-card-orgs-and-policies',
    showWhileLoading: false,
    showWhileHeavyLoading: false,
    railTone: 'orgs',
    isAvailable: (data) => isPresentMetric(data.organizations) && isPresentMetric(data.policies),
    select: (data) => ({
      dualHero: [
        { value: data.organizations?.total ?? 0, label: 'Organizations' },
        { value: data.policies?.total ?? 0, label: 'Policies' },
      ],
      href: dashboardOrgsAndPoliciesHref(),
    }),
  },
  {
    id: 'components',
    title: 'Scanned Components',
    testId: 'metric-card-components',
    showWhileLoading: false,
    showWhileHeavyLoading: true,
    railTone: 'components',
    isAvailable: (data) => isPresentMetric(data.components),
    select: (data) => {
      const relatedViolations = data.violations?.total;
      return {
        value: data.components?.total ?? 0,
        secondaryStat:
          relatedViolations != null ? { value: relatedViolations, label: 'Total Policy Violations' } : undefined,
        href: dashboardComponentsHref(),
      };
    },
  },
  {
    id: 'violations',
    title: 'Violations',
    testId: 'metric-card-violations',
    // Heavy-tier aggregate — skeleton until the second POST lands.
    showWhileLoading: false,
    showWhileHeavyLoading: true,
    railTone: 'violations',
    isAvailable: (data) => isPresentMetric(data.violations),
    select: (data) => {
      const b = data.violations?.breakdown;
      const subMetrics: SubMetric[] | undefined = b
        ? [
            { label: 'Critical', value: b.critical, tone: 'critical' },
            { label: 'Severe', value: b.severe, tone: 'severe' },
            { label: 'Moderate', value: b.moderate, tone: 'moderate' },
            { label: 'Low', value: b.low, tone: 'low' },
          ]
        : undefined;
      return {
        value: data.violations?.total ?? 0,
        subMetrics,
        href: dashboardViolationsHref(),
      };
    },
  },
  {
    id: 'vulnerabilities',
    title: 'Vulnerabilities',
    testId: 'metric-card-vulnerabilities',
    showWhileLoading: false,
    showWhileHeavyLoading: true,
    railTone: 'vulnerabilities',
    isAvailable: (data) => isPresentMetric(data.vulnerabilities),
    select: (data) => {
      const b = data.vulnerabilities?.breakdown;
      const subMetrics: SubMetric[] | undefined = b
        ? [
            { label: 'Critical', value: b.critical, tone: 'critical' },
            { label: 'High', value: b.high, tone: 'severe' },
            { label: 'Medium', value: b.medium, tone: 'moderate' },
            { label: 'Low', value: b.low, tone: 'low' },
          ]
        : undefined;
      return {
        value: data.vulnerabilities?.total ?? 0,
        subMetrics,
        href: dashboardVulnerabilitiesHref(),
      };
    },
  },
  {
    id: 'waivers',
    title: 'Waivers',
    testId: 'metric-card-waivers',
    showWhileLoading: true,
    showWhileHeavyLoading: false,
    railTone: 'waivers',
    isAvailable: (data) => isPresentMetric(data.waivers),
    select: (data) => {
      const b = data.waivers?.breakdown;
      let subMetrics: SubMetric[] | undefined;
      if (b) {
        subMetrics = [
          { label: 'Existing Waivers', value: b.existing, variant: 'stat' },
          { label: 'Requested Waivers', value: b.requested, variant: 'stat' },
        ];
        if (typeof b.expiring === 'number') {
          subMetrics.push({
            label: 'Expiring (30d)',
            value: b.expiring,
            variant: 'stat',
          });
        }
      }
      return {
        value: data.waivers?.total ?? 0,
        subMetrics,
        href: dashboardWaiversHref(),
      };
    },
  },
];
