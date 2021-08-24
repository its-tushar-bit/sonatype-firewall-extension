/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxOverflowTooltip } from '@sonatype/react-shared-components';
import React, { useEffect, Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { makeChart, showRow } from './componentCountsChartUtils';
import { componentCountsShape } from '../SuccessMetricsPropTypes';
import { renderChart } from '../../chartUtils';

const ComponentCountsChart = ({
  activeApplicationCount,
  isSingleApplicationReport,
  singleApplicationName,
  componentCounts,
}) => {
  useEffect(() => {
    let mostApplicationChartRender, mostViolationChartRender;

    if (componentCounts.componentsInTheMostApplications.length > 0 && !isSingleApplicationReport)
      mostApplicationChartRender = renderChart(
        makeChart(componentCounts, 'componentsInTheMostApplications', 'iq-chart__dataset--component'),
        '#components-in-most-applications-chart'
      );

    if (componentCounts.componentsWithTheMostViolations.length > 0)
      mostViolationChartRender = renderChart(
        makeChart(componentCounts, 'componentsWithTheMostViolations', 'iq-chart__dataset--critical'),
        '#component-with-most-violations-chart'
      );

    return () => {
      if (mostApplicationChartRender) mostApplicationChartRender();
      if (mostViolationChartRender) mostViolationChartRender();
    };
  }, []);
  return (
    <section className="nx-tile" aria-label="Component Counts Chart" id="component-counts-chart">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Components</h2>
        </div>
        <div className="nx-tile-header__subtitle">
          {isSingleApplicationReport
            ? `This data is based on the latest Lifecycle evaluations. ${singleApplicationName} contains ${componentCounts.componentsPerApplication} components.`
            : `This data is based on the latest Lifecycle evaluations of ${activeApplicationCount} applications. On average, there are ${componentCounts.componentsPerApplication} components per application.`}
        </div>
      </header>
      <div className="nx-tile-content" id="component-count-chart">
        {componentCounts.componentsInTheMostApplications.length > 0 && !isSingleApplicationReport ? (
          <Fragment>
            <h2 className="nx-h2">Components currently used across the most applications</h2>
            <div className="iq-chart iq-chart--component-counts iq-chart--horizontal-bar">
              <div id="component-in-most-applications" className="iq-chart__labels--horizontal-bar">
                {componentCounts.componentsInTheMostApplications.map((component, i) => {
                  if (showRow(component.componentDisplayName))
                    return (
                      <div
                        className="iq-chart__bar-label iq-chart__bar-label--component-counts"
                        key={component.componentDisplayName}
                      >
                        <NxOverflowTooltip>
                          <span>
                            <strong>{i + 1}.</strong> {component.componentDisplayName}
                          </span>
                        </NxOverflowTooltip>
                        <span>
                          <strong className="iq-chart__highlight iq-chart__highlight--component-counts">
                            {component.count}
                          </strong>
                          applications
                        </span>
                      </div>
                    );
                })}
              </div>
              <div id="components-in-most-applications-chart" className="iq-chart__container"></div>
            </div>
          </Fragment>
        ) : null}

        {componentCounts.componentsWithTheMostViolations.length > 0 && (
          <Fragment>
            <h2 className="nx-h2">Components currently raising the most policy violations</h2>
            <div className="iq-chart iq-chart--component-counts iq-chart--horizontal-bar">
              <div id="component-with-most-violations" className="iq-chart__labels--horizontal-bar">
                {componentCounts.componentsWithTheMostViolations.map((component, i) => {
                  if (showRow(component.componentDisplayName))
                    return (
                      <div
                        className="iq-chart__bar-label iq-chart__bar-label--component-counts"
                        key={component.componentDisplayName}
                      >
                        <NxOverflowTooltip>
                          <span>
                            <strong>{i + 1}.</strong> {component.componentDisplayName}
                          </span>
                        </NxOverflowTooltip>
                        <span>
                          <strong className="iq-chart__highlight iq-chart__highlight--component-counts">
                            {component.count}
                          </strong>
                          violations
                        </span>
                      </div>
                    );
                })}
              </div>
              <div id="component-with-most-violations-chart" className="iq-chart__container"></div>
            </div>
          </Fragment>
        )}
      </div>
    </section>
  );
};

ComponentCountsChart.propTypes = {
  activeApplicationCount: PropTypes.number,
  isSingleApplicationReport: PropTypes.bool,
  singleApplicationName: PropTypes.string,
  componentCounts: componentCountsShape,
};

export default ComponentCountsChart;
