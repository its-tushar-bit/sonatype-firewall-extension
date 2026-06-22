/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useCallback, useState, useRef } from 'react';
import * as PropTypes from 'prop-types';
import { useSelector, useDispatch } from 'react-redux';
import {
  NxH1,
  NxPageMain,
  NxLoadWrapper,
  NxLoadError,
  NxButton,
  NxTooltip,
  NxFontAwesomeIcon,
} from '@sonatype/react-shared-components';
import { faDownload, faArrowsRotate } from '@fortawesome/free-solid-svg-icons';
import moment from 'moment';

import { actions } from './usageSlice';
import {
  selectSummary,
  selectHistoryBreakdown,
  selectChartAggregation,
  selectSourceBreakdown,
  selectStageBreakdown,
  selectTopApps,
  selectLoadingAll,
  selectLoadErrorAll,
  selectActiveTab,
  selectLastRefreshedAt,
} from './usageSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import ConsumptionChart from './ConsumptionChart';
import ConsumptionBySourceChart from './ConsumptionBySourceChart';
import ConsumptionByStageChart from './ConsumptionByStageChart';
import TopConsumingApps from './TopConsumingApps';
import EvaluatedComponentsTile from './EvaluatedComponentsTile';
import MyUsageTile from './MyUsageTile';
import UsageCategoriesTile from './UsageCategoriesTile';
import UsageTabs from './UsageTabs';
import { downloadConsumptionExport } from './usageApi';
import { authErrorMessage } from 'MainRoot/util/authorizationUtil';

import './_usageDashboard.scss';

