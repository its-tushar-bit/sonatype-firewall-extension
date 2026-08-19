/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import UnsavedChangesModal from 'MainRoot/modals/unsavedChangesModal/UnsavedChangesModal';
import { mergeRight } from 'ramda';
import userEvent from '@testing-library/user-event';
import { actions } from 'MainRoot/modals/unsavedChangesModal/unsavedChangesModalSlice';

describe('UnsavedChangesModal', () => {
  let defaultPreloadedState, renderComponent;

  beforeEach(() => {
    defaultPreloadedState = {
      unsavedChangesModal: {
        open: true,
      },
    };
    renderComponent = (preloadedState) =>
      render(<UnsavedChangesModal />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('renders the modal when open is true', () => {
    renderComponent();

    expect(screen.getByRole('dialog')).toBeVisible();
    expect(screen.getByText('Unsaved Changes')).toBeVisible();
    expect(screen.getByText(/The page may contain unsaved changes; continuing will discard them./)).toBeVisible();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Continue' })).toBeVisible();
  });

  it('renders the modal when onContinue prop is provided', () => {
    const preloadedState = {
      unsavedChangesModal: {
        open: false,
      },
    };
    render(<UnsavedChangesModal onContinue={() => {}} />, { preloadedState });

    expect(screen.getByRole('dialog')).toBeVisible();
    expect(screen.getByText('Unsaved Changes')).toBeVisible();
  });

  it('renders the modal when onClose prop is provided', () => {
    const preloadedState = {
      unsavedChangesModal: {
        open: false,
      },
    };
    render(<UnsavedChangesModal onClose={() => {}} />, { preloadedState });

    expect(screen.getByRole('dialog')).toBeVisible();
    expect(screen.getByText('Unsaved Changes')).toBeVisible();
  });

  it('does not render the modal when open is false and no props are provided', () => {
    const preloadedState = {
      unsavedChangesModal: {
        open: false,
      },
    };

    renderComponent(mergeRight(defaultPreloadedState, preloadedState));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('closes the modal when the Cancel button is clicked', async () => {
    renderComponent();
    const button = screen.getByRole('button', { name: 'Cancel' });
    expect(button).toBeVisible();
    const user = userEvent.setup();

    await user.click(button);

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('closes the modal when the Continue button is clicked', async () => {
    renderComponent();
    const button = screen.getByRole('button', { name: 'Continue' });
    expect(button).toBeVisible();
    const user = userEvent.setup();

    await user.click(button);

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('calls onClose prop when the Cancel button is clicked', async () => {
    const onClose = jest.fn();
    render(<UnsavedChangesModal onClose={onClose} />);
    const button = screen.getByRole('button', { name: 'Cancel' });
    const user = userEvent.setup();

    await user.click(button);

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('calls onContinue prop when the Continue button is clicked', async () => {
    const onContinue = jest.fn();
    render(<UnsavedChangesModal onContinue={onContinue} />);
    const button = screen.getByRole('button', { name: 'Continue' });
    const user = userEvent.setup();

    await user.click(button);

    expect(onContinue).toHaveBeenCalledTimes(1);
  });

  it('renders the modal with the correct state when open is dispatched', async () => {
    const preloadedState = {
      unsavedChangesModal: {
        open: false,
      },
    };
    const { store } = renderComponent(mergeRight(defaultPreloadedState, preloadedState));

    store.dispatch(actions.open());

    expect(await screen.findByText('Unsaved Changes')).toBeVisible();
    expect(screen.getByText(/The page may contain unsaved changes; continuing will discard them./)).toBeVisible();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Continue' })).toBeVisible();
  });

  it('calls the resolve function when the Continue button is clicked', async () => {
    const preloadedState = {
      unsavedChangesModal: {
        open: false,
      },
    };
    const resolveFn = jest.fn();
    const { store } = renderComponent(mergeRight(defaultPreloadedState, preloadedState));

    store.dispatch(actions.open()).then(resolveFn, null);

    expect(await screen.findByText('Unsaved Changes')).toBeVisible();
    const button = screen.getByRole('button', { name: 'Continue' });
    const user = userEvent.setup();

    await user.click(button);

    expect(resolveFn).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('calls reject function when the Cancel button is clicked', async () => {
    const preloadedState = {
      unsavedChangesModal: {
        open: false,
      },
    };
    const rejectFn = jest.fn();
    const { store } = renderComponent(mergeRight(defaultPreloadedState, preloadedState));

    store.dispatch(actions.open()).then(null, rejectFn);

    expect(await screen.findByText('Unsaved Changes')).toBeVisible();
    const button = screen.getByRole('button', { name: 'Cancel' });
    const user = userEvent.setup();

    await user.click(button);

    expect(rejectFn).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
