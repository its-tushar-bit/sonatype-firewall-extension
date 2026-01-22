/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.InvalidProprietaryConfigRegexException;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.22
 */
@Named
@Singleton
public class ProprietaryConfigDAO
    extends AbstractOperationalSqlDAO<ProprietaryConfig>
{
  // copied from insight-scanner/RegexSelector.java
  static final List<String> REGEX_BLACK_LIST = Collections.unmodifiableList(Arrays.asList(".*", "^.*$"));

  @Inject
  public ProprietaryConfigDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public ProprietaryConfig getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public ProprietaryConfig getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT entity FROM ProprietaryConfig entity" + //
        " WHERE entity.ownerId = ?1";
    return get(tx, sQuery, ownerId);
  }

  @Override
  public void insert(TransactionContext tx, ProprietaryConfig entity) {
    validateRegexes(entity.getRegexes());

    if (getByOwnerId(tx, entity.getOwnerId()) != null) {
      throw new BadRequestException("A proprietary config already exists for owner id " + entity.getOwnerId());
    }

    super.insert(tx, entity);
  }

  @Override
  public void update(TransactionContext tx, ProprietaryConfig entity) {
    validateRegexes(entity.getRegexes());

    ProprietaryConfig existingConfigByOwner = getByOwnerId(tx, entity.getOwnerId());
    if (existingConfigByOwner != null && !existingConfigByOwner.getId().equals(entity.getId())) {
      throw new BadRequestException("A proprietary config already exists for owner id " + entity.getOwnerId());
    }

    super.update(tx, entity);
  }

  private void validateRegexes(List<String> regexes) {
    if (regexes == null || regexes.isEmpty()) {
      return;
    }

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
    if (!validationResult.isValid()) {
      throw new InvalidProprietaryConfigRegexException(validationResult.toMessageString());
    }
  }
}
