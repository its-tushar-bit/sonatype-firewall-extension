/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import { NxTableCell, NxTableRow } from '@sonatype/react-shared-components';
import LegalBinaryDonutChart from '../../../../main/frontend/legal/shared/LegalBinaryDonutChart';

describe('LegalDashboardApplicationRow component', function () {
  let getShallowComponent, terseAgoSpy, LegalDashboardApplicationRow;
  const stateGoSpy = jasmine.createSpy('stateGo');

  const minimalProps = {
    row: {
      applicationId: 'appId1',
      applicationPublicId: 'app ID 1',
      applicationName: 'appName1',
      lastScanTime: 1607030429000,
      applicationTagNames: ['tag1', 'tag2'],
      stageTypeName: 'Build',
      stageTypeId: 'build',
      componentsReviewedCount: 12,
      componentsTotalCount: 20,
    },
    stateGo: stateGoSpy,
  };

  terseAgoSpy = jasmine.createSpy('terseAgo').and.returnValue('2d');

  beforeEach(function () {
    LegalDashboardApplicationRow = require('inject-loader!../../../../main/frontend/legal/dashboard/LegalDashboardApplicationRow')(
      {
        '../../utilAngular/CommonServices': { terseAgo: terseAgoSpy },
      }
    ).default;

    getShallowComponent = enzymeUtils.getShallowComponent(LegalDashboardApplicationRow, minimalProps);
  });

  it('renders a NxTableRow with appropriate cells', function () {
    const wrapper = getShallowComponent();
    let tableRow = wrapper.find(NxTableRow);
    expect(tableRow).toExist();
    expect(tableRow).toHaveProp('isClickable', true);
    expect(tableRow).toHaveProp('onClick', jasmine.any(Function));
    let cells = tableRow.find(NxTableCell);
    expect(cells.length).toEqual(5);
    expect(cells.at(0).children().text()).toEqual('appName1');
    expect(terseAgoSpy).toHaveBeenCalledWith(1607030429000);
    expect(cells.at(1).children().text()).toEqual('2d - Build');
    expect(cells.at(2).children().text()).toEqual('tag1, tag2');
    let donutChart = cells.at(3).find(LegalBinaryDonutChart);
    expect(donutChart).toExist();
    expect(donutChart).toHaveProp('percent', 60);
    expect(cells.at(3).childAt(1).text()).toEqual('12 / 20');
    expect(cells.at(4)).toHaveProp('chevron');
  });

  it('links to the application details page', function () {
    const tableRow = getShallowComponent().find(NxTableRow);
    tableRow.simulate('click');
    expect(stateGoSpy).toHaveBeenCalledWith('legal.applicationDetails', {
      applicationPublicId: 'app ID 1',
      stageTypeId: 'build',
    });
  });

  it('passes a 100 percentage in if there are no reviews', function () {
    const props = {
      row: {
        applicationId: 'appId1',
        applicationName: 'appName1',
        lastScanTime: 1607030429000,
        applicationTagNames: ['tag1', 'tag2'],
        componentsReviewedCount: 0,
        componentsTotalCount: 0,
      },
    };
    const wrapper = getShallowComponent(props);
    let tableRow = wrapper.find(NxTableRow);
    expect(tableRow).toExist();
    let cells = tableRow.find(NxTableCell);
    let donutChart = cells.at(3).find(LegalBinaryDonutChart);
    expect(donutChart).toExist();
    expect(donutChart).toHaveProp('percent', 0);
    expect(cells.at(3).childAt(1).text()).toEqual('0 / 0');
  });

  it('passes a 100 percentage in if completedCount is higher than totalCount', function () {
    const props = {
      row: {
        applicationId: 'appId1',
        applicationName: 'appName1',
        lastScanTime: 1607030429000,
        applicationTagNames: ['tag1', 'tag2'],
        componentsReviewedCount: 10,
        componentsTotalCount: 5,
      },
    };
    const wrapper = getShallowComponent(props);
    let tableRow = wrapper.find(NxTableRow);
    expect(tableRow).toExist();
    let cells = tableRow.find(NxTableCell);
    let donutChart = cells.at(3).find(LegalBinaryDonutChart);
    expect(donutChart).toExist();
    expect(donutChart).toHaveProp('percent', 100);
    expect(cells.at(3).childAt(1).text()).toEqual('10 / 5');
  });
});
