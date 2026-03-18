/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightContextDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightFilePathDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightFilePathsDTO;
import com.sonatype.insight.brain.dataaccess.AggregateFileDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.model.AggregateFile;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.dto.model.AnameAggregateFileGroup;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentDTO;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentFilePathsDTO;
import com.sonatype.insight.license.dto.model.LegalCommentDTO;
import com.sonatype.insight.license.dto.model.LegalCopyrightDTO;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides legal information for Copyrights
 *
 * @since 1.108
 */
@Named
public class ApiLegalCopyrightService
{
  private static final Logger LOGGER = LoggerFactory.getLogger(ApiLegalCopyrightService.class);

  private final ApiLicenseLegalHdsService hdsService;

  private final ApplicationComponentDAO applicationComponentDAO;

  private final AggregateFileDAO aggregateFileDAO;

  private final ProductLicense productLicense;

  @Inject
  public ApiLegalCopyrightService(
      final ApiLicenseLegalHdsService hdsService,
      final ApplicationComponentDAO applicationComponentDAO,
      final AggregateFileDAO aggregateFileDAO,
      final ProductLicense productLicense)
  {
    this.hdsService = hdsService;
    this.applicationComponentDAO = applicationComponentDAO;
    this.aggregateFileDAO = aggregateFileDAO;
    this.productLicense = productLicense;
  }

  /**
   * Get file paths for any copyright for a given component.
   *
   * <p>
   * <strong>Note</strong> This implementation will perform a HDS request to get the full copyright data for the
   * component for <strong>each page</strong>. <br>
   * This is done to avoid introducing in-memory cache in IQ which will
   * not be shared by cluster nodes.
   * </p>
   *
   * @since 1.108
   */
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public CopyrightFilePathsDTO getCopyrightFilePaths(
      @SuppressWarnings("unused") final @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") final @AuthzContext(AuthzContext.Key.ID) String ownerId,
      final ComponentIdentifier componentIdentifier,
      final String componentHash,
      final String copyrightContentHash,
      final int start,
      final int length)
  {
    checkLicense();
    componentIdentifier.validate();
    if (start < 0 || length <= 0) {
      throw new BadRequestException("Invalid pagination parameters");
    }

    final List<CopyrightFilePathDTO> filePaths = loadCopyrightFilePaths(
        componentIdentifier,
        componentHash,
        copyrightContentHash);
    return new CopyrightFilePathsDTO(
        filePaths.stream().skip(start).limit(length).collect(Collectors.toList()),
        filePaths.size());
  }

  /**
   * Get Copyright Context content for a given copyright and file in the component
   * <p>
   * <strong>Note</strong> This implementation will perform a HDS request to get the full copyright data for the
   * component for each copyright context request.
   * <br>
   * This is done to avoid introducing in-memory cache in IQ which will not be shared by cluster nodes.
   * </p>
   *
   * @since 1.108
   */
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public List<String> getCopyrightContextContent(
      @SuppressWarnings("unused") final @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") final @AuthzContext(AuthzContext.Key.ID) String ownerId,
      final ComponentIdentifier componentIdentifier,
      final String componentHash,
      final String copyrightContentHash,
      final String filePath)
  {
    checkLicense();
    componentIdentifier.validate();

    final List<CopyrightContextDTO> matchingContexts = loadCopyrightContexts(componentIdentifier, componentHash)
        .stream()
        .filter(copyrightContextDTO -> copyrightContextDTO.getFilePaths().contains(filePath) &&
            copyrightContextDTO.getCopyrightContentHashes().contains(copyrightContentHash))
        .collect(Collectors.toList());

    return matchingContexts.stream()
        .map(CopyrightContextDTO::getContent)
        .collect(Collectors.toList());
  }

  /**
   * Returns a number map of copyright content hashes to a number of source files that copyright appears in
   */
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public Map<String, Integer> getCopyrightFileCount(
      @SuppressWarnings("unused") final @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") final @AuthzContext(AuthzContext.Key.ID) String ownerId,
      final ComponentIdentifier componentIdentifier,
      final String componentHash)
  {
    checkLicense();
    componentIdentifier.validate();

    final Map<String, Integer> copyrightFileCount = new HashMap<>();

    loadCopyrightContexts(componentIdentifier, componentHash).forEach(
        context -> context.getCopyrightContentHashes().forEach(copyrightHash -> {
          final int existing = copyrightFileCount.computeIfAbsent(copyrightHash, key -> 0);
          copyrightFileCount.put(copyrightHash, existing + context.getFilePaths().size());
        }));

    return copyrightFileCount;
  }

