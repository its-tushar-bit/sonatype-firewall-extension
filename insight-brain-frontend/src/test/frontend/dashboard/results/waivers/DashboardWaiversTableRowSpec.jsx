/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import { waiverMatcherStrategy } from 'MainRoot/util/waiverUtils';
import moment from 'moment';
import DashboardWaiversTableRow from 'MainRoot/dashboard/results/waivers/DashboardWaiversTableRow';

describe('DashboardWaiversTableRow', function () {
  const createTime = 1661485533772;
  const expiryTime = 1662094799999;
  let renderComponent, stateGoSpy, minimalProps;

  beforeEach(function () {
    stateGoSpy = jasmine.createSpy('stateGo');
    minimalProps = {
      stateGo: stateGoSpy,
      waiver: {
        id: 'waiverId',
        threatLevel: 8,
        createTime,
        expiryTime,
        policyName: 'policyName',
        ownerId: 'ownerId',
        ownerName: 'ownerName',
        ownerType: 'organization',
        scope: 'Organization - ownerName',
        associatedPackageUrl: 'a/package/url',
        matcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
        displayName: {
          parts: [
            {
              field: 'Group',
              value: 'test-group',
            },
            {
              value: ':',
            },
            {
              field: 'Artifact',
              value: 'test-artifact',
            },
            {
              value: ':',
            },
            {
              field: 'Version',
              value: '1.2.3',
            },
          ],
        },
      },
    };
    renderComponent = (additionalProps = {}) => render(<DashboardWaiversTableRow {...additionalProps} />);
  });

  it('renders all the waiver info', () => {
    renderComponent(minimalProps);

    expect(screen.getByLabelText('threat level critical')).toBeVisible();
    expect(screen.getByText('8')).toBeVisible();
    expect(screen.getByText(moment(createTime).format('YYYY-MM-DD'))).toBeVisible();
    expect(screen.getByText(moment(expiryTime).format('YYYY-MM-DD'))).toBeVisible();
    expect(screen.getByText('policyName')).toBeVisible();
    expect(screen.getByText('Organization - ownerName')).toBeVisible();
    expect(screen.getByText('test-group:test-artifact:1.2.3')).toBeVisible();
  });

  it('links on click to the waiver details state', async () => {
    const { container } = await renderComponent(minimalProps);

    fireEvent.click(container.children[0]);

    expect(stateGoSpy).toHaveBeenCalledWith('waiver.details', {
      waiverId: 'waiverId',
      ownerId: 'ownerId',
      ownerType: 'organization',
      type: 'waiver',
      sidebarReference: 'filter',
    });
  });

  it('renders waiver scope', () => {
    minimalProps.waiver.scope = 'my scope';
    renderComponent(minimalProps);

    expect(screen.getByText('my scope')).toBeVisible();
  });

  it('renders a row with expiration time set to never', () => {
    minimalProps.waiver.expiryTime = null;
    renderComponent(minimalProps);

    expect(screen.getByText('Never')).toBeVisible();
  });

  it('renders a row with threat level severe', () => {
    minimalProps.waiver.threatLevel = 5;
    renderComponent(minimalProps);

    expect(screen.getByLabelText('threat level severe')).toBeVisible();
    expect(screen.getByText('5')).toBeVisible();
  });

  it('renders a row with threat level moderate', () => {
    minimalProps.waiver.threatLevel = 3;
    renderComponent(minimalProps);

    expect(screen.getByLabelText('threat level moderate')).toBeVisible();
    expect(screen.getByText('3')).toBeVisible();
  });

  it('renders a row with threat level low', () => {
    minimalProps.waiver.threatLevel = 1;
    renderComponent(minimalProps);

    expect(screen.getByLabelText('threat level low')).toBeVisible();
    expect(screen.getByText('1')).toBeVisible();
  });

  it('renders a row with threat level none', () => {
    minimalProps.waiver.threatLevel = 0;
    renderComponent(minimalProps);

    expect(screen.getByLabelText('threat level none')).toBeVisible();
    expect(screen.getByText('0')).toBeVisible();
  });
});
