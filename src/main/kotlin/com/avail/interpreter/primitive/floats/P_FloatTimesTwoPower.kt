/*
 * P_FloatTimesTwoPower.java
 * Copyright © 1993-2018, The Avail Foundation, LLC.
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
package com.avail.interpreter.primitive.floats

import com.avail.descriptor.A_Type
import com.avail.descriptor.FloatDescriptor
import com.avail.descriptor.FloatDescriptor.fromFloatRecycling
import com.avail.descriptor.FunctionTypeDescriptor.functionType
import com.avail.descriptor.InstanceTypeDescriptor.instanceType
import com.avail.descriptor.IntegerDescriptor.two
import com.avail.descriptor.IntegerDescriptor.zero
import com.avail.descriptor.IntegerRangeTypeDescriptor.integers
import com.avail.descriptor.ObjectTupleDescriptor.tuple
import com.avail.descriptor.TypeDescriptor.Types.FLOAT
import com.avail.interpreter.Interpreter
import com.avail.interpreter.Primitive
import com.avail.interpreter.Primitive.Flag.*
import java.lang.Math.*

/**
 * **Primitive:** Compute [ float][FloatDescriptor] `a*(2**b)` without intermediate overflow or any precision
 * loss.
 */
object P_FloatTimesTwoPower : Primitive(3, CannotFail, CanFold, CanInline) {

	override fun attempt(
		interpreter: Interpreter): Primitive.Result {
		interpreter.checkArgumentCount(3)
		val a = interpreter.argument(0)
		//		final A_Token literalTwo = interpreter.argument(1);
		val b = interpreter.argument(2)

		val scale = if (b.isInt)
			min(max(b.extractInt(), -10000), 10000)
		else
			if (b.greaterOrEqual(zero())) 10000 else -10000
		val f = scalb(a.extractFloat(), scale)
		return interpreter.primitiveSuccess(fromFloatRecycling(f, a, true))
	}

	override fun privateBlockTypeRestriction(): A_Type {
		return functionType(
			tuple(
				FLOAT.o(),
				instanceType(two()),
				integers()),
			FLOAT.o())
	}
}
