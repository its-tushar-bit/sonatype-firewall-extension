/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import cx from 'classnames';
import { IqPopoverHeaderTitleText } from 'MainRoot/react/IqPopover';
import { NxButton, NxDivider, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faClose } from '@fortawesome/pro-light-svg-icons/faClose';
import PropTypes from 'prop-types';
import React from 'react';

export default function VexAnnotationPopoverHeader(props) {
  const { componentPurl, className, onClose, buttonId, headerSize, headerTitle, buttonClassnames, closeTitle } = props;
  const btnClasses = cx('iq-popover-header__close-btn', buttonClassnames);
  const btnTitle = closeTitle || 'Close';

  return (
    <header className={cx('iq-popover__header', className)}>
      <div className="iq-popover-header__title">
        <IqPopoverHeaderTitleText headerSize={headerSize} headerTitle={headerTitle} />
        <NxButton className={btnClasses} onClick={onClose} variant="icon-only" title={btnTitle} id={buttonId}>
          <NxFontAwesomeIcon icon={faClose} />
        </NxButton>
      </div>
      <div className="vex-annotation-drawer-header-popover__package-url">{componentPurl}</div>
      <NxDivider></NxDivider>
    </header>
  );
}

VexAnnotationPopoverHeader.propTypes = {
  onClose: PropTypes.func,
  componentPurl: PropTypes.string,
  className: PropTypes.string,
  buttonId: PropTypes.string,
  headerSize: PropTypes.string,
  headerTitle: PropTypes.string,
  buttonClassnames: PropTypes.string,
  closeTitle: PropTypes.string,
};
