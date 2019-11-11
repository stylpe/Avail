/*
 * P_SimpleMethodDeclaration.kt
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
package com.avail.interpreter.primitive.methods

import com.avail.AvailRuntime.currentRuntime
import com.avail.AvailTask
import com.avail.compiler.splitter.MessageSplitter.Companion.possibleErrors
import com.avail.descriptor.A_Type
import com.avail.descriptor.AbstractEnumerationTypeDescriptor.enumerationWith
import com.avail.descriptor.AtomDescriptor
import com.avail.descriptor.FunctionDescriptor
import com.avail.descriptor.FunctionTypeDescriptor.functionType
import com.avail.descriptor.FunctionTypeDescriptor.mostGeneralFunctionType
import com.avail.descriptor.ModuleDescriptor
import com.avail.descriptor.NilDescriptor.nil
import com.avail.descriptor.ObjectTupleDescriptor.tuple
import com.avail.descriptor.SetDescriptor.set
import com.avail.descriptor.StringDescriptor.stringFrom
import com.avail.descriptor.TupleTypeDescriptor.stringType
import com.avail.descriptor.TypeDescriptor.Types.TOP
import com.avail.exceptions.AmbiguousNameException
import com.avail.exceptions.AvailErrorCode.*
import com.avail.exceptions.MalformedMessageException
import com.avail.exceptions.SignatureException
import com.avail.interpreter.Interpreter
import com.avail.interpreter.Primitive
import com.avail.interpreter.Primitive.Flag.*
import com.avail.interpreter.Primitive.Result.FIBER_SUSPENDED
import com.avail.utility.Nulls.stripNull
import java.util.*

/**
 * **Primitive:** Add a method definition, given a string for which to look up
 * the corresponding [atom][AtomDescriptor] in the current
 * [module][ModuleDescriptor] and the [function][FunctionDescriptor] which will
 * act as the body of the method definition.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
object P_SimpleMethodDeclaration : Primitive(2, Bootstrap, CanSuspend, Unknown)
{
	override fun attempt(interpreter: Interpreter): Result
	{
		interpreter.checkArgumentCount(2)
		val string = interpreter.argument(0)
		val function = interpreter.argument(1)
		val fiber = interpreter.fiber()
		val loader = fiber.availLoader()
		if (loader === null || loader.module().equalsNil())
		{
			return interpreter.primitiveFailure(E_LOADING_IS_OVER)
		}
		if (!loader.phase().isExecuting)
		{
			return interpreter.primitiveFailure(
				E_CANNOT_DEFINE_DURING_COMPILATION)
		}
		val primitiveFunction = stripNull(interpreter.function)
		assert(primitiveFunction.code().primitive() === this)
		val copiedArgs = ArrayList(interpreter.argsBuffer)
		interpreter.primitiveSuspend(primitiveFunction)
		interpreter.runtime().whenLevelOneSafeDo(
			fiber.priority(),
			AvailTask.forUnboundFiber(fiber)
			{
				try
				{
					val atom = loader.lookupName(string)
					loader.addMethodBody(atom, function)
					// Quote the string to make the method name.
					function.code().setMethodName(
						stringFrom(string.toString()))
					Interpreter.resumeFromSuccessfulPrimitive(
						currentRuntime(), fiber, this, nil)
				}
				catch (e: MalformedMessageException)
				{
					Interpreter.resumeFromFailedPrimitive(
						currentRuntime(),
						fiber,
						e.numericCode(),
						primitiveFunction,
						copiedArgs)
				}
				catch (e: SignatureException)
				{
					Interpreter.resumeFromFailedPrimitive(
						currentRuntime(),
						fiber,
						e.numericCode(),
						primitiveFunction,
						copiedArgs)
				}
				catch (e: AmbiguousNameException)
				{
					Interpreter.resumeFromFailedPrimitive(
						currentRuntime(),
						fiber,
						e.numericCode(),
						primitiveFunction,
						copiedArgs)
				}
			})
		return FIBER_SUSPENDED
	}

	override fun privateBlockTypeRestriction(): A_Type =
		functionType(tuple(stringType(), mostGeneralFunctionType()), TOP.o())

	override fun privateFailureVariableType(): A_Type =
		enumerationWith(set(
				E_LOADING_IS_OVER,
				E_CANNOT_DEFINE_DURING_COMPILATION,
				E_AMBIGUOUS_NAME,
				E_METHOD_RETURN_TYPE_NOT_AS_FORWARD_DECLARED,
				E_REDEFINED_WITH_SAME_ARGUMENT_TYPES,
				E_RESULT_TYPE_SHOULD_COVARY_WITH_ARGUMENTS,
				E_METHOD_IS_SEALED)
            .setUnionCanDestroy(possibleErrors, true))
}
