package com.sonatype.insight.brain.testing.functional.modules
import geb.Module

class RoleMappingModule extends Module {
  static base = { $('#security') }

  static content = {
    role { name -> $('.role').has('tr > td:first-child > p', text : name) }
  }
}