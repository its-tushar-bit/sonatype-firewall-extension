/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.configuration

/**
 * @since 1.7
 */
class LdapConfigurationPage
    extends ConfigurationPage
{
  static url = "${ConfigurationPage.url}/ldap"

  static at = { inlineEditorSpan?.displayed }

  static content = {
    // name editor
    inlineEditorSpan(wait: true) { $('#ldapName .editable') }
    inlineEditor(required: false) { inlineEditorSpan.next().find('input') }
    save(required: false) { $('button', text: 'Save') }
    cancel(required: false) { $('button', text: 'Cancel') }

    //requires confirmation to delete
    delete(required: false) { $('a', title: 'Remove Configuration') }
    deleteConfirm(required: false) { $('button', text: 'Delete') }

    //requires confirmation to discard changes
    discard(required: false) { $('button', text: 'Discard')}

    // in-page navigation
    connectionTab(required: false, to: LdapConnectionConfigurationPage) { $('a', text: 'CONNECTION') }
    userAndGroupSettingsTab(required: false, to: LdapUserAndGroupMappingConfigurationPage) { $('a', text: 'USER & GROUP SETTINGS') }
  }

  static isActiveTab(tab){
    tab?.parent().hasClass('active')
  }
}
