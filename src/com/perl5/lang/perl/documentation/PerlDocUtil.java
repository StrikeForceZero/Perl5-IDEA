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

package com.perl5.lang.perl.documentation;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.perl5.lang.perl.PerlScopes;
import com.perl5.lang.perl.lexer.PerlElementTypes;
import com.perl5.lang.perl.psi.PerlHeredocOpener;
import com.perl5.lang.perl.psi.PerlHeredocTerminatorElement;
import com.perl5.lang.perl.psi.PerlVariable;
import com.perl5.lang.perl.psi.impl.PerlFileImpl;
import com.perl5.lang.perl.psi.impl.PerlHeredocElementImpl;
import com.perl5.lang.perl.psi.utils.PerlVariableType;
import com.perl5.lang.perl.util.PerlPackageUtil;
import com.perl5.lang.pod.PodSearchHelper;
import com.perl5.lang.pod.parser.psi.*;
import com.perl5.lang.pod.parser.psi.impl.PodFileImpl;
import com.perl5.lang.pod.parser.psi.util.PodFileUtil;
import com.perl5.lang.pod.parser.psi.util.PodRenderUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by hurricup on 26.03.2016.
 */
public class PerlDocUtil implements PerlElementTypes
{
	private static final Map<String, String> myKeywordsRedirections = new THashMap<String, String>();
	private static final Map<String, String> myOperatorsRedirections = new THashMap<String, String>();
	private static final Map<String, String> myVariablesRedirections = new THashMap<String, String>();

	static
	{
		// fixme data and end are not handled, cause atm they are parsed as comments, may be we should fix this
		myKeywordsRedirections.put("__DATA__", "perldata/\"Special Literals\"");
		myKeywordsRedirections.put("__END__", "perldata/\"Special Literals\"");

		myKeywordsRedirections.put("BEGIN", "perlmod/\"BEGIN, UNITCHECK, CHECK, INIT and END\"");
		myKeywordsRedirections.put("CHECK", "perlmod/\"BEGIN, UNITCHECK, CHECK, INIT and END\"");
		myKeywordsRedirections.put("END", "perlmod/\"BEGIN, UNITCHECK, CHECK, INIT and END\"");
		myKeywordsRedirections.put("INIT", "perlmod/\"BEGIN, UNITCHECK, CHECK, INIT and END\"");
		myKeywordsRedirections.put("UNITCHECK", "perlmod/\"BEGIN, UNITCHECK, CHECK, INIT and END\"");

		myKeywordsRedirections.put("DESTROY", "perlobj/\"Destructors\"");

		myKeywordsRedirections.put("AUTOLOAD", "perlsub/\"Autoloading\"");

		myKeywordsRedirections.put("default", "perlsyn/\"Switch Statements\"");
		myKeywordsRedirections.put("given", "perlsyn/\"Switch Statements\"");
		myKeywordsRedirections.put("when", "perlsyn/\"Switch Statements\"");

		myKeywordsRedirections.put("else", "perlsyn/\"Compound Statements\"");
		myKeywordsRedirections.put("elsif", "perlsyn/\"Compound Statements\"");
		myKeywordsRedirections.put("for", "perlsyn/\"Compound Statements\"");
		myKeywordsRedirections.put("foreach", "perlsyn/\"Compound Statements\"");
		myKeywordsRedirections.put("if", "perlsyn/\"Compound Statements\"");
		myKeywordsRedirections.put("unless", "perlsyn/\"Compound Statements\"");
		myKeywordsRedirections.put("until", "perlsyn/\"Compound Statements\"");
		myKeywordsRedirections.put("while", "perlsyn/\"Compound Statements\"");

		myOperatorsRedirections.put("~~", "perlop/\"Smartmatch Operator\"");
		myOperatorsRedirections.put("qr", "perlop/\"qr/STRING/\"");
		myOperatorsRedirections.put("s", "perlop/\"s/PATTERN/\"");
		myOperatorsRedirections.put("m", "perlop/\"m/PATTERN/\"");
		myOperatorsRedirections.put("=>", "perlop/\"Comma Operator\"");

		myVariablesRedirections.put("@ISA", "perlobj/\"A Class is Simply a Package\"");
		myVariablesRedirections.put("@EXPORT", "Exporter/\"How to Export\"");
		myVariablesRedirections.put("@EXPORT_OK", "Exporter/\"How to Export\"");
		myVariablesRedirections.put("%EXPORT_TAGS", "Exporter/\"Specialised Import Lists\"");
		myVariablesRedirections.put("$VERSION", "perlobj/\"VERSION\"");
	}

