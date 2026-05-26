/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jersey3.MetricsFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.audit.AuditContainerRequestFilter;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.jaxrs.ComponentIdentifierParamConverterProvider;
import jakarta.servlet.Filter;
import java.util.Collection;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jersey servlet configuration for JAX-RS resources.
 * Enables all existing REST resources to work without modification.
 *
 * <p>
 * We register Jersey as a filter at the root path because resource classes
 * include the full path prefix in their @Path annotations (e.g., "rest/product/version",
 * "api/v2/applications"). This matches the Dropwizard behavior where the root path was
 * empty, allowing resources to handle the complete path.
 *
 * <p>
 * Jersey is configured as a filter (not a servlet) with FORWARD_ON_404 to forward
 * requests that don't match any JAX-RS resources to the next handler in the chain.
 * This allows endpoints like /actuator/* and /ping to be handled by Spring Boot servlets.
 *
 * <p>
 * Note: Spring Boot's default Jersey auto-configuration would register at "/*" by default,
 * but we explicitly register our own to control Jersey component registration and avoid conflicts.
 *
 * <p>
 * This configuration is intended for the single-tenant IQ Server. It is guarded by
 * an explicit MTIQ marker property so that it does not activate in the multi-tenant
 * (MTIQ) context where MTIQ-specific Jersey configurations provide the REST surfaces.
 * Without this guard, MTIQ startup can activate both single-tenant and MTIQ Jersey
 * configurations, producing multiple {@link ResourceConfig} beans and causing startup
 * failures.
 */
@Configuration
@ConditionalOnProperty(name = "sonatype.mtiq.enabled", havingValue = "false", matchIfMissing = true)
public class JerseyConfiguration
{

  private static final Logger log = LoggerFactory.getLogger(JerseyConfiguration.class);

  /**
   * Register Jersey as a filter at root path with 404 forwarding.
   *
   * <p>
   * Resources define their full paths including prefixes ("rest/...", "api/..."),
   * so we register at "/*" to match the Dropwizard behavior where no path prefix was
   * stripped by the servlet container.
   *
   * <p>
   * Using a filter instead of a servlet allows us to forward requests that don't
   * match any JAX-RS resource to the next handler in the filter chain (e.g., Spring
   * MVC for actuator endpoints, other servlets for /ping).
   */
  @Bean
  public JerseyResourceRegistry jerseyResourceRegistry(final ListableBeanFactory beanFactory) {
    return new JerseyResourceRegistry(beanFactory);
  }

  @Bean
  public ResourceConfig resourceConfig(
      final JerseyResourceRegistry jerseyResourceRegistry,
      final InsightJacksonMessageBodyProvider insightJacksonMessageBodyProvider,
      final MetricRegistry metricRegistry)
  {
    Collection<Object> components = jerseyResourceRegistry.getComponents();
    ResourceConfig resourceConfig = new ResourceConfig();
    resourceConfig.register(MultiPartFeature.class);
    resourceConfig.register(insightJacksonMessageBodyProvider);
    resourceConfig.register(new MetricsFeature(metricRegistry));
    resourceConfig.register(new FuzzyEnumParamConverterProvider());
    components.forEach(resourceConfig::register);
    resourceConfig.property("jersey.config.servlet.filter.forwardOn404", true);
    log.info("Registering {} Jersey components from Spring beans", components.size());
    return resourceConfig;
  }

  @Bean
  public FilterRegistrationBean<Filter> jerseyFilter(
      final ResourceConfig resourceConfig,
      final JerseyResourceRegistry jerseyResourceRegistry)
  {
    ServletContainer container = new ServletContainer(resourceConfig);
    JerseyRequestMatcher matcher = JerseyRequestMatcher.fromComponents(jerseyResourceRegistry.getComponents());
    SelectiveJerseyFilter selectiveJerseyFilter = new SelectiveJerseyFilter(container, matcher);
    FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(selectiveJerseyFilter);
    registration.addUrlPatterns("/*");
    registration.setName("jersey");
    registration.setOrder(SecurityConfiguration.SHIRO_FILTER_ORDER + 50); // Must run after Shiro filter
    log.info("Registering selective Jersey filter for {}", matcher.describeHandledPaths());
    return registration;
  }

  /**
   * Provides the ComponentIdentifierParamConverterProvider as a Spring bean.
   * This is required for JAX-RS resources to use ComponentIdentifier as parameters.
   */
  @Bean
  public InsightJacksonMessageBodyProvider insightJacksonMessageBodyProvider(ObjectMapper objectMapper) {
    return new InsightJacksonMessageBodyProvider(objectMapper);
  }

  @Bean
  public ComponentIdentifierParamConverterProvider componentIdentifierParamConverterProvider(
      ObjectMapper objectMapper)
  {
    return new ComponentIdentifierParamConverterProvider(objectMapper);
  }

  @Bean
  public AuditContainerRequestFilter auditContainerRequestFilter(
      ApplicationDAO applicationDAO,
      OrganizationDAO organizationDAO,
      RepositoryDAO repositoryDAO,
      RepositoryManagerDAO repositoryManagerDAO)
  {
    return new SpringManagedAuditContainerRequestFilter(
        applicationDAO,
        organizationDAO,
        repositoryDAO,
        repositoryManagerDAO);
  }
}
