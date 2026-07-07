/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  dashboardApplicationsHref,
  dashboardComponentsHref,
  dashboardViolationsHref,
  dashboardWaiversHref,
} from 'MainRoot/nosc/dashboard/dashboardBundleUrls';
import type { DashboardMetricsResponse } from './dashboardMetricsTypes';
import type { SubMetric } from './MetricCard';

/**
 * Card registry for the preview dashboard metric grid (CLM-40905).
 *
 * The grid renders whatever this ordered list declares — adding a future card
 * (e.g. Components when the backend ships it) is registration-only: append a
 * definition here and the grid + hook need no changes. Each definition decides:
 *   - `isAvailable`: whether the metric is present in the response (so an absent
 *     `components` block is skipped rather than crashing the grid).
 *   - `showWhileLoading`: whether to reserve a skeleton card before the response
 *     (true for the always-present core metrics; false for optional ones whose
 *     presence is unknown until data arrives).
 *   - `select`: map the response to the card's value / breakdown / click-through.
 */

export interface MetricCardSelection {
  readonly value: number;
  readonly subMetrics?: readonly SubMetric[];
  readonly href?: string;
}

export interface MetricCardDefinition {
  readonly id: string;
  readonly title: string;
  readonly testId: string;
  readonly showWhileLoading: boolean;
  readonly isAvailable: (data: DashboardMetricsResponse) => boolean;
  readonly select: (data: DashboardMetricsResponse) => MetricCardSelection;
}

export const METRIC_CARD_DEFINITIONS: readonly MetricCardDefinition[] = [
  {
    id: 'applications',
    title: 'Applications',
    testId: 'metric-card-applications',
    showWhileLoading: true,
    isAvailable: (data) => data.applications != null,
    select: (data) => ({
      value: data.applications?.total ?? 0,
      href: dashboardApplicationsHref(),
    }),
  },
  {
    id: 'violations',
    title: 'Policy Violations',
    testId: 'metric-card-violations',
    showWhileLoading: true,
    isAvailable: (data) => data.violations != null,
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
    id: 'waivers',
    title: 'Waivers',
    testId: 'metric-card-waivers',
    showWhileLoading: true,
    isAvailable: (data) => data.waivers != null,
    select: (data) => {
      const b = data.waivers?.breakdown;
      const subMetrics: SubMetric[] | undefined = b
        ? [
            { label: 'Existing', value: b.existing, tone: 'neutral' },
            { label: 'Requested', value: b.requested, tone: 'neutral' },
          ]
        : undefined;
      return {
        value: data.waivers?.total ?? 0,
        subMetrics,
        href: dashboardWaiversHref(),
      };
    },
  },
  {
    // Optional: the backend may not ship `components` yet. Not reserved during
    // loading (presence unknown); rendered only once it appears in the response.
    id: 'components',
    title: 'Components',
    testId: 'metric-card-components',
    showWhileLoading: false,
    isAvailable: (data) => data.components != null,
    select: (data) => ({
      value: data.components?.total ?? 0,
      href: dashboardComponentsHref(),
    }),
  },
];
