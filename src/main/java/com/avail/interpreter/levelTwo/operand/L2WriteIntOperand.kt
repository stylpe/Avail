/*
 * L2WriteIntOperand.java
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
package com.avail.interpreter.levelTwo.operand

import com.avail.interpreter.levelTwo.L2OperandDispatcher
import com.avail.interpreter.levelTwo.L2OperandType
import com.avail.interpreter.levelTwo.register.L2IntRegister
import com.avail.interpreter.levelTwo.register.L2Register.RegisterKind
import com.avail.optimizer.values.L2SemanticValue

/**
 * An `L2WriteIntOperand` is an operand of type [L2OperandType.WRITE_INT].  It
 * holds the actual [L2IntRegister] that is to be accessed.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 *
 * @constructor
 * Construct a new `L2WriteIntOperand` for the specified [L2SemanticValue].
 *
 * @param semanticValues
 *   The [Set] of [L2SemanticValue] that this operand is effectively producing.
 * @param restriction
 *   The [TypeRestriction] that indicates what values are allowed to be written
 *   into the register.
 * @param register
 *   The initial [L2IntRegister] that backs this operand.
 */
class L2WriteIntOperand constructor(
		semanticValues: Set<L2SemanticValue>,
		restriction: TypeRestriction,
		register: L2IntRegister)
	: L2WriteOperand<L2IntRegister>(semanticValues, restriction, register)
{
	override fun operandType(): L2OperandType = L2OperandType.WRITE_INT

	override fun registerKind(): RegisterKind = RegisterKind.INTEGER


	override fun dispatchOperand(dispatcher: L2OperandDispatcher)
	{
		dispatcher.doOperand(this)
	}

	init
	{
		assert(restriction.isUnboxedInt)
	}
}