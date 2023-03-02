/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import moment from 'moment';
import ComponentRiskItemTable from 'MainRoot/dashboard/results/componentRisk/componentRiskItem/ComponentRiskItemTable';

describe('ComponentRiskItemTable', () => {
  let renderComponent, minimalProps;

  beforeEach(() => {
    minimalProps = {
      publicId: 'testId',
      totalRisk: 150,
      policyViolations: [
        {
          policyId: 'testPolicyId1',
          policyName: 'testPolicyName1',
          threatLevel: 10,
          time: moment().subtract(1, 'hours').valueOf(),
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
        },
        {
          policyId: 'testPolicyId2',
          policyName: 'testPolicyName2',
          threatLevel: 1,
          time: moment().subtract(2, 'hours').valueOf(),
          stageDetails: [
            {
              time: moment().subtract(2, 'hours').valueOf(),
              scanId: 'testScanId',
              stageTypeName: 'SOURCE',
            },
            {
              time: moment().subtract(2, 'days').valueOf(),
              scanId: 'testScanId',
              stageTypeName: 'BUILD',
            },
            {
              time: moment().subtract(2, 'months').valueOf(),
              scanId: 'testScanId',
              stageTypeName: 'STAGE RELEASE',
            },
            {
              time: moment().subtract(2, 'years').valueOf(),
              scanId: 'testScanId',
              stageTypeName: 'RELEASE',
            },
            {
              time: null,
              scanId: 'testScanId',
              stageTypeName: 'OPERATE',
            },
          ],
        },
      ],
    };

    renderComponent = (additionalProps = {}) =>
      render(<ComponentRiskItemTable {...minimalProps} {...additionalProps} />);
  });

  xit('renders table and rows with data for each violation', () => {
    renderComponent();

    const rows = screen.getAllByRole('row');
    const firstRowThreatAndRisk = screen.getAllByText('10');
    const secondRowThreatAndRisk = screen.getAllByText('1');

    // table headers
    expect(screen.getByText('Threat')).toBeVisible();
    expect(screen.getByText('Threat Policy')).toBeVisible();
    expect(screen.getByText('Share of Risk')).toBeVisible();
    expect(screen.getByText('Risk')).toBeVisible();
    expect(screen.getByText('Source')).toBeVisible();
    expect(screen.getByText('Build')).toBeVisible();
    expect(screen.getByText('Release')).toBeVisible();
    expect(screen.getByText('Operate')).toBeVisible();

    // total rows (application row and two violation rows)
    expect(rows.length).toEqual(3);

    // first row
    expect(screen.getByText('testPolicyName1')).toBeVisible();
    expect(firstRowThreatAndRisk.length).toEqual(2);
    expect(screen.getByText('7%')).toBeVisible();
    expect(screen.getByText('3h')).toBeVisible();
    expect(screen.getByText('3d')).toBeVisible();
    expect(screen.getByText('3mo')).toBeVisible();
    expect(screen.getByText('3y')).toBeVisible();

    // second row
    expect(screen.getByText('testPolicyName2')).toBeVisible();
    expect(secondRowThreatAndRisk.length).toEqual(2);
    expect(screen.getByText('< 1%')).toBeVisible();
    expect(screen.getByText('2h')).toBeVisible();
    expect(screen.getByText('2d')).toBeVisible();
    expect(screen.getByText('1mo')).toBeVisible();
    expect(screen.getByText('2y')).toBeVisible();
  });
});
