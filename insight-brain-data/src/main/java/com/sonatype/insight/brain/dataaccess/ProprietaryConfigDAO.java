/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProprietaryConfigDAO
{
  // copied from insight-scanner/RegexSelector.java
  public static final List<String> REGEX_BLACK_LIST = Collections.unmodifiableList(Arrays.asList(".*", "^.*$"));

  private static final String CONFIG_FILENAME = "proprietary.json";

  private static final Logger log = LoggerFactory.getLogger(ProprietaryConfigDAO.class);

  private final JsonStore store;

  private String user;

  private String ip;

  private String where;

  public ProprietaryConfigDAO(File workDir) {
    store = JsonUtils.fileStore(workDir);
  }

  public ProprietaryConfigDAO session(final String _user, final String _ip, final String _where) {
    user = _user;
    ip = _ip;
    where = _where;
    return this;
  }

  public ProprietaryConfig get() {
    try {
      final JsonNode config = store.restore(CONFIG_FILENAME);
      return (config != null) ? JsonUtils.asPojo(config, ProprietaryConfig.class) : new ProprietaryConfig();
    }
    catch (IOException e) {
      log.error("Failed to load proprietary component configuration", e);
      throw new DataAccessException(e);
    }
  }

  public void update(ProprietaryConfig config) {
    ValidationResult validationResult = validateRegexes(config.getRegexes());
    if (!validationResult.isValid()) {
      throw new InvalidProprietaryConfigRegexException(validationResult.toMessageString());
    }
    try {
      store.commit(CONFIG_FILENAME, JsonUtils.stamp(user, ip, where, JsonUtils.asTree(config)));
    }
    catch (IOException e) {
      log.error("Failed to save proprietary component configuration", e);
      throw new DataAccessException(e);
    }
  }

  private ValidationResult validateRegexes(final List<String> regexes) {
    ValidationResult validationResult = new ValidationResult();
    for (String regex : regexes) {
      if (REGEX_BLACK_LIST.contains(regex)) {
        validationResult.addError("This regex is specifically disallowed: " + regex);
      }
      else {
        try {
          Pattern.compile(regex);
        }
        catch (NullPointerException | PatternSyntaxException e) {
          validationResult.addError(e.getMessage());
        }
      }
    }
    return validationResult;
  }
}
