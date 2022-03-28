/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { NxH1, NxH2, NxTable, NxTile } from '@sonatype/react-shared-components';
import { isEmpty } from 'ramda';
import AdministratorsRow from './AdministratorsRow';
import { load, goToAdministratorPage } from '../administratorsSlice';
import { selectIsLoading, selectLoadError, selectMembersByRole } from '../administratorsSelectors';

const AdministratorsConfig = () => {
  const dispatch = useDispatch();
  const loadConfiguration = () => dispatch(load());
  const goToAdministratorRolePage = (roleId) => dispatch(goToAdministratorPage(roleId));
  const isLoading = useSelector(selectIsLoading);
  const loadError = useSelector(selectLoadError);
  const membersByRole = useSelector(selectMembersByRole);

  useEffect(() => {
    loadConfiguration();
  }, []);

  const createRows = () => {
    return membersByRole.map((role) => {
      const onRowClick = () => {
        goToAdministratorRolePage(role.roleId);
      };

      return <AdministratorsRow key={role.roleId} role={role} onClick={onRowClick} />;
    });
  };

  return (
    <main id="administrators-config-container" className="nx-page-main">
      <div className="nx-page-title">
        <NxH1>Administrators</NxH1>
      </div>
      <NxTile>
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>Configure Administrators</NxH2>
          </NxTile.HeaderTitle>
        </NxTile.Header>
        <NxTile.Content>
          <NxTable>
            <NxTable.Head>
              <NxTable.Row>
                <NxTable.Cell width="20%">Role</NxTable.Cell>
                <NxTable.Cell width="80%">Members</NxTable.Cell>
                <NxTable.Cell chevron />
              </NxTable.Row>
            </NxTable.Head>
            <NxTable.Body isLoading={isLoading} error={loadError} retryHandler={load} emptyMessage="No data found.">
              {!isEmpty(membersByRole) && createRows()}
            </NxTable.Body>
          </NxTable>
        </NxTile.Content>
      </NxTile>
    </main>
  );
};

export default AdministratorsConfig;
