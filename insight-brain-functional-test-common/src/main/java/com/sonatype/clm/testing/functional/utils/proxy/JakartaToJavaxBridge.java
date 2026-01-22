/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils.proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * Bridge classes to adapt Jakarta servlet API types to javax servlet API types. Required because
 * insight-test-reverse-proxy uses javax.servlet APIs, but the application has migrated to jakarta.servlet APIs.
 */
public class JakartaToJavaxBridge
{
  /**
   * Wraps a Jakarta HttpServletResponse to implement javax.servlet.http.HttpServletResponse.
   */
  public static class ResponseAdapter
      implements javax.servlet.http.HttpServletResponse
  {
    private final jakarta.servlet.http.HttpServletResponse jakartaResponse;

    private javax.servlet.ServletOutputStream cachedOutputStream;

    public ResponseAdapter(jakarta.servlet.http.HttpServletResponse jakartaResponse) {
      this.jakartaResponse = jakartaResponse;
    }

    @Override
    public void addCookie(javax.servlet.http.Cookie cookie) {
      jakarta.servlet.http.Cookie jakartaCookie = new jakarta.servlet.http.Cookie(
          cookie.getName(),
          cookie.getValue()
      );
      jakartaCookie.setDomain(cookie.getDomain());
      jakartaCookie.setPath(cookie.getPath());
      jakartaCookie.setMaxAge(cookie.getMaxAge());
      jakartaCookie.setSecure(cookie.getSecure());
      jakartaCookie.setHttpOnly(cookie.isHttpOnly());
      jakartaResponse.addCookie(jakartaCookie);
    }

    @Override
    public boolean containsHeader(String name) {
      return jakartaResponse.containsHeader(name);
    }

    @Override
    public String encodeURL(String url) {
      return jakartaResponse.encodeURL(url);
    }

    @Override
    public String encodeRedirectURL(String url) {
      return jakartaResponse.encodeRedirectURL(url);
    }

    @Override
    public String encodeUrl(String url) {
      return jakartaResponse.encodeURL(url);
    }

    @Override
    public String encodeRedirectUrl(String url) {
      return jakartaResponse.encodeRedirectURL(url);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
      jakartaResponse.sendError(sc, msg);
    }

    @Override
    public void sendError(int sc) throws IOException {
      jakartaResponse.sendError(sc);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
      jakartaResponse.sendRedirect(location);
    }

    @Override
    public void setDateHeader(String name, long date) {
      jakartaResponse.setDateHeader(name, date);
    }

    @Override
    public void addDateHeader(String name, long date) {
      jakartaResponse.addDateHeader(name, date);
    }

    @Override
    public void setHeader(String name, String value) {
      jakartaResponse.setHeader(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
      jakartaResponse.addHeader(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
      jakartaResponse.setIntHeader(name, value);
    }

    @Override
    public void addIntHeader(String name, int value) {
      jakartaResponse.addIntHeader(name, value);
    }

    @Override
    public void setStatus(int sc) {
      jakartaResponse.setStatus(sc);
    }

    @Override
    public void setStatus(int sc, String sm) {
      jakartaResponse.setStatus(sc);
    }

    @Override
    public int getStatus() {
      return jakartaResponse.getStatus();
    }

    @Override
    public String getHeader(String name) {
      return jakartaResponse.getHeader(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
      return jakartaResponse.getHeaders(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
      return jakartaResponse.getHeaderNames();
    }

    @Override
    public String getCharacterEncoding() {
      return jakartaResponse.getCharacterEncoding();
    }

    @Override
    public String getContentType() {
      return jakartaResponse.getContentType();
    }

    @Override
    public javax.servlet.ServletOutputStream getOutputStream() throws IOException {
      if (cachedOutputStream != null) {
        return cachedOutputStream;
      }
      jakarta.servlet.ServletOutputStream jakartaOutputStream = jakartaResponse.getOutputStream();
      cachedOutputStream = new javax.servlet.ServletOutputStream()
      {
        @Override
        public void write(int b) throws IOException {
          jakartaOutputStream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
          jakartaOutputStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
          jakartaOutputStream.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
          jakartaOutputStream.flush();
        }

        @Override
        public void close() throws IOException {
          jakartaOutputStream.close();
        }

        @Override
        public boolean isReady() {
          return jakartaOutputStream.isReady();
        }

        @Override
        public void setWriteListener(javax.servlet.WriteListener writeListener) {
          jakartaOutputStream.setWriteListener(new jakarta.servlet.WriteListener()
          {
            @Override
            public void onWritePossible() throws IOException {
              writeListener.onWritePossible();
            }

            @Override
            public void onError(Throwable t) {
              writeListener.onError(t);
            }
          });
        }
      };
      return cachedOutputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
      return jakartaResponse.getWriter();
    }

    @Override
    public void setCharacterEncoding(String charset) {
      jakartaResponse.setCharacterEncoding(charset);
    }

    @Override
    public void setContentLength(int len) {
      jakartaResponse.setContentLength(len);
    }

    @Override
    public void setContentLengthLong(long len) {
      jakartaResponse.setContentLengthLong(len);
    }

    @Override
    public void setContentType(String type) {
      jakartaResponse.setContentType(type);
    }

    @Override
    public void setBufferSize(int size) {
      jakartaResponse.setBufferSize(size);
    }

    @Override
    public int getBufferSize() {
      return jakartaResponse.getBufferSize();
    }

    @Override
    public void flushBuffer() throws IOException {
      jakartaResponse.flushBuffer();
    }

    @Override
    public void resetBuffer() {
      jakartaResponse.resetBuffer();
    }

    @Override
    public boolean isCommitted() {
      return jakartaResponse.isCommitted();
    }

    @Override
    public void reset() {
      jakartaResponse.reset();
    }

    @Override
    public void setLocale(Locale loc) {
      jakartaResponse.setLocale(loc);
    }

    @Override
    public Locale getLocale() {
      return jakartaResponse.getLocale();
    }
  }

  /**
   * Wraps a Jakarta HttpServletRequest to implement javax.servlet.http.HttpServletRequest.
   */
  public static class RequestAdapter
      implements javax.servlet.http.HttpServletRequest
  {
    private final jakarta.servlet.http.HttpServletRequest jakartaRequest;

    public RequestAdapter(jakarta.servlet.http.HttpServletRequest jakartaRequest) {
      this.jakartaRequest = jakartaRequest;
    }

    @Override
    public String getAuthType() {
      return jakartaRequest.getAuthType();
    }

    @Override
    public javax.servlet.http.Cookie[] getCookies() {
      jakarta.servlet.http.Cookie[] jakartaCookies = jakartaRequest.getCookies();
      if (jakartaCookies == null) {
        return null;
      }
      javax.servlet.http.Cookie[] cookies = new javax.servlet.http.Cookie[jakartaCookies.length];
      for (int i = 0; i < jakartaCookies.length; i++) {
        jakarta.servlet.http.Cookie jakartaCookie = jakartaCookies[i];
        javax.servlet.http.Cookie cookie = new javax.servlet.http.Cookie(
            jakartaCookie.getName(),
            jakartaCookie.getValue()
        );
        cookie.setDomain(jakartaCookie.getDomain());
        cookie.setPath(jakartaCookie.getPath());
        cookie.setMaxAge(jakartaCookie.getMaxAge());
        cookie.setSecure(jakartaCookie.getSecure());
        cookie.setHttpOnly(jakartaCookie.isHttpOnly());
        cookies[i] = cookie;
      }
      return cookies;
    }

    @Override
    public long getDateHeader(String name) {
      return jakartaRequest.getDateHeader(name);
    }

    @Override
    public String getHeader(String name) {
      return jakartaRequest.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
      return jakartaRequest.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
      return jakartaRequest.getHeaderNames();
    }

    @Override
    public int getIntHeader(String name) {
      return jakartaRequest.getIntHeader(name);
    }

    @Override
    public String getMethod() {
      return jakartaRequest.getMethod();
    }

    @Override
    public String getPathInfo() {
      return jakartaRequest.getPathInfo();
    }

    @Override
    public String getPathTranslated() {
      return jakartaRequest.getPathTranslated();
    }

    @Override
    public String getContextPath() {
      return jakartaRequest.getContextPath();
    }

    @Override
    public String getQueryString() {
      return jakartaRequest.getQueryString();
    }

    @Override
    public String getRemoteUser() {
      return jakartaRequest.getRemoteUser();
    }

    @Override
    public boolean isUserInRole(String role) {
      return jakartaRequest.isUserInRole(role);
    }

    @Override
    public Principal getUserPrincipal() {
      return jakartaRequest.getUserPrincipal();
    }

    @Override
    public String getRequestedSessionId() {
      return jakartaRequest.getRequestedSessionId();
    }

    @Override
    public String getRequestURI() {
      return jakartaRequest.getRequestURI();
    }

    @Override
    public StringBuffer getRequestURL() {
      return jakartaRequest.getRequestURL();
    }

    @Override
    public String getServletPath() {
      return jakartaRequest.getServletPath();
    }

    @Override
    public javax.servlet.http.HttpSession getSession(boolean create) {
      jakarta.servlet.http.HttpSession jakartaSession = jakartaRequest.getSession(create);
      if (jakartaSession == null) {
        return null;
      }
      return new HttpSessionAdapter(jakartaSession);
    }

    @Override
    public javax.servlet.http.HttpSession getSession() {
      return getSession(true);
    }

    @Override
    public String changeSessionId() {
      return jakartaRequest.changeSessionId();
    }

    @Override
    public boolean isRequestedSessionIdValid() {
      return jakartaRequest.isRequestedSessionIdValid();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
      return jakartaRequest.isRequestedSessionIdFromCookie();
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
      return jakartaRequest.isRequestedSessionIdFromURL();
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
      return jakartaRequest.isRequestedSessionIdFromURL();
    }

    @Override
    public boolean authenticate(javax.servlet.http.HttpServletResponse response)
        throws IOException, javax.servlet.ServletException
    {
      throw new UnsupportedOperationException("authenticate not supported in adapter");
    }

    @Override
    public void login(String username, String password) throws javax.servlet.ServletException {
      try {
        jakartaRequest.login(username, password);
      }
      catch (jakarta.servlet.ServletException e) {
        throw new javax.servlet.ServletException(e);
      }
    }

    @Override
    public void logout() throws javax.servlet.ServletException {
      try {
        jakartaRequest.logout();
      }
      catch (jakarta.servlet.ServletException e) {
        throw new javax.servlet.ServletException(e);
      }
    }

    @Override
    public Collection<javax.servlet.http.Part> getParts() throws IOException, javax.servlet.ServletException {
      throw new UnsupportedOperationException("getParts not supported in adapter");
    }

    @Override
    public javax.servlet.http.Part getPart(String name) throws IOException, javax.servlet.ServletException {
      throw new UnsupportedOperationException("getPart not supported in adapter");
    }

    @Override
    public <T extends javax.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass)
        throws IOException, javax.servlet.ServletException
    {
      throw new UnsupportedOperationException("upgrade not supported in adapter");
    }

    @Override
    public Object getAttribute(String name) {
      return jakartaRequest.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
      return jakartaRequest.getAttributeNames();
    }

    @Override
    public String getCharacterEncoding() {
      return jakartaRequest.getCharacterEncoding();
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
      jakartaRequest.setCharacterEncoding(env);
    }

    @Override
    public int getContentLength() {
      return jakartaRequest.getContentLength();
    }

    @Override
    public long getContentLengthLong() {
      return jakartaRequest.getContentLengthLong();
    }

    @Override
    public String getContentType() {
      return jakartaRequest.getContentType();
    }

    @Override
    public javax.servlet.ServletInputStream getInputStream() throws IOException {
      jakarta.servlet.ServletInputStream jakartaInputStream = jakartaRequest.getInputStream();
      return new javax.servlet.ServletInputStream()
      {
        @Override
        public int read() throws IOException {
          return jakartaInputStream.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
          return jakartaInputStream.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
          return jakartaInputStream.read(b, off, len);
        }

        @Override
        public boolean isFinished() {
          return jakartaInputStream.isFinished();
        }

        @Override
        public boolean isReady() {
          return jakartaInputStream.isReady();
        }

        @Override
        public void setReadListener(javax.servlet.ReadListener readListener) {
          jakartaInputStream.setReadListener(new jakarta.servlet.ReadListener()
          {
            @Override
            public void onDataAvailable() throws IOException {
              readListener.onDataAvailable();
            }

            @Override
            public void onAllDataRead() throws IOException {
              readListener.onAllDataRead();
            }

            @Override
            public void onError(Throwable t) {
              readListener.onError(t);
            }
          });
        }
      };
    }

    @Override
    public String getParameter(String name) {
      return jakartaRequest.getParameter(name);
    }

    @Override
    public Enumeration<String> getParameterNames() {
      return jakartaRequest.getParameterNames();
    }

    @Override
    public String[] getParameterValues(String name) {
      return jakartaRequest.getParameterValues(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
      return jakartaRequest.getParameterMap();
    }

    @Override
    public String getProtocol() {
      return jakartaRequest.getProtocol();
    }

    @Override
    public String getScheme() {
      return jakartaRequest.getScheme();
    }

    @Override
    public String getServerName() {
      return jakartaRequest.getServerName();
    }

    @Override
    public int getServerPort() {
      return jakartaRequest.getServerPort();
    }

    @Override
    public BufferedReader getReader() throws IOException {
      return jakartaRequest.getReader();
    }

    @Override
    public String getRemoteAddr() {
      return jakartaRequest.getRemoteAddr();
    }

    @Override
    public String getRemoteHost() {
      return jakartaRequest.getRemoteHost();
    }

    @Override
    public void setAttribute(String name, Object o) {
      jakartaRequest.setAttribute(name, o);
    }

    @Override
    public void removeAttribute(String name) {
      jakartaRequest.removeAttribute(name);
    }

    @Override
    public Locale getLocale() {
      return jakartaRequest.getLocale();
    }

    @Override
    public Enumeration<Locale> getLocales() {
      return jakartaRequest.getLocales();
    }

    @Override
    public boolean isSecure() {
      return jakartaRequest.isSecure();
    }

    @Override
    public javax.servlet.RequestDispatcher getRequestDispatcher(String path) {
      throw new UnsupportedOperationException("getRequestDispatcher not supported in adapter");
    }

    @Override
    public String getRealPath(String path) {
      return jakartaRequest.getServletContext().getRealPath(path);
    }

    @Override
    public int getRemotePort() {
      return jakartaRequest.getRemotePort();
    }

    @Override
    public String getLocalName() {
      return jakartaRequest.getLocalName();
    }

    @Override
    public String getLocalAddr() {
      return jakartaRequest.getLocalAddr();
    }

    @Override
    public int getLocalPort() {
      return jakartaRequest.getLocalPort();
    }

    @Override
    public javax.servlet.ServletContext getServletContext() {
      throw new UnsupportedOperationException("getServletContext not supported in adapter");
    }

    @Override
    public javax.servlet.AsyncContext startAsync() throws IllegalStateException {
      throw new UnsupportedOperationException("startAsync not supported in adapter");
    }

    @Override
    public javax.servlet.AsyncContext startAsync(
        javax.servlet.ServletRequest servletRequest,
        javax.servlet.ServletResponse servletResponse) throws IllegalStateException
    {
      throw new UnsupportedOperationException("startAsync not supported in adapter");
    }

    @Override
    public boolean isAsyncStarted() {
      return jakartaRequest.isAsyncStarted();
    }

    @Override
    public boolean isAsyncSupported() {
      return jakartaRequest.isAsyncSupported();
    }

    @Override
    public javax.servlet.AsyncContext getAsyncContext() {
      throw new UnsupportedOperationException("getAsyncContext not supported in adapter");
    }

    @Override
    public javax.servlet.DispatcherType getDispatcherType() {
      jakarta.servlet.DispatcherType jakartaType = jakartaRequest.getDispatcherType();
      return javax.servlet.DispatcherType.valueOf(jakartaType.name());
    }
  }

  /**
   * Wraps a Jakarta HttpSession to implement javax.servlet.http.HttpSession.
   */
  private static class HttpSessionAdapter
      implements javax.servlet.http.HttpSession
  {
    private final jakarta.servlet.http.HttpSession jakartaSession;

    public HttpSessionAdapter(jakarta.servlet.http.HttpSession jakartaSession) {
      this.jakartaSession = jakartaSession;
    }

    @Override
    public long getCreationTime() {
      return jakartaSession.getCreationTime();
    }

    @Override
    public String getId() {
      return jakartaSession.getId();
    }

    @Override
    public long getLastAccessedTime() {
      return jakartaSession.getLastAccessedTime();
    }

    @Override
    public javax.servlet.ServletContext getServletContext() {
      throw new UnsupportedOperationException("getServletContext not supported in adapter");
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
      jakartaSession.setMaxInactiveInterval(interval);
    }

    @Override
    public int getMaxInactiveInterval() {
      return jakartaSession.getMaxInactiveInterval();
    }

    @Override
    public javax.servlet.http.HttpSessionContext getSessionContext() {
      return null;
    }

    @Override
    public Object getAttribute(String name) {
      return jakartaSession.getAttribute(name);
    }

    @Override
    public Object getValue(String name) {
      return jakartaSession.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
      return jakartaSession.getAttributeNames();
    }

    @Override
    public String[] getValueNames() {
      return jakartaSession.getAttributeNames().asIterator().next().split(",");
    }

    @Override
    public void setAttribute(String name, Object value) {
      jakartaSession.setAttribute(name, value);
    }

    @Override
    public void putValue(String name, Object value) {
      jakartaSession.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
      jakartaSession.removeAttribute(name);
    }

    @Override
    public void removeValue(String name) {
      jakartaSession.removeAttribute(name);
    }

    @Override
    public void invalidate() {
      jakartaSession.invalidate();
    }

    @Override
    public boolean isNew() {
      return jakartaSession.isNew();
    }
  }
}
