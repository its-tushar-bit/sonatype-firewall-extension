/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import ExternalLinkModal from 'MainRoot/modals/externalLinkModal/ExternalLinkModal';
import { mergeRight } from 'ramda';
import userEvent from '@testing-library/user-event';
import { actions } from 'MainRoot/modals/externalLinkModal/externalLinkModalSlice';

describe('ExternalLinkModal', () => {
  let defaultPreloadedState, renderComponent;

  beforeEach(() => {
    defaultPreloadedState = {
      externalLinkModal: {
        open: true,
        href: 'https://example.com',
      },
    };
    renderComponent = (preloadedState) =>
      render(<ExternalLinkModal />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('renders the modal when open is true', () => {
    renderComponent();

    expect(screen.getByRole('dialog')).toBeVisible();
    expect(screen.getByText('External Link')).toBeVisible();
    expect(screen.getByText(/This link leads to an external site/)).toBeVisible();
    expect(screen.getByText('https://example.com')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Close' })).toBeVisible();
  });

  it('renders the modal with the correct href when open is dispatched', async () => {
    const preloadedState = {
      externalLinkModal: {
        open: false,
        href: null,
      },
    };
    const { store } = renderComponent(mergeRight(defaultPreloadedState, preloadedState));

    store.dispatch(actions.open('https://other.com'));

    expect(await screen.findByText('External Link')).toBeVisible();
    expect(screen.getByText(/This link leads to an external site/)).toBeVisible();
    expect(screen.getByText('https://other.com')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Close' })).toBeVisible();
  });

  it('does not render the modal when open is false', () => {
    const preloadedState = {
      externalLinkModal: {
        open: false,
      },
    };

    renderComponent(mergeRight(defaultPreloadedState, preloadedState));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('closes the modal when the Close button is clicked', async () => {
    renderComponent();
    const button = screen.getByRole('button', { name: 'Close' });
    expect(button).toBeVisible();
    const user = userEvent.setup();

    await user.click(button);

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
