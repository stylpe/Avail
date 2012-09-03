/**
 * MessageSplitterTest.java
 * Copyright © 1993-2012, Mark van Gulik and Todd L Smith.
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

package com.avail.test;

import java.util.*;
import static junit.framework.Assert.*;
import org.junit.*;
import com.avail.compiler.*;
import com.avail.descriptor.*;
import com.avail.exceptions.SignatureException;

/**
 * Test the {@link MessageSplitter}.  It splits method names into a sequence of
 * tokens to expect and underscores, but it also implements the repeated
 * argument mechanism by producing a sequence of mini-instructions to say how
 * to do the parsing.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
public class MessageSplitterTest
{
	/**
	 * Test fixture: clear and then create all special objects well-known to the
	 * Avail runtime.
	 */
	@Before
	public void initializeAllWellKnownObjects ()
	{
		AvailObject.clearAllWellKnownObjects();
		AvailObject.createAllWellKnownObjects();
	}

	/**
	 * Test fixture: clear and then create all special objects well-known to the
	 * Avail runtime.
	 */
	@After
	public void clearAllWellKnownObjects ()
	{
		AvailObject.clearAllWellKnownObjects();
	}

	/** Test cases. */
	private static String[][] splitCases =
	{
		{"Foo", "Foo", "[10]"},
		{"Print_", "Print", "_", "[10, 0, 11]"},
		{"_+_", "_", "+", "_", "[0, 11, 18, 0, 19]"},
		{"_+_*_", "_", "+", "_", "*", "_", "[0, 11, 18, 0, 19, 34, 0, 27]"},
		{"_;", "_", ";", "[0, 11, 18]"},
		{"__", "_", "_", "[0, 11, 0, 19]"},
		/* Backquotes. */
		{"`__", "`", "_", "_", "[18, 0, 11]"},
		{"_`«_", "_", "`", "«", "_", "[0, 11, 26, 0, 19]"},
		{"_``_", "_", "`", "`", "_", "[0, 11, 26, 0, 19]"},
		{"`#`?`~", "`", "#", "`", "?", "`", "~", "[18, 34, 50]"},
		{"`|`|_`|`|", "`", "|", "`", "|", "_", "`", "|", "`", "|",
			"[18, 34, 0, 11, 58, 74]"},
		/* Repeated groups. */
		{"«_;»", "«", "_", ";", "»",
			"[3, 1, 96, 0, 11, 26, 2, 88, 5, 33, 5, 4]"},
		{"«x»", "«", "x", "»",
			"[3, 1, 96, 1, 18, 80, 2, 5, 33, 2, 5, 4]"},
		{"«x y»", "«", "x", "y", "»",
			"[3, 1, 104, 1, 18, 26, 88, 2, 5, 33, 2, 5, 4]"},
		{"«x_y»", "«", "x", "_", "y", "»",
			"[3, 1, 104, 18, 0, 11, 34, 2, 96, 5, 33, 5, 4]"},
		{"«_:_»", "«", "_", ":", "_", "»",
			"[3, 1, 144, 1, 0, 11, 2, 26, 0, 19, 2, 128, 2, 5, 33, 2, 5, 4]"},
		{"«»", "«", "»",
			"[3, 1, 88, 1, 72, 2, 5, 33, 2, 5, 4]"},
		{"«»«»", "«", "»", "«", "»",
			"[3, 1, 88, 1, 72, 2, 5, 33, 2, 5, 4, 3, 1, 176, 1, 160, 2, 5, 121, 2, 5, 4]"},
		{"«_»", "«", "_", "»",
			"[3, 1, 88, 0, 11, 2, 80, 5, 33, 5, 4]"},
		/* Repeated groups with double dagger. */
		{"«_‡,»", "«", "_", "‡", ",", "»",
			"[3, 1, 96, 0, 11, 2, 88, 34, 5, 33, 5, 4]"},
		{"«‡»", "«", "‡", "»",
			"[3, 1, 88, 1, 72, 2, 5, 33, 2, 5, 4]"},
		{"new_«with«_=_‡,»»",
			"new", "_", "«", "with", "«", "_", "=", "_", "‡", ",", "»", "»",
			"[10, 0, 11, 3, 1, 256, 34, 3, 1, 208, 1, 0, 19, 2, 58, 0, 27, 2, 192, 82, 2, 5, 89, 2, 5, 4, 2, 248, 5, 57, 5, 4]"},
		/* Counting groups. */
		{"«x»#", "«", "x", "»", "#",
			"[3, 1, 88, 1, 18, 2, 80, 5, 33, 5, 4, 12]"},
		{"«x y»#", "«", "x", "y", "»", "#",
			"[3, 1, 96, 1, 18, 26, 2, 88, 5, 33, 5, 4, 12]"},
		{"«»#", "«", "»", "#",
			"[3, 1, 80, 1, 2, 72, 5, 33, 5, 4, 12]"},
		{"«»#«»#", "«", "»", "#", "«", "»", "#",
			"[3, 1, 80, 1, 2, 72, 5, 33, 5, 4, 12, 3, 1, 168, 1, 2, 160, 5, 121, 5, 4, 12]"},
		/* Counting groups with double dagger. */
		{"«‡»#", "«", "‡", "»", "#",
			"[3, 1, 80, 1, 2, 72, 5, 33, 5, 4, 12]"},
		{"«fish‡»#", "«", "fish", "‡", "»", "#",
			"[3, 1, 88, 1, 18, 2, 80, 5, 33, 5, 4, 12]"},
		{"«‡face»#", "«", "‡", "face", "»", "#",
			"[3, 1, 88, 1, 2, 80, 26, 5, 33, 5, 4, 12]"},
		{"«fish‡face»#", "«", "fish", "‡", "face", "»", "#",
			"[3, 1, 96, 1, 18, 2, 88, 34, 5, 33, 5, 4, 12]"},
		{"««fish‡face»#»", "«", "«", "fish", "‡", "face", "»", "#", "»",
			"[3, 1, 176, 3, 1, 120, 1, 26, 2, 112, 42, 5, 57, 5, 4, 12, 2, 168, 5, 33, 5, 4]"},
		/* Optional groups. */
		{"«x»?", "«", "x", "»", "?",
			"[3, 1, 64, 1, 18, 2, 5, 4, 20]"},
		{"«x y»?", "«", "x", "y", "»", "?",
			"[3, 1, 72, 1, 18, 26, 2, 5, 4, 20]"},
		{"«»?", "«", "»", "?",
			"[3, 1, 56, 1, 2, 5, 4, 20]"},
		{"««bagel»#«friend»?»", "«", "«", "bagel", "»", "#", "«", "friend", "»", "?", "»",
			"[3, 1, 272, 1, 3, 1, 120, 1, 26, 2, 112, 5, 65, 5, 4, 12, 2, 3, 1, 200, 1, 58, 2, 5, 4, 20, 2, 256, 2, 5, 33, 2, 5, 4]"},
		/* Completely optional groups. */
		{"very⁇good", "very", "⁇", "good",
			"[3, 1, 48, 10, 5, 4, 7, 26]"},
		{"«very extremely»⁇good", "«", "very", "extremely", "»", "⁇", "good",
			"[3, 1, 56, 18, 26, 5, 4, 7, 50]"},
		/* Case insensitive. */
		{"fnord~", "fnord", "~", "[13]"},
		{"the~_", "the", "~", "_", "[13, 0, 11]"},
		{"«x~»", "«", "x", "~", "»",
			"[3, 1, 96, 1, 21, 80, 2, 5, 33, 2, 5, 4]"},
		{"«x»~", "«", "x", "»", "~",
			"[3, 1, 96, 1, 21, 80, 2, 5, 33, 2, 5, 4]"},
		{"«x~y»", "«", "x", "~", "y", "»",
			"[3, 1, 104, 1, 21, 34, 88, 2, 5, 33, 2, 5, 4]"},
		{"«x y»~", "«", "x", "y", "»", "~",
			"[3, 1, 104, 1, 21, 29, 88, 2, 5, 33, 2, 5, 4]"},
		{"«x y»#~", "«", "x", "y", "»", "#", "~",
			"[3, 1, 96, 1, 21, 29, 2, 88, 5, 33, 5, 4, 12]"},
		{"«x y»?~", "«", "x", "y", "»", "?", "~",
			"[3, 1, 72, 1, 21, 29, 2, 5, 4, 20]"},
		/* Alternation. */
		{"hello|greetings", "hello", "|", "greetings",
			"[3, 1, 48, 10, 57, 26, 5, 4, 7]"},
		{"a|b|c|d|e|f|g",
			"a", "|", "b", "|", "c", "|", "d", "|", "e", "|", "f", "|", "g",
			"[3, 1, 48, 10, 177, 72, 26, 177, 96, 42, 177, 120, 58, 177, 144, 74, 177, 168, 90, 177, 106, 5, 4, 7]"},
		{"«fruit bats»|sloths|carp|«breakfast cereals»",
			"«", "fruit", "bats", "»", "|", "sloths", "|", "carp", "|", "«", "breakfast", "cereals", "»",
			"[3, 1, 144, 3, 1, 128, 1, 18, 26, 112, 2, 5, 57, 2, 5, 4, 297, 168, 50, 297, 192, 66, 297, 3, 1, 288, 1, 90, 98, 272, 2, 5, 217, 2, 5, 4, 5, 4, 7]"}
	};

	/**
	 * Test: Split the test cases.
	 *
	 * @throws SignatureException If the message name is malformed.
	 */
	@Test
	public void testSplitting () throws SignatureException
	{
		for (final String[] splitCase : Arrays.asList(splitCases))
		{
			final String msgString = splitCase[0];
			final AvailObject message = StringDescriptor.from(msgString);
			final MessageSplitter splitter = new MessageSplitter(message);
			final AvailObject parts = splitter.messageParts();
			assert splitCase.length == parts.tupleSize() + 2;
			for (int i = 1; i <= parts.tupleSize(); i++)
			{
				assertEquals(
					"Split was not as expected: " + msgString,
					splitCase[i],
					parts.tupleAt(i).asNativeString());
			}
			final AvailObject instructionsTuple = splitter.instructionsTuple();
			final List<Integer> instructionsList = new ArrayList<Integer>();
			for (final AvailObject instruction : instructionsTuple)
			{
				instructionsList.add(instruction.extractInt());
			}
			assertEquals(
				"Generated parse code was not as expected: " + msgString,
				splitCase[splitCase.length - 1],
				instructionsList.toString());
		}
	}
}
