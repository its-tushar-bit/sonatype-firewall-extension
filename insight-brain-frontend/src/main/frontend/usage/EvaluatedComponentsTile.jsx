/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxH3, NxTile } from '@sonatype/react-shared-components';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import moment from 'moment';

function formatNumber(num) {
  if (num === null || num === undefined) {
    return '0';
  }
  return num.toLocaleString();
}

function formatDayLabel(dateStr) {
  if (!dateStr) return '';
  return moment(dateStr).format('MMM D');
}

function formatCount(value) {
  if (value >= 1000) {
    return `${(value / 1000).toFixed(0)}k`;
  }
  return String(value);
}

function formatPeakDate(dateStr) {
  if (!dateStr) return '';
  return moment(dateStr).format('MMM D');
}

export default function EvaluatedComponentsTile({ dailyHistory }) {
  if (!dailyHistory) {
    return null;
  }

  const { dailyHistory: entries, dailyAverage, peakDay } = dailyHistory;

  const hasEntries = entries && entries.length > 0;
  const hasAverage = typeof dailyAverage === 'number' && dailyAverage > 0;
  const hasPeak = peakDay && typeof peakDay.count === 'number' && peakDay.count > 0;
  if (!hasEntries && !hasAverage && !hasPeak) {
    return null;
  }

  return (
    <NxTile className="iq-usage-evaluated-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH3>Evaluated Components</NxH3>
        </NxTile.HeaderTitle>
        <NxTile.HeaderActions>
          <span className="iq-usage-evaluated__period">30 Days</span>
        </NxTile.HeaderActions>
      </NxTile.Header>
      <NxTile.Content>
        <div className="iq-usage-insights-grid">
          <div className="iq-usage-insights-widget">
            <span className="iq-usage-insights-widget__label">Daily Average</span>
            <span className="iq-usage-insights-widget__value">{formatNumber(dailyAverage)}</span>
          </div>
          <div className="iq-usage-insights-widget">
            <span className="iq-usage-insights-widget__label">Peak Day</span>
            <span className="iq-usage-insights-widget__value">{peakDay ? formatNumber(peakDay.count) : '0'}</span>
            {peakDay && peakDay.date && (
              <span className="iq-usage-insights-widget__date">{formatPeakDate(peakDay.date)}</span>
            )}
          </div>
        </div>
        {entries && entries.length > 0 && (
          <div className="iq-usage-trend-section">
            <span className="iq-usage-trend-section__title">Usage Trend</span>
            <div className="iq-usage-trend-chart">
              <ResponsiveContainer width="100%" height={250}>
                <AreaChart data={entries} margin={{ top: 10, right: 20, left: 0, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="date" tickFormatter={formatDayLabel} />
                  <YAxis tickFormatter={formatCount} />
                  <Tooltip
                    labelFormatter={formatDayLabel}
                    formatter={(value) => [value.toLocaleString(), 'Components (Cumulative)']}
                    contentStyle={{
                      backgroundColor: 'var(--nx-color-component-background)',
                      border: '1px solid var(--nx-color-border)',
                      borderRadius: '4px',
                      color: 'var(--nx-color-text)',
                    }}
                  />
                  <Area
                    type="monotone"
                    dataKey="componentsCumulative"
                    stroke="var(--nx-swatch-blue-70)"
                    fill="var(--nx-swatch-blue-70)"
                    fillOpacity={0.3}
                    name="Components (Cumulative)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>
        )}
      </NxTile.Content>
    </NxTile>
  );
}

EvaluatedComponentsTile.propTypes = {
  dailyHistory: PropTypes.shape({
    dailyHistory: PropTypes.arrayOf(
      PropTypes.shape({
        date: PropTypes.string.isRequired,
        components: PropTypes.number.isRequired,
        componentsCumulative: PropTypes.number.isRequired,
      })
    ),
    dailyAverage: PropTypes.number,
    peakDay: PropTypes.shape({
      count: PropTypes.number,
      date: PropTypes.string,
    }),
  }),
};
