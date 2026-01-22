/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.lang.reflect.Method;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MultivaluedMap;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.Application;

import ru.vyarus.dropwizard.guice.module.installer.order.Order;

/**
 * Audits the event kind for a REST resource. Worth to highlight is that this request filter can grab the event even if
 * the REST method is never actually invoked (e.g. because its parameters couldn't be deserialized). Put differently,
 * this request filter is the first opportunity where the request path has been mapped to a REST resource, allowing to
 * reason about the specific operation undertaken by the caller.
 */
@jakarta.ws.rs.ext.Provider
// high priority (i.e. low number) to get called before others like LicenseAwareContainerDynamicFeature
@Priority(AuditContainerRequestFilter.PRIORITY)
@Order(Integer.MAX_VALUE - AuditContainerRequestFilter.PRIORITY)
public class AuditContainerRequestFilter
    implements ContainerRequestFilter
{
  public static final int PRIORITY = Priorities.AUTHENTICATION / 2;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryManagerDAO repositoryManagerDAO;

  private final Provider<ResourceInfo> resourceInfoProvider;

  @Inject
  public AuditContainerRequestFilter(
      final ApplicationDAO applicationDAO,
      final OrganizationDAO organizationDAO,
      final RepositoryDAO repositoryDAO,
      final RepositoryManagerDAO repositoryManagerDAO,
      final Provider<ResourceInfo> resourceInfoProvider)
  {
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.repositoryDAO = repositoryDAO;
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.resourceInfoProvider = resourceInfoProvider;
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    ResourceInfo resInfo = resourceInfoProvider.get();
    Method method = resInfo.getResourceMethod();
    if (method != null) {
      Audited audited = method.getAnnotation(Audited.class);
      if (audited == null && method.getDeclaringClass().getName().contains("Guice$$")) {
        // workaround for https://github.com/google/guice/issues/201
        // resource classes using AOP (e.g. for @Authorize) get subclassed but the generated subclasses miss the
        // annotations so we have to manually inspect the original class
        try {
          audited = method.getDeclaringClass().getSuperclass().getMethod(method.getName(), method.getParameterTypes())
              .getAnnotation(Audited.class);
        }
        catch (NoSuchMethodException e) {
          throw new IllegalStateException(e);
        }
      }
      if (audited != null) {
        AuditData.get().setEvent(audited.value());
        MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
        String applicationId = pathParameters.getFirst("applicationId");
        if (applicationId != null) {
          setByApplicationId(applicationId);
          return;
        }
        String applicationPublicId = pathParameters.getFirst("applicationPublicId");
        if (applicationPublicId != null) {
          setByApplicationPublicId(applicationPublicId);
          return;
        }
        String organizationId = pathParameters.getFirst("organizationId");
        if (organizationId != null) {
          setByOrganizationId(organizationId);
          return;
        }
        String repositoryId = pathParameters.getFirst("repositoryId");
        if (repositoryId != null) {
          setByRepositoryId(repositoryId);
          return;
        }
        String repositoryPublicId = pathParameters.getFirst("repositoryPublicId");
        if (repositoryPublicId != null) {
          String repositoryManagerInstanceId = pathParameters.getFirst("repositoryManagerInstanceId");
          if (repositoryManagerInstanceId != null) {
            setByRepositoryPublicId(repositoryPublicId, repositoryManagerInstanceId);
            return;
          }
        }
        String ownerType = pathParameters.getFirst("ownerType");
        if (ownerType != null) {
          boolean internalId = false;
          String ownerId = pathParameters.getFirst("ownerId");
          if (ownerId == null) {
            ownerId = pathParameters.getFirst("internalOwnerId");
            internalId = true;
          }
          setByOwnerIdAndType(ownerId, internalId, ownerType);
        }
        String repositoryManagerId = pathParameters.getFirst("repositoryManagerId");
        if (repositoryManagerId != null) {
          setByRepositoryManagerId(repositoryManagerId);
          return;
        }
      }
    }
  }

  private void setByOwnerIdAndType(String ownerId, boolean internalId, String ownerType) {
    switch (ownerType) {
      case "application": {
        if (ownerId != null) {
          if (internalId) {
            setByApplicationId(ownerId);
          }
          else {
            setByApplicationAnyId(ownerId);
          }
        }
        break;
      }
      case "organization": {
        if (ownerId != null) {
          setByOrganizationId(ownerId);
        }
        break;
      }
      case "repository": {
        if (ownerId != null) {
          setByRepositoryId(ownerId);
        }
        break;
      }
      case "repository_manager": {
        if (ownerId != null) {
          setByRepositoryManagerId(ownerId);
        }
        break;
      }
      case "repository_container": {
        AuditData.get().setRepositoryContainer();
        break;
      }
      case "global": {
        AuditData.get().setGlobal();
        break;
      }
      default:
        // left to REST resource
    }
  }

  private void setByApplicationId(String applicationId) {
    AuditData.get().setApplicationId(applicationId).setApplication(applicationDAO.getById(applicationId));
  }

  private void setByApplicationPublicId(String applicationPublicId) {
    AuditData.get().setApplicationPublicId(applicationPublicId)
        .setApplication(applicationDAO.getByPublicId(applicationPublicId));
  }

  private void setByApplicationAnyId(String id) {
    Application application = applicationDAO.getByPublicId(id);
    if (application == null) {
      application = applicationDAO.getById(id);
    }
    AuditData.get().setApplicationPublicId(id).setApplication(application);
  }

  private void setByOrganizationId(String organizationId) {
    AuditData.get().setOrganizationId(organizationId).setOrganization(organizationDAO.getById(organizationId));
  }

  private void setByRepositoryId(String repositoryId) {
    AuditData.get().setRepositoryId(repositoryId).setRepository(repositoryDAO.getById(repositoryId));
  }

  private void setByRepositoryPublicId(String repositoryPublicId, String repositoryManagerInstanceId) {
    AuditData.get().setRepositoryPublicId(repositoryPublicId).setRepository(
        repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repositoryManagerInstanceId, repositoryPublicId));
  }

  private void setByRepositoryManagerId(String repositoryManagerId) {
    AuditData.get().setRepositoryManagerId(repositoryManagerId)
        .setRepositoryManager(repositoryManagerDAO.getById(repositoryManagerId));
  }
}
