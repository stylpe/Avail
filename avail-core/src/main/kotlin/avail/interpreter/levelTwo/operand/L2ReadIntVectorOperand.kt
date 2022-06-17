/*
 * L2ReadIntVectorOperand.kt
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
package avail.interpreter.levelTwo.operand

import avail.interpreter.levelTwo.L2OperandDispatcher
import avail.interpreter.levelTwo.L2OperandType
import avail.interpreter.levelTwo.register.L2IntRegister
import avail.utility.cast

/**
 * An `L2ReadIntVectorOperand` is an operand of type
 * [L2OperandType.READ_INT_VECTOR]. It holds a [List] of [L2ReadIntOperand]s.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 *
 * @constructor
 * Construct a new `L2ReadIntVectorOperand` with the specified [List] of
 * [L2ReadIntOperand]s.
 *
 * @param elements
 * The list of [L2ReadIntOperand]s.
 */
class L2ReadIntVectorOperand constructor(elements: List<L2ReadIntOperand>)
	: L2ReadVectorOperand<L2IntRegister, L2ReadIntOperand>(elements)
{
	override fun clone(): L2ReadIntVectorOperand =
		L2ReadIntVectorOperand(
			// Requires explicit parameter typing
			elements.map<L2ReadIntOperand, L2ReadIntOperand>{
				it.clone().cast()
			})

	override fun clone(replacementElements: List<L2ReadIntOperand>) =
		L2ReadIntVectorOperand(replacementElements)

	override val operandType: L2OperandType
		get() = L2OperandType.READ_INT_VECTOR

	override fun dispatchOperand(dispatcher: L2OperandDispatcher)
	{
		dispatcher.doOperand(this)
	}
}
