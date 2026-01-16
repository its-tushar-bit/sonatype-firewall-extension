/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, screen } from 'TestRoot/SpecUtil';
import LogoutWarningModal from 'MainRoot/modals/logoutWarningModal/LogoutWarningModal';
import { mergeRight } from 'ramda';
import userEvent from '@testing-library/user-event';
import { getSessionUrl } from 'MainRoot/util/CLMLocation';
import { actions } from 'MainRoot/modals/logoutWarningModal/logoutWarningModalSlice';

describe('LogoutWarningModal', () => {
  let axiosMock, defaultPreloadedState, renderComponent;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    jest.useFakeTimers();
    defaultPreloadedState = {
      logoutWarningModal: {
        open: true,
        secondsLeft: 120,
        intervalId: null,
      },
      userSession: {
        data: {
          sessionTimeoutMilliseconds: 30 * 60 * 1000,
        },
        loading: false,
        error: null,
        shouldDisplayPasswordWarning: false,
      },
    };
    const mockNotification = jest.fn();
    mockNotification.permission = 'granted';
    global.Notification = mockNotification;
    renderComponent = (preloadedState) =>
      render(<LogoutWarningModal />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('renders the modal when open is true', () => {
    renderComponent();

    expect(screen.getByRole('dialog')).toBeVisible();
    expect(screen.getByText('Session Timeout Warning')).toBeVisible();
    expect(screen.getByText('Due to 30 minutes of inactivity you will be logged out in 120 seconds.')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Keep me signed in' })).toBeVisible();
  });

  it('renders the modal with the correct secondsLeft when open is dispatched with a specific startingCount', async () => {
    const preloadedState = {
      logoutWarningModal: {
        open: false,
        secondsLeft: null,
      },
    };
    const { store } = renderComponent(mergeRight(defaultPreloadedState, preloadedState));

    store.dispatch(
      actions.open({
        startingCount: 123,
        productEdition: 'MyAwesomeProduct',
      })
    );

    expect(await screen.findByText('Session Timeout Warning')).toBeVisible();
    expect(screen.getByText('Due to 30 minutes of inactivity you will be logged out in 123 seconds.')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Keep me signed in' })).toBeVisible();
    expect(window.Notification).toHaveBeenCalledWith('Session Timeout Warning', {
      body: 'Your MyAwesomeProduct session will expire in 2 minutes due to inactivity.',
    });
  });

  it('does not render the modal when open is false', () => {
    const preloadedState = {
      logoutWarningModal: {
        open: false,
      },
    };

    renderComponent(mergeRight(defaultPreloadedState, preloadedState));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('displays the correct session timeout', () => {
    const preloadedState = {
      userSession: {
        data: {
          sessionTimeoutMilliseconds: 45 * 60 * 1000,
        },
      },
    };

    renderComponent(mergeRight(defaultPreloadedState, preloadedState));

    expect(screen.getByText('Due to 45 minutes of inactivity you will be logged out in 120 seconds.')).toBeVisible();
  });

  it('displays generic inactivity text when session timeout is not a number', () => {
    const preloadedState = {
      userSession: {
        data: {
          sessionTimeoutMilliseconds: null,
        },
      },
    };

    renderComponent(mergeRight(defaultPreloadedState, preloadedState));

    expect(screen.getByText(/Due to inactivity/)).toBeVisible();
  });

  it('displays 0 seconds when secondsLeft is negative', () => {
    const preloadedState = {
      logoutWarningModal: {
        open: true,
        secondsLeft: -5,
      },
    };

    renderComponent(mergeRight(defaultPreloadedState, preloadedState));

    expect(screen.getByText(/you will be logged out in 0 seconds./)).toBeVisible();
  });

  it('sets up a timer that decrements secondsLeft when the modal is open', () => {
    const { store } = renderComponent();
    store.dispatch(
      actions.open({
        startingCount: 120,
        productEdition: 'MyAwesomeProduct',
      })
    );

    expect(screen.getByText('Due to 30 minutes of inactivity you will be logged out in 120 seconds.')).toBeVisible();

    jest.advanceTimersByTime(1000);

    expect(screen.getByText('Due to 30 minutes of inactivity you will be logged out in 119 seconds.')).toBeVisible();

    jest.advanceTimersByTime(1000);

    expect(screen.getByText('Due to 30 minutes of inactivity you will be logged out in 118 seconds.')).toBeVisible();
  });

  it("closes the modal and sends a session request to keep the session active when the 'Keep me signed in' button is clicked", async () => {
    jest.useRealTimers();
    axiosMock.onGet(getSessionUrl()).reply(200);
    renderComponent();
    const button = screen.getByRole('button', { name: 'Keep me signed in' });
    expect(button).toBeVisible();
    const user = userEvent.setup();

    await user.click(button);

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].url).toBe(getSessionUrl());
  });
});
