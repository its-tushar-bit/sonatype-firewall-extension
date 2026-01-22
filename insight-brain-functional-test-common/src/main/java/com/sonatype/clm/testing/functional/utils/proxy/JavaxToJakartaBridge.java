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
import java.util.UUID;

import jakarta.servlet.ServletConnection;

/**
 * Reverse bridge classes to adapt javax servlet API types to Jakarta servlet API types.
 * Required to convert javax types from insight-test-reverse-proxy into jakarta types
 * used by the application.
 */
public class JavaxToJakartaBridge
{
  /**
   * Wraps a javax HttpServletRequest to implement jakarta.servlet.http.HttpServletRequest.
   */
  public static class RequestAdapter
      implements jakarta.servlet.http.HttpServletRequest
  {
    private final javax.servlet.http.HttpServletRequest javaxRequest;

    public RequestAdapter(javax.servlet.http.HttpServletRequest javaxRequest) {
      this.javaxRequest = javaxRequest;
    }

    @Override
    public String getAuthType() {
      return javaxRequest.getAuthType();
    }

    @Override
    public jakarta.servlet.http.Cookie[] getCookies() {
      javax.servlet.http.Cookie[] javaxCookies = javaxRequest.getCookies();
      if (javaxCookies == null) {
        return null;
      }
      jakarta.servlet.http.Cookie[] jakartaCookies = new jakarta.servlet.http.Cookie[javaxCookies.length];
      for (int i = 0; i < javaxCookies.length; i++) {
        javax.servlet.http.Cookie jc = javaxCookies[i];
        jakarta.servlet.http.Cookie nc = new jakarta.servlet.http.Cookie(jc.getName(), jc.getValue());
        nc.setDomain(jc.getDomain());
        nc.setPath(jc.getPath());
        nc.setMaxAge(jc.getMaxAge());
        nc.setSecure(jc.getSecure());
        nc.setHttpOnly(jc.isHttpOnly());
        jakartaCookies[i] = nc;
      }
      return jakartaCookies;
    }

    @Override
    public long getDateHeader(String name) {
      return javaxRequest.getDateHeader(name);
    }

    @Override
    public String getHeader(String name) {
      return javaxRequest.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
      return javaxRequest.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
      return javaxRequest.getHeaderNames();
    }

    @Override
    public int getIntHeader(String name) {
      return javaxRequest.getIntHeader(name);
    }

    @Override
    public String getMethod() {
      return javaxRequest.getMethod();
    }

    @Override
    public String getPathInfo() {
      return javaxRequest.getPathInfo();
    }

    @Override
    public String getPathTranslated() {
      return javaxRequest.getPathTranslated();
    }

    @Override
    public String getContextPath() {
      return javaxRequest.getContextPath();
    }

    @Override
    public String getQueryString() {
      return javaxRequest.getQueryString();
    }

    @Override
    public String getRemoteUser() {
      return javaxRequest.getRemoteUser();
    }

    @Override
    public boolean isUserInRole(String role) {
      return javaxRequest.isUserInRole(role);
    }

    @Override
    public Principal getUserPrincipal() {
      return javaxRequest.getUserPrincipal();
    }

    @Override
    public String getRequestedSessionId() {
      return javaxRequest.getRequestedSessionId();
    }

    @Override
    public String getRequestURI() {
      return javaxRequest.getRequestURI();
    }

    @Override
    public StringBuffer getRequestURL() {
      return javaxRequest.getRequestURL();
    }

    @Override
    public String getServletPath() {
      return javaxRequest.getServletPath();
    }

    @Override
    public jakarta.servlet.http.HttpSession getSession(boolean create) {
      throw new UnsupportedOperationException("HttpSession bridging not implemented");
    }

    @Override
    public jakarta.servlet.http.HttpSession getSession() {
      throw new UnsupportedOperationException("HttpSession bridging not implemented");
    }

    @Override
    public String changeSessionId() {
      return javaxRequest.changeSessionId();
    }

    @Override
    public boolean isRequestedSessionIdValid() {
      return javaxRequest.isRequestedSessionIdValid();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
      return javaxRequest.isRequestedSessionIdFromCookie();
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
      return javaxRequest.isRequestedSessionIdFromURL();
    }

    @Override
    public boolean authenticate(jakarta.servlet.http.HttpServletResponse response) {
      throw new UnsupportedOperationException("authenticate bridging not implemented");
    }

    @Override
    public void login(String username, String password) {
      throw new UnsupportedOperationException("login bridging not implemented");
    }

    @Override
    public void logout() {
      throw new UnsupportedOperationException("logout bridging not implemented");
    }

    @Override
    public Collection<jakarta.servlet.http.Part> getParts() {
      throw new UnsupportedOperationException("getParts bridging not implemented");
    }

    @Override
    public jakarta.servlet.http.Part getPart(String name) {
      throw new UnsupportedOperationException("getPart bridging not implemented");
    }

    @Override
    public <T extends jakarta.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) {
      throw new UnsupportedOperationException("upgrade bridging not implemented");
    }

