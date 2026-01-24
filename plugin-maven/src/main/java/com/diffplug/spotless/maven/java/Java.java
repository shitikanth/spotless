/*
 * Copyright 2016-2025 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.spotless.maven.java;

import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;

import com.diffplug.spotless.generic.LicenseHeaderStep;
import com.diffplug.spotless.maven.FormatterConfig;
import com.diffplug.spotless.maven.FormatterFactory;
import com.diffplug.spotless.maven.generic.LicenseHeader;

/**
 * A {@link FormatterFactory} implementation that corresponds to {@code <java>...</java>} configuration element.
 * <p>
 * It defines a formatter for java source files that can execute both language agnostic (e.g. {@link LicenseHeader})
 * and java-specific (e.g. {@link Eclipse}) steps.
 */
public class Java extends FormatterFactory {

	private static final String LICENSE_HEADER_DELIMITER = LicenseHeaderStep.DEFAULT_JAVA_HEADER_DELIMITER;

	@Override
	public Set<String> defaultIncludes(MavenProject project) {
		Path projectDir = project.getBasedir().toPath();
		Build build = project.getBuild();
		return Stream.of(build.getSourceDirectory(), build.getTestSourceDirectory())
				.map(Paths::get)
				.map(projectDir::relativize)
				.map(Java::fileMask)
				.collect(toSet());
	}

	@Override
	public String licenseHeaderDelimiter() {
		return LICENSE_HEADER_DELIMITER;
	}

	public void addEclipse(Eclipse eclipse) {
		addStepFactory(eclipse);
	}

	public void addGoogleJavaFormat(GoogleJavaFormat googleJavaFormat) {
		addStepFactory(googleJavaFormat);
	}

	public void addImportOrder(ImportOrder importOrder) {
		addStepFactory(importOrder);
	}

	public void addPalantirJavaFormat(PalantirJavaFormat palantirJavaFormat) {
		addStepFactory(palantirJavaFormat);
	}

	public void addRemoveUnusedImports(RemoveUnusedImports removeUnusedImports) {
		addStepFactory(removeUnusedImports);
	}

	public void addForbidWildcardImports(ForbidWildcardImports forbidWildcardImports) {
		addStepFactory(forbidWildcardImports);
	}

	public void addExpandWildcardImports(ExpandWildcardImports expandWildcardImports) {
		addStepFactory(expandWildcardImports);
	}

	public void addForbidModuleImports(ForbidModuleImports forbidModuleImports) {
		addStepFactory(forbidModuleImports);
	}

	public void addFormatAnnotations(FormatAnnotations formatAnnotations) {
		addStepFactory(formatAnnotations);
	}

	public void addCleanthat(CleanthatJava cleanthat) {
		addStepFactory(cleanthat);
	}

	@Override
	protected Optional<Set<File>> getProjectClasspath(FormatterConfig config) {
		return config.getMavenProject().map(project -> {
			try {
				Set<File> classpath = new HashSet<>();
				// Add compile classpath
				for (String element : project.getCompileClasspathElements()) {
					classpath.add(new File(element));
				}
				// Add test classpath
				for (String element : project.getTestClasspathElements()) {
					classpath.add(new File(element));
				}
				return classpath;
			} catch (DependencyResolutionRequiredException e) {
				// If we can't resolve dependencies, return empty set
				return Collections.<File>emptySet();
			}
		});
	}

	private static String fileMask(Path path) {
		String dir = path.toString();
		if (!dir.endsWith(File.separator)) {
			dir += File.separator;
		}
		return dir + "**" + File.separator + "*.java";
	}
}
