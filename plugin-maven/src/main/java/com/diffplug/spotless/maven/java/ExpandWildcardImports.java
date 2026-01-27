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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;

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

		// Resolve dependencies using Maven's DependencyResolver API
		// This will properly handle reactor dependencies by including their target/classes directories
		// via the WorkspaceReader in the RepositorySystemSession
		typeSolverClasspath.addAll(resolveDependencies(project, config.getRepositorySystem(), config.getRepositorySystemSession()));

		return ExpandWildcardImportsStep.create(typeSolverClasspath, config.getProvisioner());
	}

	private Set<File> resolveDependencies(MavenProject project, RepositorySystem repositorySystem, RepositorySystemSession session) {
		try {
			// Use the project's already-resolved artifacts (which includes transitives)
			// and convert them to Aether dependencies for re-resolution
			// This allows the WorkspaceReader to map reactor modules to target/classes
			List<Dependency> dependencies = project.getArtifacts().stream()
					.map(artifact -> new Dependency(
							new org.eclipse.aether.artifact.DefaultArtifact(
									artifact.getGroupId(),
									artifact.getArtifactId(),
									artifact.getClassifier(),
									artifact.getType(),
									artifact.getVersion()),
							artifact.getScope()))
					.collect(Collectors.toList());

			// Create a collect request with all dependencies
			CollectRequest collectRequest = new CollectRequest();
			collectRequest.setDependencies(dependencies);
			collectRequest.setRepositories(project.getRemoteProjectRepositories());

			// Create a dependency request to resolve all artifacts
			DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);

			// Resolve dependencies - this will use the WorkspaceReader in the session
			// to resolve reactor modules to their target/classes directories
			DependencyResult result = repositorySystem.resolveDependencies(session, dependencyRequest);

			// Extract the resolved artifact files
			return result.getArtifactResults().stream()
					.map(ArtifactResult::getArtifact)
					.map(Artifact::getFile)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
		} catch (DependencyResolutionException e) {
			// If resolution fails, fall back to using the artifacts already attached to the project
			// This ensures the build doesn't fail, but reactor dependencies may not be properly resolved
			// Log at debug level to help troubleshoot dependency resolution issues
			System.err.println("Warning: Failed to resolve dependencies using RepositorySystem, " +
					"falling back to project artifacts. Reactor dependencies may not be properly resolved: " + e.getMessage());
			return project.getArtifacts().stream()
					.map(org.apache.maven.artifact.Artifact::getFile)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
		}
	}
}
