# javac-parse:  a wrapper around javac's parser

The parser in javac is the most authoritative and correct parser for Java.
Calling that parser requires setting up various data structures that javac uses.
This small package contains a method that does that setup, making it easy for you
to call javac's parser.

See the [API documentation](https://plumelib.org/javac-parse/api/org/plumelib/javacparse/package-summary.html).


## The javac AST (parse tree)

A parse tree is often called an AST (abstract syntax tree).

The `parseJavaFile()` and `parseJavaCode()` methods return an instance of class
[`JCTree.JCCompilationUnit`](https://www.javadoc.io/static/org.kohsuke.sorcerer/sorcerer-javac/0.11/com/sun/tools/javac/tree/JCTree.JCCompilationUnit.html).
It implements the interface
[`CompilationUnitTree`](https://docs.oracle.com/javase/8/docs/jdk/api/javac/tree/com/sun/source/tree/CompilationUnitTree.html),
javac-parse uses the internal javac class
[`JCTree`](https://www.javadoc.io/static/org.kohsuke.sorcerer/sorcerer-javac/0.11/com/sun/tools/javac/tree/JCTree.html)
and its subclasses because they provide more functionality than the interface.


## Editing your buildfile ##

You can obtain the javac-parse library from [Maven
Central](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.plumelib%22%20a%3A%22javac-parse%22).

In a Gradle buildfile, write

```
dependencies {
  implementation 'org.plumelib:javac-parse:0.1.0'
}
```

Other build systems are [similar](https://search.maven.org/artifact/org.plumelib/javac-parse/0.1.0/jar).

You will need to add something like this to your buildfile (Gradle example):

```
compileJava {
  options.compilerArgs += '--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED'
  options.compilerArgs += '--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED'
  options.compilerArgs += '--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED'
  options.compilerArgs += '--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED'
  options.compilerArgs += '--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED'
  options.compilerArgs += '--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED'
}
compileTestJava {
  options.compilerArgs += '--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED'
  options.compilerArgs += '--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED'
  options.compilerArgs += '--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED'
  options.compilerArgs += '--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED'
  options.compilerArgs += '--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED'
  options.compilerArgs += '--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED'
}
javadoc {
  options {
    addMultilineStringsOption("-add-exports").setValue([
      'jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED',
      'jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED',
      'jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED',
      'jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED',
      'jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED',
      'jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED',
    ])
  }
}
```

When you run your program, you will need to include the `--add-exports` flags as well:

```
java --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
     --add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED \
     --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
     --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
     --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
     --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
  -cp ... \
  my.package.Main
```


## Limitations

One limitation of the javac parser is that the `JCTree` it creates contains
Javadoc comments but omits all other comments.  JavaParser retains all comments,
though some of its handling of comments is buggy, and JavaParser doesn't
support Java syntax added after Java 21.

(Here are the gory details about javac's handling of comments.
In the javac implementation, every `Token` retains all comments
(Javadoc or not) in a a public field `comments`.  All methods look through that
field and only pick out the Javadoc comments.  For example,
`Scanner.nextToken()` populates the Scanner's `docComments` field from the
`Token`'s `comments` field, dropping the non-Javadoc comments, which don't
appear in the `JCTree`.  And the `JCTree` doesn't have access to the `Token`
objects.  If desired, it would be possible to hack around javac's limitations by
reading the file, looking at the line and column numbers of each JCTree and each
comment, and assigning the comments appropriately.)


## Alternatives

[JavaParser](https://javaparser.org/) calls itself "The most popular
parser for the Java language."  It is featureful and easy to use.  The parse
tree includes comments (though it has some bugs related to comment
handling, see above).
Unfortunately, maintenance is sporadic, and JavaParser contains many bugs that
the maintainers do not plan to fix.

One substantive difference is that javac's tree has a single class, `ClassTree`,
for class, interface, enum, record, and annotation type declarations, but
JavaParser represents them with distinct classes.  Likewise, javac has
`VariableTree` for all sorts of variables, including fields, parameters, and
locals.  To transition from JavaParser to javac-parse, you will need to change
types in your code, such as the following:
```
Node -> JCTree
MethodDeclaration -> JCTree.JCMethodDecl
Statement -> JCTree.JCStatement
...
```

[OpenRewrite](https://github.com/openrewrite/rewrite) internally uses the javac
parser, then converts the javac AST to its own AST (which they call an LST) that
includes information about formatting and comments.  However, outputting to Java
source code is a proprietary feature only available in their commercial product.
