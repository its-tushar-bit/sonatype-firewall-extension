/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event;

import java.io.IOException;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.nexus.git.utils.api.GitException;

/**
 * An instance that can handle SourceControlEvent
 */
public interface SourceControlEventListener
{
  /**
   * Handles an event, return true if the event was handled and no more processing should be done
   */
  boolean executeEvent(final SourceControlEvent event) throws GitException, IOException;
}
