/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Provides a globally cached version of notification webhooks for the current context. Note that get() callers should
 * not modify the returned object as it is shared.
 */
export default function NotificationWebhookService(CachedServiceFactory, CLMContextLocations) {
  return CachedServiceFactory.create(CLMContextLocations.getNotificationWebhooksUrl);
}
NotificationWebhookService.$inject = ['cached.service.factory', 'CLMContextLocations'];
