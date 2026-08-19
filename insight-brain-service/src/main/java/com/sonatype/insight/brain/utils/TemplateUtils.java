/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;

public final class TemplateUtils
{
  private TemplateUtils() {
    // utility class
  }

  public static Configuration createFreemarkerConfig() {
    Configuration cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
    cfg.setClassForTemplateLoading(TemplateUtils.class, "/com/sonatype/insight/brain/policy/templates");
    cfg.setObjectWrapper(new DefaultObjectWrapperBuilder(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS).build());
    cfg.setLocale(java.util.Locale.US); // Prevent use of commas for radix
    cfg.setDefaultEncoding("UTF-8");
    cfg.setLocalizedLookup(false);
    cfg.setNumberFormat("0.######");
    return cfg;
  }

  public static String render(final Template template, final Map<String, Object> model) throws IOException {
    final StringWriter out = new StringWriter(1024 * 64);

    try {
      final Environment processingEnv = template.createProcessingEnvironment(model, out);
      processingEnv.setOutputEncoding("UTF-8");
      processingEnv.process();
    }
    catch (final Exception e) {
      // NOTE: And yes, we want to capture all exceptions, e.g. ArithmeticException
      throw new IOException("Failed to process template " + template.getName() + ": " + e.getMessage(), e);
    }
    return out.toString();
  }
}
