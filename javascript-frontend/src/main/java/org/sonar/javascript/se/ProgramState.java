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
package org.sonar.javascript.se;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.javascript.api.symbols.Symbol;

public class ProgramState {

  private final ImmutableMap<Symbol, SymbolicValue> values;
  private final ImmutableMap<SymbolicValue, Constraint> constraints;

  private int counter;

  private ProgramState(ImmutableMap<Symbol, SymbolicValue> values, ImmutableMap<SymbolicValue, Constraint> constraints, int counter) {
    ImmutableMap.Builder<SymbolicValue, Constraint> constraintsBuilder = ImmutableMap.builder();
    for (Entry<SymbolicValue, Constraint> entry : constraints.entrySet()) {
      if (values.containsValue(entry.getKey())) {
        constraintsBuilder.put(entry.getKey(), entry.getValue());
      }
    }

    this.values = values;
    this.constraints = constraintsBuilder.build();
    this.counter = counter;
  }

  public static ProgramState emptyState() {
    return new ProgramState(ImmutableMap.<Symbol, SymbolicValue>of(), ImmutableMap.<SymbolicValue, Constraint>of(), 0);
  }

  // returns new PS with this constraint added to PS for this value. If constraint for this value exists IllegalStateException
  private ProgramState addConstraint(SymbolicValue value, Constraint constraint) {
    if (constraints.containsKey(value)) {
      throw new IllegalStateException();
    }

    ImmutableMap.Builder<SymbolicValue, Constraint> constraintsBuilder = ImmutableMap.builder();
    constraintsBuilder.putAll(constraints);
    constraintsBuilder.put(value, constraint);

    return new ProgramState(ImmutableMap.copyOf(values), constraintsBuilder.build(), counter);
  }


  ProgramState newSymbolicValue(Symbol symbol, @Nullable Constraint constraint) {
    SymbolicValue value = newSymbolicValue();

    ImmutableMap.Builder<Symbol, SymbolicValue> valuesBuilder = ImmutableMap.builder();
    for (Entry<Symbol, SymbolicValue> entry : values.entrySet()) {
      if (!entry.getKey().equals(symbol)) {
        valuesBuilder.put(entry.getKey(), entry.getValue());
      }
    }
    valuesBuilder.put(symbol, value);

    ProgramState newProgramState = new ProgramState(valuesBuilder.build(), ImmutableMap.copyOf(constraints), counter);
    if (constraint != null) {
      newProgramState = newProgramState.addConstraint(value, constraint);
    }

    return newProgramState;
  }

  public ProgramState constrain(@Nullable SymbolicValue value, @Nullable Constraint constraint) {
    if (value == null || constraint == null) {
      return this;
    }
    if (getConstraint(value).isIncompatibleWith(constraint)) {
      return null;
    } else {
      Constraint newConstraint = getConstraint(value).and(constraint);
      return new ProgramState(ImmutableMap.copyOf(values), replaceConstraint(value, newConstraint), counter);
    }
  }

  private ImmutableMap<SymbolicValue, Constraint> replaceConstraint(SymbolicValue value, Constraint newConstraint) {
    ImmutableMap.Builder<SymbolicValue, Constraint> constraintsBuilder = ImmutableMap.builder();
    for (Entry<SymbolicValue, Constraint> entry : constraints.entrySet()) {
      if (!entry.getKey().equals(value)) {
        constraintsBuilder.put(entry.getKey(), entry.getValue());
      }
    }

    constraintsBuilder.put(value, newConstraint);
    return constraintsBuilder.build();
  }

  private SymbolicValue newSymbolicValue() {
    SymbolicValue value = new SimpleSymbolicValue(counter);
    counter++;
    return value;
  }

  @CheckForNull
  public SymbolicValue getSymbolicValue(@Nullable Symbol symbol) {
    return values.get(symbol);
  }

  public Constraint getConstraint(@Nullable SymbolicValue value) {
    Constraint constraint = constraints.get(value);
    if (constraint == null) {
      return Constraint.ANY_VALUE;
    }
    return constraint;
  }

  public Constraint getConstraint(@Nullable Symbol symbol) {
    return getConstraint(getSymbolicValue(symbol));
  }

  public Nullability getNullability(@Nullable SymbolicValue value) {
    return getConstraint(value).nullability();
  }

  public Map<Symbol, Constraint> constraintsBySymbol() {
    ImmutableMap.Builder<Symbol, Constraint> builder = new Builder<>();
    for (Entry<Symbol, SymbolicValue> entry : values.entrySet()) {
      if (constraints.get(entry.getValue()) != null) {
        builder.put(entry.getKey(), constraints.get(entry.getValue()));
      }
    }

    return builder.build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ProgramState that = (ProgramState) o;

    return constraintsBySymbol().equals(that.constraintsBySymbol());
  }

  @Override
  public int hashCode() {
    return constraintsBySymbol().hashCode();
  }
}
