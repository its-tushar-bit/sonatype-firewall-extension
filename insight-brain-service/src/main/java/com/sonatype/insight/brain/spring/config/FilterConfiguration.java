/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.sonatype.insight.brain.audit.AuditFilter;
import com.sonatype.insight.brain.filter.ThrowableHandler;
import com.sonatype.insight.brain.firewall.FirewallRedirectFilter;
import com.sonatype.insight.brain.landing.IndexCacheControlFilter;
import com.sonatype.insight.brain.security.AuthenticationLoggingFilter;
import com.sonatype.insight.brain.security.ContentTypeOptionsHeaderFilter;
import com.sonatype.insight.brain.security.CspHeaderFilter;
import com.sonatype.insight.brain.security.FrameOptionsHeaderFilter;
import com.sonatype.insight.brain.security.HstsHeaderFilter;
import com.sonatype.insight.brain.security.HttpHeaderValidatorFilter;
import com.sonatype.insight.brain.security.McpLicenseFilter;
import com.sonatype.insight.brain.service.BaseUrlFilter;
import com.sonatype.insight.brain.service.CspFrameHeaderFilter;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * Servlet filter configuration.
 * Converts Dropwizard filter registrations to Spring FilterRegistrationBean.
 *
 * <p>
 * <strong>IMPORTANT:</strong> Shiro-managed filters are handled separately in
 * {@link com.sonatype.insight.brain.security.ShiroFilterRegistrationDisabler} to prevent
 * Spring Boot from auto-registering them outside of Shiro's filter chain management.
 *
 * <p>
 * Individual filters will be registered via their @Component annotations
 * or through their respective configuration classes.
 */
@Configuration
public class FilterConfiguration
{

  private final ThrowableHandler throwableHandler;

  private final BaseUrlFilter baseUrlFilter;

  private final AuditFilter auditFilter;

  private final HttpHeaderValidatorFilter httpHeaderValidatorFilter;

  private final ContentTypeOptionsHeaderFilter contentTypeOptionsHeaderFilter;

  private final HstsHeaderFilter hstsHeaderFilter;

  private final FrameOptionsHeaderFilter frameOptionsHeaderFilter;

  private final McpLicenseFilter mcpLicenseFilter;

  private final IndexCacheControlFilter indexCacheControlFilter;

  private final AuthenticationLoggingFilter authenticationLoggingFilter;

  private final CspHeaderFilter cspHeaderFilter;

  private final CspFrameHeaderFilter cspFrameHeaderFilter;

  private final FirewallRedirectFilter firewallRedirectFilter;

  @Inject
  public FilterConfiguration(
      ThrowableHandler throwableHandler,
      BaseUrlFilter baseUrlFilter,
      AuditFilter auditFilter,
      HttpHeaderValidatorFilter httpHeaderValidatorFilter,
      ContentTypeOptionsHeaderFilter contentTypeOptionsHeaderFilter,
      HstsHeaderFilter hstsHeaderFilter,
      FrameOptionsHeaderFilter frameOptionsHeaderFilter,
      McpLicenseFilter mcpLicenseFilter,
      IndexCacheControlFilter indexCacheControlFilter,
      AuthenticationLoggingFilter authenticationLoggingFilter,
      CspHeaderFilter cspHeaderFilter,
      CspFrameHeaderFilter cspFrameHeaderFilter,
      FirewallRedirectFilter firewallRedirectFilter)
  {
    this.throwableHandler = throwableHandler;
    this.baseUrlFilter = baseUrlFilter;
    this.auditFilter = auditFilter;
    this.httpHeaderValidatorFilter = httpHeaderValidatorFilter;
    this.contentTypeOptionsHeaderFilter = contentTypeOptionsHeaderFilter;
    this.hstsHeaderFilter = hstsHeaderFilter;
    this.frameOptionsHeaderFilter = frameOptionsHeaderFilter;
    this.mcpLicenseFilter = mcpLicenseFilter;
    this.indexCacheControlFilter = indexCacheControlFilter;
    this.authenticationLoggingFilter = authenticationLoggingFilter;
    this.cspHeaderFilter = cspHeaderFilter;
    this.cspFrameHeaderFilter = cspFrameHeaderFilter;
    this.firewallRedirectFilter = firewallRedirectFilter;
  }

