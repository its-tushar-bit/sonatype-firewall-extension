/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LabelColorMigratorTest
{
  private static Map<Color, Color> migrationMap;

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  // Using a mock for LabelDAO as new validation prohibits the deprecated colours
  @Mock(answer = Answers.RETURNS_MOCKS)
  private LabelDAO labelDAO;

  @Mock
  private InsightWork insightWork;

  private LabelColorMigrator migrator;

  @SuppressWarnings("deprecation")
  @BeforeClass
  public static void setupMigrationMap() {
    Map<Color, Color> map = new HashMap<>();
    map.put(Color.white, Color.light_green);
    map.put(Color.grey, Color.light_purple);
    map.put(Color.black, Color.dark_purple);
    map.put(Color.green, Color.dark_green);
    map.put(Color.red, Color.dark_red);
    map.put(Color.blue, Color.dark_blue);

    migrationMap = Collections.unmodifiableMap(map);
  }

  @Before
  public void setup() throws IOException {
    when(labelDAO.getAll(any(TransactionContext.class))).thenReturn(createLabels());
    when(insightWork.getWorkDir()).thenReturn(tempDir.newFolder());

    migrator = new LabelColorMigrator(insightWork, labelDAO);
  }

  @Test
  public void testMigration() throws IOException {
    migrator.migrate();

    List<Label> labels = createLabels();
    verify(labelDAO, times(labels.size())).update(any(TransactionContext.class), any(Label.class));
    for (Label label : labels) {
      label.setColor(migrationMap.get(label.getColor()));
      verify(labelDAO).update(any(TransactionContext.class), argThat(matcher(label)));
    }

    assertTrue(markerFile().isFile());
  }

  @Test
  public void testSkipMigration() throws IOException {
    markerFile().createNewFile();

    migrator.migrate();

    verify(labelDAO, never()).update(any(TransactionContext.class), any(Label.class));
  }

  private File markerFile() {
    return new File(insightWork.getWorkDir(), LabelColorMigrator.MARKER_FILE_NAME);
  }

  private List<Label> createLabels() {
    List<Label> labels = new ArrayList<>();
    for (Color source : migrationMap.keySet()) {
      Label label = new Label(source.name(), source.name());
      label.setColor(source);
      label.setId(source.name());
      labels.add(label);
    }
    return labels;
  }

  private Matcher<Label> matcher(final Label expected) {
    return new BaseMatcher<Label>() {

      @Override
      public boolean matches(Object item) {
        if (item instanceof Label) {
          Label label = (Label) item;
          return expected.getId().equals(label.getId()) && expected.getColor().equals(label.getColor());
        }
        return false;
      }

      @Override
      public void describeTo(Description description) {
        
      }
    };
  }
}
