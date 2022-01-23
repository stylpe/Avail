/*
 * AvailFileElement.kt
 * Copyright © 1993-2021, The Avail Foundation, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of the copyright holder nor the names of the contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
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

package org.availlang.ide.anvil.language

import com.intellij.psi.impl.source.tree.FileElement
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.psi.util.elementType
import org.availlang.ide.anvil.language.psi.AvailElementType
import org.availlang.ide.anvil.language.psi.AvailRootElementType
import org.availlang.ide.anvil.language.psi.AvailFile
import org.availlang.ide.anvil.language.psi.AvailManifestEntryPsiElement

/**
 * A `AvailFileElement` is TODO: Document this!
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
class AvailFileElement constructor(
	text: CharSequence,
	val availFile: AvailFile
) : FileElement(AvailRootElementType, text)
{
	override fun getFirstChildNode(): TreeElement?
	{
		val node = availFile.firstChild ?: return null
		return AnvilManifestEntryTreeElement(
			(node as AvailManifestEntryPsiElement).elementType
				as AvailElementType,
			node,
			node.manifestEntry
		)
	}

	override fun getLastChildNode(): TreeElement?
	{
		val node = availFile.lastChild ?: return null
		return AnvilManifestEntryTreeElement(
			(node as AvailManifestEntryPsiElement).elementType
				as AvailElementType,
			node,
			node.manifestEntry
		)
	}
}
