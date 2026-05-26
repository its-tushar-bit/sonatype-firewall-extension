/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

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
      {new String[]{"iq.sonatype.com", "http", "proto=http"}, null},
      {new String[]{"iq.sonatype.com", "https", "proto=https"}, null},
      // valid RFC 1123 hostname and port with valid schemes
      {new String[]{"iq.sonatype.com:8070", "http", "proto=http"}, null},
      {new String[]{"iq.sonatype.com:8070", "https", "proto=https"}, null},
      // valid IPv4 Address with valid schemes
      {new String[]{"207.223.241.78", "http", "proto=http"}, null},
      {new String[]{"207.223.241.78", "https", "proto=https"}, null},
      // valid IPv4 Address and port with valid schemes
      {new String[]{"207.223.241.78:8070", "http", "proto=http"}, null},
      {new String[]{"207.223.241.78:8070", "https", "proto=https"}, null},
      // valid IPv6 Address with valid schemes
      {new String[]{"[1762:0:0:0:0:B03:1:AF18]", "http", "proto=http"}, null},
      {new String[]{"[1762:0:0:0:0:B03:1:AF18]", "https", "proto=https"}, null},
      // valid IPv6 Address and port with valid schemes
      {new String[]{"[1762:0:0:0:0:B03:1:AF18]:8070", "http", "proto=http"}, null},
      {new String[]{"[1762:0:0:0:0:B03:1:AF18]:8070", "https", "proto=https"}, null},
      // invalid RFC 1123 hostname with valid schemes
      {new String[]{"\"><script>alert(document.domain)</script>", "http", "proto=http"}, "Host"},
      {new String[]{"\"><script>alert(document.domain)</script>", "https", "proto=https"}, "Host"},
      {new String[]{"localhost\"><script>alert(document.domain)</script>", "http", "proto=http"}, "Host"},
      {new String[]{"localhost\"><script>alert(document.domain)</script>", "https", "proto=https"}, "Host"},
      // valid RFC 1123 hostname with invalid schemes
      {
        new String[]{"iq.sonatype.com", "http\"><script>alert(document.domain)</script>", "proto=http"},
        "X-Forwarded-Proto"
      },
      {
        new String[]{"iq.sonatype.com", "https\"><script>alert(document.domain)</script>", "proto=https"},
        "X-Forwarded-Proto"
      },
      // valid IPv4 Address with invalid schemes
      {
        new String[]{"207.223.241.78", "http\"><script>alert(document.domain)</script>", "proto=http"},
        "X-Forwarded-Proto"
      },
      {
        new String[]{"207.223.241.78", "https\"><script>alert(document.domain)</script>", "proto=https"},
        "X-Forwarded-Proto"
      },
      // valid IPv6 Address with invalid schemes
      {
        new String[]{"[1762:0:0:0:0:B03:1:AF18]", "http\"><script>alert(document.domain)</script>", "proto=http"},
        "X-Forwarded-Proto"
      },
      {
        new String[]{"[1762:0:0:0:0:B03:1:AF18]", "https\"><script>alert(document.domain)</script>", "proto=https"},
        "X-Forwarded-Proto"
      },
      // valid RFC 1123 hostname and port with invalid schemes
      {
        new String[]{"iq.sonatype.com:8070", "http\"><script>alert(document.domain)</script>", "proto=http"},
        "X-Forwarded-Proto"
      },
      {
        new String[]{"iq.sonatype.com:8070", "https\"><script>alert(document.domain)</script>", "proto=https"},
        "X-Forwarded-Proto"
      },
      // valid IPv4 Address and port with invalid schemes
      {
        new String[]{"207.223.241.78:8070", "http\"><script>alert(document.domain)</script>", "proto=http"},
        "X-Forwarded-Proto"
      },
      {
        new String[]{"207.223.241.78:8070", "https\"><script>alert(document.domain)</script>", "proto=https"},
        "X-Forwarded-Proto"
      },
      // valid IPv6 Address and port with invalid schemes
      {
        new String[]{
          "[1762:0:0:0:0:B03:1:AF18]:8070", "http\"><script>alert(document.domain)</script>", "proto=http"
        }, "X-Forwarded-Proto"
      },
      {
        new String[]{
          "[1762:0:0:0:0:B03:1:AF18]:8070", "https\"><script>alert(document.domain)</script>", "proto=https"
        }, "X-Forwarded-Proto"
      },
      // valid Host, X-Forwarded-Proto, and Forwarded
      {new String[]{"iq.sonatype.com", "http", "host=iq.sonatype.com"}, null},
      {new String[]{"iq.sonatype.com", "http", "host=iq.sonatype.com:8070"}, null},
      {new String[]{"iq.sonatype.com", "http", "host=207.223.241.78"}, null},
      {new String[]{"iq.sonatype.com", "http", "host=207.223.241.78:8070"}, null},
      {new String[]{"iq.sonatype.com", "http", "host=[1762:0:0:0:0:B03:1:AF18]"}, null},
      {new String[]{"iq.sonatype.com", "http", "host=[1762:0:0:0:0:B03:1:AF18]:8070"}, null},
      {new String[]{"iq.sonatype.com", "http", "host=[1762::B03:1:AF18]"}, null},
      {new String[]{"iq.sonatype.com", "http", "host=[1762::B03:1:AF18]:8070"}, null},

      {new String[]{"iq.sonatype.com", "http", "by=207.223.241.78"}, null},
      {new String[]{"iq.sonatype.com", "http", "by=207.223.241.78:8070"}, null},
      {new String[]{"iq.sonatype.com", "http", "by=\"[1762:0:0:0:0:B03:1:AF18]\""}, null},
      {new String[]{"iq.sonatype.com", "http", "by=\"[1762:0:0:0:0:B03:1:AF18]:8070\""}, null},
      {new String[]{"iq.sonatype.com", "http", "by=\"[1762::B03:1:AF18]\""}, null},
      {new String[]{"iq.sonatype.com", "http", "by=\"[1762::B03:1:AF18]:8070\""}, null},
      {new String[]{"iq.sonatype.com", "http", "by=\"_hidden\""}, null},
      {new String[]{"iq.sonatype.com", "http", "by=\"_sE_cR-3.t\""}, null},

      {new String[]{"iq.sonatype.com", "http", "for=207.223.241.78"}, null},
      {new String[]{"iq.sonatype.com", "http", "for=207.223.241.78:8070"}, null},
      {new String[]{"iq.sonatype.com", "http", "for=\"[1762:0:0:0:0:B03:1:AF18]\""}, null},
      {new String[]{"iq.sonatype.com", "http", "for=\"[1762:0:0:0:0:B03:1:AF18]:8070\""}, null},
      {new String[]{"iq.sonatype.com", "http", "for=\"[1762::B03:1:AF18]\""}, null},
      {new String[]{"iq.sonatype.com", "http", "for=\"[1762::B03:1:AF18]:8070\""}, null},
      {new String[]{"iq.sonatype.com", "http", "for=\"_hidden\""}, null},
      {new String[]{"iq.sonatype.com", "http", "for=\"_sE_cR-3.t\""}, null},

      {
        new String[]{
          "iq.sonatype.com", "http", "proto=http; host=iq.sonatype.com; by=207.223.241.78; for=207.223.241.78"
        }, null
      },
      {
        new String[]{
          "iq.sonatype.com", "http", "proto=http; host=iq.sonatype.com; by=207.223.241.78; " +
              "for=207.223.241.78, for=\"[1762:0:0:0:0:B03:1:AF18]\""
        }, null
      },
      // valid Host, X-Forwarded-Proto, and invalid Forwarded
      {new String[]{"iq.sonatype.com", "http", "\"><script>alert(document.domain)</script>"}, "Forwarded"},
      {new String[]{"iq.sonatype.com", "http", "proto=http\"><script>alert(document.domain)</script>"}, "Forwarded"},
      {new String[]{"iq.sonatype.com", "http", "host=\"><script>alert(document.domain)</script>"}, "Forwarded"},
    });
  }

  @Before
  public void setUp() throws Exception {
    openMocks(this);
    filter = new HttpHeaderValidatorFilter();
    when(response.getWriter()).thenReturn(writer);
  }

  @Test
  public void testFilter() throws Exception {
    when(request.getHeader(anyString())).thenAnswer((Answer<String>) invocation -> {
      String headerName = (String) invocation.getArguments()[0];
      switch (headerName) {
        case "Host":
          return headers[0];
        case "X-Forwarded-Proto":
          return headers[1];
        case "X-Forwarded-Host":
          return null;
        case "Forwarded":
          return headers[2];
        default:
          throw new IllegalStateException("No header mapping set for header " + headerName);
      }
    });
    filter.doFilter(request, response, chain);
    if (invalidHeaderName == null) {
      verify(chain).doFilter(request, response);
      verifyNoInteractions(response);
      verifyNoInteractions(writer);
    }
    else {
      verifyNoInteractions(chain);
      verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
      verify(response).setContentType(HttpHeaderValidatorFilter.CONTENT_TYPE);
      verify(response).getWriter();
      verify(writer).print("Illegal header value detected in '" + invalidHeaderName + "'");
    }
  }
}
