/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

/**
 * @since 1.8
 */
class PolicyMonitoringModule
    extends Module
{
  static content = {
    expandButton { $('a', 'ng-click': 'toggleSection(\'monitoring\',monitoringExpanded)') }
    form { $('#monitoringForm') }
    policyMonitoring { form.policyMonitoring() }
    selectedOptionText { policyMonitoring.find('option', value: policyMonitoring.value()).text() }
  }
}
