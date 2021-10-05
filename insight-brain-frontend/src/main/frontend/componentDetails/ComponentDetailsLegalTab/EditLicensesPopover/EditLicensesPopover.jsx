/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxButton, NxForm } from '@sonatype/react-shared-components';

import IqPopover from '../../../react/IqPopover';

export default function EditLicensesPopover(props) {
  const { onClose, showEditLicensesPopover } = props;

  if (!showEditLicensesPopover) {
    return null;
  }

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
          <div className="nx-grid-col nx-grid-col--25">
            <section>
              <header>
                <h3>Declared Licenses</h3>
              </header>
              <div>Apache-2.0</div>
            </section>
          </div>
          <div className="nx-grid-col">form fields</div>
        </div>
      </NxForm>
    </IqPopover>
  );
}

EditLicensesPopover.propTypes = {
  onClose: PropTypes.func.isRequired,
  showEditLicensesPopover: PropTypes.bool.isRequired,
};
