/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import {
  NxTableCell,
  NxTableRow,
  NxBinaryDonutChart,
  NxFontAwesomeIcon,
  NxThreatIndicator,
} from '@sonatype/react-shared-components';
import LegalDashboardComponentRow from '../../../../main/frontend/legal/dashboard/LegalDashboardComponentRow';
import { faCheckCircle } from '@fortawesome/pro-solid-svg-icons';

describe('LegalDashboardComponentRow component', function () {
  let getMountedComponent;

  const minimalProps = {
    row: {
      applicationOccurrences: 1,
      displayName: 'Component Name',
      hash: '8c5c838e0c6d2f6cdf30',
      licenseNames: ['Apache-2.0', 'GPL 1'],
      reviewCompletedCount: 12,
      reviewTotalCount: 20,
      licenses: [
        {
          licenseId: 'Apache-2.0',
          licenseName: 'Apache-2.0',
          licenseThreatGroups: [
            {
              licenseThreatGroupName: 'Liberal',
              licenseThreatGroupLevel: 0,
              licenseThreatGroupCategory: 'no-threat',
            },
          ],
        },
      ],
    },
  };

  beforeEach(function () {
    getMountedComponent = enzymeUtils.getMountedComponent(LegalDashboardComponentRow, minimalProps);
  });

  it('renders a NxTableRow with appropriate cells', function () {
    const wrapper = getMountedComponent();
    let tableRow = wrapper.find(NxTableRow);
    expect(tableRow).toExist();
    let cells = tableRow.find(NxTableCell);
    expect(cells.length).toEqual(5);
    expect(cells.at(0).children().text()).toEqual('Component Name');
    expect(cells.at(1).children().text()).toEqual('Apache-2.0, GPL 1');
    expect(cells.at(1).children().find(NxThreatIndicator).prop('policyThreatLevel')).toEqual(0);
    expect(cells.at(2).children().text()).toEqual('1');
    let donutChart = cells.at(3).find(NxBinaryDonutChart);
    expect(donutChart).toExist();
    expect(donutChart).toHaveProp('percent', 60);
    expect(cells.at(3).at(0).text()).toEqual('12 / 20');
    expect('chevron' in cells.at(4).props()).toEqual(true);
  });

  it('passes a 0 percentage if there are no reviews', function () {
    const props = {
      row: {
        applicationOccurrences: 1,
        displayName: 'Component Name',
        hash: '8c5c838e0c6d2f6cdf30',
        licenseNames: ['Apache-2.0', 'GPL 1'],
        reviewCompletedCount: 0,
        reviewTotalCount: 0,
      },
    };
    const wrapper = getMountedComponent(props);
    let tableRow = wrapper.find(NxTableRow);
    expect(tableRow).toExist();
    let cells = tableRow.find(NxTableCell);
    let donutChart = cells.at(3).find(NxBinaryDonutChart);
    expect(donutChart).toExist();
    expect(donutChart).toHaveProp('percent', 0);
    expect(cells.at(3).at(0).text()).toEqual('0 / 0');
  });

  it('passes a 100 percentage if completedCount is equal than totalCount', function () {
    const props = {
      row: {
        applicationOccurrences: 1,
        displayName: 'Component Name 1',
        hash: '8c5c838e0c6d2f6cdf31',
        licenseNames: ['Apache-2.0', 'GPL 1'],
        reviewCompletedCount: 5,
        reviewTotalCount: 5,
      },
    };
    const wrapper = getMountedComponent(props);
    let tableRow = wrapper.find(NxTableRow);
    expect(tableRow).toExist();
    let cells = tableRow.find(NxTableCell);
    let donutChart = cells.at(3).find(NxFontAwesomeIcon);
    expect(donutChart).toExist();
    expect(donutChart).toHaveProp('icon', faCheckCircle);
  });
});
