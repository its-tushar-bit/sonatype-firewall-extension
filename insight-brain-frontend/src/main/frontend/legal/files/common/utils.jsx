/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxFontAwesomeIcon, NxTextLink } from '@sonatype/react-shared-components';
import { faAngleRight } from '@fortawesome/pro-solid-svg-icons';
import React from 'react';
import { assoc, keys, reduce } from 'ramda';

export const createLegalFileTileItem = (legalFileType, object, index, $state, targetStateName, routeParams) => (
  <section id={`${legalFileType}-section-${index}`} key={index} className="legal-file">
    <div className="legal-file-section-header">
      <span className="legal-file-path">{object.relPath}</span>
    </div>
    <blockquote id={`${legalFileType}-text-${index}`} className="nx-blockquote">
      <div className="legal-file-content">{object.originalContent}</div>
    </blockquote>
    <div id="legal-file-section-view-more-details">
      <NxTextLink href={$state.href(targetStateName, routeParams)}>
        <span>View More Details</span>
        <NxFontAwesomeIcon icon={faAngleRight} />
      </NxTextLink>
    </div>
  </section>
);

const ifExistsElseEmpty = (element, func) => (element ? func() : '');

export const attributionStatus = (item) =>
  ifExistsElseEmpty(item, () => (item.status === 'enabled' ? 'Included' : 'Excluded'));

export const legalFileSource = (item) =>
  ifExistsElseEmpty(item, () => (item.originalContentHash ? 'Sonatype Scan' : 'Manually added'));

export const renameKeys = (keysMap, obj) =>
  reduce((acc, key) => assoc(keysMap[key] || key, obj[key], acc), {}, keys(obj));
