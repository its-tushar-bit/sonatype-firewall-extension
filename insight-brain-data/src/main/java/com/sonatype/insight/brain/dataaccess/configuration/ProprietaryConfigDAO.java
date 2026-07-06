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

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.InvalidProprietaryConfigRegexException;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.UpdatableRecord;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ProprietaryConfig.PROPRIETARY_CONFIG;

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
  public ProprietaryConfigDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  protected UpdatableRecord<?> fromEntity(final UpdatableRecord<?> record, final ProprietaryConfig entity) {
    super.fromEntity(record, entity);
    record.set(PROPRIETARY_CONFIG.PACKAGES_JSON, toJson(entity.getPackages()));
    record.set(PROPRIETARY_CONFIG.REGEXES_JSON, toJson(entity.getRegexes()));
    return record;
  }

  @Override
  protected ProprietaryConfig toEntity(final Record record) {
    if (record == null) {
      return null;
    }
    ProprietaryConfig entity = super.toEntity(record);
    // Use the JSON setters which will parse the JSON into lists
    String packagesJson = record.get(PROPRIETARY_CONFIG.PACKAGES_JSON);
    if (packagesJson != null) {
      entity.setPackages(parseJsonList(packagesJson));
    }
    String regexesJson = record.get(PROPRIETARY_CONFIG.REGEXES_JSON);
    if (regexesJson != null) {
      entity.setRegexes(parseJsonList(regexesJson));
    }
    return entity;
  }

  public ProprietaryConfig getByOwnerId(final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public ProprietaryConfig getByOwnerId(final TransactionContext tx, final String ownerId) {
    return toEntity(tx.dsl()
        .selectFrom(PROPRIETARY_CONFIG)
        .where(PROPRIETARY_CONFIG.OWNER_ID.eq(ownerId))
        .fetchOne());
  }

  @Override
  public int insert(final TransactionContext tx, final ProprietaryConfig entity) {
    validateRegexes(entity.getRegexes());
    if (getByOwnerId(tx, entity.getOwnerId()) != null) {
      throw new BadRequestException("A proprietary config already exists for owner id " + entity.getOwnerId());
    }
    return super.insert(tx, entity);
  }

  @Override
  public int update(final TransactionContext tx, final ProprietaryConfig entity) {
    validateRegexes(entity.getRegexes());
    ProprietaryConfig existingConfigByOwner = getByOwnerId(tx, entity.getOwnerId());
    if (existingConfigByOwner != null && !existingConfigByOwner.getId().equals(entity.getId())) {
      throw new BadRequestException("A proprietary config already exists for owner id " + entity.getOwnerId());
    }
    return super.update(tx, entity);
  }

  private void validateRegexes(final List<String> regexes) {
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

  private String toJson(final List<String> list) {
    if (list == null || list.isEmpty()) {
      return null;
    }
    return JsonUtils.writeUnformatted(list);
  }

  @SuppressWarnings("unchecked")
  private List<String> parseJsonList(final String json) {
    try {
      return JsonUtils.parse(json, List.class);
    }
    catch (Exception e) {
      return null;
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return PROPRIETARY_CONFIG;
  }

  @Override
  public Class<ProprietaryConfig> getEntityClass() {
    return ProprietaryConfig.class;
  }
}
