/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mock.hds;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.mock.hds.HdsMockServer.RequestException;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.IO;

public class HdsMockResponse
{
  private static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";

  private static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";

  private Object body;

  private int status = HttpStatus.OK_200;

  private String contentType;

  private String method;

  private ParsedUri uri;

  private boolean withoutLicense;

  HdsMockResponse(Object body) {
    this.body = body;
    if (body instanceof byte[]) {
      contentType = CONTENT_TYPE_OCTET_STREAM;
    }
    else if (body instanceof File || body instanceof Path) {
      contentType = getContentType(body.toString());
    }
    else if (body instanceof URL) {
      contentType = getContentType(((URL) body).getPath());
    }
    else if (!(body instanceof HttpResponseProcessor)) {
      contentType = CONTENT_TYPE_JSON;
    }
  }

  private static String getContentType(String pathname) {
    return pathname.endsWith(".json") ? CONTENT_TYPE_JSON : CONTENT_TYPE_OCTET_STREAM;
  }

  public HdsMockResponse withType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  public HdsMockResponse andStatus(int status) {
    this.status = status;
    return this;
  }

  public HdsMockResponse forMethod(String method) {
    this.method = method;
    return this;
  }

  public HdsMockResponse atUri(URI uri) {
    return atUri(uri.toString());
  }

  public HdsMockResponse atUri(String uri) {
    if (!uri.startsWith("/")) {
      uri = "/" + uri;
    }
    this.uri = new ParsedUri(uri);
    return this;
  }

  public HdsMockResponse withoutLicense() {
    withoutLicense = true;
    return this;
  }

  boolean matches(String method, ParsedUri uri) {
    if (this.method != null && !this.method.equals(method)) {
      return false;
    }
    if (this.uri != null) {
      if (!this.uri.path.equals(uri.path)) {
        return false;
      }
      for (Map.Entry<String, Collection<Object>> entry : this.uri.query.entrySet()) {
        Collection<Object> targetParam = entry.getValue();
        Collection<Object> requestParam = uri.query.get(entry.getKey());
        if (!targetParam.equals(requestParam)) {
          return false;
        }
      }
    }
    return true;
  }

  private void validateLicense(HttpServletRequest request) throws RequestException {
    String licenseFingerprint = request.getHeader("X-CLM-Token");
    if (licenseFingerprint == null || licenseFingerprint.isEmpty()) {
      throw new RequestException(HttpServletResponse.SC_PAYMENT_REQUIRED, "license fingerprint required");
    }
  }

  void render(HttpServletRequest request, HttpServletResponse response) throws IOException {
    if (!withoutLicense) {
      validateLicense(request);
    }
    response.setStatus(status);
    response.setContentType(contentType);
    try (OutputStream os = response.getOutputStream()) {
      if (body instanceof String) {
        os.write(body.toString().getBytes(StandardCharsets.UTF_8));
      }
      else if (body instanceof byte[]) {
        os.write((byte[]) body);
      }
      else if (body instanceof File) {
        try (InputStream is = new FileInputStream((File) body)) {
          IO.copy(is, os);
        }
      }
      else if (body instanceof Path) {
        try (InputStream is = Files.newInputStream((Path) body)) {
          IO.copy(is, os);
        }
      }
      else if (body instanceof URL) {
        try (InputStream is = ((URL) body).openStream()) {
          IO.copy(is, os);
        }
      }
      else if (body instanceof HttpResponseProcessor) {
        ((HttpResponseProcessor) body).process(request, response);
      }
      else {
        os.write(Json.write(body));
      }
    }
  }
}
