/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { createRef } from 'react';
import * as PropTypes from 'prop-types';
import { faDownload, faTrashAlt } from '@fortawesome/pro-solid-svg-icons';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxStatefulForm,
  NxModal,
  NxWarningAlert,
  useToggle,
} from '@sonatype/react-shared-components';
import { PRODUCT_LICENSE_UNINSTALL_MASK_TIMER_DONE } from '../productLicenseActions';

function WithLicenseFooter({ fileChangeHandler, uninstallError, uninstallMaskState, uninstallLicense }) {
  const formRef = createRef();
  const inputRef = createRef();

  const [showConfirmationModal, dismissShowConfirmationModal] = useToggle(false);

  function buttonClickHandler() {
    formRef.current.reset();
    inputRef.current.click();
  }

  function uninstallLicenseHandler() {
    uninstallLicense().then(({ type: actionType }) => {
      if (actionType === PRODUCT_LICENSE_UNINSTALL_MASK_TIMER_DONE) {
        window.location.reload();
      }
    });
  }

  return (
    <div className="nx-btn-bar">
      <NxButton variant="tertiary" onClick={dismissShowConfirmationModal} id="uninstall-license">
        <NxFontAwesomeIcon icon={faTrashAlt} />
        <span>Uninstall License</span>
      </NxButton>
      <NxButton variant="tertiary" onClick={buttonClickHandler} id="install-license-btn">
        <NxFontAwesomeIcon icon={faDownload} />
        <span>Update License</span>
        <form className="iq-license-footer-form" ref={formRef}>
          <input
            id="license-input"
            className="iq-input-file--hidden"
            type="file"
            onChange={fileChangeHandler}
            ref={inputRef}
          />
        </form>
      </NxButton>
      {showConfirmationModal && (
        <NxModal variant="narrow" id="license-uninstall-modal">
          <NxStatefulForm
            submitMaskMessage="Uninstalling"
            submitMaskState={uninstallMaskState}
            onSubmit={uninstallLicenseHandler}
            submitError={uninstallError}
            onCancel={dismissShowConfirmationModal}
            submitBtnText="Uninstall"
          >
            <header className="nx-modal-header">
              <h2 className="nx-h2">Uninstall License</h2>
            </header>
            <div className="nx-modal-content">
              <NxWarningAlert>
                If you uninstall the Sonatype Nexus IQ Server License, the system will be rendered unusable. Are you
                sure you want to continue?
              </NxWarningAlert>
            </div>
          </NxStatefulForm>
        </NxModal>
      )}
    </div>
  );
}

WithLicenseFooter.propTypes = {
  fileChangeHandler: PropTypes.func.isRequired,
  uninstallError: PropTypes.string,
  uninstallMaskState: PropTypes.bool,
  uninstallLicense: PropTypes.func.isRequired,
};

export default WithLicenseFooter;