    @Override
    public Object getAttribute(String name) {
      return javaxRequest.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
      return javaxRequest.getAttributeNames();
    }

    @Override
    public String getCharacterEncoding() {
      return javaxRequest.getCharacterEncoding();
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
      try {
        javaxRequest.setCharacterEncoding(env);
      }
      catch (UnsupportedEncodingException e) {
        throw e;
      }
    }

    @Override
    public int getContentLength() {
      return javaxRequest.getContentLength();
    }

    @Override
    public long getContentLengthLong() {
      return javaxRequest.getContentLengthLong();
    }

    @Override
    public String getContentType() {
      return javaxRequest.getContentType();
    }

    @Override
    public jakarta.servlet.ServletInputStream getInputStream() throws IOException {
      javax.servlet.ServletInputStream javaxInputStream = javaxRequest.getInputStream();
      return new jakarta.servlet.ServletInputStream()
      {
        @Override
        public int read() throws IOException {
          return javaxInputStream.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
          return javaxInputStream.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
          return javaxInputStream.read(b, off, len);
        }

        @Override
        public boolean isFinished() {
          return javaxInputStream.isFinished();
        }

        @Override
        public boolean isReady() {
          return javaxInputStream.isReady();
        }

        @Override
        public void setReadListener(jakarta.servlet.ReadListener readListener) {
          javaxInputStream.setReadListener(new javax.servlet.ReadListener()
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
      return javaxRequest.getParameter(name);
    }

    @Override
    public Enumeration<String> getParameterNames() {
      return javaxRequest.getParameterNames();
    }

    @Override
    public String[] getParameterValues(String name) {
      return javaxRequest.getParameterValues(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
      return javaxRequest.getParameterMap();
    }

    @Override
    public String getProtocol() {
      return javaxRequest.getProtocol();
    }

    @Override
    public String getScheme() {
      return javaxRequest.getScheme();
    }

    @Override
    public String getServerName() {
      return javaxRequest.getServerName();
    }

    @Override
    public int getServerPort() {
      return javaxRequest.getServerPort();
    }

    @Override
    public BufferedReader getReader() throws IOException {
      return javaxRequest.getReader();
    }

    @Override
    public String getRemoteAddr() {
      return javaxRequest.getRemoteAddr();
    }

    @Override
    public String getRemoteHost() {
      return javaxRequest.getRemoteHost();
    }

    @Override
    public void setAttribute(String name, Object o) {
      javaxRequest.setAttribute(name, o);
    }

    @Override
    public void removeAttribute(String name) {
      javaxRequest.removeAttribute(name);
    }

    @Override
    public Locale getLocale() {
      return javaxRequest.getLocale();
    }

    @Override
    public Enumeration<Locale> getLocales() {
      return javaxRequest.getLocales();
    }

    @Override
    public boolean isSecure() {
      return javaxRequest.isSecure();
    }

    @Override
    public jakarta.servlet.RequestDispatcher getRequestDispatcher(String path) {
      throw new UnsupportedOperationException("getRequestDispatcher bridging not implemented");
    }

    @Override
    public int getRemotePort() {
      return javaxRequest.getRemotePort();
    }

    @Override
    public String getLocalName() {
      return javaxRequest.getLocalName();
    }

    @Override
    public String getLocalAddr() {
      return javaxRequest.getLocalAddr();
    }

    @Override
    public int getLocalPort() {
      return javaxRequest.getLocalPort();
    }

    @Override
    public jakarta.servlet.ServletContext getServletContext() {
      throw new UnsupportedOperationException("getServletContext bridging not implemented");
    }

    @Override
    public jakarta.servlet.AsyncContext startAsync() {
      throw new UnsupportedOperationException("startAsync bridging not implemented");
    }

    @Override
    public jakarta.servlet.AsyncContext startAsync(jakarta.servlet.ServletRequest servletRequest,
                                                    jakarta.servlet.ServletResponse servletResponse)
    {
      throw new UnsupportedOperationException("startAsync bridging not implemented");
    }

    @Override
    public boolean isAsyncStarted() {
      return javaxRequest.isAsyncStarted();
    }

    @Override
    public boolean isAsyncSupported() {
      return javaxRequest.isAsyncSupported();
    }

    @Override
    public jakarta.servlet.AsyncContext getAsyncContext() {
      throw new UnsupportedOperationException("getAsyncContext bridging not implemented");
    }

    @Override
    public jakarta.servlet.DispatcherType getDispatcherType() {
      String dispatcherType = javaxRequest.getDispatcherType().name();
      return jakarta.servlet.DispatcherType.valueOf(dispatcherType);
    }

    @Override
    public String getRequestId() {
      return UUID.randomUUID().toString();
    }

    @Override
    public String getProtocolRequestId() {
      return UUID.randomUUID().toString();
    }

    @Override
    public ServletConnection getServletConnection() {
      return null;
    }
  }

  /**
   * Wraps a javax HttpServletResponse to implement jakarta.servlet.http.HttpServletResponse.
   */
  public static class ResponseAdapter
      implements jakarta.servlet.http.HttpServletResponse
  {
    private final javax.servlet.http.HttpServletResponse javaxResponse;

    private jakarta.servlet.ServletOutputStream cachedOutputStream;

    public ResponseAdapter(javax.servlet.http.HttpServletResponse javaxResponse) {
      this.javaxResponse = javaxResponse;
    }

    @Override
    public void addCookie(jakarta.servlet.http.Cookie cookie) {
      javax.servlet.http.Cookie javaxCookie = new javax.servlet.http.Cookie(
          cookie.getName(),
          cookie.getValue()
      );
      javaxCookie.setDomain(cookie.getDomain());
      javaxCookie.setPath(cookie.getPath());
      javaxCookie.setMaxAge(cookie.getMaxAge());
      javaxCookie.setSecure(cookie.getSecure());
      javaxCookie.setHttpOnly(cookie.isHttpOnly());
      javaxResponse.addCookie(javaxCookie);
    }

    @Override
    public boolean containsHeader(String name) {
      return javaxResponse.containsHeader(name);
    }

    @Override
    public String encodeURL(String url) {
      return javaxResponse.encodeURL(url);
    }

    @Override
    public String encodeRedirectURL(String url) {
      return javaxResponse.encodeRedirectURL(url);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
      javaxResponse.sendError(sc, msg);
    }

    @Override
    public void sendError(int sc) throws IOException {
      javaxResponse.sendError(sc);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
      javaxResponse.sendRedirect(location);
    }

    @Override
    public void setDateHeader(String name, long date) {
      javaxResponse.setDateHeader(name, date);
    }

    @Override
    public void addDateHeader(String name, long date) {
      javaxResponse.addDateHeader(name, date);
    }

    @Override
    public void setHeader(String name, String value) {
      javaxResponse.setHeader(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
      javaxResponse.addHeader(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
      javaxResponse.setIntHeader(name, value);
    }

    @Override
    public void addIntHeader(String name, int value) {
      javaxResponse.addIntHeader(name, value);
    }

    @Override
    public void setStatus(int sc) {
      javaxResponse.setStatus(sc);
    }

    @Override
    public int getStatus() {
      return javaxResponse.getStatus();
    }

    @Override
    public String getHeader(String name) {
      return javaxResponse.getHeader(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
      return javaxResponse.getHeaders(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
      return javaxResponse.getHeaderNames();
    }

    @Override
    public String getCharacterEncoding() {
      return javaxResponse.getCharacterEncoding();
    }

    @Override
    public String getContentType() {
      return javaxResponse.getContentType();
    }

    @Override
    public jakarta.servlet.ServletOutputStream getOutputStream() throws IOException {
      if (cachedOutputStream != null) {
        return cachedOutputStream;
      }
      javax.servlet.ServletOutputStream javaxOutputStream = javaxResponse.getOutputStream();
      cachedOutputStream = new jakarta.servlet.ServletOutputStream()
      {
        @Override
        public void write(int b) throws IOException {
          javaxOutputStream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
          javaxOutputStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
          javaxOutputStream.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
          javaxOutputStream.flush();
        }

        @Override
        public void close() throws IOException {
          javaxOutputStream.close();
        }

        @Override
        public boolean isReady() {
          return javaxOutputStream.isReady();
        }

        @Override
        public void setWriteListener(jakarta.servlet.WriteListener writeListener) {
          javaxOutputStream.setWriteListener(new javax.servlet.WriteListener()
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
      return javaxResponse.getWriter();
    }

    @Override
    public void setCharacterEncoding(String charset) {
      javaxResponse.setCharacterEncoding(charset);
    }

    @Override
    public void setContentLength(int len) {
      javaxResponse.setContentLength(len);
    }

    @Override
    public void setContentLengthLong(long len) {
      javaxResponse.setContentLengthLong(len);
    }

    @Override
    public void setContentType(String type) {
      javaxResponse.setContentType(type);
    }

    @Override
    public void setBufferSize(int size) {
      javaxResponse.setBufferSize(size);
    }

    @Override
    public int getBufferSize() {
      return javaxResponse.getBufferSize();
    }

    @Override
    public void flushBuffer() throws IOException {
      javaxResponse.flushBuffer();
    }

    @Override
    public void resetBuffer() {
      javaxResponse.resetBuffer();
    }

    @Override
    public boolean isCommitted() {
      return javaxResponse.isCommitted();
    }

    @Override
    public void reset() {
      javaxResponse.reset();
    }

    @Override
    public void setLocale(Locale loc) {
      javaxResponse.setLocale(loc);
    }

    @Override
    public Locale getLocale() {
      return javaxResponse.getLocale();
    }
  }
}
