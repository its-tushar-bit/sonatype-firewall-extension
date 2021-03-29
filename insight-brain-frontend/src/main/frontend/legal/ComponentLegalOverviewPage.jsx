/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, {useEffect} from 'react';
import * as PropTypes from 'prop-types';
import {NxBackButton, NxFontAwesomeIcon} from '@sonatype/react-shared-components';
import {faGlobe, faSitemap, faTerminal} from '@fortawesome/free-solid-svg-icons';
import ComponentOverviewTile from './ComponentOverviewTile';
import LicenseDetailsTile from './LicenseDetailsTile';
import CopyrightStatementsTile from './copyright/CopyrightStatementsTile';
import LicenseObligationAttributionTileContainer from './LicenseObligationAttributionTileContainer';
import LoadWrapper from '../react/LoadWrapper';
import {
  availableScopesPropType,
  componentPropType,
  licenseLegalMetadataPropType,
  licenseObligationsPropType
} from './advancedLegalPropTypes';
import {find, flip, map, pipe, prop, propEq} from 'ramda';
import {TEXT_BASED_OBLIGATIONS} from './advancedLegalConstants';
import LicenseObligationsTileContainer from './LicenseObligationsTileContainer';
import NoticeTextsTileContainer from './files/notices/NoticeTextsTileContainer';
import LicenseTextsTileContainer from './files/licenses/LicenseTextsTileContainer';

export default function ComponentLegalOverviewPage(props) {
  const {
    component,
    licenseLegalMetadata,
    obligations,
    loading,
    error,
    organizationId,
    applicationPublicId,
    stageTypeId,
    hash,
    availableScopes,
    showEditCopyrightOverrideModal,
    $state,

    //actions
    setDisplayCopyrightOverrideModal,
    loadAvailableScopes,
    loadComponent
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

  const getLicenseNames = effectiveLicenses => map(
      pipe(propEq('licenseId'), flip(find)(licenseLegalMetadata), prop('licenseName')), effectiveLicenses);

  const licenseNames = component && getLicenseNames(component.licenseLegalData.effectiveLicenses);

  const isTextBasedObligation = (licenseObligation) => {
    return TEXT_BASED_OBLIGATIONS.indexOf(licenseObligation.name) >= 0;
  };

  const createLicenseObligationAttributionTileContainer = (licenseObligation, index) => {
    return <LicenseObligationAttributionTileContainer key={ index } name={ licenseObligation.name }/>;
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

  const backHref = applicationPublicId && stageTypeId ?
    $state.href($state.get('legalApplicationDetails'), {
      applicationPublicId: applicationPublicId,
      stageTypeId: stageTypeId
    })
    : $state.href($state.get('legalDashboard'));

  return (
    <main className="nx-page-main">
      <LoadWrapper loading={ loading }
                   error={ error }
                   retryHandler={ load }>
        <NxBackButton href={ backHref } text="Back" />
        { component &&
        <div className="nx-page-title">
          <h1 className="nx-h1">
            { component.displayName }
          </h1>
          { createSubtitle() }
        </div>}
        { component &&
        <div id="component-legal-overview-details">
          <ComponentOverviewTile applicationPublicId={applicationPublicId}
                                 component={component}
                                 licenseNames={licenseNames}
                                 $state={$state}/>
          <LicenseObligationsTileContainer />
          <div id="component-legal-overview-details-right">
            <LicenseDetailsTile licenseNames={ licenseNames }/>
            <CopyrightStatementsTile
                component={ component }
                availableScopes={ availableScopes }
                showEditCopyrightOverrideModal = { showEditCopyrightOverrideModal }
                setDisplayCopyrightOverrideModal = { setDisplayCopyrightOverrideModal }/>
            <NoticeTextsTileContainer/>
            <LicenseTextsTileContainer/>
            { obligations.filter(isTextBasedObligation).map(createLicenseObligationAttributionTileContainer) }
          </div>
        </div>}
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
  stageTypeId: PropTypes.string,
  hash: PropTypes.string,
  licenseLegalMetadata: licenseLegalMetadataPropType,
  obligations: licenseObligationsPropType,
  loadComponent: PropTypes.func,
  loadAvailableScopes: PropTypes.func,
  availableScopes: availableScopesPropType,
  showEditCopyrightOverrideModal: PropTypes.bool.isRequired,
  setDisplayCopyrightOverrideModal: PropTypes.func.isRequired,
  $state: PropTypes.object.isRequired
};
