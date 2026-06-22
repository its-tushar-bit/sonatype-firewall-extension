/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';
import { NxH2, NxTile } from '@sonatype/react-shared-components';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import moment from 'moment';

import { formatNumber } from './usageFormatters';
import {
  selectCumulativeChartSeries,
  selectCumulativeFilter,
  selectDailyHistory,
  selectSummary,
} from './usageSelectors';
import CumulativeChartFilter from './CumulativeChartFilter';

function formatCount(value) {
  if (value >= 1000) {
    return `${(value / 1000).toFixed(0)}k`;
  }
  return String(value);
}

export default function EvaluatedComponentsTile() {
  const entries = useSelector(selectCumulativeChartSeries);
  const filter = useSelector(selectCumulativeFilter);
  const summary = useSelector(selectSummary);
  // dailyAverage and peakDay always reflect the current billing month, even when
  // the chart filter is last3Months / last6Months. The "(this month)" qualifier
  // on each label keeps that scope explicit so users don't read these widgets as
  // describing the same window the chart is rendering.
  const dailyHistoryData = useSelector(selectDailyHistory);
  const dailyAverage = dailyHistoryData?.dailyAverage ?? 0;
  const peakDay = dailyHistoryData?.peakDay ?? null;

  // Granularity drives both label format and chart shape. Derived from the
  // filter explicitly — earlier code heuristic'd on `moment.date() === 1` to
  // detect month buckets, which broke on the 1st of every month when daily
  // entries naturally include that date.
  const isMonthly = filter !== 'thisMonth';
  const formatDateLabel = (dateStr) => {
    if (!dateStr) return '';
    return moment(dateStr).format(isMonthly ? 'MMM YYYY' : 'MMM D');
  };

  // For thisMonth (daily) the API exposes a real billing-period cumulative,
  // so we render a stacked bar: priorCumulative + today's components. For
  // last3/last6 the selector's `componentsCumulative` is only a sum within
  // the selected window (no pre-window baseline available), which silently
  // shifts the Y-axis baseline between filters. To avoid presenting that as a
  // "cumulative" view, render plain monthly Usage bars (single series, no
  // stacked priorCumulative segment) for those filters — the legend then
  // honestly says "Monthly Usage".
  const stackedEntries = isMonthly
    ? entries
    : entries.map((entry) => ({
        ...entry,
        priorCumulative: Math.max(0, (entry.componentsCumulative || 0) - (entry.components || 0)),
      }));

  const hasEntries = entries && entries.length > 0;
  const hasAverage = typeof dailyAverage === 'number' && dailyAverage > 0;
  const hasPeak = peakDay && typeof peakDay.count === 'number' && peakDay.count > 0;
  if (!hasEntries && !hasAverage && !hasPeak) {
    return null;
  }

  const totalEvaluated = summary?.consumed ?? 0;

  return (
    <NxTile className="iq-usage-evaluated-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>Cumulative Components Evaluated</NxH2>
        </NxTile.HeaderTitle>
        <NxTile.HeaderActions>
          <CumulativeChartFilter />
        </NxTile.HeaderActions>
      </NxTile.Header>
      <NxTile.Content>
        <div className="iq-usage-insights-grid">
          <div className="iq-usage-insights-widget">
            <span className="iq-usage-insights-widget__label">Total Components Evaluated</span>
            <span className="iq-usage-insights-widget__value">{formatNumber(totalEvaluated)}</span>
          </div>
          <div className="iq-usage-insights-widget">
            <span className="iq-usage-insights-widget__label">Daily Average (this month)</span>
            <span className="iq-usage-insights-widget__value">{formatNumber(Math.round(dailyAverage))}</span>
          </div>
          <div className="iq-usage-insights-widget">
            <span className="iq-usage-insights-widget__label">Peak Day (this month)</span>
            <span className="iq-usage-insights-widget__value">{peakDay ? formatNumber(peakDay.count) : '0'}</span>
          </div>
        </div>
        {hasEntries && (
          <div
            className="iq-usage-trend-chart"
            role="img"
            aria-label={
              isMonthly
                ? 'Monthly components evaluated chart'
                : 'Cumulative components evaluated chart with daily usage and cumulative usage series'
            }
          >
            <ResponsiveContainer width="100%" height={250}>
              <BarChart data={stackedEntries} margin={{ top: 10, right: 20, left: 0, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="date" tickFormatter={formatDateLabel} />
                <YAxis tickFormatter={formatCount} />
                <Tooltip
                  labelFormatter={formatDateLabel}
                  formatter={(value) => value.toLocaleString('en-US')}
                  contentStyle={{
                    backgroundColor: 'var(--nx-color-component-background)',
                    border: '1px solid var(--nx-color-border)',
                    borderRadius: '4px',
                    color: 'var(--nx-color-text)',
                  }}
                />
                <Legend />
                {/* Bar `name=` props are the display labels — Recharts surfaces them to
                    both the Tooltip and Legend without a translation layer. */}
                {!isMonthly && (
                  <Bar
                    dataKey="priorCumulative"
                    name="Cumulative Usage"
                    stackId="cumulative"
                    fill="var(--nx-swatch-blue-50)"
                  />
                )}
                <Bar
                  dataKey="components"
                  name={isMonthly ? 'Monthly Usage' : 'Daily Usage'}
                  stackId="cumulative"
                  fill="var(--nx-swatch-blue-70)"
                />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </NxTile.Content>
    </NxTile>
  );
}
