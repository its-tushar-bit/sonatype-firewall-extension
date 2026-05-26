/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.lifecycle;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Bridge interface that replaces Dropwizard's {@code io.dropwizard.lifecycle.Managed}.
 *
 * <p>
 * Subclasses override {@link #start()} and {@link #stop()} exactly as they did under Dropwizard.
 * Spring sees {@link InitializingBean} and {@link DisposableBean} through this interface and calls
 * {@link #afterPropertiesSet()} / {@link #destroy()}, which delegate to {@code start()} / {@code stop()}.
 */
public interface Managed
    extends InitializingBean, DisposableBean
{
  default void start() throws Exception {
  }

  default void stop() throws Exception {
  }

  @Override
  default void afterPropertiesSet() throws Exception {
    start();
  }

  @Override
  default void destroy() throws Exception {
    stop();
  }
}
