/*
 * P_SetStyleFunction.kt
 * Copyright © 1993-2021, The Avail Foundation, LLC.
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

package com.avail.interpreter.primitive.phrases

import com.avail.compiler.splitter.MessageSplitter
import com.avail.descriptor.functions.A_Function
import com.avail.descriptor.methods.A_Definition
import com.avail.descriptor.methods.A_Definition.Companion.updateStylers
import com.avail.descriptor.methods.A_Styler
import com.avail.descriptor.methods.A_Styler.Companion.module
import com.avail.descriptor.methods.StylerDescriptor.Companion.newStyler
import com.avail.descriptor.module.A_Module.Companion.moduleAddStyler
import com.avail.descriptor.objects.ObjectTypeDescriptor.Companion.Styles.stylerFunctionType
import com.avail.descriptor.representation.NilDescriptor.Companion.nil
import com.avail.descriptor.sets.A_Set.Companion.setUnionCanDestroy
import com.avail.descriptor.sets.A_Set.Companion.setWithElementCanDestroy
import com.avail.descriptor.sets.SetDescriptor.Companion.set
import com.avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tuple
import com.avail.descriptor.types.A_Type
import com.avail.descriptor.types.AbstractEnumerationTypeDescriptor.Companion.enumerationWith
import com.avail.descriptor.types.FunctionTypeDescriptor.Companion.functionType
import com.avail.descriptor.types.PrimitiveTypeDescriptor.Types.DEFINITION
import com.avail.descriptor.types.PrimitiveTypeDescriptor.Types.TOP
import com.avail.exceptions.AvailErrorCode.E_CANNOT_DEFINE_DURING_COMPILATION
import com.avail.exceptions.AvailErrorCode.E_LOADING_IS_OVER
import com.avail.exceptions.AvailErrorCode.E_STYLER_ALREADY_SET_BY_THIS_MODULE
import com.avail.interpreter.Primitive
import com.avail.interpreter.Primitive.Flag.CanSuspend
import com.avail.interpreter.Primitive.Flag.Unknown
import com.avail.interpreter.execution.Interpreter

/**
 * **Primitive:** Create and install a [styler][A_Styler], from the given
 * [function][A_Function] and for the given [definition][A_Definition]. Fail if
 * the definition already has a styling function.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
@Suppress("unused")
object P_SetStyleFunction : Primitive(2, CanSuspend, Unknown)
{
	override fun attempt(interpreter: Interpreter): Result
	{
		interpreter.checkArgumentCount(2)
		val definition: A_Definition = interpreter.argument(0)
		val function: A_Function = interpreter.argument(1)
		val loader = interpreter.fiber().availLoader()
			?: return interpreter.primitiveFailure(E_LOADING_IS_OVER)
		val module = loader.module
		if (!loader.phase().isExecuting)
		{
			return interpreter.primitiveFailure(
				E_CANNOT_DEFINE_DURING_COMPILATION)
		}
		return interpreter.suspendInLevelOneSafeThen {
			val styler = newStyler(function, definition, module)
			var bad = false
			definition.updateStylers {
				bad = any { it.module.equals(module) }
				if (bad) this else setWithElementCanDestroy(styler, true)
			}
			when
			{
				bad -> fail(E_STYLER_ALREADY_SET_BY_THIS_MODULE)
				else ->
				{
					module.moduleAddStyler(styler)
					succeed(nil)
				}
			}
		}
	}

	override fun privateBlockTypeRestriction(): A_Type =
		functionType(
			tuple(
				DEFINITION.o,
				stylerFunctionType),
			TOP.o)

	override fun privateFailureVariableType(): A_Type =
		enumerationWith(
			set(
				E_LOADING_IS_OVER,
				E_CANNOT_DEFINE_DURING_COMPILATION,
				E_STYLER_ALREADY_SET_BY_THIS_MODULE
			).setUnionCanDestroy(MessageSplitter.possibleErrors, true))
}
