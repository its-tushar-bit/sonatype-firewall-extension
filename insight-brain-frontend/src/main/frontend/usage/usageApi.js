/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  getConsumptionDailyHistoryUrl,
  getConsumptionExportUrl,
  getConsumptionHistoryBreakdownUrl,
  getConsumptionHistoryBySourceUrl,
  getConsumptionHistoryByStageUrl,
  getConsumptionSummaryUrl,
  getConsumptionTopAppsUrl,
} from 'MainRoot/util/CLMLocation';

/**
 * Returns { startDate, endDate } when both are present, otherwise an empty object.
 * Used to merge optional date-range query params into the existing param bags passed
 * to the URL builders.
 *
 * @param {Object|null} range
 * @returns {Object}
 */
function rangeParams(range) {
  if (!range || !range.startDate || !range.endDate) return {};
  return { startDate: range.startDate, endDate: range.endDate };
}

/**
 * Fetches the consumption summary for the current billing period (or an explicit range)
 * @param {Object|null} [range] - Optional { startDate, endDate } in 'YYYY-MM-DD' format
 * @returns {Promise} Axios response with summary data
 */
export function fetchConsumptionSummary(range) {
  return axios.get(getConsumptionSummaryUrl(rangeParams(range)));
}

/**
 * Fetches the consumption history with per-activity-type breakdown
 * @param {string} aggregation - 'daily', 'weekly', or 'monthly'
 * @param {Object|null} [range] - Optional { startDate, endDate } in 'YYYY-MM-DD' format
 * @returns {Promise} Axios response with breakdown array
 */
export function fetchConsumptionHistoryBreakdown(aggregation = 'monthly', range) {
  return axios.get(getConsumptionHistoryBreakdownUrl(aggregation, rangeParams(range)));
}

/**
 * Fetches the consumption history grouped by source (UI, CLI, API)
 * @param {Object|null} [range] - Optional { startDate, endDate } in 'YYYY-MM-DD' format
 * @returns {Promise} Axios response with source breakdown array
 */
export function fetchConsumptionBySource(range) {
  return axios.get(getConsumptionHistoryBySourceUrl(rangeParams(range)));
}

/**
 * Fetches the consumption history grouped by application stage
 * @param {Object|null} [range] - Optional { startDate, endDate } in 'YYYY-MM-DD' format
 * @returns {Promise} Axios response with stage breakdown array
 */
export function fetchConsumptionByStage(range) {
  return axios.get(getConsumptionHistoryByStageUrl(rangeParams(range)));
}

/**
 * Fetches the top consuming applications for the current billing month
 * @param {Object|null} [range] - Optional { startDate, endDate } in 'YYYY-MM-DD' format
 * @returns {Promise} Axios response with top apps array
 */
export function fetchTopConsumingApps(range) {
  return axios.get(getConsumptionTopAppsUrl(rangeParams(range)));
}

/**
 * Fetches daily consumption history for the last 30 days with cumulative totals
 * @param {Object|null} [range] - Optional { startDate, endDate } in 'YYYY-MM-DD' format
 * @returns {Promise} Axios response with daily history, daily average, and peak day
 */
export function fetchDailyHistory(range) {
  return axios.get(getConsumptionDailyHistoryUrl(rangeParams(range)));
}

/**
 * Triggers a CSV export download of consumption data
 * @returns {Promise} Resolves when download completes
 */
export async function downloadConsumptionExport() {
  let url, link;
  try {
    const response = await axios.get(getConsumptionExportUrl(), {
      responseType: 'blob',
    });
    url = window.URL.createObjectURL(new Blob([response.data]));
    link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'consumption-export.csv');
    document.body.appendChild(link);
    link.click();
  } finally {
    if (url) {
      window.URL.revokeObjectURL(url);
    }
    if (link) {
      link.remove();
    }
  }
}
