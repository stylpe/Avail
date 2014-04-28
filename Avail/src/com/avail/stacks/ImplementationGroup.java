/**
 * ImplementationGroup.java
 * Copyright © 1993-2014, The Avail Foundation, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of the contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.avail.stacks;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import com.avail.AvailRuntime;
import com.avail.descriptor.A_String;

/**
 * A grouping of {@linkplain AbstractCommentImplementation implementations}
 * consisting of {@linkplain MethodCommentImplementation methods},
 * {@linkplain SemanticRestrictionCommentImplementation semantic restrictions},
 * {@linkplain GrammaticalRestrictionCommentImplementation grammatical
 * restrictions}, {@linkplain GlobalCommentImplementation globals}, and
 * {@linkplain ClassCommentImplementation classes}.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
public class ImplementationGroup
{
	/**
	 * The name of the implementation.  It is not final because it can be
	 * renamed.
	 */
	private final A_String name;

	/**
	 * @return the name
	 */
	public A_String name ()
	{
		return name;
	}

	/**
	 * The name of the module where this implementation is named and exported
	 * from.
	 */
	private final String namingModule;

	/**
	 * @return the naming module of this implementation
	 */
	public String namingModule ()
	{
		return namingModule;
	}

	/**
	 * All the categories this group belongs to.
	 */
	private final HashSet<String> categories;

	/**
	 * @return the name of all the categories this group belongs to.
	 */
	public HashSet<String> categories ()
	{
		if (categories.size() > 1)
		{
			if (categories.contains("Unclassified"))
			{
				categories.remove("Unclassified");
			}
		}
		return categories;
	}

	/**
	 * The set of all names referring to this implementation
	 */
	private final HashSet<String> aliases;

	/**
	 * The relative file path and file name first, fileName second.
	 */
	private StacksFilename filepath;

	/**
	 * A map keyed by a unique identifier to {@linkplain
	 * MethodCommentImplementation methods}
	 */
	private final HashMap<String,MethodCommentImplementation> methods;

	/**
	 * @return the {@linkplain MethodCommentImplementation methods}
	 */
	public HashMap<String,MethodCommentImplementation> methods ()
	{
		return methods;
	}

	/**
	 * @param newMethod the {@linkplain MethodCommentImplementation method}
	 * to add
	 */
	public void addMethod (final MethodCommentImplementation newMethod)
	{
		methods.put(newMethod.identityCheck(),newMethod);
		addAlias(newMethod.signature().name());

		//Will only ever be one category tag in categories
		for (final QuotedStacksToken category :
			newMethod.categories.get(0).categories())
		{
			categories.add(category.lexeme());
		}
	}

	/**
	 * A list of {@linkplain SemanticRestrictionCommentImplementation
	 * semantic restrictions}
	 */
	private final HashMap<String,SemanticRestrictionCommentImplementation>
		semanticRestrictions;

	/**
	 * @return the {@linkplain SemanticRestrictionCommentImplementation
	 * semantic restrictions}
	 */
	public HashMap<String,SemanticRestrictionCommentImplementation>
		semanticRestrictions ()
	{
		return semanticRestrictions;
	}

	/**
	 * @param newSemanticRestriction the new {@linkplain
	 * SemanticRestrictionCommentImplementation semantic restriction} to add
	 */
	public void addSemanticRestriction (
		final SemanticRestrictionCommentImplementation newSemanticRestriction)
	{
		semanticRestrictions.put(newSemanticRestriction.identityCheck(),
			newSemanticRestriction);
		addAlias(newSemanticRestriction.signature().name());
		//Will only ever be one category tag in categories
		for (final QuotedStacksToken category :
			newSemanticRestriction.categories.get(0).categories())
		{
			categories.add(category.lexeme());
		}
	}

	/**
	 * A list of {@linkplain GrammaticalRestrictionCommentImplementation
	 * grammatical restrictions}
	 */
	private final HashMap<String,GrammaticalRestrictionCommentImplementation>
		grammaticalRestrictions;

	/**
	 * @return the {@linkplain GrammaticalRestrictionCommentImplementation
	 * grammatical restrictions}
	 */
	public HashMap<String,GrammaticalRestrictionCommentImplementation>
		grammaticalRestrictions ()
	{
		return grammaticalRestrictions;
	}

	/**
	 * @param newGrammaticalRestriction the {@linkplain
	 * GrammaticalRestrictionCommentImplementation grammatical restrictions}
	 * to add
	 */
	public void addGrammaticalRestriction (
		final GrammaticalRestrictionCommentImplementation
			newGrammaticalRestriction)
	{
		grammaticalRestrictions.put(newGrammaticalRestriction.identityCheck(),
			newGrammaticalRestriction);
		addAlias(newGrammaticalRestriction.signature().name());

		//Will only ever be one category tag in categories
		for (final QuotedStacksToken category :
			newGrammaticalRestriction.categories.get(0).categories())
		{
			categories.add(category.lexeme());
		}
	}

	/**
	 * A {@linkplain ClassCommentImplementation class comment}
	 */
	private ClassCommentImplementation classImplementation;

	/**
	 * @return the classImplementation
	 */
	public ClassCommentImplementation classImplementation ()
	{
		return classImplementation;
	}
	/**
	 * @param aClassImplemenataion the classImplementation to set
	 */
	public void classImplemenataion (
		final ClassCommentImplementation aClassImplemenataion)
	{
		this.classImplementation = aClassImplemenataion;
		addAlias(aClassImplemenataion.signature().name());

		for (final QuotedStacksToken category :
			aClassImplemenataion.categories.get(0).categories())
		{
			categories.add(category.lexeme());
		}
	}

	/**
	 * A module {@linkplain GlobalCommentImplementation global}.
	 */
	private GlobalCommentImplementation global;

	/**
	 * @return the global
	 */
	public GlobalCommentImplementation global ()
	{
		return global;
	}
	/**
	 * @param aGlobal the global to set
	 */
	public void global (final GlobalCommentImplementation aGlobal)
	{
		this.global = aGlobal;
		addAlias(aGlobal.signature().name());
		for (final QuotedStacksToken category :
			aGlobal.categories.get(0).categories())
		{
			categories.add(category.lexeme());
		}
	}

	/**
	 * Construct a new {@link ImplementationGroup}.
	 *
	 * @param name The name of the implementation
	 * @param namingModule The name of the module where the implementation was
	 * 		first named and exported from.
	 * @param filename The name of the html file that will represent this group.
	 */
	public ImplementationGroup (final A_String name,
		final String namingModule, final StacksFilename filename)
	{
		this.name = name;
		this.methods = new HashMap<String,MethodCommentImplementation>();
		this.semanticRestrictions =
			new HashMap<String,SemanticRestrictionCommentImplementation>();
		this.grammaticalRestrictions =
			new HashMap<String,GrammaticalRestrictionCommentImplementation>();
		this.aliases = new HashSet<String>();
		this.namingModule = namingModule;
		this.categories = new HashSet<String>();
		this.filepath = filename;
	}

	/**
	 * Construct a new {@link ImplementationGroup}.
	 *
	 * @param group The {@link ImplementationGroup} to copy.
	 * @param fileName A pair with the fileName and path first and just the
	 * 		name of the html file that will represent this group second.
	 * @param namingModule The name of the module where the implementation was
	 * 		first named and exported from.
	 * @param name The name of the implementation
	 */
	public ImplementationGroup (final ImplementationGroup group,
		final StacksFilename fileName, final String namingModule,
		final A_String name)
	{
		this.name = name;
		this.methods = group.methods();
		this.semanticRestrictions = group.semanticRestrictions();
		this.grammaticalRestrictions = group.grammaticalRestrictions();
		this.aliases = group.aliases();
		this.global = group.global();
		this.classImplementation = group.classImplementation();
		this.namingModule = namingModule;
		this.categories = group.categories();
		this.filepath = fileName;
	}

	/**
	 * Create HTML file from implementation
	 * @param outputPath
	 *        The {@linkplain Path path} to the output {@linkplain
	 *        BasicFileAttributes#isDirectory() directory} for documentation and
	 *        data files.
	 * @param htmlOpenContent
	 * 		HTML document opening tags (e.g. header etc)
	 * @param htmlCloseContent
	 * 		HTML document closing the document tags
	 * @param synchronizer
	 * 		The {@linkplain StacksSynchronizer} used to control the creation
	 * 		of Stacks documentation
	 * @param runtime
	 *      An {@linkplain AvailRuntime runtime}.
	 * @param htmlFileMap
	 * 		A mapping object for all html files in stacks
	 * @param implementationProperties
	 * 		The file path location of the HTML properties used to generate
	 * 		the bulk of the inner html of the implementations.
	 * @param startingTabCount
	 * 		The number of tabs in to start with.
	 * @param nameOfGroup
	 * 		The name of the implementation as it is to be displayed.
	 * @param errorLog The accumulating {@linkplain StacksErrorLog}
	 */
	public void toHTML(final Path outputPath,
		final String htmlOpenContent, final String htmlCloseContent,
		final StacksSynchronizer synchronizer,
		final AvailRuntime runtime,
		final HTMLFileMap htmlFileMap,
		final Path implementationProperties,
		final int startingTabCount,
		final String nameOfGroup,
		final StacksErrorLog errorLog)
	{
		if (categories.size() > 1)
		{
			if (categories.contains("Unclassified"))
			{
				categories.remove("Unclassified");
			}
		}

		final StringBuilder stringBuilder = new StringBuilder()
			.append(htmlOpenContent)
			.append(tabs(1) + "<h2 "
				+ HTMLBuilder.tagClass(HTMLClass.classMethodHeading) + ">")
			.append(nameOfGroup)
			.append("</h2>\n");

		if (!methods.isEmpty())
		{
			if (!grammaticalRestrictions.isEmpty())
			{
				final int listSize = grammaticalRestrictions.size();
				final ArrayList<GrammaticalRestrictionCommentImplementation>
					restrictions = new ArrayList
						<GrammaticalRestrictionCommentImplementation>();
				restrictions.addAll(grammaticalRestrictions.values());
				if (listSize > 1)
				{
					for (int i = 1; i < listSize; i++)
					{
						restrictions.get(0)
							.mergeGrammaticalRestrictionImplementations(
								restrictions.get(i));
					}

				}
				stringBuilder
					.append(restrictions.get(0)
						.toHTML(htmlFileMap,nameOfGroup, errorLog));
			}

			stringBuilder.append(tabs(1) + "<h4 "
					+ HTMLBuilder.tagClass(HTMLClass.classMethodSectionHeader)
					+ ">Definitions:</h4>\n")
				.append(tabs(1)
					+ "<div "
					+ HTMLBuilder
						.tagClass(HTMLClass.classMethodSectionContent) + ">\n");

			for (final MethodCommentImplementation implementation :
				methods.values())
			{
				stringBuilder.append(implementation.toHTML(htmlFileMap,
					nameOfGroup, errorLog));
			}

			stringBuilder.append(tabs(1) + "</div>\n");

			if (!semanticRestrictions.isEmpty())
			{
				stringBuilder
					.append(tabs(1) + "<h4 "
						+ HTMLBuilder
							.tagClass(HTMLClass.classMethodSectionHeader)
						+ ">Semantic restrictions:</h4>\n")
					.append(tabs(1) + "<div "
						+ HTMLBuilder
							.tagClass(HTMLClass.classMethodSectionContent)
						+ ">\n");

				for (final SemanticRestrictionCommentImplementation
					implementation : semanticRestrictions.values())
				{
					stringBuilder.append(implementation.toHTML(htmlFileMap,
						nameOfGroup, errorLog));
				}

				stringBuilder.append(tabs(1) + "</div>\n");
			}
		}
		else if (!(global == null))
		{
			stringBuilder.append(global().toHTML(htmlFileMap, nameOfGroup,
				errorLog));
		}
		else if (!(classImplementation == null))
		{
			stringBuilder.append(classImplementation
				.toHTML(htmlFileMap, nameOfGroup, errorLog));
		}

		Path fullFilePath = outputPath;
		final String [] directoryTree = filepath.pathName().split("/");

		for (final String directory : directoryTree)
		{
			fullFilePath = fullFilePath.resolve(directory);
		}

		final StacksOutputFile htmlFile = new StacksOutputFile(
			fullFilePath, synchronizer,
			filepath.leafFilename(),
			runtime, name.asNativeString());

		htmlFile.write(stringBuilder.append(htmlCloseContent).toString());
	}

	/**
	 * Determine if the implementation is populated.
	 * @return
	 */
	public boolean isPopulated()
	{
		return (!methods.isEmpty() ||
			!(global == null) ||
			!(classImplementation == null));
	}

	/**
	 * @return A set of category String names for this implementation.
	 */
	public HashSet<String> getCategorySet()
	{
		final HashSet<String> categorySet = new HashSet<String>();

		for (final MethodCommentImplementation implementation :
			methods.values())
		{
			categorySet.addAll(implementation.getCategorySet());
		}

		for (final GrammaticalRestrictionCommentImplementation implementation :
			grammaticalRestrictions.values())
		{
			categorySet.addAll(implementation.getCategorySet());
		}

		for (final SemanticRestrictionCommentImplementation implementation :
			semanticRestrictions.values())
		{
			categorySet.addAll(implementation.getCategorySet());
		}

		if (!(classImplementation == null))
		{
			categorySet.addAll(classImplementation.getCategorySet());
		}
		if (!(global == null))
		{
			categorySet.addAll(global.getCategorySet());
		}

		return categorySet;
	}

	/**
	 * @param numberOfTabs
	 * 		the number of tabs to insert into the string.
	 * @return
	 * 		a String consisting of the number of tabs requested in
	 * 		in numberOfTabs.
	 */
	public String tabs(final int numberOfTabs)
	{
		final StringBuilder stringBuilder = new StringBuilder();
		for (int i = 1; i <= numberOfTabs; i++)
		{
			stringBuilder.append('\t');
		}
		return stringBuilder.toString();
	}

	/**
	 * @return the aliases
	 */
	public HashSet<String> aliases ()
	{
		return aliases;
	}

	/**
	 * @param alias the alias to add to the set
	 */
	public void addAlias (final String alias)
	{
		aliases.add(alias);
	}

	/**
	 * @return the filepath
	 */
	public StacksFilename filepath ()
	{
		return filepath;
	}

	/**
	 * @param newFilepath the filepath to set
	 */
	public void filepath (final StacksFilename newFilepath)
	{
		this.filepath = newFilepath;
	}

	/**
	 * Generate json objects of alias names
	 * @return
	 */
	public ArrayList<String> aliasFilePathsJSON()
	{
		final ArrayList<String> jsonFilePaths = new ArrayList<String>();
		for (final String alias : aliases)
		{
			jsonFilePaths
				.add("\"" + alias + "\" : " + "\"" + filepath() + "\"");
		}
		return jsonFilePaths;
	}

	/**
	 * Merge the input {@linkplain ImplementationGroup group} with this group
	 * @param group
	 * 		The {@linkplain ImplementationGroup} to merge into this group.
	 */
	public void mergeWith (final ImplementationGroup group)
	{
		classImplementation = group.classImplementation();
		global = group.global();
		methods.putAll(group.methods());
		semanticRestrictions.putAll(group.semanticRestrictions());
		grammaticalRestrictions.putAll(group.grammaticalRestrictions());
	}
}
