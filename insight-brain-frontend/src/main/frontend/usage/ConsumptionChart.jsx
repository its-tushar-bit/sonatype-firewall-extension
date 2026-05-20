/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxH3, NxTile, NxFormSelect } from '@sonatype/react-shared-components';
import {
  AreaChart,
  Area,
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

function formatPeriodLabel(dateStr, aggregation) {
  if (!dateStr) return '';
  if (aggregation === 'monthly') {
    return moment(dateStr).format('MMM YYYY');
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

function formatCount(value) {
  if (value >= 1000) {
    return `${(value / 1000).toFixed(0)}k`;
  }
  return String(value);
}

const ACTIVITY_KEYS = [
  'App Scan + Re-evaluate',
  'Continuous Monitoring',
  'Component Details',
  'Version Recommendations',
  'APIs',
  'Reachability Analysis',
];

// Palette tuned to match the approved mockup. Weights are bumped to 60/70
// for softer saturation (NX's 50 weight is max-saturated and reads too
// vivid). Indigo + purple pair gives the blue/violet family the mockup
// shows for App Scan and Component Details.
const ACTIVITY_COLORS = {
  'App Scan + Re-evaluate': 'var(--nx-swatch-indigo-50)',
  'Continuous Monitoring': 'var(--nx-swatch-green-60)',
  'Component Details': 'var(--nx-swatch-purple-60)',
  'Version Recommendations': 'var(--nx-swatch-orange-60)',
  APIs: 'var(--nx-swatch-blue-70)',
  'Reachability Analysis': 'var(--nx-swatch-red-60)',
};

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

  return (
    <NxTile className="iq-usage-chart-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH3>Consumption by Type</NxH3>
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
            <AreaChart data={chartData} margin={{ top: 10, right: 20, left: 0, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="period" />
              <YAxis tickFormatter={formatCount} />
              <Tooltip
                formatter={(value, name) => [value.toLocaleString(), name]}
                labelFormatter={(label, payload) => {
                  const isoDate = payload && payload[0] && payload[0].payload && payload[0].payload.isoDate;
                  return formatTooltipLabel(isoDate, aggregation) || label;
                }}
                contentStyle={{
                  backgroundColor: 'var(--nx-color-component-background)',
                  border: '1px solid var(--nx-color-border)',
                  borderRadius: '4px',
                  color: 'var(--nx-color-text)',
                }}
              />
              <Legend />
              {ACTIVITY_KEYS.map((key) => (
                <Area
                  key={key}
                  type="monotone"
                  dataKey={key}
                  stackId="1"
                  stroke="none"
                  fill={ACTIVITY_COLORS[key]}
                  fillOpacity={0.6}
                />
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
            </AreaChart>
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
