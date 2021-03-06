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
package org.sonar.javascript.issues;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultTextPointer;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.Issue.Flow;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.javascript.tree.impl.lexical.InternalSyntaxToken;
import org.sonar.plugins.javascript.api.visitors.PreciseIssue;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.lexical.SyntaxTrivia;
import org.sonar.plugins.javascript.api.visitors.IssueLocation;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PreciseIssueCompatTest {

  private InputFile inputFile = new DefaultInputFile("module1", "myPath")
    .setLines(4)
    .setOriginalLineOffsets(new int[]{0, 10, 20, 30})
    .setLastValidOffset(40);
  private SensorContext sensorContext = mock(SensorContext.class);
  private RuleKey ruleKey = RuleKey.of("repo1", "key1");
  private SensorStorage storage = mock(SensorStorage.class);
  private IssueLocation primary = new IssueLocation(createToken(3, 2, "token1"), "msg1");
  private IssueLocation secondary1 = new IssueLocation(createToken(2, 1, "token2"), "msg2");
  private IssueLocation secondary2 = new IssueLocation(createToken(3, 1, "token3"));

  @Test
  public void no_secondary_location() throws Exception {
    DefaultIssue newIssue = new DefaultIssue(storage);
    when(sensorContext.newIssue()).thenReturn(newIssue);
    PreciseIssueCompat.save(sensorContext, inputFile, ruleKey, new PreciseIssue(null, primary).cost(3.));

    assertThat(newIssue.ruleKey()).isEqualTo(ruleKey);
    assertThat(newIssue.effortToFix()).isEqualTo(3.);
    assertThat(newIssue.primaryLocation().message()).isEqualTo("msg1");
    assertThat(newIssue.primaryLocation().inputComponent()).isEqualTo(inputFile);
    assertThat(newIssue.primaryLocation().textRange().start()).isEqualTo(new DefaultTextPointer(3, 2));
    assertThat(newIssue.primaryLocation().textRange().end()).isEqualTo(new DefaultTextPointer(3, 8));
    assertThat(newIssue.flows()).isEmpty();
    verify(storage).store(newIssue);
  }

  @Test
  public void secondaryLocation() throws Exception {
    DefaultIssue newIssue = new DefaultIssue(storage);
    when(sensorContext.newIssue()).thenReturn(newIssue);
    PreciseIssueCompat.save(sensorContext, inputFile, ruleKey, new PreciseIssue(null, primary).secondary(secondary1).secondary(secondary2).cost(3.));

    assertThat(newIssue.flows()).hasSize(2);
    Flow flow = newIssue.flows().get(0);
    assertThat(flow.locations()).hasSize(1);
    assertThat(flow.locations().get(0).message()).isEqualTo("msg2");
    assertThat(flow.locations().get(0).inputComponent()).isEqualTo(inputFile);
    assertThat(flow.locations().get(0).textRange().start()).isEqualTo(new DefaultTextPointer(2, 1));
    assertThat(flow.locations().get(0).textRange().end()).isEqualTo(new DefaultTextPointer(2, 7));
    assertThat(newIssue.flows().get(1).locations().get(0).message()).isNull();
    verify(storage).store(newIssue);
  }

  private Tree createToken(int line, int column, String tokenValue) {
    return new InternalSyntaxToken(line, column, tokenValue, ImmutableList.<SyntaxTrivia>of(), 0, false);
  }

}
