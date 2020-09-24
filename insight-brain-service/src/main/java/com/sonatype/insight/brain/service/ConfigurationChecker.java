/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.google.common.collect.Sets;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationParsingException;
import io.dropwizard.setup.Bootstrap;

/**
 * @since 1.43
 */
public class ConfigurationChecker
{
  @SuppressWarnings("checkstyle:LineLength")
  static final String SUGGEST_UPDATE_CONFIG_EXCEPTION_MESSAGE =
      "\n=================================================================================================================" +
          "\nYour configuration file contains properties that are only compatible with Nexus IQ Server version 1.42 and lower." +
          "\nUpdate your configuration file to be compatible with this version of Nexus IQ Server." +
          "\nRefer to our configuration update guide at" +
          "\nhttps://help.sonatype.com/display/NXIQ/Updating+your+Nexus+IQ+Server+Configuration." +
          "\n=================================================================================================================";

  private static final Set<String> DROPWIZARD_062_PROPERTIES = Sets
      .newHashSet("http", "logging.console", "logging.file", "logging.syslog");

  public void check(String[] args, Bootstrap<InsightConfig> bootstrap) throws IOException, ConfigurationException {
    if (args.length < 2) {
      return;
    }
    File configurationFile = new File(args[args.length - 1]);
    if (!configurationFile.exists()) {
      return;
    }
    try {
      bootstrap.getConfigurationFactoryFactory()
          .create(InsightConfig.class, bootstrap.getValidatorFactory().getValidator(), bootstrap.getObjectMapper(),
              "dw").build(bootstrap.getConfigurationSourceProvider(), configurationFile.getPath());
    }
    catch (ConfigurationParsingException e) {
      if (e.getCause() instanceof UnrecognizedPropertyException &&
          DROPWIZARD_062_PROPERTIES.contains(pathToString(((UnrecognizedPropertyException) e.getCause()).getPath()))) {
        throw new RuntimeException(SUGGEST_UPDATE_CONFIG_EXCEPTION_MESSAGE, e);
      }
      throw e;
    }
  }

  private String pathToString(Collection<Reference> references) {
    return
        references == null ? null : references.stream().map(Reference::getFieldName).collect(Collectors.joining("."));
  }
}
