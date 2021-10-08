/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxButton, NxForm } from '@sonatype/react-shared-components';

import { licensesPropTypes } from '../LicenseDetectionsTile/LicenseDetectionsTile';
import { renderLicensesList } from '../LegalTabUtils';
import IqPopover from '../../../react/IqPopover';

export default function EditLicensesPopover({
  onClose,
  showEditLicensesPopover,
  declaredlicenses,
  effectiveLicenses,
  observedlicenses,
}) {
  if (!showEditLicensesPopover) {
    return null;
  }

  const renderLicenseInfoSection = () => (
    <section id="license-info-section">
      <dl className="nx-read-only">
        <dt className="nx-read-only__label">Declared Licenses</dt>
        <dd className="nx-read-only__data" id="declared-licenses-container">
          {renderLicensesList(declaredlicenses)}
        </dd>
        <dt className="nx-read-only__label">Observed Licenses</dt>
        <dd className="nx-read-only__data " id="observed-licenses-container">
          {renderLicensesList(observedlicenses)}
        </dd>
        <dt className="nx-read-only__label">Effective Licenses</dt>
        <dd className="nx-read-only__data" id="effective-licenses-container">
          {renderLicensesList(effectiveLicenses)}
        </dd>
      </dl>
    </section>
  );

  return (
    <IqPopover size="extra-large" onClose={onClose} id="edit-licenses-popover">
      <IqPopover.Header
        id="edit-licenses-popover-header"
        className="edit-licenses-popover-header"
        buttonId="edit-licenses-popover-close-btn"
        onClose={onClose}
        headerTitle="Edit Licenses"
      />
      <NxForm
        onSubmit={() => {}}
        submitBtnText="Save"
        additionalFooterBtns={
          <NxButton type="button" id="edit-licenses-cancel" onClick={onClose} disabled={false}>
            Cancel
          </NxButton>
        }
      >
        <div className="nx-grid-row">
          <div className="nx-grid-col nx-grid-col--25">{renderLicenseInfoSection()}</div>
          <div className="nx-grid-col">form fields</div>
        </div>
      </NxForm>
    </IqPopover>
  );
}

EditLicensesPopover.propTypes = {
  onClose: PropTypes.func.isRequired,
  showEditLicensesPopover: PropTypes.bool.isRequired,
  declaredlicenses: PropTypes.arrayOf(licensesPropTypes),
  effectiveLicenses: PropTypes.arrayOf(licensesPropTypes),
  observedlicenses: PropTypes.arrayOf(licensesPropTypes),
};
