/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;

import io.dropwizard.core.setup.AdminEnvironment;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

public interface AdminHealthCheckEndpoint
{
  String getName();

  String getPath();

  HealthCheckResponse getHealthCheckResponse();

  class HealthCheckResponse
  {
    private boolean healthy;

    private String content;

    public boolean isHealthy() {
      return healthy;
    }

    public void setHealthy(boolean healthy) {
      this.healthy = healthy;
    }

    public String getContent() {
      return content;
    }

    public void setContent(String content) {
      this.content = content;
    }

    public HealthCheckResponse() {
    }

    public HealthCheckResponse(boolean healthy) {
      this.healthy = healthy;
    }

    public HealthCheckResponse(boolean healthy, String content) {
      this.healthy = healthy;
      this.content = content;
    }
  }

  static void addAdminHealthCheckEndpoint(
      AdminEnvironment adminEnvironment,
      AdminHealthCheckEndpoint adminHealthCheckEndpoint)
  {
    adminEnvironment.addServlet(adminHealthCheckEndpoint.getName(), new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        HealthCheckResponse healthCheckResponse = adminHealthCheckEndpoint.getHealthCheckResponse();
        String content = healthCheckResponse.getContent();
        boolean hasContent = StringUtils.isNotBlank(content);
        if (healthCheckResponse.isHealthy()) {
          httpServletResponse.setStatus(hasContent ? HttpStatus.SC_OK : HttpStatus.SC_NO_CONTENT);
        }
        else {
          httpServletResponse.setStatus(HttpStatus.SC_SERVICE_UNAVAILABLE);
        }
        if (hasContent) {
          httpServletResponse.setContentType(MediaType.TEXT_PLAIN);
          try (PrintWriter writer = httpServletResponse.getWriter()) {
            writer.println(healthCheckResponse.getContent());
          }
          catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
          }
        }
      }
    }).addMapping(adminHealthCheckEndpoint.getPath());
  }
}
