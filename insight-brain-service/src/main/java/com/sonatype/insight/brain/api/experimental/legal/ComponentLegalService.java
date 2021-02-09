/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentObligationAttributionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightOverrideDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.component.ComponentIdentifierValidator;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentCopyrightDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationAttributionDAO;
import com.sonatype.insight.brain.dataaccess.legal.CopyrightOverrideDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;
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

  private final ComponentObligationAttributionDAO componentObligationAttributionDAO;

  private final OwnerDAO ownerDAO;

  private final ProductLicense productLicense;

  private final CurrentUser currentUser;

  @Inject
  public ComponentLegalService(
      final CopyrightOverrideDAO copyrightOverrideDAO,
      final ComponentCopyrightDAO componentCopyrightDAO,
      final ComponentObligationAttributionDAO componentObligationAttributionDAO,
      final OwnerDAO ownerDAO,
      final ProductLicense productLicense,
      final CurrentUser currentUser)
  {
    this.copyrightOverrideDAO = copyrightOverrideDAO;
    this.componentCopyrightDAO = componentCopyrightDAO;
    this.componentObligationAttributionDAO = componentObligationAttributionDAO;
    this.ownerDAO = ownerDAO;
    this.productLicense = productLicense;
    this.currentUser = currentUser;
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
   * Get the {@link ComponentObligationAttribution}s for a given owner, component, and obligation name.
   *
   * @param ownerType           the owner type for each {@link ComponentObligationAttribution} owner.
   * @param ownerId             the owner id for each {@link ComponentObligationAttribution} owner.
   * @param componentIdentifier the {@link ComponentIdentifier} for each {@link ComponentObligationAttribution}.
   * @param obligationName      the obligation name for each {@link ComponentObligationAttribution}.
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
    try (TransactionContext tx = componentObligationAttributionDAO.createTransactionContext()) {
      return componentObligationAttributionDAO.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
          tx,
          owner.getId(),
          componentIdentifier,
          Collections.singleton(obligationName)
      ).stream().map(ComponentObligationAttributionDTO::new).collect(Collectors.toList());
    }
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
      componentCopyrightDAO.update(tx, componentCopyright);
      persistCopyrightOverrides(tx, copyrightOverrides);
      tx.commit();
    }
    auditComponentCopyright(componentCopyright, copyrightOverrides);
  }

  private void persistNewComponentCopyright(
      final List<CopyrightOverride> copyrightOverrides,
      final ComponentCopyright componentCopyright)
  {
    try (TransactionContext tx = componentCopyrightDAO.createTransactionContext()) {
      tx.begin();
      componentCopyrightDAO.insert(tx, componentCopyright);
      copyrightOverrides
          .forEach(copyrightOverride -> copyrightOverride.setComponentCopyrightId(componentCopyright.getId()));
      persistCopyrightOverrides(tx, copyrightOverrides);
      tx.commit();
    }

    auditComponentCopyright(componentCopyright, copyrightOverrides);
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
        }
        else {
          copyrightOverrideDAO.insert(tx, copyrightOverride);
        }
      }
      else {
        //An empty or null copyright override signifies the user wants to delete the copyright override
        // Note we can only delete copyright override for custom copyright (i.e. no original_content_hash)
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
        throw new InvalidComponentCopyrightException("CopyrightOverride must have a status");
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

  private void validateApiComponentIdentifierDTOV2(ApiComponentIdentifierDTOV2 apiComponentIdentifierDTOV2) {
    ComponentIdentifierValidator
        .validate(apiComponentIdentifierDTOV2 == null ? null : apiComponentIdentifierDTOV2.toComponentIdentifier());
  }
}
