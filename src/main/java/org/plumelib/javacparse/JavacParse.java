package org.plumelib.javacparse;

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
 * This class contains static methods {@link #parseJavaFile} and {@link #parseJavaCode} that parse
 * Java code into a JCCompilationUnit.
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
  public static JavacParseResult parseJavaFile(String filename) throws IOException {
    return parseJavaFileObject(new FileJavaFileObject(filename));
  }

  /**
   * Parse a Java file's contents.
   *
   * @param javaCode the contents of a Java file
   * @return a (parsed) compilation unit, or null if there is a parse error
   */
  public static JavacParseResult parseJavaCode(String javaCode) {
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
   * @return a compilation unit and the parse errors encountered in it
   * @throws IOException if there is trouble reading the file
   */
  @SuppressWarnings("try") // `fileManagerUnused` is not used
  public static JavacParseResult parseJavaFileObject(JavaFileObject source) throws IOException {
    // Per the documentation of Context, "a single Context is used for each invocation of the
    // compiler".  Making the Context static and re-using it causes an assertion error "duplicate
    // context value" in the compiler.
    Context context = new Context();

    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    context.put(DiagnosticListener.class, diagnostics);

    // Need to avoid a "this.fileManager is null" error in com.sun.tools.javac.comp.Modules.<init>.

    try (@SuppressWarnings("UnusedVariable") // `new JavacFileManager` sets a mapping in `context`.
        JavacFileManager fileManagerUnused =
            new JavacFileManager(context, true, StandardCharsets.UTF_8)) {

      Log.instance(context).useSource(source);
      ParserFactory parserFactory = ParserFactory.instance(context);
      JavacParser parser = parserFactory.newParser(source.getCharContent(false), true, true, true);
      JCCompilationUnit cu = parser.parseCompilationUnit();
      cu.sourcefile = source;
      return new JavacParseResult(cu, diagnostics.getDiagnostics());
    }
  }
}
