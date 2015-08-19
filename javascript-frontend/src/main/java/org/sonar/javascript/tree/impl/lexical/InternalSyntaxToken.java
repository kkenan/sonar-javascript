/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011 SonarSource and Eriks Nukis
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.javascript.tree.impl.lexical;

import com.sonar.sslr.api.TokenType;
import org.sonar.plugins.javascript.api.visitors.TreeVisitor;
import org.sonar.javascript.tree.impl.JavaScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.javascript.api.tree.lexical.SyntaxTrivia;

import java.util.Iterator;
import java.util.List;

public class InternalSyntaxToken extends JavaScriptTree implements SyntaxToken {

  private List<SyntaxTrivia> trivias;
  private int startIndex;
  private final int line;
  private final int column;
  private final String value;
  private final boolean isEOF;

  public InternalSyntaxToken(int line, int column, String value, List<SyntaxTrivia> trivias, int startIndex, boolean isEOF) {
    this.value = value;
    this.line = line;
    this.column = column;
    this.trivias = trivias;
    this.startIndex = startIndex;
    this.isEOF = isEOF;
  }

  public int toIndex() {
    return startIndex + value.length();
  }

  @Override
  public String text() {
    return value;
  }

  @Override
  public List<SyntaxTrivia> trivias() {
    return trivias;
  }

  /**
   * @deprecated Use {@link SyntaxToken#line()} instead.
   */
  @Deprecated
  @Override
  public int getLine() {
    return line();
  }

  @Override
  public int line() {
    return line;
  }

  @Override
  public int column() {
    return column;
  }

  public int startIndex() {
    return startIndex;
  }

  @Override
  public Kind getKind() {
    return Kind.TOKEN;
  }

  @Override
  public boolean isLeaf() {
    return true;
  }

  public boolean isEOF(){
    return isEOF;
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void accept(TreeVisitor visitor) {
    // FIXME do nothing at the moment
  }

  public boolean is(TokenType type) {
    return this.text().equals(type.getValue());
  }

  @Override
  public SyntaxToken getFirstToken() {
    return this;
  }

  @Override
  public SyntaxToken getLastToken() {
    return this;
  }
}
