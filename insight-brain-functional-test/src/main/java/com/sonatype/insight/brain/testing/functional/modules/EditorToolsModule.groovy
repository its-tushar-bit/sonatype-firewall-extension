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
class EditorToolsModule extends Module
{
  static content = {
    deleteButton(required: true) { $('#remove-app-org-button') }
    appEvalButton(required: true) { $('#app-evaluate-button') }
    appEval { module ApplicationEvaluationModule }
  }
}
