/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useCallback, useState } from 'react';
import * as PropTypes from 'prop-types';
import { useSelector, useDispatch } from 'react-redux';
import {
  NxH2,
  NxPageMain,
  NxTile,
  NxLoadWrapper,
  NxLoadError,
  NxButton,
  NxTooltip,
  NxFontAwesomeIcon,
} from '@sonatype/react-shared-components';
import { faDownload } from '@fortawesome/free-solid-svg-icons';
import moment from 'moment';

import { actions } from './usageSlice';
import {
  selectSummary,
  selectHistoryBreakdown,
  selectChartAggregation,
  selectSourceBreakdown,
  selectTopApps,
  selectDailyHistory,
  selectLoadingAll,
  selectLoadErrorAll,
} from './usageSelectors';
import ConsumptionChart from './ConsumptionChart';
import ConsumptionBySourceChart from './ConsumptionBySourceChart';
import TopConsumingApps from './TopConsumingApps';
import EvaluatedComponentsTile from './EvaluatedComponentsTile';
import { downloadConsumptionExport } from './usageApi';
import { authErrorMessage } from 'MainRoot/util/authorizationUtil';

import './_usageDashboard.scss';

/**
 * Formats a number with commas for display
 * @param {number} num - Number to format
 * @returns {string} Formatted number string
 */
function formatNumber(num) {
  if (num === null || num === undefined) {
    return '0';
  }
  return num.toLocaleString();
}

/**
 * Formats an ISO date-only string (YYYY-MM-DD) for display. Parses via moment
 * so the string is interpreted in local time, avoiding the "off by a day"
 * behaviour `new Date(dateString)` produces in timezones west of UTC.
 *
 * @param {string} dateString - ISO date string
 * @returns {string} Formatted date string
 */
function formatDate(dateString) {
  if (!dateString) {
    return '';
  }
  return moment(dateString).format('MMM D, YYYY');
}