	// fixme this shit must be refactored
	@Nullable
	public static PsiElement resolveDocLink(String link, PsiElement origin)
	{
		final Project project = origin.getProject();
		PodLinkDescriptor descriptor = PodLinkDescriptor.getDescriptor(link);

		if (descriptor != null)
		{
			PsiFile targetFile = origin.getContainingFile();

			if (descriptor.getFileId() != null)
			{
				// seek file
				String fileId = descriptor.getFileId();
				targetFile = null;

				if (fileId.contains(PerlPackageUtil.PACKAGE_SEPARATOR) || !StringUtil.startsWith(fileId, "perl")) // can be Foo/Bar.pod or Foo/Bar.pm
				{
					String podRelativePath = PodFileUtil.getFilenameFromPackage(fileId);

					for (VirtualFile classRoot : ProjectRootManager.getInstance(project).orderEntries().getClassesRoots())
					{
						VirtualFile targetVirtualFile = classRoot.findFileByRelativePath(podRelativePath);
						if (targetVirtualFile != null)
						{
							if ((targetFile = PsiManager.getInstance(project).findFile(targetVirtualFile)) != null)
							{
								break;
							}
						}
					}

					if (targetFile == null) // pod not found, search for PM
					{
						String packageRelativePath = PerlPackageUtil.getPackagePathByName(fileId);

						for (VirtualFile classRoot : ProjectRootManager.getInstance(project).orderEntries().getClassesRoots())
						{
							VirtualFile targetVirtualFile = classRoot.findFileByRelativePath(packageRelativePath);
							if (targetVirtualFile != null)
							{
								if ((targetFile = PsiManager.getInstance(project).findFile(targetVirtualFile)) != null)
								{
									break;
								}
							}
						}
					}
				}
				else // top level file perl.*
				{
					fileId += "." + PodFileUtil.POD_FILE_EXTENSION;

					PsiFile[] psiFiles = FilenameIndex.getFilesByName(
							origin.getProject(),
							fileId,
							PerlScopes.getProjectAndLibrariesScope(origin.getProject()));
					if (psiFiles.length > 0)
					{
						targetFile = psiFiles[0];
					}
				}

/*				// fixme invoked multiple times
				if (targetFile == null)
				{
					Messages.showErrorDialog(project, "Unable to find pod file: " + fileId, "File Not Found");
				}
*/
			}

			if (targetFile != null)
			{
				if (descriptor.getSection() == null)
				{
					return targetFile;
				}
				else    // seek section
				{
					PodDocumentPattern pattern = PodDocumentPattern.headingAndItemPattern(descriptor.getSection());
					PsiElement targetElement = searchPodElement(targetFile, pattern);
					if (targetElement == null)
					{
						System.err.println("Unable to resolve: " + descriptor.getSection());
					}
					return targetElement;
				}
			}
		}
		return null;
	}

	public static PsiElement getRegexModifierDoc(PsiElement element)
	{
		String text = element.getText();
		assert text != null;
		String anchor = "Modifiers";

		if (text.length() > 0)
		{
			char firstChar = text.charAt(0);
			if (StringUtil.containsChar("msixpn", firstChar))
			{
				anchor = "" + firstChar;
			}
			else if (StringUtil.containsChar("adlux", firstChar))
			{
				anchor = "/" + firstChar;
			}
			else if (StringUtil.containsChar("cgeor", firstChar))
			{
				anchor = "Other Modifiers";
			}
		}

		return resolveDocLink("perlre/\"" + anchor + "\"", element);
	}

	@Nullable
	public static PsiElement getPerlVarDoc(PerlVariable variable)
	{
		final Project project = variable.getProject();

		PerlVariableType actualType = variable.getActualType();
		String variableName = variable.getName();

		if (actualType != null && StringUtil.isNotEmpty(variableName))
		{
			String text = actualType.getSigil() + variableName;

			if (myVariablesRedirections.containsKey(text))
			{
				return resolveDocLink(myVariablesRedirections.get(text), variable);
			}

			if (variable.isBuiltIn())
			{
				PodDocumentPattern pattern = PodDocumentPattern.itemPattern(text);

				if (text.matches("$[123456789]"))
				{
					pattern.setItemPattern("$<digits>");
				}

				return searchPodElementInFile(project, PodSearchHelper.PERL_VAR_FILE_NAME, PodDocumentPattern.itemPattern(text));
			}
		}

		return null;
	}

