/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

class ApplicationManagementSpec extends BaseSpec
{
  def setup() {
    to ApplicationManagementPage
    login.loginAsAdmin()
  }

  def "display local image"() {
    when: "local file is selected"
      File tempFile = File.createTempFile("ApplicationManagementPageSpec", "fakeimage.jpg")
      tempFile.deleteOnExit()

      newApplicationButton.click()
      interact {
        moveToElement(applicationImage)
      }
      // Selenium 2.36+ apparently considers an element invisible when off-screen so we nuke the style that moves the element off-screen
      applicationImageFileDialog.jquery.attr('style', '')
      applicationImageFileDialog << tempFile.getAbsolutePath()

    then: "preview shows unsanitized in browser"
      applicationImage.attr('src') =~ /^blob.*/
      applicationCancelButton.click()
  }
}
