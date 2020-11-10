/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxBackButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faSitemap } from '@fortawesome/free-solid-svg-icons';
import MaximizedContainer from '../react/MaximizedContainer';
import ComponentOverviewTile from './ComponentOverviewTile';
import LicenseObligationsTile from './LicenseObligationsTile';
import LicenseDetailsTile from './LicenseDetailsTile';
import CopyrightStatementsTile from './CopyrightStatementsTile';
import NoticeTextsTile from './NoticeTextsTile';
import LicenseTextsTile from './LicenseTextsTile';

export default function ComponentLegalOverviewPage(props) {
  const {
    components,
    loadComponentDetails
  } = props;

  useEffect(() => { loadComponentDetails('an id'); }, []);

  return (
    <MaximizedContainer className="nx-page-content">
      <main className="nx-page-main">
        <NxBackButton href="#" />
        <div className="nx-page-title">
          <h1 className="nx-h1">com.google.greatgooglymoogly : jsr305 : 3.0.2</h1>
          <div className="nx-page-title__description">
            <NxFontAwesomeIcon icon = { faSitemap } />
            <span>Root Organization</span>
          </div>
        </div>
        <div id="component-legal-overview-details">
          <ComponentOverviewTile { ...components } />
          <LicenseObligationsTile />
          <div id="component-legal-overview-details-right">
            <LicenseDetailsTile />
            <CopyrightStatementsTile />
            <NoticeTextsTile />
            <LicenseTextsTile />
          </div>
        </div>
      </main>
    </MaximizedContainer>
  );
}

ComponentLegalOverviewPage.propTypes = {
  components: PropTypes.any,
  loadComponentDetails: PropTypes.func
};
