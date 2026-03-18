/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HashComponentIdentifierDAOTest
    extends AbstractDbDAOTest
{
  private HashComponentIdentifierDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createHashComponentIdentifierDAO();
  }

  @Test
  public void testCRUD() {
    String hash = "123456789012345678901";
    assertThat(hash.length()).isGreaterThan(20);
    String truncatedHash = hash.substring(0, 20);
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "e1", "c1");
    Date createTime = new Date();

    // Create
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(hash, componentIdentifier);
    hashComponentIdentifier.setCreateTime(createTime);
    assertThat(hashComponentIdentifier.getId()).isNull();
    dao.insert(hashComponentIdentifier);
    assertThat(hashComponentIdentifier.getId()).isNotNull();

    // Read
    hashComponentIdentifier = dao.getById(hashComponentIdentifier.getId());
    assertThat(hashComponentIdentifier).isNotNull();
    assertHashComponentIdentifier(truncatedHash, componentIdentifier, createTime, hashComponentIdentifier);
    assertThat(hashComponentIdentifier.getComment()).isNullOrEmpty();

    // Update
    String comment = "Comment for update";
    hashComponentIdentifier.setComment(comment);
    dao.update(hashComponentIdentifier);
    hashComponentIdentifier = dao.getById(hashComponentIdentifier.getId());
    assertThat(hashComponentIdentifier).isNotNull();
    assertHashComponentIdentifier(truncatedHash, componentIdentifier, createTime, hashComponentIdentifier);
    assertThat(hashComponentIdentifier.getComment()).isEqualTo(comment);

    // Delete
    dao.delete(hashComponentIdentifier);

    hashComponentIdentifier = dao.getById(hashComponentIdentifier.getId());
    assertThat(hashComponentIdentifier).isNull();
  }

  @Test
  public void testGetByHashNotNull() {
    String hash = "11111111111111111111";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    HashComponentIdentifier expectedHashComponentIdentifier = tempEntity.newClaimedComponent(hash, componentIdentifier);
    Date createTime = expectedHashComponentIdentifier.getCreateTime();

    HashComponentIdentifier actualHashComponentIdentifier = dao.getByHashNotNull(hash);
    assertThat(actualHashComponentIdentifier).isNotNull();
    assertHashComponentIdentifier(hash, componentIdentifier, createTime, actualHashComponentIdentifier);
  }

  @Test
  public void testGetByHashNotNull_DoesNotExist() {
    String hash = "11111111111111111111";
    assertThatThrownBy(() -> dao.getByHashNotNull(hash)).isInstanceOf(NotFoundException.class)
        .hasMessage(HashComponentIdentifierDAO.NOT_FOUND_MESSAGE + hash + ".");
  }

  @Test
  public void testGetByComponentIdentifier() {
    String hash = "11111111111111111111";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    HashComponentIdentifier expectedHashComponentIdentifier = tempEntity.newClaimedComponent(hash, componentIdentifier);
    Date createTime = expectedHashComponentIdentifier.getCreateTime();

    HashComponentIdentifier actualHashComponentIdentifier = dao.getByComponentIdentifier(componentIdentifier);
    assertThat(actualHashComponentIdentifier).isNotNull();
    assertHashComponentIdentifier(hash, componentIdentifier, createTime, actualHashComponentIdentifier);
  }

  private void assertHashComponentIdentifier(
      String hash,
      ComponentIdentifier componentIdentifier,
      Date createTime,
      HashComponentIdentifier hashComponentIdentifier)
  {
    assertThat(hashComponentIdentifier.getHash()).isEqualTo(hash);
    assertThat(hashComponentIdentifier.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(hashComponentIdentifier.getCreateTime()).isEqualTo(createTime);
  }

  @Test
  public void testAddDuplicateByHash() {
    String hash = "ab1234ab1234ab";
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");

    tempEntity.newClaimedComponent(hash, componentIdentifier1);

    HashComponentIdentifier hashComponentIdentifier2 = new HashComponentIdentifier(hash, componentIdentifier2);
    assertThatThrownBy(() -> dao.insert(hashComponentIdentifier2)).isInstanceOf(BadRequestException.class)
        .hasMessage("This component is already mapped to 'g1 : a1 : v1'.");
  }

  @Test
  public void testAddDuplicateByComponentIdentifier() {
    String hash = "ab1234ab1234ab";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");

    tempEntity.newClaimedComponent(hash, componentIdentifier);

    HashComponentIdentifier hashComponentIdentifier2 = new HashComponentIdentifier(hash + "1", componentIdentifier);
    assertThatThrownBy(() -> dao.insert(hashComponentIdentifier2)).isInstanceOf(BadRequestException.class)
        .hasMessage("Another component is already mapped to 'g1 : a1 : v1'.");
  }

  @Test
  public void testGetByHashes() {
    String hash = "11111111111111111111";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    HashComponentIdentifier expectedHashComponentIdentifier = tempEntity.newClaimedComponent(hash, componentIdentifier);
    Date createTime = expectedHashComponentIdentifier.getCreateTime();

    List<HashComponentIdentifier> hashIdentifiers = dao.getByHashes(ImmutableList.of(hash));
    assertThat(hashIdentifiers).isNotEmpty();

    for (HashComponentIdentifier hashIdentifier : hashIdentifiers) {
      assertHashComponentIdentifier(hash, componentIdentifier, createTime, hashIdentifier);
    }
  }

  @Test
  public void testGetByHashes_GetsHashComponentIdentifiersInBatches() {
    TestHashComponentIdentifierDAO dao = new TestHashComponentIdentifierDAO(databaseRule.getOperationalDataStore());

    List<HashComponentIdentifier> expectedIdentifiers = new ArrayList<>();
    List<String> hashes = new ArrayList<>();

    for (int i = 0; i < dao.getInOperatorThreshold() + 1; i++) {
      String hash = UUID.randomUUID().toString();
      hashes.add(hash);

      HashComponentIdentifier expectedIdentifier = tempEntity.newClaimedComponent(hash,
          ComponentIdentifier.createMavenCoordinates("g" + i, "a" + i, "v" + i, "c" + i, "e" + i));
      expectedIdentifiers.add(expectedIdentifier);
    }

    List<HashComponentIdentifier> actualIdentifiers = dao.getByHashes(hashes);
    assertThat(actualIdentifiers).hasSize(dao.getInOperatorThreshold() + 1);

    for (HashComponentIdentifier actualIdentifier : actualIdentifiers) {
      HashComponentIdentifier expectedIdentifier =
          expectedIdentifiers.stream().filter(i -> i.getHash().equals(actualIdentifier.getHash())).findFirst().get();

      assertHashComponentIdentifier(expectedIdentifier.getHash(), expectedIdentifier.getComponentIdentifier(),
          expectedIdentifier.getCreateTime(), actualIdentifier);
    }
  }

  /**
   * Extend HashComponentIdentifierDAO so that we can change the partition threshold to make testing easier/quicker
   */
  private static class TestHashComponentIdentifierDAO
      extends HashComponentIdentifierDAO
  {
    TestHashComponentIdentifierDAO(OperationalDataStore operationalDataStore) {
      super(operationalDataStore);
    }

    private static final int PARTITION_THRESHOLD = 2;

    @Override
    public int getInOperatorThreshold() {
      return PARTITION_THRESHOLD;
    }
  }
}
