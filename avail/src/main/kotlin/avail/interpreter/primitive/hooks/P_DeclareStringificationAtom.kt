/*
 * P_DeclareStringificationAtom.kt
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

package avail.interpreter.primitive.hooks

import avail.AvailRuntime.HookType.STRINGIFICATION
import avail.descriptor.atoms.A_Atom.Companion.bundleOrCreate
import avail.descriptor.atoms.AtomDescriptor
import avail.descriptor.functions.FunctionDescriptor.Companion.createFunction
import avail.descriptor.methods.MethodDescriptor
import avail.descriptor.representation.NilDescriptor.Companion.nil
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tuple
import avail.descriptor.tuples.TupleDescriptor.Companion.emptyTuple
import avail.descriptor.types.A_Type
import avail.descriptor.types.FunctionTypeDescriptor.Companion.functionType
import avail.descriptor.types.TupleTypeDescriptor.Companion.stringType
import avail.descriptor.types.PrimitiveTypeDescriptor.Types.ANY
import avail.descriptor.types.PrimitiveTypeDescriptor.Types.ATOM
import avail.descriptor.types.PrimitiveTypeDescriptor.Types.TOP
import avail.exceptions.AvailRuntimeException
import avail.exceptions.MalformedMessageException
import avail.interpreter.Primitive
import avail.interpreter.Primitive.Flag.CannotFail
import avail.interpreter.Primitive.Flag.HasSideEffect
import avail.interpreter.Primitive.Flag.Private
import avail.interpreter.Primitive.Flag.WritesToHiddenGlobalState
import avail.interpreter.execution.Interpreter
import avail.interpreter.levelOne.L1InstructionWriter
import avail.interpreter.levelOne.L1Operation

/**
 * **Primitive:** Inform the VM of the [atom][AtomDescriptor] of the preferred
 * stringification [method][MethodDescriptor].
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
@Suppress("unused")
object P_DeclareStringificationAtom : Primitive(
	1, CannotFail, HasSideEffect, Private, WritesToHiddenGlobalState)
{
	override fun attempt(interpreter: Interpreter): Result
	{
		interpreter.checkArgumentCount(1)
		val atom = interpreter.argument(0)
		// Generate a function that will invoke the stringifier method for
		// the specified value.
		val writer = L1InstructionWriter(nil, 0, nil)
		writer.argumentTypes(ANY.o)
		writer.returnType = stringType
		writer.returnTypeIfPrimitiveFails = stringType
		writer.write(0, L1Operation.L1_doPushLocal, 1)
		try
		{
			writer.write(
				0,
				L1Operation.L1_doCall,
				writer.addLiteral(atom.bundleOrCreate()),
				writer.addLiteral(stringType))
		}
		catch (e: MalformedMessageException)
		{
			assert(false) { "This should never happen!" }
			throw AvailRuntimeException(e.errorCode)
		}

		val function = createFunction(writer.compiledCode(), emptyTuple)
		function.makeShared()
		// Set the stringification function.
		STRINGIFICATION[interpreter.runtime] = function
		interpreter.availLoaderOrNull()?.statementCanBeSummarized(false)
		return interpreter.primitiveSuccess(nil)
	}

	override fun privateBlockTypeRestriction(): A_Type =
		functionType(tuple(ATOM.o), TOP.o)
}
