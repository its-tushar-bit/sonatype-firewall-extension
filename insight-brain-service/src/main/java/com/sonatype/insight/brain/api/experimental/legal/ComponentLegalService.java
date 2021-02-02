/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightOverrideDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.component.ComponentIdentifierValidator;
import com.sonatype.insight.brain.dataaccess.legal.ComponentCopyrightDAO;
import com.sonatype.insight.brain.dataaccess.legal.CopyrightOverrideDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ComponentLegalService
{
  private static final Logger log = LoggerFactory.getLogger(ComponentLegalService.class);

  //TODO: Temporary placeholder until legalContentHash is implemented
  private static final String NOT_IMPLEMENTED = "NA";

  private final CopyrightOverrideDAO copyrightOverrideDAO;

  private final ComponentCopyrightDAO componentCopyrightDAO;

  private final ProductLicense productLicense;

  private final CurrentUser currentUser;

  @Inject
  public ComponentLegalService(
      final CopyrightOverrideDAO copyrightOverrideDAO,
      final ComponentCopyrightDAO componentCopyrightDAO,
      final ProductLicense productLicense, final CurrentUser currentUser)
  {
    this.copyrightOverrideDAO = copyrightOverrideDAO;
    this.componentCopyrightDAO = componentCopyrightDAO;
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

  private void persistExitingComponentCopyright(
      final List<CopyrightOverride> copyrightOverrides,
      final ComponentCopyright componentCopyright)
  {
    try (TransactionContext tx = componentCopyrightDAO.createTransactionContext()) {
      tx.begin();
      componentCopyrightDAO.update(componentCopyright);
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
      componentCopyrightDAO.insert(componentCopyright);
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
    ComponentIdentifierValidator.validate(componentCopyrightDTO.getComponentIdentifier().toComponentIdentifier());

    for (CopyrightOverrideDTO copyrightOverrideDTO : componentCopyrightDTO.getCopyrightOverrides()) {
      if (copyrightOverrideDTO.getStatus() == null) {
        throw new InvalidComponentCopyrightException("CopyrightOverride must have a status");
      }
    }
  }
}
