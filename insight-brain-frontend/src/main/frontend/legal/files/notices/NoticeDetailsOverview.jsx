/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  availableScopesPropType,
  componentNoticeDetailsPropType,
  componentPropType,
} from '../../advancedLegalPropTypes';
import React from 'react';
import { timeAgo } from '../../../util/CommonServices';
import { scopeName, attributionStatus, legalSource } from '../../legalUtility';
import * as PropTypes from 'prop-types';

export default function NoticeDetailsOverview(props) {
  const { availableScopes, componentNoticeDetails, component, loading, error } = props;

  const licenseLegalData = component && component.licenseLegalData;

  const noticeModification = () => {
    if (licenseLegalData && licenseLegalData.componentNoticesLastUpdatedAt) {
      let age = timeAgo(licenseLegalData.componentNoticesLastUpdatedAt);
      return `${age.age} ${age.qualifier} by ${licenseLegalData.componentNoticesLastUpdatedByUsername}`;
    } else {
      return 'N/A';
    }
  };

  return loading || error ? null : (
    <section id="notice-details-tile" className="nx-tile nx-viewport-sized__container">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Overview</h2>
        </div>
      </header>
      <div id="notice-overview-tile" className="nx-tile-content nx-viewport-sized__container">
        <dl className="nx-read-only notice-overview">
          <div className="notice-overview-item">
            <dt className="nx-read-only__label">Attribution Report status</dt>
            <dd className="nx-read-only__data">{attributionStatus(componentNoticeDetails.selectedNotice)}</dd>
          </div>
          <div className="notice-overview-item">
            <dt className="nx-read-only__label">Scope</dt>
            <dd className="nx-read-only__data">{scopeName(availableScopes)}</dd>
          </div>
          <div className="notice-overview-item">
            <dt className="nx-read-only__label">Source</dt>
            <dd className="nx-read-only__data">{legalSource(componentNoticeDetails.selectedNotice)}</dd>
          </div>
          <div className="notice-overview-item">
            <dt className="nx-read-only__label">Last Modified</dt>
            <dd className="nx-read-only__data">{noticeModification()}</dd>
          </div>
          <div className="nx-read-only notice-overview-text nx-viewport-sized__container">
            <dt className="nx-read-only__label">Notice Text</dt>
            <dd className="nx-read-only__data">
              {componentNoticeDetails.selectedNotice &&
                componentNoticeDetails.selectedNotice.relPath &&
                'Notice Text found in '}
              <span className="notice-included-in-detail-filepath">
                {componentNoticeDetails.selectedNotice && componentNoticeDetails.selectedNotice.relPath}
              </span>
            </dd>
            <dd className="nx-read-only__data nx-viewport-sized__container">
              <blockquote
                className="nx-blockquote nx-scrollable notice-preformatted nx-viewport-sized__scrollable"
                id="notice-text-quote"
              >
                {componentNoticeDetails.selectedNotice && componentNoticeDetails.selectedNotice.content}
              </blockquote>
            </dd>
          </div>
        </dl>
      </div>
    </section>
  );
}

NoticeDetailsOverview.propTypes = {
  loading: PropTypes.bool,
  error: PropTypes.string,
  availableScopes: availableScopesPropType,
  componentNoticeDetails: componentNoticeDetailsPropType,
  component: componentPropType,
};
