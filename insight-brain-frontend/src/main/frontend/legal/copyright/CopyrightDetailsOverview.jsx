/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  availableScopesPropType,
  componentCopyrightDetailsPropType,
  componentPropType,
} from '../advancedLegalPropTypes';
import React from 'react';
import { timeAgo } from '../../utilAngular/CommonServices';
import { getRelevantScope } from '../legalUtility';

export default function CopyrightDetailsOverview(props) {
  const { availableScopes, component, componentCopyrightDetails } = props;

  const ifExistsElseEmpty = (element, func) => (element ? func() : '');

  const attributionStatus = (item) =>
    ifExistsElseEmpty(item, () => (item.status === 'enabled' ? 'Included' : 'Excluded'));

  const copyrightSource = (item) =>
    ifExistsElseEmpty(item, () => (item.originalContentHash ? 'Sonatype Scan' : 'Manually added'));

  const copyrightModification = () => {
    const licenseLegalData = component && component.licenseLegalData;
    if (licenseLegalData && licenseLegalData.componentCopyrightLastUpdatedAt) {
      const age = timeAgo(licenseLegalData.componentCopyrightLastUpdatedAt);
      return `${age.age} ${age.qualifier} by ${licenseLegalData.componentCopyrightLastUpdatedByUsername || 'N/A'}`;
    } else {
      return 'N/A';
    }
  };

  const scopeName = () => {
    const scopeOwnerId =
      (component && component.licenseLegalData && component.licenseLegalData.componentCopyrightScopeOwnerId) ||
      'ROOT_ORGANIZATION_ID';
    return getRelevantScope(scopeOwnerId, availableScopes).name;
  };

  return (
    <section id="copyright-details-tile" className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Overview</h2>
        </div>
      </header>
      <div id="copyright-overview-tile" className="nx-tile-content">
        <dl className="nx-read-only nx-read-only--grid copyright-overview">
          <div className="nx-read-only__item copyright-overview-item">
            <dt className="nx-read-only__label">Attribution Report Status</dt>
            <dd className="nx-read-only__data">{attributionStatus(componentCopyrightDetails.selectedCopyright)}</dd>
          </div>
          <div className="nx-read-only__item copyright-overview-item">
            <dt className="nx-read-only__label">Scope</dt>
            <dd className="nx-read-only__data">{scopeName()}</dd>
          </div>
          <div className="nx-read-only__item copyright-overview-item">
            <dt className="nx-read-only__label">Source</dt>
            <dd className="nx-read-only__data">{copyrightSource(componentCopyrightDetails.selectedCopyright)}</dd>
          </div>
          <div className="nx-read-only__item copyright-overview-item">
            <dt className="nx-read-only__label">Last Modified</dt>
            <dd className="nx-read-only__data">{copyrightModification()}</dd>
          </div>
          <div className="nx-read-only__item copyright-overview-text">
            <dt className="nx-read-only__label">Copyright Text</dt>
            <dd className="nx-read-only__data">
              {componentCopyrightDetails.selectedCopyright && componentCopyrightDetails.selectedCopyright.content}
            </dd>
          </div>
        </dl>
      </div>
    </section>
  );
}

CopyrightDetailsOverview.propTypes = {
  availableScopes: availableScopesPropType,
  component: componentPropType,
  componentCopyrightDetails: componentCopyrightDetailsPropType,
};
