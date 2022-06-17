/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import ThreatDropdownSelector from 'MainRoot/react/ThreatDropdownSelector';

describe('ThreatDropdownSelector', () => {
  let renderComponent, minimalProps, onSelectThreatLevelSpy;

  beforeEach(() => {
    onSelectThreatLevelSpy = jasmine.createSpy('onSelectThreatLevel');
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

    expect(button).toHaveTextContent('Critical 10 - Critical');
  });

  it('fires onSelectThreatLevel and hides dropdown', () => {
    renderComponent();

    let button = screen.getByRole('button');

    fireEvent.click(button);

    const threatButtons = screen.getAllByRole('button');
    expect(threatButtons.length).toBe(12);

    fireEvent.click(threatButtons[7]);

    button = screen.getByRole('button');
    expect(onSelectThreatLevelSpy).toHaveBeenCalledOnceWith(4);
  });
});
