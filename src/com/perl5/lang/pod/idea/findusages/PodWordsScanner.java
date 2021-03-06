/*
 * Copyright 2016 Alexandr Evstigneev
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

package com.perl5.lang.pod.idea.findusages;

import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.psi.tree.TokenSet;
import com.perl5.lang.pod.PodParserDefinition;
import com.perl5.lang.pod.lexer.PodElementTypes;
import com.perl5.lang.pod.lexer.PodLexerAdapter;

/**
 * Created by hurricup on 03.04.2016.
 */
public class PodWordsScanner extends DefaultWordsScanner implements PodElementTypes
{

	public PodWordsScanner()
	{
		super(new PodLexerAdapter(null),
				PodParserDefinition.IDENTIFIERS,
				PodParserDefinition.COMMENTS,
				TokenSet.EMPTY);
	}
}
