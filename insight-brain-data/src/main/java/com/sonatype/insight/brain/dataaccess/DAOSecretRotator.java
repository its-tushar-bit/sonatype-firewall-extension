/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.security.RotatableSecret;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.apache.commons.lang3.tuple.Pair;

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
   * The DAOs parametrized entity must have fields annotated with @Table, @RotatableSecret and @Id
   *
   * @param secretRotator a function that takes an encrypted secret and returns the rotated secret
   * @throws SQLException if an SQL error occurs during the rotation process
   */
  public void rotateEncryptedSecrets(
      AbstractOperationalSqlDAO<?> operationalDataStoreDAO,
      Function<String, String> secretRotator) throws SQLException
  {
    Class<?> typeArgument = getTypeArgument(operationalDataStoreDAO);
    String tableName = getTableName(typeArgument);
    String tableIdField = getTableIdField(typeArgument);
    Pair<String, String> rotatableSecretFieldAndColumnName = getRotatableSecretFieldAndColumnName(typeArgument);
    List<?> entities = getBySecretField(operationalDataStoreDAO, rotatableSecretFieldAndColumnName.getLeft());

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
      String rotatableSecretColumnName) throws SQLException, IllegalAccessException, NoSuchFieldException
  {
    int count = 0;

    for (Object entity : entities) {
      Field field = entity.getClass().getDeclaredField(rotatableSecretColumnName);
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

  private <T extends HasStringId> Class<T> getTypeArgument(AbstractOperationalSqlDAO<T> operationalDataStoreDAO) {
    ParameterizedType parameterizedType = operationalDataStoreDAO.getParameterizedSuperClass();
    return (Class<T>) parameterizedType.getActualTypeArguments()[0];
  }

  private String getTableName(Class<?> typeArgument) {
    if (typeArgument.isAnnotationPresent(Table.class)) {
      return typeArgument.getAnnotation(Table.class).name();
    }
    throw new IllegalStateException("No @Table annotation found");
  }

  private Pair<String, String> getRotatableSecretFieldAndColumnName(Class<?> typeArgument) {
    for (Field field : typeArgument.getDeclaredFields()) {
      if (field.isAnnotationPresent(RotatableSecret.class) && field.isAnnotationPresent(Column.class)) {
        return Pair.of(field.getName(), field.getAnnotation(Column.class).name());
      }
    }
    throw new IllegalStateException("No @RotatableSecret annotation found");
  }

  private String getTableIdField(Class<?> typeArgument) {
    for (Field field : typeArgument.getDeclaredFields()) {
      if (field.isAnnotationPresent(Id.class) && field.isAnnotationPresent(Column.class)) {
        return field.getAnnotation(Column.class).name();
      }
    }
    throw new IllegalStateException("No @Id annotation found");
  }

  private List<?> getBySecretField(AbstractOperationalSqlDAO<?> operationalDataStoreDAO, String secretFieldName) {
    try (TransactionContext tx = operationalDataStoreDAO.createTransactionContext()) {
      String sQuery =
          "SELECT entity FROM " + operationalDataStoreDAO.getEntityName() + " entity WHERE entity." + secretFieldName +
              " IS NOT NULL";
      return operationalDataStoreDAO.getList(tx, sQuery);
    }
  }
}
