/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.sbom.utils.SbomSpdxUtils;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.scan.file.SbomFormat;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.spdx.library.DefaultModelStore;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.SpdxConstants;
import org.spdx.library.model.ExternalRef;
import org.spdx.library.model.ReferenceType;
import org.spdx.library.model.Relationship;
import org.spdx.library.model.SpdxCreatorInformation;
import org.spdx.library.model.SpdxDocument;
import org.spdx.library.model.SpdxPackage;
import org.spdx.library.model.SpdxPackage.SpdxPackageBuilder;
import org.spdx.library.model.enumerations.ReferenceCategory;
import org.spdx.library.model.enumerations.RelationshipType;
import org.spdx.library.model.license.AnyLicenseInfo;
import org.spdx.library.model.license.ExtractedLicenseInfo;
import org.spdx.library.model.license.ListedLicenses;
import org.spdx.library.model.license.SpdxListedLicense;

@Named
public class SpdxToSpdxExporter
    extends AbstractSbomExporter
{
  private static final String LICENSE_REF_PREFIX = "LicenseRef-";

  private final BaseUrl baseUrl;

  private final IdUtils idUtils;

  private final VersionService versionService;

  @Inject
  protected SpdxToSpdxExporter(
      final InsightWork insightWork,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO,
      final BaseUrl baseUrl,
      final IdUtils idUtils,
      final VersionService versionService)
  {
    super(insightWork, thirdPartyFileCoordinateDAO, thirdPartyCoordinateSecurityDAO, thirdPartyCoordinateLicenseDAO,
        thirdPartyVulnerabilityExploitabilityExchangeDAO);
    this.baseUrl = baseUrl;
    this.idUtils = idUtils;
    this.versionService = versionService;
  }

  @Override
  public String export() {
    try (InputStream gis = new GZIPInputStream(Files.newInputStream(getOriginalSbomFile().toPath()))) {
      SpdxDocument originalDocument = SbomSpdxUtils.parseContentStreamNoValidation(gis,
          SbomFormat.forString(exportParams.sbomMetadata.getSpecFormat()));
      SpdxDocument newDocument = createNewDocumentFrom(originalDocument);
      return generateTargetSbomString(newDocument);
    }
    catch (InvalidSPDXAnalysisException | IOException e) {
      throw new SbomExportException(
          String.format("Internal error reading from the original SBOM file for application %s, version %s",
              exportParams.sbomMetadata.getApplicationId(), exportParams.sbomMetadata.getSbomVersion()), e);
    }
  }

  private SpdxDocument createNewDocumentFrom(
      final SpdxDocument originalDocument) throws InvalidSPDXAnalysisException
  {
    DefaultModelStore.reset();
    SpdxDocument newDocument = new SpdxDocument(getBillOfMaterialsPath());
    newDocument.setSpecVersion("SPDX-" + exportParams.exportSpecification.getVersion());
    newDocument.setName(idUtils.getPublicOwnerId(OwnerType.APPLICATION, exportParams.sbomMetadata.getApplicationId()));
    newDocument.setDataLicense(new SpdxListedLicense(SpdxConstants.SPDX_DATA_LICENSE_ID));

    SpdxCreatorInformation creatorInfo = new SpdxCreatorInformation();
    DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd")
        .appendLiteral('T')
        .appendPattern("HH:mm:ss")
        .appendLiteral('Z');
    String date = LocalDateTime.now(ZoneOffset.UTC).format(builder.toFormatter());

    creatorInfo.setCreated(date);
    creatorInfo.getCreators().add("Tool: Sonatype SBOM Manager - " + versionService.getFullVersion());
    //not adding data date (for NDE customers) until is needed.
    newDocument.setCreationInfo(creatorInfo);
    SpdxPackage rootPackage = SbomSpdxUtils.getRootPackage(originalDocument);
    copyComponents(originalDocument, newDocument, rootPackage);
    newDocument.setExternalDocumentRefs(originalDocument.getExternalDocumentRefs());
    return newDocument;
  }

  private String getBillOfMaterialsPath() {
    String iqBaseUrl = "";
    try {
      iqBaseUrl = this.baseUrl.get();
    }
    catch (IllegalStateException e) {
      log.warn("SBOM Manager base URL is not configured", e);
    }
    return iqBaseUrl + UserInterfaceLinksHelper.getSBOMBillOfMaterialPath(
        idUtils.getPublicOwnerId(OwnerType.APPLICATION, exportParams.sbomMetadata.getApplicationId()),
        exportParams.sbomMetadata.getSbomVersion());
  }

  private ExternalRef newVulnerabilityRefFor(SpdxDocument document, final ThirdPartyCoordinateSecurity dbVulnerability)
      throws InvalidSPDXAnalysisException
  {
    String source = dbVulnerability.getVulnerabilitySource();
    String comment = null;
    if (StringUtils.isNotBlank(source)) {
      comment = "source: " + source.toUpperCase(Locale.ROOT);
    }
    return document.createExternalRef(ReferenceCategory.SECURITY,
        new ReferenceType("advisory"), dbVulnerability.getLink(), comment);
  }

  private void copyComponents(
      final SpdxDocument originalDocument,
      final SpdxDocument newDocument,
      final SpdxPackage originalRootPkg)
      throws InvalidSPDXAnalysisException
  {
    SpdxPackage newRootPkg = null;
    List<SpdxPackage> potentialDirects = new ArrayList<>();
    List<String> transitives = new ArrayList<>();
    Map<String, ExtractedLicenseInfo> extractedLicenses = new HashMap<>();
    for (SpdxPackage pkg : SbomSpdxUtils.getAllPackages(originalDocument)) {
      // a valid existing package will always have a name. It is unlikely to have this "unknown package" to get called
      String pkgName = pkg.getName().orElse("UNKNOWN PACKAGE");

      SpdxPackageBuilder pkgBuilder = newDocument
          .createPackage(pkg.getId(), pkgName, pkg.getLicenseConcluded(), pkg.getCopyrightText(),
              pkg.getLicenseDeclared())
          //files needs to be analyzed in order for any license information to be added
          .setFilesAnalyzed(true);
      pkg.getDownloadLocation().ifPresent(pkgBuilder::setDownloadLocation);

      ThirdPartyFileCoordinate matchingDbComponent = getMatchingDbComponent(pkg);
      Collection<ExternalRef> externalRefs = pkg.getExternalRefs();
      if (matchingDbComponent != null) {
        pkgBuilder.setLicenseInfosFromFile(getAllLicenseInfoFromDb(matchingDbComponent, extractedLicenses));
        addVulnerabilityDiffsFromDatabase(newDocument, pkgBuilder, externalRefs, matchingDbComponent);
      }
      externalRefs.forEach(pkgBuilder::addExternalRef);

      pkg.getVersionInfo().ifPresent(pkgBuilder::setVersionInfo);
      pkg.getChecksums().forEach(pkgBuilder::addChecksum);
      pkg.getDescription().ifPresent(pkgBuilder::setDescription);
      pkg.getHomepage().ifPresent(pkgBuilder::setHomepage);
      pkg.getOriginator().ifPresent(pkgBuilder::setOriginator);
      pkg.getPackageFileName().ifPresent(pkgBuilder::setPackageFileName);
      pkg.getPackageVerificationCode().ifPresent(pkgBuilder::setPackageVerificationCode);
      pkg.getPrimaryPurpose().ifPresent(pkgBuilder::setPrimaryPurpose);
      pkg.getReleaseDate().ifPresent(pkgBuilder::setReleaseDate);
      pkg.getSourceInfo().ifPresent(pkgBuilder::setSourceInfo);
      pkg.getSupplier().ifPresent(pkgBuilder::setSupplier);
      pkg.getValidUntilDate().ifPresent(pkgBuilder::setValidUntilDate);
      pkg.getComment().ifPresent(pkgBuilder::setComment);
      pkg.getFiles().forEach(pkgBuilder::addFile);
      pkg.getAnnotations().forEach(pkgBuilder::addAnnotation);

      //pre-process relationships
      if (StringUtils.equals(originalRootPkg.getId(), pkg.getId())) {
        newRootPkg = buildPackage(pkgBuilder);
      }
      else {
        Collection<Relationship> relationships = pkg.getRelationships();
        if (CollectionUtils.isNotEmpty(relationships)) {
          for (Relationship relationship : relationships) {
            pkgBuilder.addRelationship(relationship);
          }
          potentialDirects.add(buildPackage(pkgBuilder));
        }
        else {
          transitives.add(buildPackage(pkgBuilder).getId());
        }
      }
    }

    //any non-SPDX licenses added from DB (extracted) needs to be referenced at document level too.
    // Otherwise the document is considered invalid.
    newDocument.getExtractedLicenseInfos().addAll(extractedLicenses.values());
    //For some (weird) reason, copying the original dependency graph as is to the new document does not generate the
    // same dependency graph by the SDK. Hence, the creation of the graph manually.
    addDependencyRelationships(newDocument, newRootPkg, potentialDirects, transitives);
  }

  private static void addDependencyRelationships(
      final SpdxDocument newDocument,
      final SpdxPackage newRootPkg,
      final List<SpdxPackage> potentialDirects,
      final List<String> transitives) throws InvalidSPDXAnalysisException
  {
    if (newRootPkg != null) {
      newDocument.getDocumentDescribes().add(newRootPkg);
      for (SpdxPackage potentialDirect : potentialDirects) {
        if (!transitives.contains(potentialDirect.getId())) {
          //true direct
          Relationship relationship =
              newDocument.createRelationship(potentialDirect, RelationshipType.DEPENDS_ON, null);
          newRootPkg.addRelationship(relationship);
        }
      }
    }
  }

  private SpdxPackage buildPackage(final SpdxPackageBuilder pkgBuilder) throws InvalidSPDXAnalysisException {
    return pkgBuilder.build();
  }

  private void addVulnerabilityDiffsFromDatabase(
      final SpdxDocument document,
      final SpdxPackageBuilder pkgBuilder,
      final Collection<ExternalRef> externalRefs,
      final ThirdPartyFileCoordinate dbComponent) throws InvalidSPDXAnalysisException
  {
    List<ThirdPartyCoordinateSecurity> dbVulnerabilities =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(dbComponent.getId());
    for (ThirdPartyCoordinateSecurity dbVulnerability : dbVulnerabilities) {
      if (vulnerabilityAlreadyExists(externalRefs, dbVulnerability.getRefId())) {
        continue;
      }
      //new vulnerability not in original sbom. adding it
      pkgBuilder.addExternalRef(newVulnerabilityRefFor(document, dbVulnerability));
    }
  }

  private boolean vulnerabilityAlreadyExists(final Collection<ExternalRef> externalRefs, final String refId)
      throws InvalidSPDXAnalysisException
  {
    for (ExternalRef externalRef : externalRefs) {
      if (StringUtils.equals(SbomSpdxUtils.getRefIdForVulnerability(externalRef), refId)) {
        return true;
      }
    }
    return false;
  }

  private Collection<AnyLicenseInfo> getAllLicenseInfoFromDb(
      final ThirdPartyFileCoordinate dbComponent,
      final Map<String, ExtractedLicenseInfo> extractedLicenses)
  {
    List<ThirdPartyCoordinateLicense> licenses =
        thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(dbComponent.getId());
    return licenses.stream().map(license -> createLicenseObject(license.getLicenseId(), extractedLicenses))
        .collect(Collectors.toSet());
  }

  private ThirdPartyFileCoordinate getMatchingDbComponent(final SpdxPackage pkg) {
    try {
      String purl = SbomSpdxUtils.getPurl(pkg);
      return thirdPartyFileCoordinateDAO.getByThirdPartyFileIdAndPackageUrl(
          exportParams.sbomMetadata.getThirdPartyFileId(), purl);
    }
    catch (InvalidSPDXAnalysisException e) {
      throw new SbomExportException("error determining purl for pkg", e);
    }
  }

  private AnyLicenseInfo createLicenseObject(
      final String licenseId,
      final Map<String, ExtractedLicenseInfo> extractedLicenses)
  {
    try {
      if (ListedLicenses.getListedLicenses().isSpdxListedLicenseId(licenseId)) {
        return ListedLicenses.getListedLicenses().getListedLicenseById(licenseId);
      }
      ExtractedLicenseInfo extractedLicenseInfo = new ExtractedLicenseInfo(LICENSE_REF_PREFIX + licenseId, licenseId);
      extractedLicenses.put(extractedLicenseInfo.getLicenseId(), extractedLicenseInfo);
      return extractedLicenseInfo;
    }
    catch (InvalidSPDXAnalysisException e) {
      throw new SbomExportException("Internal error extracting license information for " + licenseId, e);
    }
  }
}
