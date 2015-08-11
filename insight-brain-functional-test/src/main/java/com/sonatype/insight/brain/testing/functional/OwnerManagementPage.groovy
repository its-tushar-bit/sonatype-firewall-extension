/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.testing.functional.modules.ImportPolicyModule

import geb.Module

class OwnerManagementPage
    extends BasePage
{
  static url = "assets/index.html#/management"
  static defaultOrganizationName = 'test organization'
  static defaultApplicationName = 'test application'
  static defaultApplicationId = 'test-application'

  static at = { ownerTreeView.displayed }

  static content = {
    ownerTreeView(required: false) { module OwnerTreeViewModule, $('.tree-view') }
  }

  def selectOrganization(organizationName) {
    def organizationNode = ownerTreeView.organization(organizationName)
    waitFor { organizationNode.displayed }
    organizationNode.treeViewElement.click()
  }

  def selectApplication(organizationName, applicationName) {
    def organizationNode = ownerTreeView.organization(organizationName)
    waitFor { organizationNode.displayed }
    organizationNode.twisty.click()
    def applicationNode = organizationNode.application(applicationName)
    waitFor { applicationNode.displayed }
    applicationNode.click()
  }

  def createOrganization(name = defaultOrganizationName) {
    ownerTreeView.createOrganization(name)
  }

  def createApplication(applicationName = defaultApplicationName, applicationId = defaultApplicationId,
      organizationName = defaultOrganizationName)
  {
    def organizationNode = ownerTreeView.organization(organizationName)
    waitFor { organizationNode.displayed }
    organizationNode.treeViewElement.click()
    organizationNode.createApplication(applicationName, applicationId, organizationName)
  }
}

class OwnerTreeViewModule
    extends Module
{
  static content = {
    ownerFilter { $('.tree-view-filter input') }
    rootOrganization(required: false) { module RootOrganizationNode, $('.tree-view-root-organization-group') }
    organizations(required: false) { moduleList OrganizationNode, $('.tree-view-organization-group') }
    organization(required: false, to: OrganizationPage) {
      name -> organizations.find { it.organizationName.text() == name }
    }
  }

  def createOrganization(name = OwnerManagementPage.defaultOrganizationName) {
    rootOrganization.treeViewElement.click()
    waitFor{ rootOrganization.newOrganizationButton.displayed }
    rootOrganization.newOrganizationButton.click()
    browser.with {
      OrganizationPage organizationPage = at(OrganizationPage)
      organizationPage.editOrg(name)
    }
  }

  def createOrgWithDefaultPolicy(name = OwnerManagementPage.defaultOrganizationName, File file = ImportPolicyModule.samplePolicyFile) {
    rootOrganization.treeViewElement.click()
    waitFor{ rootOrganization.newOrganizationButton.displayed }
    rootOrganization.newOrganizationButton.click()
    browser.with {
      OrganizationPage organizationPage = at(OrganizationPage)
      organizationPage.editOrg(name)
      waitFor { organizationPage.policies.displayed }
      organizationPage.policyImport.importPolicy(file)
    }
  }
}

class RootOrganizationNode
    extends Module
{
  static content = {
    treeViewElement { $('.tree-view-item') }
    newOrganizationButton { $('.tree-view-new-organization button') }
  }

  def getName() {
    return treeViewElement.text().replace('\nNew Organization', '')
  }
}

class OrganizationNode
    extends Module
{
  static content = {
    treeViewElement { $('.tree-view-item:first-child') }
    organizationName { $('.tree-view-item:first-child > span') }
    twisty { $('.twisty') }
    newApplicationButton { $('.tree-view-new-application button') }
    applications(required: false) { $('.tree-view-item:not(:first-child)') }
    application(required: false, to: ApplicationPage) { name -> applications.find { it.text() == name } }
  }

  def createApplication(name = OwnerManagementPage.defaultApplicationName,
      id = OwnerManagementPage.defaultApplicationId, orgName = OwnerManagementPage.defaultOrganizationName)
  {
    newApplicationButton.click()
    browser.with {
      ApplicationPage applicationPage = at(ApplicationPage)
      applicationPage.editNewApp(name, id, orgName)
    }
  }
}
