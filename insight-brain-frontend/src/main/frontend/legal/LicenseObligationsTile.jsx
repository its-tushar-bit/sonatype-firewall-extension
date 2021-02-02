/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import { NxAccordion, NxDropdown, NxFontAwesomeIcon, NxOverflowTooltip } from '@sonatype/react-shared-components';
import { licenseObligationPropTypes } from './advancedLegalPropTypes';
import { OBLIGATION_STATUSES, OBLIGATION_STATUS_TO_DISPLAY } from './advancedLegalConstants';
import { faCheckCircle, faExclamationTriangle, faMinusCircle } from '@fortawesome/pro-solid-svg-icons';

export default function LicenseObligationsTile({ licenseObligations }) {
  const createItemContentTexts = (licenseObligationLicenseText, index) => {
    return <p className="obligation-text" key={ index }>{ licenseObligationLicenseText }</p>;
  };

  const createItemContent = (licenseObligationLicense, index) => {
    return <div key={ index }>
      <h4 className="nx-h4">{ licenseObligationLicense.name }</h4>
      { licenseObligationLicense.texts.map(createItemContentTexts) }
    </div>;
  };

  const createItemAccordionHeader = (licenseObligation) => {
    let headerText = licenseObligation.name;
    if (licenseObligation.licenses && licenseObligation.licenses.length > 1) {
      headerText += ` (${ licenseObligation.licenses.length })`;
    }
    return headerText;
  };

  const createItem = (licenseObligation, index) => {
    const [isAccordionOpen, setAccordionOpen] = useState(false);
    const [isStatusDropdownOpen, setStatusDropdownOpen] = useState(false);
    const onToggleCollapse = () => { setStatusDropdownOpen(!isStatusDropdownOpen); };
    const statusDropdownLabel = <span>{ createObligationStatusIcon(licenseObligation.status) }
      { OBLIGATION_STATUS_TO_DISPLAY[licenseObligation.status] }</span>;
    return <NxAccordion key={ index } open={ isAccordionOpen } onToggle={ setAccordionOpen }>
      <NxAccordion.Header>
        <NxOverflowTooltip>
          <h3 className="nx-accordion__header-title nx-truncate-ellipsis">
            { createItemAccordionHeader(licenseObligation) }
          </h3>
        </NxOverflowTooltip>
        <NxDropdown label={ statusDropdownLabel } isOpen={ isStatusDropdownOpen } onToggleCollapse={ onToggleCollapse }>
          {
            OBLIGATION_STATUSES.filter(obligationStatus => obligationStatus !== licenseObligation.status)
                .map((obligationStatus, index) => {
                  return <button key={ index } className="nx-dropdown-button">
                    Mark as { OBLIGATION_STATUS_TO_DISPLAY[obligationStatus] }
                  </button>;
                })
          }
        </NxDropdown>
      </NxAccordion.Header>
      { licenseObligation.licenses.map(createItemContent) }
    </NxAccordion>;
  };

  const createObligationStatusIcon = obligationStatus => {
    switch (obligationStatus) {
      case 'FULFILLED':
        return <NxFontAwesomeIcon icon={ faCheckCircle } className="license-obligation-fulfilled-icon" />;
      case 'FLAGGED':
        return <NxFontAwesomeIcon icon={ faExclamationTriangle } className="license-obligation-flagged-icon"/>;
      case 'IGNORED':
        return <NxFontAwesomeIcon icon={ faMinusCircle } className="license-obligation-ignored-icon"/>;
    }
  };

  return (
    <section id="license-obligations-tile" className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">License Obligations</h2>
        </div>
      </header>
      <div className="nx-tile-content nx-tile-content--accordion-container">
        { licenseObligations.map(createItem) }
      </div>
    </section>
  );
}

LicenseObligationsTile.propTypes = {
  licenseObligations: licenseObligationPropTypes
};
