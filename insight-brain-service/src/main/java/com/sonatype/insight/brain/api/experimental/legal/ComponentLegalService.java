/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalObligationDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightWithOwnerDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentLegalFileDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentObligationAttributionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentSourceLinkDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightOverrideDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LegalFileOverrideDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LegalSourceLinkDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.SourceLinkOverrideDTO;
import com.sonatype.insight.brain.api.v2.service.legal.LegalReportBuilder;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.component.ComponentIdentifierValidator;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentCopyrightDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentLegalFileDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationAttributionDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentSourceLinkDAO;
import com.sonatype.insight.brain.dataaccess.legal.CopyrightOverrideDAO;
import com.sonatype.insight.brain.dataaccess.legal.LegalFileOverrideDAO;
import com.sonatype.insight.brain.dataaccess.legal.SourceLinkOverrideDAO;
import com.sonatype.insight.brain.model.HasOwnerId;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.brain.model.legal.ComponentSourceLink;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.legal.SourceLinkOverride;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.model.HasStringId;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.api.v2.service.legal.LicenseLegalComparators.LEGAL_SOURCE_LINK_COMPARATOR;

@Named
public class ComponentLegalService
{
  private static final Logger log = LoggerFactory.getLogger(ComponentLegalService.class);

  private static final int COPYRIGHT_CONTENT_MAX_CHARACTER = 1000;

  public static final int ATTRIBUTION_CONTENT_MAX_CHARACTER = 1000;

  private static final int SOURCE_LINK_CONTENT_MAX_CHARACTER = 1000;

  // TODO: Temporary placeholder until legalContentHash is implemented
  static final String NOT_IMPLEMENTED = "NA";

  private final CopyrightOverrideDAO copyrightOverrideDAO;

  private final ComponentCopyrightDAO componentCopyrightDAO;

  private final SourceLinkOverrideDAO sourceLinkOverrideDAO;

  private final ComponentSourceLinkDAO componentSourceLinkDAO;

  private final LegalFileOverrideDAO legalFileOverrideDAO;

  private final ComponentLegalFileDAO componentLegalFileDAO;

  private final ComponentObligationDAO componentObligationDAO;

  private final ComponentObligationAttributionDAO componentObligationAttributionDAO;

  private final OwnerDAO ownerDAO;

  private final ProductLicense productLicense;

  private final CurrentUser currentUser;

  private final IdUtils idUtils;

  @Inject
  public ComponentLegalService(
      final CopyrightOverrideDAO copyrightOverrideDAO,
      final ComponentCopyrightDAO componentCopyrightDAO,
      final SourceLinkOverrideDAO sourceLinkOverrideDAO,
      final ComponentSourceLinkDAO componentSourceLinkDAO,
      final LegalFileOverrideDAO legalFileOverrideDAO,
      final ComponentLegalFileDAO componentLegalFileDAO,
      final ComponentObligationDAO componentObligationDAO,
      final ComponentObligationAttributionDAO componentObligationAttributionDAO,
      final OwnerDAO ownerDAO,
      final ProductLicense productLicense,
      final CurrentUser currentUser,
      final IdUtils idUtils)
  {
    this.copyrightOverrideDAO = copyrightOverrideDAO;
    this.componentCopyrightDAO = componentCopyrightDAO;
    this.sourceLinkOverrideDAO = sourceLinkOverrideDAO;
    this.componentSourceLinkDAO = componentSourceLinkDAO;
    this.legalFileOverrideDAO = legalFileOverrideDAO;
    this.componentLegalFileDAO = componentLegalFileDAO;
    this.componentObligationDAO = componentObligationDAO;
    this.componentObligationAttributionDAO = componentObligationAttributionDAO;
    this.ownerDAO = ownerDAO;
    this.productLicense = productLicense;
    this.currentUser = currentUser;
    this.idUtils = idUtils;
  }

