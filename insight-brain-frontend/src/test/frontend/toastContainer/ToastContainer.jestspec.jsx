/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import ToastContainer from 'MainRoot/toastContainer/ToastContainer';
import { render, screen } from 'TestRoot/SpecUtil';
import { fireEvent, within } from '@testing-library/react';

import 'TestRoot/SpecUtil';

describe('Toast', () => {
  let renderComponent;

  beforeEach(() => {
    const defaultPreloadedState = {
      toast: {
        toasts: [],
        toastIdInc: 0,
      },
    };

    renderComponent = (preloadedState) =>
      render(<ToastContainer />, {
        preloadedState: preloadedState || defaultPreloadedState,
      });
  });

  it('does not render if toasts array is empty', () => {
    renderComponent();
    const toast = screen.queryByRole('alert');
    expect(toast).not.toBeInTheDocument();
  });

  it('renders a toast if toasts array is not empty ', () => {
    renderComponent({
      toast: {
        toasts: [
          {
            id: 1,
            message: 'Toast Message',
            type: 'success',
          },
        ],
        toastIdInc: 1,
      },
    });
    const toast = screen.getByRole('alert');
    expect(toast).toBeVisible();
    expect(toast).toHaveTextContent('Toast Message');
  });

  describe('renders correct type of toasts', () => {
    let toastsTypes = ['success', 'info', 'error', 'warning'];
    toastsTypes.forEach((type) => {
      it(`when type: ${type} is provided`, () => {
        renderComponent({
          toast: {
            toasts: [
              {
                id: 1,
                message: `${type} Toast`,
                type: `${type}`,
              },
            ],
            toastIdInc: 1,
          },
        });
        const toast = screen.getByRole('alert');
        expect(toast).toBeVisible();
        expect(toast).toHaveClass(`nx-alert nx-alert--${type}`);
        expect(toast).toHaveTextContent(`${type} Toast`);
      });
    });
  });

  it('removes toast when the close button is clicked', () => {
    renderComponent({
      toast: {
        toasts: [
          {
            id: 1,
            message: 'Toast Message',
            type: 'success',
          },
        ],
        toastIdInc: 1,
      },
    });
    const toast = screen.getByRole('alert');
    expect(toast).toBeVisible();
    const closeButton = screen.getByRole('button', { name: 'Close' });
    fireEvent.click(closeButton);
    fireEvent.animationEnd(toast);
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('removes specific toast when the close button is clicked when there are multiple toasts', () => {
    renderComponent({
      toast: {
        toasts: [
          {
            id: 1,
            message: 'Success Toast 1',
            type: 'success',
          },
          {
            id: 2,
            message: 'Success Toast 2',
            type: 'success',
          },
          {
            id: 3,
            message: 'Error Toast 1',
            type: 'error',
          },
        ],
        toastIdInc: 4,
      },
    });
    const toasts = screen.getAllByRole('alert');
    expect(toasts.length).toBe(3);

    const firstSuccessToast = toasts[0];
    const secondSuccessToast = toasts[1];
    const firstErrorToast = toasts[2];

    expect(firstSuccessToast).toBeVisible();
    expect(secondSuccessToast).toBeVisible();
    expect(firstErrorToast).toBeVisible();

    let closeButton = within(firstSuccessToast).getByRole('button', { name: 'Close' });
    fireEvent.click(closeButton);
    fireEvent.animationEnd(firstSuccessToast);

    expect(screen.getAllByRole('alert').length).toBe(2);

    closeButton = within(firstErrorToast).getByRole('button', { name: 'Close' });
    fireEvent.click(closeButton);
    fireEvent.animationEnd(firstErrorToast);

    expect(screen.getAllByRole('alert').length).toBe(1);

    closeButton = within(secondSuccessToast).getByRole('button', { name: 'Close' });
    fireEvent.click(closeButton);
    fireEvent.animationEnd(secondSuccessToast);

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
