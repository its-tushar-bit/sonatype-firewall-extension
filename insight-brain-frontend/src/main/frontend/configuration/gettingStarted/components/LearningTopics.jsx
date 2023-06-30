/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as PropTypes from 'prop-types';
import GettingStartedDocLink from './GettingStartedDocLink';

export default function LearningTopics({ tenantMode }) {
  const isSingleTenant = tenantMode !== 'multi-tenant';

  return (
    <section id="learning-topics" className="nx-tile" aria-labelledby="learning-topics-title">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 id="learning-topics-title" className="nx-h2">
            Learning Topics
          </h2>
        </div>
      </header>

      <div className="nx-tile-content">
        <p>Concepts and features that will help you get the most out of IQ Server.</p>

        <hr className="nx-grid-h-keyline" />

        <div className="nx-grid-row">
          <div className="nx-grid-col nx-grid-col--50">
            <div>
              <section className="nx-grid-col__section">
                <div className="nx-read-only__item">
                  <h4 className="nx-h4 nx-grid-header__title">Policies</h4>
                  <p className="nx-p">Policies define the conditions under which Lifecycle will take action.</p>
                  <p className="nx-p">
                    <GettingStartedDocLink
                      href="http://links.sonatype.com/products/nxiq/doc/policy-management"
                      text="Documentation"
                    />
                  </p>

                  <hr className="nx-grid-h-keyline" />

                  <h4 className="nx-h4 nx-grid-header__title">Hierarchy and Inheritance</h4>
                  <p className="nx-p">
                    Organization hierarchy affects how policies, categories, and license threat groups inherit to your
                    applications.
                  </p>
                  <p className="nx-p">
                    <GettingStartedDocLink
                      href="http://links.sonatype.com/products/nxiq/doc/hierarchy-and-inheritance"
                      text="Documentation"
                    />
                  </p>
                  {isSingleTenant && (
                    <>
                      <hr className="nx-grid-h-keyline" />

                      <h4 className="nx-h4 nx-grid-header__title">Integrations</h4>
                      <p className="nx-p">IQ Server integrates with other tools in your CI/CD pipeline.</p>
                      <p className="nx-p">
                        <GettingStartedDocLink
                          href="http://links.sonatype.com/products/nxiq/doc/iq-server-integrations"
                          text="Documentation"
                        />
                      </p>
                    </>
                  )}
                </div>
              </section>
            </div>
          </div>

          <div className="nx-grid-col nx-grid-col--50">
            <div>
              <section className="nx-grid-col__section">
                <div className="nx-read-only__item">
                  <h4 className="nx-h4 nx-grid-header__title">Dashboard</h4>
                  <p className="nx-p">
                    The dashboard provides real time insight into your applications and components.
                  </p>
                  <p className="nx-p">
                    <GettingStartedDocLink
                      href="http://links.sonatype.com/products/nxiq/doc/dashboard"
                      text="Documentation"
                    />
                  </p>
                  {isSingleTenant && (
                    <>
                      <hr className="nx-grid-h-keyline" />

                      <h4 className="nx-h4 nx-grid-header__title">Evaluation Reports</h4>
                      <p className="nx-p">A report shows the results of a single Lifecycle evaluation.</p>
                      <p className="nx-p">
                        <GettingStartedDocLink
                          href="http://links.sonatype.com/products/nxiq/doc/application-composition-report"
                          text="Documentation"
                        />
                      </p>

                      <hr className="nx-grid-h-keyline" />

                      <h4 className="nx-h4 nx-grid-header__title">Success Metrics</h4>
                      <p className="nx-p">
                        Success Metrics provide a high-level summary of your usage of Sonatype Lifecycle.
                      </p>
                      <p className="nx-p">
                        <GettingStartedDocLink
                          href="http://links.sonatype.com/products/nxiq/doc/success-metrics"
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

LearningTopics.propTypes = {
  tenantMode: PropTypes.string,
};
