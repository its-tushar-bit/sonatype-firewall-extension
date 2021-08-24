/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ViolationsByCategoryChart from '../../../../../main/frontend/labs/successMetrics/successMetricsReport/violationsByCategoryChart/ViolationsByCategoryChart';
import { getShallowComponent } from '../../../enzymeUtils';

describe('violationsByCategoryChart', () => {
  let violationsByCategoryData, component;

  beforeEach(() => {
    violationsByCategoryData = [
      {
        timePeriodName: '9 Apr',
        security: null,
        license: null,
        quality: null,
        other: null,
      },
      {
        timePeriodName: '16 Apr',
        security: null,
        license: null,
        quality: null,
        other: null,
      },
      {
        timePeriodName: '23 Apr',
        security: null,
        license: null,
        quality: null,
        other: null,
      },
      {
        timePeriodName: '30 Apr',
        security: null,
        license: null,
        quality: null,
        other: null,
      },
      {
        timePeriodName: '7 May',
        security: null,
        license: null,
        quality: null,
        other: null,
      },
      {
        timePeriodName: '14 May',
        security: null,
        license: null,
        quality: null,
        other: null,
      },
      {
        timePeriodName: '21 May',
        security: null,
        license: null,
        quality: null,
        other: null,
      },
      {
        timePeriodName: '28 May',
        security: null,
        license: null,
        quality: null,
        other: null,
      },
      {
        timePeriodName: '4 Jun',
        security: null,
        license: null,
        quality: null,
        other: null,
      },
      {
        timePeriodName: '11 Jun',
        security: 5,
        license: 4,
        quality: 2,
        other: 1,
      },
      {
        timePeriodName: '18 Jun',
        security: 7,
        license: 5,
        quality: 1,
        other: 0,
      },
      {
        timePeriodName: '25 Jun',
        security: 0,
        license: 0,
        quality: 0,
        other: 1,
      },
    ];

    const getShallow = getShallowComponent(ViolationsByCategoryChart, { violationsByCategoryData });
    component = getShallow();
  });

  it('renders description', () => {
    const description = component.find('.nx-tile-header__subtitle');
    expect(description).toHaveText(`Open violations over the past 3 weeks by policy type.`);
  });
  it('renders chart container', () => {
    expect(component.find('#bycategory-chart-container')).toExist();
  });
});
