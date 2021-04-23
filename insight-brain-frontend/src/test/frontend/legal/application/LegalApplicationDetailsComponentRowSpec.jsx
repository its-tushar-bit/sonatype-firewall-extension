/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import LegalApplicationDetailsComponentRow from '../../../../main/frontend/legal/application/LegalApplicationDetailsComponentRow';
import { NxBinaryDonutChart, NxTableCell, NxTableRow, NxThreatIndicator } from '@sonatype/react-shared-components';

describe('LegalApplicationDetailsComponentRow component', function () {
  let getShallowComponent;
  const stateGoSpy = jasmine.createSpy('stateGo');

  const minimalProps = {
    applicationPublicId: 'app-id',
    stageTypeId: 'stage-id',
    row: {
      displayName: 'g : a : v',
      hash: 'some-hash',
      reviewCompletedCount: 0,
      reviewStatus: 'COMPLETED',
      reviewTotalCount: 0,
    },
    stateGo: stateGoSpy,
  };

  beforeEach(function () {
    getShallowComponent = enzymeUtils.getShallowComponent(LegalApplicationDetailsComponentRow, minimalProps);
  });

  it('renders a NxTableRow with appropriate cells', function () {
    const wrapper = getShallowComponent();
    const tableRow = wrapper.find(NxTableRow);
    expect(tableRow).toExist();
    expect(tableRow).toHaveProp('isClickable', true);
    expect(tableRow).toHaveProp('onClick', jasmine.any(Function));
    const cells = tableRow.find(NxTableCell);
    expect(cells.length).toEqual(4);
    expect(cells.at(0).children().text()).toEqual('g : a : v');
    expect(cells.at(1).children().length).toEqual(0);
    expect(cells.at(2).children().length).toEqual(0);
    expect(cells.at(3).children().text()).toEqual('Completed');
    expect(cells.at(3)).toHaveClassName('status-COMPLETED');
  });

  it('links to the component overview page', function () {
    const tableRow = getShallowComponent().find(NxTableRow);
    tableRow.simulate('click');
    expect(stateGoSpy).toHaveBeenCalledWith('legal.applicationStageTypeComponentOverview', {
      applicationPublicId: 'app-id',
      stageTypeId: 'stage-id',
      hash: 'some-hash',
    });
  });

  it('renders licenses review progress indicators for component with licenses and obligations', function () {
    const props = {
      row: {
        displayName: 'g : a : v',
        hash: 'some-hash',
        licenses: [
          {
            licenseId: 'license-1',
            licenseName: 'License 1',
            licenseThreatGroups: [
              {
                licenseThreatGroupCategory: 'testA',
                licenseThreatGroupLevel: 5,
                licenseThreatGroupName: 'test-group-a',
              },
            ],
          },
          {
            licenseId: 'license-2',
            licenseName: 'License 2',
            licenseThreatGroups: [
              {
                licenseThreatGroupCategory: 'testB',
                licenseThreatGroupLevel: 9,
                licenseThreatGroupName: 'test-group-b',
              },
            ],
          },
        ],
        reviewCompletedCount: 4,
        reviewStatus: 'FLAGGED',
        reviewTotalCount: 10,
      },
    };

    const wrapper = getShallowComponent(props);
    const tableRow = wrapper.find(NxTableRow);
    const cells = tableRow.find(NxTableCell);
    expect(cells.at(1).children().find(NxThreatIndicator).prop('policyThreatLevel')).toEqual(9);
    expect(cells.at(1).childAt(0).childAt(1).text()).toEqual('License 1, License 2');
    const donutChart = cells.at(2).find(NxBinaryDonutChart);
    expect(donutChart).toExist();
    expect(donutChart).toHaveProp('percent', 40);
    expect(cells.at(2).find('span').text()).toEqual('4 / 10');
  });

  it('renders blank review progress indicators for component with licenses and no obligations', function () {
    const props = {
      row: {
        displayName: 'g : a : v',
        hash: 'some-hash',
        licenses: [
          {
            licenseId: 'license-1',
            licenseName: 'License 1',
          },
        ],
        reviewCompletedCount: 4,
        reviewStatus: 'COMPLETED',
        reviewTotalCount: 0,
      },
    };

    const wrapper = getShallowComponent(props);
    const tableRow = wrapper.find(NxTableRow);
    const cells = tableRow.find(NxTableCell);
    expect(cells.at(1).children().find(NxThreatIndicator).prop('policyThreatLevel')).toEqual(0);
    expect(cells.at(1).childAt(0).childAt(1).text()).toEqual('License 1');
    const donutChart = cells.at(2).find(NxBinaryDonutChart);
    expect(donutChart).toExist();
    expect(donutChart).toHaveProp('percent', 100);
    expect(cells.at(2).find('span').text()).toEqual('- / -');
  });

  it('passes a 100 percentage in the edge case reviewCompletedCount is higher than reviewTotalCount', function () {
    const props = {
      row: {
        displayName: 'g : a : v',
        hash: 'some-hash',
        licenses: [
          {
            licenseId: 'license-1',
            licenseName: 'License 1',
          },
        ],
        reviewCompletedCount: 100,
        reviewStatus: 'COMPLETED',
        reviewTotalCount: 5,
      },
    };

    const wrapper = getShallowComponent(props);
    const cells = wrapper.find(NxTableCell);
    const donutChart = cells.at(2).find(NxBinaryDonutChart);
    expect(donutChart).toHaveProp('percent', 100);
    expect(cells.at(2).find('span').text()).toEqual('100 / 5');
  });
});
