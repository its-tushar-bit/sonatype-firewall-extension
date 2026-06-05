/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxH2, NxTile, NxFormSelect } from '@sonatype/react-shared-components';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ReferenceLine,
  ReferenceArea,
  Label,
  ResponsiveContainer,
} from 'recharts';
import moment from 'moment';

import { ACTIVITY_COLORS, FALLBACK_COLOR } from './usageChartPalette';
import { formatCount, formatNumber } from './usageFormatters';

function formatPeriodLabel(dateStr, aggregation) {
  if (!dateStr) return '';
  if (aggregation === 'monthly') {
    return moment(dateStr).format('MMM YYYY');
  }
  if (aggregation === 'weekly') {
    // The backend labels each weekly bucket by its week-start date. We
    // display the week-end (week-start + 6) so the rightmost bar reads as
    // "the week of <date near today>" instead of a date a week ago.
    return moment(dateStr).add(6, 'days').format('MMM D');
  }
  return moment(dateStr).format('MMM D');
}

function formatTooltipLabel(dateStr, aggregation) {
  if (!dateStr) return '';
  if (aggregation === 'monthly') {
    return moment(dateStr).format('MMMM YYYY');
  }
  if (aggregation === 'weekly') {
    const start = moment(dateStr);
    const end = start.clone().add(6, 'days');
    return `${start.format('MMM D')} – ${end.format('MMM D, YYYY')}`;
  }
  return moment(dateStr).format('MMM D, YYYY');
}

const ACTIVITY_KEYS = [
  'App Scan + Re-evaluate',
  'Continuous Monitoring',
  'Component Details',
  'Version Recommendations',
  'APIs',
  'Reachability Analysis',
];

function colorFor(key) {
  return ACTIVITY_COLORS[key] || FALLBACK_COLOR;
}

/**
 * Custom recharts <Tooltip content> renderer. Filters payload entries with
 * value=0 so the tooltip only shows metrics with actual activity. Lays out
 * each row as `label: count (pct%)` per the approved mockup, with the
 * monthly limit shown in the footer when known. Exported for unit testing.
 */
export function renderTooltipContent({ active, payload, label, monthlyLimit }) {
  if (!active || !payload) return null;
  const nonZero = payload.filter((entry) => entry.value);
  const total = nonZero.reduce((sum, e) => sum + (e.value || 0), 0);
  const formatPct = (v) => (total > 0 ? `${Math.round((v / total) * 100)}%` : '0%');
  return (
    <div className="iq-usage-chart__tooltip">
      <div className="iq-usage-chart__tooltip-label">{label}</div>
      {nonZero.length === 0 ? (
        <div className="iq-usage-chart__tooltip-empty">No activity</div>
      ) : (
        <>
          <div className="iq-usage-chart__tooltip-total">{`Total: ${formatNumber(total)} components`}</div>
          <ul className="iq-usage-chart__tooltip-list">
            {nonZero.map((entry) => (
              <li key={entry.name} className="iq-usage-chart__tooltip-row">
                <span
                  className="iq-usage-chart__tooltip-swatch"
                  style={{ backgroundColor: entry.color }}
                  aria-hidden="true"
                />
                <span className="iq-usage-chart__tooltip-name">{entry.name}:</span>
                <span className="iq-usage-chart__tooltip-value">
                  {formatNumber(entry.value)} ({formatPct(entry.value)})
                </span>
              </li>
            ))}
          </ul>
          {typeof monthlyLimit === 'number' && monthlyLimit > 0 && (
            <div className="iq-usage-chart__tooltip-footer">{`Monthly Limit: ${formatNumber(monthlyLimit)}`}</div>
          )}
        </>
      )}
    </div>
  );
}

/**
 * Custom legend renderer for the stacked bar chart. Renders colored swatches
 * with dark text labels instead of Recharts' default colored text.
 */
function renderLegendContent({ payload }) {
  return (
    <ul className="iq-usage-chart-legend" role="list">
      {payload.map((entry) => (
        <li key={entry.value} className="iq-usage-chart-legend__item">
          <span className="iq-usage-chart-legend__swatch" style={{ backgroundColor: entry.color }} aria-hidden="true" />
          <span className="iq-usage-chart-legend__label">{entry.value}</span>
        </li>
      ))}
    </ul>
  );
}