  private Collection<CopyrightContextDTO> loadCopyrightContexts(
      final ComponentIdentifier componentIdentifier,
      final String componentHash)
  {
    if (componentIdentifier.isAname()) {
      return getAnameCopyrightContexts(componentIdentifier, componentHash);
    }
    final Collection<ComponentLegalCommentFilePathsDTO> componentLegalComment =
        hdsService.getComponentLegalCommentFilePaths(componentIdentifier);
    return componentLegalComment.stream()
        .flatMap(it -> it.getComments().stream())
        .map(comment -> new CopyrightContextDTO(
            comment.getContent(),
            comment.getFilePaths(),
            comment.getCopyrightContentHashes()))
        .collect(Collectors.toSet());
  }

  private List<CopyrightFilePathDTO> loadCopyrightFilePaths(
      final ComponentIdentifier componentIdentifier,
      final String componentHash,
      final String copyrightContentHash)
  {
    return loadCopyrightContexts(componentIdentifier, componentHash).stream()
        .filter(it -> it.getCopyrightContentHashes().contains(copyrightContentHash))
        .flatMap(it -> it.getFilePaths().stream())
        .collect(Collectors.toMap(Function.identity(), path -> 1, Integer::sum))
        .entrySet()
        .stream()
        .map(entry -> new CopyrightFilePathDTO(entry.getKey(), entry.getValue()))
        .sorted()
        .collect(Collectors.toList());
  }

  private Collection<CopyrightContextDTO> getAnameCopyrightContexts(
      final ComponentIdentifier componentIdentifier,
      final String componentHash)
  {
    final Map<String, AggregateFile> componentAggregateHashes = getAggregateFiles(componentHash);
    if (componentAggregateHashes.isEmpty()) {
      return Collections.emptySet();
    }
    final AnameAggregateFileGroup anameAggregateFileGroup =
        new AnameAggregateFileGroup(
            componentIdentifier,
            ImmutableList.copyOf(componentAggregateHashes.keySet()));

    final Set<ComponentLegalCommentDTO> componentLegalComments =
        hdsService.getAnameRawComponentLegalComments(
            ImmutableSet.of(anameAggregateFileGroup));

    // collect all different file paths which contain the same copyright context
    final Map<String, CopyrightContextDTO> commentFileMap = new HashMap<>();
    componentLegalComments.forEach(componentLegalCommentDTO -> {
      final AggregateFile aggregateFile = componentAggregateHashes.get(componentLegalCommentDTO.getHash());
      final Set<String> filePaths = aggregateFile == null ? ImmutableSet.of() : aggregateFile.getPathnames();
      componentLegalCommentDTO.getComments().forEach(commentDTO -> {
        commentFileMap.putIfAbsent(commentDTO.getContent(),
            new CopyrightContextDTO(
                commentDTO.getContent(),
                new HashSet<>(),
                getCopyrightContentHashes(commentDTO)));
        commentFileMap.get(commentDTO.getContent()).getFilePaths().addAll(filePaths);
      });
    });
    return commentFileMap.values();
  }

  private static Set<String> getCopyrightContentHashes(final LegalCommentDTO comment) {
    return comment.getCopyrights()
        .stream()
        .map(LegalCopyrightDTO::getContentHash)
        .collect(Collectors.toSet());
  }

  private Map<String, AggregateFile> getAggregateFiles(final String componentHash) {
    if (componentHash == null) {
      return Collections.emptyMap();
    }
    final ApplicationComponent lastByHash = applicationComponentDAO.getLastByHash(componentHash);
    final List<AggregateFile> aggregateFiles =
        aggregateFileDAO.getByApplicationComponentId(lastByHash.getId());
    return aggregateFiles == null
        ? Collections.emptyMap()
        : aggregateFiles.stream()
            .collect(Collectors.toMap(AggregateFile::getHash, Function.identity()));
  }

  private void checkLicense() {
    if (!productLicense.hasFeature(LicensedFeature.ADVANCED_LEGAL_PACK)) {
      LOGGER.debug("License does not support Advanced Legal Pack features");
      throw new InvalidLicenseException();
    }
  }
}
