/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module
import groovy.json.JsonSlurper

class ImportPolicyModule
    extends Module
{
  public static samplePolicyFile = new File(getClass().getResource("/ImportPolicyTest/Sonatype-Sample-Policy-1.6.json").toURI())
  public static sampleOrgPolicyFile = new File(getClass().getResource("/ImportPolicyTest/Sonatype-Org-Sample-Policy-1.6.json").toURI())

  static content = {

    importIcon { $('#policy-import-button') }
    importDialog { $('#import-policy-dialog') }
    fileInput { $('#policyFile') }

    buttons { module ButtonsModule, $('#import-policy-dialog') }
    importButton { buttons.button('Import') }
    cancelButton { buttons.cancel }

    policyList(required: false) { $('.policy-top') }
    alertError(required: false) { $('#import-policy-dialog .alert-error') }
  }

  def importPolicy(File policyFile = samplePolicyFile) {
    Map policy = parsePolicyFile(policyFile)
    importIcon.click()
    waitFor { fileInput.displayed }
    fileInput << policyFile.getAbsolutePath()
    waitFor { !importButton.disabled }
    importButton.click()
    waitFor { policyList.size() == policy.policies.size() }
  }

  static Map parsePolicyFile(File file) {
    return new JsonSlurper().parse(file.newReader())
  }
}
