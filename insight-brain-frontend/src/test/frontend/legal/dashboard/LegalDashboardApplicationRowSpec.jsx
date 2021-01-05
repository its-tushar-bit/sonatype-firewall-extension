/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import { NxBinaryDonutChart, NxTableCell, NxTableRow } from '@sonatype/react-shared-components';
import LegalDashboardApplicationRow from '../../../../main/frontend/legal/dashboard/LegalDashboardApplicationRow';
import moment from 'moment';

describe('LegalDashboardApplicationRow component', function() {

  let getShallowComponent;

  const minimalProps = {
    row: {
      applicationId: 'appId1',
      applicationName: 'appName1',
      lastScanTime: 1607030429000,
      applicationTagNames: ['tag1', 'tag2'],
      reviewCompletedCount: 12,
      reviewTotalCount: 20
    }
  };

  const baselineDate = moment('2020-12-05T19:56:17.509+0000', 'YYYY-MM-DDThh:mm:ss.SSS+0000');
  const currentDate = moment.now();

  beforeEach(function() {
    getShallowComponent = enzymeUtils.getShallowComponent(LegalDashboardApplicationRow, minimalProps);
    moment.now = () => baselineDate;
  });

  afterEach(() => moment.now = () => currentDate);

  it('renders a NxTableRow with appropriate cells', function() {

    const wrapper = getShallowComponent();
    let tableRow = wrapper.find(NxTableRow);
    expect(tableRow).toExist();
    let cells = tableRow.find(NxTableCell);
    expect(cells.length).toEqual(4);
    expect(cells.at(0).children().text()).toEqual('appName1');
    expect(cells.at(1).children().text()).toEqual('2 days ago');
    expect(cells.at(2).children().text()).toEqual('tag1, tag2');
    let donutChart = cells.at(3).find(NxBinaryDonutChart);
    expect(donutChart).toExist();
    expect(donutChart).toHaveProp('percent', 60);
    expect(cells.at(3).childAt(1).text()).toEqual('12 / 20');
  });

  it('passes a 0 percentage in if there are no reviews', function() {
    const props = {
      row: {
        applicationId: 'appId1',
        applicationName: 'appName1',
        lastScanTime: 1607030429000,
        applicationTagNames: ['tag1', 'tag2'],
        reviewCompletedCount: 0,
        reviewTotalCount: 0
      }
    };
    const wrapper = getShallowComponent(props);
    let tableRow = wrapper.find(NxTableRow);
    expect(tableRow).toExist();
    let cells = tableRow.find(NxTableCell);
    let donutChart = cells.at(3).find(NxBinaryDonutChart);
    expect(donutChart).toExist();
    expect(donutChart).toHaveProp('percent', 0);
    expect(cells.at(3).childAt(1).text()).toEqual('0 / 0');
  });

  it('passes a 100 percentage in if completedCount is higher than totalCount', function() {
    const props = {
      row: {
        applicationId: 'appId1',
        applicationName: 'appName1',
        lastScanTime: 1607030429000,
        applicationTagNames: ['tag1', 'tag2'],
        reviewCompletedCount: 10,
        reviewTotalCount: 5
      }
    };
    const wrapper = getShallowComponent(props);
    let tableRow = wrapper.find(NxTableRow);
    expect(tableRow).toExist();
    let cells = tableRow.find(NxTableCell);
    let donutChart = cells.at(3).find(NxBinaryDonutChart);
    expect(donutChart).toExist();
    expect(donutChart).toHaveProp('percent', 100);
    expect(cells.at(3).childAt(1).text()).toEqual('10 / 5');
  });
});
