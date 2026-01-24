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
}
