/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

class RoleMappingModule extends Module {
  static base = { $('#security') }

  static content = {
    role(wait: true) { name -> module RoleModule, $('.role').has('div > div:first-child', text : name) }
    roles { moduleList RoleModule, $('.role') }
  }
}