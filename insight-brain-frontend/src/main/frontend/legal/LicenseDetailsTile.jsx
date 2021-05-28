/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faAngleRight } from '@fortawesome/pro-solid-svg-icons';
import * as PropTypes from 'prop-types';
import { componentPropType, licenseLegalMetadataPropType } from './advancedLegalPropTypes';
import { findSingleLicenseIndex, getComponentEffectiveLicenseNamesAndIds } from './legalUtility';

export default function LicenseDetailsTile(props) {
  const { component, licenseLegalMetadata, ownerType, ownerId, hash, stageTypeId, $state } = props;

  const licenses = getComponentEffectiveLicenseNamesAndIds(component, licenseLegalMetadata);
  const isLicensePresent = () => licenses.length > 0;
  const licenseDetailsTargetState = () =>
    stageTypeId ? 'legal.stageTypeComponentLicenseFilesDetails' : 'legal.componentLicenseFilesDetails';

  const createItem = (license, index) => {
    return (
      <li className="nx-list__item nx-list__item--link" key={index}>
        <a
          className="nx-list__link"
          href={$state.href(licenseDetailsTargetState(), {
            ownerType,
            ownerId,
            hash,
            stageTypeId,
            licenseIndex: findSingleLicenseIndex(license.licenseId, licenseLegalMetadata),
          })}
        >
          <span className="nx-list__text">{license.licenseName}</span>
          <NxFontAwesomeIcon icon={faAngleRight} className="nx-chevron" />
        </a>
      </li>
    );
  };

  return (
    <section id="license-details-tile" className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Licenses</h2>
        </div>
      </header>
      <div className="nx-tile-content">
        <ul className="nx-list nx-list--clickable">{isLicensePresent() ? licenses.map(createItem) : 'None found'}</ul>
      </div>
    </section>
  );
}

LicenseDetailsTile.propTypes = {
  component: componentPropType,
  licenseLegalMetadata: licenseLegalMetadataPropType,
  ownerType: PropTypes.string.isRequired,
  ownerId: PropTypes.string.isRequired,
  hash: PropTypes.string.isRequired,
  stageTypeId: PropTypes.string,
  $state: PropTypes.object.isRequired,
};
