/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalObligationDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightWithOwnerDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentLegalFileDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentObligationAttributionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightOverrideDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LegalFileOverrideDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.component.ComponentIdentifierValidator;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentCopyrightDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentLegalFileDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationAttributionDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO;
import com.sonatype.insight.brain.dataaccess.legal.CopyrightOverrideDAO;
import com.sonatype.insight.brain.dataaccess.legal.LegalFileOverrideDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ComponentLegalService
{
  private static final Logger log = LoggerFactory.getLogger(ComponentLegalService.class);

  //TODO: Temporary placeholder until legalContentHash is implemented
  static final String NOT_IMPLEMENTED = "NA";

  private final CopyrightOverrideDAO copyrightOverrideDAO;

  private final ComponentCopyrightDAO componentCopyrightDAO;

  private final LegalFileOverrideDAO legalFileOverrideDAO;

  private final ComponentLegalFileDAO componentLegalFileDAO;

  private final ComponentObligationDAO componentObligationDAO;

  private final ComponentObligationAttributionDAO componentObligationAttributionDAO;

  private final OwnerDAO ownerDAO;

  private final ProductLicense productLicense;

  private final CurrentUser currentUser;

  @Inject
  public ComponentLegalService(
      final CopyrightOverrideDAO copyrightOverrideDAO,
      final ComponentCopyrightDAO componentCopyrightDAO,
      final LegalFileOverrideDAO legalFileOverrideDAO,
      final ComponentLegalFileDAO componentLegalFileDAO,
      final ComponentObligationDAO componentObligationDAO,
      final ComponentObligationAttributionDAO componentObligationAttributionDAO,
      final OwnerDAO ownerDAO,
      final ProductLicense productLicense,
      final CurrentUser currentUser)
  {
    this.copyrightOverrideDAO = copyrightOverrideDAO;
    this.componentCopyrightDAO = componentCopyrightDAO;
    this.legalFileOverrideDAO = legalFileOverrideDAO;
    this.componentLegalFileDAO = componentLegalFileDAO;
    this.componentObligationDAO = componentObligationDAO;
    this.componentObligationAttributionDAO = componentObligationAttributionDAO;
    this.ownerDAO = ownerDAO;
    this.productLicense = productLicense;
    this.currentUser = currentUser;
  }

  /**
   * Given a ComponentIdentifier and scope, return a ComponentCopyright which is equal in scope or higher. ROOT_ORG >
   * Organization > Application.
   * <p>
   * Throws {@link NotFoundException} if none match.
   *
   * @param ownerType           - The ownerType of the scope we want
   * @param ownerId             - The ownerId of the scope we want
   * @param componentIdentifier - The component identifier of the ComponentCopyright
   * @return A {@link ComponentCopyrightWithOwnerDTO}, which contains the ComponentCopyright at the scope at which it is
   * applied.
   * @throws NotFoundException if no ComponentCopyrightFound
   * @since 1.107
   */
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public ComponentCopyrightWithOwnerDTO getComponentCopyrightWithHierarchy(
      @AuthzContext(Key.TYPE) final OwnerType ownerType, @AuthzContext(Key.ID) final String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    checkLicense();
    componentIdentifier.validate();
    Owner owner = IdUtils.getOwnerNotNull(ownerType, ownerId);

    ComponentCopyright componentCopyright =
        componentCopyrightDAO.getByOwnerIdAndComponentIdentifierWithHierarchy(owner.getId(), componentIdentifier);
    if (componentCopyright == null) {
      throw new NotFoundException("No component copyright found.");
    }
    List<CopyrightOverride> copyrightOverrides =
        copyrightOverrideDAO.getByComponentCopyrightId(componentCopyright.getId());

    return new ComponentCopyrightWithOwnerDTO(
        ComponentCopyrightDTO.fromComponentCopyright(
            componentCopyright,
            copyrightOverrides.stream().sorted(LegalReportBuilder::sortCopyrightOverrides)
                .map(CopyrightOverrideDTO::fromCopyrightOverride).collect(Collectors.toList())),
        componentCopyright.getOwnerId()
    );
  }

  /**
   * Save or update a {@link ComponentCopyright} and its {@link CopyrightOverride}s. If the given list of
   * CopyrightOverride is missing entries that are present in the database they will not be removed and will still be
   * associated with the ComponentCopyright. To remove a CopyrightOverride we need to "rollback" the ComponentCopyright
   * to the original HDS data, that is delete the ComponentCopyright and all of its children.
   *
   * @param ownerType             - the owner type we are applying the ComponentCopyright from.
   * @param ownerId               - the owner id we are applying the ComponentCopyright from.
   * @param componentCopyrightDTO - the ComponentCopyrightDTO to be persisted
   * @return the persisted ComponentCopyrightDTO.
   */
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public ComponentCopyrightDTO saveComponentCopyright(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId,
      final ComponentCopyrightDTO componentCopyrightDTO)
  {
    checkLicense();
    validateComponentCopyrightDTO(componentCopyrightDTO);

    Owner owner = IdUtils.getOwnerNotNull(ownerType, ownerId);
    List<CopyrightOverride> copyrightOverrides = componentCopyrightDTO.getCopyrightOverrides().stream()
        .map(dto -> {
          final String content = StringUtils.trimToEmpty(dto.getContent());
          CopyrightOverride copyrightOverride = new CopyrightOverride(
              dto.getOriginalContentHash(),
              ContentHashUtil.getContentHash(content),
              content,
              dto.getStatus(),
              componentCopyrightDTO.getId()
          );
          copyrightOverride.setId(dto.getId());
          return copyrightOverride;
        })
        .collect(Collectors.toList());

    ComponentCopyright componentCopyright = new ComponentCopyright(
        componentCopyrightDTO.getComponentIdentifier().toComponentIdentifier(),
        owner.getId(),
        NOT_IMPLEMENTED,
        currentUser.getUsername()
    );
    componentCopyright.setId(componentCopyrightDTO.getId());

    if (StringUtils.isBlank(componentCopyright.getId())) {
      persistNewComponentCopyright(copyrightOverrides, componentCopyright);
    }
    else {
      persistExitingComponentCopyright(copyrightOverrides, componentCopyright);
    }

    return ComponentCopyrightDTO.fromComponentCopyright(
        componentCopyright,
        copyrightOverrides.stream().map(CopyrightOverrideDTO::fromCopyrightOverride).collect(Collectors.toList())
    );
  }

  /**
   * Save or update a {@link ComponentLegalFile} and its {@link LegalFileOverride}s. If the given list of
   * LegalFileOverride is missing entries that are present in the database they will not be removed and will still be
   * associated with the ComponentLegalFile. To remove a LegalFileOverride we need to "rollback" the ComponentLegalFile
   * to the original HDS data, that is delete the ComponentLegalFile and all of its children.
   *
   * @param ownerType             - the owner type we are applying the ComponentLegalFile from.
   * @param ownerId               - the owner id we are applying the ComponentLegalFile from.
   * @param componentLegalFileDTO - the ComponentLegalFileDTO to be persisted.
   * @return the persisted ComponentLegalFileDTO.
   * @since 1.107
   */
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public ComponentLegalFileDTO saveComponentLegalFile(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      ComponentLegalFileDTO componentLegalFileDTO)
  {
    checkLicense();
    validateComponentLegalFileDTO(componentLegalFileDTO);
    Owner owner = IdUtils.getOwnerNotNull(ownerType, ownerId);
    List<LegalFileOverride> legalFileOverrides = componentLegalFileDTO.getLegalFileOverrides().stream()
        .map(dto -> {
          String content = StringUtils.trimToEmpty(dto.getContent());
          LegalFileOverride legalFileOverride = new LegalFileOverride(
              dto.getLegalFileType(),
              dto.getOriginalContentHash(),
              ContentHashUtil.getContentHash(content),
              content,
              dto.getStatus(),
              componentLegalFileDTO.getId()
          );
          legalFileOverride.setId(dto.getId());
          return legalFileOverride;
        })
        .collect(Collectors.toList());
    ComponentLegalFile componentLegalFile = new ComponentLegalFile(
        componentLegalFileDTO.getComponentIdentifier().toComponentIdentifier(),
        owner.getId(),
        NOT_IMPLEMENTED,
        currentUser.getUsername()
    );
    componentLegalFile.setId(componentLegalFileDTO.getId());
    if (StringUtils.isBlank(componentLegalFile.getId())) {
      persistNewComponentLegalFile(componentLegalFile, legalFileOverrides);
    }
    else {
      persistExitingComponentLegalFile(componentLegalFile, legalFileOverrides);
    }
    auditComponentLegalFile(componentLegalFile, legalFileOverrides);
    return new ComponentLegalFileDTO(componentLegalFile, legalFileOverrides);
  }

  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public ComponentLegalFileDTO getComponentLegalFile(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      ComponentIdentifier componentIdentifier,
      LegalFileType legalFileType)
  {
    checkLicense();
    ComponentIdentifierValidator.validate(componentIdentifier);
    Owner owner = IdUtils.getOwnerNotNull(ownerType, ownerId);
    List<LegalFileOverride> legalFileOverrides = legalFileOverrideDAO
        .getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(owner.getId(), componentIdentifier, legalFileType)
        .stream().sorted(LegalReportBuilder::sortLegalFileOverrides).collect(Collectors.toList());
    if (legalFileOverrides.isEmpty()) {
      return null;
    }
    ComponentLegalFile componentLegalFile =
        componentLegalFileDAO.getById(legalFileOverrides.get(0).getComponentLegalFileId());
    return new ComponentLegalFileDTO(componentLegalFile, legalFileOverrides);
  }

  /**
   * Create or update a {@link ComponentObligationAttribution}. If {@link ComponentObligationAttributionDTO#getId()} is
   * null, then the {@link ComponentObligationAttribution} will be created. Otherwise, if {@link
   * ComponentObligationAttributionDTO#getId()} is not null, then it must correspond to an existing {@link
   * ComponentObligationAttribution#getId()} and this will be updated. Note that in either case, {@link
   * ComponentObligationAttributionDTO#getComponentIdentifier()} must be valid and {@link
   * ComponentObligationAttributionDTO#getContent()} must not be null or empty.
   *
   * @param ownerType                         the owner type for the {@link ComponentObligationAttribution} owner.
   * @param ownerId                           the owner id for the {@link ComponentObligationAttribution} owner.
   * @param componentObligationAttributionDTO the {@link ComponentObligationAttributionDTO} representing the {@link
   *                                          ComponentObligationAttribution} to be created/updated.
   * @return a {@link ComponentObligationAttributionDTO} representing the created/updated {@link
   * ComponentObligationAttribution}.
   * @since 1.106
   */
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public ComponentObligationAttributionDTO saveComponentObligationAttribution(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      ComponentObligationAttributionDTO componentObligationAttributionDTO)
  {
    checkLicense();
    validateComponentObligationAttributionDTO(componentObligationAttributionDTO);
    ComponentIdentifier componentIdentifier =
        componentObligationAttributionDTO.getComponentIdentifier().toComponentIdentifier();
    Owner owner = IdUtils.getOwnerNotNull(ownerType, ownerId);
    auditComponentObligationAttribution(owner, componentIdentifier,
        componentObligationAttributionDTO.getObligationName(), componentObligationAttributionDTO.getContent());
    ComponentObligationAttribution componentObligationAttribution;
    if (componentObligationAttributionDTO.getId() == null) {
      componentObligationAttribution = new ComponentObligationAttribution(
          componentIdentifier,
          owner.getId(),
          componentObligationAttributionDTO.getObligationName(),
          componentObligationAttributionDTO.getContent(),
          NOT_IMPLEMENTED,
          currentUser.getUsername());
      componentObligationAttributionDAO.insert(componentObligationAttribution);
    }
    else {
      try (TransactionContext tx = componentObligationAttributionDAO.createTransactionContext()) {
        tx.begin();
        componentObligationAttribution =
            componentObligationAttributionDAO.getByIdNotNull(tx, componentObligationAttributionDTO.getId());
        if (!componentObligationAttribution.getOwnerId().equals(owner.getId())) {
          checkLegalReviewerPermission(ownerDAO.getById(tx, componentObligationAttribution.getOwnerId()));
        }
        componentObligationAttribution.setComponentIdentifier(componentIdentifier);
        componentObligationAttribution.setOwnerId(owner.getId());
        componentObligationAttribution.setObligationName(componentObligationAttributionDTO.getObligationName());
        componentObligationAttribution.setContent(componentObligationAttributionDTO.getContent());
        componentObligationAttribution.setLastUpdatedByUsername(currentUser.getUsername());
        componentObligationAttributionDAO.update(tx, componentObligationAttribution);
        tx.commit();
      }
    }
    return new ComponentObligationAttributionDTO(componentObligationAttribution);
  }

  /**
   * Delete a {@link ComponentObligationAttribution} by its {@link ComponentObligationAttribution#getId()}.
   *
   * @param componentObligationAttributionId the {@link ComponentObligationAttribution#getId()} representing the {@link
   *                                         ComponentObligationAttribution} to be deleted.
   * @since 1.106
   */
  public void deleteComponentObligationAttribution(String componentObligationAttributionId) {
    checkLicense();
    try (TransactionContext tx = componentObligationAttributionDAO.createTransactionContext()) {
      tx.begin();
      ComponentObligationAttribution componentObligationAttribution =
          componentObligationAttributionDAO.getByIdNotNull(tx, componentObligationAttributionId);
      Owner owner = ownerDAO.getById(tx, componentObligationAttribution.getOwnerId());
      auditComponentObligationAttribution(owner, componentObligationAttribution.getComponentIdentifier(),
          componentObligationAttribution.getObligationName(), componentObligationAttribution.getContent());
      checkLegalReviewerPermission(owner);
      componentObligationAttributionDAO.delete(tx, componentObligationAttribution);
      tx.commit();
    }
  }

  /**
   * Get a list of {@link ComponentObligationAttributionDTO} representing the {@link ComponentObligationAttribution}s
   * for a given owner, component, and obligation name.
   *
   * @param ownerType           the owner type for each {@link ComponentObligationAttribution} owner.
   * @param ownerId             the owner id for each {@link ComponentObligationAttribution} owner.
   * @param componentIdentifier the {@link ComponentIdentifier} for each {@link ComponentObligationAttribution}.
   * @param obligationName      the obligation name for each {@link ComponentObligationAttribution}.
   * @return a list of {@link ComponentObligationAttributionDTO} representing the {@link
   * ComponentObligationAttribution}s.
   * @since 1.106
   */
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public List<ComponentObligationAttributionDTO> getComponentObligationAttributions(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      ComponentIdentifier componentIdentifier,
      String obligationName)
  {
    checkLicense();
    ComponentIdentifierValidator.validate(componentIdentifier);
    Owner owner = IdUtils.getOwnerNotNull(ownerType, ownerId);
    return componentObligationAttributionDAO.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        owner.getId(),
        componentIdentifier,
        Collections.singleton(obligationName)
    ).stream().map(ComponentObligationAttributionDTO::new).collect(Collectors.toList());
  }

  /**
   * Create or update a {@link ComponentObligation}. If {@link ApiLicenseLegalObligationDTO#getId()} is null, then the
   * {@link ComponentObligation} will be created. Otherwise, if {@link ApiLicenseLegalObligationDTO#getId()} is not
   * null, then it must correspond to an existing {@link ComponentObligation#getId()} and this will be updated. Note
   * that in either case, {@link ApiLicenseLegalObligationDTO#getComponentIdentifier()} must be valid, {@link
   * ApiLicenseLegalObligationDTO#getName()} must not be null or empty, and {@link
   * ApiLicenseLegalObligationDTO#getStatus()} must not be null.
   *
   * @param ownerType              the owner type for the {@link ComponentObligation} owner.
   * @param ownerId                the owner id for the {@link ComponentObligation} owner.
   * @param componentObligationDTO the {@link ApiLicenseLegalObligationDTO} representing the {@link ComponentObligation}
   *                               to be created/updated.
   * @return a {@link ApiLicenseLegalObligationDTO} representing the created/updated {@link ComponentObligation}.
   * @since 1.106
   */
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public ApiLicenseLegalObligationDTO saveComponentObligation(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      ApiLicenseLegalObligationDTO componentObligationDTO)
  {
    checkLicense();
    validateComponentObligationDTO(componentObligationDTO);
    ComponentIdentifier componentIdentifier = componentObligationDTO.getComponentIdentifier().toComponentIdentifier();
    Owner owner = IdUtils.getOwnerNotNull(ownerType, ownerId);
    auditComponentObligation(owner, componentIdentifier, componentObligationDTO.getName(),
        componentObligationDTO.getStatus(), componentObligationDTO.getComment());
    ComponentObligation componentObligation;
    if (componentObligationDTO.getId() == null) {
      componentObligation = new ComponentObligation(
          componentIdentifier,
          owner.getId(),
          componentObligationDTO.getName(),
          componentObligationDTO.getComment(),
          componentObligationDTO.getStatus(),
          NOT_IMPLEMENTED,
          currentUser.getUsername());
      componentObligationDAO.insert(componentObligation);
    }
    else {
      try (TransactionContext tx = componentObligationDAO.createTransactionContext()) {
        tx.begin();
        componentObligation =
            componentObligationDAO.getByIdNotNull(tx, componentObligationDTO.getId());
        if (!componentObligation.getOwnerId().equals(owner.getId())) {
          checkLegalReviewerPermission(ownerDAO.getById(tx, componentObligation.getOwnerId()));
        }
        componentObligation.setComponentIdentifier(componentIdentifier);
        componentObligation.setOwnerId(owner.getId());
        componentObligation.setObligationName(componentObligationDTO.getName());
        componentObligation.setComment(componentObligationDTO.getComment());
        componentObligation.setStatus(componentObligationDTO.getStatus());
        componentObligation.setLastUpdatedByUsername(currentUser.getUsername());
        componentObligationDAO.update(tx, componentObligation);
        tx.commit();
      }
    }
    return new ApiLicenseLegalObligationDTO(componentObligation);
  }

  /**
   * Delete a {@link ComponentObligation} by its {@link ComponentObligation#getId()}.
   *
   * @param componentObligationId the {@link ComponentObligation#getId()} representing the {@link ComponentObligation}
   *                              to be deleted.
   * @since 1.106
   */
  public void deleteComponentObligation(String componentObligationId) {
    checkLicense();
    try (TransactionContext tx = componentObligationDAO.createTransactionContext()) {
      tx.begin();
      ComponentObligation componentObligation = componentObligationDAO.getByIdNotNull(tx, componentObligationId);
      Owner owner = ownerDAO.getById(tx, componentObligation.getOwnerId());
      auditComponentObligation(owner, componentObligation.getComponentIdentifier(),
          componentObligation.getObligationName(), componentObligation.getStatus(), componentObligation.getComment());
      checkLegalReviewerPermission(owner);
      componentObligationDAO.delete(tx, componentObligation);
      tx.commit();
    }
  }

  /**
   * Get a {@link ApiLicenseLegalObligationDTO} representing the {@link ComponentObligation} for a given owner,
   * component, and obligation name.
   *
   * @param ownerType           the owner type for the {@link ComponentObligation} owner.
   * @param ownerId             the owner id for the {@link ComponentObligation} owner.
   * @param componentIdentifier the {@link ComponentIdentifier} for the {@link ComponentObligation}.
   * @param obligationName      the obligation name for the {@link ComponentObligation}.
   * @return a {@link ApiLicenseLegalObligationDTO} representing the {@link ComponentObligation}.
   * @since 1.106
   */
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public ApiLicenseLegalObligationDTO getComponentObligation(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      ComponentIdentifier componentIdentifier,
      String obligationName)
  {
    checkLicense();
    ComponentIdentifierValidator.validate(componentIdentifier);
    Owner owner = IdUtils.getOwnerNotNull(ownerType, ownerId);
    return componentObligationDAO.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        owner.getId(),
        componentIdentifier,
        Collections.singleton(obligationName)
    ).stream().map(ApiLicenseLegalObligationDTO::new).findFirst().orElse(null);
  }

  @Authorize(permission = Permission.LEGAL_REVIEWER)
  void checkLegalReviewerPermission(@SuppressWarnings("unused") @AuthzContext(Key.OWNER) Owner owner) {
    // actual work done by AOP interceptor
  }

  private void persistExitingComponentCopyright(
      final List<CopyrightOverride> copyrightOverrides,
      final ComponentCopyright componentCopyright)
  {
    try (TransactionContext tx = componentCopyrightDAO.createTransactionContext()) {
      tx.begin();
      ComponentCopyright existingComponentCopyright = componentCopyrightDAO.getById(tx, componentCopyright.getId());

      if (!existingComponentCopyright.getOwnerId().equals(componentCopyright.getOwnerId())) {
        checkLegalReviewerPermission(ownerDAO.getById(existingComponentCopyright.getOwnerId()));
      }
      findConflictingComponentCopyright(componentCopyright, tx)
          .ifPresent(copyright -> componentCopyrightDAO.delete(tx, copyright));

      componentCopyrightDAO.update(tx, componentCopyright);
      persistCopyrightOverrides(tx, copyrightOverrides);
      tx.commit();
    }
    auditComponentCopyright(componentCopyright, copyrightOverrides);
  }

  private Optional<ComponentCopyright> findConflictingComponentCopyright(
      final ComponentCopyright componentCopyright, final TransactionContext tx)
  {
    ComponentCopyright conflictingComponentCopyright = componentCopyrightDAO
        .getByOwnerIdAndComponentIdentifier(tx, componentCopyright.getOwnerId(),
            componentCopyright.getComponentIdentifier());
    if (conflictingComponentCopyright == null ||
        conflictingComponentCopyright.getId().equals(componentCopyright.getId())) {
      return Optional.empty();
    }
    return Optional.of(conflictingComponentCopyright);
  }

  private void persistNewComponentCopyright(
      final List<CopyrightOverride> copyrightOverrides,
      final ComponentCopyright componentCopyright)
  {
    try (TransactionContext tx = componentCopyrightDAO.createTransactionContext()) {
      tx.begin();
      // Check if we are creating a new ComponentCopyright at an existing scope, if so
      // delete the existing ComponentCopyright and replace with the new one.
      findConflictingComponentCopyright(componentCopyright, tx)
          .ifPresent(copyright -> {
            componentCopyrightDAO.delete(tx, copyright);
            copyrightOverrides.forEach(co -> co.setId(null));
          });

      componentCopyrightDAO.insert(tx, componentCopyright);
      copyrightOverrides
          .forEach(copyrightOverride -> copyrightOverride.setComponentCopyrightId(componentCopyright.getId()));
      persistCopyrightOverrides(tx, copyrightOverrides);
      tx.commit();
    }

    auditComponentCopyright(componentCopyright, copyrightOverrides);
  }

  private void persistExitingComponentLegalFile(
      ComponentLegalFile componentLegalFile,
      List<LegalFileOverride> legalFileOverrides)
  {
    try (TransactionContext tx = componentLegalFileDAO.createTransactionContext()) {
      tx.begin();
      ComponentLegalFile existingComponentLegalFile = componentLegalFileDAO.getById(tx, componentLegalFile.getId());
      if (!existingComponentLegalFile.getOwnerId().equals(componentLegalFile.getOwnerId())) {
        checkLegalReviewerPermission(ownerDAO.getById(existingComponentLegalFile.getOwnerId()));
      }
      findConflictingComponentLegalFile(tx, componentLegalFile).ifPresent(clf -> componentLegalFileDAO.delete(tx, clf));
      componentLegalFileDAO.update(tx, componentLegalFile);
      persistLegalFileOverrides(tx, legalFileOverrides);
      tx.commit();
    }
  }

  private Optional<ComponentLegalFile> findConflictingComponentLegalFile(
      TransactionContext tx,
      ComponentLegalFile componentLegalFile)
  {
    ComponentLegalFile conflictingComponentLegalFile = componentLegalFileDAO
        .getByOwnerIdAndComponentIdentifier(tx, componentLegalFile.getOwnerId(),
            componentLegalFile.getComponentIdentifier());
    if (conflictingComponentLegalFile == null ||
        conflictingComponentLegalFile.getId().equals(componentLegalFile.getId())) {
      return Optional.empty();
    }
    return Optional.of(conflictingComponentLegalFile);
  }

  private void persistNewComponentLegalFile(
      ComponentLegalFile componentLegalFile,
      List<LegalFileOverride> legalFileOverrides)
  {
    try (TransactionContext tx = componentLegalFileDAO.createTransactionContext()) {
      tx.begin();
      findConflictingComponentLegalFile(tx, componentLegalFile).ifPresent(clf -> {
        componentLegalFileDAO.delete(tx, clf);
        legalFileOverrides.forEach(lfo -> lfo.setId(null));
      });
      componentLegalFileDAO.insert(tx, componentLegalFile);
      legalFileOverrides
          .forEach(legalFileOverride -> legalFileOverride.setComponentLegalFileId(componentLegalFile.getId()));
      persistLegalFileOverrides(tx, legalFileOverrides);
      tx.commit();
    }
  }

  private void auditComponentCopyright(
      final ComponentCopyright componentCopyright,
      final List<CopyrightOverride> copyrightOverrides)
  {
    AuditData.get().setComponentIdentifier(componentCopyright.getComponentIdentifier());
    AuditData.get().setData("copyrights",
        copyrightOverrides.stream()
            .filter(c -> c.getStatus() == ComponentLegalPartStatus.ENABLED)
            .map(CopyrightOverride::getContent).collect(Collectors.toList()));
  }

  private void auditComponentLegalFile(
      ComponentLegalFile componentLegalFile,
      List<LegalFileOverride> legalFileOverrides)
  {
    AuditData.get().setComponentIdentifier(componentLegalFile.getComponentIdentifier());
    AuditData.get().setData("notices",
        legalFileOverrides.stream()
            .filter(clf -> clf.getType() == LegalFileType.NOTICE)
            .filter(clf -> clf.getStatus() == ComponentLegalPartStatus.ENABLED)
            .map(LegalFileOverride::getId).collect(Collectors.toList()));
    AuditData.get().setData("licenses",
        legalFileOverrides.stream()
            .filter(clf -> clf.getType() == LegalFileType.LICENSE)
            .filter(clf -> clf.getStatus() == ComponentLegalPartStatus.ENABLED)
            .map(LegalFileOverride::getId).collect(Collectors.toList()));
  }

  private void auditComponentObligationAttribution(
      Owner owner,
      ComponentIdentifier componentIdentifier,
      String obligationName,
      String content)
  {
    AuditData.get()
        .setOwner(owner)
        .setComponentIdentifier(componentIdentifier)
        .setData("obligationName", obligationName)
        .setData("content", content);
  }

  private void auditComponentObligation(
      Owner owner,
      ComponentIdentifier componentIdentifier,
      String obligationName,
      ObligationStatus obligationStatus,
      String comment)
  {
    AuditData.get()
        .setOwner(owner)
        .setComponentIdentifier(componentIdentifier)
        .setData("obligationName", obligationName)
        .setData("obligationStatus", obligationStatus.toString())
        .setData("comment", comment);
  }

  private void persistCopyrightOverrides(
      final TransactionContext tx,
      final List<CopyrightOverride> copyrightOverrides)
  {
    Iterator<CopyrightOverride> i = copyrightOverrides.iterator();
    while (i.hasNext()) {
      CopyrightOverride copyrightOverride = i.next();
      if (StringUtils.isBlank(copyrightOverride.getId())) {
        if (StringUtils.isBlank(copyrightOverride.getContent()) &&
            copyrightOverride.isUserCreated()) {
          log.debug("Ignoring copyrightOverride {}. Won't persist blank content for custom copyrights.",
              copyrightOverride);
          i.remove();
        }
        else {
          copyrightOverrideDAO.insert(tx, copyrightOverride);
        }
      }
      else {
        // An empty or null copyright override signifies the user wants to delete the copyright override
        // Note we can only delete a copyright override for a custom copyright (i.e. no original_content_hash)
        if (StringUtils.isBlank(copyrightOverride.getContent()) &&
            copyrightOverride.isUserCreated()) {
          copyrightOverrideDAO.delete(tx, copyrightOverride);
          i.remove();
        }
        else {
          copyrightOverrideDAO.update(tx, copyrightOverride);
        }
      }
    }
  }

  private void persistLegalFileOverrides(TransactionContext tx, List<LegalFileOverride> legalFileOverrides) {
    Iterator<LegalFileOverride> i = legalFileOverrides.iterator();
    while (i.hasNext()) {
      LegalFileOverride legalFileOverride = i.next();
      if (StringUtils.isBlank(legalFileOverride.getId())) {
        if (StringUtils.isBlank(legalFileOverride.getContent()) &&
            legalFileOverride.isUserCreated()) {
          log.debug("Ignoring legalFileOverride {}. Won't persist blank content for custom legal files.",
              legalFileOverride);
          i.remove();
        }
        else {
          legalFileOverrideDAO.insert(tx, legalFileOverride);
        }
      }
      else {
        // An empty or null legal file override signifies the user wants to delete the legal file override
        // Note we can only delete a legal file override for a custom legal file (i.e. no original_content_hash)
        if (StringUtils.isBlank(legalFileOverride.getContent()) &&
            legalFileOverride.isUserCreated()) {
          legalFileOverrideDAO.delete(tx, legalFileOverride);
          i.remove();
        }
        else {
          legalFileOverrideDAO.update(tx, legalFileOverride);
        }
      }
    }
  }

  private void checkLicense() {
    if (!productLicense.hasFeature(LicensedFeature.ADVANCED_LEGAL_PACK)) {
      log.debug("License does not support Advanced Legal Pack features");
      throw new InvalidLicenseException();
    }
  }

  private void validateComponentCopyrightDTO(final ComponentCopyrightDTO componentCopyrightDTO) {
    validateApiComponentIdentifierDTOV2(componentCopyrightDTO.getComponentIdentifier());

    for (CopyrightOverrideDTO copyrightOverrideDTO : componentCopyrightDTO.getCopyrightOverrides()) {
      if (copyrightOverrideDTO.getStatus() == null) {
        throw new InvalidComponentCopyrightException("CopyrightOverride must have a status.");
      }
    }
  }

  private void validateComponentLegalFileDTO(ComponentLegalFileDTO componentLegalFileDTO) {
    validateApiComponentIdentifierDTOV2(componentLegalFileDTO.getComponentIdentifier());

    for (LegalFileOverrideDTO legalFileOverrideDTO : componentLegalFileDTO.getLegalFileOverrides()) {
      if (legalFileOverrideDTO.getLegalFileType() == null) {
        throw new BadRequestException("LegalFileOverride must have a legal file type.");
      }
      if (legalFileOverrideDTO.getStatus() == null) {
        throw new BadRequestException("LegalFileOverride must have a status.");
      }
    }
  }

  private void validateComponentObligationAttributionDTO(
      ComponentObligationAttributionDTO componentObligationAttributionDTO)
  {
    validateApiComponentIdentifierDTOV2(componentObligationAttributionDTO.getComponentIdentifier());
    if (StringUtils.isBlank(componentObligationAttributionDTO.getContent())) {
      throw new BadRequestException("ComponentObligationAttribution must have content.");
    }
  }

  private void validateComponentObligationDTO(ApiLicenseLegalObligationDTO componentObligationDTO) {
    validateApiComponentIdentifierDTOV2(componentObligationDTO.getComponentIdentifier());
    if (StringUtils.isBlank(componentObligationDTO.getName())) {
      throw new BadRequestException("ComponentObligation must have a name.");
    }
    if (componentObligationDTO.getStatus() == null) {
      throw new BadRequestException("ComponentObligation must have a status.");
    }
  }

  private void validateApiComponentIdentifierDTOV2(ApiComponentIdentifierDTOV2 apiComponentIdentifierDTOV2) {
    ComponentIdentifierValidator
        .validate(apiComponentIdentifierDTOV2 == null ? null : apiComponentIdentifierDTOV2.toComponentIdentifier());
  }
}