  /**
   * Register ThrowableHandler for all requests.
   * Converts uncaught downstream exceptions into mapped HTTP responses.
   */
  @Bean
  public FilterRegistrationBean<ThrowableHandler> throwableHandlerRegistration() {
    return registerFilter(throwableHandler, FilterOrder.THROWABLE_HANDLER, "/*");
  }

  @Bean
  public FilterRegistrationBean<Filter> gzipRequestDecompressionFilterRegistration() {
    return registerFilter(new GzipRequestDecompressionFilter(), FilterOrder.GZIP_REQUEST_DECOMPRESSION, "/*");
  }

  @Bean
  public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilterRegistration() {
    return registerFilter(new ForwardedHeaderFilter(), FilterOrder.FORWARDED_HEADER, "/*");
  }

  /**
   * Register charset filter for static assets.
   * Adds charset=UTF-8 to Content-Type for text-based static resources.
   * This restores the behavior from Dropwizard's AssetBundle.
   */
  @Bean
  public FilterRegistrationBean<StaticAssetsCharsetFilter> staticAssetsCharsetFilterRegistration() {
    return registerFilter(new StaticAssetsCharsetFilter(), FilterOrder.STATIC_ASSETS_CHARSET, "/assets/*");
  }

  @Bean
  public FilterRegistrationBean<LegacyWebCorsFilter> legacyWebCorsFilterRegistration(
      ObjectProvider<DropwizardWebSettings> webSettingsProvider)
  {
    DropwizardWebSettings webSettings = webSettingsProvider.getIfAvailable(DropwizardWebSettings::empty);
    FilterRegistrationBean<LegacyWebCorsFilter> registration = registerFilter(
        new LegacyWebCorsFilter(webSettings.getCorsSettingsOrDefault()),
        FilterOrder.LEGACY_WEB_CORS,
        webSettings.getUrlPattern());
    registration.setEnabled(webSettings.hasCorsSettings());
    return registration;
  }

  /**
   * Register BaseUrl filter for all requests.
   * Captures the active HttpServletRequest for redirect generation.
   */
  @Bean
  public FilterRegistrationBean<BaseUrlFilter> baseUrlFilterRegistration() {
    return registerFilter(baseUrlFilter, FilterOrder.BASE_URL, "/*");
  }

  @Bean
  public FilterRegistrationBean<AuditFilter> auditFilterRegistration() {
    return registerFilter(auditFilter, FilterOrder.AUDIT, AuditFilter.URL_PATTERNS);
  }

  @Bean
  public FilterRegistrationBean<HttpHeaderValidatorFilter> httpHeaderValidatorFilterRegistration() {
    return registerFilter(httpHeaderValidatorFilter, FilterOrder.HTTP_HEADER_VALIDATION,
        HttpHeaderValidatorFilter.URL_PATTERN);
  }

  /**
   * Register Content-Type-Options header filter for all requests.
   * Adds X-Content-Type-Options: nosniff header.
   */
  @Bean
  public FilterRegistrationBean<ContentTypeOptionsHeaderFilter> contentTypeOptionsHeaderFilterRegistration() {
    return registerFilter(contentTypeOptionsHeaderFilter, FilterOrder.CONTENT_TYPE_OPTIONS, "/*");
  }

  /**
   * Register HSTS header filter for all requests.
   * Adds Strict-Transport-Security header to HTTPS responses.
   */
  @Bean
  public FilterRegistrationBean<HstsHeaderFilter> hstsHeaderFilterRegistration() {
    return registerFilter(hstsHeaderFilter, FilterOrder.HSTS, "/*");
  }

