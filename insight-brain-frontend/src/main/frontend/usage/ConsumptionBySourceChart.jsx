/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useMemo } from 'react';
import * as PropTypes from 'prop-types';
import { ResponsivePie } from '@nivo/pie';
import { NxH2, NxTile } from '@sonatype/react-shared-components';

import { SOURCE_COLORS, FALLBACK_COLOR } from './usageChartPalette';
import { formatNumber } from './usageFormatters';

export const SOURCE_LABELS = {
  UI: 'UI',
  CLI: 'CLI',
  CI_CD: 'CI/CD',
  IDE: 'IDE',
  REPO_MANAGER: 'Repository Manager',
  API: 'API',
  CONTINUOUS_MONITOR: 'Continuous Monitoring',
  UNKNOWN: 'Unknown',
};

// Re-exports for backward compatibility with existing tests.
export { SOURCE_COLORS, FALLBACK_COLOR };

function labelFor(token) {
  return SOURCE_LABELS[token] || token;
}

function colorFor(token) {
  return SOURCE_COLORS[token] || FALLBACK_COLOR;
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
      if (b.count !== a.count) return b.count - a.count;
      return a.label.localeCompare(b.label);
    });

  return { entries, total };
}

export default function ConsumptionBySourceChart({ sourceBreakdown }) {
  const { entries, total, chartData, maxCount } = useMemo(() => {
    if (!sourceBreakdown || sourceBreakdown.length === 0) {
      return { entries: [], total: 0, chartData: [], maxCount: 0 };
    }
    const currentMonth = [...sourceBreakdown].sort((a, b) => (b.month || '').localeCompare(a.month || ''))[0];
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
      // Use reduce(Math.max) — entries[0].count was correct only because Source sorts
      // by count desc; this is robust to any future sort-order change and mirrors the
      // pattern in ConsumptionByStageChart.jsx where canonical-stage sort exposed the
      // entries[0]-as-max bug.
      maxCount: built.entries.reduce((m, e) => Math.max(m, e.count), 0),
    };
  }, [sourceBreakdown]);

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
        className="iq-usage-source-chart__center-total"
      >
        {formatNumber(total)}
      </text>
      <text
        x={centerX}
        y={centerY + 14}
        textAnchor="middle"
        dominantBaseline="central"
        className="iq-usage-source-chart__center-label"
      >
        Evaluations
      </text>
    </g>
  );

  return (
    <NxTile className="iq-usage-source-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>Consumption by Source</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <div className="iq-usage-source-chart">
          <div className="iq-usage-source-chart__donut">
            <ResponsivePie
              data={chartData}
              innerRadius={0.72}
              padAngle={1}
              cornerRadius={2}
              colors={(d) => d.data.color}
              borderWidth={2}
              borderColor="var(--nx-color-component-background)"
              enableArcLinkLabels={false}
              enableArcLabels={false}
              activeOuterRadiusOffset={4}
              layers={['arcs', centerLayer]}
              margin={{ top: 10, right: 10, bottom: 10, left: 10 }}
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
          <ul className="iq-usage-source-chart__legend" role="list">
            {entries.map((entry) => (
              <li key={entry.token} className="iq-usage-source-chart__legend-row">
                <span
                  className="iq-usage-source-chart__swatch"
                  style={{ backgroundColor: entry.color }}
                  aria-hidden="true"
                />
                <span className="iq-usage-source-chart__label" title={entry.label}>
                  {entry.label}
                </span>
                <span className="iq-usage-source-chart__bar-track" aria-hidden="true">
                  <span
                    className="iq-usage-source-chart__bar-fill"
                    style={{
                      width: maxCount > 0 ? `${(entry.count / maxCount) * 100}%` : '0%',
                      backgroundColor: entry.color,
                    }}
                  />
                </span>
                <span className="iq-usage-source-chart__count">{formatNumber(entry.count)}</span>
              </li>
            ))}
          </ul>
        </div>
      </NxTile.Content>
    </NxTile>
  );
}

ConsumptionBySourceChart.propTypes = {
  sourceBreakdown: PropTypes.arrayOf(
    PropTypes.shape({
      month: PropTypes.string.isRequired,
      consumed: PropTypes.number.isRequired,
      breakdown: PropTypes.object.isRequired,
    })
  ),
};
