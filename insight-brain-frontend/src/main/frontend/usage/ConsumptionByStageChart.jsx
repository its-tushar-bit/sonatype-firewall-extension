/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useMemo } from 'react';
import * as PropTypes from 'prop-types';
import { ResponsivePie } from '@nivo/pie';
import { NxH2, NxTile } from '@sonatype/react-shared-components';

import { STAGE_COLORS, FALLBACK_COLOR } from './usageChartPalette';
import { formatNumber } from './usageFormatters';

// Canonical SDLC phase order. Anything not in this list comes after, in
// alphabetical order, with `Unknown` always last (data-quality bucket).
export const STAGE_CANONICAL_ORDER = ['develop', 'build', 'stage-release', 'release', 'operate'];

function canonicalIndex(token) {
  const i = STAGE_CANONICAL_ORDER.indexOf(token);
  return i === -1 ? Number.MAX_SAFE_INTEGER : i;
}

// The literal "Unknown" must match ConsumptionEventDAO.STAGE_UNKNOWN (Java).
// If renamed there, update this map and STAGE_COLORS in usageChartPalette.js.
export const STAGE_LABELS = {
  build: 'Build',
  'stage-release': 'Stage Release',
  release: 'Release',
  operate: 'Operate',
  develop: 'Develop',
  'continuous-monitoring': 'Continuous Monitoring',
  proxy: 'Proxy',
  Unknown: 'Unknown',
};

function labelFor(token) {
  return STAGE_LABELS[token] || token;
}

function colorFor(token) {
  return STAGE_COLORS[token] || FALLBACK_COLOR;
}

function buildEntries(breakdown) {
  const raw = Object.entries(breakdown || {}).filter(([, count]) => typeof count === 'number' && count > 0);
  const total = raw.reduce((acc, [, count]) => acc + count, 0);
  if (total === 0) return { entries: [], total: 0 };
  const entries = raw
    .map(([token, count]) => ({
      token,
      label: labelFor(token),
      color: colorFor(token),
      count,
    }))
    .sort((a, b) => {
      if (a.token === 'Unknown') return 1;
      if (b.token === 'Unknown') return -1;
      const ai = canonicalIndex(a.token);
      const bi = canonicalIndex(b.token);
      if (ai !== bi) return ai - bi;
      // Both share a canonical index (i.e. both non-canonical, mapped to MAX_SAFE_INTEGER): sort by label.
      return a.label.localeCompare(b.label);
    });
  return { entries, total };
}

export default function ConsumptionByStageChart({ stageBreakdown }) {
  const { entries, total, chartData, maxCount } = useMemo(() => {
    if (!stageBreakdown || stageBreakdown.length === 0) {
      return { entries: [], total: 0, chartData: [], maxCount: 0 };
    }
    const currentMonth = [...stageBreakdown].sort((a, b) => (b.month || '').localeCompare(a.month || ''))[0];
    if (!currentMonth || !currentMonth.breakdown) {
      return { entries: [], total: 0, chartData: [], maxCount: 0 };
    }
    const built = buildEntries(currentMonth.breakdown);
    const data = built.entries.map(({ token, label, count, color }) => ({
      id: token,
      label,
      value: count,
      color,
    }));
    return {
      entries: built.entries,
      total: built.total,
      chartData: data,
      // Must use the true max — canonical-phase sort puts e.g. `develop` first
      // even when `build` has a higher count, so `entries[0].count` would
      // understate the max and break the legend bar-width proportions.
      maxCount: built.entries.reduce((m, e) => Math.max(m, e.count), 0),
    };
  }, [stageBreakdown]);

  if (entries.length === 0) {
    return null;
  }

  const centerLayer = ({ centerX, centerY }) => (
    <g>
      <text
        x={centerX}
        y={centerY - 10}
        textAnchor="middle"
        dominantBaseline="central"
        className="iq-usage-stage-chart__center-total"
      >
        {formatNumber(total)}
      </text>
      <text
        x={centerX}
        y={centerY + 14}
        textAnchor="middle"
        dominantBaseline="central"
        className="iq-usage-stage-chart__center-label"
      >
        Evaluations
      </text>
    </g>
  );

  return (
    <NxTile className="iq-usage-stage-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>Consumption by Stage</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <div className="iq-usage-stage-chart">
          <div className="iq-usage-stage-chart__donut">
            <ResponsivePie
              data={chartData}
              innerRadius={0.72}
              padAngle={1}
              cornerRadius={2}
              colors={(d) => d.data.color}
              enableArcLinkLabels={false}
              enableArcLabels={false}
              activeOuterRadiusOffset={4}
              layers={['arcs', centerLayer]}
              margin={{ top: 10, right: 10, bottom: 10, left: 10 }}
              borderWidth={2}
              borderColor="var(--nx-color-component-background)"
              tooltip={({ datum }) => {
                // Per the Mateo Figma: legend rows show count only; donut hover
                // adds the percentage. total > 0 is guaranteed inside this branch
                // (the entries-empty path returns null above).
                const percent = Math.round((datum.value / total) * 100);
                return (
                  <div className="iq-usage-chart__tooltip">
                    <strong>{datum.label}</strong>: {formatNumber(datum.value)} ({percent}%)
                  </div>
                );
              }}
            />
          </div>
          <ul className="iq-usage-stage-chart__legend" role="list">
            {entries.map((entry) => (
              <li key={entry.token} className="iq-usage-stage-chart__legend-row">
                <span
                  className="iq-usage-stage-chart__swatch"
                  style={{ backgroundColor: entry.color }}
                  aria-hidden="true"
                />
                <span className="iq-usage-stage-chart__label" title={entry.label}>
                  {entry.label}
                </span>
                <span className="iq-usage-stage-chart__bar-track" aria-hidden="true">
                  <span
                    className="iq-usage-stage-chart__bar-fill"
                    style={{
                      width: maxCount > 0 ? `${(entry.count / maxCount) * 100}%` : '0%',
                      backgroundColor: entry.color,
                    }}
                  />
                </span>
                <span className="iq-usage-stage-chart__count">{formatNumber(entry.count)}</span>
              </li>
            ))}
          </ul>
        </div>
      </NxTile.Content>
    </NxTile>
  );
}

ConsumptionByStageChart.propTypes = {
  stageBreakdown: PropTypes.arrayOf(
    PropTypes.shape({
      month: PropTypes.string.isRequired,
      consumed: PropTypes.number.isRequired,
      breakdown: PropTypes.object.isRequired,
    })
  ),
};
