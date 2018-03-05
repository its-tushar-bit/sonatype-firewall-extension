/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;
import java.util.Collection;

import javax.validation.Validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.logging.AbstractAppenderFactory;
import io.dropwizard.logging.AppenderFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import io.dropwizard.request.logging.RequestLogFactory;
import io.dropwizard.server.AbstractServerFactory;

public class InsightConfigurationFactory
    extends YamlConfigurationFactory<InsightConfig>
{
  static final String DEFAULT_REQUEST_LOG_FORMAT =
      "%clientHost %l %user [%date] \"%requestURL\" %statusCode %bytesSent %elapsedTime \"%header{User-Agent}\"";

  public InsightConfigurationFactory(final Class<InsightConfig> klass,
                                     final Validator validator,
                                     final ObjectMapper objectMapper,
                                     final String propertyPrefix)
  {
    super(klass, validator, objectMapper, propertyPrefix);
  }

  @Override
  public InsightConfig build(ConfigurationSourceProvider provider, String path)
      throws IOException, ConfigurationException
  {
    InsightConfig insightConfig = super.build(provider, path);
    setDefaultRequestLogFormat(insightConfig);
    return insightConfig;
  }

  private void setDefaultRequestLogFormat(InsightConfig insightConfig) {
    RequestLogFactory<?> requestLogFactory = ((AbstractServerFactory) insightConfig.getServerFactory())
        .getRequestLogFactory();
    if (requestLogFactory instanceof LogbackAccessRequestLogFactory) {
      setAppenderFactoriesLogFormats(((LogbackAccessRequestLogFactory) requestLogFactory).getAppenders(),
          AbstractAppenderFactory.class, DEFAULT_REQUEST_LOG_FORMAT);
    }
  }

  private void setAppenderFactoriesLogFormats(Collection<? extends AppenderFactory<?>> appenderFactories,
                                              @SuppressWarnings("rawtypes") Class<? extends AbstractAppenderFactory> appenderFactoryType,
                                              String logFormat)
  {
    appenderFactories.stream().filter(appenderFactoryType::isInstance).map(appenderFactoryType::cast)
        .filter(abtractAppenderFactory -> abtractAppenderFactory.getLogFormat() == null)
        .forEach(abtractAppenderFactory -> abtractAppenderFactory.setLogFormat(logFormat));
  }
}
