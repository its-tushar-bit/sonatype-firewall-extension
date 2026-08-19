/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.audit.AuditContainerRequestFilter;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.spring.config.InsightJacksonMessageBodyProvider;
import com.sonatype.insight.brain.spring.config.JerseyRequestMatcher;
import com.sonatype.insight.brain.spring.config.JerseyResourceRegistry;
import com.sonatype.insight.brain.spring.config.SelectiveJerseyFilter;
import com.sonatype.insight.brain.spring.config.SpringManagedAuditContainerRequestFilter;
import com.sonatype.insight.jaxrs.ComponentIdentifierParamConverterProvider;
import jakarta.servlet.Filter;
import java.util.Collection;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Configuration
@Import(MtiqObjectMapperConfiguration.class)
public class MtiqJerseyConfiguration
{
  private static final Logger log = LoggerFactory.getLogger(MtiqJerseyConfiguration.class);

  private AdminResourceBundle createAdminResourceBundle() {
    return new AdminResourceBundle(AdminApiPaths.ADMIN_PATH);
  }

  @Bean
  @Primary
  public JerseyResourceRegistry jerseyResourceRegistry(final ListableBeanFactory beanFactory) {
    return new JerseyResourceRegistry(beanFactory);
  }

  @Bean(name = "mtiqMainResourceConfig")
  @Primary
  public ResourceConfig resourceConfig(
      final JerseyResourceRegistry jerseyResourceRegistry,
      final InsightJacksonMessageBodyProvider insightJacksonMessageBodyProvider)
  {
    Collection<Object> components = jerseyResourceRegistry.getComponents();
    ResourceConfig resourceConfig = new ResourceConfig();
    resourceConfig.register(MultiPartFeature.class);
    resourceConfig.register(insightJacksonMessageBodyProvider);
    resourceConfig.register(new com.sonatype.insight.brain.spring.config.FuzzyEnumParamConverterProvider());
    AdminResourceBundle adminResourceBundle = createAdminResourceBundle();
    int mainCount = 0;
    int adminCount = 0;
    int excludedCount = 0;

    for (Object component : components) {
      MtiqResourceInstaller.RegistrationDestination destination =
          MtiqResourceInstaller.register(component, resourceConfig, adminResourceBundle);
      if (destination == MtiqResourceInstaller.RegistrationDestination.MAIN) {
        mainCount++;
      }
      else if (destination == MtiqResourceInstaller.RegistrationDestination.ADMIN) {
        adminCount++;
      }
      else {
        excludedCount++;
      }
    }

    resourceConfig.property("jersey.config.servlet.filter.forwardOn404", true);
    log.info(
        "Registering {} MTIQ Jersey components from Spring beans ({} admin resources, {} IQ-only exclusions)",
        mainCount,
        adminCount,
        excludedCount);
    return resourceConfig;
  }

  @Bean
  @Primary
  public FilterRegistrationBean<Filter> jerseyFilter(
      @Qualifier("mtiqMainResourceConfig") final ResourceConfig resourceConfig,
      final JerseyResourceRegistry jerseyResourceRegistry)
  {
    ServletContainer container = new ServletContainer(resourceConfig);
    JerseyRequestMatcher matcher = JerseyRequestMatcher.fromComponents(jerseyResourceRegistry.getComponents());
    SelectiveJerseyFilter selectiveJerseyFilter = new SelectiveJerseyFilter(container, matcher);
    FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(selectiveJerseyFilter);
    registration.addUrlPatterns("/*");
    registration.setName("jersey");
    registration.setOrder(100);
    log.info("Registering selective MTIQ Jersey filter for {}", matcher.describeHandledPaths());
    return registration;
  }

  @Bean
  @Primary
  public InsightJacksonMessageBodyProvider insightJacksonMessageBodyProvider(
      @Qualifier(MtiqObjectMapperConfiguration.MTIQ_JERSEY_OBJECT_MAPPER) ObjectMapper objectMapper)
  {
    return new InsightJacksonMessageBodyProvider(objectMapper);
  }

  @Bean
  @Primary
  public ComponentIdentifierParamConverterProvider componentIdentifierParamConverterProvider(
      @Qualifier(MtiqObjectMapperConfiguration.MTIQ_JERSEY_OBJECT_MAPPER) ObjectMapper objectMapper)
  {
    return new ComponentIdentifierParamConverterProvider(objectMapper);
  }

  @Bean
  @Primary
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
