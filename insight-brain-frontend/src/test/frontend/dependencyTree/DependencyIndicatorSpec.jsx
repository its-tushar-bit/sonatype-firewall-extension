/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, fireEvent, within } from '../SpecUtil';

import DependencyIndicator from 'MainRoot/DependencyTree/DependencyIndicator';

describe('DependencyIndicator ', () => {
  let renderComponent;
  beforeEach(() => {
    renderComponent = (additionalProps) => render(<DependencyIndicator {...additionalProps} />);
  });

  const testCases = [
    { type: 'direct', label: 'D', toolTipTitle: 'Direct' },
    { type: 'transitive', label: 'T', toolTipTitle: 'Transitive' },
    { type: 'inner-source', label: 'IS', toolTipTitle: 'InnerSource' },
  ];

  testCases.forEach(({ type, label, toolTipTitle }) => {
    it(`renders a label for dependency type: '${type}'`, () => {
      renderComponent({ type });

      expect(screen.getByText(label)).toBeVisible();
    });

    it(`renders a tooltip for dependency type: '${type}'`, async () => {
      renderComponent({ type });
      fireEvent.mouseOver(screen.getByText(label));
      const tooltip = await screen.findByRole('tooltip');

      expect(within(tooltip).getByText(toolTipTitle)).toBeInTheDocument();
    });

    it(`renders a class for dependency type: '${type}'`, () => {
      renderComponent({ type });

      expect(screen.getByText(label).closest('div')).toHaveClassName(type);
    });
  });

  it('renders null for unknown dependency types', () => {
    const { container } = renderComponent();

    expect(container).toBeEmptyDOMElement();
  });
});
