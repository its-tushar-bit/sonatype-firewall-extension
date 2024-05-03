/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.components;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.sbom.utils.SbomCreationDetails;
import com.sonatype.insight.brain.sbom.utils.SbomCreationDetails.Creator;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import static com.sonatype.insight.brain.sbom.utils.SbomCreationDetails.CreatorType.parseCreatorType;

@Named
@Singleton
public class SbomComponentsService
{
  private static final String cannotFindVersionError = "Cannot find version %s for application with ID %s.";

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  @Inject
  public SbomComponentsService(
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO)
  {
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
  }

  @Authorize(permission = Permission.READ)
  public BomPageMetadataDTO getBomPageMetadata(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String sbomVersion)
  {
    ThirdPartySbomMetadata metadataEntity =
        thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(applicationId, sbomVersion);
    if (metadataEntity == null) {
      throw new NotFoundException(String.format(cannotFindVersionError, sbomVersion, applicationId));
    }

    ThirdPartyScan scanEntity =
        thirdPartyScanDAO.getSingleByThirdPartyFileId(metadataEntity.getThirdPartyFileId());

    return buildSbomMetadataDTO(
        new SbomMetadataDTO(metadataEntity.getSpec(), metadataEntity.getSpecVersion(), metadataEntity.getSpecFormat(),
            metadataEntity.getMetadataJson(), scanEntity.getScanId()));
  }

  private BomPageMetadataDTO buildSbomMetadataDTO(SbomMetadataDTO sbomMetadataDTO) {
    String metadataJson = sbomMetadataDTO.metadataJson;
    List<String> manufacturerList = new ArrayList<>();
    List<String> supplierList = new ArrayList<>();
    List<String> authorList = new ArrayList<>();
    List<String> personList = new ArrayList<>();
    List<String> organizationList = new ArrayList<>();
    String createdAt = "";
    if (metadataJson != null) {
      try {
        SbomCreationDetails creationDetails = JsonUtils.parse(metadataJson, SbomCreationDetails.class);
        if (creationDetails.creators != null) {
          for (Creator creator : creationDetails.creators) {
            switch (parseCreatorType(creator.type)) {
              case Manufacturer:
                if (!organizationList.contains(creator.name)) {
                  manufacturerList.add(creator.name);
                }
                break;
              case Supplier:
                if (!supplierList.contains(creator.name)) {
                  supplierList.add(creator.name);
                }
                break;
              case Author:
                if (!authorList.contains(creator.name)) {
                  authorList.add(creator.name);
                }
                break;
              case Person:
                if (!personList.contains(creator.name)) {
                  personList.add(creator.name);
                }
                break;
              case Organization:
                if (!organizationList.contains(creator.name)) {
                  organizationList.add(creator.name);
                }
                break;
              default:
                break;
            }
          }
        }
        createdAt = creationDetails.created;
      }
      catch (IOException e) {
        throw new IllegalStateException("Can not read metadata json, incorrect format", e);
      }
    }
    return new BomPageMetadataDTO(
        authorList,
        manufacturerList,
        supplierList,
        personList,
        organizationList,
        sbomMetadataDTO.specification,
        sbomMetadataDTO.specVersion,
        sbomMetadataDTO.fileFormat,
        createdAt,
        sbomMetadataDTO.scanId
    );
  }
}
