/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { existsSync, readFileSync, readdirSync, statSync } from 'fs';
import path from 'path';

/**
 * Enforces a single Lucide entry point: only {@code nosc/icons/**} may import
 * {@code lucide-react} directly. Application code imports named icons from
 * {@code MainRoot/nosc/icons} so we get one catalog, consistent sizing/stroke,
 * and predictable bundles (Epic 1.5 / ux-standards icon discipline).
 *
 * The nexus-one bundle has no local icons/ tree yet — it must use nosc/icons too.
 * ESLint {@code no-restricted-imports} is a follow-up; this Jest scan runs in CI today.
 */

/**
 * Recursively collect all *.ts and *.tsx files under `dir`, excluding
 * `node_modules`. Returns absolute paths.
 */
function collectTsFiles(dir: string): string[] {
  if (!existsSync(dir)) {
    return [];
  }
  const out: string[] = [];
  for (const entry of readdirSync(dir)) {
    if (entry === 'node_modules') continue;
    const full = path.join(dir, entry);
    const st = statSync(full);
    if (st.isDirectory()) {
      out.push(...collectTsFiles(full));
    } else if (st.isFile() && /\.(ts|tsx)$/.test(entry)) {
      out.push(full);
    }
  }
  return out;
}

/**
 * Files that must import icons via {@code MainRoot/nosc/icons}, not lucide-react.
 *
 * @param root bundle source root (nosc/ or nexus-one/)
 * @param iconCatalogDirName when set (e.g. {@code icons}), that subtree may import lucide-react
 */
function filesUnderIconRule(root: string, iconCatalogDirName: string | null): string[] {
  const allFiles = collectTsFiles(root);
  if (iconCatalogDirName === null) {
    return allFiles;
  }
  const catalogDir = path.join(root, iconCatalogDirName);
  const catalogPrefix = catalogDir.endsWith(path.sep) ? catalogDir : catalogDir + path.sep;
  return allFiles.filter((file) => !file.startsWith(catalogPrefix));
}

describe('Nexus One UI icon-import discipline (nosc + nexus-one bundles)', () => {
  const frontendRoot = path.resolve(__dirname, '../../../../main/frontend');
  const noscRoot = path.join(frontendRoot, 'nosc');
  const nexusOneRoot = path.join(frontendRoot, 'nexus-one');

  const noscUnderRule = filesUnderIconRule(noscRoot, 'icons');
  const nexusOneUnderRule = filesUnderIconRule(nexusOneRoot, null);
  const filesUnderRule = [...noscUnderRule, ...nexusOneUnderRule];

  const noscAll = collectTsFiles(noscRoot);
  const noscIconsDir = path.join(noscRoot, 'icons');
  const noscIconsPrefix = noscIconsDir.endsWith(path.sep) ? noscIconsDir : noscIconsDir + path.sep;
  const noscCatalogFiles = noscAll.filter((f) => f.startsWith(noscIconsPrefix));

  it('found at least one nosc file outside the icon catalog', () => {
    expect(noscUnderRule.length).toBeGreaterThan(0);
  });

  it('found at least one icon-catalog file (sanity for the nosc exclusion)', () => {
    expect(noscCatalogFiles.length).toBeGreaterThan(0);
  });

  it('found at least one nexus-one file subject to the rule', () => {
    expect(nexusOneUnderRule.length).toBeGreaterThan(0);
  });

  it.each(filesUnderRule)('%s does not import directly from lucide-react', (file) => {
    const content = readFileSync(file, 'utf-8');
    expect(content).not.toMatch(/from\s+['"]lucide-react['"]/);
  });
});
