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
import { formatNumber, formatPercent } from './usageFormatters';

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
      percent: (count / total) * 100,
    }))
    .sort((a, b) => {
      if (b.count !== a.count) return b.count - a.count;
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
      maxCount: built.entries[0]?.count ?? 0,
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
              tooltip={({ datum }) => (
                <div className="iq-usage-chart__tooltip">
                  <strong>{datum.label}</strong>: {formatNumber(datum.value)}
                </div>
              )}
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
                <span className="iq-usage-stage-chart__percent">{formatPercent(entry.percent)}</span>
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
