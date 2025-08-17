package org.plumelib.javacparse;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

/**
 * This class contains static methods that parse Java code.
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
   * @return a (parsed) compilation unit, which may include parse errors
   * @throws IOException if there is trouble reading the file
   */
  public static JavacParseResult<CompilationUnitTree> parseJavaFile(String filename)
      throws IOException {
    return parseJavaFileObject(new FileJavaFileObject(filename));
  }

  /**
   * Parse a Java file's contents.
   *
   * @param javaCode the contents of a Java file
   * @return a (parsed) compilation unit, which may include parse errors
   */
  public static JavacParseResult<CompilationUnitTree> parseJavaCode(String javaCode) {
    try {
      return parseJavaFileObject(new StringJavaFileObject(javaCode));
    } catch (IOException e) {
      throw new Error("This can't happen", e);
    }
  }

  /**
   * Parse the contents of a JavaFileObject.
   *
   * @param source a JavaFileObject
   * @return a (parsed) compilation unit, which may include parse errors
   * @throws IOException if there is trouble reading the file
   */
  @SuppressWarnings("try") // `fileManagerUnused` is not used
  public static JavacParseResult<CompilationUnitTree> parseJavaFileObject(JavaFileObject source)
      throws IOException {
    // The documentation of Context says "a single Context is used for each invocation of the
    // compiler".  Re-using the Context causes an error "duplicate context value" in the compiler.
    // A Context is just a map.
    Context context = new Context();

    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    context.put(DiagnosticListener.class, diagnostics);

    // Needed to avoid "this.fileManager is null" error in com.sun.tools.javac.comp.Modules.<init>.
    try (@SuppressWarnings("UnusedVariable") // `new JavacFileManager` sets a mapping in `context`.
        JavacFileManager fileManagerUnused =
            new JavacFileManager(context, true, StandardCharsets.UTF_8)) {

      Log.instance(context).useSource(source);
      ParserFactory parserFactory = ParserFactory.instance(context);
      JavacParser parser = parserFactory.newParser(source.getCharContent(false), true, true, true);
      CompilationUnitTree cu = parser.parseCompilationUnit();
      ((JCCompilationUnit) cu).sourcefile = source;
      return new JavacParseResult<>(cu, diagnostics.getDiagnostics());
    }
  }
}
