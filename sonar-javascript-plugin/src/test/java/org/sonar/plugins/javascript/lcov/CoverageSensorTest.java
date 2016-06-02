/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.javascript.lcov;

import java.io.File;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.Settings;
import org.sonar.api.internal.google.common.base.Charsets;
import org.sonar.plugins.javascript.JavaScriptPlugin;

import static org.fest.assertions.Assertions.assertThat;

public class CoverageSensorTest {

  private SensorContextTester context;
  private Settings settings;

  private UTCoverageSensor utCoverageSensor = new UTCoverageSensor();
  private ITCoverageSensor itCoverageSensor = new ITCoverageSensor();
  private OverallCoverageSensor overallCoverageSensor = new OverallCoverageSensor();
  private File moduleBaseDir = new File("src/test/resources/coverage/");

  @Before
  public void init() {
    settings = new Settings();
    settings.setProperty(JavaScriptPlugin.LCOV_UT_REPORT_PATH, "reports/report_ut.lcov");
    settings.setProperty(JavaScriptPlugin.LCOV_IT_REPORT_PATH, "reports/report_it.lcov");
    context = SensorContextTester.create(moduleBaseDir);
    context.setSettings(settings);

    inputFile("file1.js", InputFile.Type.MAIN);
    inputFile("file2.js", InputFile.Type.MAIN);
    inputFile("tests/file1.js", Type.TEST);
  }

  private InputFile inputFile(String relativePath, Type type) {
    DefaultInputFile inputFile = new DefaultInputFile("moduleKey", relativePath)
      .setModuleBaseDir(moduleBaseDir.toPath())
      .setLanguage("js")
      .setType(type);

    inputFile.initMetadata(new FileMetadata().readMetadata(inputFile.file(), Charsets.UTF_8));
    context.fileSystem().add(inputFile);

    return inputFile;
  }

  @Test
  public void sensor_descriptor() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();

    utCoverageSensor.describe(descriptor);
    assertThat(descriptor.name()).isEqualTo("UT Coverage Sensor");
    assertThat(descriptor.languages()).containsOnly("js");
    assertThat(descriptor.type()).isEqualTo(Type.MAIN);

    itCoverageSensor.describe(descriptor);
    assertThat(descriptor.name()).isEqualTo("IT Coverage Sensor");
    assertThat(descriptor.languages()).containsOnly("js");
    assertThat(descriptor.type()).isEqualTo(Type.MAIN);

