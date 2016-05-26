/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @since 1.21
 */
@RunWith(Parameterized.class)
public class HttpHeaderValidatorFilterTest
{

  @Mock
  private FilterChain chain;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private PrintWriter writer;

  private HttpHeaderValidatorFilter filter;

  private final String[] headers;

  private final String invalidHeaderName;

  public HttpHeaderValidatorFilterTest(String[] headers, String invalidHeaderName) {
    this.headers = headers;
    this.invalidHeaderName = invalidHeaderName;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        // valid RFC 1123 hostname with valid schemes
        {new String[]{"iq.sonatype.com", "http"}, null},
        {new String[]{"iq.sonatype.com", "https"}, null},
        // valid RFC 1123 hostname and port with valid schemes
        {new String[]{"iq.sonatype.com:8070", "http"}, null},
        {new String[]{"iq.sonatype.com:8070", "https"}, null},
        // valid IPv4 Address with valid schemes
        {new String[]{"207.223.241.78", "http"}, null},
        {new String[]{"207.223.241.78", "https"}, null},
        // valid IPv4 Address and port with valid schemes
        {new String[]{"207.223.241.78:8070", "http"}, null},
        {new String[]{"207.223.241.78:8070", "https"}, null},
        // valid IPv6 Address with valid schemes
        {new String[]{"[1762:0:0:0:0:B03:1:AF18]", "http"}, null},
        {new String[]{"[1762:0:0:0:0:B03:1:AF18]", "https"}, null},
        // valid IPv6 Address and port with valid schemes
        {new String[]{"[1762:0:0:0:0:B03:1:AF18]:8070", "http"}, null},
        {new String[]{"[1762:0:0:0:0:B03:1:AF18]:8070", "https"}, null},
        // invalid RFC 1123 hostname with valid schemes
        {new String[]{"\"><script>alert(document.domain)</script>", "http"}, "Host"},
        {new String[]{"\"><script>alert(document.domain)</script>", "https"}, "Host"},
        {new String[]{"localhost\"><script>alert(document.domain)</script>", "http"}, "Host"},
        {new String[]{"localhost\"><script>alert(document.domain)</script>", "https"}, "Host"},
        // valid RFC 1123 hostname with invalid schemes
        {new String[]{"iq.sonatype.com", "http\"><script>alert(document.domain)</script>"}, "X-Forwarded-Proto"},
        {new String[]{"iq.sonatype.com", "https\"><script>alert(document.domain)</script>"}, "X-Forwarded-Proto"},
        // valid IPv4 Address with invalid schemes
        {new String[]{"207.223.241.78", "http\"><script>alert(document.domain)</script>"}, "X-Forwarded-Proto"},
        {new String[]{"207.223.241.78", "https\"><script>alert(document.domain)</script>"}, "X-Forwarded-Proto"},
        // valid IPv6 Address with invalid schemes
        {
            new String[]{"[1762:0:0:0:0:B03:1:AF18]", "http\"><script>alert(document.domain)</script>"},
            "X-Forwarded-Proto"
        },
        {
            new String[]{"[1762:0:0:0:0:B03:1:AF18]", "https\"><script>alert(document.domain)</script>"},
            "X-Forwarded-Proto"
        },
        // valid RFC 1123 hostname and port with invalid schemes
        {new String[]{"iq.sonatype.com:8070", "http\"><script>alert(document.domain)</script>"}, "X-Forwarded-Proto"},
        {new String[]{"iq.sonatype.com:8070", "https\"><script>alert(document.domain)</script>"}, "X-Forwarded-Proto"},
        // valid IPv4 Address and port with invalid schemes
        {new String[]{"207.223.241.78:8070", "http\"><script>alert(document.domain)</script>"}, "X-Forwarded-Proto"},
        {new String[]{"207.223.241.78:8070", "https\"><script>alert(document.domain)</script>"}, "X-Forwarded-Proto"},
        // valid IPv6 Address and port with invalid schemes
        {
            new String[]{"[1762:0:0:0:0:B03:1:AF18]:8070", "http\"><script>alert(document.domain)</script>"},
            "X-Forwarded-Proto"
        },
        {
            new String[]{"[1762:0:0:0:0:B03:1:AF18]:8070", "https\"><script>alert(document.domain)</script>"},
            "X-Forwarded-Proto"
        }
    });
  }

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    filter = new HttpHeaderValidatorFilter();
    when(response.getWriter()).thenReturn(writer);
  }

  @Test
  public void testFilter() throws Exception {
    when(request.getHeader(anyString())).thenAnswer(new Answer<String>()
    {
      @Override
      public String answer(final InvocationOnMock invocation) throws Throwable {
        String headerName = (String) invocation.getArguments()[0];
        switch (headerName) {
          case "Host":
            return headers[0];
          case "X-Forwarded-Proto":
            return headers[1];
          default:
            throw new IllegalStateException("No header mapping set for header " + headerName);
        }
      }
    });
    filter.doFilter(request, response, chain);
    if (invalidHeaderName == null) {
      verify(chain).doFilter(request, response);
      verifyZeroInteractions(response);
      verifyZeroInteractions(writer);
    }
    else {
      verifyZeroInteractions(chain);
      verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
      verify(response).setContentType(HttpHeaderValidatorFilter.CONTENT_TYPE);
      verify(response).getWriter();
      verify(writer).print("Illegal header value detected in '" + invalidHeaderName + "'");
    }
  }

}
