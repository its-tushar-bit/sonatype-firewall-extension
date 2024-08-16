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
import { commonGraphProps, graphColors, formatAdoptionGraphData } from '../../utils/graphUtils';
export default function AdoptionGraph() {
  const dispatch = useDispatch();
  const { graphData, loading, loadError } = useSelector(selectDeveloperDashboardGraphsSlice);
  const formattedGraphData = formatAdoptionGraphData(graphData);

  const doLoad = () => {
    dispatch(actions.loadDeveloperDashboardGraphsData());
  };

  return (
    <div className="iq-developer-dashboard-adoption-graph">
      <NxH3>Adoption Profile</NxH3>
      <NxLoadWrapper error={loadError} retryHandler={doLoad} loading={loading}>
        <div className="iq-developer-dashboard-graph-wrapper">
          <ResponsiveLine
            {...commonGraphProps()}
            data={formattedGraphData}
            colors={graphColors['adoptionGraph']}
            tooltip={(tooltip) => getTooltip(tooltip)}
            axisLeft={{
              legend: 'Onboarded Apps',
              legendOffset: -50,
              legendPosition: 'middle',
              format: (value) => `${Math.round(value * 100)}%`,
            }}
            legends={[
              {
                anchor: 'bottom',
                direction: 'row',
                translateY: 80,
                translateX: -10,
                itemWidth: 80,
                itemHeight: 20,
                symbolShape: 'circle',
                itemTextColor: 'var(--nx-swatch-grey-30)',
                data: [
                  {
                    id: 'scm',
                    label: 'SCM',
                    color: 'var(--nx-swatch-orange-40)',
                  },
                  {
                    id: 'cicd',
                    label: 'CI/CD',
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
  const {
    serieId,
    data: { enabled, y, totalNumberOfApps },
  } = tooltip.point;
  const title = `Total ${serieId === 'cicd' ? 'CI' : 'SCM'} Apps:`;
  const metric = serieId === 'cicd' ? 'CI/CD' : 'SCM';

  return (
    <div className="iq-developer-dashboard-graph-tooltip">
      <div>
        {title} <strong>{enabled}</strong>
      </div>
      <div>
        Total Apps: <strong>{totalNumberOfApps}</strong>
      </div>
      <div>
        {metric} Adoption: <strong>{Math.round(y * 100)}%</strong>
      </div>
    </div>
  );
}
