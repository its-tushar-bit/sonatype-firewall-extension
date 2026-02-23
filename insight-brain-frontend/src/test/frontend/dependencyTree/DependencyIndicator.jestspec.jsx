/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, fireEvent, within } from '../SpecUtil';

import DependencyIndicator from 'MainRoot/DependencyTree/DependencyIndicator';

import 'TestRoot/SpecUtil';

describe('DependencyIndicator ', () => {
  let renderComponent;
  beforeEach(() => {
    renderComponent = (additionalProps) => render(<DependencyIndicator {...additionalProps} />);
  });

  const testCases = [
    { type: 'direct', label: 'D', toolTipTitle: 'Direct Dependency' },
    { type: 'transitive', label: 'T', toolTipTitle: 'Transitive Dependency' },
    { type: 'inner-source', label: 'IS', toolTipTitle: 'InnerSource' },
  ];

  testCases.forEach(({ type, label, toolTipTitle }) => {
    it(`renders a label for dependency type: '${type}'`, () => {
      renderComponent({ type });

      expect(screen.getByText(label)).toBeVisible();
    });

    it(`renders a tooltip for dependency type: '${type}'`, async () => {
      SpecUtil.requestIdleCallbackInvokeImmediateJest();

      renderComponent({ type });
      fireEvent.mouseOver(screen.getByText(label));
      const tooltip = await screen.findByRole('tooltip');

      expect(within(tooltip).getByText(toolTipTitle)).toBeInTheDocument();
    });

    it(`renders a class for dependency type: '${type}'`, () => {
      renderComponent({ type });

      expect(screen.getByText(label).closest('div')).toHaveClass(type);
    });

    it(`renders a tooltip for dependency type: '${type}' with a custom tooltip message`, async () => {
      SpecUtil.requestIdleCallbackInvokeImmediateJest();

      renderComponent({ type, tooltip: 'Custom tooltip message' });
      fireEvent.mouseOver(screen.getByText(label));
      const tooltip = await screen.findByRole('tooltip');

      expect(within(tooltip).getByText('Custom tooltip message')).toBeInTheDocument();
    });
  });

  it('renders null for unknown dependency types', () => {
    const { container } = renderComponent();

    expect(container).toBeEmptyDOMElement();
  });
});
