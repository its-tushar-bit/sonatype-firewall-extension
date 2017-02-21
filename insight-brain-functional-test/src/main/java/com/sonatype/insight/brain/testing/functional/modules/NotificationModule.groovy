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
    notificationCount (required: false) { $('.iq-count-circle') }
    notificationList (required: false) { moduleList NotificationItemModule, $('.iq-notification') }
    detailHeader(required: false) { $('.iq-dropdown-submenu .iq-dropdown-submenu__title') }
    detailBody(required: false) { $('#detail-html-container div') }
    detailedBodyLinks(required: false) { detailBody.find('a') }
  }
}
