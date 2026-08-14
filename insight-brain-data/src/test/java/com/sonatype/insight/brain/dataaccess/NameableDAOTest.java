/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.brain.model.Nameable;
import com.sonatype.insight.model.HasStringId;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class NameableDAOTest<T extends Nameable & HasStringId>
    extends AbstractDbDAOTest
{
  @Test
  public void testInsert_ValidateNameInvalidChars() {
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      assertThatThrownBy(() -> createNameable(name)).isInstanceOf(InvalidNameException.class)
          .hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0));
    }
  }

  @Test
  public void testInsert_ValidateNameSpaces() {
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      assertThatThrownBy(() -> createNameable(name)).isInstanceOf(InvalidNameException.class)
          .hasMessage("Name must not have leading or trailing spaces, or have two spaces in a row.");
    }
  }

  @Test
  public void testInsert_ValidateEmptyName() {
    assertThatThrownBy(() -> createNameable(" ")).isInstanceOf(InvalidNameException.class)
        .hasMessage("Name is required.");
  }

  @Test
  public void testInsert_ValidateNullName() {
    assertThatExceptionOfType(InvalidNameException.class).isThrownBy(() -> createNameable(null))
        .withMessage("Name is required.");
  }

  @Test
  public void testInsert_ValidateNameValidChars() {
    for (String name : NameHelperTest.VALID_NAMES) {
      createNameable(name);
    }
  }

  @Test
  public void testInsert_DuplicateName() {
    createNameable("testName");
    assertThatThrownBy(() -> createNameable("testName")).isInstanceOf(InvalidNameException.class)
        .hasMessageContaining("testName is already used as a name");
  }

  @Test
  public void testInsert_ValidateNameLength() {
    String name = StringUtils.repeat("a", getMaxNameLength() + 1);
    assertThatThrownBy(() -> createNameable(name)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Name must be " + getMaxNameLength() + " characters or less.");
  }

  @Test
  public void testUpdate_ValidateNullName() {
    T nameable = createNameable("testValidateNullName");
    assertThat(nameable.getNameLowercaseNoWhitespace()).isEqualTo("testvalidatenullname");

    nameable.setName(null);
    assertThat(nameable.getNameLowercaseNoWhitespace()).isNull();
    assertThatExceptionOfType(InvalidNameException.class).isThrownBy(() -> getDao().update(nameable))
        .withMessage("Name is required.");
  }

  @Test
  public void testNameIsCaseAndWhitespaceInsensitive() {
    String name = "test string With Case and Whitespace";

    T nameable = createNameable(name);

    assertThat(nameable.getName()).isEqualTo(name);
    assertThat(nameable.getNameLowercaseNoWhitespace()).isEqualTo("teststringwithcaseandwhitespace");

    String name1 = "TEST String      With    cASE and      whitespace";
    T stored = getEntityByName(name1);
    assertThat(stored).isNotNull();
    assertThat(stored.getId()).isEqualTo(nameable.getId());
  }

  @Test
  public void testUpdate_ValidateEmptyName() {
    T nameable = createNameable("a");
    nameable.setName(" ");
    assertThat(nameable.getNameLowercaseNoWhitespace()).isEqualTo("");
    assertThatThrownBy(() -> getDao().update(nameable)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Name is required.");
  }

  @Test
  public void testUpdate_ValidateNameInvalidChars() {
    T nameable = createNameable("a");
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      nameable.setName(name);
      assertThatThrownBy(() -> getDao().update(nameable)).isInstanceOf(InvalidNameException.class)
          .hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0));
    }
  }

  @Test
  public void testUpdate_ValidateNameValidChars() {
    T nameable = createNameable("a");
    for (String name : NameHelperTest.VALID_NAMES) {
      nameable.setName(name);
      getDao().update(nameable);
    }
  }

  protected abstract T createNameable(String a);

  @Test
  public void testUpdate_ValidateNameSpaces() {
    T nameable = createNameable("a");

    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      nameable.setName(name);
      assertThatThrownBy(() -> getDao().update(nameable)).isInstanceOf(InvalidNameException.class)
          .hasMessage("Name must not have leading or trailing spaces, or have two spaces in a row.");
    }
  }

  @Test
  public void testUpdate_DuplicateName() {
    createNameable("testDuplicateName");
    T nameable1 = createNameable("testDuplicateName1");

    nameable1.setName("Test Duplicate Name");
    assertThatThrownBy(() -> getDao().update(nameable1)).isInstanceOf(InvalidNameException.class)
        .hasMessageContaining("Test Duplicate Name is already used as a name");
  }

  @Test
  public void testUpdate_ValidateNameLength() {
    T nameable = createNameable("a");

    String name = StringUtils.repeat("a", getMaxNameLength());
    nameable.setName(name + "a");
    assertThatThrownBy(() -> getDao().update(nameable)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Name must be " + getMaxNameLength() + " characters or less.");

    nameable.setName(name);
    getDao().update(nameable);
  }

  protected abstract int getMaxNameLength();

  protected abstract AbstractOperationalSqlDAO<T> getDao();

  protected abstract T getEntityByName(String name1);
}
