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
