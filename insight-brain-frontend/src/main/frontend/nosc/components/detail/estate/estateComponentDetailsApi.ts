/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getApiV2ComponentDetailsUrl } from 'MainRoot/util/CLMLocation';

/** Wire request for {@code POST /api/v2/components/details} (hash-only). */
export type EstateComponentDetailsRequest = {
  readonly components: ReadonlyArray<{ readonly hash: string }>;
};

export type EstateComponentLicense = {
  readonly licenseId?: string;
  readonly licenseName?: string;
};

export type EstateComponentLicenseData = {
  readonly declaredLicenses?: ReadonlyArray<EstateComponentLicense>;
  readonly observedLicenses?: ReadonlyArray<EstateComponentLicense>;
  readonly effectiveLicenses?: ReadonlyArray<EstateComponentLicense>;
  readonly status?: string;
};

export type EstateComponentSecurityIssue = {
  readonly reference?: string;
  readonly severity?: number;
  readonly threatCategory?: string;
  readonly status?: string;
};

export type EstateComponentIdentifier = {
  readonly format?: string;
  readonly coordinates?: Record<string, unknown>;
};

export type EstateComponentDetails = {
  readonly hash?: string;
  readonly displayName?: string;
  readonly packageUrl?: string;
  readonly format?: string;
  readonly componentIdentifier?: EstateComponentIdentifier;
  readonly matchState?: string;
  readonly licenseData?: EstateComponentLicenseData;
  readonly securityIssues?: ReadonlyArray<EstateComponentSecurityIssue>;
};

type ApiComponentDetailsResult = {
  readonly componentDetails?: ReadonlyArray<{
    readonly component?: {
      readonly hash?: string;
      readonly displayName?: string;
      readonly packageUrl?: string;
      readonly componentIdentifier?: EstateComponentIdentifier;
    };
    readonly matchState?: string;
    readonly licenseData?: EstateComponentLicenseData;
    readonly securityData?: { readonly securityIssues?: ReadonlyArray<EstateComponentSecurityIssue> };
  }>;
};

export function buildEstateComponentDetailsRequest(
  componentHash: string,
): EstateComponentDetailsRequest {
  return { components: [{ hash: componentHash }] };
}

export function mapEstateComponentDetailsResponse(
  data: ApiComponentDetailsResult | null | undefined,
): EstateComponentDetails | null {
  const row = data?.componentDetails?.[0];
  if (!row) {
    return null;
  }
  const component = row.component;
  return {
    hash: component?.hash,
    displayName: component?.displayName,
    packageUrl: component?.packageUrl,
    format: component?.componentIdentifier?.format,
    componentIdentifier: component?.componentIdentifier,
    matchState: row.matchState,
    licenseData: row.licenseData,
    securityIssues: row.securityData?.securityIssues ?? [],
  };
}

export async function fetchEstateComponentDetails(
  componentHash: string,
  signal?: AbortSignal,
): Promise<EstateComponentDetails | null> {
  const { data } = await axios.post<ApiComponentDetailsResult>(
    getApiV2ComponentDetailsUrl(),
    buildEstateComponentDetailsRequest(componentHash),
    { signal },
  );
  return mapEstateComponentDetailsResponse(data);
}