	@Nullable
	public static PsiElement getPerlFuncDoc(PsiElement element)
	{
		final Project project = element.getProject();
		String text = element.getText();

		String redirect = myKeywordsRedirections.get(text);
		if (redirect != null)
		{
			return resolveDocLink(redirect, element);
		}

		if (text.matches("-[rwxoRWXOeszfdlpSbctugkTBMAC]"))
		{
			text = "-X";
		}

		PodCompositeElement podElement = searchPodElementInFile(project, PodSearchHelper.PERL_FUNC_FILE_NAME, PodDocumentPattern.itemPattern(text));

		return podElement == null ? getPerlOpDoc(element) : podElement;
	}

	@Nullable
	public static PsiElement getPerlOpDoc(@NotNull PsiElement element)
	{
		final Project project = element.getProject();
		String text = element.getText();
		IElementType elementType = element.getNode().getElementType();

		String redirect = myOperatorsRedirections.get(text);
		if (redirect != null)
		{
			return resolveDocLink(redirect, element);
		}


		// fixme use map?
		PodDocumentPattern pattern = PodDocumentPattern.indexPattern(text);
		if (element instanceof PerlHeredocOpener || element instanceof PerlHeredocTerminatorElement || element instanceof PerlHeredocElementImpl)
		{
			pattern.setIndexKey("heredoc");    // searches with X<>
		}
		else if (text.matches("-[rwxoRWXOeszfdlpSbctugkTBMAC]"))
		{
			pattern.setIndexKey("-X");
		}
		else if ("?".equals(text) || ":".equals(text))
		{
			pattern.setIndexKey("?:");
		}

		return searchPodElementInFile(project, PodSearchHelper.PERL_OP_FILE_NAME, pattern);
	}

	protected static PodCompositeElement searchPodElementInFile(Project project, String fileName, PodDocumentPattern pattern)
	{
		PsiFile[] psiFiles = FilenameIndex.getFilesByName(project, fileName, PerlScopes.getProjectAndLibrariesScope(project));
		if (psiFiles.length > 0)
		{
			return searchPodElement(psiFiles[0], pattern);
		}
		return null;
	}

	@Nullable
	protected static PodCompositeElement searchPodElement(PsiFile psiFile, final PodDocumentPattern pattern)
	{
		final List<PodCompositeElement> result = new ArrayList<PodCompositeElement>();

		PsiTreeUtil.processElements(psiFile, new PsiElementProcessor()
		{
			@Override
			public boolean execute(@NotNull PsiElement element)
			{
				if (pattern.accepts(element))
				{
					if (element instanceof PodFormatterX)
					{
						PsiElement container = PsiTreeUtil.getParentOfType(element, PodTitledSection.class);
						if (container != null)
						{
							result.add((PodCompositeElement) container);
							return false;
						}
					}
					else
					{
						result.add((PodCompositeElement) element);
						return false;
					}
				}
				return true;
			}
		});

		return result.isEmpty() ? null : result.get(0);
	}

	protected static String renderPodFile(PodFileImpl file)
	{
		StringBuilder builder = new StringBuilder();

		renderFileToc(file, builder);

		PodRenderUtil.renderPsiRangeAsHTML(file.getFirstNamedBlock(), null, builder, new PodRenderingContext());
		return builder.toString();
	}

	protected static String renderPerlFile(PerlFileImpl file)
	{
		final StringBuilder builder = new StringBuilder();
		final PodRenderingContext renderingContext = new PodRenderingContext();

		renderFileToc(file, builder);

		PsiTreeUtil.processElements(file, new PsiElementProcessor()
		{
			@Override
			public boolean execute(@NotNull PsiElement element)
			{
				if (element.getNode().getElementType() == POD)
				{
					PodRenderUtil.renderPsiRangeAsHTML(element.getFirstChild(), null, builder, renderingContext);
				}
				return true;
			}
		});

		return builder.toString();
	}

