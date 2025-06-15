package org.plumelib.javacparse;

import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/** Represents the result of parsing a {@code .java} file. */
public final class JavacParseResult {

  /** The compilation unit. */
  private final JCCompilationUnit compilationUnit;

  /** The diagnostics. */
  private final List<Diagnostic<? extends JavaFileObject>> diagnostics;

  /**
   * Create a JavacParseResult.
   *
   * @param compilationUnit the compilation unit
   * @param diagnostics the diagnostics
   */
  public JavacParseResult(
      JCCompilationUnit compilationUnit, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
    this.compilationUnit = compilationUnit;
    this.diagnostics = diagnostics;
  }

  /**
   * Returns the compilation unit.
   *
   * @return the compilation unit
   */
  public final JCCompilationUnit getCompilationUnit() {
    return compilationUnit;
  }

  /**
   * Returns the diagnostics.
   *
   * @return the diagnostics
   */
  public final List<Diagnostic<? extends JavaFileObject>> getDiagnostics() {
    return diagnostics;
  }

  /**
   * Returns true if at least one diagnostic is a parse error.
   *
   * @return true if at least one diagnostic is a parse error
   */
  public final boolean hasParseError() {
    return diagnostics.stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR);
  }
}
