/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as PropTypes from 'prop-types';
import GettingStartedDocLink from './GettingStartedDocLink';

export default function SystemSetup({ tenantMode }) {
  const isSingleTenant = tenantMode !== 'multi-tenant';

  const userSectionTitle = isSingleTenant ? 'MANUALLY ADD USERS' : 'INVITE USERS',
    userSectionLink = isSingleTenant
      ? 'https://links.sonatype.com/products/nxiq/doc/user-management/creating-a-user'
      : 'https://links.sonatype.com/products/nxiq/doc/firewall-saas-getting-started-on-cloud/user-management';

  return (
    <section id="system-setup" className="nx-tile" aria-labelledby="system-setup-title">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 id="system-setup-title" className="nx-h2">
            System Setup
          </h2>
        </div>
      </header>

      <div className="nx-tile-content">
        <p>Administrative tasks required to get up and running.</p>

        <hr className="nx-grid-h-keyline" />

        <div className="nx-grid-row">
          <div className="nx-grid-col nx-grid-col--50">
            {isSingleTenant && (
              <>
                <div>
                  <div className="nx-read-only__item">
                    <dt className="nx-read-only__label">Storage and Backup</dt>
                    <dd className="nx-read-only__data">
                      Ensure you have the required storage, and establish a data recovery plan.
                    </dd>
                  </div>
                  <section className="nx-grid-col__section">
                    <div className="nx-read-only__item">
                      <h4 className="nx-h4 nx-grid-header__title">SYSTEM REQUIREMENTS</h4>
                      <p className="nx-p">
                        <GettingStartedDocLink
                          href="http://links.sonatype.com/products/nxiq/doc/requirements"
                          text="Documentation"
                        />
                      </p>
                      <h4 className="nx-h4 nx-grid-header__title">BACKING UP IQ SERVER</h4>
                      <p className="nx-p">
                        <GettingStartedDocLink
                          href="http://links.sonatype.com/products/nxiq/doc/backup"
                          text="Documentation"
                        />
                      </p>
                    </div>
                  </section>
                </div>
                <hr className="nx-grid-h-keyline" />
              </>
            )}

            <div id="system-setup-adding-users">
              <div className="nx-read-only__item">
                <dt className="nx-read-only__label">Adding Users</dt>
                <dd className="nx-read-only__data">
                  Start in the system preferences menu. You have two ways to add users to IQ Server.
                </dd>
              </div>
              <section className="nx-grid-col__section">
                <div className="nx-read-only__item">
                  {isSingleTenant && (
                    <>
                      <h4 className="nx-h4 nx-grid-header__title">CONFIGURE LDAP</h4>
                      <p className="nx-p">
                        <GettingStartedDocLink
                          href="http://links.sonatype.com/products/nxiq/doc/ldap-integration"
                          text="Documentation"
                        />
                      </p>
                      <h4 className="nx-h4 nx-grid-header__title">CONFIGURE SAML</h4>
                      <p className="nx-p">
                        <GettingStartedDocLink
                          href="http://links.sonatype.com/products/nxiq/doc/saml-integration"
                          text="Documentation"
                        />
                      </p>
                    </>
                  )}
                  <h4 className="nx-h4 nx-grid-header__title">{userSectionTitle}</h4>
                  <p className="nx-p">
                    <GettingStartedDocLink
                      href={userSectionLink}
                      text="Documentation"
                      aria-label="Documentation for adding users"
                    />
                  </p>
                </div>
              </section>
            </div>
          </div>

          <div className="nx-grid-col nx-grid-col--50">
            <div>
              <div className="nx-read-only__item">
                <dt className="nx-read-only__label">Onboarding Applications</dt>
                <dd className="nx-read-only__data">Applications in IQ Server represent your development projects.</dd>
              </div>
              <section className="nx-grid-col__section">
                <div className="nx-read-only__item">
                  <h4 className="nx-h4 nx-grid-header__title">ADD APPLICATIONS MANUALLY</h4>
                  <p className="nx-p">Start in the Orgs and Apps page</p>
                  <p className="nx-p">
                    <GettingStartedDocLink
                      href="http://links.sonatype.com/products/nxiq/doc/application-management/creating-an-application"
                      text="Documentation"
                    />
                  </p>
                  <h4 className="nx-h4 nx-grid-header__title">ADD APPLICATIONS AUTOMATICALLY</h4>
                  <p className="nx-p">
                    <GettingStartedDocLink
                      href="http://links.sonatype.com/products/nxiq/doc/managing-automatic-applications"
                      text="Documentation"
                    />
                  </p>
                  {isSingleTenant && (
                    <>
                      <h4 className="nx-h4 nx-grid-header__title">REST API</h4>
                      <p className="nx-p">
                        Organizations:{' '}
                        <GettingStartedDocLink
                          href="http://links.sonatype.com/products/nxiq/doc/organization-rest-apis/v2/add-organization"
                          text="Documentation"
                        />
                      </p>
                      <p className="nx-p">
                        Applications:{' '}
                        <GettingStartedDocLink
                          href="http://links.sonatype.com/products/nxiq/doc/application-rest-apis/v2"
                          text="Documentation"
                        />
                      </p>
                    </>
                  )}
                </div>
              </section>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

SystemSetup.propTypes = {
  tenantMode: PropTypes.string,
};
