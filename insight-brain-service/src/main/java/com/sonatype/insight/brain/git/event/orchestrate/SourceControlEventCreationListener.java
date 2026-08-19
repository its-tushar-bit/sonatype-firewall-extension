/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

public interface SourceControlEventCreationListener
{
  void onNewEvent(SourceControlEvent event);
}
