/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

class NotificationItemModule
    extends Module
{
  static content = {
    age { $('.notification-age') }
    ageLabel { $('.notification-age-label') }
    summary { $('.notification-text') }
    detailHeader(required: false) { $('.dropdown-sub-menu .disabled') }
    detailBody(required: false) { $('#detail-html-container div') }
    detailedBodyLinks(required: false) { detailBody.find('a') }
  }
}
