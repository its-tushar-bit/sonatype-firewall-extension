package com.sonatype.insight.brain.model.security;

public class UserQuery implements Comparable<Object>
{
  private String username;
  private String firstName;
  private String lastName;
  private String email;
  private String realm;

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(final String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(final String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(final String email) {
    this.email = email;
  }

  public String getRealm() {
    return realm;
  }

  public void setRealm(final String realm) {
    this.realm = realm;
  }

  public UserQuery() {
  }

  public UserQuery(final String username, final String firstName, final String lastName,
                      final String email, final String realm)
  {
    this.username = username;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.realm = realm;
  }

  @Override
  public int compareTo(final Object o) {
    UserQuery checkDTO = (UserQuery) o;
    return this.username.compareToIgnoreCase(checkDTO.username);
  }
}
