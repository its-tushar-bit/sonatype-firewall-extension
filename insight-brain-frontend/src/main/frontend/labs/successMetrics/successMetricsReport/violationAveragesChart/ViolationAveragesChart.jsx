/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { averagesShape } from '../SuccessMetricsPropTypes';
import { makeChart } from './violationAveragesChartUtils';
import { renderChart } from '../../chartUtils';

const ViolationAveragesChart = ({ averages, isSingleApplicationReport, activeApplicationCount, monthCount }) => {
  const averageEvaluationsRounded = Math.round(averages.evaluationCount);
  const averageDiscoveredTotal = averages.totalViolations.averageDiscovered;
  const averageDiscoveredTotalCritical = averages.totalViolations.averageDiscoveredCritical;

  useEffect(() => renderChart(makeChart(averages), '#violation-averages-chart-container'), []);

  return (
    <section className="nx-tile" id="violation-averages-chart" aria-label="Violation Averages Chart">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">{`Average Number of Violations Discovered Per Month${
            isSingleApplicationReport ? '' : ', Per Application'
          }`}</h2>
        </div>
        <div className="nx-tile-header__subtitle">
          {`Lifecycle performed an average of ${averageEvaluationsRounded} evaluation${
            averageEvaluationsRounded === 1 ? '' : 's'
          } per month on ${activeApplicationCount} application${
            isSingleApplicationReport ? '' : 's'
          } over the past ${monthCount} ${
            monthCount === 1 ? 'month' : 'months'
          }. Lifecycle found an average of ${averageDiscoveredTotal.toFixed(0)} policy violations${
            isSingleApplicationReport ? ',' : ' per application,'
          } ${averageDiscoveredTotalCritical.toFixed(0)} of which were critical.`}
        </div>
      </header>
      <div className="nx-tile-content" id="violation-averages">
        <div className="iq-chart iq-chart--violation-averages iq-chart--horizontal-bar">
          <div className="iq-chart__labels--horizontal-bar">
            <div className="iq-chart__bar-label">
              <span>
                <strong className="iq-chart__highlight">
                  {averages.securityViolations.averageDiscovered.toFixed(0)}
                </strong>
                Security Violations
              </span>
              <span>
                <strong className="iq-chart__highlight">
                  {averages.securityViolations.averageDiscoveredCritical.toFixed(0)}
                </strong>
                Critical
              </span>
            </div>
            <div className="iq-chart__bar-label">
              <span>
                <strong className="iq-chart__highlight">
                  {averages.licenseViolations.averageDiscovered.toFixed(0)}
                </strong>
                License Violations
              </span>
              <span>
                <strong className="iq-chart__highlight">
                  {averages.licenseViolations.averageDiscoveredCritical.toFixed(0)}
                </strong>
                Critical
              </span>
            </div>
            <div className="iq-chart__bar-label">
              <span>
                <strong className="iq-chart__highlight">
                  {averages.qualityViolations.averageDiscovered.toFixed(0)}
                </strong>
                Quality Violations
              </span>
              <span>
                <strong className="iq-chart__highlight">
                  {averages.qualityViolations.averageDiscoveredCritical.toFixed(0)}
                </strong>
                Critical
              </span>
            </div>
            <div className="iq-chart__bar-label">
              <span>
                <strong className="iq-chart__highlight">{averages.otherViolations.averageDiscovered.toFixed(0)}</strong>
                Other Violations
              </span>
              <span>
                <strong className="iq-chart__highlight">
                  {averages.otherViolations.averageDiscoveredCritical.toFixed(0)}
                </strong>
                Critical
              </span>
            </div>
          </div>
          <div id="violation-averages-chart-container" className="iq-chart__container"></div>
        </div>
      </div>
    </section>
  );
};

ViolationAveragesChart.propTypes = {
  averages: averagesShape,
  isSingleApplicationReport: PropTypes.bool,
  activeApplicationCount: PropTypes.number,
  monthCount: PropTypes.number,
};

export default ViolationAveragesChart;
