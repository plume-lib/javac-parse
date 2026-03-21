package org.plumelib.javacparse;

import com.sun.source.tree.Tree;
import java.util.List;
import java.util.StringJoiner;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.checkerframework.checker.lock.qual.GuardSatisfied;

/**
 * Represents the result of parsing Java code (a file or a subpart thereof).
 *
 * @param <T> the type of the Java code being parsed
 * @param tree the parse tree
 * @param diagnostics the diagnostics
 */
public record JavacParseResult<T extends Tree>(
    T tree, List<Diagnostic<? extends JavaFileObject>> diagnostics) {

  /**
   * Returns true if at least one diagnostic is a parse error.
   *
   * @return true if at least one diagnostic is a parse error
   */
  public boolean hasParseError() {
    return diagnostics.stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR);
  }

  /**
   * Returns all the parse error messages, concatenated. May return an empty string.
   *
   * @return all the parse error messages, concatenated
   */
  public String getParseErrorMessages() {
    StringJoiner sj = new StringJoiner("; ");
    for (Diagnostic<? extends JavaFileObject> d : diagnostics) {
      if (d.getKind() == Diagnostic.Kind.ERROR) {
        @SuppressWarnings("nullness:argument") // javac is not annotated
        String msg = d.getMessage(null);
        sj.add(msg);
      }
    }
    return sj.toString();
  }

  @Override
  @SuppressWarnings({
    "allcheckers:purity.not.sideeffectfree.call",
    "lock:method.guarantee.violated"
  }) // side effect to local StringJoiner
  public String toString(@GuardSatisfied JavacParseResult<T> this) {
    String prefix =
        "JPR{" + tree + " [" + tree.getClass().getSimpleName() + "] [" + tree.getKind() + "]";
    if (diagnostics.isEmpty()) {
      return prefix + "}";
    }

    StringJoiner sj = new StringJoiner(System.lineSeparator());
    sj.add(prefix);
    for (Diagnostic<?> d : diagnostics) {
      sj.add("  " + d);
    }
    sj.add("}");

    return sj.toString();
  }
}
