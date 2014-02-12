/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/**
 * @since 1.7
 */
@Grab(group = 'com.sonatype.insight.brain', module = 'insight-brain-functional-test', version = '1.8.1-SNAPSHOT', changing = true)
@Grab('org.seleniumhq.selenium:selenium-firefox-driver:2.39.0')
import com.sonatype.insight.brain.testing.functional.*
import com.sonatype.insight.brain.testing.functional.configuration.LdapConfigurationPage
import com.sonatype.insight.brain.testing.functional.configuration.LdapConnectionConfigurationPage
import com.sonatype.insight.brain.testing.functional.configuration.LdapUserAndGroupMappingConfigurationPage
import geb.Browser
import org.openqa.selenium.Keys
import org.openqa.selenium.firefox.FirefoxDriver

/**
 * Connect
 */

CliBuilder cli = new CliBuilder()
cli.usage = 'configure LDAP for either Active Directory or Apache DS internal testing systems'
cli.with {
  s longOpt: 'source', args: 1, 'The CLM server root url to configure, defaults to http://localhost:8070'
  a longOpt: 'activeDirectory', 'Configure Active Directory(default)'
  d longOpt: 'apacheDS', 'Configure Apache DS'
  m longOpt: 'authMethod', args: 1, '''One of SIMPLE, DIGESTMD5 or CRAMMD5. Active directory will be set by default with SIMPLE as
     it does not support testing the connection without credentials'''
  u longOpt: 'ssh',
      'Use ssh connection, assumes JVM already started with appropriate truststore for LDAP server in order to test'
  h longOpt: 'help', 'usage details'
  g longOpt: 'groups', args: 1, 'configure group mappings, either STATIC or DYNAMIC'
}

def options = cli.parse(args)
if (options.h) {
  cli.usage()
  System.exit(0)
}

def configs = [
    activeDirectory: [
        name: 'Active Directory',
        hostname: 'win-clm01',
        protocol: 'LDAP',
        port: '389',
        sshPort: '636',
        searchBase: 'dc=win,dc=blackforest,dc=local',
        simpleUsername: 'cn=testuser1,cn=users,dc=win,dc=blackforest,dc=local',
        digestmd5Username: 'testuser1',
        crammd5Username: 'testuser1',
        password: 'T3stU5er1',
        baseDN: 'cn=users',
        objectClass: 'user',
        userIdAttribute: 'sAMAccountName',
        realNameAttribute: 'displayName',
        emailAttribute: 'mail',
        groupBaseDN: 'cn=users',
        groupObjectClass: 'group',
        groupIDAttribute:'sAMAccountName',
        groupMemberAttribute:'member',
        groupMemberFormat:'${dn}',
        memberOfAttribute: 'memberOf'
    ],

    apacheDS: [
        name: 'Apache DS',
        hostname: 'win-clm01',
        protocol: 'LDAP',
        port: '10389',
        sshPort: '10636',
        searchBase: 'dc=apache,dc=blackforest,dc=local',
        simpleUsername: 'cn=testuser1,ou=users,ou=system',
        digestmd5Username: 'testuser2',
        crammd5Username: 'testuser2',
        password: 'T3stU5er1',
        saslRealm: 'win-clm01',
        baseDN: 'ou=people',
        objectClass: 'person',
        userIdAttribute: 'uid',
        realNameAttribute: 'cn',
        emailAttribute: 'mail',
        groupBaseDN: 'ou=group',
        groupObjectClass: 'groupOfUniqueNames',
        groupIDAttribute:'cn',
        groupMemberAttribute:'uniqueMember',
        groupMemberFormat:'${dn}',
        memberOfAttribute: 'memberOf'
    ]]

