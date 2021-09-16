/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { attributionStatus, legalFileSource } from './utils';
import { getRelevantScope } from '../../legalUtility';
import React from 'react';

export const LegalFileOverviewHeader = (legalFileElement, availableScopes, lastModified, title, scopeOwnerId) => (
  <section id="legal-file-details-tile" className="nx-tile nx-viewport-sized__container">
    <header className="nx-tile-header">
      <div className="nx-tile-header__title">
        <h2 className="nx-h2">Overview</h2>
      </div>
    </header>
    <div id="legal-file-overview-tile" className="nx-tile-content nx-viewport-sized__container">
      <dl className="nx-read-only legal-file-overview">
        <div className="legal-file-overview-item">
          <dt className="nx-read-only__label">Attribution Report status</dt>
          <dd className="nx-read-only__data">{attributionStatus(legalFileElement)}</dd>
        </div>
        <div className="legal-file-overview-item">
          <dt className="nx-read-only__label">Scope</dt>
          <dd className="nx-read-only__data">{getRelevantScope(scopeOwnerId, availableScopes).name}</dd>
        </div>
        <div className="legal-file-overview-item">
          <dt className="nx-read-only__label">Source</dt>
          <dd className="nx-read-only__data">{legalFileSource(legalFileElement)}</dd>
        </div>
        <div className="legal-file-overview-item">
          <dt className="nx-read-only__label">Last Modified</dt>
          <dd className="nx-read-only__data">{lastModified}</dd>
        </div>
        <div className="nx-read-only legal-file-overview-text nx-viewport-sized__container">
          <dt className="nx-read-only__label">{`${title} Text`}</dt>
          <dd className="nx-read-only__data">
            {legalFileElement && legalFileElement.relPath && `${title} Text found in `}
            <span className="legal-file-included-in-detail-filepath">
              {legalFileElement && legalFileElement.relPath}
            </span>
          </dd>
          <dd className="nx-read-only__data nx-viewport-sized__container">
            <blockquote
              className="nx-blockquote nx-scrollable legal-file-preformatted nx-viewport-sized__scrollable"
              id="legal-file-text-quote"
            >
              {legalFileElement && legalFileElement.content}
            </blockquote>
          </dd>
        </div>
      </dl>
    </div>
  </section>
);
