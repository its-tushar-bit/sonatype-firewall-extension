/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { requestNotificationPermission, showNotification } from 'MainRoot/utility/services/notificationService';

describe('notificationService', () => {
  let mockNotification, originalRef;

  beforeEach(() => {
    /**
     * Doing `spyOn('window`, Notification).mockReturnValue(mockNotification)` does not work.
     * So we have to directly overwrite `window.Notification` with our actual mock, while saving
     * the reference to the original `window.Notification`.
     */
    originalRef = window.Notification;
    mockNotification = jest.fn().mockName('NotificationConstructor');
    mockNotification.permission = 'default';
    mockNotification.requestPermission = jest.fn().mockName('requestNotificationPermission');
    window.Notification = mockNotification;
  });

  afterEach(() => {
    /**
     * Restore `window.Notification` to its original value.
     */
    window.Notification = originalRef;
  });

  describe('requestNotificationPermission', () => {
    it('calls Notification.requestPermission() if the permission level is `default`', () => {
      requestNotificationPermission();
      expect(Notification.requestPermission).toHaveBeenCalled();
    });

    it('does not calls Notification.requestPermission() if the permission level is not `default`', () => {
      Notification.permission = 'granted';
      requestNotificationPermission();
      expect(Notification.requestPermission).not.toHaveBeenCalled();

      Notification.permission = 'denied';
      requestNotificationPermission();
      expect(Notification.requestPermission).not.toHaveBeenCalled();
    });
  });

  describe('showNotification', () => {
    it('creates a notification object with the passed data if the permission is enabled', () => {
      Notification.permission = 'granted';
      showNotification('title', { body: 'data' });

      expect(window.Notification).toHaveBeenCalledWith('title', { body: 'data' });
    });

    it('does not creates a notification object if the permission is not granted', () => {
      Notification.permission = 'default';
      showNotification('title');
      expect(window.Notification).not.toHaveBeenCalled();

      Notification.permission = 'denied';
      showNotification('title');
      expect(window.Notification).not.toHaveBeenCalled();
    });
  });
});
