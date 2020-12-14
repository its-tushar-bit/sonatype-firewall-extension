/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import { NxAccordion } from '@sonatype/react-shared-components';
import { chain, groupBy, map, pipe, prop, toPairs, values } from 'ramda';
import { licenseLegalMetadataPropType } from './advancedLegalPropTypes';

export default function LicenseObligationsTile({ licenseLegalMetadata }) {

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
    const [open, setOpen] = useState(false);
    return <NxAccordion key={ index } open={ open } onToggle={ setOpen }>
      <NxAccordion.Header>
        <h3 className="nx-accordion__header-title">
          { createItemAccordionHeader(licenseObligation) }
        </h3>
      </NxAccordion.Header>
      { licenseObligation.licenses.map(createItemContent) }
    </NxAccordion>;
  };

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
  licenseLegalMetadata: licenseLegalMetadataPropType
};
