/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.ldap;

import java.util.LinkedHashSet;
import java.util.Set;

import com.sonatype.insight.brain.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.configuration.ldap.LdapConfiguration;
import com.sonatype.insight.brain.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class LdapConfigurationDAOTest
    extends AbstractDbDAOTest
{
  private LdapConfigurationDAO dao = new LdapConfigurationDAO();

  protected Set<LdapConfiguration> configsToDelete = new LinkedHashSet<LdapConfiguration>();

  @Test
  public void testCRUD() {
    String name = "name";
    LdapProtocol protocol = LdapProtocol.LDAPS;
    String hostname = "hostname";
    int port = 389;
    String searchBase = "searchBase";
    LdapAuthenticationMethod authenticationMethod = LdapAuthenticationMethod.DIGESTMD5;
    String saslRealm = "saslRealm";
    String systemUsername = "systemUsername";
    String systemPassword = "systemPassword";
    int connectionTimeout = 123;
    int retryDelay = 345;

    // insert

    LdapConfiguration config = new LdapConfiguration();
    config.setName(name);
    config.setProtocol(protocol);
    config.setHostname(hostname);
    config.setPort(port);
    config.setSearchBase(searchBase);
    config.setAuthenticationMethod(authenticationMethod);
    config.setSaslRealm(saslRealm);
    config.setSystemUsername(systemUsername);
    config.setSystemPassword(systemPassword);
    config.setConnectionTimeout(connectionTimeout);
    config.setRetryDelay(retryDelay);
    Assert.assertNull(config.getId()); // sanity check
    dao.insert(config);

    // select by id

    LdapConfiguration echo = dao.getById(config.getId());
    Assert.assertNotNull(echo);
    Assert.assertEquals(name, echo.getName());
    Assert.assertEquals(NameHelper.normalize(name), echo.getNameLowercaseNoWhitespace());
    Assert.assertEquals(protocol, echo.getProtocol());
    Assert.assertEquals(hostname, echo.getHostname());
    Assert.assertEquals(port, echo.getPort());
    Assert.assertEquals(searchBase, echo.getSearchBase());
    Assert.assertEquals(authenticationMethod, echo.getAuthenticationMethod());
    Assert.assertEquals(saslRealm, echo.getSaslRealm());
    Assert.assertEquals(systemUsername, echo.getSystemUsername());
    Assert.assertEquals(systemPassword, echo.getSystemPassword());
    Assert.assertEquals(connectionTimeout, echo.getConnectionTimeout());
    Assert.assertEquals(retryDelay, echo.getRetryDelay());

    // select by name

    echo = dao.getByName(name);
    Assert.assertNotNull(echo);

    // update

    String changedPassword = "changed_password";
    config.setSystemPassword(changedPassword);
    dao.update(config);
    echo = dao.getById(config.getId());
    Assert.assertEquals(changedPassword, echo.getSystemPassword());

    // delete
    dao.delete(config);
    Assert.assertNull(dao.getById(config.getId()));
  }

  @Test
  public void testValidateNullName_Insert() {
    LdapConfiguration config = createLdapConfiguration(null /* name */);
    try {
      dao.insert(config);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNullName_Update() {
    LdapConfiguration config = insertLdapConfiguration("testValidateNullName");
    assertEquals("testvalidatenullname", config.getNameLowercaseNoWhitespace());

    config.setName(null);
    assertNull(config.getNameLowercaseNoWhitespace());
    try {
      dao.update(config);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateEmptyName_Insert() {
    try {
      insertLdapConfiguration(" ");
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateEmptyName_Update() {
    LdapConfiguration config = insertLdapConfiguration("testValidateEmptyName");
    assertEquals("testvalidateemptyname", config.getNameLowercaseNoWhitespace());

    config.setName(" ");
    assertEquals("", config.getNameLowercaseNoWhitespace());
    try {
      dao.update(config);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNameInvalidChars_Insert() {
    String[] invalidAlphaNumericNames = { "!", "@", "#", "$", "%", "^", "&", "*", "(", "_", "+" };
    for (String name : invalidAlphaNumericNames) {
      LdapConfiguration config = createLdapConfiguration(name);
      try {
        dao.insert(config);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("Name must be alpha numeric.", expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNameInvalidChars_Update() {
    LdapConfiguration config = insertLdapConfiguration("testValidateNameInvalidChars");
    String[] invalidAlphaNumericNames = { "!", "@", "#", "$", "%", "^", "&", "*", "(", "_", "+" };
    for (String name : invalidAlphaNumericNames) {
      config.setName(name);
      try {
        dao.update(config);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("Name must be alpha numeric.", expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNameSpaces_Insert() {
    String[] invalidSpacingNames = { " leading space", "trailing space ", "double  space",
        "  starts with double space", "ends with double space  " };
    for (String name : invalidSpacingNames) {
      try {
        insertLdapConfiguration(name);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("Name must not have leading or trailing spaces, or have two spaces in a row.",
            expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNameSpaces_Update() {
    LdapConfiguration config = insertLdapConfiguration("testValidateNameSpaces");

    String[] invalidSpacingNames = { " leading space", "trailing space ", "double  space",
        "  starts with double space", "ends with double space  " };
    for (String name : invalidSpacingNames) {
      config.setName(name);
      try {
        dao.update(config);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("Name must not have leading or trailing spaces, or have two spaces in a row.",
            expected.getMessage());
      }
    }
  }

  @Test
  public void testNameIsCaseAndWhitespaceInsensitive() {
    String name = "test string With Case and Whitespace";

    LdapConfiguration config = insertLdapConfiguration(name);

    assertEquals(name, config.getName());
    assertEquals("teststringwithcaseandwhitespace", config.getNameLowercaseNoWhitespace());

    String name1 = "TEST String      With    cASE and      whitespace";
    LdapConfiguration config1 = dao.getByName(name1);
    assertNotNull(config1);
    assertEquals(config.getId(), config1.getId());
  }

  @Test
  public void testDuplicateName_Insert() {
    insertLdapConfiguration("testDuplicateName");

    try {
      insertLdapConfiguration("testDuplicateName");
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("testDuplicateName is already used as a name.", expected.getMessage());
    }
  }

  @Test
  public void testDuplicateName_Update() {
    insertLdapConfiguration("testDuplicateName");
    LdapConfiguration config = insertLdapConfiguration("testDuplicateName1");

    config.setName("Test Duplicate Name");
    try {
      dao.update(config);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Test Duplicate Name is already used as a name.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNameLength_Insert() {
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    try {
      insertLdapConfiguration(name + "a");
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name must be 60 characters or less.", expected.getMessage());
    }

    insertLdapConfiguration(name);
  }

  @Test
  public void testValidateNameLength_Update() {
    LdapConfiguration config = insertLdapConfiguration("test name");

    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    config.setName(name + "a");
    try {
      dao.update(config);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name must be 60 characters or less.", expected.getMessage());
    }

    config.setName(name);
    dao.update(config);
  }

  protected LdapConfiguration createLdapConfiguration(String name) {
    LdapConfiguration config = new LdapConfiguration();
    config.setName(name);
    config.setHostname("localhost");
    config.setPort(389);
    config.setProtocol(LdapProtocol.LDAP);
    config.setAuthenticationMethod(LdapAuthenticationMethod.NONE);
    return config;
  }

  protected LdapConfiguration insertLdapConfiguration(String name) {
    LdapConfiguration config = createLdapConfiguration(name);
    dao.insert(config);
    configsToDelete.add(config);
    return config;
  }

  @After
  @Override
  public void tearDown() {
    for (LdapConfiguration config : configsToDelete) {
      config = dao.getById(config.getId());
      if (config != null) {
        dao.delete(config);
      }
    }
    super.tearDown();
  }
}
