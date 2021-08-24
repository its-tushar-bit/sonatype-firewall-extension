/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Dataset, Plots, Scales } from 'plottable';

const EMPTY_PREFIX = '~empty~';

export function showRow(componentDisplayName) {
  return componentDisplayName.indexOf(EMPTY_PREFIX) === -1;
}

function makeDataset(data, type, datasetClassName) {
  return new Dataset(
    data[type].map(function (element) {
      return { y: element.hash, x: element.count };
    }),
    { className: datasetClassName }
  );
}

export function makeChart(data, type, datasetClassName) {
  const dataset = makeDataset(data, type, datasetClassName),
    max = dataset
      .data()
      .map((d) => d.x)
      .reduce((a, b) => Math.max(a, b), 0),
    plot = new Plots.Bar('horizontal')
      .addDataset(dataset)
      .y(({ y }) => y, new Scales.Category())
      .x(({ x }) => x, new Scales.Linear().domain([0, max]))
      .attr('class', (d, i, dtSet) => dtSet.metadata().className)
      .attr('fill', () => undefined);

  return plot;
}
