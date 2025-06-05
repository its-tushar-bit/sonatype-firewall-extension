/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.component.HashComponentIdentifierDTO;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.json.store.ISODateSerializer;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @since 1.85
 */
public class ApiHashComponentIdentifierDTO
{
  public String hash;

  public String comment;

  @JsonSerialize(using = ISODateSerializer.class)
  public Date createTime;

  public String claimerId;

  public String claimerName;

  public ApiComponentIdentifierDTOV2 componentIdentifier;

  public String packageUrl;

  public ApiHashComponentIdentifierDTO() {
    // for jackson
  }

  public ApiHashComponentIdentifierDTO(HashComponentIdentifier hashComponentIdentifier) {
    hash = hashComponentIdentifier.getHash();
    comment = hashComponentIdentifier.getComment();
    createTime = hashComponentIdentifier.getCreateTime();
    claimerId = hashComponentIdentifier.getClaimerId();
    claimerName = hashComponentIdentifier.getClaimerName();
    componentIdentifier =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(hashComponentIdentifier.getComponentIdentifier());
    packageUrl =
        PackageUrlIdentifier.fromComponentIdentifier(hashComponentIdentifier.getComponentIdentifier()).getPackageUrl();
  }

  public ApiHashComponentIdentifierDTO(HashComponentIdentifierDTO hashComponentIdentifierDTO) {
    hash = hashComponentIdentifierDTO.hash;
    comment = hashComponentIdentifierDTO.comment;
    createTime = hashComponentIdentifierDTO.createTime;
    claimerId = hashComponentIdentifierDTO.claimerId;
    claimerName = hashComponentIdentifierDTO.claimerName;
    componentIdentifier =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(hashComponentIdentifierDTO.componentIdentifier);
    packageUrl =
        PackageUrlIdentifier.fromComponentIdentifier(hashComponentIdentifierDTO.componentIdentifier).getPackageUrl();
  }

  public HashComponentIdentifier toHashComponentIdentifier() {
    ComponentIdentifier componentIdentifier =
        this.componentIdentifier == null ? null : this.componentIdentifier.toComponentIdentifier();
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(hash, componentIdentifier);
    hashComponentIdentifier.setComment(comment);
    hashComponentIdentifier.setCreateTime(createTime);
    hashComponentIdentifier.setClaimerId(claimerId);
    hashComponentIdentifier.setClaimerName(claimerName);
    return hashComponentIdentifier;
  }
}
