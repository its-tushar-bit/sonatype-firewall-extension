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
import { commonGraphProps, graphColors, formatMttrGraphData } from '../../utils/graphUtils';

export default function MTTRGraph() {
  const dispatch = useDispatch();
  const { graphData, loading, loadError } = useSelector(selectDeveloperDashboardGraphsSlice);
  const formattedGraphData = formatMttrGraphData(graphData);

  const doLoad = () => {
    dispatch(actions.loadDeveloperDashboardGraphsData());
  };

  return (
    <div className="iq-developer-dashboard-mttr-graph">
      <NxH3>Mean Time to Remediate</NxH3>
      <NxLoadWrapper error={loadError} retryHandler={doLoad} loading={loading}>
        <div className="iq-developer-dashboard-graph-wrapper">
          <ResponsiveLine
            {...commonGraphProps()}
            data={formattedGraphData}
            colors={graphColors['mttrGraph']}
            tooltip={(tooltip) => getTooltip(tooltip)}
            axisLeft={{
              legend: 'Number of Days',
              legendOffset: -50,
              legendPosition: 'middle',
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
                    id: 'meanTimeToRemediate',
                    label: 'Mean Time to Remediate',
                    color: 'var(--nx-swatch-turquoise-40)',
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
  const meanTime = tooltip.point.data.y.toFixed(1);
  return (
    <div className="iq-developer-dashboard-graph-tooltip">
      <div>
        Mean Time to Remediate: <strong>{meanTime} Days</strong>
      </div>
    </div>
  );
}
