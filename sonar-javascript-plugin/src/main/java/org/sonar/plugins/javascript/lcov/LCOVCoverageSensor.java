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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.javascript.JavaScriptLanguage;

abstract class LCOVCoverageSensor implements Sensor {
  private static final Logger LOG = Loggers.get(UTCoverageSensor.class);

  protected abstract String[] reportPathProperties();
  protected abstract CoverageType getCoverageType();

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguage(JavaScriptLanguage.KEY)
      .onlyOnFileType(Type.MAIN);
  }

  @Override
  public void execute(SensorContext context) {
    if (isLCOVReportProvided(context)) {
      saveMeasureFromLCOVFile(context);
    }
  }

  private boolean isLCOVReportProvided(SensorContext context) {
    for(String reportPathProperty: reportPathProperties()){
      if (StringUtils.isNotBlank(context.settings().getString(reportPathProperty))){
        return true;
      }
    }
    return false;
  }

  private void saveMeasureFromLCOVFile(SensorContext context) {
    LinkedList<File> lcovFiles=new LinkedList<>();
    for(String reportPathProperty: reportPathProperties()) {
      String providedPath = context.settings().getString(reportPathProperty);
      if (StringUtils.isBlank(providedPath)){
        continue;
      }
      File lcovFile = getIOFile(context.fileSystem().baseDir(), providedPath);

      if (lcovFile.isFile()) {
        lcovFiles.add(lcovFile);
      } else {
        LOG.warn("No coverage information will be saved because LCOV file cannot be found.");
        LOG.warn("Provided LCOV file path: {}. Seek file with path: {}", providedPath, lcovFile.getAbsolutePath());
      }
    }

    if(lcovFiles.isEmpty()) {
      LOG.warn("No coverage information will be saved because all LCOV files cannot be found.");
      return;
    }

    LOG.info("Analysing {}", lcovFiles);

    LCOVParser parser = LCOVParser.create(context, lcovFiles.toArray(new File[lcovFiles.size()]));
    Map<InputFile, NewCoverage> coveredFiles = parser.coverageByFile();

    FileSystem fileSystem = context.fileSystem();
    FilePredicate mainFilePredicate = fileSystem.predicates().and(
      fileSystem.predicates().hasType(Type.MAIN),
      fileSystem.predicates().hasLanguage(JavaScriptLanguage.KEY));

    for (InputFile inputFile : fileSystem.inputFiles(mainFilePredicate)) {
      NewCoverage fileCoverage = coveredFiles.get(inputFile);

      if (fileCoverage != null) {
        fileCoverage
          .ofType(getCoverageType())
          .save();

      } else {
        // colour all lines as not executed
        LOG.debug("Default value of zero will be saved for file: {} (assuming there is no exclusion to this file)", inputFile.relativePath());
        LOG.debug("Because was not present in LCOV report.");
      }
    }

    List<String> unresolvedPaths = parser.unresolvedPaths();
    if (!unresolvedPaths.isEmpty()) {
      LOG.warn(
        String.format(
          "Could not resolve %d file paths in %s, first unresolved path: %s",
          unresolvedPaths.size(), lcovFiles, unresolvedPaths.get(0)));
    }
  }

  /**
   * Returns a java.io.File for the given path.
   * If path is not absolute, returns a File with module base directory as parent path.
   */
  private static File getIOFile(File baseDir, String path) {
    File file = new File(path);
    if (!file.isAbsolute()) {
      file = new File(baseDir, path);
    }

    return file;
  }
}
