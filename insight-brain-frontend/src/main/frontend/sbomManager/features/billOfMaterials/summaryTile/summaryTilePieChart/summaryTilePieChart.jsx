/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as PropTypes from 'prop-types';
import { ResponsivePie } from '@nivo/pie';
import { NxH3 } from '@sonatype/react-shared-components';
import * as R from 'ramda';

import { formatNumberLocale } from 'MainRoot/util/formatUtils';

export const NIVO_PERCENTAGE_COLOR_MAP = {
  percentage: 'var(--nx-swatch-teal-40)',
};

const NIVO_COMPLEMENT_COLOR_MAP = {
  complement: 'var(--nx-color-progress-background)',
};

const NIVO_EMPTY_CHART_DATA = [{ id: 'complement', label: 'complement', value: 100 }];

export const PieChart = ({ total, data, colorMap }) => {
  const totalValue = R.anyPass([R.is(Number), R.is(String)])(total) ? (
    <NxH3 className="sbom-manager-bill-of-materials-summary-pie-chart__total" data-testid="pie-chart-total">
      {R.when(R.is(Number), formatNumberLocale)(total)}
    </NxH3>
  ) : null;

  const isPercentage = R.values(data).length === 1;
  const rawChartData = isPercentage ? { complement: 100 - R.head(R.values(data)), ...data } : data;

  const isEmptyChart = R.values(data).every((value) => value === 0 || value === null);
  const chartData = isEmptyChart
    ? NIVO_EMPTY_CHART_DATA
    : R.keys(rawChartData).map((field) => ({ id: field, label: field, value: rawChartData[field] }));

  const combinedColorMap = {
    ...colorMap,
    ...NIVO_COMPLEMENT_COLOR_MAP,
  };

  return (
    <div className="sbom-manager-bill-of-materials-summary-pie-chart">
      <ResponsivePie
        data={chartData}
        enableArcLabels={false}
        enableArcLinkLabels={false}
        isInteractive={false}
        cornerRadius={4}
        borderWidth={0}
        innerRadius={0.7}
        padAngle={isPercentage ? 0 : 2}
        colors={chartData.map(({ label }) => combinedColorMap[label])}
      />
      {totalValue}
    </div>
  );
};

PieChart.propTypes = {
  total: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  data: PropTypes.object.isRequired,
  colorMap: PropTypes.object.isRequired,
};

export default PieChart;
