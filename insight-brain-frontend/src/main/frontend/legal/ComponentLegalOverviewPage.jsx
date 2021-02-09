/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxBackButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faGlobe, faSitemap, faTerminal } from '@fortawesome/free-solid-svg-icons';
import ComponentOverviewTile from './ComponentOverviewTile';
import LicenseObligationsTile from './LicenseObligationsTile';
import LicenseDetailsTile from './LicenseDetailsTile';
import CopyrightStatementsTile from './CopyrightStatementsTile';
import NoticeTextsTile from './NoticeTextsTile';
import LicenseTextsTile from './LicenseTextsTile';
import LicenseObligationAttributionTileContainer from './LicenseObligationAttributionTileContainer';
import LoadWrapper from '../react/LoadWrapper';
import {
  componentPropType,
  licenseLegalMetadataPropType,
  licenseObligationsPropType,
  availableScopesPropType
} from './advancedLegalPropTypes';
import { chain, find, flip, groupBy, map, pipe, prop, propEq, toPairs, values, reject } from 'ramda';
import { TEXT_BASED_OBLIGATIONS } from './advancedLegalConstants';

export default function ComponentLegalOverviewPage(props) {
  const {
    component,
    licenseLegalMetadata,
    obligations,
    loading,
    error,
    organizationId,
    applicationPublicId,
    hash,
    loadComponent,
    loadAvailableScopes,
    availableScopes
  } = props;

  function load() {
    if (hash) {
      if (organizationId) {
        loadComponent('organization', organizationId, hash);
        loadAvailableScopes('organization', organizationId);
      }
      else if (applicationPublicId) {
        loadComponent('application', applicationPublicId, hash);
        loadAvailableScopes('application', applicationPublicId);
      }
      else {
        loadComponent('organization', 'ROOT_ORGANIZATION_ID', hash);
        loadAvailableScopes('organization', 'ROOT_ORGANIZATION_ID');
      }
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

  const mergeByName = (array1, array2) =>
    array1.map(itm => ({
      ...array2.find((item) => (item.name === itm.name) && item),
      ...itm
    }));

  const licenseObligations = licenseLegalMetadata && obligations && mergeByName(getLicenseObligationsByName(
      reject(licenseLegalMetadata => !licenseLegalMetadata.obligations, values(licenseLegalMetadata))), obligations);

  const getLicenseNames = effectiveLicenses => map(
      pipe(propEq('licenseId'), flip(find)(licenseLegalMetadata), prop('licenseName')), effectiveLicenses);

  const licenseNames = component && getLicenseNames(component.licenseLegalData.effectiveLicenses);

  const isTextBasedObligation = (licenseObligation) => {
    return TEXT_BASED_OBLIGATIONS.includes(licenseObligation.name);
  };

  const createLicenseObligationAttributionTileContainer = (licenseObligation, index) => {
    return <LicenseObligationAttributionTileContainer key={ index }
                                                      name={ licenseObligation.name }
                                                      attributionText={ licenseObligation.attributions.length > 0 ?
                                                        licenseObligation.attributions[0].content : '' }
                                                      obligationFulfilled={ licenseObligation.status === 'FULFILLED' }
                                                      availableScopes={ availableScopes }
    />;
  };

  const createSubtitle = () => {
    let availableScopeValuesReversed = availableScopes && availableScopes.values && [...availableScopes.values] || [];
    availableScopeValuesReversed.reverse();
    return (
      <div className="nx-page-title__description">
        { availableScopeValuesReversed.map((availableScope, index) => {
          return <span key={ index } className="iq-violation-details__subtitle-part">
            <NxFontAwesomeIcon
                icon={ availableScope.id === 'ROOT_ORGANIZATION_ID' ? faGlobe : availableScope.type ===
                'organization' ? faSitemap : faTerminal }/>
            <span>{ availableScope.name }</span>
          </span>;
        }) }
      </div>
    );
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
          { createSubtitle() }
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
  organizationId: PropTypes.string,
  applicationPublicId: PropTypes.string,
  hash: PropTypes.string,
  licenseLegalMetadata: licenseLegalMetadataPropType,
  obligations: licenseObligationsPropType,
  loadComponent: PropTypes.func,
  loadAvailableScopes: PropTypes.func,
  availableScopes: availableScopesPropType
};