  /**
   * Register X-Frame-Options header filter for all requests.
   * Restores the legacy Dropwizard default of DENY.
   */
  @Bean
  public FilterRegistrationBean<FrameOptionsHeaderFilter> frameOptionsHeaderFilterRegistration() {
    return registerFilter(frameOptionsHeaderFilter, FilterOrder.FRAME_OPTIONS, "/*");
  }

  @Bean
  public FilterRegistrationBean<McpLicenseFilter> mcpLicenseFilterRegistration() {
    return registerFilter(mcpLicenseFilter, FilterOrder.MCP_LICENSE, McpLicenseFilter.URL_PATTERNS);
  }

  @Bean
  public FilterRegistrationBean<IndexCacheControlFilter> indexCacheControlFilterRegistration() {
    return registerFilter(indexCacheControlFilter, FilterOrder.INDEX_CACHE_CONTROL,
        IndexCacheControlFilter.URL_PATTERN);
  }

  @Bean
  public FilterRegistrationBean<AuthenticationLoggingFilter> authenticationLoggingFilterRegistration() {
    return registerFilter(authenticationLoggingFilter, FilterOrder.AUTHENTICATION_LOGGING,
        AuthenticationLoggingFilter.URL_PATTERN);
  }

  /**
   * Register CSP header filter for assets.
   * Adds Content-Security-Policy header to responses.
   */
  @Bean
  public FilterRegistrationBean<CspHeaderFilter> cspHeaderFilterRegistration() {
    return registerFilter(cspHeaderFilter, FilterOrder.CSP, CspHeaderFilter.URL_PATTERN);
  }

  /**
   * Register CSP frame header filter for all requests.
   * Adds Content-Security-Policy frame-ancestors header.
   */
  @Bean
  public FilterRegistrationBean<CspFrameHeaderFilter> cspFrameHeaderFilterRegistration() {
    return registerFilter(cspFrameHeaderFilter, FilterOrder.CSP_FRAME, "/*");
  }

  @Bean
  public FilterRegistrationBean<LegacyWebHeaderFilter> legacyWebHeaderFilterRegistration(
      ObjectProvider<DropwizardWebSettings> webSettingsProvider)
  {
    DropwizardWebSettings webSettings = webSettingsProvider.getIfAvailable(DropwizardWebSettings::empty);
    FilterRegistrationBean<LegacyWebHeaderFilter> registration = registerFilter(
        new LegacyWebHeaderFilter(webSettings),
        FilterOrder.LEGACY_WEB_HEADERS,
        webSettings.getUrlPattern());
    registration.setEnabled(webSettings.hasHeaders());
    return registration;
  }

  @Bean
  public FilterRegistrationBean<FirewallRedirectFilter> firewallRedirectFilterRegistration() {
    return registerFilter(firewallRedirectFilter, FilterOrder.FIREWALL_REDIRECT, "/*");
  }

  /**
   * Helper method to register a filter with Spring Boot.
   */
  private <T extends Filter> FilterRegistrationBean<T> registerFilter(T filter, int order, String... patterns) {
    FilterRegistrationBean<T> registration = new FilterRegistrationBean<>();
    registration.setFilter(filter);
    registration.addUrlPatterns(patterns);
    registration.setOrder(order);
    return registration;
  }

  private static final class GzipRequestDecompressionFilter
      implements Filter
  {
    private static final String CONTENT_ENCODING_HEADER = "Content-Encoding";

    /**
     * Maximum decompressed size limit (100MB) to prevent zip bomb attacks.
     * A small gzip payload could expand to gigabytes, causing OOM.
     */
    private static final long MAX_DECOMPRESSED_SIZE = 100 * 1024 * 1024L; // 100MB

    @Override
    public void doFilter(
        final ServletRequest request,
        final ServletResponse response,
        final jakarta.servlet.FilterChain chain) throws IOException, jakarta.servlet.ServletException
    {
      if (request instanceof HttpServletRequest httpServletRequest && isGzipEncoded(httpServletRequest)) {
        chain.doFilter(new GzipHttpServletRequestWrapper(httpServletRequest, MAX_DECOMPRESSED_SIZE), response);
        return;
      }
      chain.doFilter(request, response);
    }

    private boolean isGzipEncoded(final HttpServletRequest request) {
      String contentEncoding = request.getHeader(CONTENT_ENCODING_HEADER);
      return contentEncoding != null && contentEncoding.toLowerCase(Locale.ROOT).contains("gzip");
    }
  }

