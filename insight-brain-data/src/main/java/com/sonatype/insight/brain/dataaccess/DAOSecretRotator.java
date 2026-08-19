/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.security.RotatableSecret;
import com.sonatype.insight.model.HasStringId;

import org.apache.commons.lang3.tuple.Pair;
import org.jooq.Table;

/**
 * The DAOs that extend this class should implement the @RotatableSecretsDAO interface to expose the
 * rotateEncryptedSecrets method
 */
@Named
@Singleton
public class DAOSecretRotator
{
  private static final int BATCH_SIZE = 1000;

  public DAOSecretRotator() {
  }

  /**
   * Rotates the DAO encrypted secrets in the database using the provided secretRotator function.
   * The @RotatableSecrets interface should be implemented by the DAO class to ensure this class is included in the
   * RotateEncryptionKeyTask.
   * The DAOs parametrized entity must have a field annotated with @RotatableSecret.
   *
   * @param secretRotator a function that takes an encrypted secret and returns the rotated secret
   * @throws SQLException if an SQL error occurs during the rotation process
   */
  public void rotateEncryptedSecrets(
      AbstractOperationalSqlDAO<?> operationalDataStoreDAO,
      Function<String, String> secretRotator) throws SQLException
  {
    Class<?> entityClass = operationalDataStoreDAO.getEntityClass();
    Table<?> jooqTable = operationalDataStoreDAO.getJooqTable();
    String tableName = jooqTable.getName();
    String tableIdField = getIdColumnName(jooqTable);
    Pair<String, String> rotatableSecretFieldAndColumnName = getRotatableSecretFieldAndColumnName(entityClass);
    List<?> entities =
        getEntitiesWithNonNullSecret(operationalDataStoreDAO, rotatableSecretFieldAndColumnName.getLeft());

    try (Connection connection = operationalDataStoreDAO.getDataStore().getDataSource().getConnection()) {
      try (PreparedStatement statement = createPreparedStatement(operationalDataStoreDAO, connection, tableName,
          rotatableSecretFieldAndColumnName.getRight(), tableIdField))
      {

        connection.setAutoCommit(false);
        processEntities(secretRotator, entities, statement, rotatableSecretFieldAndColumnName.getLeft());
        connection.commit();
      }
      catch (SQLException | IllegalAccessException | NoSuchFieldException e) {
        connection.rollback();
        throw new RuntimeException("Error rotating encrypted secrets", e);
      }
      finally {
        connection.setAutoCommit(true);
      }
    }
  }

  private PreparedStatement createPreparedStatement(
      AbstractOperationalSqlDAO<?> operationalDataStoreDAO,
      Connection connection,
      String tableName,
      String rotatableSecretColumnName,
      String tableIdField) throws SQLException
  {
    String sql =
        "UPDATE _SCHEMA_." + tableName + " SET " + rotatableSecretColumnName + "=? WHERE " + tableIdField + "=?";
    return connection.prepareStatement(operationalDataStoreDAO.injectSchemaName(sql));
  }

  private void processEntities(
      Function<String, String> secretRotator,
      List<?> entities,
      PreparedStatement statement,
      String rotatableSecretFieldName) throws SQLException, IllegalAccessException, NoSuchFieldException
  {
    int count = 0;

    for (Object entity : entities) {
      Field field = entity.getClass().getDeclaredField(rotatableSecretFieldName);
      field.setAccessible(true);

      String secret = (field.getType() == char[].class)
          ? new String((char[]) field.get(entity))
          : (String) field.get(entity);

      statement.setString(1, secretRotator.apply(secret));
      statement.setString(2, ((HasStringId) entity).getId());
      statement.addBatch();

      if (++count % BATCH_SIZE == 0) {
        statement.executeBatch();
      }
    }

    statement.executeBatch();
  }

  /**
   * Gets the ID column name from the jOOQ table's primary key.
   */
  private String getIdColumnName(Table<?> table) {
    var primaryKey = table.getPrimaryKey();
    if (primaryKey == null) {
      throw new IllegalStateException("Table " + table.getName() + " has no primary key defined");
    }
    var fields = primaryKey.getFields();
    if (fields.isEmpty()) {
      throw new IllegalStateException("Table " + table.getName() + " has no primary key fields");
    }
    return fields.get(0).getName();
  }

  /**
   * Gets the field name and column name for the field annotated with @RotatableSecret.
   * The column name is derived from the field name using snake_case convention.
   */
  private Pair<String, String> getRotatableSecretFieldAndColumnName(Class<?> entityClass) {
    for (Field field : entityClass.getDeclaredFields()) {
      if (field.isAnnotationPresent(RotatableSecret.class)) {
        String fieldName = field.getName();
        String columnName = toSnakeCase(fieldName);
        return Pair.of(fieldName, columnName);
      }
    }
    throw new IllegalStateException("No @RotatableSecret annotation found on " + entityClass.getName());
  }

  /**
   * Converts a camelCase field name to snake_case column name.
   */
  private String toSnakeCase(String camelCase) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < camelCase.length(); i++) {
      char c = camelCase.charAt(i);
      if (Character.isUpperCase(c)) {
        if (i > 0) {
          result.append('_');
        }
        result.append(Character.toLowerCase(c));
      }
      else {
        result.append(c);
      }
    }
    return result.toString();
  }

  private List<?> getEntitiesWithNonNullSecret(
      AbstractOperationalSqlDAO<?> operationalDataStoreDAO,
      String rotatableSecretFieldName)
  {
    // Use the DAO's getAll() method instead of jOOQ's fetchInto() because:
    // 1. DAOs have custom mapRecord() methods that properly handle char[] <-> String conversion for password fields
    // 2. jOOQ's fetchInto() doesn't handle char[] fields properly
    // Then filter out entities with null secret values in Java
    return operationalDataStoreDAO.getAll()
        .stream()
        .filter(entity -> {
          try {
            Field field = entity.getClass().getDeclaredField(rotatableSecretFieldName);
            field.setAccessible(true);
            return field.get(entity) != null;
          }
          catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Error accessing secret field: " + rotatableSecretFieldName, e);
          }
        })
        .collect(Collectors.toList());
  }
}
