/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.api.IqOnlyEndpoint;
import jakarta.ws.rs.Path;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class JerseyResourceRegistryTest
{
  @Test
  public void shouldIncludeIqOnlyResourcesWhenBuildingSingleTenantRegistry() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(TestJerseyBeans.class);
      context.refresh();

      JerseyResourceRegistry registry = new JerseyResourceRegistry(context);

      assertThat(registry.getComponents())
          .extracting(instance -> instance.getClass().getName())
          .contains(RegularTestResource.class.getName(), IqOnlyTestResource.class.getName());
    }
  }

  @Configuration
  static class TestJerseyBeans
  {
    @Bean
    RegularTestResource regularTestResource() {
      return new RegularTestResource();
    }

    @Bean
    IqOnlyTestResource iqOnlyTestResource() {
      return new IqOnlyTestResource();
    }
  }

  @Path("rest/regular")
  static class RegularTestResource
  {
  }

  @IqOnlyEndpoint
  @Path("rest/support")
  static class IqOnlyTestResource
  {
  }
}
