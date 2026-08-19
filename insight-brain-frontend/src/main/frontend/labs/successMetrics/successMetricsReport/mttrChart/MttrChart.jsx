/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { getMttrData, makeMttrChart } from './mttrChartUtils';
import { mttrShape } from '../SuccessMetricsPropTypes';
import { renderChart } from '../../chartUtils';

const MttrChart = ({ activeApplicationCount, monthCount, mttrs }) => {
  useEffect(() => renderChart(makeMttrChart(getMttrData(mttrs)), '#mttr-chart-container'), []);

  return (
    <section className="nx-tile" id="mttr-chart" aria-label="MTTR Chart">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Mean Time to Resolution by Month</h2>
        </div>
        <div className="nx-tile-header__subtitle">
          {` This data represents the average age of violations that were resolved each month in ${activeApplicationCount} application${
            activeApplicationCount === 1 ? '' : 's'
          } over the past ${monthCount} month${
            monthCount === 1 ? '' : 's'
          }. A violation that does not reappear in a subsequent evaluation is considered resolved.`}
        </div>
      </header>
      <div className="nx-tile-content" id="mttr-chart">
        <div className="iq-chart iq-chart--mttr">
          <div id="mttr-chart-container"></div>
        </div>
      </div>
    </section>
  );
};

MttrChart.propTypes = {
  activeApplicationCount: PropTypes.number,
  monthCount: PropTypes.number,
  mttrs: PropTypes.arrayOf(mttrShape),
};

export default MttrChart;
