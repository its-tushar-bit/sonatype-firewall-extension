/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import {
  translateViolationsSortFields,
  translateComponentsSortFields,
  translateApplicationsSortFields
} from './sortFieldsUtils';

import {
  getNewestRisksUrl,
  getApplicationRisksUrl,
  getComponentRisksUrl
} from '../../util/CLMLocation';

import { createDashboardDataRequestPayload } from '../utils/dashboard.utils.module';
import { createClassyBrew } from '../utils/classybrew.factory';

export const MAX_RESULTS = 100;

export function getNewestRisks(filters, sortFields) {
  const request = createDashboardDataRequestPayload(filters, MAX_RESULTS, translateViolationsSortFields(sortFields));
  return axios.post(getNewestRisksUrl(), request)
      .then(({ data }) => {
        const { dashboardResults, numResults } = data;
        return {
          results: dashboardResults,
          numResults
        };
      });
}

export function getApplicationRisks(filters, sortFields) {
  const request = createDashboardDataRequestPayload(filters, MAX_RESULTS, translateApplicationsSortFields(sortFields));
  return axios.post(getApplicationRisksUrl(), request)
      .then(({ data }) => {
        const { dashboardResults, numResults } = data;
        const series = generateApplicationsSeries(dashboardResults);
        return {
          results: dashboardResults,
          classyBrew: createClassyBrew(series),
          numResults
        };
      });
}

const scoreFields = ['totalRisk', 'criticalRisk', 'severeRisk', 'moderateRisk', 'lowRisk'];

function generateApplicationsSeries(applications) {
  const series = {};
  applications.forEach(function(application) {
    scoreFields.forEach(function(scoreField) {
      if (application.totalApplicationRisk[scoreField]) {
        series[application.totalApplicationRisk[scoreField]] = true;
      }
    });
  });

  return Object.keys(series).map(function(x) {
    return parseInt(x, 10);
  });
}

export function getComponentRisks(filters, sortFields) {
  const request = createDashboardDataRequestPayload(filters, MAX_RESULTS, translateComponentsSortFields(sortFields));
  return axios.post(getComponentRisksUrl(), request)
      .then(({ data }) => {
        const { dashboardResults, numResults } = data;
        const series = generateComponentsSeries(dashboardResults);
        return {
          results: dashboardResults,
          classyBrew: createClassyBrew(series),
          numResults
        };
      });
}

function generateComponentsSeries(components) {
  const series = [];
  const scoreFields = ['score', 'scoreCritical', 'scoreSevere', 'scoreModerate', 'scoreLow'];
  components.forEach(function(component) {
    scoreFields.forEach(function(scoreField) {
      if (component[scoreField] && series.lastIndexOf(component[scoreField]) === -1) {
        series.push(component[scoreField]);
      }
    });
  });
  return series;
}