    overallCoverageSensor.describe(descriptor);
    assertThat(descriptor.name()).isEqualTo("Overall Coverage Sensor");
    assertThat(descriptor.languages()).containsOnly("js");
    assertThat(descriptor.type()).isEqualTo(Type.MAIN);
  }

  @Test
  public void report_not_found() throws Exception {
    settings.setProperty(JavaScriptPlugin.LCOV_UT_REPORT_PATH, "/fake/path/lcov_report.dat");

    utCoverageSensor.execute(context);

    // expected logged text: "No coverage information will be saved because all LCOV files cannot be found."
    assertThat(context.lineHits("moduleKey:file1.js", CoverageType.UNIT, 1)).isNull();
  }

  @Test
  public void test_ut_coverage() {
    utCoverageSensor.execute(context);
    Integer[] file1Expected = {2, 2, 1, null};
    Integer[] file2Expected = {5, 5, null, null};

    for (int line = 1; line <= 4; line++) {
      assertThat(context.lineHits("moduleKey:file1.js", CoverageType.UNIT, line)).isEqualTo(file1Expected[line - 1]);
      assertThat(context.lineHits("moduleKey:file1.js", CoverageType.IT, line)).isNull();
      assertThat(context.lineHits("moduleKey:file1.js", CoverageType.OVERALL, line)).isNull();

      assertThat(context.lineHits("moduleKey:file2.js", CoverageType.UNIT, line)).isEqualTo(file2Expected[line - 1]);
      assertThat(context.lineHits("moduleKey:file3.js", CoverageType.UNIT, line)).isNull();
      assertThat(context.lineHits("moduleKey:tests/file1.js", CoverageType.UNIT, line)).isNull();;
    }

    assertThat(context.conditions("moduleKey:file1.js", CoverageType.UNIT, 1)).isNull();
    assertThat(context.conditions("moduleKey:file1.js", CoverageType.UNIT, 2)).isEqualTo(4);
    assertThat(context.coveredConditions("moduleKey:file1.js", CoverageType.UNIT, 2)).isEqualTo(2);
  }


  @Test
  public void test_it_coverage() {
    itCoverageSensor.execute(context);

    Integer[] file1Expected = {1, 1, 0, null};

    for (int line = 1; line <= 4; line++) {
      assertThat(context.lineHits("moduleKey:file1.js", CoverageType.IT, line)).isEqualTo(file1Expected[line - 1]);
      assertThat(context.lineHits("moduleKey:file1.js", CoverageType.UNIT, line)).isNull();

      assertThat(context.lineHits("moduleKey:file2.js", CoverageType.IT, line)).isNull();
    }

    assertThat(context.conditions("moduleKey:file1.js", CoverageType.IT, 1)).isNull();
    assertThat(context.conditions("moduleKey:file1.js", CoverageType.IT, 2)).isEqualTo(4);
    assertThat(context.coveredConditions("moduleKey:file1.js", CoverageType.IT, 2)).isEqualTo(1);
  }

  @Test
  public void test_overall_coverage() {
    overallCoverageSensor.execute(context);

    Integer[] file1Expected = {3, 3, 1, null};
    Integer[] file2Expected = {5, 5, null, null};

    for (int line = 1; line <= 4; line++) {
      assertThat(context.lineHits("moduleKey:file1.js", CoverageType.OVERALL, line)).isEqualTo(file1Expected[line - 1]);
      assertThat(context.lineHits("moduleKey:file1.js", CoverageType.IT, line)).isNull();
      assertThat(context.lineHits("moduleKey:file1.js", CoverageType.UNIT, line)).isNull();

      assertThat(context.lineHits("moduleKey:file2.js", CoverageType.OVERALL, line)).isEqualTo(file2Expected[line - 1]);
      assertThat(context.lineHits("moduleKey:file3.js", CoverageType.OVERALL, line)).isNull();
      assertThat(context.lineHits("moduleKey:tests/file1.js", CoverageType.OVERALL, line)).isNull();
    }

    assertThat(context.conditions("moduleKey:file1.js", CoverageType.OVERALL, 1)).isNull();
    assertThat(context.conditions("moduleKey:file1.js", CoverageType.OVERALL, 2)).isEqualTo(4);
    assertThat(context.coveredConditions("moduleKey:file1.js", CoverageType.OVERALL, 2)).isEqualTo(3);
  }

  @Test
  public void test_invalid_line() {
    settings.setProperty(JavaScriptPlugin.LCOV_UT_REPORT_PATH, "reports/wrong_line_report.lcov");
    utCoverageSensor.execute(context);

    assertThat(context.lineHits("moduleKey:file1.js", CoverageType.UNIT, 0)).isNull();
    assertThat(context.lineHits("moduleKey:file1.js", CoverageType.UNIT, 2)).isEqualTo(1);

    assertThat(context.conditions("moduleKey:file1.js", CoverageType.UNIT, 102)).isNull();
    assertThat(context.conditions("moduleKey:file1.js", CoverageType.UNIT, 2)).isEqualTo(3);
    assertThat(context.coveredConditions("moduleKey:file1.js", CoverageType.UNIT, 2)).isEqualTo(1);
  }

  @Test
  public void test_unresolved_path() {
    settings.setProperty(JavaScriptPlugin.LCOV_UT_REPORT_PATH, "reports/unresolved_path.lcov");
    utCoverageSensor.execute(context);

    // expected logged text: "Could not resolve 1 file paths in [...], first unresolved path: unresolved/file1.js"
    assertThat(context.lineHits("moduleKey:file1.js", CoverageType.UNIT, 1)).isNull();
  }

  @Test
  public void test_no_report_path() {
    context.setSettings(new Settings());
    utCoverageSensor.execute(context);
    assertThat(context.lineHits("moduleKey:file1.js", CoverageType.UNIT, 1)).isNull();

    context.setSettings(new Settings().setProperty(JavaScriptPlugin.LCOV_UT_REPORT_PATH, "reports/report_ut.lcov"));
    itCoverageSensor.execute(context);
    assertThat(context.lineHits("moduleKey:file1.js", CoverageType.UNIT, 1)).isNull();
    overallCoverageSensor.execute(context);
    assertThat(context.lineHits("moduleKey:file1.js", CoverageType.UNIT, 1)).isNull();

    context.setSettings(settings);
    utCoverageSensor.execute(context);
    assertThat(context.lineHits("moduleKey:file1.js", CoverageType.UNIT, 1)).isEqualTo(2);
  }


}
