package org.plumelib.mergetools.javacparse;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This class contains static methods {@link #parseJavaFile} and {@link #parseJavaCode} that parse
 * Java code into a CompilationUnit.
 *
 * <p>Internally, this class calls the javac parser from the JDK.
 */
public final class JavacParse {

  /** Do not instantiate. */
  private JavacParse() {
    throw new Error("Do not instantiate.");
  }

  /**
   * Parse a Java file.
   *
   * @param filename the file to parse
   * @return a (parsed) compilation unit, or null if there is a parse error
   * @throws IOException if there is trouble reading the file
   */
  public static @Nullable JCCompilationUnit parseJavaFile(String filename) throws IOException {
    return parseJavaFileObject(new FileJavaFileObject(filename));
  }

  /**
   * Parse a Java file's contents.
   *
   * @param javaCode the contents of a Java file
   * @return a (parsed) compilation unit, or null if there is a parse error
   */
  public static @Nullable JCCompilationUnit parseJavaCode(String javaCode) {
    try {
      return parseJavaFileObject(new StringJavaFileObject(javaCode));
    } catch (IOException e) {
      throw new Error("This can't happen", e);
    }
  }

  /**
   * Parse a the contents of a JavaFileObject. Returns null if there was a parse error (even if the
   * javac parser could create a CompilationUnit, some of whose subcomponents are erroneous).
   *
   * @param source a JavaFileObject
   * @return a (parsed) compilation unit, or null if the source yields a parse error
   * @throws IOException if there is trouble reading the file
   */
  public static @Nullable JCCompilationUnit parseJavaFileObject(JavaFileObject source)
      throws IOException {
    Context context = new Context();

    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    context.put(DiagnosticListener.class, diagnostics);

    @SuppressWarnings({
      "builder", // No not close the JavacFileManager, which is reused by javac.
      "UnusedVariable" // `new JavacFileManager` is called for side effect; the variable is a place
      // to put this @SuppressWarnings annotation
    })
    JavacFileManager fileManager = new JavacFileManager(context, true, StandardCharsets.UTF_8);

    Log.instance(context).useSource(source);
    ParserFactory parserFactory = ParserFactory.instance(context);
    JavacParser parser = parserFactory.newParser(source.getCharContent(false), true, true, true);
    JCCompilationUnit cu = parser.parseCompilationUnit();
    cu.sourcefile = source;

    boolean parseError =
        diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR);
    if (parseError) {
      return null;
    } else {
      return cu;
    }
  }
}
