/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getApiV2ComponentDetailsUrl, getApiV2ComponentVersionsUrl } from 'MainRoot/util/CLMLocation';

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

export type EstateComponentVersionRow = {
  readonly version: string;
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

type EstateComponentVersionsRequest = {
  readonly packageUrl?: string;
  readonly componentIdentifier?: EstateComponentIdentifier;
};

type EstateComponentVersionDetailsRequest = {
  readonly components: ReadonlyArray<{
    readonly packageUrl?: string;
    readonly componentIdentifier?: EstateComponentIdentifier;
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

export function buildEstateComponentVersionsRequest(details: EstateComponentDetails): EstateComponentVersionsRequest | null {
  if (details.packageUrl) {
    return { packageUrl: details.packageUrl };
  }
  if (details.componentIdentifier) {
    return { componentIdentifier: details.componentIdentifier };
  }
  return null;
}

function replacePackageUrlVersion(packageUrl: string, version: string): string | null {
  const atIndex = packageUrl.lastIndexOf('@');
  if (atIndex < 0) {
    return null;
  }
  const queryIndex = packageUrl.indexOf('?', atIndex);
  const suffix = queryIndex < 0 ? '' : packageUrl.slice(queryIndex);
  return `${packageUrl.slice(0, atIndex + 1)}${version}${suffix}`;
}

function replaceComponentIdentifierVersion(
  componentIdentifier: EstateComponentIdentifier,
  version: string,
): EstateComponentIdentifier {
  return {
    ...componentIdentifier,
    coordinates: {
      ...(componentIdentifier.coordinates ?? {}),
      version,
    },
  };
}

export function buildEstateComponentVersionDetailsRequest(
  details: EstateComponentDetails,
  version: string,
): EstateComponentVersionDetailsRequest | null {
  if (details.packageUrl) {
    const packageUrl = replacePackageUrlVersion(details.packageUrl, version);
    return packageUrl ? { components: [{ packageUrl }] } : null;
  }
  if (details.componentIdentifier) {
    return { components: [{ componentIdentifier: replaceComponentIdentifierVersion(details.componentIdentifier, version) }] };
  }
  return null;
}

export function mapEstateComponentVersionHash(detailsData: ApiComponentDetailsResult | null | undefined): string | null {
  return detailsData?.componentDetails?.find((row) => row.component?.hash)?.component?.hash ?? null;
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

export async function fetchEstateComponentVersionRows(
  details: EstateComponentDetails,
  signal?: AbortSignal,
): Promise<ReadonlyArray<EstateComponentVersionRow>> {
  const versionsRequest = buildEstateComponentVersionsRequest(details);
  if (!versionsRequest) {
    return [];
  }

  const { data: versions } = await axios.post<ReadonlyArray<string> | null>(
    getApiV2ComponentVersionsUrl(),
    versionsRequest,
    {
      signal,
    }
  );

  if (!Array.isArray(versions)) {
    return [];
  }

  return versions.map((version) => ({ version }));
}

export async function resolveEstateComponentVersionHash(
  details: EstateComponentDetails,
  version: string,
  signal?: AbortSignal,
): Promise<string | null> {
  const versionDetailsRequest = buildEstateComponentVersionDetailsRequest(details, version);
  if (!versionDetailsRequest) {
    return null;
  }

  const { data: versionDetails } = await axios.post<ApiComponentDetailsResult>(
    getApiV2ComponentDetailsUrl(),
    versionDetailsRequest,
    { signal },
  );
  return mapEstateComponentVersionHash(versionDetails);
}
