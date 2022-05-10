/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxOverflowTooltip, NxTableCell, NxTableRow, NxThreatIndicator } from '@sonatype/react-shared-components';
import ComponentDisplay from '../../../../main/frontend/ComponentDisplay/ReactComponentDisplay';
import * as enzymeUtils from '../../enzymeUtils';

describe('DashboardViolationsTableRow', function () {
  let minimalProps, getShallowComponent, stateGoSpy, terseAgoSpy, DashboardViolationsTableRow;

  beforeEach(() => {
    stateGoSpy = jasmine.createSpy('stateGo');
    terseAgoSpy = jasmine.createSpy('terseAgo').and.returnValue('5d');

    minimalProps = {
      stateGo: stateGoSpy,
      violation: {
        policyViolationId: 'policyViolationId1',
        threatLevel: 7,
        policyName: 'policyName1',
        applicationName: 'App1',
        firstOccurrenceTime: Date.now(),
      },
    };

    DashboardViolationsTableRow = require('inject-loader!../../../../main/frontend/dashboard/results/violations/DashboardViolationsTableRow')(
      {
        '../../../utilAngular/CommonServices': { terseAgo: terseAgoSpy },
      }
    ).default;

    getShallowComponent = enzymeUtils.getShallowComponent(DashboardViolationsTableRow, minimalProps);
  });

  it('links on click to the violation details state', () => {
    const row = getShallowComponent();

    expect(row).toMatchSelector(NxTableRow);
    expect(row.key()).toEqual(minimalProps.violation.policyViolationId);
    expect(row).toHaveProp('onClick');
    expect(row).toHaveProp('isClickable');

    row.simulate('click');

    expect(stateGoSpy).toHaveBeenCalledWith('sidebarView.violation', {
      id: 'policyViolationId1',
      type: 'violation',
      sidebarReference: 'filter',
    });
  });

  it('renders the appropriate number of cells', () => {
    const row = getShallowComponent(),
      cells = row.find(NxTableCell);

    expect(cells.length).toEqual(6);
  });

  describe('row cells', () => {
    it('renders a cell with the threat level', () => {
      const row = getShallowComponent(),
        cells = row.find(NxTableCell),
        threatLevelCell = cells.at(0);

      expect(threatLevelCell.find(NxThreatIndicator)).toHaveProp(
        'policyThreatLevel',
        minimalProps.violation.threatLevel
      );
      expect(threatLevelCell.find('.nx-threat-number')).toExist();
    });

    it('renders a cell with the policy name', () => {
      const row = getShallowComponent(),
        cells = row.find(NxTableCell),
        policyNameCell = cells.at(1),
        overflowWrapper = policyNameCell.find(NxOverflowTooltip),
        truncatingDiv = overflowWrapper.find('.nx-truncate-ellipsis');

      expect(truncatingDiv).toHaveText(minimalProps.violation.policyName);
    });

    it('renders a cell with the application name', () => {
      const row = getShallowComponent(),
        cells = row.find(NxTableCell),
        applicationNameCell = cells.at(2),
        overflowWrapper = applicationNameCell.find(NxOverflowTooltip),
        truncatingDiv = overflowWrapper.find('.nx-truncate-ellipsis');

      expect(overflowWrapper).toExist();
      expect(truncatingDiv).toHaveText(minimalProps.violation.applicationName);
    });

    it('renders a cell with the component name', () => {
      const row = getShallowComponent(),
        cells = row.find(NxTableCell),
        componentCell = cells.at(3),
        componentDisplay = componentCell.find(ComponentDisplay);

      expect(componentDisplay).toHaveProp('component', minimalProps.violation);
      expect(componentDisplay).toHaveProp('truncate', true);
    });

    it('renders a cell with the time', () => {
      const row = getShallowComponent(),
        cells = row.find(NxTableCell),
        timeCell = cells.at(4);

      expect(terseAgoSpy).toHaveBeenCalledWith(minimalProps.violation.firstOccurrenceTime);
      expect(timeCell).toExist();
    });

    it('renders a cell with a chevron', () => {
      const row = getShallowComponent(),
        cells = row.find(NxTableCell),
        chevronCell = cells.at(5);

      expect(chevronCell).toHaveProp('chevron');
    });
  });
});
