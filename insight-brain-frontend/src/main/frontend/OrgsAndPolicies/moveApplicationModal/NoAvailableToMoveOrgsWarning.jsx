/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxButton, NxButtonBar, NxErrorAlert, NxFooter } from '@sonatype/react-shared-components';
import PropTypes from 'prop-types';

const NoAvailableToMoveOrgsWarning = ({ closeModal }) => {
  return (
    <>
      <NxErrorAlert>No available destination organizations.</NxErrorAlert>
      <NxFooter>
        <NxButtonBar>
          <NxButton onClick={closeModal}>OK</NxButton>
        </NxButtonBar>
      </NxFooter>
    </>
  );
};

export default NoAvailableToMoveOrgsWarning;

NoAvailableToMoveOrgsWarning.propTypes = {
  closeModal: PropTypes.func.isRequired,
};
