/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryPathResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryPathResponseDTO.ApiRepositoryComponentPath;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryPathResponseDTO.ApiRepositoryPathVersions;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.integration.repository.AbstractRepositoryService;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.error.exception.BadRequestException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.125
 */
@Named
@Singleton
public class ApiRepositoryPathService
{
  private static final Logger log = LoggerFactory.getLogger(ApiRepositoryPathService.class);

  private final RepositoryDAO repositoryDAO;

  private final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  @Inject
  public ApiRepositoryPathService(
      final RepositoryDAO repositoryDAO,
      final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO)
  {
    this.repositoryDAO = repositoryDAO;
    this.proxyRepositoryComponentDAO = proxyRepositoryComponentDAO;
  }

  @Authorize(permission = Permission.READ)
  public ApiRepositoryPathResponseDTO getQuarantinedByPathnames(
      final String repositoryManagerInstanceId,
      final String repositoryPublicId,
      final List<String> pathnames)
  {
    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(
        repositoryManagerInstanceId, repositoryPublicId);

    log.debug("Getting unquarantined component paths for repository {}:{} ({}), since {}.", repositoryManagerInstanceId,
        repositoryPublicId, repository.getId(), pathnames);
    validateIsProxyRepository(repository);

    ApiRepositoryPathResponseDTO repositoryPathResponse = new ApiRepositoryPathResponseDTO();
    if (pathnames == null) {
      return repositoryPathResponse;
    }

    List<ProxyRepositoryComponent> quarantinedComponents =
        proxyRepositoryComponentDAO.getQuarantinedByRepositoryId(repository.getId());
    int index = 0;
    for (String path : pathnames) {
      ApiRepositoryPathVersions pathVersions = new ApiRepositoryPathVersions();
      pathVersions.requestIndex = index;
      for (ProxyRepositoryComponent matchedRepositoryPath : getComponentsAtPath(repository.getFormat(),
          quarantinedComponents, path))
      {
        ApiRepositoryComponentPath repositoryPathStatus = new ApiRepositoryComponentPath();
        repositoryPathStatus.quarantine = true;
        repositoryPathStatus.pathname = matchedRepositoryPath.getPathname();
        pathVersions.repositoryComponentPaths.add(repositoryPathStatus);
      }
      repositoryPathResponse.pathVersions.add(pathVersions);
      index++;
    }
    return repositoryPathResponse;
  }

  private List<ProxyRepositoryComponent> getComponentsAtPath(
      final String format,
      final List<ProxyRepositoryComponent> quarantinedComponents,
      final String path)
  {
    List<ProxyRepositoryComponent> matchedComponents;
    if (ComponentIdentifier.FORMAT_NPM.equals(format)) {
      if (path.matches(".+/-/.+")) {
        final String searchPath = extractNpmPath(AbstractRepositoryService.normalizePathname(path));
        matchedComponents = quarantinedComponents.stream()
            .filter(
                component -> extractNpmPath(component.getPathname()).equals(searchPath))
            .collect(Collectors.toList());
      }
      else {
        throw new UnsupportedOperationException("NPM path is not supported for repository paths.");
      }
    }
    else {
      throw new UnsupportedOperationException("Format is not supported for repository paths.");
    }
    return matchedComponents;
  }

  private String extractNpmPath(final String pathname) {
    return pathname.substring(0, pathname.lastIndexOf("/") + 1);
  }

  private static void validateIsProxyRepository(Repository repository) {
    if (!RepositoryType.proxy.equals(repository.getRepositoryType())) {
      throw new BadRequestException(
          "Repository " + repository.getPublicId() + " (" + repository.getId() + ") is not a proxy repository");
    }
  }
}
