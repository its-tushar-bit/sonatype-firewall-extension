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
 * Fetches the consumption summary for the current billing period
 * @returns {Promise} Axios response with summary data
 */
export function fetchConsumptionSummary() {
  return axios.get(getConsumptionSummaryUrl());
}

/**
 * Fetches the consumption history with per-activity-type breakdown
 * @param {string} aggregation - 'daily', 'weekly', or 'monthly'
 * @returns {Promise} Axios response with breakdown array
 */
export function fetchConsumptionHistoryBreakdown(aggregation = 'monthly') {
  return axios.get(getConsumptionHistoryBreakdownUrl(aggregation));
}

/**
 * Fetches the consumption history grouped by source (UI, CLI, API)
 * @returns {Promise} Axios response with source breakdown array
 */
export function fetchConsumptionBySource() {
  return axios.get(getConsumptionHistoryBySourceUrl());
}

/**
 * Fetches the consumption history grouped by application stage
 * @returns {Promise} Axios response with stage breakdown array
 */
export function fetchConsumptionByStage() {
  return axios.get(getConsumptionHistoryByStageUrl());
}

/**
 * Fetches the top consuming applications for the current billing month
 * @returns {Promise} Axios response with top apps array
 */
export function fetchTopConsumingApps() {
  return axios.get(getConsumptionTopAppsUrl());
}

/**
 * Fetches daily consumption history for the last 30 days with cumulative totals
 * @returns {Promise} Axios response with daily history, daily average, and peak day
 */
export function fetchDailyHistory() {
  return axios.get(getConsumptionDailyHistoryUrl());
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
