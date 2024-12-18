/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import * as PropTypes from 'prop-types';
import moment from 'moment';
import {
  NxTextLink,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxReadOnly,
} from '@sonatype/react-shared-components';

import { displayWaiverScope, formatWaiverDetails, isWaiverAllVersionsOrExact } from 'MainRoot/util/waiverUtils';
import ComponentDisplay from 'MainRoot/ComponentDisplay/ReactComponentDisplay';
import { STANDARD_DATE_FORMAT } from 'MainRoot/util/dateUtils';
import { selectViolationFilteredSimilarWaivers, selectViolationSlice } from 'MainRoot/violation/violationSelectors';
import { selectIsFirewallOrRepository } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectFirewallComponentDetailsPageRouteParams } from 'MainRoot/firewall/firewallSelectors';
import {
  selectSelectedPolicyViolation,
  selectSelectedViolationId,
} from 'MainRoot/componentDetails/ViolationsTableTile/PolicyViolationsSelectors';
import { loadSimilarWaivers } from './waiverActions';

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
      <NxTableRow key={similarWaiver.policyWaiverId}>
        <NxTableCell>
          <NxReadOnly.Label>Created</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-similar-waivers-table__created visual-testing-ignore">
            {moment(similarWaiver.createTime).format(STANDARD_DATE_FORMAT)}
          </NxReadOnly.Data>

          <NxReadOnly.Label className="iq-similar-waivers-table__expiration-label">Expiration</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-similar-waivers-table__expiration visual-testing-ignore">
            {similarWaiver.expiryTime ? moment(similarWaiver.expiryTime).format(STANDARD_DATE_FORMAT) : 'Never'}
          </NxReadOnly.Data>
        </NxTableCell>
        <NxTableCell>
          <NxReadOnly.Label>Scope</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-similar-waivers-table__scope">
            {displayWaiverScope(similarWaiver)}
          </NxReadOnly.Data>

          <NxReadOnly.Label>Component</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-similar-waivers-table__component">
            {isWaiverAllVersionsOrExact(similarWaiver) ? (
              <ComponentDisplay
                component={similarWaiver}
                truncate={false}
                matcherStrategy={similarWaiver.matcherStrategy}
                displayTextIfUnknown={unknownComponentName}
              />
            ) : (
              'All'
            )}
          </NxReadOnly.Data>

          {reasons.length > 0 && (
            <>
              <NxReadOnly.Label>Conditions</NxReadOnly.Label>
              <NxReadOnly.Data className="iq-similar-waivers-table__conditions">
                {reasons.map((reason, index) => (
                  <p key={index}>{reason}</p>
                ))}
              </NxReadOnly.Data>
            </>
          )}
          <>
            <NxReadOnly.Label>Reason</NxReadOnly.Label>
            <NxReadOnly.Data className="iq-waivers-table__reason">
              {similarWaiver.reasonText ?? '\u2014'}
            </NxReadOnly.Data>
          </>
          {similarWaiver.comment && (
            <>
              <NxReadOnly.Label>Comment</NxReadOnly.Label>
              <NxReadOnly.Data className="iq-similar-waivers-table__comment">{similarWaiver.comment}</NxReadOnly.Data>
            </>
          )}

          <NxReadOnly.Label>Author</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-similar-waivers-table__author">
            {similarWaiver?.creatorName || '- -'}
          </NxReadOnly.Data>
        </NxTableCell>
      </NxTableRow>
    );
  };

  useEffect(() => {
    const id = isFirewallOrRepository ? firewallSelectedViolationId : selectedViolationId;
    load(id);
  }, [isFirewallOrRepository, selectedViolationId, firewallSelectedViolationId]);

  return (
    <NxTable id="list-similar-waivers-table" className="iq-similar-waivers-table">
      <NxTableHead>
        <NxTableRow>
          <NxTableCell className="iq-similar-waivers-table__duration">DURATION</NxTableCell>
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
