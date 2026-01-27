/*
 * Copyright 2025 DiffPlug
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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.java.ExpandWildcardImportsStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

public class ExpandWildcardImports implements FormatterStepFactory {

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig config) {
		MavenProject project = config.getProject();
		Set<File> typeSolverClasspath = new HashSet<>();

		// Add all main source roots
		project.getCompileSourceRoots().stream()
				.map(File::new)
				.filter(File::exists)
				.forEach(typeSolverClasspath::add);

		// Add all test source roots
		project.getTestCompileSourceRoots().stream()
				.map(File::new)
				.filter(File::exists)
				.forEach(typeSolverClasspath::add);

		// Add compiled dependencies
		project.getArtifacts().stream()
				.map(Artifact::getFile)
				.filter(Objects::nonNull)
				.forEach(typeSolverClasspath::add);

		return ExpandWildcardImportsStep.create(typeSolverClasspath, config.getProvisioner());
	}
}
