/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';

import { NxTableCell, NxTableRow } from '@sonatype/react-shared-components';

import DashboardComponentsTableRow from '../../../../../main/frontend/dashboard/results/components/DashboardComponentsTableRow';
import DashboardHeatMapCell from '../../../../../main/frontend/dashboard/results/DashboardHeatMapCell';

describe('DashboardComponentsTableRow', function () {
  let minimalProps, getShallowComponent, getMountedComponent;

  beforeEach(function () {
    minimalProps = {
      stateGo: jasmine.createSpy('stateGo'),
    };

    getShallowComponent = enzymeUtils.getShallowComponent(
      DashboardComponentsTableRow,
      minimalProps
    );
    getMountedComponent = enzymeUtils.getMountedComponent(
      DashboardComponentsTableRow,
      minimalProps
    );
  });

  it('renders a clickable NxTableRow with appropriate columns', function () {
    const row = getMountedComponent({
      component: {
        hash: 'componentHash',
        derivedComponentName: 'componentNameFromCoordinates',
        affectedApplications: 9001,
        score: 94784,
        scoreCritical: 300,
        scoreSevere: 20,
        scoreModerate: 10,
        scoreLow: 35,
      },
    });

    const tableRow = row.find(NxTableRow),
      cells = row.find(NxTableCell),
      heatmapColoredCells = row.find(DashboardHeatMapCell);

    expect(tableRow).toHaveProp('isClickable', true);
    expect(heatmapColoredCells.length).toBe(6);

    expect(cells.at(0)).toHaveText('componentNameFromCoordinates');
    expect(cells.at(1)).toHaveText('9001');
    expect(cells.at(2)).toHaveText('94784');
    expect(cells.at(3)).toHaveText('300');
    expect(cells.at(4)).toHaveText('20');
    expect(cells.at(5)).toHaveText('10');
    expect(cells.at(6)).toHaveText('35');

    expect(cells.at(7)).toHaveProp('chevron', true);
    expect(heatmapColoredCells.at(5)).toHaveProp('chevron', true);
    expect(heatmapColoredCells.at(5)).toHaveProp('threatScore', 35);
  });

  it('opens component details when clicking on the row', function () {
    const row = getShallowComponent({
      component: {
        hash: 'componentHash',
      },
    });

    row.simulate('click');
    expect(minimalProps.stateGo).toHaveBeenCalledWith('dashboard.component', {
      hash: 'componentHash',
    });
  });
});
