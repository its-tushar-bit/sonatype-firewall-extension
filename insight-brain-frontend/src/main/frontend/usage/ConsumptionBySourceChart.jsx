/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useMemo, useState } from 'react';
import * as PropTypes from 'prop-types';
import { ResponsivePie } from '@nivo/pie';
import { NxH3, NxTextLink, NxTile } from '@sonatype/react-shared-components';

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

// Palette tuned to match the approved mockup. Three-blue family ordered
// by darkness (CI/CD → IDE → CLI) so slices read as a visually coherent
// group while still separating cleanly.
export const SOURCE_COLORS = {
  CI_CD: 'var(--nx-swatch-indigo-50)',
  IDE: 'var(--nx-swatch-blue-70)',
  CLI: 'var(--nx-swatch-indigo-30)',
  REPO_MANAGER: 'var(--nx-swatch-green-60)',
  CONTINUOUS_MONITOR: 'var(--nx-swatch-orange-60)',
  UI: 'var(--nx-swatch-purple-60)',
  API: 'var(--nx-swatch-yellow-60)',
  UNKNOWN: 'var(--nx-swatch-grey-60)',
};

export const FALLBACK_COLOR = 'var(--nx-swatch-grey-60)';

const TOP_N_VISIBLE = 5;

function labelFor(token) {
  return SOURCE_LABELS[token] || token;
}

function colorFor(token) {
  return SOURCE_COLORS[token] || FALLBACK_COLOR;
}

function buildEntries(breakdown) {
  const raw = Object.entries(breakdown || {})
    .filter(([, count]) => typeof count === 'number' && count > 0);

  const total = raw.reduce((acc, [, count]) => acc + count, 0);
  if (total === 0) return { entries: [], total: 0 };

  const entries = raw
    .map(([token, count]) => ({
      token,
      label: labelFor(token),
      color: colorFor(token),
      count,
      percent: (count / total) * 100,
    }))
    .sort((a, b) => {
      if (b.count !== a.count) return b.count - a.count;
      return a.label.localeCompare(b.label);
    });

  return { entries, total };
}

function formatPercent(pct) {
  return `${Math.round(pct)}%`;
}

export default function ConsumptionBySourceChart({ sourceBreakdown }) {
  const [expanded, setExpanded] = useState(false);

  const { entries, total, chartData, maxCount } = useMemo(() => {
    if (!sourceBreakdown || sourceBreakdown.length === 0) {
      return { entries: [], total: 0, chartData: [], maxCount: 0 };
    }
    const currentMonth = [...sourceBreakdown].sort((a, b) => (b.month || '').localeCompare(a.month || ''))[0];
    if (!currentMonth || !currentMonth.breakdown) {
      return { entries: [], total: 0, chartData: [], maxCount: 0 };
    }
    const { entries, total } = buildEntries(currentMonth.breakdown);
    const chartData = entries.map(({ token, label, count, color }) => ({
      id: token,
      label,
      value: count,
      color,
    }));
    const maxCount = entries.length > 0 ? entries[0].count : 0;
    return { entries, total, chartData, maxCount };
  }, [sourceBreakdown]);

  if (entries.length === 0) {
    return null;
  }

  const overflowCount = Math.max(0, entries.length - TOP_N_VISIBLE);
  const visibleEntries = expanded ? entries : entries.slice(0, TOP_N_VISIBLE);

  const centerLayer = ({ centerX, centerY }) => (
    <g>
      <text
        x={centerX}
        y={centerY - 10}
        textAnchor="middle"
        dominantBaseline="central"
        className="iq-usage-source-chart__center-total"
      >
        {total.toLocaleString()}
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
          <NxH3>Consumption by Source</NxH3>
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
              enableArcLinkLabels={false}
              enableArcLabels={false}
              activeOuterRadiusOffset={4}
              layers={['arcs', centerLayer]}
              margin={{ top: 10, right: 10, bottom: 10, left: 10 }}
              tooltip={({ datum }) => (
                <div className="iq-usage-chart__tooltip">
                  <strong>{datum.label}</strong>: {datum.value.toLocaleString()}
                </div>
              )}
            />
          </div>
          <ul className="iq-usage-source-chart__legend" role="list">
            {visibleEntries.map((entry) => (
              <li key={entry.token} className="iq-usage-source-chart__legend-row">
                <span
                  className="iq-usage-source-chart__swatch"
                  style={{ backgroundColor: entry.color }}
                  aria-hidden="true"
                />
                <span
                  className="iq-usage-source-chart__label"
                  title={entry.label}
                >
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
                <span className="iq-usage-source-chart__count">
                  {entry.count.toLocaleString()}
                </span>
                <span className="iq-usage-source-chart__percent">
                  {formatPercent(entry.percent)}
                </span>
              </li>
            ))}
            {overflowCount > 0 && (
              <li className="iq-usage-source-chart__legend-more">
                <NxTextLink onClick={() => setExpanded((prev) => !prev)}>
                  {expanded ? 'Show fewer' : `More sources (${overflowCount})`}
                </NxTextLink>
              </li>
            )}
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
