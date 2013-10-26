package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

/**
 * @since 1.7
 */
class LogoutModule extends Module
{
  static content = {
    logout { $('a', text: 'Logout') }
  }
}
