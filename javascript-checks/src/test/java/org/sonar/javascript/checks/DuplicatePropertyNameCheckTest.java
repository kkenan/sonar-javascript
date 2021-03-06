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
package org.sonar.javascript.checks;

import java.io.File;
import org.junit.Test;
import org.sonar.javascript.checks.verifier.JavaScriptCheckVerifier;

public class DuplicatePropertyNameCheckTest {

  @Test
  public void test() {
    DuplicatePropertyNameCheck check = new DuplicatePropertyNameCheck();

    JavaScriptCheckVerifier.issues(check, new File("src/test/resources/checks/duplicatePropertyName.js"))
      .next().atLine(5).withMessage("Rename or remove duplicate property name 'key'.")
      .next().atLine(6).withMessage("Rename or remove duplicate property name 'key'.")
      .next().atLine(7).withMessage("Rename or remove duplicate property name 'key'.")
      .next().atLine(8).withMessage("Rename or remove duplicate property name '\\u006bey'.")
      .next().atLine(9).withMessage("Rename or remove duplicate property name '\\u006bey'.")
      .next().atLine(10).withMessage("Rename or remove duplicate property name '\\x6bey'.")
      .next().atLine(11).withMessage("Rename or remove duplicate property name '1'.")
      .next().atLine(12)
      .noMore();
  }

}
