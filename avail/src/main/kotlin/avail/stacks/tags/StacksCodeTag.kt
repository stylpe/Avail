/*
 * StacksCodeTag.kt
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
import avail.stacks.tokens.AbstractStacksToken
import org.availlang.json.JSONWriter

/**
 * The Avail comment "@code" tag. This is used for code like syntax styles.
 *
 * THIS IS LIKELY NOT USED AS IT IS A TAG IN DESCRIPTION TEXT
 * DEPRECATED -- DELETE ME
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
class StacksCodeTag
/**
 * Construct a new `StacksCodeTag`.
 *
 * @param codeStyledText
 * The text that is intended to be styled as code.  Can either be
 * quoted text or numerical value.  Multiple tokens should be quoted.
 */
	(
	/**
	 * The text that is intended to be styled as code.  Can either be quoted
	 * text or numerical value.  Multiple tokens should be quoted.
	 */
	private val codeStyledText: AbstractStacksToken) : StacksTag()
{

	/**
	 * @return the codeStyledText
	 */
	fun codeStyledText(): AbstractStacksToken
	{
		return codeStyledText
	}

	override fun toJSON(
		linkingFileMap: LinkingFileMap,
		hashID: Int,
		errorLog: StacksErrorLog,
		position: Int,
		jsonWriter: JSONWriter)
	{
		//DO NOTHING
	}

	override fun toString(): String
	{
		return this@StacksCodeTag.javaClass.simpleName
	}
}
