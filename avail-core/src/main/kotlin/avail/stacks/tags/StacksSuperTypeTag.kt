/*
 * StacksSuperTypeTag.kt
 * Copyright © 1993-2022, The Avail Foundation, LLC.
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

package avail.stacks.tags

import avail.stacks.LinkingFileMap
import avail.stacks.StacksErrorLog
import avail.stacks.tokens.QuotedStacksToken
import org.availlang.json.JSONWriter

/**
 * The "@supertype" keyword of an Avail class comment.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
class StacksSuperTypeTag
/**
 * Construct a new [StacksSuperTypeTag].
 *
 * @param superType
 * The supertype keyword indicates the supertype of the class
 * implementation
 */
	(
	/**
	 * The supertype keyword indicates the supertype of the class
	 * implementation
	 */
	private val superType: QuotedStacksToken) : StacksTag()
{

	/**
	 * @return the superType
	 */
	fun superType(): QuotedStacksToken
	{
		return superType
	}

	override fun toJSON(
		linkingFileMap: LinkingFileMap,
		hashID: Int,
		errorLog: StacksErrorLog,
		position: Int,
		jsonWriter: JSONWriter)
	{
		jsonWriter.startObject()
		jsonWriter.write("type")
		jsonWriter.write(superType.lexeme)
		jsonWriter.write("link")
		jsonWriter.write(linkingFileMap.internalLinks!![superType.lexeme])
		jsonWriter.endObject()
	}
}