  private static final class GzipHttpServletRequestWrapper
      extends HttpServletRequestWrapper
  {
    private final long maxDecompressedSize;

    private ServletInputStream gzipInputStream;

    private BufferedReader reader;

    private GzipHttpServletRequestWrapper(final HttpServletRequest request, final long maxDecompressedSize) {
      super(request);
      this.maxDecompressedSize = maxDecompressedSize;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      if (gzipInputStream == null) {
        BoundedInputStream bounded = new BoundedInputStream(
            new GZIPInputStream(super.getInputStream()),
            maxDecompressedSize);
        gzipInputStream = new DelegatingServletInputStream(bounded);
      }
      return gzipInputStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
      if (reader == null) {
        Charset charset = getCharacterEncoding() != null
            ? Charset.forName(getCharacterEncoding())
            : StandardCharsets.UTF_8;
        reader = new BufferedReader(new InputStreamReader(getInputStream(), charset));
      }
      return reader;
    }

    @Override
    public String getHeader(final String name) {
      if ("Content-Encoding".equalsIgnoreCase(name)) {
        return null;
      }
      return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(final String name) {
      if ("Content-Encoding".equalsIgnoreCase(name)) {
        return Collections.emptyEnumeration();
      }
      return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
      Enumeration<String> names = super.getHeaderNames();
      List<String> filtered = new ArrayList<>();
      while (names.hasMoreElements()) {
        String name = names.nextElement();
        if (!"Content-Encoding".equalsIgnoreCase(name)) {
          filtered.add(name);
        }
      }
      return Collections.enumeration(filtered);
    }

    @Override
    public int getContentLength() {
      return -1;
    }

    @Override
    public long getContentLengthLong() {
      return -1L;
    }
  }

  private static final class DelegatingServletInputStream
      extends ServletInputStream
  {
    private final InputStream delegate;

    private boolean finished;

    private DelegatingServletInputStream(final InputStream delegate) {
      this.delegate = delegate;
    }

    @Override
    public int read() throws IOException {
      int b = delegate.read();
      if (b == -1) {
        finished = true;
      }
      return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      int count = delegate.read(b, off, len);
      if (count == -1) {
        finished = true;
      }
      return count;
    }

    @Override
    public boolean isFinished() {
      return finished;
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setReadListener(final ReadListener readListener) {
      throw new UnsupportedOperationException("Async request body reading is not supported for gzip wrapper");
    }
  }

  /**
   * InputStream wrapper that throws IOException after a maximum byte limit is reached.
   * Protects against zip bomb attacks where a small compressed payload expands to huge sizes.
   */
  private static final class BoundedInputStream
      extends InputStream
  {
    private final InputStream delegate;

    private final long maxSize;

    private long bytesRead;

    private BoundedInputStream(final InputStream delegate, final long maxSize) {
      this.delegate = delegate;
      this.maxSize = maxSize;
      this.bytesRead = 0;
    }

    @Override
    public int read() throws IOException {
      int b = delegate.read();
      if (b != -1) {
        bytesRead++;
        checkLimit();
      }
      return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      int count = delegate.read(b, off, len);
      if (count > 0) {
        bytesRead += count;
        checkLimit();
      }
      return count;
    }

    private void checkLimit() throws IOException {
      if (bytesRead > maxSize) {
        throw new IOException(
            "Decompressed request body exceeds maximum allowed size of " + maxSize + " bytes (zip bomb protection)");
      }
    }

    @Override
    public void close() throws IOException {
      delegate.close();
    }

    @Override
    public int available() throws IOException {
      return delegate.available();
    }
  }
}