	protected static void renderFileToc(PsiFile file, final StringBuilder builder)
	{
		PodTocBuilder tocBuilder = new PodTocBuilder(builder);
		PsiTreeUtil.processElements(file, tocBuilder);
		tocBuilder.adjustLevelTo(0);
	}

	@Nullable
	protected static String renderFile(PsiFile file)
	{
		if (file instanceof PodFileImpl)
		{
			return renderPodFile((PodFileImpl) file);
		}
		else if (file instanceof PerlFileImpl)
		{
			return renderPerlFile((PerlFileImpl) file);
		}
		return null;
	}

	@Nullable
	protected static String renderElement(PodCompositeElement element)
	{
		if (element == null)
			return null;

		PodTitledSection podSection = null;

		if (element instanceof PodTitledSection)
		{
			podSection = (PodTitledSection) element;
		}

		if (podSection != null)
		{
			boolean hasContent = podSection.hasContent();
			PsiElement run = podSection;
			PsiElement lastSection = podSection;

			// detecting first section
			while (true)
			{
				PsiElement prevSibling = run.getPrevSibling();
				if (prevSibling == null)
				{
					break;
				}
				if (prevSibling instanceof PodSection && ((PodSection) prevSibling).hasContent())
				{
					break;
				}
				if (prevSibling instanceof PodTitledSection)
				{
					podSection = (PodTitledSection) prevSibling;
				}
				run = prevSibling;
			}

			// detecting last section
			while (!hasContent)
			{
				PsiElement nextSibling = lastSection.getNextSibling();

				if (nextSibling == null)
					break;
				hasContent = nextSibling instanceof PodSection && ((PodSection) nextSibling).hasContent();

				lastSection = nextSibling;
			}

			StringBuilder builder = new StringBuilder();

			// appending breadcrumbs
			List<String> breadCrumbs = new ArrayList<String>();
			run = podSection.getParent();
			while (true)
			{
				if (run instanceof PodLinkTarget)
				{
					String bcLink = ((PodLinkTarget) run).getPodLink();
					if (StringUtil.isNotEmpty(bcLink))
					{
						breadCrumbs.add(0, PodRenderUtil.getHTMLPsiLink(bcLink, ((PodLinkTarget) run).getPodLinkText()));
					}
				}
				if (run instanceof PsiFile)
				{
					break;
				}
				run = run.getParent();
			}

			if (!breadCrumbs.isEmpty())
			{
				builder.append("<p>");
				builder.append(StringUtil.join(breadCrumbs, ": "));
				builder.append("</p>");
			}

			String closeTag = "";

			if (podSection instanceof PodSectionItem)
			{
				if (((PodSectionItem) podSection).isBulleted())
				{
					builder.append("<ul style=\"fon-size:200%;\">");
					closeTag = "</ul>";
				}
				else
				{
					builder.append("<dl>");
					closeTag = "</dl>";
				}
			}

			builder.append(PodRenderUtil.renderPsiRangeAsHTML(podSection, lastSection));
			builder.append(closeTag);
			return builder.toString();
		}
		return null;
	}

	private static class PodTocBuilder implements PsiElementProcessor
	{
		private final StringBuilder myBuilder;
		private int myHeaderLevel = 0;

		public PodTocBuilder(StringBuilder myBuilder)
		{
			this.myBuilder = myBuilder;
		}

		@Override
		public boolean execute(@NotNull PsiElement element)
		{
			if (element instanceof PodTitledSection && ((PodTitledSection) element).isHeading())
			{
				adjustLevelTo(((PodTitledSection) element).getHeadingLevel());
				myBuilder.append("<li style=\"margin-top:3px;margin-bottom:2px;\">");
				myBuilder.append(PodRenderUtil.getHTMLPsiLink(((PodTitledSection) element)));
				myBuilder.append("</li>");
			}

			return true;
		}

		public void adjustLevelTo(int level)
		{
			while (myHeaderLevel < level)
			{
				if (myHeaderLevel == 0)
				{
					myBuilder.append("<ul style=\"margin-left: 0px;margin-top:0px;margin-bottom:0px;\">");
				}
				else
				{
					myBuilder.append("<ul style=\"margin-left: 20px;margin-top:0px;margin-bottom:0px;\">");
				}
				myHeaderLevel++;
			}
			while (myHeaderLevel > level)
			{
				myBuilder.append("</ul>");
				myHeaderLevel--;
			}
		}
	}

}
