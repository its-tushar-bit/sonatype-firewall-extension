/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.render;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import com.sonatype.insight.brain.git.render.model.ComponentFeedbackContext;
import com.sonatype.insight.brain.utils.TemplateUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Template;
import org.slf4j.Logger;

import static com.sonatype.insight.brain.utils.TemplateUtils.createFreemarkerConfig;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

public class ComponentFeedbackMDRenderer
{
  private static final Logger log = getLogger(ComponentFeedbackMDRenderer.class);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static Template COMPONENT_FEEDBACK_EMBEDDED_HTML_TEMPLATE;

  static {
    try {
      COMPONENT_FEEDBACK_EMBEDDED_HTML_TEMPLATE =
          createFreemarkerConfig().getTemplate("pullrequest-component-feedback.ftl");
    }
    catch (final IOException e) {
      log.error("Error loading component feedback template: {}", e.getMessage(), e);
    }
  }

  public static Optional<String> render(final ComponentFeedbackContext context) {
    String contents = null;
    try {
      contents = TemplateUtils.render(COMPONENT_FEEDBACK_EMBEDDED_HTML_TEMPLATE, getModelMap(context));
    }
    catch (final IOException e) {
      log.debug(format("Cannot render template '%s'", COMPONENT_FEEDBACK_EMBEDDED_HTML_TEMPLATE.getName()), e);
    }
    return ofNullable(contents);
  }

  private static Map<String, Object> getModelMap(final ComponentFeedbackContext context) {
    return OBJECT_MAPPER.convertValue(context, new TypeReference<Map<String, Object>>()
    {
    });
  }
}
