/*
 * Copyright 2025-2026 DiffPlug
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

import java.io.File;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.java.ExpandWildcardImportsStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

public class ExpandWildcardImports implements FormatterStepFactory {
	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig config) {
		MavenProject project = config.getMavenProject();
		Set<File> typeSolverClasspath = new HashSet<>();

		// Add source directories
		Build build = project.getBuild();
		if (build.getSourceDirectory() != null) {
			typeSolverClasspath.add(Paths.get(build.getSourceDirectory()).toFile());
		}
		if (build.getTestSourceDirectory() != null) {
			typeSolverClasspath.add(Paths.get(build.getTestSourceDirectory()).toFile());
		}

		// Add compile and test classpath elements (dependencies)
		try {
			List<String> compileClasspathElements = project.getCompileClasspathElements();
			for (String element : compileClasspathElements) {
				typeSolverClasspath.add(new File(element));
			}
		} catch (Exception e) {
			// If compilation classpath is not available, continue with available dependencies
		}

		try {
			List<String> testClasspathElements = project.getTestClasspathElements();
			for (String element : testClasspathElements) {
				typeSolverClasspath.add(new File(element));
			}
		} catch (Exception e) {
			// If test classpath is not available, continue with available dependencies
		}

		// Add project artifacts (dependencies)
		for (Artifact artifact : project.getArtifacts()) {
			File file = artifact.getFile();
			if (file != null) {
				typeSolverClasspath.add(file);
			}
		}

		return ExpandWildcardImportsStep.create(typeSolverClasspath, config.getProvisioner());
	}
}
