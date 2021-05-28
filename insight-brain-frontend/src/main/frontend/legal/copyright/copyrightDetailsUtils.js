/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export const copyrightDetailsStateNameSuffix = '.copyrightDetails';

export const FILE_PATH_PAGE_SIZE = 15;

export function isCopyrightDetailsState(stateName) {
  return stateName.endsWith(copyrightDetailsStateNameSuffix);
}

export function pageOffset(pageNumber) {
  return pageNumber * FILE_PATH_PAGE_SIZE;
}

export function pageRange(pageNumber, filePaths) {
  const first = pageOffset(pageNumber) + 1;
  const length = first + filePaths.length - 1;
  return `${first}  - ${length}`;
}

export function pageCount(totalFilePaths) {
  return Math.ceil(totalFilePaths / FILE_PATH_PAGE_SIZE);
}
