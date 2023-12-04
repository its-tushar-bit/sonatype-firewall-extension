/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;

import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.io.IOUtils;
import org.cyclonedx.model.AttachmentText;
import org.cyclonedx.model.Swid;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class SbomIdentityUtilsSwidTest
{
  private final String filename;

  private final int tagVersion;

  private final boolean patch;

  private final String expectedOutput;

  public SbomIdentityUtilsSwidTest(
      final String filename,
      final int tagVersion,
      final boolean patch,
      final String expectedOutput)
  {
    this.filename = filename;
    this.tagVersion = tagVersion;
    this.patch = patch;
    this.expectedOutput = expectedOutput;
  }

  @Parameterized.Parameters(name = "{0} : {1} : {2}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {null, 0, false,
            "pkg:swid/acme-application@9.1.1?tag_id=swidgen-242eb18a-503e-ca37-393b-cf156ef09691_9.1.1"},
        {null, 1, true, "pkg:swid/acme-application@9.1.1?patch=true&" +
            "tag_id=swidgen-242eb18a-503e-ca37-393b-cf156ef09691_9.1.1&tag_version=1"},
        {"swid-software-creator-only.xml", 0, false, "pkg:swid/Acme%2C%20Inc./example.com/acme-application@9.1.1?" +
            "tag_id=swidgen-242eb18a-503e-ca37-393b-cf156ef09691_9.1.1"},
        {"swid-tag-creator-only.xml", 0, false, "pkg:swid/acme-application@9.1.1?tag_creator_name=Acme%2C%20Inc.&" +
            "tag_creator_regid=example.com&tag_id=swidgen-242eb18a-503e-ca37-393b-cf156ef09691_9.1.1"},
        {"swid-both-creators-same-data.xml", 0, false, "pkg:swid/Acme%2C%20Inc./example.com/acme-application@9.1.1?" +
            "tag_id=swidgen-242eb18a-503e-ca37-393b-cf156ef09691_9.1.1"},
        {"swid-both-creators-different-data.xml", 0, false,
            "pkg:swid/Acme%2C%20Inc./example.com/acme-application@9.1.1?" +
            "tag_creator_name=News%2C%20Corp.&tag_creator_regid=news.com&" +
            "tag_id=swidgen-242eb18a-503e-ca37-393b-cf156ef09691_9.1.1"}
    });
  }

  @Test
  public void testBuildPackageUrlFromSwid_tagCreatorOnly() throws IOException {
    Swid swid = createTestInstance();

    PackageUrlIdentifier purl = SbomIdentityUtils.buildPackageUrlFromSwid(swid);

    assertThat(purl).isNotNull();
    assertThat(purl.getPackageUrl()).isEqualTo(expectedOutput);
  }

  private Swid createTestInstance() throws IOException {
    Swid swid = new Swid();
    swid.setTagId("swidgen-242eb18a-503e-ca37-393b-cf156ef09691_9.1.1");
    swid.setName("acme-application");
    swid.setVersion("9.1.1");
    swid.setTagVersion(tagVersion);
    swid.setPatch(patch);
    if (filename != null) {
      AttachmentText text = createAttachmentText();
      swid.setAttachmentText(text);
    }
    return swid;
  }

  private AttachmentText createAttachmentText() throws IOException {
    URL resource = SbomIdentityUtilsSwidTest.class.getClassLoader()
        .getResource(SbomIdentityUtilsSwidTest.class.getSimpleName() + "/" + filename);
    assert resource != null;
    String content = IOUtils.toString(resource.openStream(), StandardCharsets.UTF_8);

    String encoded = Base64.getEncoder().encodeToString(content.getBytes());

    AttachmentText attachmentText = new AttachmentText();
    attachmentText.setEncoding("base64");
    attachmentText.setContentType("text/xml");
    attachmentText.setText(encoded);
    return attachmentText;
  }
}
