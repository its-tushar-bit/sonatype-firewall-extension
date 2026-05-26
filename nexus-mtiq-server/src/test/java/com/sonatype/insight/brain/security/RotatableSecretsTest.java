/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;

import org.junit.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import static org.assertj.core.api.Assertions.assertThat;

public class RotatableSecretsTest
{
  @Test
  public void testRotatableSecretsInterface_extendAbstractOperationalSqlDAO() throws Exception {
    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AssignableTypeFilter(RotatableSecrets.class));

    Set<Class<?>> implementations = scanner.findCandidateComponents("com.sonatype.insight.brain.dataaccess")
        .stream()
        .map(beanDefinition -> {
          try {
            return Class.forName(beanDefinition.getBeanClassName());
          }
          catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
          }
        })
        .filter(type -> !type.isInterface())
        .collect(Collectors.toSet());

    assertThat(implementations).isNotEmpty();
    for (Class<?> implementation : implementations) {
      assertThat(implementation)
          .as("%s should extend AbstractOperationalSqlDAO", implementation.getName())
          .isAssignableTo(AbstractOperationalSqlDAO.class);
    }
  }
}
