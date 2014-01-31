/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import org.codehaus.plexus.util.FileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Stepwise

@Stepwise
class ImportPolicySpec extends BaseSpec
{

  @Rule
  private TemporaryFolder tmpDir = new TemporaryFolder();

  def setupSpec() {
    loginAsAdminVia()
    createOrganization()
    createApplication()
    at ApplicationPage
  }

  def cleanupSpec() {
    cleanAppsAndOrgs()
  }


  def "validate initial button state"() {

    when: 'User selects import policy on application page'
      policyImport.importIcon.click();
      waitFor { policyImport.importDialog.displayed }

    then: 'Import dialog with import button disabled, cancel button enabled is displayed'
      policyImport.importButton.disabled == true
      policyImport.cancelButton.disabled == false
  }

  def "validate local issue with file"() {

    when: 'User selects import file which disappears'
      File tempFile = tmpDir.newFile("ImportPolicyTest.testDisappearingFile")
      FileUtils.copyFile(getValidImportFile(), tempFile);
      policyImport.fileInput << tempFile.getAbsolutePath()
      waitFor { !policyImport.importButton.disabled }
      // We remove the file to simulate an error which the browser might discover
      tempFile.delete()
      policyImport.importButton.click();

    then: 'Error message displayed'
      // Error message at this point is likely browser specific
      waitFor { policyImport.alertError.displayed }
  }

  def "validate user canceling out of dialog"() {

    when: 'User cancels dialog'
      policyImport.fileInput << getValidImportFile().getAbsolutePath()
      waitFor { !policyImport.importButton.disabled }
      policyImport.cancelButton.click()

    then: 'File not imported'
      policyImport.policyList.empty
  }

  def "validate bad file not imported"() {

    given: 'User selects import policy on application page'
      policyImport.importIcon.click()
      waitFor { policyImport.fileInput.displayed }

    when: 'User selects bad file'
      policyImport.fileInput << getBadImportFilePath()
      waitFor { !policyImport.importButton.disabled }
      policyImport.importButton.click()

    then: 'Error message displayed'
      waitFor { policyImport.alertError.text() != null }
      policyImport.alertError.text() == "The file you selected failed to upload correctly, are you certain it is a properly formatted policy import json file?"
  }

  def "validate successful import"() {

    when: 'User selects valid import file'
      policyImport.fileInput << getValidImportFile().getAbsolutePath()
      waitFor { !policyImport.importButton.disabled }
      policyImport.importButton.click()

    then: 'Policy file is imported'
      waitFor { policyImport.policyList.size() == 4 }
      def names = policyImport.policyList.collect{ it.text().trim()}
      names.contains("Security-High")
      names.contains("Security-Medium")
      names.contains("License-Copyleft")
      names.contains("Architecture-Quality")
  }

  File getValidImportFile() {
    return new File(getClass().getResource("/ImportPolicyTest/Sonatype-Sample-Policy-1.6.json").toURI())
  }

  String getBadImportFilePath() {
    return new File(getClass().getResource("/ImportPolicyTest/invalid-policy-import-file.txt").toURI()).getAbsoluteFile().getAbsolutePath()
  }
}
