/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxTable } from '@sonatype/react-shared-components';

const AdministratorsRow = ({ role, onClick }) => {
  const { roleName, membersByOwner } = role;
  const members = membersByOwner[0].members;
  const displayMembersString = members.map((m) => m.displayName).join(', ');

  return (
    <NxTable.Row isClickable onClick={onClick}>
      <NxTable.Cell>{roleName}</NxTable.Cell>
      <NxTable.Cell>{displayMembersString}</NxTable.Cell>
      <NxTable.Cell chevron />
    </NxTable.Row>
  );
};

export default AdministratorsRow;

AdministratorsRow.propTypes = {
  role: PropTypes.shape({
    roleId: PropTypes.string.isRequired,
    roleName: PropTypes.string.isRequired,
    membersByOwner: PropTypes.array.isRequired,
  }),
  onClick: PropTypes.func.isRequired,
};
