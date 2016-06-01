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
package org.sonar.plugins.javascript;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.javascript.tree.symbols.type.JQuery;
import org.sonar.plugins.javascript.cpd.JavaScriptCpdMapping;
import org.sonar.plugins.javascript.lcov.ITCoverageSensor;
import org.sonar.plugins.javascript.lcov.OverallCoverageSensor;
import org.sonar.plugins.javascript.lcov.UTCoverageSensor;
import org.sonar.plugins.javascript.rules.JavaScriptRulesDefinition;

public class JavaScriptPlugin extends SonarPlugin {

  // Subcategories

  private static final String GENERAL = "General";
  private static final String TEST_AND_COVERAGE = "Tests and Coverage";
  private static final String LIBRARIES = "Libraries";


  // Global JavaScript constants

  public static final String FILE_SUFFIXES_KEY = "sonar.javascript.file.suffixes";
  public static final String FILE_SUFFIXES_DEFVALUE = ".js";

  public static final String PROPERTY_PREFIX = "sonar.javascript";

  public static final String LCOV_UT_REPORT_PATH = PROPERTY_PREFIX + ".lcov.reportPath";
  public static final String LCOV_UT_REPORT_PATH_DEFAULT_VALUE = "";

  public static final String LCOV_IT_REPORT_PATH = PROPERTY_PREFIX + ".lcov.itReportPath";
  public static final String LCOV_IT_REPORT_PATH_DEFAULT_VALUE = "";

  public static final String JQUERY_OBJECT_ALIASES = JQuery.JQUERY_OBJECT_ALIASES;
  public static final String JQUERY_OBJECT_ALIASES_DEFAULT_VALUE = JQuery.JQUERY_OBJECT_ALIASES_DEFAULT_VALUE;

  public static final String IGNORE_HEADER_COMMENTS = PROPERTY_PREFIX + ".ignoreHeaderComments";
  public static final Boolean IGNORE_HEADER_COMMENTS_DEFAULT_VALUE = true;

  @Override
  public List getExtensions() {
    return ImmutableList.of(
      JavaScriptLanguage.class,
      JavaScriptCpdMapping.class,

      JavaScriptSquidSensor.class,
      JavaScriptRulesDefinition.class,
      JavaScriptProfile.class,

      UTCoverageSensor.class,
      ITCoverageSensor.class,
      OverallCoverageSensor.class,

      PropertyDefinition.builder(FILE_SUFFIXES_KEY)
        .defaultValue(FILE_SUFFIXES_DEFVALUE)
        .name("File Suffixes")
        .description("Comma-separated list of suffixes for files to analyze.")
        .subCategory(GENERAL)
        .onQualifiers(Qualifiers.PROJECT)
        .build(),

      PropertyDefinition.builder(JavaScriptPlugin.IGNORE_HEADER_COMMENTS)
        .defaultValue(JavaScriptPlugin.IGNORE_HEADER_COMMENTS_DEFAULT_VALUE.toString())
        .name("Ignore header comments")
        .description("True to not count file header comments in comment metrics.")
        .onQualifiers(Qualifiers.MODULE, Qualifiers.PROJECT)
        .subCategory(GENERAL)
        .type(PropertyType.BOOLEAN)
        .build(),

      PropertyDefinition.builder(LCOV_UT_REPORT_PATH)
        .defaultValue(LCOV_UT_REPORT_PATH_DEFAULT_VALUE)
        .name("Unit Tests LCOV File")
        .description("Path (absolute or relative) to the file with LCOV data for unit tests.")
        .onQualifiers(Qualifiers.MODULE, Qualifiers.PROJECT)
        .subCategory(TEST_AND_COVERAGE)
        .build(),

      PropertyDefinition.builder(LCOV_IT_REPORT_PATH)
        .defaultValue(LCOV_IT_REPORT_PATH_DEFAULT_VALUE)
        .name("Integration Tests LCOV File")
        .description("Path (absolute or relative) to the file with LCOV data for integration tests.")
        .onQualifiers(Qualifiers.MODULE, Qualifiers.PROJECT)
        .subCategory(TEST_AND_COVERAGE)
        .build(),

      PropertyDefinition.builder(JavaScriptPlugin.JQUERY_OBJECT_ALIASES)
        .defaultValue(JavaScriptPlugin.JQUERY_OBJECT_ALIASES_DEFAULT_VALUE)
        .name("jQuery object aliases")
        .description("Comma-separated list of names used to address jQuery object.")
        .onQualifiers(Qualifiers.MODULE, Qualifiers.PROJECT)
        .subCategory(LIBRARIES)
        .build()
    );
  }

}
