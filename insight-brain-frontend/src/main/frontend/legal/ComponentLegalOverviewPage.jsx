/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxBackButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faSitemap } from '@fortawesome/free-solid-svg-icons';
import ComponentOverviewTile from './ComponentOverviewTile';
import LicenseObligationsTile from './LicenseObligationsTile';
import LicenseDetailsTile from './LicenseDetailsTile';
import CopyrightStatementsTile from './CopyrightStatementsTile';
import NoticeTextsTile from './NoticeTextsTile';
import LicenseTextsTile from './LicenseTextsTile';
import LicenseObligationAttributionTileContainer from './LicenseObligationAttributionTileContainer';
import LoadWrapper from '../react/LoadWrapper';
import { componentPropType, licenseLegalMetadataPropType } from './advancedLegalPropTypes';
import { chain, find, flip, groupBy, map, pipe, prop, propEq, toPairs, values, reject } from 'ramda';
import { TEXT_BASED_OBLIGATIONS } from './advancedLegalConstants';

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
    obligationName: obligation.name,
    licenseName,
    texts: obligation.obligationTexts
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

  const licenseObligations = licenseLegalMetadata && getLicenseObligationsByName(
      reject(licenseLegalMetadata => !licenseLegalMetadata.obligations, values(licenseLegalMetadata)));

  const getLicenseNames = effectiveLicenses => map(
      pipe(propEq('licenseId'), flip(find)(licenseLegalMetadata), prop('licenseName')), effectiveLicenses);

  const licenseNames = component && getLicenseNames(component.licenseLegalData.effectiveLicenses);

  const isTextBasedObligation = (licenseObligation) => {
    return TEXT_BASED_OBLIGATIONS.includes(licenseObligation.name);
  };

  const createLicenseObligationAttributionTileContainer = (licenseObligation, index) => {
    return <LicenseObligationAttributionTileContainer key={ index } name={ licenseObligation.name } />;
  };

  return (
    <main className="nx-page-main">
      <LoadWrapper loading={ loading }
                   error={ error }
                   retryHandler={ load }>
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
          <ComponentOverviewTile obligationCount={ licenseObligations && licenseObligations.length }
                                 licenseNames={ licenseNames }
          />
          <LicenseObligationsTile licenseObligations={ licenseObligations } />
          <div id="component-legal-overview-details-right">
            <LicenseDetailsTile licenseNames={ licenseNames }/>
            <CopyrightStatementsTile component={ component }/>
            <NoticeTextsTile component={ component }/>
            <LicenseTextsTile component={ component }/>
            { licenseObligations &&
            licenseObligations.filter(isTextBasedObligation).map(createLicenseObligationAttributionTileContainer) }
          </div>
        </div>
      </LoadWrapper>
    </main>
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
