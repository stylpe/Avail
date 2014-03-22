/**
 * AbstractCommentImplementation.java
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

import java.util.ArrayList;
import java.util.List;

/**
 * An Avail comment implementation
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
public abstract class AbstractCommentImplementation
{
	/**
	 * The {@link CommentSignature signature} of the class/method the comment
	 * describes.
	 */
	final CommentSignature signature;

	/**
	 * The start line in the module the comment being parsed appears.
	 */
	final int commentStartLine;

	/**
	 * The author of the implementation.
	 */
	final ArrayList<StacksAuthorTag> author;

	/**
	 * Any "@sees" references.
	 */
	final ArrayList<StacksSeeTag> sees;

	/**
	 * The overall description of the implementation
	 */
	final ArrayList<AbstractStacksToken> description;

	/**
	 * The list of categories the implementation applies to.
	 */
	final ArrayList<StacksCategoryTag> categories;

	/**
	 *
	 * Construct a new {@link AbstractCommentImplementation}.
	 *
	 * @param signature
	 * 		The {@link CommentSignature signature} of the class/method the
	 * 		comment describes.
	 * @param commentStartLine
	 * 		The start line in the module the comment being parsed appears.
	 * @param author
	 * 		The {@link StacksAuthorTag author} of the implementation.
	 * @param sees
	 * 		A {@link List} of any {@link StacksSeeTag "@sees"} references.
	 * @param description
	 * 		The overall description of the implementation
	 * @param categories
	 * 		The categories the implementation appears in
	 */
	AbstractCommentImplementation(
		final CommentSignature signature,
		final int commentStartLine,
		final ArrayList<StacksAuthorTag> author,
		final ArrayList<StacksSeeTag> sees,
		final ArrayList<AbstractStacksToken> description,
		final ArrayList<StacksCategoryTag> categories)
	{
		this.signature = signature;
		this.commentStartLine = commentStartLine;
		this.author = author;
		this.sees = sees;
		this.description = description;
		this.categories = categories;
	}

	@Override
	public String toString ()
	{
		return new StringBuilder().append("<li>")
			.append(signature.toString())
			.append("\n</li>").toString();
	}
}
