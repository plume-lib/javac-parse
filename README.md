# javac-parse:  a wrapper around javac's parser

The parser in javac is the most authoritative and correct parser for Java.
Calling that parser requires setting up various data structures that javac uses.
This small package contains a method that does that setup, making it easy for you
to call javac's parser.

See the [API documentation](http://plumelib.org/javac-parse/api/org/plumelib/util/package-summary.html#package.description).


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
  implementation 'org.plumelib:javac-parse:0.0.1'
}
```

Other build systems are [similar](https://search.maven.org/artifact/org.plumelib/javac-parse/0.0.1/jar).

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


## Why not use the JavaParser project?

The [JavaParser project](https://javaparser.org/) calls itself "The most popular parser for the Java language."
It is featureful and easy to use.
Unfortunately, JavaParser only parses Java 1-17 or higher, with [no plans](https://github.com/javaparser/javaparser/issues/3907) to support Java 18 or higher.
JavaParser also contains many bugs that the maintainers do not plan to fix.
The parser in javac does not have these limitations.
