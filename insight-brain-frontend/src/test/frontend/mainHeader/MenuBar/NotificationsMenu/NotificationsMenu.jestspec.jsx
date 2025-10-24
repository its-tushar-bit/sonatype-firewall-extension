/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import NotificationsMenu from 'MainRoot/mainHeader/MenuBar/NotificationsMenu/NotificationsMenu';
import { screen, render, fireEvent, within } from 'TestRoot/SpecUtil';
import { timeAgo } from 'MainRoot/util/CommonServices';

describe('NotificationsMenu', () => {
  let renderComponent,
    setNotificationViewedSpy = jest.fn();

  const minimalProps = {
    notificationsToDisplay: [
      {
        id: 'f2f3b89a-b946-419c-89f2-4829f009d628',
        type: 'DEFAULT',
        summaryText: 'Sonatype IQ Server 188 Release',
        summaryUrl: null,
        detailHtml: '<div>some html for Sonatype IQ Server 188 Release</div>',
        dateCreated: 1741121127541,
        viewed: false,
      },
      {
        id: 'efc14dec-f2f9-4645-8a99-a52c71588a81',
        type: 'DEFAULT',
        summaryText: 'Sonatype IQ Server 187 Release',
        summaryUrl: null,
        detailHtml: '<div>some html for Sonatype IQ Server 187 Release</div>',
        dateCreated: 1738684811661,
        viewed: false,
      },
      {
        id: '8f3479ab-81ea-4797-a3b3-6a490c5dc877',
        type: 'DEFAULT',
        summaryText: 'Sonatype IQ Server 186 Release',
        summaryUrl: null,
        detailHtml: '<div>some html for Sonatype IQ Server 188 Release</div>',
        dateCreated: 1736360775878,
        viewed: false,
      },
    ],
    loading: false,
    error: null,
    setNotificationViewed: setNotificationViewedSpy,
  };

  beforeEach(() => {
    renderComponent = (props) => render(<NotificationsMenu {...minimalProps} {...props} />);
  });

  it('renders a loading indicator when loading is true', () => {
    const propsWithLoading = { ...minimalProps, loading: true };
    renderComponent(propsWithLoading);

    const notificationBtn = screen.getByRole('button');
    fireEvent.click(notificationBtn);
    expect(screen.getByText('Loading notification content from server...')).toBeInTheDocument();
  });

  it('renders an error message when there is an error', () => {
    const propsWithError = { ...minimalProps, error: 'An error occurred' };
    renderComponent(propsWithError);

    const notificationBtn = screen.getByRole('button');
    fireEvent.click(notificationBtn);
    const error = screen.getByRole('alert');
    expect(error).toBeInTheDocument();
    expect(error).toHaveTextContent('An error occurred');
  });

  it('renders the notifications menu with unread notifications', () => {
    renderComponent();

    const notificationBtn = screen.getByRole('button');
    expect(notificationBtn).toBeInTheDocument();
    expect(screen.getByTestId('iq-unread-notif-dot')).toBeInTheDocument();
  });

  it('renders the notifications menu without unread notifications dot when all notifications are read', () => {
    const propsWithAllReadNotifications = {
      ...minimalProps,
      notificationsToDisplay: minimalProps.notificationsToDisplay.map((notification) => ({
        ...notification,
        viewed: true,
      })),
    };

    renderComponent(propsWithAllReadNotifications);

    const notificationBtn = screen.getByRole('button');
    expect(notificationBtn).toBeInTheDocument();
    expect(screen.queryByTestId('iq-unread-notif-dot')).not.toBeInTheDocument();
  });

  it('renders the notifications menu with list of notifications when clicked', () => {
    renderComponent();

    const notificationBtn = screen.getByRole('button');
    fireEvent.click(notificationBtn);

    expect(screen.getByRole('heading', { name: 'Notifications' })).toBeInTheDocument();
    expect(screen.getAllByRole('button').length).toBe(4); // 1 icon button - 3 notifications

    expect(screen.getByRole('button', { name: /Sonatype IQ Server 188 Release/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Sonatype IQ Server 187 Release/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Sonatype IQ Server 186 Release/i })).toBeInTheDocument();
  });

  it('renders the notification contents correctly', () => {
    renderComponent();

    const notificationBtn = screen.getByRole('button');
    fireEvent.click(notificationBtn);

    expect(screen.getByRole('heading', { name: 'Notifications' })).toBeInTheDocument();

    minimalProps.notificationsToDisplay.forEach((notification) => {
      const notificationButton = screen.getByRole('button', { name: new RegExp(notification.summaryText, 'i') });
      expect(notificationButton).toBeInTheDocument();
      expect(notificationButton).toHaveTextContent(notification.summaryText);
      expect(notificationButton).toHaveTextContent(
        `${timeAgo(notification.dateCreated).age} ${timeAgo(notification.dateCreated).qualifier}`
      );
    });
  });

  it('renders a modal with notification details when a notification is clicked', () => {
    renderComponent();

    const notificationBtn = screen.getByRole('button');
    fireEvent.click(notificationBtn);

    const notification1 = screen.getByRole('button', { name: /Sonatype IQ Server 188 Release/i });
    fireEvent.click(notification1);

    const dialog = screen.getByRole('dialog');
    expect(dialog).toBeInTheDocument();
    expect(within(dialog).getByText(/some html for Sonatype IQ Server 188 Release/i)).toBeInTheDocument();
    expect(within(dialog).getByRole('button', { name: /Close/i })).toBeInTheDocument();
    fireEvent.click(within(dialog).getByRole('button', { name: /Close/i }));
    expect(dialog).not.toBeInTheDocument();
  });
});
