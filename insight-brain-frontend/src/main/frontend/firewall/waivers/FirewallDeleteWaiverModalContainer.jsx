/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useSelector, useDispatch } from 'react-redux';
import React from 'react';
import FirewallDeleteWaiverModal from './FirewallDeleteWaiverModal';
import {
  deleteFirewallWaiver,
  hideFirewallDeleteWaiverModal,
} from './firewallDashboardWaiverActions';
import {
  selectFirewallWaiverToDelete,
  selectFirewallDeleteWaiverSaving,
  selectFirewallDeleteWaiverError,
} from './firewallDashboardWaiverSelectors';

export default function FirewallDeleteWaiverModalContainer() {
  const dispatch = useDispatch();
  const waiverToDelete = useSelector(selectFirewallWaiverToDelete);
  const deleteWaiverSaving = useSelector(selectFirewallDeleteWaiverSaving);
  const deleteWaiverError = useSelector(selectFirewallDeleteWaiverError);

  return (
    <FirewallDeleteWaiverModal
      waiverToDelete={waiverToDelete}
      deleteFirewallWaiver={(...args) => dispatch(deleteFirewallWaiver(...args))}
      hideFirewallDeleteWaiverModal={() => dispatch(hideFirewallDeleteWaiverModal())}
      deleteWaiverSaving={deleteWaiverSaving}
      deleteWaiverError={deleteWaiverError}
    />
  );
}
