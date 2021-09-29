/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import { NxButton } from '@sonatype/react-shared-components';

import NotificationsMenu from '../../../../../main/frontend/mainHeader/MenuBar/NotificationsMenu/NotificationsMenu';
import { MenuButton } from '../../../../../main/frontend/mainHeader/MenuBar/MenuButton/MenuButton';
import { NotificationDetails } from '../../../../../main/frontend/mainHeader/MenuBar/NotificationsMenu/NotificationDetails';

describe('NotificationsMenu', function () {
  let minimalProps, getShallowComponent, getMountedComponent;

  beforeEach(function () {
    minimalProps = {
      notificationsToDisplay: null,
      loading: false,
      error: null,
      loadNotifications: () => {},
      setNotificationViewed: () => {},
    };

    getShallowComponent = enzymeUtils.getShallowComponent(NotificationsMenu, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(NotificationsMenu, minimalProps);
  });

  it('loads available notifications if there are none present', function () {
    const notificationLoadSpy = jasmine.createSpy('loadNotifications');
    getMountedComponent({ loadNotifications: notificationLoadSpy });
    expect(notificationLoadSpy).toHaveBeenCalled();
  });

  it('does not continue trying to load available notifications if there was an error', function () {
    const notificationLoadSpy = jasmine.createSpy('loadNotifications');
    getMountedComponent({ loadNotifications: notificationLoadSpy, error: 'some error' });
    expect(notificationLoadSpy).not.toHaveBeenCalled();
  });

  describe('when loading notifications', function () {
    it('renders a loading message as the MenuButton content', function () {
      const component = getShallowComponent({ loading: true }),
        menu = component.find(MenuButton),
        alertDiv = menu.childAt(1); // the first child is the title

      expect(alertDiv).toHaveText('Loading notification content from server...');
    });
  });

  describe('on error when loading notifications', function () {
    it('renders an error message as the MenuButton content', function () {
      const component = getShallowComponent({ error: 'Error while loading notifications' }),
        menu = component.find(MenuButton),
        alertDiv = menu.childAt(1); // the first child is the title

      expect(alertDiv).toHaveText('Error while loading notifications');
    });
  });

  describe('after loading notifications', function () {
    it('renders each notification summary and publishing date information in the menu', function () {
      const notificationsProp = [
        { id: 'id0', summaryText: 'summary id0' },
        { id: 'id1', summaryText: 'summary id1' },
      ];
      const component = getShallowComponent({ notificationsToDisplay: notificationsProp }),
        menu = component.find(MenuButton),
        notifications = menu.find('.iq-notification');

      expect(notifications.length).toBe(2);
      expect(notifications.at(0)).toHaveText('summary id0');
      expect(notifications.at(1)).toHaveText('summary id1');
    });

    it('renders a counter when there are unread notifications', function () {
      const notificationsProp = [
        { id: 'id0', summaryText: 'summary id0', viewed: false },
        { id: 'id1', summaryText: 'summary id1', viewed: false },
        { id: 'id2', summaryText: 'summary id2', viewed: true },
      ];
      const component = getShallowComponent({ notificationsToDisplay: notificationsProp }),
        counter = component.find('.iq-count-circle');

      expect(counter).toExist();
      expect(counter).toHaveText('2');
    });

    it('does not render a counter when there are no unread notifications', function () {
      const notificationsProp = [
        { id: 'id0', summaryText: 'summary id0', viewed: true },
        { id: 'id1', summaryText: 'summary id1', viewed: true },
        { id: 'id2', summaryText: 'summary id2', viewed: true },
      ];
      const component = getShallowComponent({ notificationsToDisplay: notificationsProp }),
        counter = component.find('.iq-count-circle');

      expect(counter).not.toExist();
    });
  });

  describe('when clicking/selecting a notification', function () {
    it('sets the notification as viewed in the store', function () {
      const notificationsProp = [
        { id: 'id0', summaryText: 'summary id0', viewed: false },
        { id: 'id1', summaryText: 'summary id1', viewed: false },
        { id: 'id2', summaryText: 'summary id2', viewed: true },
      ];
      const setNotificationViewedSpy = jasmine.createSpy('setNotificationViewed');
      const componentProps = {
        notificationsToDisplay: notificationsProp,
        setNotificationViewed: setNotificationViewedSpy,
      };
      const component = getShallowComponent(componentProps),
        menu = component.find(MenuButton),
        notifications = menu.find('.iq-notification');

      notifications.at(1).simulate('click');

      expect(setNotificationViewedSpy).toHaveBeenCalledWith({ id: 'id1', summaryText: 'summary id1', viewed: false });
    });

    it('renders the notification detail if it was not previously selected', function () {
      const notificationsProp = [
        { id: 'id0', summaryText: 'summary id0', viewed: false, detailHtml: 'forcibly set html' },
      ];

      const component = getMountedComponent({ notificationsToDisplay: notificationsProp }),
        menu = component.find(NxButton);
      menu.simulate('click');
      component.update();

      const notificationsPanel = component.find('.iq-scrollable');
      const notification = notificationsPanel.find('.iq-notification');
      notification.simulate('click');
      component.update();

      const notificationDetail = component.find(NotificationDetails);
      expect(notificationDetail).toExist();
      // Should display the summary and the detailHtml in the detail component
      expect(notificationDetail).toHaveText('summary id0forcibly set html');
    });

    it('closes the notification detail if it was previously selected', function () {
      const notificationsProp = [
        { id: 'id0', summaryText: 'summary id0', viewed: false, detailHtml: 'forcibly set html' },
      ];

      const component = getMountedComponent({ notificationsToDisplay: notificationsProp }),
        menu = component.find(NxButton);
      menu.simulate('click');
      component.update();

      const notificationsPanel = component.find('.iq-scrollable');
      const notification = notificationsPanel.find('.iq-notification');
      notification.simulate('click');
      component.update();

      const notificationDetail = component.find('.iq-dropdown-submenu__container');
      expect(notificationDetail).toExist();

      notification.simulate('click');
      component.update();

      const notificationDetailAfterReClick = component.find('.iq-dropdown-submenu__container');
      expect(notificationDetailAfterReClick).not.toExist();
    });
  });
});
