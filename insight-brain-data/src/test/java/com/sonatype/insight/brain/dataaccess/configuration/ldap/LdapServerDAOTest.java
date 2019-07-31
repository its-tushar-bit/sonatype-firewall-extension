/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.ldap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class LdapServerDAOTest
    extends AbstractDbDAOTest
{
  private LdapServerDAO dao = new LdapServerDAO();

  @Test
  public void testCRUD() {
    String name = "name";

    // insert

    LdapServer server = tempEntity.newLdapServer(name);

    // select by id

    LdapServer echo = dao.getById(server.getId());
    assertThat(echo).isNotNull();
    assertThat(echo.getName()).isEqualTo(name);
    assertThat(echo.getNameLowercaseNoWhitespace()).isEqualTo(NameHelper.normalize(name));

    // select by name

    echo = dao.getByName(name);
    assertThat(echo).isNotNull();

    // update

    String changedName = "changedName";
    server.setName(changedName);
    dao.update(server);
    echo = dao.getById(server.getId());
    assertThat(echo.getName()).isEqualTo(changedName);

    // delete
    dao.delete(server);
    assertThat(dao.getById(server.getId())).isNull();
  }

  @Test
  public void testValidateNullName_Insert() {
    LdapServer ldapServer = new LdapServer(null /* name */);
    assertThatExceptionOfType(InvalidNameException.class).isThrownBy(() -> {
      dao.insert(ldapServer);
    }).withMessage("Name is required.");
  }

  @Test
  public void testValidateNullName_Update() {
    LdapServer ldapServer = tempEntity.newLdapServer("testValidateNullName");
    assertThat(ldapServer.getNameLowercaseNoWhitespace()).isEqualTo("testvalidatenullname");

    ldapServer.setName(null);
    assertThat(ldapServer.getNameLowercaseNoWhitespace()).isNull();
    assertThatExceptionOfType(InvalidNameException.class).isThrownBy(() -> {
      dao.update(ldapServer);
    }).withMessage("Name is required.");
  }

  @Test
  public void testValidateEmptyName_Insert() {
    assertThatExceptionOfType(InvalidNameException.class).isThrownBy(() -> {
      tempEntity.newLdapServer(" ");
    }).withMessage("Name is required.");
  }

  @Test
  public void testValidateEmptyName_Update() {
    LdapServer ldapServer = tempEntity.newLdapServer("testValidateEmptyName");
    assertThat(ldapServer.getNameLowercaseNoWhitespace()).isEqualTo("testvalidateemptyname");

    ldapServer.setName(" ");
    assertThat(ldapServer.getNameLowercaseNoWhitespace()).isEqualTo("");
    assertThatExceptionOfType(InvalidNameException.class).isThrownBy(() -> {
      dao.update(ldapServer);
    }).withMessage("Name is required.");
  }

  @Test
  public void testValidateNameInvalidChars_Insert() {
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      LdapServer ldapServer = new LdapServer(name);
      assertThatExceptionOfType(InvalidNameException.class).isThrownBy(() -> {
        dao.insert(ldapServer);
      }).withMessage(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0));
    }
  }

  @Test
  public void testValidateNameInvalidChars_Update() {
    LdapServer ldapServer = tempEntity.newLdapServer("testValidateNameInvalidChars");
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      ldapServer.setName(name);
      assertThatExceptionOfType(InvalidNameException.class).isThrownBy(() -> {
        dao.update(ldapServer);
      }).withMessage(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0));
    }
  }

  @Test
  public void testValidateNameValidChars_Insert() {
    for (String name : NameHelperTest.VALID_NAMES) {
      tempEntity.newLdapServer(name);
    }
  }

  @Test
  public void testValidateNameValidChars_Update() {
    LdapServer ldapServer = tempEntity.newLdapServer("a");
    for (String name : NameHelperTest.VALID_NAMES) {
      ldapServer.setName(name);
      dao.update(ldapServer);
    }
  }

  @Test
  public void testValidateNameSpaces_Insert() {
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      assertThatExceptionOfType(InvalidNameException.class).isThrownBy(() -> {
        tempEntity.newLdapServer(name);
      }).withMessage("Name must not have leading or trailing spaces, or have two spaces in a row.");
    }
  }

  @Test
  public void testValidateNameSpaces_Update() {
    LdapServer ldapServer = tempEntity.newLdapServer("testValidateNameSpaces");
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      ldapServer.setName(name);
      assertThatExceptionOfType(InvalidNameException.class).isThrownBy(() -> {
        dao.update(ldapServer);
      }).withMessage("Name must not have leading or trailing spaces, or have two spaces in a row.");
    }
  }

  @Test
  public void testNameIsCaseAndWhitespaceInsensitive() {
    String name = "test string With Case and Whitespace";

    LdapServer ldapServer = tempEntity.newLdapServer(name);

    assertThat(ldapServer.getName()).isEqualTo(name);
    assertThat(ldapServer.getNameLowercaseNoWhitespace()).isEqualTo("teststringwithcaseandwhitespace");

    String name1 = "TEST String      With    cASE and      whitespace";
    LdapServer ldapServer1 = dao.getByName(name1);
    assertThat(ldapServer1).isNotNull();
    assertThat(ldapServer1.getId()).isEqualTo(ldapServer.getId());
  }

  @Test
  public void testDuplicateName_Insert() {
    tempEntity.newLdapServer("testDuplicateName");

    assertThatExceptionOfType(InvalidNameException.class).isThrownBy(() -> {
      tempEntity.newLdapServer("testDuplicateName");
    }).withMessage("testDuplicateName is already used as a name.");
  }

  @Test
  public void testDuplicateName_Update() {
    tempEntity.newLdapServer("testDuplicateName");
    LdapServer ldapServer = tempEntity.newLdapServer("testDuplicateName1");

    ldapServer.setName("Test Duplicate Name");
    assertThatExceptionOfType(InvalidNameException.class).isThrownBy(() -> {
      dao.update(ldapServer);
    }).withMessage("Test Duplicate Name is already used as a name.");
  }

  @Test
  public void testValidateNameLength_Insert() {
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    assertThatExceptionOfType(InvalidNameException.class).isThrownBy(() -> {
      tempEntity.newLdapServer(name + "a");
    }).withMessage("Name must be 60 characters or less.");

    tempEntity.newLdapServer(name);
  }

  @Test
  public void testValidateNameLength_Update() {
    LdapServer ldapServer = tempEntity.newLdapServer("test name");

    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    ldapServer.setName(name + "a");
    assertThatExceptionOfType(InvalidNameException.class).isThrownBy(() -> {
      dao.update(ldapServer);
    }).withMessage("Name must be 60 characters or less.");

    ldapServer.setName(name);
    dao.update(ldapServer);
  }

  @Test
  public void testInsert_AutoIncrementsPriority() {
    LdapServer ldapServer1 = tempEntity.newLdapServer("test1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("test2");

    assertThat(ldapServer2.getPriority()).isGreaterThan(ldapServer1.getPriority());
  }

  @Test
  public void testUpdatePriority() {
    LdapServer ldapServer1 = tempEntity.newLdapServer("test1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("test2");

    List<String> serverPriorityList = new ArrayList<>();
    serverPriorityList.add(ldapServer2.getId());
    serverPriorityList.add(ldapServer1.getId());

    dao.updatePriority(serverPriorityList);

    List<LdapServer> servers = dao.getAll();
    assertThat(servers.get(0).getName()).isEqualTo("test2");
    assertThat(servers.get(0).getPriority()).isEqualTo(1);
    assertThat(servers.get(1).getName()).isEqualTo("test1");
    assertThat(servers.get(1).getPriority()).isEqualTo(2);
  }

  @Test
  public void testUpdatePriority_IncorrectNumberOfServers() {
    tempEntity.newLdapServer("test1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("test2");

    List<String> mismatchServerList = Collections.singletonList(ldapServer2.getId());

    assertThatExceptionOfType(DataAccessException.class).isThrownBy(() -> {
      dao.updatePriority(mismatchServerList);
    }).withMessageContaining("Unable to update priority of Ldap servers due to server list mismatch.");
  }

  @Test
  public void testUpdatePriority_DuplicateServers() {
    LdapServer ldapServer1 = tempEntity.newLdapServer("test1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("test2");

    List<String> serverPriorityList = new ArrayList<>();
    serverPriorityList.add(ldapServer1.getId());
    serverPriorityList.add(ldapServer1.getId());
    serverPriorityList.add(ldapServer2.getId());

    assertThatExceptionOfType(DataAccessException.class).isThrownBy(() -> {
      dao.updatePriority(serverPriorityList);
    }).withMessageContaining("Unable to update priority of Ldap servers due to duplicate server IDs.");
  }

  @Test
  public void testUpdatePriority_IncorrectServerId() {
    tempEntity.newLdapServer("test1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("test2");

    List<String> serverPriorityList = new ArrayList<>();
    serverPriorityList.add(ldapServer2.getId());
    serverPriorityList.add("incorrectServerId");

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      dao.updatePriority(serverPriorityList);
    }).withMessageContaining("Cannot find LdapServer with ID incorrectServerId");
  }
}
