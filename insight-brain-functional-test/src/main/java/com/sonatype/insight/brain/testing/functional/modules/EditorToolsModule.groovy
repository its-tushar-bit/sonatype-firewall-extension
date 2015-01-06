/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

/**
 * @since 1.8
 */
class EditorToolsModule
    extends Module
{
  static content = {
    deleteButton(required: false) { $('#remove-app-org-button') }
    appEvalButton(required: false) { $('#app-evaluate-button') }
    appEval { module ApplicationEvaluationModule }
  }
}
