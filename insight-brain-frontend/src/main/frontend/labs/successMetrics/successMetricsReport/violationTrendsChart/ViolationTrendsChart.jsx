/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { renderCombinedTrendsChart, calculateTotals, getViolationTrendsData } from './ViolationTrendsChartUtils';
import { violationCountsShape } from '../SuccessMetricsPropTypes';

const ViolationTrendsChart = ({ violationCounts }) => {
  const weekCount = violationCounts.length;
  const data = getViolationTrendsData(violationCounts);
  const totals = calculateTotals(data.all);

  useEffect(() => {
    renderCombinedTrendsChart('#iq-violation-trends-all', data.all, data.statistics);
    renderCombinedTrendsChart('#iq-violation-trends-security', data.security, data.statistics);
    renderCombinedTrendsChart('#iq-violation-trends-license', data.license, data.statistics);
    renderCombinedTrendsChart('#iq-violation-trends-quality', data.quality, data.statistics);
    renderCombinedTrendsChart('#iq-violation-trends-other', data.other, data.statistics);
  }, []);

  return (
    <section className="nx-tile" id="violation-trends-chart" aria-label="Violation Trends Chart">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">12 Week Policy Violation Activity</h2>
        </div>
        <div className="nx-tile-header__subtitle">
          Violations and remediation over the past {weekCount} week{weekCount === 1 ? '' : 's'}.
        </div>
      </header>
      <div className="nx-tile-content">
        <div id="violation-trends-chart">
          <table className="iq-table iq-table--violation-trends-chart-table">
            <thead>
              <tr className="iq-table-row">
                <th className="iq-cell iq-cell--header"></th>
                <th className="iq-cell iq-cell--header">{weekCount} Week Total</th>
                <th className="iq-cell iq-cell--header">All Violations</th>
                <th className="iq-cell iq-cell--header iq-cell--violation-trends-gutter"></th>
                <th className="iq-cell iq-cell--header">Security</th>
                <th className="iq-cell iq-cell--header">License</th>
                <th className="iq-cell iq-cell--header">Quality</th>
                <th className="iq-cell iq-cell--header">Other</th>
              </tr>
            </thead>
            <tbody>
              <tr className="iq-table-row iq-table-row--violation-trends-chart-deltas">
                <td className="iq-cell iq-cell--violation-trends-row-label">
                  <h4>Deltas</h4>
                </td>
                <td className="iq-cell iq-cell--violation-trends-chart-total">
                  {totals.totalDelta !== 0 && (
                    <div className="iq-pull-right iq-cell--violation-trends-chart-total-delta-icon">
                      <i
                        className={`iq-violation-trends__delta-icon fa ${
                          totals.totalDelta > 0 ? 'fa-caret-up' : 'fa-caret-down'
                        }`}
                      ></i>
                    </div>
                  )}
                  <div className="iq-cell--violation-trends-chart-total-number">{totals.totalDelta}</div>
                </td>
                <td className="iq-cell iq-cell--violation-trends-chart" rowSpan="4">
                  <div className="iq-violation-trends__chart">
                    <div id="iq-violation-trends-all"></div>
                  </div>
                </td>
                <td className="iq-cell iq-cell--violation-trends-gutter" rowSpan="4"></td>
                <td className="iq-cell iq-cell--violation-trends-chart" rowSpan="4">
                  <div className="iq-violation-trends__chart">
                    <div id="iq-violation-trends-security"></div>
                  </div>
                </td>
                <td className="iq-cell iq-cell--violation-trends-chart" rowSpan="4">
                  <div className="iq-violation-trends__chart">
                    <div id="iq-violation-trends-license"></div>
                  </div>
                </td>
                <td className="iq-cell iq-cell--violation-trends-chart" rowSpan="4">
                  <div className="iq-violation-trends__chart">
                    <div id="iq-violation-trends-quality"></div>
                  </div>
                </td>
                <td className="iq-cell iq-cell--violation-trends-chart" rowSpan="4">
                  <div className="iq-violation-trends__chart">
                    <div id="iq-violation-trends-other"></div>
                  </div>
                </td>
              </tr>
              <tr className="iq-table-row iq-table-row--violation-trends-chart-new">
                <td className="iq-cell iq-cell--violation-trends-row-label">
                  <h4>New</h4>
                </td>
                <td className="iq-cell iq-cell--violation-trends-chart-total">
                  <div className="iq-cell--violation-trends-chart-total-number">{totals.totalDiscovered}</div>
                </td>
              </tr>
              <tr className="iq-table-row iq-table-row--violation-trends-chart-waived">
                <td className="iq-cell iq-cell--violation-trends-row-label">
                  <h4>Waived</h4>
                </td>
                <td className="iq-cell iq-cell--violation-trends-chart-total">
                  <div className="iq-cell--violation-trends-chart-total-number">{totals.totalWaived}</div>
                </td>
              </tr>
              <tr className="iq-table-row iq-table-row--violation-trends-chart-fixed">
                <td className="iq-cell iq-cell--violation-trends-row-label">
                  <h4>Fixed</h4>
                </td>
                <td className="iq-cell iq-cell--violation-trends-chart-total">
                  <div className="iq-cell--violation-trends-chart-total-number">{totals.totalFixed}</div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
};
ViolationTrendsChart.propTypes = {
  violationCounts: PropTypes.arrayOf(violationCountsShape),
};

export default ViolationTrendsChart;
