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

import { getNewestRisksUrl, getApplicationRisksUrl, getComponentRisksUrl, getWaiversUrl } from '../../util/CLMLocation';

import { createDashboardDataRequestPayload } from '../utils/dashboard.utils.module';
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
  return axios.post(getNewestRisksUrl(), request).then(dashboardRespopnseHandler());
}

export function getApplicationRisks(filters, sortFields) {
  const request = createDashboardDataRequestPayload(filters, MAX_RESULTS, translateApplicationsSortFields(sortFields));
  return axios.post(getApplicationRisksUrl(), request).then(dashboardRespopnseHandler(generateApplicationsSeries));
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

export function getWaivers(filters, sortFields) {
  const request = createDashboardDataRequestPayload(filters, MAX_RESULTS, translateWaiversSortFields(sortFields));
  return axios.post(getWaiversUrl(), request).then(dashboardRespopnseHandler());
}

export function getComponentRisks(filters, sortFields, page) {
  const request = createDashboardDataRequestPayload(
    filters,
    DASHBOARD_PAGE_SIZE,
    translateComponentsSortFields(sortFields),
    page
  );
  return axios.post(getComponentRisksUrl(), request).then(dashboardRespopnseHandler(generateComponentsSeries));
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

function dashboardRespopnseHandler(seriesGenerator) {
  return ({ data }) => {
    const { dashboardResults, numResults } = data;
    let series = undefined;
    if (typeof seriesGenerator === 'function') {
      series = seriesGenerator(dashboardResults);
    }
    return {
      results: dashboardResults,
      ...(series && { classyBrew: createClassyBrew(series) }),
      numResults,
    };
  };
}
