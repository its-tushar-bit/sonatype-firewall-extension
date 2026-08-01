/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { ComingSoonModuleSlug } from 'MainRoot/nosc/comingSoon/comingSoonModules';

/**
 * Slugs whose Coming Soon entry is a native Classic embed in the Preview shell.
 * Prefer {@link usesEmbeddedHrefPrimary} when choosing a primary URL — that helper
 * excludes slugs in {@link CLEAN_PATH_OWNED_ELSEWHERE} (today: {@code repositories},
 * whose clean path is already owned by {@code nexusOneRepositories}).
 */
export const NATIVE_CLASSIC_EMBED_SLUGS = [
  'success-metrics',
  'api',
  'repositories',
  'legal',
  'orgs-and-policies',
  'reports',
  // App Detail Quick Actions / Coming Soon bookmarks → in-shell RSC (not stubs).
  'policies',
  'source-control',
  'waiver-requests',
] as const satisfies readonly ComingSoonModuleSlug[];

export type NativeClassicEmbedSlug = (typeof NATIVE_CLASSIC_EMBED_SLUGS)[number];

/**
 * Embed slugs whose clean {@code /${slug}} path is already owned by another route.
 * Those embeds keep {@code /coming-soon/${slug}} as their primary Coming Soon URL.
 */
/**
 * {@code repositories} — owned by {@code nexusOneRepositories}.
 * {@code legal} — owned by {@code nexusOneLegal} (LEGAL_VIOLATION license-risk triage, CLM-43207).
 * Classic ALP dashboard remains at {@code /legal/applicationsDashboard}.
 * {@code policies} — Classic href collides with {@code orgs-and-policies}; keep
 * {@code /coming-soon/policies} as the entry so reverse-map prefers {@code /orgs-and-policies}.
 * {@code waiver-requests} — redirects to {@code nexusOneWaivers} ({@code /waivers}).
 */
const CLEAN_PATH_OWNED_ELSEWHERE_SLUGS = [
  'repositories',
  'legal',
  'policies',
  'waiver-requests',
] as const satisfies readonly NativeClassicEmbedSlug[];

export const CLEAN_PATH_OWNED_ELSEWHERE: ReadonlySet<ComingSoonModuleSlug> = new Set(
  CLEAN_PATH_OWNED_ELSEWHERE_SLUGS,
);

/** Slugs allowed by {@link embeddedHref} — native embeds that own a clean primary URL. */
export type EmbeddedHrefPrimarySlug = Exclude<
  NativeClassicEmbedSlug,
  (typeof CLEAN_PATH_OWNED_ELSEWHERE_SLUGS)[number]
>;

export function isNativeClassicEmbedSlug(slug: ComingSoonModuleSlug): slug is NativeClassicEmbedSlug {
  return (NATIVE_CLASSIC_EMBED_SLUGS as readonly string[]).includes(slug);
}

/**
 * Whether the Coming Soon state for this embed owns a clean {@code /${slug}} primary URL.
 * False when the clean path is listed in {@link CLEAN_PATH_OWNED_ELSEWHERE}.
 * Acts as a type predicate so callers can pass {@code slug} to {@link embeddedHref} without a cast.
 */
export function usesEmbeddedHrefPrimary(
  slug: ComingSoonModuleSlug,
): slug is EmbeddedHrefPrimarySlug {
  return isNativeClassicEmbedSlug(slug) && !CLEAN_PATH_OWNED_ELSEWHERE.has(slug);
}

/**
 * In-hash path for a native Classic embed that owns a clean {@code /${slug}} primary URL
 * (no {@code /coming-soon/} prefix).
 *
 * Only for slugs where {@link usesEmbeddedHrefPrimary} is true. Throws for Coming Soon stubs
 * and {@link CLEAN_PATH_OWNED_ELSEWHERE} slugs so untyped JS callers fail loudly.
 */
export function embeddedHref(slug: ComingSoonModuleSlug): string {
  if (!usesEmbeddedHrefPrimary(slug)) {
    throw new Error(`embeddedHref requires a clean-primary embed slug, got: ${slug}`);
  }
  return `/${slug}`;
}