export default function UsageDashboard({ isAuthorized }) {
  const dispatch = useDispatch();
  const summary = useSelector(selectSummary);
  const historyBreakdown = useSelector(selectHistoryBreakdown);
  const chartAggregation = useSelector(selectChartAggregation);
  const sourceBreakdown = useSelector(selectSourceBreakdown);
  const stageBreakdown = useSelector(selectStageBreakdown);
  const topApps = useSelector(selectTopApps);
  const loading = useSelector(selectLoadingAll);
  const loadErrorProp = useSelector(selectLoadErrorAll);
  const activeTab = useSelector(selectActiveTab);
  const lastRefreshedAt = useSelector(selectLastRefreshedAt);
  const routerParams = useSelector(selectRouterCurrentParams);

  // Use authorization error message if not authorized, otherwise use the actual load error
  const loadError = isAuthorized ? loadErrorProp : authErrorMessage;

  // Initial load on mount. Aggregation changes are handled by handleAggregationChange,
  // not by re-running this effect — so chartAggregation is intentionally excluded from
  // deps to avoid refetching all 6 endpoints on every aggregation toggle.
  // Skip the dispatch when the user is unauthorized so we don't fire 6 requests
  // that would all return 403.
  // If isAuthorized flips false→true after a re-auth flow, the effect re-fires with
  // whatever aggregation the user had already selected — re-fetching at the current
  // selection is the correct behavior here (not resetting to the default 'daily').
  // (chartAggregation intentionally omitted from deps — see comment above)
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

  const handleRefresh = useCallback(() => dispatch(actions.refresh()), [dispatch]);

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

  // 60s ticker that re-renders the subtitle so moment.fromNow() advances over time.
  // Without this, "a few seconds ago" stays put even after several minutes of idle.
  // useState-bumping the counter forces a re-render; the actual value isn't read.
  const [, setTick] = useState(0);
  useEffect(() => {
    if (!lastRefreshedAt) return undefined;
    const id = setInterval(() => setTick((t) => t + 1), 60000);
    return () => clearInterval(id);
  }, [lastRefreshedAt]);

  // Guard ref: set to true when the URL→slice effect fires a dispatch so the
  // slice→URL effect knows NOT to write the old activeTab value back to the URL.
  // Without this guard a deep-link reload races: on mount both effects run,
  // slice→URL sees the initial 'overview' state and overwrites the URL before
  // the URL→slice dispatch has reduced into the store.
  const urlSyncInProgressRef = useRef(false);

  // Sync URL ?tab → slice on mount / route param change.
  // Deps intentionally exclude `activeTab` and `dispatch`: the `tab !== activeTab` guard prevents
  // a loop with the slice→URL effect below, and dispatch is stable across renders.
  useEffect(() => {
    const tab = routerParams?.tab;
    if (tab && (tab === 'overview' || tab === 'trends') && tab !== activeTab) {
      urlSyncInProgressRef.current = true;
      dispatch(actions.setActiveTab(tab));
    }
  }, [routerParams?.tab]);

  // Sync slice activeTab → URL (replace to avoid history pollution).
  // When urlSyncInProgressRef is set it means the change came from the URL (not a
  // user click), so we must NOT write back — doing so would overwrite the URL with
  // the stale initial state before the dispatch has reduced.
  // Deps intentionally exclude `routerParams` and `dispatch`: the in-loop guard keeps us out
  // of cycles, and dispatch is stable across renders.
  useEffect(() => {
    if (urlSyncInProgressRef.current) {
      // URL→slice dispatch is in flight; skip the write-back this cycle.
      urlSyncInProgressRef.current = false;
      return;
    }
    if (activeTab && routerParams?.tab !== activeTab) {
      dispatch(stateGo('usage', { tab: activeTab }, { location: 'replace' }));
    }
  }, [activeTab]);

  return (
    <NxPageMain id="usage-page" className="iq-usage-page">
      <div className="iq-usage-page__header">
        <div className="iq-usage-page__header-title">
          <NxH1>Usage</NxH1>
          <span className="iq-usage-page__subtitle">
            {/* Hide the "Last refreshed: …" prefix until the first load fulfills.
                Before that we'd render the fallback "recently", which reads as a
                vague claim ("recently when?") rather than honest "no data yet". */}
            {lastRefreshedAt && <>Last refreshed: {moment(lastRefreshedAt).fromNow()}</>}
            <button
              type="button"
              className="iq-usage-page__refresh-icon"
              onClick={handleRefresh}
              aria-label="Refresh usage data"
              aria-busy={loading}
              disabled={loading || !isAuthorized}
            >
              <NxFontAwesomeIcon icon={faArrowsRotate} spin={loading} />
            </button>
          </span>
        </div>
        <div className="iq-usage-page__header-actions">
          {loading || exporting || !isAuthorized ? (
            <NxButton variant="tertiary" onClick={handleExport} disabled>
              <NxFontAwesomeIcon icon={faDownload} />
              <span>Export Report</span>
            </NxButton>
          ) : (
            <NxTooltip title="Download usage data as CSV">
              <NxButton variant="tertiary" onClick={handleExport}>
                <NxFontAwesomeIcon icon={faDownload} />
                <span>Export Report</span>
              </NxButton>
            </NxTooltip>
          )}
        </div>
      </div>
      <NxLoadError error={loadError} retryHandler={load} />
      <NxLoadError error={exportError} retryHandler={handleExport} />
      {!loadError && (
        <NxLoadWrapper loading={loading} retryHandler={load}>
          <UsageTabs
            overview={
              <div className="iq-usage-overview-pane">
                <MyUsageTile summary={summary} />
                <UsageCategoriesTile summary={summary} />
                <EvaluatedComponentsTile />
              </div>
            }
            trends={
              <div className="iq-usage-trends-pane">
                <ConsumptionChart
                  historyBreakdown={historyBreakdown}
                  aggregation={chartAggregation}
                  onAggregationChange={handleAggregationChange}
                  monthlyLimit={summary?.limit}
                />
                <div className="iq-usage-page__chart-row">
                  <ConsumptionBySourceChart sourceBreakdown={sourceBreakdown} />
                  <ConsumptionByStageChart stageBreakdown={stageBreakdown} />
                </div>
                <TopConsumingApps topApps={topApps} />
              </div>
            }
          />
        </NxLoadWrapper>
      )}
    </NxPageMain>
  );
}

UsageDashboard.propTypes = {
  isAuthorized: PropTypes.bool.isRequired,
};
