/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { ResponsiveLine } from '@nivo/line';
import { NxH2, NxLoadWrapper, NxTile } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { selectAdoptionGraphSlice } from './adoptionGraphSelectors';
import { actions } from '../../slices/adoptionGraphSlice';
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
  pipe,
  prop,
  props,
  reduce,
} from 'ramda';

export default function AdoptionGraph() {
  const dispatch = useDispatch();
  const { graphData, loading, loadError } = useSelector(selectAdoptionGraphSlice);
  const formattedGraphData = formatGraphData(graphData);

  const doLoad = () => {
    dispatch(actions.loadAdoptionGraphData());
  };

  useEffect(() => {
    doLoad();
  }, []);

  return (
    <NxTile className="iq-developer-dashboard-adoption-chart">
      <NxTile.Header>
        <NxH2>Integration Adoption Report</NxH2>
      </NxTile.Header>
      <NxLoadWrapper error={loadError} retryHandler={doLoad} loading={loading}>
        <div style={{ height: 400 }}>
          <ResponsiveLine
            data={formattedGraphData}
            theme={{
              grid: {
                line: {
                  stroke: 'var(--nx-swatch-grey-90)',
                  strokeWidth: 1,
                },
              },
            }}
            margin={{ top: 50, right: 80, bottom: 80, left: 80 }}
            xScale={{ type: 'point' }}
            yScale={{
              type: 'linear',
              stacked: false,
              reverse: false,
            }}
            pointBorderWidth={2}
            pointBorderColor={{ from: 'serieColor' }}
            colors={['var(--nx-swatch-teal-40)', 'var(--nx-swatch-orange-40)']}
            useMesh={true}
            tooltip={(tooltip) => getTooltip(tooltip)}
            axisBottom={{
              legend: 'Weeks',
              legendOffset: 40,
              legendPosition: 'middle',
              tickRotation: -30,
            }}
            axisLeft={{
              legend: 'Developer adoption',
              legendOffset: -50,
              legendPosition: 'middle',
              format: (value) => `${Math.round(value * 100)}%`,
            }}
            legends={[
              {
                anchor: 'bottom',
                direction: 'row',
                translateY: 80,
                translateX: -10,
                itemWidth: 80,
                itemHeight: 20,
                symbolShape: 'circle',
                itemTextColor: 'var(--nx-swatch-grey-30)',
                data: [
                  {
                    id: 'scm',
                    label: 'SCM',
                    color: 'var(--nx-swatch-orange-40)',
                  },
                  {
                    id: 'cicd',
                    label: 'CI/CD',
                    color: 'var(--nx-swatch-teal-40)',
                  },
                ],
              },
            ]}
          />
        </div>
      </NxLoadWrapper>
    </NxTile>
  );
}

function getTooltip(tooltip) {
  return (
    <div className="iq-developer-adoption-tooltip">
      <div>
        Total CI Apps: <strong>{tooltip.point.data.totalNumberOfApps}</strong>
      </div>
      <div>
        {tooltip.point.serieId === 'cicd' ? 'CI/CD' : 'SCM'} Adoption:{' '}
        <strong>{Math.round(tooltip.point.data.y * 100)}%</strong>
      </div>
    </div>
  );
}

function formatTimestampToMonthDay(timestamp) {
  const date = new Date(timestamp);
  const monthAbbreviation = date.toLocaleString('default', { month: 'short' });
  const day = date.getDate().toString().padStart(2, '0');
  return `${monthAbbreviation} ${day}`;
}

function formatGraphData(graphData) {
  if (graphData) {
    const { cicd, scm } = graphData;
    const renameKeys = curry((keysMap, obj) =>
      reduce((acc, key) => assoc(keysMap[key] || key, obj[key], acc), {}, keys(obj))
    );

    // Rename keys so that accessing props is easier. Associate x axis with dateTimeMillis
    const getRenamedKeys = (data) =>
      renameKeys({
        totalNumberOfAppsWithCiCdEnabled: 'enabled',
        totalNumberOfAppsWithScmEnabled: 'enabled',
      })(data);

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

    // Format timestamps to be in the format of Nov 17
    const getUniformTimeRange = (obj) => assoc('x', formatTimestampToMonthDay(obj.dateTimeMillis), obj);

    // Perform % calculation and time range formatting
    const addPercent = (inputData) => map(compose(getUniformTimeRange, calculatePercentApps), inputData);

    // Execute in order of rename, % calculation, time range format.
    const format = compose(addPercent, map(getRenamedKeys));

    const data = [
      { id: 'cicd', data: format(cicd) },
      { id: 'scm', data: format(scm) },
    ];
    return data;
  }
  return null;
}
