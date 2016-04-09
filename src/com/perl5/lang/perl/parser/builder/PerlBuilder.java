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

package com.perl5.lang.perl.parser.builder;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.lang.impl.PsiBuilderImpl;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.tree.IElementType;
import com.perl5.lang.perl.PerlParserDefinition;
import com.perl5.lang.perl.lexer.PerlElementTypes;
import com.perl5.lang.perl.parser.PerlParserImpl;
import com.perl5.lang.perl.parser.PerlTokenData;
import com.perl5.lang.perl.psi.utils.PerlNamesCache;
import com.perl5.lang.perl.util.PerlPackageUtil;
import gnu.trove.THashMap;

import java.util.*;

/**
 * Created by hurricup on 04.05.2015.
 * This wrapper created to be able to store per-parsing data like pragmas, warnings and variables ?
 */
public class PerlBuilder extends GeneratedParserUtilBase.Builder implements PerlElementTypes
{
	private final Map<Pair<IElementType, Integer>, Integer> MARKERS_MAP = new THashMap<Pair<IElementType, Integer>, Integer>();

	private final PerlParserImpl perlParser;
	protected Set<String> KNOWN_SUBS;
	protected Set<String> KNOWN_PACKAGES;
	// flag forces stringification of -identifiers, required for use Package -option;
	boolean stringify = false;
	// flag shows that SQ strings should be re-parsed as QQ strings. Used in use vars expr
	boolean reparseSQString = false;
	// flag allowes additional sigils to parse, required in use vars reparsed strings
	boolean isUseVarsContent = false;
	// flag shows that we are in the interpolated string. Involves additional checkings like space between $var and {hash_key}
	boolean isInterpolated = false;
	// flag set if we are inside of regexp. Safe parsing for array indexes
	boolean isRegex = false;
	// flag marks that interpolated string should stop on >
	boolean stopOnNumericGt = false;
	// this is a stop quote for nexted strings parsing
	IElementType extraStopQuote = null;
	// flag allowes special variable names
	boolean isSpecialVariableNamesAllowed = true;
	/**
	 * This element may be set to make an additional wrapping for strings, like constants and so on
	 */
	IElementType stringWrapper = null;
	Project myProject = getProject();
	private IElementType mySubElementType = SUB;

	public PerlBuilder(PsiBuilder builder, GeneratedParserUtilBase.ErrorState state, PsiParser parser)
	{
		super(builder, state, parser);
		perlParser = (PerlParserImpl) parser;
		initIndexes();
	}

	protected void initIndexes()
	{
		KNOWN_SUBS = PerlNamesCache.getSubsNamesSet(myProject, false);
		KNOWN_PACKAGES = PerlNamesCache.getPackagesNamesSet(myProject, false);
	}

	/**
	 * Return token ahead of current, skips spaces and comments
	 *
	 * @param steps positive or negative steps number to get token
	 * @return token data: type and text
	 */
	public PerlTokenData lookupToken(int steps)
	{
		assert steps != 0;
		int rawStep = 0;
		int step = steps / Math.abs(steps);

		IElementType rawTokenType = null;

		while (steps != 0)
		{
			rawStep += step;
			rawTokenType = rawLookup(rawStep);

			// reached end
			if (rawTokenType == null)
				return null;

			if (!PerlParserDefinition.WHITE_SPACE_AND_COMMENTS.contains(rawTokenType))
				steps -= step;
		}

		// fixme crushes on quick s typing
		return new PerlTokenData(rawTokenType, getOriginalText().subSequence(rawTokenTypeStart(rawStep), rawTokenTypeStart(rawStep + 1)).toString());
	}

	/**
	 * Checks if sub is indexed.
	 *
	 * @param subName canonical sub name Foo::somesub
	 * @return checking result
	 */

	public boolean isKnownSub(String subName)
	{
		return KNOWN_SUBS.contains(subName);
	}


	/**
	 * Checks if package is indexed.
	 *
	 * @return checking result
	 */

	public boolean isKnownPackage(String packageName)
	{
		return KNOWN_PACKAGES.contains(PerlPackageUtil.getCanonicalPackageName(packageName));
	}

