/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  always,
  apply,
  assoc,
  compose,
  curry,
  divide,
  equals,
  ifElse,
  keys,
  map,
  pick,
  pipe,
  prop,
  props,
  reduce,
} from 'ramda';

export function commonGraphProps() {
  return {
    theme: {
      grid: {
        line: {
          stroke: 'var(--nx-swatch-grey-90)',
          strokeWidth: 1,
        },
      },
    },
    margin: { top: 20, right: 40, bottom: 80, left: 80 },
    xScale: { type: 'point' },
    yScale: {
      type: 'linear',
      stacked: false,
      reverse: false,
    },
    pointBorderWidth: 2,
    pointBorderColor: { from: 'serieColor' },
    useMesh: true,
    axisBottom: {
      legendOffset: 40,
      legendPosition: 'middle',
      tickRotation: -30,
    },
  };
}

export const graphColors = {
  adoptionGraph: ['var(--nx-swatch-teal-40)', 'var(--nx-swatch-orange-40)'],
  riskremediationGraph: ['var(--nx-swatch-teal-40)', 'var(--nx-swatch-purple-40)'],
  mttrGraph: ['var(--nx-swatch-turquoise-40)'],
};

function formatTimestampToMonthDay(timestamp) {
  const date = new Date(timestamp);
  const monthAbbreviation = date.toLocaleString('default', { month: 'short' });
  const day = date.getDate().toString().padStart(2, '0');
  return `${monthAbbreviation} ${day}`;
}

export const renameKeys = curry((keysMap, obj) =>
  reduce((acc, key) => assoc(keysMap[key] || key, obj[key], acc), {}, keys(obj))
);

// Rename keys so that accessing props is easier. Associate x axis with dateTimeMillis
export const getRenamedKeys = (data) =>
  renameKeys({
    totalNumberOfAppsUsingCiCd: 'enabled',
    totalNumberOfAppsWithScmEnabled: 'enabled',
    totalNumberOfPolicyActionFailuresByAppCount: 'y',
    totalNumberOfWaivers: 'y',
    meanTimeToRemediateMs: 'y',
  })(data);

export const getUniformTimeRange = (obj) => assoc('x', formatTimestampToMonthDay(obj.dateTimeMillis), obj);

function millisecondsToDays(milliseconds) {
  const millisecondsInOneDay = 24 * 60 * 60 * 1000;

  const days = milliseconds / millisecondsInOneDay;

  return days;
}

export function formatAdoptionGraphData(graphData) {
  if (graphData) {
    // Pick keys for associated dataset
    const cicd = map(pick(['dateTimeMillis', 'totalNumberOfAppsUsingCiCd', 'totalNumberOfApps']), graphData);
    const scm = map(pick(['dateTimeMillis', 'totalNumberOfAppsWithScmEnabled', 'totalNumberOfApps']), graphData);

    // Calculate percent based on totalEnabled / totalApps. Associate y axis with enabled
    const calculatePercentApps = (obj) =>
      assoc(
        'y',
        ifElse(
          pipe(prop('totalNumberOfApps'), equals(0)),
          always(0), // If totalNumberOfApps is 0, set 'y' to 0
          pipe(props(['enabled', 'totalNumberOfApps']), apply(divide))
        )(obj),
        obj
      );

    // Execute in order of rename, % calculation, time range format.
    const format = map(compose(getUniformTimeRange, calculatePercentApps, getRenamedKeys));

    const data = [
      { id: 'cicd', data: format(cicd) },
      { id: 'scm', data: format(scm) },
    ];
    return data;
  }
  return null;
}

export function formatRiskRemediationGraphData(graphData) {
  if (graphData) {
    // Pick keys for associated dataset
    const policyActionFailuresData = map(
      pick(['dateTimeMillis', 'totalNumberOfPolicyActionFailuresByAppCount']),
      graphData
    );
    const remediationWaiversData = map(pick(['dateTimeMillis', 'totalNumberOfWaivers']), graphData);

    // Execute in order of rename and time range format.
    const formatData = (data) => map(compose(getUniformTimeRange, getRenamedKeys), data);

    const data = [
      { id: 'policyActionFailures', data: formatData(policyActionFailuresData) },
      { id: 'remediationWaivers', data: formatData(remediationWaiversData) },
    ];
    return data;
  }
  return null;
}

export function formatMttrGraphData(graphData) {
  if (graphData) {
    // Pick keys for associated dataset
    const mttrData = map(pick(['dateTimeMillis', 'meanTimeToRemediateMs']), graphData);

    // Convert ms to days
    const getMeanTimeToRemediateInDays = (obj) => assoc('y', millisecondsToDays(obj.y), obj);

    // Execute in order of rename and time range format.
    const formatData = (data) => map(compose(getMeanTimeToRemediateInDays, getUniformTimeRange, getRenamedKeys), data);

    const data = [{ id: 'meanTimeToRemediate', data: formatData(mttrData) }];
    return data;
  }
  return null;
}
