/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { act, screen } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { userEvent, render } from 'TestRoot/SpecUtil';
import NoscToastHost from 'MainRoot/nosc/toast/NoscToastHost';
import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';

describe('NoscToastHost', () => {
  it('renders success and error toasts from the shared toast slice', async () => {
    const { store } = render(
      <Theme>
        <NoscToastHost />
      </Theme>,
    );

    act(() => {
      store.dispatch(toastActions.addToast({ type: 'success', message: 'Waiver deleted' }));
    });
    expect(await screen.findByTestId('nosc-toast-success')).toHaveTextContent('Waiver deleted');
    // Portaled to document.body (not trapped under shell Theme stacking).
    expect(document.body.querySelector('[data-testid="nosc-toast-host"]')).toBeTruthy();

    act(() => {
      store.dispatch(toastActions.addToast({ type: 'error', message: 'Extend failed' }));
    });
    expect(await screen.findByTestId('nosc-toast-error')).toHaveTextContent('Extend failed');
  });

  it('dismisses a toast when the close control is clicked', async () => {
    const { store } = render(
      <Theme>
        <NoscToastHost />
      </Theme>,
    );

    act(() => {
      store.dispatch(toastActions.addToast({ type: 'success', message: 'Done' }));
    });
    const toast = await screen.findByTestId('nosc-toast-success');
    const dismiss = screen.getByTestId(/nosc-toast-dismiss-/);
    await userEvent.click(dismiss);
    expect(toast).not.toBeInTheDocument();
  });
});
