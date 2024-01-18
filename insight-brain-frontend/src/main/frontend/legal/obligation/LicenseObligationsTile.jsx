/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useState } from 'react';
import {
  NxAccordion,
  NxButton,
  NxFontAwesomeIcon,
  NxOverflowTooltip,
  NxSegmentedButton,
  NxStatefulAccordion,
} from '@sonatype/react-shared-components';
import { licenseLegalMetadataPropType, licenseObligationsPropTypes } from '../advancedLegalPropTypes';
import { OBLIGATION_STATUS_TO_DISPLAY, OBLIGATION_STATUSES } from '../advancedLegalConstants';
import { faCheckCircle, faExclamationTriangle, faMinusCircle } from '@fortawesome/pro-solid-svg-icons';
import * as PropTypes from 'prop-types';
import LicenseObligationModalContainer from './LicenseObligationModalContainer';
import { find, propEq } from 'ramda';
import AllLicenseObligationsModalContainer from './AllLicenseObligationsModalContainer';
import { findSingleLicenseIndex } from '../legalUtility';

export default function LicenseObligationsTile(props) {
  const {
    // actions
    setObligationStatus,
    setShowObligationModal,
    setShowAllObligationsModal,
    // state
    licenseObligations,
    licenseLegalMetadata,
    showAllObligationsModal,
    ownerType,
    ownerId,
    hash,
    stageTypeId,
    $state,
    effectiveLicenses,
    componentIdentifier,
  } = props;

  const flatEffectiveLicenses = effectiveLicenses.map((e) => e.licenseId);
  const isObligationPresent = () => licenseObligations.length > 0;

  const licenseDetailsTargetState = () => {
    if (stageTypeId) {
      return 'legal.stageTypeComponentLicenseDetails';
    } else if (componentIdentifier) {
      return 'legal.componentLicenseDetailsByComponentIdentifier';
    } else {
      return 'legal.componentLicenseDetails';
    }
  };

  const createObligationStatusIcon = (obligationStatus) => {
    switch (obligationStatus) {
      case 'FULFILLED':
        return <NxFontAwesomeIcon icon={faCheckCircle} className="license-obligation-fulfilled-icon" />;
      case 'FLAGGED':
        return <NxFontAwesomeIcon icon={faExclamationTriangle} className="license-obligation-flagged-icon" />;
      case 'IGNORED':
        return <NxFontAwesomeIcon icon={faMinusCircle} className="license-obligation-ignored-icon" />;
    }
  };

  const getLicensesWithObligation = (licenseObligation) => {
    return licenseLegalMetadata.filter(
      (element) =>
        element.obligations !== null &&
        flatEffectiveLicenses.includes(element.licenseId) &&
        element.obligations.some((obligation) => obligation.name === licenseObligation.name)
    );
  };

  const createItemAccordionHeader = (licenseObligation, licensesWithObligation) => {
    let headerText = licenseObligation.name;
    if (licensesWithObligation && licensesWithObligation.length > 1) {
      headerText += ` (${licensesWithObligation.length})`;
    }
    return headerText;
  };

  const [openSegmentedButton, setOpenSegmentedButton] = useState(null);

  const createSegmentedButtonDropdownOptions = (licenseObligation) => {
    return OBLIGATION_STATUSES.filter((obligationStatus) => obligationStatus !== licenseObligation.originalStatus).map(
      (obligationStatus) => {
        return (
          <button
            key={obligationStatus + '-segmented-button-dropdown-option'}
            type="button"
            className="nx-dropdown-button"
            onClick={() => {
              setOpenSegmentedButton(null);
              setObligationStatus({
                name: licenseObligation.name,
                value: obligationStatus,
              });
              setShowObligationModal({
                name: licenseObligation.name,
                value: true,
              });
            }}
          >
            {createObligationStatusIcon(obligationStatus)}
            <span>Mark as {OBLIGATION_STATUS_TO_DISPLAY[obligationStatus]}</span>
          </button>
        );
      }
    );
  };

  const createReviewStatus = (licenseObligation) => {
    return (
      <div>
        <h4 className="nx-h4">Review Status</h4>
        <p className="obligation-text">{OBLIGATION_STATUS_TO_DISPLAY[licenseObligation.originalStatus]}</p>
      </div>
    );
  };

  const createItemContentTexts = (licenseObligationLicenseText, index) => {
    return (
      <blockquote className="nx-blockquote" key={index}>
        {licenseObligationLicenseText}
      </blockquote>
    );
  };

  const createItemContent = (licenseObligation, licenseWithObligations) => {
    return (
      <div key={licenseWithObligations.licenseName + '-' + licenseObligation.name} className="license-obligation-item">
        <h4 className="nx-h4">{licenseWithObligations.licenseName} — Obligation Text</h4>
        {find(propEq('name', licenseObligation.name), licenseWithObligations.obligations).obligationTexts.map(
          createItemContentTexts
        )}
        <div className="license-obligation-view-full-license">
          <a
            href={$state.href(licenseDetailsTargetState(), {
              ownerType,
              ownerId,
              hash,
              stageTypeId,
              componentIdentifier,
              licenseIndex: findSingleLicenseIndex(licenseWithObligations.licenseId, licenseLegalMetadata),
            })}
          >
            View full license text
          </a>
        </div>{' '}
      </div>
    );
  };

  const createItem = (licenseObligation) => {
    const statusSegmentedButtonLabel = (
      <Fragment>
        {createObligationStatusIcon(licenseObligation.originalStatus)}
        <span>{OBLIGATION_STATUS_TO_DISPLAY[licenseObligation.originalStatus]}</span>
      </Fragment>
    );
    const licensesWithObligation = getLicensesWithObligation(licenseObligation);
    return (
      <NxStatefulAccordion key={licenseObligation.name + '-accordion'} defaultOpen={false}>
        <NxAccordion.Header>
          <NxOverflowTooltip>
            <h3 className="nx-accordion__header-title nx-truncate-ellipsis">
              {createItemAccordionHeader(licenseObligation, licensesWithObligation)}
            </h3>
          </NxOverflowTooltip>
          <NxSegmentedButton
            variant="tertiary"
            buttonContent={statusSegmentedButtonLabel}
            isOpen={openSegmentedButton === licenseObligation.name}
            onToggleOpen={() =>
              openSegmentedButton === licenseObligation.name
                ? setOpenSegmentedButton(null)
                : setOpenSegmentedButton(licenseObligation.name)
            }
            onClick={() => {
              setShowObligationModal({
                name: licenseObligation.name,
                value: true,
              });
            }}
          >
            {createSegmentedButtonDropdownOptions(licenseObligation)}
          </NxSegmentedButton>
        </NxAccordion.Header>
        {createReviewStatus(licenseObligation)}
        {licensesWithObligation.map((licenseWithObligation) =>
          createItemContent(licenseObligation, licenseWithObligation)
        )}
      </NxStatefulAccordion>
    );
  };

  return (
    <section id="license-obligations-tile" className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">License Obligations</h2>
        </div>
        <div className="nx-tile__actions">
          {licenseObligations.length > 0 ? (
            <NxButton
              id="mark-all-obligations-resolved"
              variant="tertiary"
              onClick={() => setShowAllObligationsModal(true)}
            >
              <span>Mark all as Fulfilled</span>
            </NxButton>
          ) : null}
        </div>
        {showAllObligationsModal && (
          <AllLicenseObligationsModalContainer
            key={'all-license-obligations-modal'}
            createObligationStatusIcon={createObligationStatusIcon}
          />
        )}
      </header>
      <div className="nx-tile-content nx-tile-content--accordion-container">
        {isObligationPresent() ? licenseObligations.map(createItem) : 'None found'}
      </div>
      {licenseObligations.map((licenseObligation) => {
        return (
          licenseObligation.showObligationModal && (
            <LicenseObligationModalContainer
              key={licenseObligation.name + '-modal'}
              licenseObligation={licenseObligation}
              createObligationStatusIcon={createObligationStatusIcon}
            />
          )
        );
      })}
    </section>
  );
}

LicenseObligationsTile.propTypes = {
  setObligationStatus: PropTypes.func.isRequired,
  setShowObligationModal: PropTypes.func.isRequired,
  setShowAllObligationsModal: PropTypes.func.isRequired,
  licenseObligations: licenseObligationsPropTypes,
  licenseLegalMetadata: licenseLegalMetadataPropType,
  showAllObligationsModal: PropTypes.bool,
  ownerType: PropTypes.string.isRequired,
  ownerId: PropTypes.string.isRequired,
  hash: PropTypes.string,
  stageTypeId: PropTypes.string,
  componentIdentifier: PropTypes.string,
  $state: PropTypes.object.isRequired,
  effectiveLicenses: PropTypes.arrayOf(PropTypes.object),
};
