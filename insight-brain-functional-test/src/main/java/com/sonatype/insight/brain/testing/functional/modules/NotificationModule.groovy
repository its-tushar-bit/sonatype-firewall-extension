/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

class NotificationModule
    extends Module
{
  static base = { $('#notification-menu') }

  static content = {
    dropdown { $('#notification-dropdown-toggle') }
    notificationCount (required: false) { $('.count-circle') }
    notificationList (required: false) { moduleList NotificationItemModule, $('.notification-link') }
  }
}
