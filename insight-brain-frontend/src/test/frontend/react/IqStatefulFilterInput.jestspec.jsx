/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import IqStatefulFilterInput from 'MainRoot/react/IqStatefulFilterInput';

import 'TestRoot/SpecUtil';

describe('IqStatefulFilterInput', () => {
  let renderComponent, minimalProps, onChangeSpy;

  beforeEach(() => {
    onChangeSpy = jest.fn().mockName('onChange');
    minimalProps = {
      onChange: onChangeSpy,
    };
    renderComponent = () => render(<IqStatefulFilterInput {...minimalProps} />);
  });

  it('calls onChange', () => {
    const inputValue = 'value';
    renderComponent();

    const input = screen.getByRole('textbox');
    fireEvent.change(input, { target: { value: inputValue } });

    expect(screen.getByDisplayValue(inputValue)).toBeVisible();
    expect(onChangeSpy).toHaveBeenCalledTimes(1);
    expect(onChangeSpy).toHaveBeenCalledWith(inputValue);
  });

  it('renders default value if provided', () => {
    const defaultValue = 'defaultValue';
    renderComponent({ defaultValue });

    const input = screen.getByRole('textbox');
    fireEvent.change(input, { target: { value: defaultValue } });

    expect(screen.getByDisplayValue(defaultValue)).toBeVisible();
  });
});
