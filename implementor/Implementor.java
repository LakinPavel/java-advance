package info.kgeorgiy.ja.Laskin_Pavel.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.tools.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class for generating implementations of interfaces and creating JAR files with these implementations.
 * Implements the {@link JarImpler} interface for generating source code and JAR files.
 *
 * @author Laskin_Pavel
 * @see JarImpler
 * @see ImplerException
 */
public class Implementor implements JarImpler {

    /**
     * Constructs a new instance of the {@code Implementor} class.
     * This default constructor initializes an empty {@code Implementor} object.
     */
    public Implementor() {
    }

    /**
     * Buffer for writing generated code to a file.
     */
    private BufferedWriter code;

    /**
     * Set of imports required for the generated class.
     */
    private Set<String> imports;

    /**
     * Main method that accepts command-line arguments to generate a JAR file.
     * Expects three arguments: "-jar", the interface/class name, and the output JAR file path.
     *
     * @param args array of command-line string arguments
     */
    public static void main(String[] args) {
        if (args.length != 3 || !args[0].equals("-jar")) {
            System.err.println("Incorrect arguments. Should be 3 args, sand first of them it's: -jar");
            return;
        }

        String className = args[0];
        String FileNameJar = args[1];

        try {
            Implementor implementor = new Implementor();
            Class<?> token = Class.forName(className);
            Path pathToJarFile = Path.of(FileNameJar);
            implementor.implementJar(token, pathToJarFile);
        } catch (ClassNotFoundException e) {
            System.err.println("Class with name: " + className + " not found");
        } catch (ImplerException e) {
            System.err.println("Can't create jar. ERROR: " + e.getMessage());
        }
    }

