package org.plumelib.javacparse;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
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
  public static JavacParseResult<CompilationUnitTree> parseFile(String filename)
      throws IOException {
    return parseCompilationUnit(new FileJavaFileObject(filename));
  }

  /**
   * Parse a Java file's contents.
   *
   * @param javaCode the contents of a Java file
   * @return a (parsed) compilation unit, which may include parse errors
   */
  public static JavacParseResult<CompilationUnitTree> parseCompilationUnit(String javaCode) {
    try {
      return parseCompilationUnit(new StringJavaFileObject(javaCode));
    } catch (IOException e) {
      throw new UncheckedIOException("This can't happen", e);
    }
  }

  /**
   * Parses the given Java type declaration (class, interface, enum, record, etc.).
   *
   * @param classSource the string representation of a Java type declaration
   * @return the parsed type declaration
   * @throws IllegalArgumentException if the source is not parsable as a type declaration or
   *     contains a top-level ";"
   */
  public static JavacParseResult<ClassTree> parseTypeDeclaration(String classSource) {
    JavacParseResult<CompilationUnitTree> parsedCU = parseCompilationUnit(classSource);

    if (parsedCU.hasParseError()) {
      String msg = parsedCU.getParseErrorMessages();
      if (msg.isEmpty()) {
        throw new Error("Has parse errors, but empty message: " + parsedCU.diagnostics());
      }
      throw new IllegalArgumentException("Invalid type declaration (" + msg + "): " + classSource);
    }

    CompilationUnitTree cu = parsedCU.tree();

    if (!cu.getImports().isEmpty()) {
      throw new IllegalArgumentException(
          "Type declaration source code has imports: " + classSource);
    }
    if (cu.getModule() != null) {
      throw new IllegalArgumentException(
          "Type declaration source code has a module declaration: " + classSource);
    }
    if (cu.getPackage() != null) {
      throw new IllegalArgumentException(
          "Type declaration source code has a package declaration: " + classSource);
    }

    List<? extends Tree> decls = cu.getTypeDecls();
    for (Tree decl : decls) {
      if (decl instanceof EmptyStatementTree) {
        throw new IllegalArgumentException(
            "Type declaration source code contains a top-level `;`: " + classSource);
      }
    }
    int numDecls = decls.size();
    if (numDecls != 1) {
      throw new IllegalArgumentException(
          "Type declaration source code has %d top-level forms, not 1: %s"
              .formatted(numDecls, classSource));
    }

    Tree decl = decls.get(0);
    if (decl instanceof ClassTree ct) {
      return new JavacParseResult<>(ct, parsedCU.diagnostics());
    } else {
      throw new IllegalArgumentException(
          "source code should be a type declaration but is "
              + decl.getClass().getSimpleName()
              + ":"
              + classSource);
    }
  }

  /**
   * Parses a member of a type declaration.
   *
   * @param memberSource the string representation of a Java method, field, static initializer,
   *     class, etc.
   * @return the parsed type member
   * @throws IllegalArgumentException if the member source does not parse
   */
  private static Tree parseTypeMember(String memberSource) {
    String dummySource = "class DummyClass { " + memberSource + "; }";

    JavacParseResult<ClassTree> parsedTypeDecl = parseTypeDeclaration(dummySource);
    if (parsedTypeDecl.hasParseError()) {
      throw new IllegalArgumentException("Invalid type member: " + memberSource);
    }
    ClassTree typeDecl = parsedTypeDecl.tree();

    List<? extends Tree> members = typeDecl.getMembers();
    if (members.size() != 1) {
      // This was an injection attack, such as "0; int x = 1".
      throw new IllegalArgumentException("Invalid type member: " + memberSource);
    }

    return members.get(0);
  }

  /**
   * Parses the given Java method or annotation type element.
   *
   * @param methodSource the string representation of a Java method or annotation type element
   * @return the parsed method
   * @throws IllegalArgumentException if the method source does not parse
   */
  @SuppressWarnings("PMD.AvoidThrowingNewInstanceOfSameException") // bug in PMD
  public static MethodTree parseMethod(String methodSource) {
    Tree member;
    try {
      member = parseTypeMember(methodSource);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid method: " + methodSource);
    }
    if (!(member instanceof MethodTree mt)) {
      throw new IllegalArgumentException("Invalid method: " + methodSource);
    }
    return mt;
  }

  /**
   * Parses the given Java expression string, such as "foo.bar()" or "1 + 2".
   *
   * @param expressionSource the string representation of a Java expression
   * @return the parsed expression
   * @throws IllegalArgumentException if the expression source does not parse
   */
  @SuppressWarnings("PMD.AvoidThrowingNewInstanceOfSameException") // bug in PMD
  public static ExpressionTree parseExpression(String expressionSource) {
    String dummySource = "Object expression = " + expressionSource + ";";

    Tree member;
    try {
      member = parseTypeMember(dummySource);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid expression: " + expressionSource);
    }
    if (!(member instanceof VariableTree vt)) {
      throw new IllegalArgumentException("Invalid expression: " + expressionSource);
    }
    ExpressionTree expr = vt.getInitializer();
    return expr;
  }

  /**
   * Parses the given Java type use, such as "int", "String", or "List&lt;? extends Number&gt;".
   *
   * @param typeSource the string representation of a Java type use
   * @return the parsed type use
   * @throws IllegalArgumentException if the type source does not parse
   */
  @SuppressWarnings("PMD.AvoidThrowingNewInstanceOfSameException") // bug in PMD
  public static Tree parseTypeUse(String typeSource) {
    String dummySource = typeSource + " fieldName;";

    Tree member;
    try {
      member = parseTypeMember(dummySource);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid type use: " + typeSource);
    }
    if (!(member instanceof VariableTree vt)) {
      throw new IllegalArgumentException("Invalid type use: " + typeSource);
    }
    Tree type = vt.getType();
    return type;
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Low-level routines that take a JavaFileObject instead of a String
  //

  // These routines have the downside that they may parse a prefix of the JavaFileObject rather than
  // the whole thing.

  // Implementation notes:
  // 1. The documentation of Context says "a single Context is used for each invocation of the
  //    compiler".  Re-using the Context causes an error "duplicate context value" in the compiler.
  //    A Context is just a map.
  // 2. Calling `new JavacFileManager` sets a mapping in `context`.  It is necessary to avoid
  //    "this.fileManager is null" error in com.sun.tools.javac.comp.Modules.<init>.

  /**
   * Parse the contents of a JavaFileObject.
   *
   * @param source a JavaFileObject
   * @return a (parsed) compilation unit, which may include parse errors
   * @throws IOException if there is trouble reading the file
   */
  // TODO: Document whether this can parse just a prefix of the JavaFileObject.
  public static JavacParseResult<CompilationUnitTree> parseCompilationUnit(JavaFileObject source)
      throws IOException {
    JavacParseResult<CompilationUnitTree> result =
        parseWith(source, JavacParser::parseCompilationUnit);
    ((JCCompilationUnit) result.tree()).sourcefile = source;
    return result;
  }

  /**
   * Parse a Java expression.
   *
   * <p><b>Warning:</b> If the prefix of the string is a Java expression, this may return the result
   * of parsing that prefix, even if the whole string is not an expression. For example, it parses
   * "Hello this is nonsense." without error as an identifier "Hello", but it parses "1 +" into a
   * parse error. Therefore, this routine is not appropriate for most uses.
   *
   * @param source a JavaFileObject
   * @return a (parsed) expression, possibly an ErroneousTree
   * @throws IOException if there is trouble reading the file
   * @deprecated may parse a prefix rather than the whole string
   */
  @Deprecated // not for removal
  public static JavacParseResult<ExpressionTree> parseExpression(JavaFileObject source)
      throws IOException {
    return parseWith(source, JavacParser::parseExpression);
  }

  /**
   * Parse a type use.
   *
   * <p><b>Warning:</b> If the prefix of the string is a Java type, this may return the result of
   * parsing that prefix, even if the whole string is not a type. For example, it parses "Foo bar
   * baz" without error as the type "Foo". Therefore, this routine is not appropriate for most uses;
   * prefer {@link #parseTypeUse(String)}.
   *
   * @param source a JavaFileObject
   * @return a (parsed) type use, possibly an ErroneousTree
   * @throws IOException if there is trouble reading the file
   * @deprecated may parse a prefix rather than the whole string
   */
  @Deprecated // not for removal
  public static JavacParseResult<ExpressionTree> parseTypeUse(JavaFileObject source)
      throws IOException {
    return parseWith(source, JavacParser::parseType);
  }

  /**
   * Creates a javac parser for {@code source} and applies {@code parserFn} to it, returning the
   * result.
   *
   * @param <T> the type of parse tree produced
   * @param source the source to parse
   * @param parserFn the parsing operation to apply to the parser
   * @return the parse result
   * @throws IOException if there is trouble reading the file
   */
  @SuppressWarnings("try") // `fileManagerUnused` is not used
  private static <T extends Tree> JavacParseResult<T> parseWith(
      JavaFileObject source, Function<JavacParser, T> parserFn) throws IOException {
    Context context = new Context();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    context.put(DiagnosticListener.class, diagnostics);

    try (@SuppressWarnings({
          "UnusedVariable",
          "PMD.UnusedLocalVariable"
        }) // `new JavacFileManager` sets a mapping in `context`.
        JavacFileManager fileManagerUnused =
            new JavacFileManager(context, true, StandardCharsets.UTF_8)) {

      Log.instance(context).useSource(source);
      ParserFactory parserFactory = ParserFactory.instance(context);
      JavacParser parser = parserFactory.newParser(source.getCharContent(false), true, true, true);
      return new JavacParseResult<>(parserFn.apply(parser), diagnostics.getDiagnostics());
    }
  }
}