  /**
   * Save or update a {@link ComponentCopyright} and its {@link CopyrightOverride}s. If the given list of
   * CopyrightOverride is missing entries that are present in the database they will not be removed and will still be
   * associated with the ComponentCopyright. To remove a CopyrightOverride we need to "rollback" the ComponentCopyright
   * to the original HDS data, that is delete the ComponentCopyright and all of its children.
   *
   * @param ownerType - the owner type we are applying the ComponentCopyright from.
   * @param ownerId - the owner id we are applying the ComponentCopyright from.
   * @param componentCopyrightDTO - the ComponentCopyrightDTO to be persisted
   * @return the persisted ComponentCopyrightDTO.
   */
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public ComponentCopyrightDTO saveComponentCopyright(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId,
      final ComponentCopyrightDTO componentCopyrightDTO)
  {
    LegalServiceUtil.checkLicense(productLicense, log);
    ComponentIdentifier componentIdentifier = getComponentIdentifier(componentCopyrightDTO.getComponentIdentifier(),
        componentCopyrightDTO.getPackageUrl());
    validateComponentCopyrightDTO(componentCopyrightDTO, componentIdentifier);
    Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);
    ComponentCopyright componentCopyright = new ComponentCopyright(
        componentIdentifier,
        owner.getId(),
        NOT_IMPLEMENTED,
        currentUser.getUsername());
    componentCopyright.setId(componentCopyrightDTO.getId());
    List<CopyrightOverride> copyrightOverrides = componentCopyrightDTO.getCopyrightOverrides()
        .stream()
        .map(dto -> {
          final String content = StringUtils.isBlank(dto.getContent()) ? "" : dto.getContent();
          CopyrightOverride copyrightOverride = new CopyrightOverride(
              dto.getOriginalContentHash(),
              LegalServiceUtil.getContentHash(content),
              content,
              dto.getStatus(),
              componentCopyrightDTO.getId());
          copyrightOverride.setId(dto.getId());
          return copyrightOverride;
        })
        .collect(Collectors.toList());
    try (TransactionContext tx = componentCopyrightDAO.createTransactionContext()) {
      tx.begin();
      save(tx,
          componentCopyright,
          componentCopyrightDAO.getById(tx, componentCopyright.getId()),
          componentCopyrightDAO.getByOwnerIdAndComponentIdentifier(tx, componentCopyright.getOwnerId(),
              componentIdentifier),
          componentCopyrightDAO,
          copyrightOverrides,
          CopyrightOverride::setComponentCopyrightId);
      saveOverrides(tx,
          copyrightOverrides,
          copyrightOverrideDAO.getByComponentCopyrightId(tx, componentCopyright.getId()),
          copyrightOverrideDAO,
          copyrightOverrideDAO::getById,
          CopyrightOverride::getContent,
          CopyrightOverride::isUserCreated);
      tx.commit();
    }
    auditComponentCopyright(componentCopyright, copyrightOverrides);
    return ComponentCopyrightDTO.fromComponentCopyright(
        componentCopyright,
        copyrightOverrides.stream().map(CopyrightOverrideDTO::fromCopyrightOverride).collect(Collectors.toList()));
  }

  /**
   * Given a ComponentIdentifier and scope, return a ComponentCopyright which is equal in scope or higher. ROOT_ORG >
   * Organization > Application.
   * <p>
   * Throws {@link NotFoundException} if none match.
   *
   * @param ownerType - The ownerType of the scope we want
   * @param ownerId - The ownerId of the scope we want
   * @param componentIdentifier - The component identifier of the ComponentCopyright
   * @return A {@link ComponentCopyrightWithOwnerDTO}, which contains the ComponentCopyright at the scope at which it is
   *         applied.
   * @since 1.107
   */
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public ComponentCopyrightWithOwnerDTO getComponentCopyrightWithHierarchy(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.ID) final String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    LegalServiceUtil.checkLicense(productLicense, log);
    componentIdentifier.validate();
    Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);
    ComponentCopyright componentCopyright =
        componentCopyrightDAO.getByOwnerIdAndComponentIdentifierWithHierarchy(owner.getId(), componentIdentifier);
    if (componentCopyright == null) {
      return null;
    }
    List<CopyrightOverride> copyrightOverrides =
        copyrightOverrideDAO.getByComponentCopyrightId(componentCopyright.getId());
    return new ComponentCopyrightWithOwnerDTO(
        ComponentCopyrightDTO.fromComponentCopyright(
            componentCopyright,
            copyrightOverrides.stream()
                .sorted(LegalReportBuilder::sortCopyrightOverrides)
                .map(CopyrightOverrideDTO::fromCopyrightOverride)
                .collect(Collectors.toList())),
        componentCopyright.getOwnerId());
  }

  /**
   * Save or update a {@link ComponentLegalFile} and its {@link LegalFileOverride}s. If the given list of
   * LegalFileOverride is missing entries that are present in the database they will not be removed and will still be
   * associated with the ComponentLegalFile. To remove a LegalFileOverride we need to "rollback" the ComponentLegalFile
   * to the original HDS data, that is delete the ComponentLegalFile and all of its children.
   *
   * @param ownerType - the owner type we are applying the ComponentLegalFile from.
   * @param ownerId - the owner id we are applying the ComponentLegalFile from.
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
    LegalServiceUtil.checkLicense(productLicense, log);
    ComponentIdentifier componentIdentifier = getComponentIdentifier(componentLegalFileDTO.getComponentIdentifier(),
        componentLegalFileDTO.getPackageUrl());
    validateComponentLegalFileDTO(componentLegalFileDTO, componentIdentifier);
    Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);
    ComponentLegalFile componentLegalFile = new ComponentLegalFile(
        componentIdentifier,
        owner.getId(),
        componentLegalFileDTO.getLegalFileType(),
        NOT_IMPLEMENTED,
        currentUser.getUsername());
    componentLegalFile.setId(componentLegalFileDTO.getId());
    List<LegalFileOverride> legalFileOverrides = componentLegalFileDTO.getLegalFileOverrides()
        .stream()
        .map(dto -> {
          String content = StringUtils.isBlank(dto.getContent()) ? "" : dto.getContent();
          LegalFileOverride legalFileOverride = new LegalFileOverride(
              dto.getOriginalContentHash(),
              LegalServiceUtil.getContentHash(content),
              content,
              dto.getStatus(),
              componentLegalFileDTO.getId());
          legalFileOverride.setId(dto.getId());
          return legalFileOverride;
        })
        .collect(Collectors.toList());
    try (TransactionContext tx = componentLegalFileDAO.createTransactionContext()) {
      tx.begin();
      save(tx,
          componentLegalFile,
          componentLegalFileDAO.getById(tx, componentLegalFile.getId()),
          componentLegalFileDAO.getByOwnerIdAndComponentIdentifierAndType(tx, componentLegalFile.getOwnerId(),
              componentIdentifier, componentLegalFile.getType()),
          componentLegalFileDAO,
          legalFileOverrides,
          LegalFileOverride::setComponentLegalFileId);
      saveOverrides(tx,
          legalFileOverrides,
          legalFileOverrideDAO.getByComponentLegalFileId(tx, componentLegalFile.getId()),
          legalFileOverrideDAO,
          legalFileOverrideDAO::getById,
          LegalFileOverride::getContent,
          LegalFileOverride::isUserCreated);
      tx.commit();
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
    LegalServiceUtil.checkLicense(productLicense, log);
    ComponentIdentifierValidator.validate(componentIdentifier);
    Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);
    List<LegalFileOverride> legalFileOverrides = legalFileOverrideDAO
        .getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(owner.getId(), componentIdentifier, legalFileType)
        .stream()
        .sorted(LegalReportBuilder::sortLegalFileOverrides)
        .collect(Collectors.toList());
    if (legalFileOverrides.isEmpty()) {
      return null;
    }
    ComponentLegalFile componentLegalFile =
        componentLegalFileDAO.getById(legalFileOverrides.get(0).getComponentLegalFileId());
    return new ComponentLegalFileDTO(componentLegalFile, legalFileOverrides);
  }

  /**
   * Create or update a {@link ComponentObligation}s. If {@link ApiLicenseLegalObligationDTO#getId()} is null, then the
   * {@link ComponentObligation} will be created. Otherwise, if {@link ApiLicenseLegalObligationDTO#getId()} is not
   * null, then it must correspond to an existing {@link ComponentObligation#getId()} and this will be updated. Note
   * that in either case, {@link ApiLicenseLegalObligationDTO#getComponentIdentifier()} must be valid, {@link
   * ApiLicenseLegalObligationDTO#getName()} must not be null or empty, and {@link
   * ApiLicenseLegalObligationDTO#getStatus()} must not be null.
   *
   * @param ownerType the owner type for the {@link ComponentObligation} owner.
   * @param ownerId the owner id for the {@link ComponentObligation} owner.
   * @param componentObligationDTOs the {@link ApiLicenseLegalObligationDTO}s representing the {@link
   *          ComponentObligation}s to be created/updated.
   * @return a list of {@link ApiLicenseLegalObligationDTO}s representing the created/updated {@link
   *         ComponentObligation}s.
   * @since 1.106
   */
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public List<ApiLicenseLegalObligationDTO> saveComponentObligations(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      List<ApiLicenseLegalObligationDTO> componentObligationDTOs)
  {
    LegalServiceUtil.checkLicense(productLicense, log);
    componentObligationDTOs.forEach(this::validateComponentObligationDTO);
    Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);
    List<ApiLicenseLegalObligationDTO> result = new ArrayList<>();
    try (TransactionContext tx = componentObligationDAO.createTransactionContext()) {
      tx.begin();
      componentObligationDTOs.forEach(componentObligationDTO -> {
        AuditEvent auditEvent = componentObligationDTO.getId() == null
            ? AuditEvent.CREATE_COMPONENT_OBLIGATION
            : AuditEvent.UPDATE_COMPONENT_OBLIGATION;
        try (AuditSession ignored = AuditData.get().recordSubEvent(auditEvent, false)) {
          ComponentIdentifier componentIdentifier =
              getComponentIdentifier(componentObligationDTO.getComponentIdentifier(),
                  componentObligationDTO.getPackageUrl());
          auditComponentObligation(owner, componentIdentifier, componentObligationDTO.getName(),
              componentObligationDTO.getStatus(), componentObligationDTO.getComment());
          ComponentObligation componentObligation = new ComponentObligation(
              componentIdentifier,
              owner.getId(),
              componentObligationDTO.getName(),
              componentObligationDTO.getComment(),
              componentObligationDTO.getStatus(),
              NOT_IMPLEMENTED,
              currentUser.getUsername());
          componentObligation.setId(componentObligationDTO.getId());
          save(tx,
              componentObligation,
              componentObligationDAO.getById(tx, componentObligation.getId()),
              componentObligationDAO.getByOwnerIdAndComponentIdentifierAndObligationName(tx, owner.getId(),
                  componentIdentifier, componentObligation.getObligationName()),
              componentObligationDAO);
          result.add(new ApiLicenseLegalObligationDTO(componentObligation));
        }
      });
      tx.commit();
    }
    return result;
  }

  /**
   * Get a {@link ApiLicenseLegalObligationDTO} representing the {@link ComponentObligation} for a given owner,
   * component, and obligation name.
   *
   * @param ownerType the owner type for the {@link ComponentObligation} owner.
   * @param ownerId the owner id for the {@link ComponentObligation} owner.
   * @param componentIdentifier the {@link ComponentIdentifier} for the {@link ComponentObligation}.
   * @param obligationName the obligation name for the {@link ComponentObligation}.
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
    LegalServiceUtil.checkLicense(productLicense, log);
    ComponentIdentifierValidator.validate(componentIdentifier);
    Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);
    List<String> ownerIds = ownerDAO.getOwnerIds(owner.getId());
    try (TransactionContext tx = componentObligationDAO.createTransactionContext()) {
      return componentObligationDAO.getByOwnerIdsAndComponentIdentifierAndObligationNames(tx,
          ownerIds,
          componentIdentifier,
          Set.of(obligationName))
          .stream()
          .findFirst()
          .map(ApiLicenseLegalObligationDTO::new)
          .orElse(null);
    }
  }

  /**
   * Delete {@link ComponentObligation}s by its {@link ComponentObligation#getId()}.
   *
   * @param componentObligationIds a list of the {@link ComponentObligation#getId()} representing the {@link
   *          ComponentObligation} to be deleted.
   * @since 1.106
   */
  public void deleteComponentObligations(List<String> componentObligationIds) {
    LegalServiceUtil.checkLicense(productLicense, log);
    try (TransactionContext tx = componentObligationDAO.createTransactionContext()) {
      tx.begin();
      for (String componentObligationId : componentObligationIds) {
        ComponentObligation componentObligation = componentObligationDAO.getByIdNotNull(tx, componentObligationId);
        Owner owner = ownerDAO.getById(tx, componentObligation.getOwnerId());
        auditComponentObligation(owner, componentObligation.getComponentIdentifier(),
            componentObligation.getObligationName(), componentObligation.getStatus(), componentObligation.getComment());
        checkLegalReviewerPermission(owner);
        componentObligationDAO.delete(tx, componentObligation);
      }
      tx.commit();
    }
  }

  /**
   * Create or update a {@link ComponentObligationAttribution}. If {@link ComponentObligationAttributionDTO#getId()} is
   * null, then the {@link ComponentObligationAttribution} will be created. Otherwise, if {@link
   * ComponentObligationAttributionDTO#getId()} is not null, then it must correspond to an existing {@link
   * ComponentObligationAttribution#getId()} and this will be updated. Note that in either case, {@link
   * ComponentObligationAttributionDTO#getComponentIdentifier()} must be valid and {@link
   * ComponentObligationAttributionDTO#getContent()} must not be null or empty.
   *
   * @param ownerType the owner type for the {@link ComponentObligationAttribution} owner.
   * @param ownerId the owner id for the {@link ComponentObligationAttribution} owner.
   * @param componentObligationAttributionDTO the {@link ComponentObligationAttributionDTO} representing the {@link
   *          ComponentObligationAttribution} to be created/updated.
   * @return a {@link ComponentObligationAttributionDTO} representing the created/updated {@link
   *         ComponentObligationAttribution}.
   * @since 1.106
   */
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public ComponentObligationAttributionDTO saveComponentObligationAttribution(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      ComponentObligationAttributionDTO componentObligationAttributionDTO)
  {
    LegalServiceUtil.checkLicense(productLicense, log);
    ComponentIdentifier componentIdentifier = getComponentIdentifier(
        componentObligationAttributionDTO.getComponentIdentifier(), componentObligationAttributionDTO.getPackageUrl());
    validateComponentObligationAttributionDTO(componentObligationAttributionDTO, componentIdentifier);

    Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);
    auditComponentObligationAttribution(owner, componentIdentifier,
        componentObligationAttributionDTO.getObligationName(), componentObligationAttributionDTO.getContent());
    ComponentObligationAttribution componentObligationAttribution = new ComponentObligationAttribution(
        componentIdentifier,
        owner.getId(),
        componentObligationAttributionDTO.getObligationName(),
        componentObligationAttributionDTO.getContent(),
        NOT_IMPLEMENTED,
        currentUser.getUsername());
    componentObligationAttribution.setId(componentObligationAttributionDTO.getId());
    try (TransactionContext tx = componentObligationAttributionDAO.createTransactionContext()) {
      tx.begin();
      save(tx,
          componentObligationAttribution,
          componentObligationAttributionDAO.getById(tx, componentObligationAttribution.getId()),
          componentObligationAttributionDAO.getByOwnerIdAndComponentIdentifierAndObligationNames(tx, owner.getId(),
              componentIdentifier, Collections.singleton(componentObligationAttribution.getObligationName()))
              .stream()
              .findFirst()
              .orElse(null),
          componentObligationAttributionDAO);
      tx.commit();
    }
    return new ComponentObligationAttributionDTO(componentObligationAttribution);
  }

  /**
   * Get a list of {@link ComponentObligationAttributionDTO} representing the {@link ComponentObligationAttribution}s
   * for a given owner, component, and obligation name.
   *
   * @param ownerType the owner type for each {@link ComponentObligationAttribution} owner.
   * @param ownerId the owner id for each {@link ComponentObligationAttribution} owner.
   * @param componentIdentifier the {@link ComponentIdentifier} for each {@link ComponentObligationAttribution}.
   * @param obligationName the obligation name for each {@link ComponentObligationAttribution}.
   * @return a list of {@link ComponentObligationAttributionDTO} representing the {@link
   *         ComponentObligationAttribution}s.
   * @since 1.106
   */
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public List<ComponentObligationAttributionDTO> getComponentObligationAttributions(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      ComponentIdentifier componentIdentifier,
      String obligationName)
  {
    LegalServiceUtil.checkLicense(productLicense, log);
    ComponentIdentifierValidator.validate(componentIdentifier);
    Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);
    return componentObligationAttributionDAO.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        owner.getId(),
        componentIdentifier,
        Collections.singleton(obligationName))
        .stream()
        .map(ComponentObligationAttributionDTO::new)
        .collect(Collectors.toList());
  }

  /**
   * Delete a {@link ComponentObligationAttribution} by its {@link ComponentObligationAttribution#getId()}.
   *
   * @param componentObligationAttributionId the {@link ComponentObligationAttribution#getId()} representing the {@link
   *          ComponentObligationAttribution} to be deleted.
   * @since 1.106
   */
  public void deleteComponentObligationAttribution(String componentObligationAttributionId) {
    LegalServiceUtil.checkLicense(productLicense, log);
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
   * Save or update a {@link SourceLink} and its {@link SourceLinkOverride}s. If the given list of
   * SourceLinkOverride is missing entries that are present in the database they will not be removed and will still be
   * associated with the ComponentSourceLink. To remove a SourceLinkOverride we need to "rollback" the
   * ComponentSourceLink
   * to the original HDS data, that is delete the ComponentSourceLink and all of its children.
   *
   * @param ownerType - the owner type we are applying the ComponentSourceLink from.
   * @param ownerId - the owner id we are applying the ComponentSourceLink from.
   * @param componentSourceLinkDTO - the ComponentSourceLinkDTO to be persisted
   * @return the persisted ComponentSourceLinkDTO.
   *
   * @since 1.133
   */
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public ComponentSourceLinkDTO saveComponentSourceLink(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId,
      final ComponentSourceLinkDTO componentSourceLinkDTO)
  {
    LegalServiceUtil.checkLicense(productLicense, log);

    ComponentIdentifier componentIdentifier =
        getComponentIdentifier(componentSourceLinkDTO.getComponentIdentifier(),
            componentSourceLinkDTO.getPackageUrl());
    validateComponentSourceLinkDTO(componentSourceLinkDTO, componentIdentifier);

    // ownerType and ownerId from the params are meant to check for permissions, but the custom data is not scoped
    ComponentSourceLink componentSourceLink =
        new ComponentSourceLink(componentIdentifier,
            Organization.ROOT_ORGANIZATION_ID, currentUser.getUsername());
    componentSourceLink.setId(componentSourceLinkDTO.getId());
    List<SourceLinkOverride> sourceLinkOverrides = componentSourceLinkDTO.getSourceLinkOverrides().stream().map(dto -> {
      final String content = StringUtils.isBlank(dto.getContent()) ? "" : dto.getContent();
      final String originalContent = StringUtils.defaultIfBlank(dto.getOriginalContent(), dto.getContent());
      SourceLinkOverride sourceLinkOverride =
          new SourceLinkOverride(content, originalContent, dto.getStatus(), componentSourceLinkDTO.getId());
      sourceLinkOverride.setId(dto.getId());
      return sourceLinkOverride;
    }).collect(Collectors.toList());
    try (TransactionContext tx = componentSourceLinkDAO.createTransactionContext()) {
      tx.begin();
      save(tx,
          componentSourceLink,
          componentSourceLinkDAO.getById(tx, componentSourceLink.getId()),
          componentSourceLinkDAO.getByOwnerIdAndComponentIdentifier(tx, componentSourceLink.getOwnerId(),
              componentIdentifier),
          componentSourceLinkDAO,
          sourceLinkOverrides,
          SourceLinkOverride::setComponentSourceLinkId);
      saveOverrides(tx,
          sourceLinkOverrides,
          sourceLinkOverrideDAO.getByComponentSourceLinkId(tx, componentSourceLink.getId()),
          sourceLinkOverrideDAO,
          sourceLinkOverrideDAO::getById,
          SourceLinkOverride::getContent,
          SourceLinkOverride::isUserCreated);
      tx.commit();
    }
    auditComponentSourceLink(componentSourceLink, sourceLinkOverrides);
    return ComponentSourceLinkDTO.fromComponentSourceLink(componentSourceLink,
        sourceLinkOverrides.stream().map(SourceLinkOverrideDTO::fromSourceLinkOverride).collect(Collectors.toList()));
  }

  /**
   * Saves a legal entity.
   * <p>
   * If an id was given, but no corresponding old entity was found, then a NotFoundException is thrown.
   * <p>
   * If there's no old or conflicting entity, then a new one will be created.
   * <p>
   * Otherwise
   * <ul>
   * <li>If there's a conflicting entity, then it will be updated, else the old one will be updated.
   * Each override will have its legal entity id updated.</li>
   * <li>
   * If there's an old entity
   * <ul>
   * <li>And its owner is changing (either directly or by updating the conflicting entity), then the user must
   * have permission at the old owner.</li>
   * <li>And it's not being directly updated, then it will be deleted.
   * Each override to be saved will have its id set to null since any existing will have also been deleted.</li>
   * </ul>
   * </li>
   * </ul>
   *
   * @param tx the transaction context.
   * @param entity the legal entity to be saved.
   * @param old the legal entity, if any, that already exists with the id of the entity to be saved.
   * @param conflicting the legal entity, if any, that should be updated.
   * @param dao the legal entity dao.
   * @param overrides the overrides for the legal entity to be saved.
   * @param setLegalEntityId a function to set the legal entity id on an override.
   * @throws NotFoundException if the legal entity to be saved has an id that does not exist.
   */
  private <T extends HasStringId & HasOwnerId, K extends HasStringId> void save(
      TransactionContext tx,
      T entity,
      T old,
      T conflicting,
      AbstractOperationalSqlDAO<T> dao,
      List<K> overrides,
      BiConsumer<K, String> setLegalEntityId)
  {
    if (entity.getId() != null && old == null) {
      throw new NotFoundException(
          entity.getClass().getSimpleName() + " with ID " + entity.getId() + " does not exist.");
    }
    boolean isNewEntity = old == null && conflicting == null;
    if (isNewEntity) {
      dao.insert(tx, entity);
    }
    else {
      if (conflicting != null) {
        entity.setId(conflicting.getId());
      }
      else {
        entity.setId(old.getId());
      }
      if (old != null) {
        if (!old.getOwnerId().equals(entity.getOwnerId())) {
          checkLegalReviewerPermission(ownerDAO.getById(tx, old.getOwnerId()));
        }
        if (!old.getId().equals(entity.getId())) {
          dao.delete(tx, old);
          overrides.forEach(override -> override.setId(null));
        }
      }
      dao.update(tx, entity);
    }
    overrides.forEach(override -> setLegalEntityId.accept(override, entity.getId()));
  }

  private <T extends HasStringId & HasOwnerId> void save(
      TransactionContext tx,
      T entity,
      T old,
      T conflicting,
      AbstractOperationalSqlDAO<T> dao)
  {
    save(tx, entity, old, conflicting, dao, Collections.emptyList(), null);
  }

  /**
   * Saves the overrides for a legal entity.
   * <p>
   * Note that when saving overrides, they should all be passed in a batch.
   * <p>
   * If the legal entity has any old overrides that are not being updated, then they will be deleted.
   * <p>
   * For each override
   * <ul>
   * <li>If it has an id, but no corresponding old override was found, then a NotFoundException is thrown.</li>
   * <li>If it should not exist (i.e. it has no content and it's user created), then it will be ignored if it doesn't
   * exist or deleted it if it does exist.</li>
   * <li>Otherwise it will be inserted if it doesn't exist or updated it if it does exist.</li>
   * </ul>
   *
   * @param tx the transaction context.
   * @param overrides the overrides for the legal entity to be saved.
   * @param oldOverrides the old overrides, if any, for the legal entity.
   * @param dao the override dao.
   * @param getById a function returning an override, if any, given a transaction context and its id.
   * @param getContent a function returning the content of an override.
   * @param isUserCreated a function returning true if an override was created by a user.
   * @throws NotFoundException if an override to be saved has an id that does not exist.
   */
  private <T extends HasStringId> void saveOverrides(
      TransactionContext tx,
      List<T> overrides,
      List<T> oldOverrides,
      AbstractOperationalSqlDAO<T> dao,
      BiFunction<TransactionContext, String, T> getById,
      Function<T, String> getContent,
      Predicate<T> isUserCreated)
  {
    for (T old : oldOverrides) {
      if (overrides.stream().noneMatch(override -> old.getId().equals(override.getId()))) {
        dao.delete(tx, old);
      }
    }
    Iterator<T> i = overrides.iterator();
    while (i.hasNext()) {
      T override = i.next();
      T old = null;
      if (override.getId() != null) {
        old = getById.apply(tx, override.getId());
        if (old == null) {
          throw new NotFoundException(
              override.getClass().getSimpleName() + " with ID " + override.getId() + " does not exist.");
        }
      }
      boolean isNewEntity = old == null;
      boolean doNotPersist = StringUtils.isBlank(getContent.apply(override)) && isUserCreated.test(override);
      if (doNotPersist) {
        if (isNewEntity) {
          log.debug("Ignoring override {}. Won't persist blank content for custom overrides.", override);
        }
        else {
          dao.delete(tx, override);
        }
        i.remove();
      }
      else {
        if (isNewEntity) {
          dao.insert(tx, override);
        }
        else {
          dao.update(tx, override);
        }
      }
    }
  }

  @Authorize(permission = Permission.LEGAL_REVIEWER)
  void checkLegalReviewerPermission(@SuppressWarnings("unused") @AuthzContext(Key.OWNER) Owner owner) {
    // actual work done by AOP interceptor
  }

  private void validateComponentCopyrightDTO(
      final ComponentCopyrightDTO componentCopyrightDTO,
      ComponentIdentifier componentIdentifier)
  {
    ComponentIdentifierValidator.validate(componentIdentifier);

    for (CopyrightOverrideDTO copyrightOverrideDTO : componentCopyrightDTO.getCopyrightOverrides()) {
      if (copyrightOverrideDTO.getStatus() == null) {
        throw new InvalidComponentCopyrightException("CopyrightOverride must have a status.");
      }
      if (copyrightOverrideDTO.getContent() != null &&
          copyrightOverrideDTO.getContent().length() > COPYRIGHT_CONTENT_MAX_CHARACTER)
      {
        throw new InvalidComponentCopyrightException(
            String.format("CopyrightOverride content must be less than %s characters",
                COPYRIGHT_CONTENT_MAX_CHARACTER));
      }
    }
  }

  private void validateComponentSourceLinkDTO(
      final ComponentSourceLinkDTO componentSourceLinkDTO,
      ComponentIdentifier componentIdentifier)
  {
    ComponentIdentifierValidator.validate(componentIdentifier);

    for (SourceLinkOverrideDTO sourceLinkOverrideDTO : componentSourceLinkDTO.getSourceLinkOverrides()) {
      if (sourceLinkOverrideDTO.getStatus() == null) {
        throw new InvalidComponentSourceLinkException("SourceLinkOverride must have a status.");
      }
      if ((sourceLinkOverrideDTO.getContent() != null
          && sourceLinkOverrideDTO.getContent().length() > SOURCE_LINK_CONTENT_MAX_CHARACTER) ||
          (sourceLinkOverrideDTO.getOriginalContent() != null
              && sourceLinkOverrideDTO.getOriginalContent().length() > SOURCE_LINK_CONTENT_MAX_CHARACTER))
      {
        throw new InvalidComponentSourceLinkException(String
            .format("SourceLinkOverride content must be less than %s characters", SOURCE_LINK_CONTENT_MAX_CHARACTER));
      }
    }
  }

  private void validateComponentLegalFileDTO(
      ComponentLegalFileDTO componentLegalFileDTO,
      ComponentIdentifier componentIdentifier)
  {
    ComponentIdentifierValidator.validate(componentIdentifier);

    if (componentLegalFileDTO.getLegalFileType() == null) {
      throw new BadRequestException("ComponentLegalFileDTO must have a legal file type.");
    }

    for (LegalFileOverrideDTO legalFileOverrideDTO : componentLegalFileDTO.getLegalFileOverrides()) {
      if (legalFileOverrideDTO.getStatus() == null) {
        throw new BadRequestException("LegalFileOverride must have a status.");
      }
    }
  }

  private void validateComponentObligationDTO(ApiLicenseLegalObligationDTO componentObligationDTO) {
    ComponentIdentifier componentIdentifier = getComponentIdentifier(componentObligationDTO.getComponentIdentifier(),
        componentObligationDTO.getPackageUrl());
    ComponentIdentifierValidator.validate(componentIdentifier);
    if (StringUtils.isBlank(componentObligationDTO.getName())) {
      throw new BadRequestException("ComponentObligation must have a name.");
    }
    if (componentObligationDTO.getStatus() == null) {
      throw new BadRequestException("ComponentObligation must have a status.");
    }
  }

  private void validateComponentObligationAttributionDTO(
      ComponentObligationAttributionDTO componentObligationAttributionDTO,
      ComponentIdentifier componentIdentifier)
  {
    ComponentIdentifierValidator.validate(componentIdentifier);
    if (StringUtils.isBlank(componentObligationAttributionDTO.getContent())) {
      throw new BadRequestException("ComponentObligationAttribution must have content.");
    }
    if (componentObligationAttributionDTO.getContent() != null &&
        componentObligationAttributionDTO.getContent().length() > ATTRIBUTION_CONTENT_MAX_CHARACTER)
    {
      throw new BadRequestException(String.format(
          "ComponentObligationAttribution content must be less than %s characters", ATTRIBUTION_CONTENT_MAX_CHARACTER));
    }
  }

  private ComponentIdentifier getComponentIdentifier(ApiComponentIdentifierDTOV2 componentIdentifier, String pkgUrl) {
    if (componentIdentifier != null) {
      return componentIdentifier.toComponentIdentifier();
    }
    if (pkgUrl != null) {
      return new PackageUrlIdentifier(pkgUrl).toComponentIdentifier();
    }
    throw new BadRequestException("The component identifier cannot be null.");
  }

  private void auditComponentCopyright(
      final ComponentCopyright componentCopyright,
      final List<CopyrightOverride> copyrightOverrides)
  {
    AuditData.get().setComponentIdentifier(componentCopyright.getComponentIdentifier());
    AuditData.get()
        .setData("copyrights",
            copyrightOverrides.stream()
                .filter(c -> c.getStatus() == ComponentLegalPartStatus.ENABLED)
                .map(CopyrightOverride::getContent)
                .collect(Collectors.toList()));
  }

  private void auditComponentSourceLink(
      final ComponentSourceLink componentSourceLink,
      final List<SourceLinkOverride> sourceLinkOverrides)
  {
    AuditData.get().setComponentIdentifier(componentSourceLink.getComponentIdentifier());
    AuditData.get()
        .setData("Source Link",
            sourceLinkOverrides.stream()
                .filter(c -> c.getStatus() == ComponentLegalPartStatus.ENABLED)
                .map(SourceLinkOverride::getContent)
                .collect(Collectors.toList()));
  }

  private void auditComponentLegalFile(
      ComponentLegalFile componentLegalFile,
      List<LegalFileOverride> legalFileOverrides)
  {
    AuditData.get().setComponentIdentifier(componentLegalFile.getComponentIdentifier());
    AuditData.get()
        .setData(componentLegalFile.getType().toString() + "s",
            legalFileOverrides.stream()
                .filter(clf -> clf.getStatus() == ComponentLegalPartStatus.ENABLED)
                .map(LegalFileOverride::getId)
                .collect(Collectors.toList()));
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

  public Set<LegalSourceLinkDTO> getSourceLinksOverridesFromComponentIdentifier(
      String ownerId,
      ComponentIdentifier componentIdentifier)
  {
    return sourceLinkOverrideDAO.getByOwnerIdAndComponentIdentifierWithHierarchy(ownerId, componentIdentifier)
        .stream()
        .map(LegalSourceLinkDTO::new)
        .sorted(LEGAL_SOURCE_LINK_COMPARATOR)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
