/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;

import org.apache.commons.lang3.StringUtils;

public class OwnerComponentLicensesDTO
{
  private static final char LICENSES_DELIMITER_CHAR = '\n';

  /** The licenses delimiter character escaped for regular expressions. */
  private static final String LICENSES_DELIMITER_REGEX = "\\" + LICENSES_DELIMITER_CHAR;

  private String ownerId;

  private String hash;

  private String componentIdFormat;

  private String componentIdCoordinatesJson;

  private String licensesString;

  private ComponentIdentifier componentIdentifier;

  public OwnerComponentLicensesDTO(
      String ownerId,
      String hash,
      String componentIdFormat,
      String componentIdCoordinatesJson,
      String licensesString)
  {
    this.ownerId = ownerId;
    this.hash = hash;
    this.componentIdFormat = componentIdFormat;
    this.componentIdCoordinatesJson = componentIdCoordinatesJson;
    this.licensesString = licensesString;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public String getHash() {
    return hash;
  }

  public ComponentIdentifier getComponentIdentifier() {
    if (componentIdentifier == null) {
      componentIdentifier =
          stripEmptyCoordinatesIfNeeded(ComponentIdentifierAdapter.formatAndJsonToComponentIdentifier(componentIdFormat,
              componentIdCoordinatesJson));
    }
    return componentIdentifier;
  }

  public Set<String> getLicenses() {
    if (StringUtils.isEmpty(licensesString)) {
      return Collections.emptySet();
    }

    return new HashSet<>(Arrays.asList(licensesString.split(LICENSES_DELIMITER_REGEX)));
  }

  public void setLicenses(Set<String> licenseIds) {
    licensesString = String.join(String.valueOf(LICENSES_DELIMITER_CHAR), licenseIds);
  }

  /**
   * See CLM-34753
   * The ComponentIdentifier we save to the database originates from the ComponentDetails from HDS
   * which strip out the empty coordinates for conan
   * https://github.com/sonatype/insight-brain/blob/93b43562a4ce96795bc01b9e0c99b838a89be9a0/insight-brain-service/src/main/java/com/sonatype/insight/brain/api/v2/service/legal/ApiLicenseLegalService.java#L851-L854
   * https://github.com/sonatype/hosted-data-services/blob/57787712fb319d1f07a6be78f75a78409aacf5a1/insight-scan-processor/src/main/java/com/sonatype/insight/scan/ComponentDetailsLoader.java#L280-L291
   * https://github.com/sonatype/insight-dto-model/blob/1b69168eecb74481e30a2cd95b4947dc4440284e/com.sonatype.clm.dto.model/src/main/java/com/sonatype/clm/dto/model/component/ComponentIdentifier.java#L726-L738
   * However when loading
   * 1. All application legal obligations
   * https://github.com/sonatype/insight-brain/blob/cf2b94b7ba0fc9ee67238001628b6b096cea50c0/insight-brain-data/src/main/java/com/sonatype/insight/brain/dataaccess/ApplicationComponentLicenseDAO.java#L154
   * 2. All component legal obligations
   * https://github.com/sonatype/insight-brain/blob/cf2b94b7ba0fc9ee67238001628b6b096cea50c0/insight-brain-data/src/main/java/com/sonatype/insight/brain/dataaccess/ApplicationComponentLicenseDAO.java#L98
   * 3. Application legal obligations
   * https://github.com/sonatype/insight-brain/blob/cf2b94b7ba0fc9ee67238001628b6b096cea50c0/insight-brain-data/src/main/java/com/sonatype/insight/brain/dataaccess/ApplicationComponentLicenseDAO.java#L154
   * We load the ComponentIdentifier from the application_component table.
   * This table is populated based on the bom.json
   * which HDS populates with the full coordinates
   * https://github.com/sonatype/hosted-data-services/blob/60d878cb36cefc7a21868f496296c3c62d3a6636/insight-scan-processor/src/main/java/com/sonatype/insight/scan/application/ApplicationScanProcessor.java#L723
   * Note that in the code link above, HDS does not call ComponentIdentifier.ensureComplete for conan, but it turns out
   * the coordinates are already complete i.e. possibly having empty values at this point.
   * So we need to strip the empty coordinates from a conan ComponentIdentifier to query the database correctly
   */
  private ComponentIdentifier stripEmptyCoordinatesIfNeeded(final ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null) {
      return null;
    }
    if (ComponentIdentifier.FORMAT_CONAN.equals(componentIdentifier.getFormat())) {
      return ComponentIdentifier.createConanCoordinates(
          componentIdentifier.get(ComponentIdentifier.CONAN_NAME),
          componentIdentifier.get(ComponentIdentifier.VERSION),
          componentIdentifier.get(ComponentIdentifier.CONAN_OWNER),
          componentIdentifier.get(ComponentIdentifier.CONAN_CHANNEL));
    }
    return componentIdentifier;
  }
}
