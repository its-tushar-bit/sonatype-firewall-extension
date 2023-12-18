/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { ResponsiveLine } from '@nivo/line';
import { NxH2, NxLoadWrapper } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { selectDeveloperDashboardGraphsSlice } from './developerDashboardGraphsSelectors';
import { actions } from '../../slices/developerDashboardGraphsSlice';
import { commonGraphProps, graphColors, formatRiskRemediationGraphData } from '../../utils/graphUtils';

export default function RiskAndRemediationGraph() {
  const dispatch = useDispatch();
  const { graphData, loading, loadError } = useSelector(selectDeveloperDashboardGraphsSlice);
  const formattedGraphData = formatRiskRemediationGraphData(graphData);

  const doLoad = () => {
    dispatch(actions.loadDeveloperDashboardGraphsData());
  };

  return (
    <div className="iq-developer-dashboard-risk-remediation-graph">
      <NxH2>Risk & Remediation Timeline</NxH2>
      <NxLoadWrapper error={loadError} retryHandler={doLoad} loading={loading}>
        <div style={{ height: 400 }}>
          <ResponsiveLine
            {...commonGraphProps()}
            data={formattedGraphData}
            colors={graphColors['riskremediationGraph']}
            tooltip={(tooltip) => getTooltip(tooltip)}
            axisLeft={{
              legend: 'Policy Action Failures & Remediation Waivers',
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
                itemTextColor: 'var(--nx-swatch-grey-30)',
                data: [
                  {
                    id: 'remediationWaivers',
                    label: 'Remediation Waivers',
                    color: 'var(--nx-swatch-purple-40)',
                  },
                  {
                    id: 'policyActionFailures',
                    label: 'Policy Action Failures',
                    color: 'var(--nx-swatch-teal-40)',
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
        {tooltip.point.serieId === 'policyActionFailures'
          ? 'Number of apps with failed policies: '
          : 'Number of active waivers: '}
        <strong>{tooltip.point.data.y}</strong>
      </div>
    </div>
  );
}