export default function UsageDashboard({ isAuthorized }) {
  const dispatch = useDispatch();
  const summary = useSelector(selectSummary);
  const historyBreakdown = useSelector(selectHistoryBreakdown);
  const chartAggregation = useSelector(selectChartAggregation);
  const sourceBreakdown = useSelector(selectSourceBreakdown);
  const topApps = useSelector(selectTopApps);
  const dailyHistory = useSelector(selectDailyHistory);
  const loading = useSelector(selectLoadingAll);
  const loadErrorProp = useSelector(selectLoadErrorAll);

  // Use authorization error message if not authorized, otherwise use the actual load error
  const loadError = isAuthorized ? loadErrorProp : authErrorMessage;

  // Initial load on mount. Aggregation changes are handled by handleAggregationChange,
  // not by re-running this effect — so chartAggregation is intentionally excluded from
  // deps to avoid refetching all 6 endpoints on every aggregation toggle.
  // Skip the dispatch when the user is unauthorized so we don't fire 6 requests
  // that would all return 403.
  useEffect(() => {
    if (isAuthorized) {
      dispatch(actions.loadAllUsageData(chartAggregation));
    }
  }, [dispatch, isAuthorized]);

  // Retry handler for load errors — closes over the current aggregation value at click time.
  const load = () => {
    dispatch(actions.loadAllUsageData(chartAggregation));
  };

  const handleAggregationChange = useCallback(
    (newAggregation) => {
      dispatch(actions.setChartAggregation(newAggregation));
      dispatch(actions.loadHistoryBreakdown(newAggregation));
    },
    [dispatch]
  );

  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState(null);

  const handleExport = useCallback(async () => {
    setExportError(null);
    setExporting(true);
    try {
      await downloadConsumptionExport();
    } catch (error) {
      setExportError('An error occurred while exporting usage data. Please try again.');
    } finally {
      setExporting(false);
    }
  }, []);

  const renderMonthlySummary = () => {
    if (!summary) {
      return null;
    }

    const { consumed, limit, percentUsed, remaining, resetDate, billingWindowStart } = summary;
    const hasLimit = limit !== null && limit !== undefined;
    const hasValidPercent = hasLimit && typeof percentUsed === 'number';
    const displayPct = hasValidPercent ? Math.floor(percentUsed * 10) / 10 : null;
    const progressBarValue = displayPct !== null ? Math.min(displayPct, 100) : 0;
    const isOverLimit = displayPct !== null && displayPct >= 100;
    const isStrictlyOverLimit = hasLimit && consumed > limit;
    const withinLimitPct = isStrictlyOverLimit && consumed > 0 ? (limit / consumed) * 100 : 0;
    const overagePct = isStrictlyOverLimit ? 100 - withinLimitPct : 0;

    const getState = () => (isOverLimit ? 'over' : 'normal');

    const billingMonth = billingWindowStart ? moment(billingWindowStart).format('MMMM YYYY') : '';

    return (
      <div className="iq-usage-summary-card">
        <div className="iq-usage-card__header">
          <span className="iq-usage-card__label">Evaluated Components</span>
          {billingMonth && <span className="iq-usage-card__billing-month">{billingMonth}</span>}
        </div>
        <div className="iq-usage-card__content">
          <div className="iq-usage-card__value">
            {formatNumber(consumed)}
            {hasLimit && ` / ${formatNumber(limit)}`}
          </div>
          {hasLimit && (
            <div className={`iq-usage-card__progress-container iq-usage-card__progress-container--${getState()}`}>
              <div className="iq-usage-card__progress-bar">
                {isStrictlyOverLimit ? (
                  <>
                    <div
                      className="iq-usage-card__progress-fill iq-usage-card__progress-fill--within"
                      style={{ width: `${withinLimitPct}%` }}
                    />
                    <div
                      className="iq-usage-card__progress-fill iq-usage-card__progress-fill--overage"
                      style={{ width: `${overagePct}%` }}
                    />
                  </>
                ) : (
                  <div className="iq-usage-card__progress-fill" style={{ width: `${progressBarValue}%` }} />
                )}
              </div>
              <div className="iq-usage-card__progress-percentage">
                {displayPct !== null ? `${displayPct.toFixed(1)}%` : '—'}
              </div>
            </div>
          )}
          <div className="iq-usage-card__details">
            {hasLimit && (
              <span>
                {isStrictlyOverLimit ? (
                  <span className="iq-usage-card__over-limit">Over limit by {formatNumber(consumed - limit)}</span>
                ) : isOverLimit ? (
                  <span className="iq-usage-card__over-limit">Limit reached</span>
                ) : (
                  `${formatNumber(remaining)} remaining`
                )}
              </span>
            )}
            {resetDate && <span className="iq-usage-card__resets">Resets: {formatDate(resetDate)}</span>}
          </div>
        </div>
      </div>
    );
  };

  return (
    <NxPageMain id="usage-page" className="iq-usage-page">
      <div className="iq-usage-page__header">
        <NxH2>Usage</NxH2>
        {loading || exporting || !isAuthorized ? (
          <NxButton variant="tertiary" onClick={handleExport} disabled>
            <NxFontAwesomeIcon icon={faDownload} />
            <span>Export</span>
          </NxButton>
        ) : (
          <NxTooltip title="Download usage data as CSV">
            <NxButton variant="tertiary" onClick={handleExport}>
              <NxFontAwesomeIcon icon={faDownload} />
              <span>Export</span>
            </NxButton>
          </NxTooltip>
        )}
      </div>
      <NxLoadError error={loadError} retryHandler={load} />
      <NxLoadError error={exportError} retryHandler={handleExport} />
      {!loadError && (
        <NxLoadWrapper loading={loading} retryHandler={load}>
          <div className="iq-usage-content">
            {summary && (
              <NxTile className="iq-usage-tile">
                <NxTile.Content>{renderMonthlySummary()}</NxTile.Content>
              </NxTile>
            )}
            <ConsumptionChart
              historyBreakdown={historyBreakdown}
              aggregation={chartAggregation}
              onAggregationChange={handleAggregationChange}
              monthlyLimit={summary?.limit}
            />
            <ConsumptionBySourceChart sourceBreakdown={sourceBreakdown} />
            <TopConsumingApps topApps={topApps} />
            <EvaluatedComponentsTile dailyHistory={dailyHistory} />
          </div>
        </NxLoadWrapper>
      )}
    </NxPageMain>
  );
}

UsageDashboard.propTypes = {
  isAuthorized: PropTypes.bool.isRequired,
};
