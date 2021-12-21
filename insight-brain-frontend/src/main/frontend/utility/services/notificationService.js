/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export function requestNotificationPermission() {
  if (Notification && Notification.permission === 'default') {
    Notification.requestPermission();
  }
}

export function showNotification(message, body = {}) {
  if (Notification && Notification.permission === 'granted') {
    new Notification(message, body);
  } else {
    console.info('Notifications not supported or enabled');
  }
}
