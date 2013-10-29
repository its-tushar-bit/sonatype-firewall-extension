/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

class RoleModule extends Module {
  static content = {
    editButton { $('button').has('i.icon-pencil') }

    editor(required:false) { $('div[app-security-editor] > div') }
    queryInput(required:false) { $('input[name=filter]') }
    appliedUsers(required:false) { $('div[app-security-editor] .selectList:last-child .licenseSelectListItem', text : name) }
    availableUsers(required:false) { name -> $('div[app-security-editor] .selectList:first-child .licenseSelectListItem', text : name) }
  }
}