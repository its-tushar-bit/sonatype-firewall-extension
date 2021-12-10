/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import ActiveWaiversIndicator from 'MainRoot/violation/ActiveWaiversIndicator';
import { render, screen } from 'TestRoot/SpecUtil';

describe('ActiveWaiversIndicator', function () {
  let minimalProps, renderComponent;
  const activeTextPlural = 'Active Waivers',
    activeTextSingular = 'Active Waiver',
    unappliedTextSingular = 'Unapplied Waiver',
    inactiveClass = 'iq-waiver-indicator--inactive',
    activeClass = 'iq-waiver-indicator--active';

  beforeEach(function () {
    minimalProps = {
      activeWaiverCount: 0,
      waived: true,
      showUnapplied: false,
    };
    renderComponent = (additionalProps) =>
      render(<ActiveWaiversIndicator {...minimalProps} {...additionalProps} />, minimalProps);
  });

  it('renders as inactive when there are no active waivers for the violation', function () {
    renderComponent();

    expect(screen.getByText('0')).toBeVisible();
    expect(screen.getByText(activeTextPlural)).toBeVisible();
    expect(screen.getByText(activeTextPlural).closest('div')).toHaveClassName(inactiveClass);
    expect(screen.getByText(activeTextPlural).closest('div')).not.toHaveClassName(activeClass);
  });

  it('renders as active and singular when there is only one active waiver for the violation', function () {
    renderComponent({
      activeWaiverCount: 1,
    });

    expect(screen.getByText('1')).toBeVisible();
    expect(screen.getByText(activeTextSingular)).toBeVisible();
    expect(screen.getByText(activeTextSingular).closest('div')).not.toHaveClassName(inactiveClass);
    expect(screen.getByText(activeTextSingular).closest('div')).toHaveClassName(activeClass);
  });

  it('renders as unapplied when waived is false and showUnapplied is set', function () {
    renderComponent({
      activeWaiverCount: 1,
      waived: false,
      showUnapplied: true,
    });
    expect(screen.getByText(unappliedTextSingular)).toBeVisible();
    expect(screen.getByText(unappliedTextSingular).closest('div')).not.toHaveClassName(inactiveClass);
    expect(screen.getByText(unappliedTextSingular).closest('div')).not.toHaveClassName(activeClass);
  });

  it('renders as active when waived is false and showUnapplied is false', function () {
    renderComponent({
      activeWaiverCount: 1,
      waived: false,
    });

    expect(screen.getByText('1')).toBeVisible();
    expect(screen.getByText(activeTextSingular)).toBeVisible();
    expect(screen.getByText(activeTextSingular).closest('div')).not.toHaveClassName(inactiveClass);
    expect(screen.getByText(activeTextSingular).closest('div')).toHaveClassName(activeClass);
  });

  it('renders as active and plural when there is more than one active waiver for the violation', function () {
    renderComponent({
      activeWaiverCount: 2,
    });

    expect(screen.getByText('2')).toBeVisible();
    expect(screen.getByText(activeTextPlural)).toBeVisible();
    expect(screen.getByText(activeTextPlural).closest('div')).not.toHaveClassName(inactiveClass);
    expect(screen.getByText(activeTextPlural).closest('div')).toHaveClassName(activeClass);
  });
});
