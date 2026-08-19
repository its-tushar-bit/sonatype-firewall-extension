/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DropwizardConfigCompat
{
  private static final Logger log = LoggerFactory.getLogger(DropwizardConfigCompat.class);

  private DropwizardConfigCompat() {
  }

  static void warnOnDeprecatedFields(Object config, String sectionName) {
    Class<?> clazz = config.getClass();
    while (clazz != null && clazz != Object.class) {
      for (Field field : clazz.getDeclaredFields()) {
        if (!field.isAnnotationPresent(Deprecated.class) || !field.isAnnotationPresent(JsonProperty.class)) {
          continue;
        }
        field.setAccessible(true);
        try {
          if (field.get(config) != null) {
            log.warn("The '{}' property '{}' is no longer supported and will be ignored",
                sectionName, field.getName());
          }
        }
        catch (IllegalAccessException e) {
          log.debug("Unable to check deprecated field {}.{}", sectionName, field.getName(), e);
        }
      }
      clazz = clazz.getSuperclass();
    }
  }
}
