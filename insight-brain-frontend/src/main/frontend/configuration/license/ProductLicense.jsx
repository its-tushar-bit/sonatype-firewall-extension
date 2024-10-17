/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect, useState } from 'react';
import * as PropTypes from 'prop-types';

import { NxErrorAlert, NxLoadWrapper, NxSubmitMask } from '@sonatype/react-shared-components';

import ProductLicenseInfo from './contents/ProductLicenseInfo';
import NoProductLicense from './contents/NoProductLicense';
import WithLicenseFooter from './footers/WithLicenseFooter';
import WithNoLicenseFooter from './footers/WithNoLicenseFooter';
import EulaModal from './EulaModal';
import { PRODUCT_LICENSE_UPDATE_LICENSE_FAILED } from './productLicenseActions';

function ProductLicense({
  license,
  load,
  loading,
  loadError,
  updateLicense,
  submitMaskState,
  updateLicenseError,
  clearUpdateLicenseError,
  uninstallMaskState,
  uninstallError,
  uninstallLicense,
}) {
  const [showEulaModal, setShowEulaModal] = useState(false);
  const [updatedLicense, setUpdatedLicense] = useState(null);

  useEffect(() => {
    load();
  }, []);

  function fileChangeHandler({
    target: {
      files: [file],
    },
  }) {
    setUpdatedLicense(file);
    setShowEulaModal(true);
  }

  function updateLicenseHandler() {
    updateLicense(updatedLicense).then(({ type: actionType }) => {
      if (actionType === PRODUCT_LICENSE_UPDATE_LICENSE_FAILED) return;

      if (!license) {
        window.location.href = '#/gettingStarted';
      }

      window.location.reload();
    });

    setShowEulaModal(false);
  }

  return (
    <main className="nx-page-main">
      <NxLoadWrapper loading={loading} error={loadError} retryHandler={load}>
        <header className="nx-page-title">
          <h1 className="nx-h1">Product License</h1>
        </header>
        <section className="nx-tile">
          <div className="nx-tile-content">
            {license ? <ProductLicenseInfo license={license} /> : <NoProductLicense />}
          </div>
          <footer
            className={
              license ? 'nx-footer iq-product-license-details-footer' : 'iq-product-license-details-footer--no-license'
            }
          >
            {updateLicenseError && <NxErrorAlert onClose={clearUpdateLicenseError}>{updateLicenseError}</NxErrorAlert>}
            {license ? (
              <WithLicenseFooter
                fileChangeHandler={fileChangeHandler}
                uninstallError={uninstallError}
                uninstallMaskState={uninstallMaskState}
                uninstallLicense={uninstallLicense}
              />
            ) : (
              <WithNoLicenseFooter fileChangeHandler={fileChangeHandler} />
            )}
          </footer>
        </section>
      </NxLoadWrapper>
      {showEulaModal && (
        <EulaModal
          submitMaskState={submitMaskState}
          updateLicense={updateLicenseHandler}
          closeModal={() => setShowEulaModal(false)}
        />
      )}
      {submitMaskState !== null && <NxSubmitMask message="Installing" success={submitMaskState} />}
    </main>
  );
}

ProductLicense.propTypes = {
  license: PropTypes.object,
  load: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  updateLicense: PropTypes.func.isRequired,
  submitMaskState: PropTypes.bool,
  updateLicenseError: PropTypes.string,
  clearUpdateLicenseError: PropTypes.func.isRequired,
  uninstallMaskState: PropTypes.bool,
  uninstallError: PropTypes.string,
  uninstallLicense: PropTypes.func.isRequired,
};

export default ProductLicense;
