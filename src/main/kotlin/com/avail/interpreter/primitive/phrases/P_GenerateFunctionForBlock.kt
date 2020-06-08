/*
 * P_GenerateFunctionForBlock.kt
 * Copyright © 1993-2019, The Avail Foundation, LLC.
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

import com.avail.descriptor.functions.A_RawFunction
import com.avail.descriptor.functions.FunctionDescriptor
import com.avail.descriptor.functions.FunctionDescriptor.Companion.createFunction
import com.avail.descriptor.module.ModuleDescriptor.Companion.currentModule
import com.avail.descriptor.phrases.A_Phrase.Companion.generateInModule
import com.avail.descriptor.phrases.BlockPhraseDescriptor
import com.avail.descriptor.phrases.BlockPhraseDescriptor.Companion.recursivelyValidate
import com.avail.descriptor.sets.SetDescriptor.Companion.set
import com.avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tuple
import com.avail.descriptor.tuples.TupleDescriptor.Companion.emptyTuple
import com.avail.descriptor.types.A_Type
import com.avail.descriptor.types.AbstractEnumerationTypeDescriptor.Companion.enumerationWith
import com.avail.descriptor.types.FunctionTypeDescriptor.Companion.functionType
import com.avail.descriptor.types.FunctionTypeDescriptor.Companion.mostGeneralFunctionType
import com.avail.descriptor.types.PhraseTypeDescriptor.PhraseKind.BLOCK_PHRASE
import com.avail.exceptions.AvailErrorCode.E_BLOCK_COMPILATION_FAILED
import com.avail.exceptions.AvailErrorCode.E_BLOCK_IS_INVALID
import com.avail.exceptions.AvailErrorCode.E_BLOCK_MUST_NOT_CONTAIN_OUTERS
import com.avail.exceptions.AvailRuntimeException
import com.avail.interpreter.Primitive
import com.avail.interpreter.Primitive.Flag.CanFold
import com.avail.interpreter.Primitive.Flag.CanInline
import com.avail.interpreter.execution.Interpreter

/**
 * **Primitive:** Compile the specified [block][BlockPhraseDescriptor] into a
 * [function][FunctionDescriptor]. The block is treated as a top-level
 * construct, so it must not refer to any outer variables.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
object P_GenerateFunctionForBlock : Primitive(1, CanFold, CanInline)
{
	override fun attempt(interpreter: Interpreter): Result
	{
		interpreter.checkArgumentCount(1)
		val block = interpreter.argument(0)
		try
		{
			recursivelyValidate(block)
		}
		catch (e: AvailRuntimeException)
		{
			return interpreter.primitiveFailure(e)
		}
		catch (e: Exception)
		{
			return interpreter.primitiveFailure(E_BLOCK_IS_INVALID)
		}

		val compiledCode: A_RawFunction
		try
		{
			compiledCode = block.generateInModule(currentModule())
		}
		catch (e: Exception)
		{
			return interpreter.primitiveFailure(E_BLOCK_COMPILATION_FAILED)
		}

		val function = createFunction(compiledCode, emptyTuple())
		return interpreter.primitiveSuccess(function.makeImmutable())
	}

	override fun privateBlockTypeRestriction(): A_Type =
		functionType(
			tuple(
				BLOCK_PHRASE.mostGeneralType()),
			mostGeneralFunctionType())

	override fun privateFailureVariableType(): A_Type =
		enumerationWith(set(
			E_BLOCK_IS_INVALID,
			E_BLOCK_MUST_NOT_CONTAIN_OUTERS,
			E_BLOCK_COMPILATION_FAILED))
}
