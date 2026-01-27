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

import static java.util.Collections.emptyMap;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class ExpandWildcardImportsStepTest extends MavenIntegrationHarness {

	@Test
	void testExpandWildcardImports() throws Exception {
		writePomWithJavaSteps("<expandWildcardImports/>");

		// Create the supporting classes needed for import resolution
		setFile("src/main/java/foo/bar/AnotherClassInSamePackage.java")
				.toResource("java/expandwildcardimports/AnotherClassInSamePackage.test");
		setFile("src/main/java/foo/bar/baz/AnotherImportedClass.java")
				.toResource("java/expandwildcardimports/AnotherImportedClass.test");
		
		// Create the annotation class that's used in the test
		setFile("src/main/java/org/example/SomeAnnotation.java")
				.toContent("package org.example;\n\npublic @interface SomeAnnotation {}\n");

		// Set the main file to format
		String path = "src/main/java/foo/bar/JavaClassWithWildcards.java";
		setFile(path).toResource("java/expandwildcardimports/JavaClassWithWildcardsUnformatted.test");

		// Run spotless:apply
		mavenRunner().withArguments("spotless:apply").runNoError();

		// Verify the wildcards were expanded
		assertFile(path).sameAsResource("java/expandwildcardimports/JavaClassWithWildcardsFormatted.test");
	}

	@Test
	void testExpandWildcardImportsMultiModule() throws Exception {
		/*
		Create a multi-module project with the following structure:

		    /junit-tmp-dir
		    ├── common
		    │   ├── pom.xml
		    │   └── src/main/java
		    │       ├── foo/bar/AnotherClassInSamePackage.java
		    │       ├── foo/bar/baz/AnotherImportedClass.java
		    │       └── org/example/SomeAnnotation.java
		    ├── app
		    │   ├── pom.xml
		    │   └── src/main/java
		    │       └── foo/bar/JavaClassWithWildcards.java (depends on common module)
		    ├── pom.xml (parent)
		    ├── .mvn
		    ├── mvnw
		    └── mvnw.cmd
		 */

		// Create parent POM
		String[] modules = new String[]{"common", "app"};
		String[] configuration = new String[]{
				"<java>",
				"  <expandWildcardImports/>",
				"</java>"
		};
		Map<String, Object> parentPomParams = buildPomXmlParams(null, null, null, configuration, modules, null, null);
		setFile("pom.xml").toContent(createPomXmlContent("/multi-module/pom-parent.xml.mustache", parentPomParams));

		// Create common module with supporting classes
		setFile("common/pom.xml").toContent(createChildPom("common"));
		setFile("common/src/main/java/foo/bar/AnotherClassInSamePackage.java")
				.toResource("java/expandwildcardimports/AnotherClassInSamePackage.test");
		setFile("common/src/main/java/foo/bar/baz/AnotherImportedClass.java")
				.toResource("java/expandwildcardimports/AnotherImportedClass.test");
		setFile("common/src/main/java/org/example/SomeAnnotation.java")
				.toContent("package org.example;\n\npublic @interface SomeAnnotation {}\n");

		// Create app module that depends on common
		setFile("app/pom.xml").toContent(createChildPomWithDependency("app", "common"));
		String path = "app/src/main/java/foo/bar/JavaClassWithWildcards.java";
		setFile(path).toResource("java/expandwildcardimports/JavaClassWithWildcardsUnformatted.test");

		// Format all files in the multi-module project without requiring 'mvn install' first
		// This tests that spotless:apply works in a reactor build without pre-installing dependencies
		mavenRunner().withArguments("spotless:apply").runNoError();

		// Verify the wildcards were expanded in app module
		assertFile(path).sameAsResource("java/expandwildcardimports/JavaClassWithWildcardsFormatted.test");
	}

	private String createChildPom(String childId) throws IOException {
		return createPomXmlContent("/multi-module/pom-child.xml.mustache", Map.of("childId", childId));
	}

	private String createChildPomWithDependency(String childId, String dependencyModule) throws IOException {
		String childPom = createChildPom(childId);
		// Add dependency to the common module
		String dependency = """
				<dependencies>
				    <dependency>
				        <groupId>com.diffplug.spotless</groupId>
				        <artifactId>spotless-maven-plugin-tests-child-%s</artifactId>
				        <version>1.0.0-SNAPSHOT</version>
				    </dependency>
				</dependencies>
				""".formatted(dependencyModule);
		// Insert before </project>
		return childPom.replace("</project>", dependency + "\n</project>");
	}
}
