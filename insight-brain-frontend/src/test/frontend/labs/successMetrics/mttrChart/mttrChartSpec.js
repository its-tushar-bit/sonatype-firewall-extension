/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import MttrChart from '../../../../../main/frontend/labs/successMetrics/successMetricsReport/mttrChart/MttrChart';
import { getShallowComponent } from '../../../enzymeUtils';

describe('mttrChart', () => {
  let activeApplicationCount, monthCount, mttrs, component;

  beforeEach(() => {
    activeApplicationCount = 7;
    monthCount = 3;
    mttrs = [
      {
        timePeriodName: 'Sep',
        mttrInSeconds: 1309714,
        criticalMttrInSeconds: 129714,
      },
      {
        timePeriodName: 'Oct',
        mttrInSeconds: 1299714,
        criticalMttrInSeconds: 1299714,
      },
      {
        timePeriodName: 'Nov',
        mttrInSeconds: 1289714,
        criticalMttrInSeconds: 1209714,
      },
      {
        timePeriodName: 'Dec',
        mttrInSeconds: null,
        criticalMttrInSeconds: null,
      },
      {
        timePeriodName: 'Jan',
        mttrInSeconds: null,
        criticalMttrInSeconds: null,
      },
      {
        timePeriodName: 'Feb',
        mttrInSeconds: 384000,
        criticalMttrInSeconds: 384000,
      },
      {
        timePeriodName: 'Mar',
        mttrInSeconds: 384000,
        criticalMttrInSeconds: 384000,
      },
      {
        timePeriodName: 'Apr',
        mttrInSeconds: null,
        criticalMttrInSeconds: null,
      },
      {
        timePeriodName: 'May',
        mttrInSeconds: null,
        criticalMttrInSeconds: null,
      },
      {
        timePeriodName: 'Jun',
        mttrInSeconds: 1209714,
        criticalMttrInSeconds: 1209714,
      },
      {
        timePeriodName: 'Jul',
        mttrInSeconds: 484000,
        criticalMttrInSeconds: 484000,
      },
      {
        timePeriodName: 'Aug',
        mttrInSeconds: null,
        criticalMttrInSeconds: null,
      },
    ];

    const getShallow = getShallowComponent(MttrChart, { activeApplicationCount, monthCount, mttrs });
    component = getShallow();
  });

  it('renders description', () => {
    const description = component.find('.nx-tile-header__subtitle');
    expect(description).toHaveText(
      ` This data represents the average age of violations that were resolved each month in ${activeApplicationCount} application${
        activeApplicationCount === 1 ? '' : 's'
      } over the past ${monthCount} month${
        monthCount === 1 ? '' : 's'
      }. A violation that does not reappear in a subsequent evaluation is considered resolved.`
    );
  });
  it('renders chart container', () => {
    expect(component.find('#mttr-chart-container')).toExist();
  });
});
