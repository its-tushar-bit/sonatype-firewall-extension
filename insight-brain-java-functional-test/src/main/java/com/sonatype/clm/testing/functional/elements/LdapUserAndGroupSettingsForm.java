/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

public class LdapUserAndGroupSettingsForm
    extends BasicElement<LdapUserAndGroupSettingsForm>
    implements ILdapForm
{
  public static final String GROUP_SEARCH_WARNING = "Disabling group search may improve performance, but groups will "
      + "not appear in search results.";

  public LdapUserAndGroupSettingsForm(String... selectors) {
    super(selectors);
  }

  public SelenideElement userBaseDN() {
    return child("#ldap-user-base-dn");
  }

  public NxToggle userSubtree() {
    return new NxToggle(childSelector("#ldap-user-subtree"));
  }

  public SelenideElement userObjectClass() {
    return child("#ldap-user-object-class");
  }

  public SelenideElement userFilter() {
    return child("#ldap-user-filter");
  }

  public SelenideElement userIDAttribute() {
    return child("#ldap-user-id-attribute");
  }

  public SelenideElement userRealNameAttribute() {
    return child("#ldap-user-real-name-attribute");
  }

  public SelenideElement userEmailAttribute() {
    return child("#ldap-user-email-attribute");
  }

  public SelenideElement userPasswordAttribute() {
    return child("#ldap-user-password-attribute");
  }

  @Override
  public List<SelenideElement> requiredFields() {
    return Arrays.asList(userObjectClass(), userIDAttribute(), userRealNameAttribute(), userEmailAttribute());
  }

  public NxFormSelect groupMappingType() {
    return new NxFormSelect(childSelector("#ldap-group-mapping-type"));
  }

  public SelenideElement groupBaseDN() {
    return child("#ldap-group-base-dn");
  }

  public SelenideElement groupSubtree() {
    return child("#ldap-group-subtree");
  }

  public SelenideElement groupObjectClass() {
    return child("#ldap-group-object-class");
  }

  public SelenideElement groupIDAttribute() {
    return child("#ldap-group-id-attribute");
  }

  public SelenideElement groupMemberAttribute() {
    return child("#ldap-group-member-attribute");
  }

  public SelenideElement groupMemberFormat() {
    return child("#ldap-group-member-format");
  }

  public SelenideElement userMemberOfGroupAttribute() {
    return child("#ldap-user-member-of-group-attribute");
  }

  public SelenideElement groupSearchWarning() {
    return child(".iq-ldap-group-search-toggle");
  }

  public TestLoginModal testLoginModal() {
    return new TestLoginModal("#ldap-check-login-modal");
  }

  public CheckUserMappingModal checkUserMappingModal() {
    return new CheckUserMappingModal("#ldap-user-mapping-modal");
  }

  public SelenideElement successAlertBox() {
    return child(".nx-alert--success");
  }

  public SelenideElement checkUserMappingButton() {
    return $("#check-user-mapping");
  }

  public SelenideElement checkUserLoginButton() {
    return $("#check-login");
  }

  @Override
  public SelenideElement cancelButton() {
    return $(".nx-form__cancel-btn");
  }

  @Override
  public SelenideElement saveButton() {
    return $(".nx-form__submit-btn");
  }

  public static class TestLoginModal
      extends BasicElement<TestLoginModal>
  {
    public TestLoginModal(String... selectors) {
      super(selectors);
    }

    public SelenideElement username() {
      return child("#check-login-username");
    }

    public SelenideElement password() {
      return child("#check-login-password");
    }

    public SelenideElement successAlertBox() {
      return child(".nx-alert.nx-alert--success");
    }

    public SelenideElement testLoginButton() {
      return child(".nx-form__submit-btn");
    }

    public SelenideElement cancelButton() {
      return child(".nx-form__cancel-btn");
    }
  }

  public static class CheckUserMappingModal
      extends BasicElement<CheckUserMappingModal>
  {
    public CheckUserMappingModal(String... selectors) {
      super(selectors);
    }

    public ElementsCollection rows() {
      return children("tr");
    }

    public SelenideElement cancelButton() {
      return child(".nx-btn.nx-btn--secondary");
    }

    public CheckUserMappingModal shouldHaveUserEntry(
        int row,
        String username,
        String name,
        String email,
        String groups)
    {
      UserRow userRow = new UserRow("tbody", "tr", SelectorUtils.nthChild(row));

      userRow.username().shouldHave(username.isEmpty() ? empty : text(username));
      userRow.name().shouldHave(name.isEmpty() ? empty : text(name));
      userRow.email().shouldHave(email.isEmpty() ? empty : text(email));
      userRow.groups().shouldHave(groups.isEmpty() ? empty : text(groups));

      return this;
    }

    private static class UserRow
        extends BasicElement<UserRow>
    {
      public UserRow(String... selectors) {
        super(selectors);
      }

      public SelenideElement username() {
        return child("td", SelectorUtils.nthChild(1));
      }

      public SelenideElement name() {
        return child("td", SelectorUtils.nthChild(2));
      }

      public SelenideElement email() {
        return child("td", SelectorUtils.nthChild(3));
      }

      public SelenideElement groups() {
        return child("td", SelectorUtils.nthChild(4));
      }
    }
  }
}
