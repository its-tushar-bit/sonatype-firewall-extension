/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import * as PropTypes from 'prop-types';
import {
  NxTextLink,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
} from '@sonatype/react-shared-components';

import { formatWaiverDetails } from 'MainRoot/util/waiverUtils';
import { selectViolationFilteredSimilarWaivers, selectViolationSlice } from 'MainRoot/violation/violationSelectors';
import { selectIsFirewallOrRepository } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectFirewallComponentDetailsPageRouteParams } from 'MainRoot/firewall/firewallSelectors';
import {
  selectSelectedPolicyViolation,
  selectSelectedViolationId,
} from 'MainRoot/componentDetails/ViolationsTableTile/PolicyViolationsSelectors';
import { loadSimilarWaivers } from './waiverActions';
import WaiverRow from './WaiverRow';

const EmptyMessage = ({ similarWaivers }) =>
  similarWaivers.length === 0 ? (
    <span>
      No similar waivers for this violation, to learn more about waivers see our{' '}
      <NxTextLink external href="https://links.sonatype.com/products/nxiq/doc/similar-waivers">
        help documentation
      </NxTextLink>
    </span>
  ) : (
    <span>No data available given the applied filters.</span>
  );

export default function ListSimilarWaiversTable() {
  const { similarWaivers, loadingSimilarWaivers: loading, loadSimilarWaiversError: loadError } = useSelector(
    selectViolationSlice
  );

  const filteredSimilarWaivers = useSelector(selectViolationFilteredSimilarWaivers);
  const isFirewallOrRepository = useSelector(selectIsFirewallOrRepository);
  const firewallComponentDetailsPageParams = useSelector(selectFirewallComponentDetailsPageRouteParams);
  const unknownComponentName = isFirewallOrRepository ? firewallComponentDetailsPageParams.componentDisplayName : null;

  const selectedViolationId = useSelector(selectSelectedViolationId);
  const firewallSelectedViolationId = useSelector(selectSelectedPolicyViolation)?.policyViolationId;

  const dispatch = useDispatch();
  const load = (id) => dispatch(loadSimilarWaivers(id));

  const renderSimilarWaiver = (similarWaiver) => {
    const { reasons } = formatWaiverDetails(similarWaiver);
    return (
      <WaiverRow
        waiver={similarWaiver}
        unknownComponentName={unknownComponentName}
        reasons={reasons}
        isSimilarWaiver
        key={similarWaiver.policyWaiverId}
      />
    );
  };

  useEffect(() => {
    const id = isFirewallOrRepository ? firewallSelectedViolationId : selectedViolationId;
    load(id);
  }, [isFirewallOrRepository, selectedViolationId, firewallSelectedViolationId]);

  return (
    <NxTable id="list-similar-waivers-table" className="iq-waivers-table">
      <NxTableHead>
        <NxTableRow>
          <NxTableCell className="iq-waivers-table__duration">DURATION</NxTableCell>
          <NxTableCell>WAIVER DETAILS</NxTableCell>
        </NxTableRow>
      </NxTableHead>
      <NxTableBody
        emptyMessage={<EmptyMessage similarWaivers={similarWaivers} />}
        isLoading={loading}
        error={loadError}
        retryHandler={load}
      >
        {filteredSimilarWaivers?.length > 0 ? filteredSimilarWaivers.map(renderSimilarWaiver) : null}
      </NxTableBody>
    </NxTable>
  );
}

EmptyMessage.propTypes = {
  similarWaivers: PropTypes.arrayOf(PropTypes.object),
};