def selectedConfig = options.d ? configs.apacheDS : configs.activeDirectory
if (options.u) {
  selectedConfig.protocol = 'LDAPS'
  selectedConfig.port = selectedConfig.sshPort
}
if (options.m) {
  switch (options.m) {
    case 'SIMPLE':
      selectedConfig.authMethod = 'SIMPLE'
      selectedConfig.username = selectedConfig.simpleUsername
      break
    case 'DIGESTMD5':
      if (selectedConfig == configs.apacheDS) {
        throw new IllegalStateException("Apache DS does not support DIGESTMD5 authentication")
      }
      selectedConfig.authMethod = 'DIGESTMD5'
      selectedConfig.username = selectedConfig.digestmd5Username
      break
    case 'CRAMMD5':
      if (selectedConfig == configs.activeDirectory) {
        throw new IllegalStateException("Active Directory does not support CRAMMD5 authentication")
      }
      selectedConfig.authMethod = 'CRAMMD5'
      selectedConfig.username = selectedConfig.crammd5Username
      selectedConfig.password = 'T3stU5er2' //for whatever reason the password is different here
      break
  }
}
Browser browser = new Browser(baseUrl: options.s ?: 'http://localhost:8070/', driver: new FirefoxDriver( ))
browser.getDriver().manage().window().maximize()
Browser.drive(browser) {

  via ReportViolationsPage
  login.loginAsAdmin()
  to LdapConfigurationPage

  // delete any existing ldap configuration
  if (delete?.present && delete.displayed) {
    delete.click()
    deleteConfirm.click()
    to LdapConfigurationPage
    waitFor { at LdapConfigurationPage }
  }

  // add new ldap configuration
  inlineEditorSpan.click()
  inlineEditor.value(selectedConfig.name)
  save.click()

  waitFor { at LdapConnectionConfigurationPage }

  //fill out the required fields and save
  protocol.value(selectedConfig.protocol)
  hostname << selectedConfig.hostname
  (port.value().size()).times {
    port << Keys.BACK_SPACE
  }
  port << selectedConfig.port
  searchBase << selectedConfig.searchBase
  if (selectedConfig.authMethod) {
    authenticationMethod.value(selectedConfig.authMethod)
    systemUsername << selectedConfig.username
    systemPassword << selectedConfig.password
  }

  save.click()
  waitFor { $('div.alert-success span', text: 'Configuration saved.')?.displayed }

  //test the connection
  testConnection.click()
  waitFor { $('div.alert-success span', text: 'Success!')?.displayed }

  //configure user/group
  userAndGroupSettingsTab.click()

  at LdapUserAndGroupMappingConfigurationPage

  userBaseDN << selectedConfig.baseDN
  userObjectClass << selectedConfig.objectClass
  userIDAttribute << selectedConfig.userIdAttribute
  userRealNameAttribute << selectedConfig.realNameAttribute
  userEmailAttribute << selectedConfig.emailAttribute

  if(options.g == 'STATIC'){
    groupMappingType.value('STATIC')
    groupBaseDN << selectedConfig.groupBaseDN
    //groupSubtree { $('#groupSubtree') }
    groupObjectClass << selectedConfig.groupObjectClass
    groupIDAttribute << selectedConfig.groupIDAttribute
    groupMemberAttribute << selectedConfig.groupMemberAttribute
    groupMemberFormat << selectedConfig.groupMemberFormat
  }
  else if(options.g == 'DYNAMIC'){
    groupMappingType.value('DYNAMIC')
    userMemberOfGroupAttribute << selectedConfig.memberOfAttribute
  }

  save.click()

  waitFor { $('div.alert-success span', text: 'Configuration saved.')?.displayed }

  // test the user login
  checkUserLogin.click()
  waitFor { userLoginDialog.displayed }
  userLoginUsername << selectedConfig.digestmd5Username
  userLoginPassword << (selectedConfig.equals(configs.apacheDS) ? 'T3stU5er2' : selectedConfig.password)
  userLoginDialogTest.click()

  waitFor { userLoginDialog.find('div.alert-success span')?.displayed }

  userLoginDialogClose.click()

  // test the user mapping
  checkUserMapping.click()

  waitFor { userMappingDialog.displayed }

  userMappingDialogClose.click()

}.quit()