	public boolean isStringify()
	{
		return stringify;
	}

	public boolean setStringify(boolean stringify)
	{
		boolean oldState = this.stringify;
		this.stringify = stringify;
		return oldState;
	}

	public boolean isReparseSQString()
	{
		return reparseSQString;
	}

	public boolean setReparseSQString(boolean newState)
	{
		boolean currentState = isReparseSQString();
		reparseSQString = newState;
		return currentState;
	}

	public boolean isUseVarsContent()
	{
		return isUseVarsContent;
	}

	public boolean setUseVarsContent(boolean newState)
	{
		boolean currentState = isUseVarsContent();
		isUseVarsContent = newState;
		return currentState;
	}

	public boolean isInterpolated()
	{
		return isInterpolated;
	}

	public boolean setIsInterpolated(boolean newState)
	{
		boolean currentState = isInterpolated();
		isInterpolated = newState;
		return currentState;
	}

	public boolean isRegex()
	{
		return isRegex;
	}

	public boolean setIsRegex(boolean newState)
	{
		boolean currentState = isRegex();
		isRegex = newState;
		return currentState;
	}

	public boolean isStopOnNumericGt()
	{
		return stopOnNumericGt;
	}

	public void setStopOnNumericGt(boolean stopOnNumericGt)
	{
		this.stopOnNumericGt = stopOnNumericGt;
	}

	public IElementType getExtraStopQuote()
	{
		return extraStopQuote;
	}

	public IElementType setExtraStopQuote(IElementType extraStopQuote)
	{
		IElementType currentExtraStopQuote = getExtraStopQuote();
		this.extraStopQuote = extraStopQuote;
		return currentExtraStopQuote;
	}

	public boolean isSpecialVariableNamesAllowed()
	{
		return isSpecialVariableNamesAllowed;
	}

	public boolean setSpecialVariableNamesAllowed(boolean specialVariableNamesAllowed)
	{
		boolean oldValue = isSpecialVariableNamesAllowed;
		isSpecialVariableNamesAllowed = specialVariableNamesAllowed;
		return oldValue;
	}

	public IElementType getStringWrapper()
	{
		return stringWrapper;
	}

	public IElementType setStringWrapper(IElementType stringWrapper)
	{
		IElementType currentValue = this.stringWrapper;
		this.stringWrapper = stringWrapper;
		return currentValue;
	}

	public PerlParserImpl getPerlParser()
	{
		return perlParser;
	}

	public IElementType popSubElementType()
	{
		IElementType result = mySubElementType;
		mySubElementType = SUB;
		return result;
	}

	public void setNextSubElementType(IElementType subElement)
	{
		this.mySubElementType = subElement;
	}

	@Override
	public Marker mark()
	{
		return super.mark();
	}

	public void registerMarker(IElementType elementType, PsiBuilder.Marker marker)
	{
		if (elementType == null)
			return;

		assert marker instanceof PsiBuilderImpl.ProductionMarker;

		Pair<IElementType, Integer> key = new Pair<IElementType, Integer>(elementType, ((PsiBuilderImpl.ProductionMarker) marker).getStartIndex());
		Integer result = MARKERS_MAP.get(key);
		if (result == null)
		{
			result = 0;
		}
		result++;
		MARKERS_MAP.put(key, result);
	}

	public void dumpMarkers()
	{
		List<Map.Entry<Pair<IElementType, Integer>, Integer>> entryList = new ArrayList<Map.Entry<Pair<IElementType, Integer>, Integer>>(MARKERS_MAP.entrySet());
		Collections.sort(entryList, new Comparator<Map.Entry<Pair<IElementType, Integer>, Integer>>()
		{
			@Override
			public int compare(Map.Entry<Pair<IElementType, Integer>, Integer> o1, Map.Entry<Pair<IElementType, Integer>, Integer> o2)
			{
				return o1.getValue().compareTo(o2.getValue());
			}
		});

		for (Map.Entry<Pair<IElementType, Integer>, Integer> entry : entryList)
		{
			if (entry.getValue() > 1)
			{
				System.err.println(entry.getKey().first + " at " + entry.getKey().second + " parsed " + entry.getValue() + " times");
			}
		}
	}
}
