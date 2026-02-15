/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiCompositeSourceControlDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.git.IqForScmLicenseChecker;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.security.CipherFactory;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.security.EncryptionKeyStore;
import com.sonatype.insight.brain.security.TestFipsEncryptionKeyStore;
import com.sonatype.nexus.scm.SourceControlProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import jakarta.inject.Inject;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;

public class ApiCompositeSourceControlServiceFipsTest extends ApiCompositeSourceControlServiceTest
{
  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private IqForScmLicenseChecker iqForScmLicenseChecker;

  @Inject
  private OwnerDAO ownerDAO;

  @Inject
  private GitHubAppDAO gitHubAppDAO;

  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  private static final String TOKEN = "thisisafipstoken";

  private static final String ROOT_TOKEN = "root-token-fips";

  @Before
  @Override
  public void setup() throws Exception {
    // Ensure that the Bouncy Castle FIPS provider is inserted before the tests.
    insertBouncyCastleFipsProvider();

    // Initialize the EnvironmentVariables here instead of as a class variable as this gets run as part of a JUnit rule
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    super.setup();
  }

  @Override
  @Test
  public void getCompositeSourceControlByOwnerDecrypted() throws Exception {
    EncryptionKeyStore keyStore = new TestFipsEncryptionKeyStore();
    ApiCompositeSourceControlService apiCompositeSourceControlServiceLocal =
        new ApiCompositeSourceControlService(sourceControlDAO, gitHubAppDAO, applicationDAO,
            iqForScmLicenseChecker, organizationDAO, ownerDAO, keyStore);
    PlexusCipher plexusCipherLocal = CipherFactory.createCipher();
    setUpRootOrg(plexusCipherLocal, keyStore.getKey());

    Organization level1OrgLocal = tempEntity.newOrganization();

    tempEntity.newSourceControl(level1OrgLocal.getId(), null, plexusCipherLocal.encrypt(TOKEN,
        keyStore.getKey()), null);

    // when we get source control decrypted
    ApiCompositeSourceControlDTO dto =
        apiCompositeSourceControlServiceLocal.getCompositeSourceControlByOwnerDecrypted(
            OwnerType.ORGANIZATION,
            level1OrgLocal.getId()
        );

    // then the passwords at both levels match
    assertThat(dto.token.value).isEqualTo(TOKEN);
    assertThat(dto.token.parentValue).isEqualTo(ROOT_TOKEN);

    // clean up for following tests
    setUpRootOrg(plexusCipher, ENC);
  }

  private void setUpRootOrg(PlexusCipher plexusCipherRoot, String encryptionKey) {
    try {
      SourceControl rootSourceControl = sourceControlDAO.getByOwnerId(ROOT_ORGANIZATION_ID);
      if (rootSourceControl != null) {
        sourceControlDAO.delete(rootSourceControl);
      }
      rootOrgSourcecontrol = tempEntity
          .newSourceControl(ROOT_ORGANIZATION_ID, null, plexusCipherRoot.encrypt(ROOT_TOKEN, encryptionKey),
              SourceControlProvider.GITHUB);
      rootOrganization = organizationDAO.getById(ROOT_ORGANIZATION_ID);
    }
    catch (PlexusCipherException e) {
      throw new RuntimeException("Failed to set up root organization source control", e);
    }
  }
}
