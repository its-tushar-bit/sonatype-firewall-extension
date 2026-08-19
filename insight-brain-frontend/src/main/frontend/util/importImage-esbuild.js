/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Esbuild-compatible replacement for RSC's importImage utility.
 *
 * RSC's importImage uses dynamic require() which esbuild cannot handle.
 * This shim uses static imports so esbuild's file loader can resolve
 * each asset at build time, returning the correct output path.
 */
import sonatypeHeader from '@sonatype/react-shared-components/assets/img/sonatype-header.svg';
import sonatypeHeaderDark from '@sonatype/react-shared-components/assets/img/sonatype-header-dark-mode.svg';
import logoWithHexagon from '@sonatype/react-shared-components/assets/img/sonatype-logo-with-hexagon.png';
import logoWithHexagonDark from '@sonatype/react-shared-components/assets/img/sonatype-logo-with-hexagon-dark-mode.png';

const images = {
  'sonatype-header.svg': sonatypeHeader,
  'sonatype-header-dark-mode.svg': sonatypeHeaderDark,
  'sonatype-logo-with-hexagon.png': logoWithHexagon,
  'sonatype-logo-with-hexagon-dark-mode.png': logoWithHexagonDark,
};

export default function importImage(basename) {
  const result = images[basename];
  if (result !== undefined) {
    return result;
  }
  console.warn(`importImage: unknown image "${basename}"`);
  return '';
}
