/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import ThreatDropdownSelector from 'MainRoot/react/ThreatDropdownSelector';

import 'TestRoot/SpecUtil';

describe('ThreatDropdownSelector', () => {
  let renderComponent, minimalProps, onSelectThreatLevelSpy;

  beforeEach(() => {
    onSelectThreatLevelSpy = jest.fn().mockName('onSelectThreatLevel');
    minimalProps = {
      onSelectThreatLevel: onSelectThreatLevelSpy,
    };
    renderComponent = (additionalProps) => render(<ThreatDropdownSelector {...minimalProps} {...additionalProps} />);
  });

  it('renders default label', () => {
    renderComponent();

    const button = screen.getByRole('button');

    expect(button).toHaveTextContent('Threat level');
  });

  it('renders threat label', () => {
    renderComponent({ threatLevel: 10 });

    const button = screen.getByRole('button');

    expect(button).toHaveTextContent('10 - Critical');
  });

  it('fires onSelectThreatLevel and hides dropdown', () => {
    renderComponent();

    let button = screen.getByRole('button');

    fireEvent.click(button);

    const threatButtons = screen.getAllByRole('button');
    expect(threatButtons.length).toBe(12);

    fireEvent.click(threatButtons[7]);

    button = screen.getByRole('button');
    expect(onSelectThreatLevelSpy).toHaveBeenCalledTimes(1);
    expect(onSelectThreatLevelSpy).toHaveBeenCalledWith(4);
  });

  it('renders with threat level zero excluded', () => {
    renderComponent({ excludeThreatLevelZero: true });

    const button = screen.getByRole('button');

    fireEvent.click(button);

    const threatButtons = screen.getAllByRole('button');
    expect(threatButtons.length).toBe(11);
    expect(threatButtons[10]).toHaveTextContent('1 - Low');
  });

  it('renders with threat level zero included as default', () => {
    renderComponent();

    const button = screen.getByRole('button');

    fireEvent.click(button);

    const threatButtons = screen.getAllByRole('button');
    expect(threatButtons.length).toBe(12);
    expect(threatButtons[11]).toHaveTextContent('0 - None');
  });

  it('renders with threat level zero included', () => {
    renderComponent({ excludeThreatLevelZero: false });

    const button = screen.getByRole('button');

    fireEvent.click(button);

    const threatButtons = screen.getAllByRole('button');
    expect(threatButtons.length).toBe(12);
    expect(threatButtons[11]).toHaveTextContent('0 - None');
  });
});
