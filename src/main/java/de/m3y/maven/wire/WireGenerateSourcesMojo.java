package de.m3y.maven.wire;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Stopwatch;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import com.squareup.wire.java.JavaGenerator;
import com.squareup.wire.java.Profile;
import com.squareup.wire.java.ProfileLoader;
import com.squareup.wire.schema.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

/**
 * A maven mojo that executes Wire's <a href="https://square.github.io/wire/wire_compiler/#java">JavaGenerator</a>.
 *
 * Based on original <a href="https://github.com/square/wire/tree/3.0.2/wire-maven-plugin">plugin</a> which got
 * <a href="https://github.com/square/wire/pull/1326">dropped</a> by the project.
 */
@Mojo(name = "generate-sources",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        threadSafe = true
)
public class WireGenerateSourcesMojo extends AbstractMojo {
    /**
     * The root of the proto source directory.
     *
     * If configured, wire.protoPaths will be ignored!
     */
    @Parameter(
            property = "wire.protoSourceDirectory",
            defaultValue = "${project.basedir}/src/main/proto")
    private String protoSourceDirectory;

    /**
     * The root of one or more proto source directories.
     *
     * Only used if wire.protoSourceDirectory is not configured!
     */
    @Parameter(property = "wire.protoPaths")
    private String[] protoPaths;

    /**
     * True for emitted types to implement android.os.Parcelable.
     *
     * See https://square.github.io/wire/wire_compiler/#java
     */
    @Parameter(property = "wire.android", defaultValue = "false")
    private boolean emitAndroid;

    /**
     * True to emit code that uses reflection for reading, writing, and toString
     * methods which are normally implemented with generated code.
     *
     * See https://square.github.io/wire/wire_compiler/#java
     */
    @Parameter(property = "wire.compact", defaultValue = "false")
    private boolean emitCompact;

    /**
     * Configures Wire compiler Proto types pruning 'parts to be kept' of the generated sources.
     *
     * This list should contain package names (suffixed with `.*`) and type names
     * only. It should not contain member names.
     *
     * Example: 'com.example.pizza.*'
     *
     * https://square.github.io/wire/wire_compiler/#pruning
     */
    @Parameter(property = "wire.includes")
    private String[] includes;

    /**
     * Configures Wire compiler Proto types pruning 'parts to be removed' of the generated sources.
     *
     * This list should contain package names (suffixed with `.*`) and type names
     * only. It should not contain member names.
     *
     * Example: 'com.example.sales.*'
     *
     * https://square.github.io/wire/wire_compiler/#pruning
     */
    @Parameter(property = "wire.excludes")
    private String[] excludes;

    /**
     * List of proto files to compile relative to ${protoPaths}.
     */
    @Parameter(property = "wire.protoFiles", required = true)
    private String[] protoFiles;

    /**
     * Location for the wire compiler generated sources.
     */
    @Parameter(
            property = "wire.generatedSourceDirectory",
            defaultValue = "${project.build.directory}/generated-sources/wire")
    private String generatedSourceDirectory;

    /**
     * Current Maven project.
     */
    @Parameter(
            defaultValue = "${project}",
            required = true,
            readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        // Add the directory into which generated sources are placed as a compiled source root.
        project.addCompileSourceRoot(generatedSourceDirectory);

        try {
            List<String> directories = protoPaths != null && protoPaths.length > 0
                    ? Arrays.asList(protoPaths)
                    : Collections.singletonList(protoSourceDirectory);
            List<String> protoFilesList = Arrays.asList(protoFiles);

            Schema schema = loadSchema(directories, protoFilesList);
            Profile profile = loadProfile(schema);

            PruningRules pruningRules = pruningRules();
            if (!pruningRules.isEmpty()) {
                schema = retainRoots(pruningRules, schema);
            }

            JavaGenerator javaGenerator = JavaGenerator.get(schema)
                    .withAndroid(emitAndroid)
                    .withCompact(emitCompact)
                    .withProfile(profile);

            for (ProtoFile protoFile : schema.getProtoFiles()) {
                if (!protoFilesList.isEmpty() && !protoFilesList.contains(protoFile.getLocation().getPath())) {
                    continue; // Don't emit anything for files not explicitly compiled.
                }

                for (Type type : protoFile.getTypes()) {
                    Stopwatch stopwatch = Stopwatch.createStarted();
                    TypeSpec typeSpec = javaGenerator.generateType(type);
                    ClassName javaTypeName = javaGenerator.generatedTypeName(type);
                    writeJavaFile(javaTypeName, typeSpec, type.getLocation().withPathOnly());
                    getLog().info(String.format("Generated %s in %s", javaTypeName, stopwatch));
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Wire Plugin: Failure compiling proto sources.", e);
        }
    }

    private PruningRules pruningRules() {
        PruningRules.Builder pruningRulesBuilder = new PruningRules.Builder();
        if (includes != null) {
            for (String identifier : includes) {
                pruningRulesBuilder.addRoot(identifier);
            }
        }
        if (excludes != null) {
            for (String identifier : excludes) {
                pruningRulesBuilder.prune(identifier);
            }
        }
        return pruningRulesBuilder.build();
    }

    private Schema retainRoots(PruningRules pruningRules, Schema schema) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        int oldSize = countTypes(schema);

        Schema prunedSchema = schema.prune(pruningRules);
        int newSize = countTypes(prunedSchema);

        for (String rule : pruningRules.unusedRoots()) {
            getLog().warn(String.format("Unused include: %s", rule));
        }
        for (String rule : pruningRules.unusedPrunes()) {
            getLog().warn(String.format("Unused exclude: %s", rule));
        }

        getLog().info(String.format("Pruned schema from %s types to %s types in %s",
                oldSize, newSize, stopwatch));

        return prunedSchema;
    }

    private int countTypes(Schema prunedSchema) {
        int result = 0;
        for (ProtoFile protoFile : prunedSchema.getProtoFiles()) {
            result += protoFile.getTypes().size();
        }
        return result;
    }

    private Schema loadSchema(List<String> directories, List<String> protos) throws IOException {
        Stopwatch stopwatch = Stopwatch.createStarted();

        SchemaLoader schemaLoader = new SchemaLoader();
        for (String directory : directories) {
            schemaLoader.addSource(new File(directory));
        }
        for (String proto : protos) {
            schemaLoader.addProto(proto);
        }
        Schema schema = schemaLoader.load();

        if (getLog().isDebugEnabled()) {
            getLog().debug("Found in " + directories + " proto files " + schema.getProtoFiles());
        }
        getLog().info(String.format("Loaded %s proto files in %s",
                schema.getProtoFiles().size(), stopwatch));

        return schema;
    }

    private Profile loadProfile(Schema schema) throws IOException {
        String profileName = emitAndroid ? "android" : "java";
        return new ProfileLoader(profileName)
                .schema(schema)
                .load();
    }

    private void writeJavaFile(ClassName javaTypeName, TypeSpec typeSpec, Location location)
            throws IOException {
        JavaFile.Builder builder = JavaFile.builder(javaTypeName.packageName(), typeSpec)
                .addFileComment("$L", "Code generated by Wire protocol buffer compiler, do not edit.");
        if (location != null) {
            builder.addFileComment("\nSource file: $L", location);
        }
        JavaFile javaFile = builder.build();
        try {
            javaFile.writeTo(new File(generatedSourceDirectory));
        } catch (IOException e) {
            throw new IOException("Failed to write " + javaFile.packageName + "."
                    + javaFile.typeSpec.name + " to " + generatedSourceDirectory, e);
        }
    }
}
