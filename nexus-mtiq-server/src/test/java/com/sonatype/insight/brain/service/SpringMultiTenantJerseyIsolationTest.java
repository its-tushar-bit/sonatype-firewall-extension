/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class SpringMultiTenantJerseyIsolationTest
{
  private static final String SINGLE_TENANT_JERSEY_CONFIGURATION_RESOURCE =
      "com/sonatype/insight/brain/spring/config/JerseyConfiguration.class";

  @Test
  public void shouldKeepSingleTenantJerseyConfigurationOutOfParentAndManagementContexts() {
    BeanDefinitionCapture capture = new BeanDefinitionCapture();

    SpringApplicationBuilder parentBuilder = SpringMultiTenantTestInsightBrainServiceTestSupport.newBuilder(
        "management.server.port=-1");
    parentBuilder.initializers(capture.initializerFor("parent"));
    catchThrowable(parentBuilder::run);

    SpringApplicationBuilder managementChildBuilder = new SpringApplicationBuilder(
        enableChildManagementContextConfiguration())
            .profiles("test")
            .properties(
                "spring.main.web-application-type=servlet",
                "spring.main.lazy-initialization=true",
                "spring.main.allow-bean-definition-overriding=true",
                "server.port=0",
                "management.server.port=1");
    managementChildBuilder.initializers(capture.initializerFor("management-child"));
    catchThrowable(managementChildBuilder::run);

    List<BeanOrigin> jerseyFilterDefinitions = capture.beanDefinitionsNamed("jerseyFilter");
    List<BeanOrigin> resourceConfigDefinitions = capture.resourceConfigDefinitions();
    List<BeanOrigin> leakedSingleTenantDefinitions = capture.singleTenantJerseyDefinitions();

    assertThat(capture.contextIds())
        .as("captured context ids")
        .contains("parent", "management-child");

    assertThat(jerseyFilterDefinitions)
        .as("jerseyFilter definitions: %s", jerseyFilterDefinitions)
        .anySatisfy(origin -> {
          assertThat(origin.beanName()).isEqualTo("jerseyFilter");
          assertThat(origin.resourceDescription()).contains("MtiqJerseyConfiguration");
        });

    assertThat(jerseyFilterDefinitions)
        .as("jerseyFilter definitions: %s", jerseyFilterDefinitions)
        .noneSatisfy(origin -> assertThat(origin.resourceDescription())
            .contains(SINGLE_TENANT_JERSEY_CONFIGURATION_RESOURCE));

    assertThat(resourceConfigDefinitions)
        .as("resourceConfig definitions: %s", resourceConfigDefinitions)
        .extracting(BeanOrigin::beanName)
        .contains("mtiqMainResourceConfig", "mtiqAdminResourceConfig")
        .doesNotContain("resourceConfig");

    assertThat(leakedSingleTenantDefinitions)
        .as("single-tenant Jersey definitions leaked into contexts: %s", leakedSingleTenantDefinitions)
        .isEmpty();
  }

  private Class<?> enableChildManagementContextConfiguration() {
    try {
      return Class.forName(
          "org.springframework.boot.actuate.autoconfigure.web.server.EnableChildManagementContextConfiguration");
    }
    catch (ClassNotFoundException e) {
      throw new IllegalStateException("Could not load Spring Boot child management context configuration", e);
    }
  }

  private record BeanOrigin(String contextId, String beanName, String resourceDescription)
  {
  }

  private static final class BeanDefinitionCapture
  {
    private final Set<String> contextIds = new LinkedHashSet<>();

    private final List<BeanOrigin> jerseyFilterDefinitions = new ArrayList<>();

    private final List<BeanOrigin> resourceConfigDefinitions = new ArrayList<>();

    private ApplicationContextInitializer<ConfigurableApplicationContext> initializerFor(String contextId) {
      return context -> context.addBeanFactoryPostProcessor(beanFactory -> {
        contextIds.add(contextId);
        captureBeanDefinition(beanFactory, contextId, "jerseyFilter", jerseyFilterDefinitions);
        captureBeanDefinitionsByType(beanFactory, contextId, ResourceConfig.class, resourceConfigDefinitions);
        throw new StopAfterBeanCapture();
      });
    }

    private void captureBeanDefinition(
        ConfigurableListableBeanFactory beanFactory,
        String contextId,
        String beanName,
        List<BeanOrigin> capturedDefinitions)
    {
      if (beanFactory.containsBeanDefinition(beanName)) {
        capturedDefinitions.add(beanOrigin(contextId, beanName, beanFactory.getBeanDefinition(beanName)));
      }
    }

    private void captureBeanDefinitionsByType(
        ConfigurableListableBeanFactory beanFactory,
        String contextId,
        Class<?> beanType,
        List<BeanOrigin> capturedDefinitions)
    {
      for (String beanName : beanFactory.getBeanNamesForType(beanType, true, false)) {
        if (beanFactory.containsBeanDefinition(beanName)) {
          capturedDefinitions.add(beanOrigin(contextId, beanName, beanFactory.getBeanDefinition(beanName)));
        }
      }
    }

    private BeanOrigin beanOrigin(String contextId, String beanName, BeanDefinition beanDefinition) {
      String resourceDescription = beanDefinition.getResourceDescription();
      return new BeanOrigin(contextId, beanName, resourceDescription == null ? "" : resourceDescription);
    }

    private Set<String> contextIds() {
      return contextIds;
    }

    private List<BeanOrigin> beanDefinitionsNamed(String beanName) {
      return jerseyFilterDefinitions.stream()
          .filter(origin -> origin.beanName().equals(beanName))
          .collect(Collectors.toList());
    }

    private List<BeanOrigin> resourceConfigDefinitions() {
      return resourceConfigDefinitions;
    }

    private List<BeanOrigin> singleTenantJerseyDefinitions() {
      List<BeanOrigin> leakedDefinitions = new ArrayList<>();
      for (BeanOrigin origin : jerseyFilterDefinitions) {
        if (origin.resourceDescription().contains(SINGLE_TENANT_JERSEY_CONFIGURATION_RESOURCE)) {
          leakedDefinitions.add(origin);
        }
      }
      for (BeanOrigin origin : resourceConfigDefinitions) {
        if (origin.beanName().equals("resourceConfig")
            || origin.resourceDescription().contains(SINGLE_TENANT_JERSEY_CONFIGURATION_RESOURCE))
        {
          leakedDefinitions.add(origin);
        }
      }
      return leakedDefinitions;
    }
  }
}
