/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import jakarta.inject.Named;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

/**
 * Registers Jakarta {@link Named} beans early enough for single-tenant Spring contexts to satisfy startup wiring.
 */
@Configuration(proxyBeanMethods = false)
public class NamedBeanRegistrationConfiguration
    implements BeanDefinitionRegistryPostProcessor
{

  private static final String NAMED_BEAN_BASE_PACKAGE = "com.sonatype.insight.brain";

  private static final Pattern TEST_SUPPORT_CLASS = Pattern.compile(".*\\.Test[^.$]*(?:[.$].*)?$");

  /** Packages under the base package that never contain {@link Named} beans. */
  private static final Pattern[] NON_BEAN_PACKAGES = {
    Pattern.compile("com\\.sonatype\\.insight\\.brain\\.jooq\\..*"),
    Pattern.compile("com\\.sonatype\\.insight\\.brain\\.db\\.generated\\..*"),
    Pattern.compile("com\\.sonatype\\.insight\\.brain\\.api\\.admin\\.authorization\\..*"),
  };

  @Override
  public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) throws BeansException {
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry, false)
    {
      @Override
      protected boolean checkCandidate(final String beanName, final BeanDefinition beanDefinition) {
        if (!registry.containsBeanDefinition(beanName)) {
          return super.checkCandidate(beanName, beanDefinition);
        }

        BeanDefinition existingBeanDefinition = registry.getBeanDefinition(beanName);
        if (isEquivalentRegistration(existingBeanDefinition, beanDefinition)) {
          return false;
        }

        return super.checkCandidate(beanName, beanDefinition);
      }

    };
    scanner.addIncludeFilter(new AnnotationTypeFilter(Named.class));
    scanner.addExcludeFilter(new RegexPatternTypeFilter(TEST_SUPPORT_CLASS));
    for (Pattern nonBeanPackage : NON_BEAN_PACKAGES) {
      scanner.addExcludeFilter(new RegexPatternTypeFilter(nonBeanPackage));
    }
    scanner.scan(NAMED_BEAN_BASE_PACKAGE);
  }

  private boolean isEquivalentRegistration(
      final BeanDefinition existingBeanDefinition,
      final BeanDefinition scannedBeanDefinition)
  {
    String scannedBeanClassName = scannedBeanDefinition.getBeanClassName();
    if (Objects.equals(existingBeanDefinition.getBeanClassName(), scannedBeanClassName)) {
      return true;
    }

    if (existingBeanDefinition instanceof AnnotatedBeanDefinition annotatedBeanDefinition) {
      MethodMetadata factoryMethodMetadata = annotatedBeanDefinition.getFactoryMethodMetadata();
      if (factoryMethodMetadata != null) {
        return Objects.equals(factoryMethodMetadata.getReturnTypeName(), scannedBeanClassName);
      }
    }

    return false;
  }

  @Override
  public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException {
    // no-op
  }
}
