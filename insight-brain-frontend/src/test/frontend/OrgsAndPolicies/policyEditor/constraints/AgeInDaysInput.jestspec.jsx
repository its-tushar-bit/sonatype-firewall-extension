/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import AgeInDaysInput from 'MainRoot/OrgsAndPolicies/policyEditor/constraints/AgeInDaysInput';

import 'TestRoot/SpecUtil';

describe('AgeInDaysInput', () => {
  let renderComponent, onChangeSpy;

  beforeEach(() => {
    onChangeSpy = jest.fn().mockName('onChangeSpy');
    const minimalProps = {
      id: '202',
      onChange: onChangeSpy,
      ageInDays: {
        isPristine: true,
        trimmedValue: '1095',
        validationErrors: null,
        value: '1095',
      },
    };
    renderComponent = (additionalProps) => render(<AgeInDaysInput {...minimalProps} {...additionalProps} />);
  });

  it('renders input with converted value and dropdown', () => {
    renderComponent();
    expect(screen.getByDisplayValue('3')).toBeInTheDocument();
    expect(screen.getByDisplayValue('years')).toBeInTheDocument();
  });

  it('recalculates input value on select value change', () => {
    const weeksModifier = 7;
    const expectedConvertedValue = (1095 % weeksModifier) * weeksModifier;
    renderComponent();

    fireEvent.change(screen.getByRole('combobox'), { target: { value: weeksModifier } });

    expect(onChangeSpy).toHaveBeenCalledWith(String(expectedConvertedValue));
  });

  it('calls onChange handler on input value change', () => {
    renderComponent();
    const yearModifier = 365;
    const expectedConvertedValue = yearModifier * 2;

    fireEvent.change(screen.getByRole('textbox'), { target: { value: '2' } });

    expect(onChangeSpy).toHaveBeenCalledWith(String(expectedConvertedValue));
  });
});
