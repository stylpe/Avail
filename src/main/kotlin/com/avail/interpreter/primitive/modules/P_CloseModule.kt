/*
 * P_CloseModule.kt
 * Copyright © 1993-2020, The Avail Foundation, LLC.
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

package com.avail.interpreter.primitive.modules

import com.avail.descriptor.module.A_Module
import com.avail.descriptor.module.A_Module.Companion.moduleState
import com.avail.descriptor.module.A_Module.Companion.setModuleState
import com.avail.descriptor.module.ModuleDescriptor.State.Loaded
import com.avail.descriptor.module.ModuleDescriptor.State.Loading
import com.avail.descriptor.sets.SetDescriptor.Companion.set
import com.avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tuple
import com.avail.descriptor.types.AbstractEnumerationTypeDescriptor.Companion.enumerationWith
import com.avail.descriptor.types.FunctionTypeDescriptor.Companion.functionType
import com.avail.descriptor.types.TypeDescriptor.Types.MODULE
import com.avail.descriptor.types.TypeDescriptor.Types.TOP
import com.avail.exceptions.AvailErrorCode.E_MODULE_IS_CLOSED
import com.avail.interpreter.Primitive
import com.avail.interpreter.Primitive.Flag.CanInline
import com.avail.interpreter.execution.Interpreter

/**
 * **Primitive**: Close the specified anonymous [module][A_Module],
 * thereby preventing the addition of any new statements.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
object P_CloseModule : Primitive(1, CanInline)
{
	override fun attempt (interpreter: Interpreter): Result
	{
		interpreter.checkArgumentCount(1)
		val module: A_Module = interpreter.argument(0)

		if (module.moduleState() != Loading)
		{
			// TODO Should rename error code.
			return interpreter.primitiveFailure(E_MODULE_IS_CLOSED)
		}
		module.setModuleState(Loaded)
		return interpreter.primitiveSuccess(TOP.o)
	}

	override fun privateBlockTypeRestriction () =
		functionType(tuple(MODULE.o), TOP.o)

	override fun privateFailureVariableType() =
		enumerationWith(set(E_MODULE_IS_CLOSED))
}
