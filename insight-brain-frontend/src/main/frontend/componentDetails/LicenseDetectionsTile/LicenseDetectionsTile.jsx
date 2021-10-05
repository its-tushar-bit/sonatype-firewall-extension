/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faPen } from '@fortawesome/pro-solid-svg-icons';
import * as PropTypes from 'prop-types';

export default function LicenseDetectionsTile(props) {
  const { toggleShowEditLicensesPopover } = props;
  return (
    <section id="component-details-legal-license-detections-tile" className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2" id="license-detections-title">
            License Detections
          </h2>
        </div>
        <NxButton
          id="component-details-edit-licenses"
          className="nx-tile__actions"
          variant="tertiary"
          onClick={toggleShowEditLicensesPopover}
        >
          <NxFontAwesomeIcon icon={faPen} />
          <span>Edit</span>
        </NxButton>
      </header>
    </section>
  );
}

LicenseDetectionsTile.propTypes = {
  toggleShowEditLicensesPopover: PropTypes.func.isRequired,
};
