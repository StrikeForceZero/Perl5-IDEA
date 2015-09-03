/*
 * Copyright 2015 Alexandr Evstigneev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.perl5.lang.perl.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.Processor;
import com.perl5.lang.perl.idea.stubs.variables.PerlVariableStubIndexKeys;
import com.perl5.lang.perl.lexer.PerlElementTypes;
import com.perl5.lang.perl.psi.PerlVariable;
import com.perl5.lang.perl.util.processors.PerlImportsCollector;

import java.util.*;

/**
 * Created by hurricup on 19.04.2015.
 */
public class PerlHashUtil implements PerlElementTypes
{
	public static final HashSet<String> BUILT_IN = new HashSet<String>(Arrays.asList(
			"!",
			"+",
			"-",
			"^H",
			"ENV",
			"INC",
			"OVERLOAD",
			"SIG",
			"LAST_PAREN_MATCH",
			"LAST_MATCH_START",
			"OS_ERROR",
			"ERRNO"
	));

	public static boolean isBuiltIn(String variable)
	{
		return BUILT_IN.contains(variable);
	}

	/**
	 * Searching project files for global hash definitions by specific package and variable name
	 *
	 * @param project       project to search in
	 * @param canonicalName canonical variable name package::name
	 * @return Collection of found definitions
	 */
	public static Collection<PerlVariable> getGlobalHashDefinitions(Project project, String canonicalName)
	{
		assert canonicalName != null;
		return StubIndex.getElements(PerlVariableStubIndexKeys.KEY_HASH, canonicalName, project, GlobalSearchScope.allScope(project), PerlVariable.class);
	}

	/**
	 * Returns list of defined global hashes
	 *
	 * @param project project to search in
	 * @return collection of variable canonical names
	 */
	public static Collection<String> getDefinedGlobalHashNames(Project project)
	{
		return PerlUtil.getIndexKeysWithoutInternals(PerlVariableStubIndexKeys.KEY_HASH, project);
	}

	/**
	 * Processes all global hashes names with specific processor
	 *
	 * @param project   project to search in
	 * @param processor string processor for suitable strings
	 * @return collection of constants names
	 */
	public static boolean processDefinedGlobalHashNames(Project project, Processor<String> processor)
	{
		return StubIndex.getInstance().processAllKeys(PerlVariableStubIndexKeys.KEY_HASH, project, processor);
	}

	/**
	 * Returns a map of imported hashes names
	 *
	 * @param project   Project to search in
	 * @param namespace namespace to search in
	 * @param file      PsiFile to search in
	 * @return result map
	 */
	public static Map<String, Set<String>> getImportedHashes(Project project, String namespace, PsiFile file)
	{
		PerlImportsCollector collector = new PerlImportsCollector('%', new HashMap<String, Set<String>>());
		PerlUtil.getImportedNames(project, namespace, file, collector);
		return collector.getResult();
	}


}
