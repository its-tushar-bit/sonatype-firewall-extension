/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import com.sonatype.insight.SbomTaxonomy;
import org.cyclonedx.parsers.BomParserFactory;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Property;
import org.cyclonedx.parsers.Parser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SbomCycloneDxUtilsTest
{
  @Test
  public void testGetApplicationNameAndVersionSafely_noMetadata() throws Exception {
    Bom bom = getCycloneDxDocument("sbom-no-metadata.json");
    assertThat(SbomCycloneDxUtils.getApplicationNameSafely(bom)).isNull();
    assertThat(SbomCycloneDxUtils.getApplicationVersionSafely(bom)).isNull();
  }

  @Test
  public void testGetApplicationNameAndVersionSafely_Missing_AppName_AppVersion() throws Exception {
    Bom bom = getCycloneDxDocument("sbom-no-appName-appVersion.xml");
    assertThat(SbomCycloneDxUtils.getApplicationNameSafely(bom)).isNull();
    assertThat(SbomCycloneDxUtils.getApplicationVersionSafely(bom)).isNull();
  }

  @Test
  public void testGetApplicationNameAndVersionSafely() throws Exception {
    Bom bom = getCycloneDxDocument("sbom-with-metadata.xml");
    assertThat(SbomCycloneDxUtils.getApplicationNameSafely(bom)).isEqualTo("MyAppName");
    assertThat(SbomCycloneDxUtils.getApplicationVersionSafely(bom)).isEqualTo("1.0.1");
  }

  @Test
  public void testGetSbomCreationDetails_ToolChoice_Xml() throws Exception {
    Bom bom = getCycloneDxDocument("sbom-cyclonedx-with-metadata.xml");
    String actual = SbomCycloneDxUtils.getSbomCreationDetailsJson(bom);
    assertThat(actual).isEqualTo(expectedToolChoiceSbomMetadata());
  }

  @Test
  public void testGetSbomCreationDetails_Json_NoContactForSupplierAndManufacturer_Xml() throws Exception {
    Bom bom = getCycloneDxDocument("sbom-cyclonedx-with-metadata-no-contact.xml");
    String actual = SbomCycloneDxUtils.getSbomCreationDetailsJson(bom);
    assertThat(actual).isEqualTo(expectedNoContactSbomMetadata());
  }

  @Test
  public void testGetSbomCreationDetails_Json_ToolChoice_Json() throws Exception {
    Bom bom = getCycloneDxDocument("sbom-cyclonedx-with-metadata.json");
    String actual = SbomCycloneDxUtils.getSbomCreationDetailsJson(bom);
    assertThat(actual).isEqualTo(expectedToolChoiceSbomMetadata());
  }

  @Test
  public void testGetSbomCreationDetails_Json_NoContactForSupplierAndManufacturer_Json() throws Exception {
    Bom bom = getCycloneDxDocument("sbom-cyclonedx-with-metadata-no-contact.json");
    String actual = SbomCycloneDxUtils.getSbomCreationDetailsJson(bom);
    assertThat(actual).isEqualTo(expectedNoContactSbomMetadata());
  }

  @Test
  public void testGetSbomCreationDetails_LegacyTool_Xml() throws Exception {
    Bom bom = getCycloneDxDocument("sbom-cyclonedx-with-metadata-legacy-tools.xml");
    String actual = SbomCycloneDxUtils.getSbomCreationDetailsJson(bom);
    assertThat(actual).isEqualTo(expectedLegacyToolSbomMetadata());
  }

  @Test
  public void testGetSbomCreationDetails_LegacyTool_Json() throws Exception {
    Bom bom = getCycloneDxDocument("sbom-cyclonedx-with-metadata-legacy-tools.json");
    String actual = SbomCycloneDxUtils.getSbomCreationDetailsJson(bom);
    assertThat(actual).isEqualTo(expectedLegacyToolSbomMetadata());
  }

  @Test
  public void testGetSbomCreationDetails_OnlyCreators() throws Exception {
    Bom bom = getCycloneDxDocument("sbom-cyclonedx-with-metadata-only-creators.xml");
    String actual = SbomCycloneDxUtils.getSbomCreationDetailsJson(bom);
    assertThat(actual).isEqualTo(expectedOnlyCreatorsSbomMetadata());
  }

  @Test
  public void testGetSbomCreationDetails_OnlyCreators_NoManufactureContacts() throws Exception {
    Bom bom = getCycloneDxDocument("sbom-cyclonedx-with-metadata-only-creators-manufacture-null-contacts.xml");
    String actual = SbomCycloneDxUtils.getSbomCreationDetailsJson(bom);
    assertThat(actual).isEqualTo(expectedOnlyCreatorsSbomMetadataManufacturerNoContact());
  }

  @Test
  public void testGetSbomCreationDetails_OnlyCreators_NoSupplierContacts() throws Exception {
    Bom bom = getCycloneDxDocument("sbom-cyclonedx-with-metadata-only-creators-supplier-null-contacts.xml");
    String actual = SbomCycloneDxUtils.getSbomCreationDetailsJson(bom);
    assertThat(actual).isEqualTo(expectedOnlyCreatorsSbomMetadataSupplierNoContact());
  }

  @Test
  public void testGetSbomCreationDetails_OnlyTools() throws Exception {
    Bom bom = getCycloneDxDocument("sbom-cyclonedx-with-metadata-only-tools.xml");
    String actual = SbomCycloneDxUtils.getSbomCreationDetailsJson(bom);
    assertThat(actual).isEqualTo(expectedOnlyToolsSbomMetadata());
  }

  @Test
  public void testGetSbomCreationDetails_OnlyServiceTools() throws Exception {
    Bom bom = getCycloneDxDocument("sbom-cyclonedx-with-metadata-service-tools.json");
    String actual = SbomCycloneDxUtils.getSbomCreationDetailsJson(bom);
    assertThat(actual).isEqualTo(expectedOnlyServiceToolsSbomMetadata());
  }

  @Test
  public void testGetSbomCreationDetails_CompositeTools() throws Exception {
    Bom bom = getCycloneDxDocument("sbom-cyclonedx-with-metadata-composite-tools.json");
    String actual = SbomCycloneDxUtils.getSbomCreationDetailsJson(bom);
    assertThat(actual).isEqualTo(expectedCompositeToolsSbomMetadata());
  }

  @Test
  public void testGetSbomCreationDetails_NoMetadata() throws Exception {
    Bom bom = getCycloneDxDocument("sbom-no-metadata.json");
    String actual = SbomCycloneDxUtils.getSbomCreationDetailsJson(bom);
    assertThat(actual).isNullOrEmpty();
  }

  @Test
  public void testGetSonatypeTruncatedSha1_NullComponent() {
    String result = SbomCycloneDxUtils.getSonatypeTruncatedSha1(null);
    assertThat(result).isNull();
  }

  @Test
  public void testGetSonatypeTruncatedSha1_ComponentWithNoProperties() {
    Component component = new Component();
    String result = SbomCycloneDxUtils.getSonatypeTruncatedSha1(component);
    assertThat(result).isNull();
  }

  @Test
  public void testGetSonatypeTruncatedSha1_ComponentWithPropertiesButNoSha1() {
    Component component = new Component();
    Property property = new Property();
    property.setName("someOtherProperty");
    property.setValue("someValue");
    component.addProperty(property);

    String result = SbomCycloneDxUtils.getSonatypeTruncatedSha1(component);
    assertThat(result).isNull();
  }

  @Test
  public void testGetSonatypeTruncatedSha1_ComponentWithMultiplePropertiesIncludingSha1() {
    Component component = new Component();

    Property otherProperty = new Property();
    otherProperty.setName("someOtherProperty");
    otherProperty.setValue("someValue");
    component.addProperty(otherProperty);

    Property sha1Property = new Property();
    sha1Property.setName(SbomTaxonomy.CDX_SONATYPE_SHA1_PROPERTY_NAME);
    sha1Property.setValue("efgh5678");
    component.addProperty(sha1Property);

    Property anotherProperty = new Property();
    anotherProperty.setName("anotherProperty");
    anotherProperty.setValue("anotherValue");
    component.addProperty(anotherProperty);

    String result = SbomCycloneDxUtils.getSonatypeTruncatedSha1(component);
    assertThat(result).isEqualTo("efgh5678");
  }

  private static Bom getCycloneDxDocument(
      final String fileName) throws IOException, ParseException, URISyntaxException
  {
    URL resource = SbomCycloneDxUtilsTest.class.getResource("/SbomCycloneDxUtilsTest/" + fileName);
    String content =
        new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(resource).toURI())), StandardCharsets.UTF_8);
    byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(contentBytes);
    return parser.parse(contentBytes);
  }

  private String expectedToolChoiceSbomMetadata() {
    return "{\"type\":\"application\",\"created\":\"2024-02-29T23:41:22Z\",\"creators\":[{\"type\":\"Author\"," +
        "\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"phone\":\"1-800-111-1111\"},{\"type\":" +
        "\"Manufacturer\",\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"phone\":\"1-800-111-1111\"," +
        "\"url\":\"example.com,example2.com,example3.com\"},{\"type\":\"Manufacturer\",\"name\":\"Jane Doe\"," +
        "\"email\":\"jane.doe@example.com\",\"phone\":\"1-800-222-2222\",\"url\":\"example.com,example2.com," +
        "example3.com\"},{\"type\":\"Supplier\",\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"phone\"" +
        ":\"1-800-111-1111\",\"url\":\"example.com,example2.com,example3.com\"},{\"type\":\"Supplier\",\"name\"" +
        ":\"Jane Doe\",\"email\":\"jane.doe@example.com\",\"phone\":\"1-800-222-2222\",\"url\":\"example.com," +
        "example2.com,example3.com\"}],\"tools\":[{\"type\":\"application\",\"name\":\"Tool\"," +
        "\"version\":\"1.0-RELEASE\"}]}";
  }

  private String expectedNoContactSbomMetadata() {
    return "{\"type\":\"application\",\"created\":\"2024-02-29T23:41:22Z\",\"creators\":[{\"type\":\"Author\"," +
        "\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"phone\":\"1-800-111-1111\"},{\"type\":" +
        "\"Manufacturer\",\"name\":\"manufacturer\",\"url\":\"example.com,example2.com,example3.com\"}," +
        "{\"type\":\"Supplier\",\"name\":\"supplier\",\"url\":\"example.com,example2.com,example3.com\"}]," +
        "\"tools\":[{\"type\":\"application\",\"name\":\"Tool\"," +
        "\"version\":\"1.0-RELEASE\"}]}";
  }

  private String expectedLegacyToolSbomMetadata() {
    return "{\"type\":\"application\",\"created\":\"2024-02-29T23:41:22Z\",\"creators\":[{\"type\":\"Author\"" +
        ",\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"phone\":\"1-800-111-1111\"},{\"type\":" +
        "\"Manufacturer\",\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"phone\":\"1-800-111-1111\"," +
        "\"url\":\"example.com,example2.com,example3.com\"},{\"type\":\"Manufacturer\",\"name\":\"Jane Doe\"," +
        "\"email\":\"jane.doe@example.com\",\"phone\":\"1-800-222-2222\",\"url\":\"example.com,example2.com," +
        "example3.com\"},{\"type\":\"Supplier\",\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"phone\"" +
        ":\"1-800-111-1111\",\"url\":\"example.com,example2.com,example3.com\"},{\"type\":\"Supplier\",\"name\":" +
        "\"Jane Doe\",\"email\":\"jane.doe@example.com\",\"phone\":\"1-800-222-2222\",\"url\":\"example.com," +
        "example2.com,example3.com\"}],\"tools\":[{\"name\":\"Tool\",\"version\":\"1.0-RELEASE\"}]}";
  }

  private String expectedOnlyCreatorsSbomMetadata() {
    return "{\"type\":\"application\",\"created\":\"2024-02-29T23:41:22Z\",\"creators\":[{\"type\":\"Author\"" +
        ",\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"phone\":\"1-800-111-1111\"}]}";
  }

  private String expectedOnlyCreatorsSbomMetadataManufacturerNoContact() {
    return "{\"type\":\"application\",\"created\":\"2024-02-29T23:41:22Z\",\"creators\":[{\"type\":\"Author\"" +
        ",\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"phone\":\"1-800-111-1111\"}," +
        "{\"type\":\"Manufacturer\",\"name\":\"John Doe Inc.\"}]}";
  }

  private String expectedOnlyCreatorsSbomMetadataSupplierNoContact() {
    return "{\"type\":\"application\",\"created\":\"2024-02-29T23:41:22Z\",\"creators\":[{\"type\":\"Author\"" +
        ",\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"phone\":\"1-800-111-1111\"}," +
        "{\"type\":\"Supplier\",\"name\":\"John Doe Inc.\"}]}";
  }

  private String expectedOnlyToolsSbomMetadata() {
    return "{\"type\":\"application\",\"created\":\"2024-02-29T23:41:22Z\",\"tools\":[{\"type\":\"application\"" +
        ",\"name\":\"Tool\",\"version\":\"1.0-RELEASE\"}]}";
  }

  private String expectedOnlyServiceToolsSbomMetadata() {
    return "{\"type\":\"application\",\"created\":\"2024-03-14T19:06:34Z\",\"tools\":[{\"name\":" +
        "\"insight-brain-service\",\"version\":\"2.36.79-01\"}]}";
  }

  private String expectedCompositeToolsSbomMetadata() {
    return "{\"type\":\"application\",\"created\":\"2024-03-14T19:06:34Z\",\"tools\":[{\"type\":\"framework\"," +
        "\"name\":\"framework\",\"version\":\"2.1.1-01\"},{\"type\":\"application\",\"name\":" +
        "\"insight-brain-service\",\"version\":\"1.36.79-01\"},{\"name\":\"service\",\"version\":\"2.0.0\"}," +
        "{\"name\":\"insight-brain-service\",\"version\":\"2.36.79-01\"}]}";
  }
}
