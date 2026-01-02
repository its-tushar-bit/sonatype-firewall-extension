/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.sbom.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;

import com.google.inject.Binder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(Parameterized.class)
public class S3SbomPersistenceServiceTest
    extends AbstractComponentTest
{
  private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:4.10.0");

  private static final String BUCKET_NAME = "test-sbom-bucket";

  private static final String REGION = "us-east-2";

  private static final String APP_ID = "test-app";

  private static final String FILE_NAME = "test-sbom.json.gz";

  @ClassRule
  public static LocalStackContainer localstack = new LocalStackContainer(LOCALSTACK_IMAGE).withServices(Service.S3);

  @BeforeClass
  public static void createBucket() {
    try (S3Client s3Client = createS3Client()) {
      s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
    }
  }

  private static S3Client createS3Client() {
    return S3Client.builder()
        .endpointOverride(localstack.getEndpoint())
        .region(Region.of(REGION))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(
                localstack.getAccessKey(),
                localstack.getSecretKey()
            )))
        .build();
  }

  @After
  public void cleanup() throws Exception {
    s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(BUCKET_NAME).build())
        .contents()
        .forEach(obj -> s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(BUCKET_NAME).key(obj.key()).build()));
  }

  @Inject
  protected InsightConfig insightConfig;

  @Inject
  private S3Client s3Client;

  private S3SbomPersistenceService service;

  private final String prefix;

  @Parameters
  public static List<String> prefixes() {
    return Arrays.asList(
        null,
        "",
        "valid-prefix/with/path/",
        "valid-prefix/with/path/ends-with-slash/"
    );
  }

  public S3SbomPersistenceServiceTest(String configuredPrefix) {
    this.prefix = configuredPrefix;
  }

  @Override
  protected void customizeConfig(InsightConfig insightConfig) {
    var storageConfig = insightConfig.getStorage();
    var s3Config = new S3DataStoreConfig();
    s3Config.setBucketName(BUCKET_NAME);
    s3Config.setRegion(REGION);
    s3Config.setObjectKeyPrefix(prefix);
    s3Config.setEndpoint(localstack.getEndpoint());
    storageConfig.setS3Config(s3Config);
    storageConfig.setType(DataStoreType.S3);
  }

  @Before
  public void setup() {
    service = lookup(S3SbomPersistenceService.class);
  }

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    AwsCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(
        localstack.getAccessKey(),
        localstack.getSecretKey()
    ));
    binder.bind(AwsCredentialsProvider.class).toInstance(awsCredentialsProvider);
  }

  @Test
  public void testServiceType() {
    assertThat(service).isInstanceOf(S3SbomPersistenceService.class);
  }

  @Test
  public void testDoGetSbom() {
    SbomEntity entity = service.doGetSbom(APP_ID, FILE_NAME);

    assertThat(entity).isInstanceOf(S3SbomEntity.class);
    assertThat(entity.getAppId()).isEqualTo(APP_ID);
    assertThat(entity.getName()).isEqualTo(FILE_NAME);
    assertThat(entity.getLocation()).contains(BUCKET_NAME);
    assertThat(entity.getLocation()).contains("sboms/" + APP_ID + "/" + FILE_NAME);
  }

  @Test
  public void testGetTemporarySbom() {
    String prefixId = "test-prefix";
    SbomEntity entity = service.getTemporarySbom(FILE_NAME, prefixId);

    assertThat(entity).isInstanceOf(S3SbomEntity.class);
    assertThat(entity.getAppId()).isNull();
    assertThat(entity.getName()).isEqualTo(FILE_NAME);
    assertThat(entity.getLocation()).contains("sboms/temp/persistent/" + prefixId + "/" + FILE_NAME);
  }

  @Test
  public void testGetTemporarySbomWithNullPrefix() {
    SbomEntity entity = service.getTemporarySbom(FILE_NAME, null);

    assertThat(entity).isInstanceOf(S3SbomEntity.class);
    assertThat(entity.getLocation()).contains("sboms/temp/persistent/" + FILE_NAME);
  }

  @Test
  public void testCreatePermanentSbom() throws IOException {
    SbomEntity entity = service.doGetSbom(APP_ID, FILE_NAME);

    assertThat(entity).isInstanceOf(S3SbomEntity.class);
    assertThat(entity.getAppId()).isEqualTo(APP_ID);
    assertThat(entity.getName()).isEqualTo(FILE_NAME);
    assertThat(entity.getLocation()).contains("sboms/" + APP_ID + "/" + FILE_NAME);

    assertThat(entity.exists()).isFalse();

    try (var outputStream = entity.getOutputStream()) {
      outputStream.write("test content".getBytes());
    }

    assertThat(entity.exists()).isTrue();
  }

  @Test
  public void testCreateTransientSbom() {
    SbomEntity entity = service.getTransientSbom(FILE_NAME);

    assertThat(entity).isInstanceOf(S3SbomEntity.class);
    assertThat(entity.getAppId()).isNull();
    assertThat(entity.getName()).isNotEqualTo(FILE_NAME);
    assertThat(entity.getName()).startsWith("sbom-");
    assertThat(entity.getLocation()).isEqualTo(
        "s3://" + BUCKET_NAME + "/" + (prefix == null ? "" : prefix) + "sboms/temp/transient/" + entity.getName()
    );
  }

  @Test
  public void testSaveTemporarySbom() throws IOException {
    SbomEntity sourceEntity = service.getTransientSbom("source.json.gz");

    try (var outputStream = sourceEntity.getOutputStream()) {
      outputStream.write("test SBOM content".getBytes());
    }

    String targetFileName = "target.json.gz";
    String prefixId = "test-prefix";
    SbomEntity resultEntity = service.saveTemporarySbom(sourceEntity, targetFileName, prefixId);

    assertThat(resultEntity).isInstanceOf(S3SbomEntity.class);
    assertThat(resultEntity.getAppId()).isEqualTo(sourceEntity.getAppId());
    assertThat(resultEntity.getName()).isEqualTo(targetFileName);
    assertThat(resultEntity.getLocation()).contains("sboms/temp/persistent/" + prefixId + "/" + targetFileName);
  }

  @Test
  public void testDeleteSbomByEntity() throws IOException {
    SbomEntity entity = service.doGetSbom(APP_ID, FILE_NAME);
    try (var outputStream = entity.getOutputStream()) {
      outputStream.write("test content".getBytes());
    }

    try (var inputStream = entity.getInputStream()) {
      assertThat(inputStream.readAllBytes()).isNotEmpty();
    }

    service.deleteSbom(entity);

    // Verify it's deleted by attempting to access it
    SbomEntity deletedEntity = service.doGetSbom(APP_ID, FILE_NAME);
    assertThatThrownBy(() -> deletedEntity.getInputStream())
        .isInstanceOf(IOException.class);
  }

  @Test
  public void testDeleteSbomByAppIdAndFileName() throws IOException {
    SbomEntity entity = service.doGetSbom(APP_ID, FILE_NAME);
    try (var outputStream = entity.getOutputStream()) {
      outputStream.write("test content".getBytes());
    }

    try (var inputStream = entity.getInputStream()) {
      assertThat(inputStream.readAllBytes()).isNotEmpty();
    }

    service.deleteSbom(APP_ID, FILE_NAME);

    SbomEntity deletedEntity = service.doGetSbom(APP_ID, FILE_NAME);
    assertThatThrownBy(() -> deletedEntity.getInputStream())
        .isInstanceOf(IOException.class);
  }

  @Test
  public void testDeleteSbomsFor() throws IOException {
    SbomEntity entity1 = service.doGetSbom(APP_ID, "sbom1.json.gz");
    SbomEntity entity2 = service.doGetSbom(APP_ID, "sbom2.json.gz");
    SbomEntity entity3 = service.doGetSbom(APP_ID, "sbom3.xml.gz");

    try (var outputStream = entity1.getOutputStream()) {
      outputStream.write("sbom1 content".getBytes());
    }
    try (var outputStream = entity2.getOutputStream()) {
      outputStream.write("sbom2 content".getBytes());
    }
    try (var outputStream = entity3.getOutputStream()) {
      outputStream.write("sbom3 content".getBytes());
    }

    try (var inputStream = entity1.getInputStream()) {
      assertThat(inputStream.readAllBytes()).isNotEmpty();
    }
    try (var inputStream = entity2.getInputStream()) {
      assertThat(inputStream.readAllBytes()).isNotEmpty();
    }
    try (var inputStream = entity3.getInputStream()) {
      assertThat(inputStream.readAllBytes()).isNotEmpty();
    }

    service.deleteSbomsFor(APP_ID);

    // Verify all are deleted
    SbomEntity deletedEntity1 = service.doGetSbom(APP_ID, "sbom1.json.gz");
    SbomEntity deletedEntity2 = service.doGetSbom(APP_ID, "sbom2.json.gz");
    SbomEntity deletedEntity3 = service.doGetSbom(APP_ID, "sbom3.xml.gz");

    assertThatThrownBy(() -> deletedEntity1.getInputStream()).isInstanceOf(IOException.class);
    assertThatThrownBy(() -> deletedEntity2.getInputStream()).isInstanceOf(IOException.class);
    assertThatThrownBy(() -> deletedEntity3.getInputStream()).isInstanceOf(IOException.class);
  }

  @Test
  public void testDeleteTransientSbomsOlderThan() throws IOException {
    SbomEntity oldEntity1 = service.getTransientSbom("old-sbom.json.gz");
    SbomEntity oldEntity2 = service.getTransientSbom("another-old-sbom.xml.gz");

    try (var outputStream = oldEntity1.getOutputStream()) {
      outputStream.write("old sbom1 content".getBytes());
    }
    try (var outputStream = oldEntity2.getOutputStream()) {
      outputStream.write("old sbom2 content".getBytes());
    }

    try (var inputStream = oldEntity1.getInputStream()) {
      assertThat(inputStream.readAllBytes()).isNotEmpty();
    }
    try (var inputStream = oldEntity2.getInputStream()) {
      assertThat(inputStream.readAllBytes()).isNotEmpty();
    }

    // Delete transient SBOMs older than a future time (should catch all)
    Instant cutoffTime = Instant.now().plus(1, ChronoUnit.HOURS);
    service.deleteTransientSbomsOlderThan(cutoffTime);

    // Verify they are deleted
    assertThatThrownBy(() -> oldEntity1.getInputStream()).isInstanceOf(IOException.class);
    assertThatThrownBy(() -> oldEntity2.getInputStream()).isInstanceOf(IOException.class);
  }

  @Test
  public void testMoveSbomEntity() throws Exception {
    String content = "some content";
    SbomEntity from = service.getPermanentSbom(APP_ID, FILE_NAME);
    try (OutputStream outputStream = from.getOutputStream()) {
      outputStream.write(content.getBytes(StandardCharsets.UTF_8));
    }
    SbomEntity to = service.getPermanentSbom(APP_ID + "2", FILE_NAME);
    assertThat(from.exists()).isTrue();
    assertThat(to.exists()).isFalse();

    service.moveSbomEntity(from, to);

    assertThat(from.exists()).isFalse();
    assertThat(to.exists()).isTrue();
    try (InputStream inputStream = to.getInputStream()) {
      assertThat(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(content);
    }
  }
}
