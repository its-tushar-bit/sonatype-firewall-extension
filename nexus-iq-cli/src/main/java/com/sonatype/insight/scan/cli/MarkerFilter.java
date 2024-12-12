/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.AbstractMatcherFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * A marker filter that can be applied to individual appenders as opposed to the global turbo filter shipped with
 * logback.
 *
 * @since 1.10
 */
public class MarkerFilter
    extends AbstractMatcherFilter<ILoggingEvent>
{
  private Marker markerToMatch;

  /**
   * The marker to match in the event.
   * 
   * @param markerStr
   */
  public void setMarker(String markerStr) {
    if (markerStr != null) {
      this.markerToMatch = MarkerFactory.getMarker(markerStr);
    }
  }

  @Override
  public FilterReply decide(ILoggingEvent event) {
    if (!isStarted()) {
      return FilterReply.NEUTRAL;
    }

    List<Marker> markerList = event.getMarkerList();
    if (CollectionUtils.isEmpty(markerList)) {
      return onMismatch;
    }

    Marker marker = markerList.get(0);
    if (markerToMatch.contains(marker)) {
      return onMatch;
    }
    else {
      return onMismatch;
    }
  }

  @Override
  public void start() {
    if (markerToMatch != null) {
      super.start();
    }
    else {
      addError("The marker property must be set for [" + getName() + "]");
    }
  }
}
