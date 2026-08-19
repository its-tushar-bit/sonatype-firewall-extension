/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.error.exception.BadRequestException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet adapter for {@link AdminTask} instances, following the Dropwizard TaskServlet pattern.
 * Handles HTTP POST dispatch, parameter parsing, error mapping, and response writing.
 */
public class AdminTaskServlet
    extends HttpServlet
{
  private static final Logger log = LoggerFactory.getLogger(AdminTaskServlet.class);

  private final AdminTask task;

  public AdminTaskServlet(AdminTask task) {
    this.task = task;
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType(MediaType.TEXT_PLAIN);
    try {
      task.execute(getParameters(request), response.getWriter());
      response.setStatus(HttpStatus.SC_OK);
    }
    catch (BadRequestException e) {
      response.setStatus(HttpStatus.SC_BAD_REQUEST);
      response.getWriter().println(e.getMessage());
    }
    catch (Exception e) {
      log.error("Error running {}", task.getName(), e);
      response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().println(e.getMessage());
    }
  }

  private static Map<String, List<String>> getParameters(HttpServletRequest request) {
    Map<String, List<String>> results = new HashMap<>();
    Enumeration<String> names = request.getParameterNames();
    while (names.hasMoreElements()) {
      String name = names.nextElement();
      results.put(name, Arrays.asList(request.getParameterValues(name)));
    }
    return results;
  }
}
