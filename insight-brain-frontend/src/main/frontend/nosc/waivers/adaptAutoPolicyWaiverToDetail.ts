/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ApiAutoPolicyWaiverDTO, PolicyWaiverDetailDTO } from './waiverTypes';

/**
 * Maps an auto-waiver's own API shape onto {@link PolicyWaiverDetailDTO} so
 * `WaiverDetailPage` can render it through the same header/meta/card layout it
 * already uses for manual waivers. Auto-waivers apply broadly (by scope, not
 * to one violation), so there is no policy name, constraint blurb, comment, or
 * vulnerability link to carry over — those rows are simply absent/omitted in
 * the resulting DTO, and the page's existing fallbacks (`'Policy waiver'`,
 * `'—'`, `'No additional comments'`) render in their place.
 */
export function adaptAutoPolicyWaiverToDetail(dto: ApiAutoPolicyWaiverDTO): PolicyWaiverDetailDTO {
  return {
    id: dto.autoPolicyWaiverId,
    ownerId: dto.ownerId,
    ownerType: dto.ownerType,
    ownerName: dto.ownerName,
    // Unused by WaiverDetailPage (it reads ownerName/scopeOwnerName directly via
    // formatWaiverScopeLabel) — populated only to satisfy PolicyWaiverDTO's required field.
    scope: dto.ownerName ?? '',
    // threatLevel is omitted by the backend's @JsonInclude(NON_EMPTY) when 0.
    threatLevel: dto.threatLevel ?? 0,
    createTime: dto.createTime,
    creatorName: dto.creatorName,
    isAutoWaiver: true,
  };
}
