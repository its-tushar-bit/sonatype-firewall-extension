/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.configuration

import com.sonatype.insight.brain.testing.functional.BasePage

/**
 * @since 1.7
 */
class LdapConfigurationPage
    extends BasePage
{
  static url = 'assets/index.html#/ldap'

  static at = { inlineEditorSpan?.displayed }

  static content = {
    // name editor
    inlineEditorSpan(wait: true) { $('#ldap-name .editable') }
    inlineEditor(required: false) { $('#ldap-name input') }
    save(required: false) { $('#ldap-name button.btn-primary') }
    cancel(required: false) { $('#ldap-name button:first-child') }

    //requires confirmation to delete
    delete(required: false) { $('a[title="Remove Configuration"]') }
    deleteConfirm(required: false) { $('#delete-ldap-confirmation button.btn-primary') }

    //requires confirmation to discard changes
    discard(required: false) { $('#ldap-unsaved-changes button.btn-primary') }

    // in-page navigation
    connectionTab(required: false, to: LdapConnectionConfigurationPage) { $('.tri-pane li:first-child a') }
    userAndGroupSettingsTab(required: false, to: LdapUserAndGroupMappingConfigurationPage) {
      $('.tri-pane li:nth-child(2) a')
    }
  }

  static isActiveTab(tab) {
    tab?.parent().hasClass('active')
  }
}
