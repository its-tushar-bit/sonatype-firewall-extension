/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { generateChart, computeWeekCount } from './ViolationsByCategoryUtils';
import * as PropTypes from 'prop-types';
import { violationsByCategoryShape } from '../SuccessMetricsPropTypes';
import { renderChart } from '../../chartUtils';

const ViolationsByCategoryChart = ({ violationsByCategoryData }) => {
  const weekCount = computeWeekCount(violationsByCategoryData);

  useEffect(() => renderChart(generateChart(violationsByCategoryData), '#bycategory-chart-container'), []);

  return (
    <section className="nx-tile" id="violations-by-category-chart" aria-label="Violation By Category Chart">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">12 Week Open Violation Totals</h2>
        </div>
        <div className="nx-tile-header__subtitle">
          Open violations over the past {weekCount} week{weekCount === 1 ? '' : 's'} by policy type.
        </div>
      </header>
      <div className="nx-tile-content" id="violation-by-category-chart">
        <div className="iq-chart iq-chart--violations-by-category">
          <div id="bycategory-chart-container"></div>
        </div>
      </div>
    </section>
  );
};

ViolationsByCategoryChart.propTypes = {
  violationsByCategoryData: PropTypes.arrayOf(violationsByCategoryShape),
};

export default ViolationsByCategoryChart;
