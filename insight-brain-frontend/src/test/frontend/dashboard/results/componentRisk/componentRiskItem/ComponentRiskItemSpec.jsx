/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import moment from 'moment';
import ComponentRiskItem from 'MainRoot/dashboard/results/componentRisk/componentRiskItem/ComponentRiskItem';

describe('ComponentRiskListItem', () => {
  let renderComponent, minimalProps;

  beforeEach(() => {
    minimalProps = {
      totalRisk: 100,
      application: {
        name: 'test',
        publicId: 'testId',
      },
      risk: 50,
      policyViolations: [],
      stageDetails: [
        {
          time: moment().subtract(3, 'hours').valueOf(),
          scanId: 'testScanId',
          stageTypeName: 'SOURCE',
        },
        {
          time: moment().subtract(3, 'days').valueOf(),
          scanId: 'testScanId',
          stageTypeName: 'BUILD',
        },
        {
          time: moment().subtract(3, 'months').valueOf(),
          scanId: 'testScanId',
          stageTypeName: 'STAGE RELEASE',
        },
        {
          time: moment().subtract(3, 'years').valueOf(),
          scanId: 'testScanId',
          stageTypeName: 'RELEASE',
        },
        {
          time: null,
          scanId: 'testScanId',
          stageTypeName: 'OPERATE',
        },
      ],
    };

    renderComponent = (additionalProps = {}) => render(<ComponentRiskItem {...minimalProps} {...additionalProps} />);
  });

  it('renders list item with data and titles', () => {
    renderComponent();

    expect(screen.getByText('SHARE OF RISK')).toBeVisible();
    expect(screen.getByText('50%')).toBeVisible();
    expect(screen.getByText('RISK')).toBeVisible();
    expect(screen.getByText('50')).toBeVisible();
    expect(screen.getByText('SOURCE')).toBeVisible();
    expect(screen.getByText('3h')).toBeVisible();
    expect(screen.getByText('BUILD')).toBeVisible();
    expect(screen.getByText('3d')).toBeVisible();
    expect(screen.getByText('STAGE RELEASE')).toBeVisible();
    expect(screen.getByText('3mo')).toBeVisible();
    expect(screen.getByText('RELEASE')).toBeVisible();
    expect(screen.getByText('3y')).toBeVisible();
    expect(screen.getByText('OPERATE')).toBeVisible();
  });
});
