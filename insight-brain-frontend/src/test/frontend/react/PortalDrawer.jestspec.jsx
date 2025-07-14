/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, setupPortalContainer, removePortalContainer } from 'TestRoot/SpecUtil';
import PortalDrawer from 'MainRoot/react/PortalDrawer';

describe('PortalDrawer', () => {
  let renderComponent;

  beforeAll(() => setupPortalContainer());
  afterAll(() => removePortalContainer());

  const minimalProps = {
    open: false,
    onClose: jest.fn(),
  };

  beforeEach(() => {
    renderComponent = (props) => {
      return render(<PortalDrawer {...props} {...minimalProps} />);
    };
  });

  it('renders an NxDrawer as a child of .nx-page', () => {
    renderComponent();

    const drawer = screen.getByRole('dialog', { hidden: true });
    expect(drawer).toBeInTheDocument();
    expect(drawer.parentNode).toHaveClass('nx-page');
  });

  it('passes additional props to NxDrawer', () => {
    const customProps = {
      className: 'custom-class',
      'aria-label': 'test-label',
      'aria-labelledby': 'test-label-id',
    };
    renderComponent(customProps);

    const drawer = screen.getByRole('dialog', { hidden: true });
    expect(drawer).toHaveAttribute('aria-labelledby', 'test-label-id');
    expect(drawer).toHaveAttribute('aria-label', 'test-label');
    expect(drawer.classList.contains('custom-class')).toBe(true);
  });
});