export default function ConsumptionChart({ historyBreakdown, aggregation, onAggregationChange, monthlyLimit }) {
  if (!historyBreakdown || historyBreakdown.length === 0) {
    return null;
  }

  // Backend sends entries in ascending date order (oldest first). The chart plots in
  // array order (leftmost = first), so no reversal is needed here.
  const chartData = historyBreakdown.map((entry) => {
    const row = {
      period: formatPeriodLabel(entry.month, aggregation),
      isoDate: entry.month,
    };
    for (const key of ACTIVITY_KEYS) {
      row[key] = (entry.breakdown && entry.breakdown[key]) || 0;
    }
    return row;
  });

  const hasLimit = typeof monthlyLimit === 'number' && monthlyLimit > 0;
  const maxStackedTotal = chartData.reduce((max, row) => {
    const rowTotal = ACTIVITY_KEYS.reduce((sum, key) => sum + (row[key] || 0), 0);
    return rowTotal > max ? rowTotal : max;
  }, 0);
  const isOverLimit = hasLimit && maxStackedTotal > monthlyLimit;
  const overageCeiling = isOverLimit ? maxStackedTotal * 1.05 : 0;

  // Wrap renderTooltipContent so it has access to `aggregation` for the
  // human-readable date and `monthlyLimit` for the footer line.
  const tooltipContentWithLabel = ({ active, payload, label }) => {
    const isoDate = payload && payload[0] && payload[0].payload && payload[0].payload.isoDate;
    const niceLabel = formatTooltipLabel(isoDate, aggregation) || label;
    return renderTooltipContent({ active, payload, label: niceLabel, monthlyLimit });
  };

  return (
    <NxTile className="iq-usage-chart-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>Consumption by Type</NxH2>
        </NxTile.HeaderTitle>
        <NxTile.HeaderActions>
          <NxFormSelect
            className="iq-usage-chart__aggregation-select"
            value={aggregation}
            onChange={onAggregationChange}
          >
            <option value="daily">Daily</option>
            <option value="weekly">Weekly</option>
            <option value="monthly">Monthly</option>
          </NxFormSelect>
        </NxTile.HeaderActions>
      </NxTile.Header>
      <NxTile.Content>
        <div className="iq-usage-chart__wrapper">
          <ResponsiveContainer width="100%" height={400}>
            <BarChart data={chartData} margin={{ top: 10, right: 20, left: 0, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="period" />
              <YAxis tickFormatter={formatCount} />
              <Tooltip content={tooltipContentWithLabel} wrapperStyle={{ outline: 'none' }} />
              <Legend content={renderLegendContent} />
              {ACTIVITY_KEYS.map((key) => (
                <Bar key={key} dataKey={key} stackId="1" fill={colorFor(key)} fillOpacity={1} />
              ))}
              {isOverLimit && (
                <ReferenceArea
                  y1={monthlyLimit}
                  y2={overageCeiling}
                  fill="var(--nx-swatch-red-60)"
                  fillOpacity={0.08}
                  ifOverflow="extendDomain"
                />
              )}
              {hasLimit && (
                <ReferenceLine
                  y={monthlyLimit}
                  stroke="var(--nx-swatch-red-60)"
                  strokeDasharray="4 4"
                  strokeWidth={isOverLimit ? 2 : 1.5}
                  strokeOpacity={isOverLimit ? 1 : 0.7}
                  ifOverflow="visible"
                >
                  <Label
                    value={`Limit: ${formatCount(monthlyLimit)}`}
                    position="insideTopRight"
                    fill="var(--nx-swatch-red-60)"
                  />
                </ReferenceLine>
              )}
            </BarChart>
          </ResponsiveContainer>
        </div>
      </NxTile.Content>
    </NxTile>
  );
}

ConsumptionChart.propTypes = {
  historyBreakdown: PropTypes.arrayOf(
    PropTypes.shape({
      month: PropTypes.string.isRequired,
      consumed: PropTypes.number.isRequired,
      breakdown: PropTypes.object.isRequired,
    })
  ),
  aggregation: PropTypes.oneOf(['daily', 'weekly', 'monthly']),
  onAggregationChange: PropTypes.func,
  monthlyLimit: PropTypes.number,
};
