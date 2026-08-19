/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { actions as waiverActions } from './waiverSlice';
import {
  selectCanWaivePolicyViolations,
  selectPermissionsLoading,
  selectPermissionsError,
} from './bulkWaiverSelectors';
import { goToBulkWaivePage } from 'MainRoot/applicationReport/applicationReportActions';
import { selectHasBulkWaivers } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { EnterpriseLockButton } from 'MainRoot/shared/enterpriseTier';
import { faCircleNotch } from '@fortawesome/free-solid-svg-icons';

/**
 * Reusable Bulk Waive button component that handles permission checking and navigation.
 * Only renders if user has WAIVE_POLICY_VIOLATIONS permission.
 *
 * @param {Object} props
 * @param {boolean} props.disabled - Whether the button should be disabled
 * @param {string} props.publicId - Application publicId for permission check
 * @param {string} [props.className] - Optional additional CSS class for the button
 * @param {boolean} [props.skipPermissionCheck] - Skip loading permissions (default: false)
 */
export default function BulkWaiveButton({ disabled, publicId, className = '', skipPermissionCheck = false }) {
  const dispatch = useDispatch();
  const canWaivePolicyViolations = useSelector((state) => selectCanWaivePolicyViolations(state, publicId));
  const hasBulkWaivers = useSelector(selectHasBulkWaivers);
  const permissionsLoading = useSelector((state) => selectPermissionsLoading(state, publicId));
  const permissionsError = useSelector((state) => selectPermissionsError(state, publicId));

  useEffect(() => {
    if (publicId && !skipPermissionCheck) {
      dispatch(waiverActions.loadPermissionForAppWaivers(publicId));
    }
  }, [dispatch, publicId, skipPermissionCheck]);

  const handleClick = () => {
    dispatch(waiverActions.clearBulkWaiveCheckboxes());
    dispatch(waiverActions.resetWaiverConfiguration());
    dispatch(goToBulkWaivePage());
  };

  if (permissionsLoading) {
    return <NxFontAwesomeIcon icon={faCircleNotch} spin className="nx-loading-spinner__icon" />;
  }

  if (permissionsError) {
    console.error('Failed to load waiver permissions.');

    return null;
  }

  if (!canWaivePolicyViolations) {
    return null;
  }

  if (!hasBulkWaivers) {
    return <EnterpriseLockButton label="Preview Bulk Waive" onClick={handleClick} variant="tertiary" />;
  }

  return (
    <NxButton onClick={handleClick} variant="tertiary" id="bulk-waive" disabled={disabled} className={className}>
      Bulk Waive
    </NxButton>
  );
}

BulkWaiveButton.propTypes = {
  disabled: PropTypes.bool.isRequired,
  publicId: PropTypes.string,
  className: PropTypes.string,
  skipPermissionCheck: PropTypes.bool,
};
