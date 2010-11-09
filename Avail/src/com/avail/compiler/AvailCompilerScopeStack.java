/**
 * compiler/AvailCompilerScopeStack.java
 * Copyright (c) 2010, Mark van Gulik.
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

package com.avail.compiler;

import com.avail.compiler.AvailCompilerScopeStack;
import com.avail.compiler.AvailVariableDeclarationNode;

public class AvailCompilerScopeStack
{
	String _name;
	AvailVariableDeclarationNode _declaration;
	AvailCompilerScopeStack _next;


	// accessing

	AvailVariableDeclarationNode declaration ()
	{
		return _declaration;
	}

	void declaration (
			final AvailVariableDeclarationNode anAvailVariableDeclarationNode)
	{

		_declaration = anAvailVariableDeclarationNode;
	}

	String name ()
	{
		return _name;
	}

	void name (
			final String aString)
	{

		_name = aString;
	}

	AvailCompilerScopeStack next ()
	{
		return _next;
	}

	void next (
			final AvailCompilerScopeStack anAvailCompilerScopeStack)
	{

		_next = anAvailCompilerScopeStack;
	}





	// Constructor

	AvailCompilerScopeStack (
		String name,
		AvailVariableDeclarationNode declaration,
		AvailCompilerScopeStack next)
	{
		_name = name;
		_declaration = declaration;
		_next = next;
	}

}
