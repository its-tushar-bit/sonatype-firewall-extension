/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { faDownload } from '@fortawesome/pro-solid-svg-icons';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import React, { createRef } from 'react';
import * as PropTypes from 'prop-types';

function WithNoLicenseFooter({ fileChangeHandler }) {
  const inputRef = createRef();
  const formRef = createRef();

  function buttonClickHandler() {
    formRef.current.reset();
    inputRef.current.click();
  }

  return (
    <NxButton variant="tertiary" onClick={buttonClickHandler} id="install-license-btn">
      <NxFontAwesomeIcon icon={faDownload} />
      <span>Install License</span>
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
  );
}

WithNoLicenseFooter.propTypes = {
  fileChangeHandler: PropTypes.func.isRequired,
};

export default WithNoLicenseFooter;
