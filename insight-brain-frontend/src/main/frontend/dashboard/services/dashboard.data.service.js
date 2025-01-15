/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import {
  translateViolationsSortFields,
  translateComponentsSortFields,
  translateApplicationsSortFields,
  translateWaiversSortFields,
} from './sortFieldsUtils';

import {
  getNewestRisksUrl,
  getApplicationRisksUrl,
  getComponentRisksUrl,
  getWaiversUrl,
  getWaiversAndAutoWaiversUrl,
} from '../../util/CLMLocation';

import { createDashboardDataRequestPayload } from '../utils/dashboardUtils';
import { createClassyBrew } from '../utils/classybrew.factory';

export const MAX_RESULTS = 100;

export const DASHBOARD_PAGE_SIZE = 100;

export function getNewestRisks(filters, sortFields, page) {
  const request = createDashboardDataRequestPayload(
    filters,
    DASHBOARD_PAGE_SIZE,
    translateViolationsSortFields(sortFields),
    page
  );
  return axios.post(getNewestRisksUrl(), request).then(dashboardResponseHandler());
}

export function getApplicationRisks(filters, sortFields, page) {
  const request = createDashboardDataRequestPayload(
    filters,
    DASHBOARD_PAGE_SIZE,
    translateApplicationsSortFields(sortFields),
    page
  );
  return axios.post(getApplicationRisksUrl(), request).then(dashboardResponseHandler(generateApplicationsSeries));
}

export function getWaiversAndAutoWaivers(filters, sortFields, page) {
  const request = createDashboardDataRequestPayload(
    filters,
    DASHBOARD_PAGE_SIZE,
    translateWaiversSortFields(sortFields),
    page
  );
  return axios.post(getWaiversAndAutoWaiversUrl(), request).then(dashboardResponseHandler());
}

const applicationsScoreFields = ['totalRisk', 'criticalRisk', 'severeRisk', 'moderateRisk', 'lowRisk'];

function generateApplicationsSeries(applications) {
  const series = {};
  applications.forEach(function (application) {
    applicationsScoreFields.forEach(function (scoreField) {
      if (application.totalApplicationRisk[scoreField]) {
        series[application.totalApplicationRisk[scoreField]] = true;
      }
    });
  });

  return Object.keys(series).map(function (x) {
    return parseInt(x, 10);
  });
}

export function getWaivers(filters, sortFields, page) {
  const request = createDashboardDataRequestPayload(
    filters,
    DASHBOARD_PAGE_SIZE,
    translateWaiversSortFields(sortFields),
    page
  );
  return axios.post(getWaiversUrl(), request).then(dashboardResponseHandler());
}

export function getComponentRisks(filters, sortFields, page) {
  const request = createDashboardDataRequestPayload(
    filters,
    DASHBOARD_PAGE_SIZE,
    translateComponentsSortFields(sortFields),
    page
  );
  return axios.post(getComponentRisksUrl(), request).then(dashboardResponseHandler(generateComponentsSeries));
}

const componentsScoreFields = ['score', 'scoreCritical', 'scoreSevere', 'scoreModerate', 'scoreLow'];

function generateComponentsSeries(components) {
  const series = [];
  components.forEach(function (component) {
    componentsScoreFields.forEach(function (scoreField) {
      if (component[scoreField] && series.lastIndexOf(component[scoreField]) === -1) {
        series.push(component[scoreField]);
      }
    });
  });
  return series;
}

function dashboardResponseHandler(seriesGenerator) {
  return ({ data }) => {
    const { dashboardResults, hasNextPage } = data;
    let series = undefined;
    if (typeof seriesGenerator === 'function') {
      series = seriesGenerator(dashboardResults);
    }
    return {
      results: dashboardResults,
      ...(series && { classyBrew: createClassyBrew(series) }),
      hasNextPage,
    };
  };
}
