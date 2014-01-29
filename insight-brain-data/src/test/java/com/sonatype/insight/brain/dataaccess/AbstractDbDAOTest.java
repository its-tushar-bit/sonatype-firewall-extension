/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.After;

public abstract class AbstractDbDAOTest
{
  protected Application application;

  protected String applicationId;

  protected Organization organization;

  protected Set<Application> applicationsToDelete = new LinkedHashSet<Application>();

  protected Set<Organization> organizationsToDelete = new LinkedHashSet<Organization>();

  protected Set<Tag> tagsToDelete = new LinkedHashSet<>();

  protected Set<ApplicationTag> appTagsToDelete = new LinkedHashSet<>();

  public static final String[] INVALID_ALPHANUMERIC = { "!", "@", "#", "$", "%", "^", "&", "*", "(", "_", "+" };

  public static final String[] INVALID_SPACING_NAMES = {
      " leading space", "trailing space ", "double  space",
      "  starts with double space", "ends with double space  "
  };

  private String uuid() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  protected Organization createOrganization(String name) {
    Organization organization = new Organization(name);
    new OrganizationDAO().insert(organization);
    organizationsToDelete.add(organization);
    return organization;
  }

  protected void createDefaultApplication() {
    // Create an organization
    organization = createOrganization("AbstractDbDAOTest");

    application = new Application("AbstractDbDAOTest_AppPublicId", "AbstractDbDAOTest-AppName", organization.getId());
    new ApplicationDAO().insert(application);
    applicationsToDelete.add(application);
    applicationId = application.getId();
  }

  protected Application createApplication(String name, String publicId, String parentId) {
    Application application = new Application(publicId, name, parentId);
    new ApplicationDAO().insert(application);
    applicationsToDelete.add(application);
    return application;
  }

  protected Tag createTag(String parentId) {
    return createTag("Tag name " + uuid(), "Tag description " + uuid(), parentId);
  }

  protected Tag createTag(String name, String description, String parentId) {
    return createTag(name, description, parentId, Color.yellow);
  }

  protected Tag createTag(String name, String description, String parentId, Color color) {
    Tag tag = new Tag(parentId, name, description, color);
    new TagDAO().insert(tag);
    tagsToDelete.add(tag);
    return tag;
  }

  protected ApplicationTag createApplicationTag(String applicationId, String tagId) {
    ApplicationTag appTag = new ApplicationTag(applicationId, tagId);
    new ApplicationTagDAO().insert(appTag);
    return appTag;
  }

  protected PolicyTag createPolicyTag(String policyId, String tagId) {
    PolicyTag policyTag = new PolicyTag(policyId, tagId);
    new PolicyTagDAO().insert(policyTag);
    return policyTag;
  }

  @After
  public void tearDown() {
    TagDAO tagDAO = new TagDAO();
    PolicyTagDAO policyTagDAO = new PolicyTagDAO();
    for (Tag tag : tagsToDelete) {
      tag = tagDAO.getById(tag.getId());
      if (tag != null) {
        for (PolicyTag policyTag : policyTagDAO.getByTagId(tag.getId())) {
          policyTagDAO.delete(policyTag);
        }
        tagDAO.delete(tag);
      }
    }

    ApplicationDAO applicationDAO = new ApplicationDAO();
    for (Application application : applicationsToDelete) {
      application = applicationDAO.getById(application.getId());
      if (application != null) {
        applicationDAO.delete(application);
      }
    }

    OrganizationDAO organizationDAO = new OrganizationDAO();
    for (Organization organization : organizationsToDelete) {
      organization = organizationDAO.getById(organization.getId());
      if (organization != null) {
        organizationDAO.delete(organization);
      }
    }

    ApplicationTagDAO appTagDAO = new ApplicationTagDAO();
    for (ApplicationTag appTag : appTagsToDelete) {
      appTag = appTagDAO.getById(appTag.getId());
      if (appTag != null) {
        appTagDAO.delete(appTag);
      }
    }
  }
}
