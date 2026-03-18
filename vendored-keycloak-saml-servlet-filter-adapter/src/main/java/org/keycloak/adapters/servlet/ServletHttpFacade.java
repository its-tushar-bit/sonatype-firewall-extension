/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.adapters.servlet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import javax.security.cert.X509Certificate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.LogoutError;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.UriUtils;

/**
 * Copied from <a
 * href=
 * "https://github.com/keycloak/keycloak/blob/23.0.7/adapters/spi/servlet-adapter-spi/src/main/java/org/keycloak/adapters/servlet/ServletHttpFacade.java">...</a>
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletHttpFacade
    implements HttpFacade
{
  protected final RequestFacade requestFacade = new RequestFacade();

  protected final ResponseFacade responseFacade = new ResponseFacade();

  protected HttpServletRequest request;

  protected HttpServletResponse response;

  protected MultivaluedHashMap<String, String> queryParameters;

  public ServletHttpFacade(HttpServletRequest request, HttpServletResponse response) {
    this.request = request;
    this.response = response;
  }

  protected class RequestFacade
      implements Request
  {
    private InputStream inputStream;

    @Override
    public String getMethod() {
      return request.getMethod();
    }

    @Override
    public String getURI() {
      StringBuffer buf = request.getRequestURL();
      if (request.getQueryString() != null) {
        buf.append('?').append(request.getQueryString());
      }
      return buf.toString();
    }

    @Override
    public String getRelativePath() {
      String uri = request.getRequestURI();
      String contextPath = request.getContextPath();
      String servletPath = uri.substring(uri.indexOf(contextPath) + contextPath.length());

      if ("".equals(servletPath)) {
        servletPath = "/";
      }

      return servletPath;
    }

    @Override
    public boolean isSecure() {
      return request.isSecure();
    }

    @Override
    public String getFirstParam(String param) {
      return request.getParameter(param);
    }

    @Override
    public String getQueryParamValue(String param) {
      if (queryParameters == null) {
        queryParameters = UriUtils.decodeQueryString(request.getQueryString());
      }
      return queryParameters.getFirst(param);
    }

    public MultivaluedHashMap<String, String> getQueryParameters() {
      if (queryParameters == null) {
        queryParameters = UriUtils.decodeQueryString(request.getQueryString());
      }
      return queryParameters;
    }

    @Override
    public Cookie getCookie(String cookieName) {
      if (request.getCookies() == null) {
        return null;
      }
      jakarta.servlet.http.Cookie cookie = null;
      for (jakarta.servlet.http.Cookie c : request.getCookies()) {
        if (c.getName().equals(cookieName)) {
          cookie = c;
          break;
        }
      }
      if (cookie == null) {
        return null;
      }
      return new Cookie(cookie.getName(), cookie.getValue(), 0, cookie.getDomain(), cookie.getPath());
    }

    @Override
    public String getHeader(String name) {
      return request.getHeader(name);
    }

    @Override
    public List<String> getHeaders(String name) {
      Enumeration<String> values = request.getHeaders(name);
      List<String> list = new LinkedList<>();
      while (values.hasMoreElements()) {
        list.add(values.nextElement());
      }
      return list;
    }

    @Override
    public InputStream getInputStream() {
      return getInputStream(false);
    }

    @Override
    public InputStream getInputStream(boolean buffered) {
      if (inputStream != null) {
        return inputStream;
      }

      if (buffered) {
        try {
          return inputStream = new BufferedInputStream(request.getInputStream());
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      try {
        return request.getInputStream();
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public String getRemoteAddr() {
      return request.getRemoteAddr();
    }

    @Override
    public void setError(final AuthenticationError error) {
      request.setAttribute(AuthenticationError.class.getName(), error);
    }

    @Override
    public void setError(final LogoutError error) {
      request.setAttribute(LogoutError.class.getName(), error);
    }
  }

  public boolean isEnded() {
    return responseFacade.isEnded();
  }

  protected class ResponseFacade
      implements Response
  {
    protected boolean ended;

    @Override
    public void setStatus(int status) {
      response.setStatus(status);
    }

    @Override
    public void addHeader(String name, String value) {
      response.addHeader(name, value);
    }

    @Override
    public void setHeader(String name, String value) {
      response.setHeader(name, value);
    }

    @Override
    public void resetCookie(String name, String path) {
      setCookie(name, "", path, null, 0, false, false);
    }

    @Override
    public void setCookie(
        String name,
        String value,
        String path,
        String domain,
        int maxAge,
        boolean secure,
        boolean httpOnly)
    {
      jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(name, value);
      if (path != null) {
        cookie.setPath(path);
      }
      if (domain != null) {
        cookie.setDomain(domain);
      }
      if (maxAge >= 0) { // -1 means "not set"
        cookie.setMaxAge(maxAge);
      }
      cookie.setSecure(secure);
      cookie.setHttpOnly(httpOnly);

      response.addCookie(cookie);
    }

    @Override
    public OutputStream getOutputStream() {
      try {
        return response.getOutputStream();
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void sendError(int code) {
      try {
        response.sendError(code);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void sendError(int code, String message) {
      try {
        response.sendError(code, message);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void end() {
      ended = true;
    }

    public boolean isEnded() {
      return ended;
    }
  }

  @Override
  public Request getRequest() {
    return requestFacade;
  }

  @Override
  public Response getResponse() {
    return responseFacade;
  }

  @Override
  public X509Certificate[] getCertificateChain() {
    throw new IllegalStateException("Not supported yet");
  }
}