    /**
     * Compiles a list of Java files using the system Java compiler.
     *
     * @param files        list of paths to Java files to compile
     * @param dependencies list of dependency classes
     * @param charset      encoding for compilation
     */
    public static void compile(
            final List<Path> files,
            final List<Class<?>> dependencies,
            final Charset charset
    ) {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final String classpath = getClassPath(dependencies).stream()
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));
        final String[] args = Stream.concat(
                Stream.of("-cp", classpath, "-encoding", charset.name()),
                files.stream().map(Path::toString)
        ).toArray(String[]::new);
        final int exitCode = compiler.run(null, null, null, args);
    }

    /**
     * Retrieves a list of classpath paths based on the dependencies.
     *
     * @param dependencies list of classes to determine classpath for
     * @return list of paths to class files
     */
    private static List<Path> getClassPath(final List<Class<?>> dependencies) {
        return dependencies.stream()
                .map(dependency -> {
                    try {
                        return Path.of(dependency.getProtectionDomain().getCodeSource().getLocation().toURI());
                    } catch (final URISyntaxException e) {
                        throw new AssertionError(e);
                    }
                })
                .toList();
    }

    /**
     * Checks if the provided token is a valid interface for implementation.
     *
     * @param token class or interface to check
     * @throws ImplerException if the token is not an interface or is private
     */
    private void checkImpl(Class<?> token) throws ImplerException {
        if (!token.isInterface()) {
            throw new ImplerException("Token should be an interface");
        }
        if (Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Cannot implement private interface");
        }
    }

    /**
     * Generates a JAR file containing the implementation of the specified interface.
     *
     * @param token   interface to generate an implementation for
     * @param jarFile path to the output JAR file
     * @throws ImplerException if the interface cannot be implemented or writing fails
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        checkImpl(token);

        try {
            Path tempDir = Path.of("temp");
            Files.createDirectories(tempDir);

            implement(token, tempDir);

            String packageName = token.getPackageName();
            String className = token.getSimpleName() + "Impl";
            String packagePath = packageName.replace(".", "/");
            Path PathToJavaFile = tempDir.resolve(packageName.replace(".", File.separator))
                    .resolve(className + ".java");
            Path classFile = tempDir.resolve(packageName.replace(".", File.separator)).resolve(className + ".class");

            if (!Files.exists(PathToJavaFile)) {
                throw new ImplerException("Generated Java file not found: " + PathToJavaFile);
            }
            compile(List.of(PathToJavaFile), List.of(token), StandardCharsets.UTF_8);


            try (JarOutputStream jarOutput = new JarOutputStream(new FileOutputStream(jarFile.toFile()))) {
                String entryName = (packageName.isEmpty() ? "" : packagePath + "/") + className + ".class";
                JarEntry entry = new JarEntry(entryName);
                jarOutput.putNextEntry(entry);
                Files.copy(classFile, jarOutput);
                jarOutput.closeEntry();
            }

            clean(tempDir);
        } catch (IOException e) {
            throw new ImplerException("IOException", e);
        }
    }


    /**
     * Visitor for recursively deleting files and directories.
     */
    private static final SimpleFileVisitor<Path> DELETE_VISITOR = new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };

    /**
     * Recursively deletes a directory and all its contents.
     *
     * @param root path to the directory to delete
     */
    protected static void clean(final Path root) {
        if (Files.exists(root)) {
            try {
                Files.walkFileTree(root, DELETE_VISITOR);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }


    /**
     * Generates an implementation of the specified interface in the given directory.
     *
     * @param token interface to generate an implementation for
     * @param root  path to the directory where the generated code will be saved
     * @throws ImplerException if the interface cannot be implemented or writing fails
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        checkImpl(token);

        String packageName = token.getPackageName();
        String className = token.getSimpleName() + "Impl";

        Path packagePath = root.resolve(packageName.replace(".", File.separator));
        Path filePath = packagePath.resolve(className + ".java");

        try {
            Files.createDirectories(packagePath);
            try (BufferedWriter code = Files.newBufferedWriter(filePath)) {
                this.code = code;
                makeCode(token);
            }
        } catch (IOException e) {
            throw new ImplerException("Error writing to file", e);
        }
    }

    /**
     * Writes a line of code to the current buffer.
     *
     * @param line line of code to write
     * @throws ImplerException if an error occurs during writing
     */
    private void writeCode(String line) throws ImplerException {
        try {
            code.write(line);
        } catch (IOException e) {
            throw new ImplerException("can't write " + e.getMessage());
        }
    }


    /**
     * Generates the source code for the interface implementation.
     *
     * @param token interface to generate code for
     * @throws ImplerException if an error occurs during code generation
     */
    private void makeCode(Class<?> token) throws ImplerException {
        this.imports = new HashSet<>();
        Deque<String> fullCode = new ArrayDeque<>();

        fullCode.addLast(makeClassAndMain(token));


        Method[] privateMethods = token.getDeclaredMethods();
        Set<Method> methods = new HashSet<>(Arrays.asList(token.getMethods()));
        methods.addAll(Arrays.asList(privateMethods));

        for (Method method : methods) {
            StringBuilder fullMethod = new StringBuilder();
            fullMethod.append(System.lineSeparator());


            String typeOfMethod = method.getReturnType().getCanonicalName();
            fullMethod.append(beginOfTheMethod(method, typeOfMethod));


            fullMethod.append(defaultType(typeOfMethod));
            if (!typeOfMethod.equals("void")) {
                fullMethod.append(";").append(System.lineSeparator());
            }
            fullMethod.append("\t" + "}");
            fullCode.addLast(fullMethod.toString());
        }


        writeCode(makePackage(token));
        for (String imp : imports) {
            String curImport = "import " + imp + ";";
            writeCode(curImport);
        }
        writeCode(System.lineSeparator());
        for (String el : fullCode) {
            writeCode(el);
        }
        writeCode(System.lineSeparator() + "}");

    }

    /**
     * Determines the access modifier for a method.
     *
     * @param mod modifier flag
     * @return string representing the modifier
     * @throws ImplerException if the modifier is unknown
     */
    private String modifier(int mod) throws ImplerException {
        if (Modifier.isPublic(mod)) {
            return "public";
        } else if (Modifier.isPrivate(mod)) {
            return "private";
        } else if (Modifier.isProtected(mod)) {
            return "protected";
        }
        throw new ImplerException("incorrect modifier");
    }

    /**
     * Generates a default return value for a method's return type.
     *
     * @param typeOfMethod canonical name of the return type
     * @return string with the default return code
     */
    private String defaultType(String typeOfMethod) {
        StringBuilder bodyOfMethod = new StringBuilder("\t" + "\t" + "return ");
        switch (typeOfMethod) {
            case "int", "byte", "short" -> bodyOfMethod.append("0");
            case "long" -> bodyOfMethod.append("0L");
            case "float" -> bodyOfMethod.append("0.0f");
            case "double" -> bodyOfMethod.append("0.0d");
            case "boolean" -> bodyOfMethod.append("false");
            case "void" -> bodyOfMethod = new StringBuilder();
            case "char" -> bodyOfMethod.append("'\\u0000'");
            default -> bodyOfMethod.append("null");
        }
        return bodyOfMethod.toString();
    }

    /**
     * Creates a list of method parameters for generating the method signature.
     *
     * @param method method whose parameters are processed
     * @return list of strings with parameters
     */
    private ArrayList<String> param(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        ArrayList<String> parameters = new ArrayList<>();
        StringBuilder parameterName = new StringBuilder(" a");
        int i = 1;
        int len = parameterTypes.length;
        for (Class<?> p : parameterTypes) {
            String varible;
            if (!p.getPackageName().equals("java.lang")) {
                varible = p.getCanonicalName() + parameterName;
            } else {
                varible = p.getSimpleName() + parameterName;
            }
            if (i < len) {
                varible += ", ";
            }
            parameters.add(varible);
            parameterName.append("a");
            i += 1;
        }
        return parameters;
    }

    /**
     * Constructs a string listing the method's exceptions.
     *
     * @param method method whose exceptions are processed
     * @return {@link StringBuilder} with the list of exceptions
     */
    private StringBuilder exceptions(Method method) {
        StringBuilder except = new StringBuilder();
        Class<?>[] exceptionTypes = method.getExceptionTypes();
        int i = 1;
        int len = exceptionTypes.length;
        if (len != 0) {
            except.append(" throws ");
        }
        for (Class<?> ex : exceptionTypes) {
            if (!ex.getPackageName().equals("java.lang")) {
                imports.add(ex.getCanonicalName());
            }
            String exeption = ex.getSimpleName();
            if (i < len) {
                exeption += ", ";
            }
            except.append(exeption);
            i += 1;
        }
        return except;
    }

    /**
     * Generates the beginning of a method signature.
     *
     * @param method       method to create the signature for
     * @param typeOfMethod return type of the method
     * @return string with the method's beginning
     * @throws ImplerException if the method's modifier is invalid
     */
    private String beginOfTheMethod(Method method, String typeOfMethod) throws ImplerException {
        String methodName = method.getName();
        ArrayList<String> parameters = param(method);

        String modOfMethod = modifier(method.getModifiers());

        StringBuilder titleOfMethod = new StringBuilder("\t" + modOfMethod + " " + typeOfMethod + " " + methodName + "(");
        for (String el : parameters) {
            titleOfMethod.append(el);
        }
        titleOfMethod.append(")");

        titleOfMethod.append(exceptions(method));
        titleOfMethod.append(" {").append(System.lineSeparator());

        return titleOfMethod.toString();
    }

    /**
     * Generates a package declaration string for the class.
     *
     * @param token interface for which the package is created
     * @return package declaration string or empty string if no package exists
     */
    private String makePackage(Class<?> token) {
        return token.getPackageName().isEmpty() ? "" :
                "package " + token.getPackageName() + ";" + System.lineSeparator();
    }

    /**
     * Generates the class header and default constructor.
     *
     * @param token interface for which the class is created
     * @return string with class declaration and constructor
     */
    private String makeClassAndMain(Class<?> token) {
        String nameOfClass = "public class " + token.getSimpleName() +
                "Impl implements " + token.getCanonicalName() + " {" + System.lineSeparator();
        String main = "\t" + "public " + token.getSimpleName() + "Impl() { }" + System.lineSeparator();
        return nameOfClass + main;
    }

}




