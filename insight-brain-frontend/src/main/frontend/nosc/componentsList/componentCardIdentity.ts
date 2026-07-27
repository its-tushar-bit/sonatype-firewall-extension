/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ComponentListRow } from 'MainRoot/nosc/componentsList/componentListTypes';

export type ComponentCardIdentity = {
  /** Martha primary line — typically {@code name@version}. */
  readonly title: string;
  /** Full coordinate / package URL shown under the title when distinct from {@link title}. */
  readonly coordinate?: string;
};

function looksLikeVersion(value: string): boolean {
  // Catalog subtitles are usually bare versions; reject multi-word prose.
  if (!value || /\s/.test(value)) return false;
  return /^v?\d/.test(value) || value.includes('.') || value.includes('-');
}

/** IQ local display names often look like {@code group : artifact : version}. */
function parseColonCoordinate(value: string): { readonly name: string; readonly version?: string } | null {
  const parts = value.split(' : ').map((part) => part.trim()).filter(Boolean);
  if (parts.length < 2) return null;
  const version = parts[parts.length - 1];
  if (!looksLikeVersion(version)) return null;
  const name = parts.length >= 3 ? parts[parts.length - 2] : parts[0];
  return { name, version };
}

/**
 * Derive Martha-style {@code name@version} + full coordinate from catalog/local list fields.
 * Catalog rows: title=name, subtitle=version, id=coordinate. Local rows often use
 * {@code group : name : version} as the catalog id/title.
 */
export function componentCardIdentity(component: ComponentListRow): ComponentCardIdentity {
  const name = component.name.trim() || component.id.trim();
  const id = component.id.trim();
  const subtitle = component.subtitle?.trim();
  const versionFromSubtitle = subtitle && looksLikeVersion(subtitle) ? subtitle : undefined;
  const colon = parseColonCoordinate(name) || parseColonCoordinate(id);

  let version = versionFromSubtitle ?? colon?.version;
  if (!version) {
    const at = name.lastIndexOf('@');
    if (at > 0) version = name.slice(at + 1);
  }
  if (!version) {
    const at = id.lastIndexOf('@');
    if (at > 0) version = id.slice(at + 1);
  }

  const baseName = colon?.name
    ? colon.name
    : name.lastIndexOf('@') > 0
      ? name.slice(0, name.lastIndexOf('@'))
      : name;
  // Prefer name@version when we have a version and the name is not already versioned.
  // Use lastIndexOf('@') > 0 so a leading scoped-package '@' (e.g. @babel/core) is not
  // mistaken for an embedded version separator.
  const title =
    version && name.lastIndexOf('@') <= 0
      ? `${baseName}@${version}`
      : name;

  const coordinateCandidates = [
    // Prefer the full local/catalog coordinate string when it differs from the short title.
    id && id !== title ? id : undefined,
    name !== title && name !== id ? name : undefined,
    subtitle && subtitle !== versionFromSubtitle ? subtitle : undefined,
    component.ecosystem
      ? `${component.ecosystem}:${baseName}${version ? `@${version}` : ''}`
      : undefined,
  ].filter((value): value is string => Boolean(value && value !== title));

  return {
    title,
    coordinate: coordinateCandidates[0],
  };
}
