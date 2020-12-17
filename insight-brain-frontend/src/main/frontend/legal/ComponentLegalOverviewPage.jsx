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
import LoadWrapper from '../react/LoadWrapper';
import { componentPropType, licenseLegalMetadataPropType } from './advancedLegalPropTypes';
import { chain, groupBy, map, pipe, prop, toPairs, values } from 'ramda';

export default function ComponentLegalOverviewPage(props) {
  const {
    component,
    licenseLegalMetadata,
    loading,
    error,
    hash,
    loadComponent
  } = props;

  function load() {
    if (hash) {
      loadComponent('organization', 'ROOT_ORGANIZATION_ID', hash);
    }
  }

  useEffect(load, [hash]);

  const mapObligationsToLicenseAndTexts = chain(({ licenseName, obligations }) => map(obligation => ({
    obligationName: obligation.licenseObligation.name,
    licenseName,
    texts: obligation.licenseObligation.obligationTexts
  }), obligations));

  const groupObligationsByLicense = map(([obligationName, licenses]) => ({
    name: obligationName,
    licenses: map(({ licenseName, texts }) => ({ name: licenseName, texts }), licenses)
  }));

  const getLicenseObligationsByName = pipe(
      mapObligationsToLicenseAndTexts,
      groupBy(prop('obligationName')),
      toPairs,
      groupObligationsByLicense
  );

  const licenseObligations = getLicenseObligationsByName(values(licenseLegalMetadata));

  return (
    <LoadWrapper loading={ loading }
                 error={ error }
                 retryHandler={ load }>
      <MaximizedContainer className="nx-page-content">
        <main className="nx-page-main">
          <NxBackButton href="#" />
          <div className="nx-page-title">
            <h1 className="nx-h1">
              { component && component.displayName }
            </h1>
            <div className="nx-page-title__description">
              <NxFontAwesomeIcon icon = { faSitemap } />
              <span>Root Organization</span>
            </div>
          </div>
          <div id="component-legal-overview-details">
            <ComponentOverviewTile component={ component } obligationCount={ licenseObligations.length }
            />
            <LicenseObligationsTile licenseObligations={ licenseObligations } />
            <div id="component-legal-overview-details-right">
              <LicenseDetailsTile component={ component }/>
              <CopyrightStatementsTile component={ component }/>
              <NoticeTextsTile component={ component }/>
              <LicenseTextsTile component={ component }/>
            </div>
          </div>
        </main>
      </MaximizedContainer>
    </LoadWrapper>
  );
}

ComponentLegalOverviewPage.propTypes = {
  component: componentPropType,
  loading: PropTypes.bool,
  error: PropTypes.string,
  hash: PropTypes.string.isRequired,
  licenseLegalMetadata: licenseLegalMetadataPropType,
  loadComponent: PropTypes.func
};
