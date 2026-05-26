/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Base class for admin tasks that are exposed as HTTP POST endpoints on the management port.
 *
 * <p>
 * Replaces the Dropwizard {@code Task} base class with the same constructor and execute semantics.
 * Implementations are registered as servlets at {@code /tasks/{name}} via
 * {@code AdminCompatibilityConfiguration}.
 *
 * <p>
 * Tasks that need HTTP request parameters should override
 * {@link #execute(Map, PrintWriter)}. Tasks that don't need parameters can simply override
 * {@link #execute()}.
 */
public abstract class AdminTask
{
  private final String name;

  protected AdminTask(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String getPath() {
    return name;
  }

  /**
   * Execute the task with the given HTTP request parameters.
   *
   * <p>
   * The default implementation delegates to {@link #execute()}, ignoring parameters and output.
   *
   * @param parameters the query parameters from the HTTP request
   * @param output writer for sending text back to the admin API caller
   */
  public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
    execute();
  }

  /**
   * Execute the task with no parameters.
   *
   * <p>
   * Override this for tasks that don't need HTTP request parameters.
   * The default throws {@link UnsupportedOperationException} to catch missing implementations.
   */
  public void execute() throws Exception {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + " must override execute() or execute(parameters, output)");
  }
}
