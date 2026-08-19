/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxInfoAlert, NxTextLink } from '@sonatype/react-shared-components';

/**
 * Info alert shown at the bottom of enterprise feature preview forms.
 * Displays "This is an Enterprise feature. Changes can't be saved."
 * Optionally shows a "Go back to Default" link for edit pages with mode switch.
 *
 */
export default function EnterpriseInfoAlert({ onGoBackToDefault }) {
  return (
    <NxInfoAlert>
      This is an Enterprise feature. Changes can&apos;t be saved.
      {onGoBackToDefault && (
        <>
          {' '}
          <NxTextLink onClick={onGoBackToDefault}>Return to Lifecycle Pro</NxTextLink>
        </>
      )}
    </NxInfoAlert>
  );
}

EnterpriseInfoAlert.propTypes = {
  onGoBackToDefault: PropTypes.func,
};
