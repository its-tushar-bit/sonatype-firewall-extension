/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { ResponsiveLine } from '@nivo/line';
import { NxH3, NxLoadWrapper } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { selectDeveloperDashboardGraphsSlice } from 'MainRoot/development/developmentDashboard/selectors/developerDashboardGraphsSelectors';
import { actions } from 'MainRoot/development/developmentDashboard/slices/developerDashboardGraphsSlice';
import { commonGraphProps, graphColors, formatRiskRemediationGraphData, lineColors } from '../../utils/graphUtils';

export default function RiskAndRemediationGraph() {
  const dispatch = useDispatch();
  const { graphData, loading, loadError } = useSelector(selectDeveloperDashboardGraphsSlice);
  const formattedGraphData = formatRiskRemediationGraphData(graphData);

  const doLoad = () => {
    dispatch(actions.loadDeveloperDashboardGraphsData());
  };

  return (
    <div className="iq-developer-dashboard-risk-remediation-graph">
      <NxH3>Risk and Remediation</NxH3>
      <NxLoadWrapper error={loadError} retryHandler={doLoad} loading={loading}>
        <div className="iq-developer-dashboard-graph-wrapper">
          <ResponsiveLine
            {...commonGraphProps()}
            data={formattedGraphData}
            colors={graphColors['riskremediationGraph']}
            tooltip={(tooltip) => getTooltip(tooltip)}
            axisLeft={{
              legend: 'Count',
              legendOffset: -50,
              legendPosition: 'middle',
              format: (tickVal) => (Math.floor(tickVal) === tickVal ? tickVal : ''),
            }}
            legends={[
              {
                anchor: 'bottom',
                direction: 'row',
                translateY: 80,
                itemWidth: 160,
                itemHeight: 20,
                symbolShape: 'circle',
                data: [
                  {
                    id: 'remediationWaivers',
                    label: 'Active Waivers',
                    color: lineColors.riskremediationGraph.activeWaivers,
                  },
                  {
                    id: 'policyActionFailures',
                    label: 'Apps with Failing Violations',
                    color: lineColors.riskremediationGraph.appsWithFailingViolations,
                  },
                ],
              },
            ]}
          />
        </div>
      </NxLoadWrapper>
    </div>
  );
}

function getTooltip(tooltip) {
  return (
    <div className="iq-developer-dashboard-graph-tooltip">
      <div>
        {tooltip.point.serieId === 'policyActionFailures' ? 'Apps with Failing Violations: ' : 'Active Waivers: '}
        <strong>{tooltip.point.data.y}</strong>
      </div>
    </div>
  );
}
