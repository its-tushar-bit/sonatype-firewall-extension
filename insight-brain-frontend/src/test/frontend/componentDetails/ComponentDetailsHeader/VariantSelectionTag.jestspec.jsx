/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render } from 'TestRoot/SpecUtil';
import { screen } from '@testing-library/dom';
import userEvent from '@testing-library/user-event';
import VariantSelectionTag from 'MainRoot/componentDetails/ComponentDetailsHeader/VariantSelectionTag';

describe('VariantSelectionTag', () => {
  let renderPage;

  beforeEach(() => {
    renderPage = (props = {}) => render(<VariantSelectionTag {...props} />);
  });

  // jsdom never matches :focus-visible, which MUI's tooltip requires before opening on focus; a real browser
  // matches it for keyboard-focused elements, so emulate that for the element that currently holds focus.
  function emulateBrowserFocusVisibleForActiveElement() {
    const realMatches = Element.prototype.matches;
    jest.spyOn(Element.prototype, 'matches').mockImplementation(function (selector) {
      return selector === ':focus-visible' ? this === document.activeElement : realMatches.call(this, selector);
    });
  }

  it('renders the pill when variant selection was applied', async () => {
    renderPage({ variantSelected: true });
    expect(await screen.findByText('Representative variant selected')).toBeVisible();
  });

  it('shows the explanatory tooltip when the focusable info icon is hovered', async () => {
    const user = userEvent.setup();
    const { container } = renderPage({ variantSelected: true });
    const icon = container.querySelector('.variant-selection-info-icon');
    expect(icon).toHaveAttribute('tabindex', '0');

    await user.hover(icon);

    expect(await screen.findByRole('tooltip', { name: /one representative package variant to reduce duplicate results/ })).toBeInTheDocument();
  });

  it('shows the explanatory tooltip when the info icon receives keyboard focus', async () => {
    emulateBrowserFocusVisibleForActiveElement();

    const user = userEvent.setup();
    const { container } = renderPage({ variantSelected: true });
    const icon = container.querySelector('.variant-selection-info-icon');

    await user.tab();

    expect(icon).toHaveFocus();
    expect(await screen.findByRole('tooltip', { name: /one representative package variant to reduce duplicate results/ })).toBeInTheDocument();
  });

  it('links the pill to a persistent description for screen readers', () => {
    const { container } = renderPage({ variantSelected: true });
    const tag = container.querySelector('.variant-selection-tag');
    const descriptionId = tag.getAttribute('aria-describedby');

    expect(descriptionId).toBeTruthy();
    expect(document.getElementById(descriptionId)).toHaveTextContent(/one representative package variant to reduce duplicate results/);
  });

  it('renders nothing when variant selection was not applied', () => {
    renderPage({ variantSelected: false });
    expect(screen.queryByText('Representative variant selected')).not.toBeInTheDocument();
  });

  it('renders nothing when the flag is undefined', () => {
    renderPage({});
    expect(screen.queryByText('Representative variant selected')).not.toBeInTheDocument();
  });
});
