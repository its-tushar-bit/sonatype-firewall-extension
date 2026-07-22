/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export type ViolationDetailTabId = 'overview' | 'vulnerability' | 'waivers';

export interface ViolationPolicyOwnerDTO {
  readonly ownerName?: string;
  readonly ownerType?: string;
  readonly ownerId?: string;
  readonly ownerPublicId?: string;
}

export interface ComponentIdentifierDTO {
  readonly format?: string;
  readonly coordinates?: Record<string, string>;
}

export interface ComponentDisplayNamePartDTO {
  readonly field?: string;
  readonly value?: string;
}

export interface ComponentDisplayNameDTO {
  readonly parts?: ReadonlyArray<ComponentDisplayNamePartDTO>;
}

export interface ViolationReasonReferenceDTO {
  readonly type: string;
  readonly value: string;
}

export interface ViolationReasonDTO {
  readonly reason: string;
  readonly reference?: ViolationReasonReferenceDTO;
}

export interface ConstraintViolationDTO {
  readonly constraintName: string;
  readonly reasons?: ReadonlyArray<ViolationReasonDTO>;
}

export interface ViolationStageDataDTO {
  readonly mostRecentEvaluationTime: string;
  readonly mostRecentScanId: string;
  readonly actionTypeId?: 'fail' | 'warn' | null;
}

export interface ViolationDetailsDTO {
  readonly policyViolationId: string;
  readonly policyName: string;
  readonly policyThreatCategory: string;
  readonly policyOwner: ViolationPolicyOwnerDTO;
  readonly threatLevel: number;
  readonly openTime: string;
  readonly stageData: Record<string, ViolationStageDataDTO>;
  readonly applicationPublicId: string;
  readonly organizationName: string;
  readonly applicationName: string;
  readonly componentIdentifier?: ComponentIdentifierDTO | string;
  readonly identificationSource?: string;
  readonly displayName?: ComponentDisplayNameDTO | string | null;
  readonly filenames?: ReadonlyArray<string>;
  readonly hash?: string;
  readonly constraintViolations?: ReadonlyArray<ConstraintViolationDTO>;
  readonly reachabilityStatus?: string;
  readonly waived?: boolean;
}

export interface ApplicableWaiverDTO {
  readonly policyWaiverId: string;
  readonly comment?: string;
  readonly expiryTime?: string | null;
  readonly scopeOwnerType: string;
  readonly scopeOwnerId: string;
  readonly scopeOwnerName: string;
  readonly hash?: string;
  readonly policyId: string;
}

export interface ApplicableWaiversDTO {
  readonly activeWaivers: ReadonlyArray<ApplicableWaiverDTO>;
  readonly expiredWaivers: ReadonlyArray<ApplicableWaiverDTO>;
}

export interface VulnerabilitySeverityDTO {
  readonly score?: number;
  readonly source?: string;
  readonly vector?: string;
}

export interface VulnerabilitySummaryDTO {
  readonly identifier?: string;
  readonly mainSeverity?: VulnerabilitySeverityDTO;
  readonly description?: string;
  readonly hasEditIqPermission?: boolean;
}
