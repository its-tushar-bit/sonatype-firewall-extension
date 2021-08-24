/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { makeChart } from './applicationCountsChartUtils';
import { applicationCountsShape } from '../SuccessMetricsPropTypes';
import { renderChart } from '../../chartUtils';

const ApplicationCountsChart = ({ monthCount, applicationCounts }) => {
  useEffect(() => renderChart(makeChart(applicationCounts), '#application-count-chart'), []);

  return (
    <section className="nx-tile" id="application-counts-chart" aria-label="Applications Chart">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Applications with Violations by Policy Type</h2>
        </div>
        <div className="nx-tile-header__subtitle">
          {`Over the past ${monthCount} ${monthCount === 1 ? 'month' : 'months'},
    ${applicationCounts.total.applicationsWithViolations}
    out of ${applicationCounts.activeApplications} applications contained violations,
    and ${applicationCounts.total.applicationsWithCriticalViolations} contained
    critical violations.`}
        </div>
      </header>
      <div className="nx-tile-content" id="violation-averages">
        <div className="iq-chart iq-chart--application-counts iq-chart--horizontal-bar">
          <div className="iq-chart__labels--horizontal-bar">
            <div className="iq-chart__bar-label">
              <span>
                <strong className="iq-chart__highlight">{applicationCounts.security.applicationsWithViolations}</strong>
                With Security Violations
              </span>
              <span>
                <strong className="iq-chart__highlight">
                  {applicationCounts.security.applicationsWithCriticalViolations}
                </strong>
                Critical
              </span>
            </div>
            <div className="iq-chart__bar-label">
              <span>
                <strong className="iq-chart__highlight">{applicationCounts.license.applicationsWithViolations}</strong>
                With License Violations
              </span>
              <span>
                <strong className="iq-chart__highlight">
                  {applicationCounts.license.applicationsWithCriticalViolations}
                </strong>
                Critical
              </span>
            </div>
            <div className="iq-chart__bar-label">
              <span>
                <strong className="iq-chart__highlight">{applicationCounts.quality.applicationsWithViolations}</strong>
                With Quality Violations
              </span>
              <span>
                <strong className="iq-chart__highlight">
                  {applicationCounts.quality.applicationsWithCriticalViolations}
                </strong>
                Critical
              </span>
            </div>
            <div className="iq-chart__bar-label">
              <span>
                <strong className="iq-chart__highlight">{applicationCounts.other.applicationsWithViolations}</strong>
                With Other Violations
              </span>
              <span>
                <strong className="iq-chart__highlight">
                  {applicationCounts.other.applicationsWithCriticalViolations}
                </strong>
                Critical
              </span>
            </div>
          </div>
          <div id="application-count-chart" className="iq-chart__container"></div>
        </div>
      </div>
    </section>
  );
};

ApplicationCountsChart.propTypes = {
  monthCount: PropTypes.number,
  applicationCounts: applicationCountsShape,
};

export default ApplicationCountsChart;
