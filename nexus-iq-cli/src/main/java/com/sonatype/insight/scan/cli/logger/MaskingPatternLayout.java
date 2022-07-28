/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * A logger custom layout that masks any sensitive data that may be present on any log message.
 * More information about layouts here: https://logback.qos.ch/manual/layouts.html
 *
 * This class uses a set of regex to find the sensitive data and then replace it with '*' char.
 * It is important to note that in order for the regex to mask sensitive data, it should have a
 * <strong>group</strong>, the <strong>group</strong> is the one that will be masked.
 *
 * Also ensure you use a valid regex expression as masked pattern. If you use an invalid
 * expression, CLI will compile, but will fail at runtime.
 *
 * Here you have an example of how to configure this layout
 *
 * <pre>
 *  {@code
 *    <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
 *       <layout class="com.sonatype.insight.scan.cli.logger.DataMaskingPatternLayout">
 *         <maskPattern>Authorization: Basic (.*)</maskPattern>
 *         <maskPattern>Password: (.*)</maskPattern>
 *         <pattern>[%level] %m%n</pattern>
 *       </layout>
 *     </encoder>
 *  }
 * </pre>
 */
public class MaskingPatternLayout
    extends PatternLayout
{
  private static final String MASKING_STRING = "************";

  private Pattern finalPattern;

  // The advice is to use a plain string as index, and then use a group to identify the sensitive data on the regex
  private List<String> maskPatterns = new ArrayList<>();

  public void addMaskPattern(String maskPattern) {
    maskPatterns.add(maskPattern);
    finalPattern = Pattern.compile(maskPatterns.stream()
        .collect(Collectors.joining("|")));
  }

  @Override
  public String doLayout(ILoggingEvent event) {
    return maskMessage(super.doLayout(event));
  }

  /**
   * Checks if the message match with any of the given patterns and replace the sensitive data with
   * the '*' char.
   *
   * @param message, message to log
   * @return masked message
   */
  private String maskMessage(String message) {
    Matcher matcher = finalPattern.matcher(message);

    while (matcher.find()) {
      for (int i = 1; i <= matcher.groupCount(); i++) {
        String groupToMask = matcher.group(i);
        if (groupToMask != null) {
          message = message.replace(groupToMask, MASKING_STRING);
        }
      }
    }

    return message;
  }
}
