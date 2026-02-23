/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import moment from 'moment';
import ComponentRisk from 'MainRoot/dashboard/results/componentRisk/ComponentRisk';
import { actions } from 'MainRoot/dashboard/results/componentRisk/componentRiskSlice';
import * as componentRiskSelectors from 'MainRoot/dashboard/results/componentRisk/componentRiskSelectors';

import 'TestRoot/SpecUtil';

describe('ComponentRisk', () => {
  let renderComponent, loadDetailsAndComponentsSpy, componentRiskProps, componentRiskSelectorsSpy;

  beforeEach(() => {
    componentRiskProps = {
      loading: false,
      loadError: null,
      componentName: 'Test component name',
      totalRisk: 50,
      applicationComponents: [
        {
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
              time: null,
              scanId: 'testScanId',
              stageTypeName: 'BUILD',
            },
            {
              time: null,
              scanId: 'testScanId',
              stageTypeName: 'STAGE RELEASE',
            },
            {
              time: null,
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

    componentRiskSelectorsSpy = jest
      .spyOn(componentRiskSelectors, 'selectComponentRisk')
      .mockReturnValue(componentRiskProps);
    loadDetailsAndComponentsSpy = jest.spyOn(actions, 'loadDetailsAndComponents');

    renderComponent = (additionalProps = {}) => render(<ComponentRisk {...additionalProps} />);
  });

  it('render component risk details', () => {
    renderComponent();

    // Each component risk item is its own section
    const listItems = screen.getAllByRole('region');

    expect(screen.getByText('Test component name')).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Total risk: 50' })).toBeVisible();
    expect(screen.getByText('Risk score by application')).toBeVisible();
    expect(listItems.length).toEqual(1);
  });

  it('calls loadDetailsAndComponents once', () => {
    renderComponent();

    expect(loadDetailsAndComponentsSpy).toHaveBeenCalledTimes(1);

    componentRiskProps.loading = true;
    componentRiskSelectorsSpy.mockReturnValue(componentRiskProps);
    renderComponent();

    expect(loadDetailsAndComponentsSpy).toHaveBeenCalledTimes(1);
  });
});
