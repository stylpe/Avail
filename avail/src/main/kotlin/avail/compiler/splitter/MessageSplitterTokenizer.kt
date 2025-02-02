/*
 * MessageSplitterTokenizer.kt
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

package avail.compiler.splitter

import avail.compiler.ParsingOperation
import avail.compiler.problems.CompilerDiagnostics.Companion.errorIndicatorSymbol
import avail.compiler.splitter.MessageSplitter.Companion.isUnderscoreOrSpaceOrOperator
import avail.compiler.splitter.MessageSplitter.Companion.throwMalformedMessageException
import avail.compiler.splitter.MessageSplitter.Metacharacter
import avail.compiler.splitter.MessageSplitter.Metacharacter.BACK_QUOTE
import avail.compiler.splitter.MessageSplitter.Metacharacter.SPACE
import avail.compiler.splitter.MessageSplitter.Metacharacter.UNDERSCORE
import avail.descriptor.tuples.A_String
import avail.descriptor.tuples.A_Tuple.Companion.concatenateTuplesCanDestroy
import avail.descriptor.tuples.A_Tuple.Companion.tupleCodePointAt
import avail.descriptor.tuples.A_Tuple.Companion.tupleSize
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tuple
import avail.descriptor.tuples.StringDescriptor
import avail.descriptor.tuples.StringDescriptor.Companion.stringFrom
import avail.exceptions.AvailErrorCode.E_EXPECTED_OPERATOR_AFTER_BACKQUOTE
import avail.exceptions.AvailErrorCode.E_METHOD_NAME_IS_NOT_CANONICAL
import avail.exceptions.MalformedMessageException
import avail.utility.safeWrite
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read

/**
 * `MessageSplitterTokenizer` breaks a message name into a sequence of token
 * strings.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 *
 * @constructor
 *
 * Construct a new `MessageSplitter`, parsing the provided message into token
 * strings and generating [parsing][ParsingOperation] for parsing occurrences of
 * this message.
 *
 * @param messageName
 *   An Avail [string][StringDescriptor] specifying the keywords and arguments
 *   of some message being defined.
 * @throws MalformedMessageException
 *         If the message name is malformed.
 */
class MessageSplitterTokenizer
@Throws(MalformedMessageException::class) constructor(messageName: A_String)
{
	/**
	 * The [A_String] to be parsed into message token strings.
	 */
	val messageName: A_String = messageName.makeShared()

	/**
	 * The number of codepoints in the [messageName].
	 */
	private val messageNameSize: Int = messageName.tupleSize

	/** The current one-based index into the messageName. */
	private var positionInName = 1

	/**
	 * The individual tokens ([strings][StringDescriptor]) constituting the
	 * message.
	 *
	 * @see [canonicalMessageParts]
	 */
	private val messagePartsList = mutableListOf<A_String>()

	/**
	 * A collection of one-based positions in the original string, corresponding
	 * to the [messagePartsList] that have been extracted.
	 */
	private val messagePartPositions = mutableListOf<Int>()

	/**
	 * Access the (read-only) array of one-based positions of tokens in the
	 * original string.
	 */
	fun messagePartPositions(): IntArray = messagePartPositions.toIntArray()

	init
	{
		try
		{
			tokenizeMessage()
		}
		catch (e: MalformedMessageException)
		{
			// Add contextual text and rethrow it.
			throw MalformedMessageException(e.errorCode)
			{
				buildString {
					val annotated = tuple(
						messageName.copyStringFromToCanDestroy(
							1, positionInName, false),
						stringFrom(errorIndicatorSymbol),
						messageName.copyStringFromToCanDestroy(
							positionInName + 1, messageNameSize, false)
					).concatenateTuplesCanDestroy(false)
					append(e.describeProblem())
					append(". See arrow (")
					append(errorIndicatorSymbol)
					append(") in: ")
					append(annotated.toString())
				}
			}
		}
	}

	/**
	 * Answer a variant of the [message&#32;name][messageName] with backquotes
	 * stripped.
	 *
	 * @param range
	 *   The range of codepoint indices to extract.
	 * @return
	 *   The [A_String] without backquotes.
	 */
	private fun stripBackquotes(range: IntRange) = stringFrom(
		buildString {
			for (i in range) {
				val cp = messageName.tupleCodePointAt(i)
				if (cp != '`'.code) appendCodePoint(cp)
			}
		})

	private fun atEnd(): Boolean = positionInName > messageNameSize

	private fun peek(): Int = messageName.tupleCodePointAt(positionInName)

	private fun peek(metacharacter: Metacharacter): Boolean {
		val peeked = !atEnd() && peek() == metacharacter.codepoint
		if (peeked) positionInName++
		return peeked
	}

	private fun peek(vararg metacharacters: Metacharacter): Boolean {
		val save = positionInName
		return if (metacharacters.all(this::peek)) true
		else
		{
			positionInName = save
			false
		}
	}

	private fun previous(n: Int = 1): Int =
		messageName.tupleCodePointAt(positionInName - n)

	private fun backup(n: Int = 1) { positionInName -= n }

	private fun skip(n: Int = 1) { positionInName += n }

	private fun accept(start: Int, end: Int = start) {
		messagePartsList.add(
			messageName.copyStringFromToCanDestroy(start, end, false))
		messagePartPositions.add(start)
	}

	private fun acceptStripped(range: IntRange) {
		messagePartsList.add(stripBackquotes(range))
		messagePartPositions.add(range.first)
	}

	/**
	 * Decompose the message name into its constituent token strings. These can
	 * be subsequently parsed to generate the actual parse instructions. Do not
	 * do any semantic analysis here, not even backquote processing – that would
	 * lead to confusion over whether an operator was supposed to be treated as
	 * a special token like open-guillemet («) rather than like a
	 * backquote-escaped open-guillemet token.
	 *
	 * @throws MalformedMessageException
	 *   If the signature is invalid.
	 */
	@Throws(MalformedMessageException::class)
	private fun tokenizeMessage()
	{
		while (!atEnd())
		{
			val start = positionInName
			when
			{
				peek(SPACE) ->
				{
					if (messagePartsList.size == 0 ||
						isUnderscoreOrSpaceOrOperator(previous(2)))
					{
						// The problem is before the space.
						backup()
						throwMalformedMessageException(
							E_METHOD_NAME_IS_NOT_CANONICAL,
							"Expected alphanumeric character before space")
					}
					if (atEnd() || isUnderscoreOrSpaceOrOperator(peek()))
					{
						if (!peek(BACK_QUOTE, UNDERSCORE))
						{
							// Problem is after the space.
							throwMalformedMessageException(
								E_METHOD_NAME_IS_NOT_CANONICAL,
								"Expected alphanumeric character after space")
						}
						// This is legal; we want to be able to parse
						// expressions like "a _b".
						backup(2)
					}
				}
				peek(BACK_QUOTE) -> when {
					atEnd() -> {
						// Found a trailing backquote at the end of the name.
						throwMalformedMessageException(
							E_EXPECTED_OPERATOR_AFTER_BACKQUOTE,
							"Expected character after backquote")
					}
					peek(UNDERSCORE) -> {
						// Despite what the method comment says, backquote needs
						// to be processed specially when followed by an
						// underscore so that identifiers containing (escaped)
						// underscores can be treated as a single token.
						// Otherwise, they are unparseable.
						var sawRegular = false
						loop@ while (!atEnd()) {
							when {
								!isUnderscoreOrSpaceOrOperator(peek()) -> {
									sawRegular = true
									skip()
								}
								!peek(BACK_QUOTE, UNDERSCORE) -> break@loop
							}
						}
						if (sawRegular) {
							// If we ever saw something other than `_ in the
							// sequence, then produce a single token that
							// includes the underscores (but not the
							// backquotes).
							acceptStripped(start until positionInName)
						}
						else
						{
							// If we never saw a regular character, then produce
							// a token for each backquote and each underscore.
							(start until positionInName).forEach(
								this@MessageSplitterTokenizer::accept)
						}
					}
					else -> {
						// We didn't find an underscore, so we need to deal with
						// the backquote in the usual way.
						accept(positionInName - 1)
						accept(positionInName)
						skip()
					}
				}
				isUnderscoreOrSpaceOrOperator(peek()) ->
				{
					accept(positionInName)
					skip()
				}
				else ->
				{
					loop@ while (!atEnd())
					{
						if (!isUnderscoreOrSpaceOrOperator(peek())) skip()
						else if (!peek(BACK_QUOTE, UNDERSCORE)) break@loop
					}
					acceptStripped(start until positionInName)
				}
			}
		}
	}

	/**
	 * Answer the message parts that this tokenizer has produced, but mapped to
	 * canonical values to minimize the cost of storage and indirections.
	 *
	 * @return
	 *   An immutable [List] of canonical [A_String]s.
	 */
	fun canonicalMessageParts(): Array<A_String>
	{
		// First, hold a shared read lock while checking if all parts are
		// already present in the canonical map.  DO NOT modify the map.
		var allPresent = true
		val firstAttempt = allCanonicalMessagePartsLock.read {
			messagePartsList.map { part ->
				allCanonicalMessageParts.getOrElse(part) {
					allPresent = false
					part
				}
			}.toTypedArray()
		}
		if (allPresent) return firstAttempt
		// At least one part was absent from the canonical map.  Hold the write
		// lock and translate each part, updating the map as needed.
		return allCanonicalMessagePartsLock.safeWrite {
			firstAttempt.map { part ->
				allCanonicalMessageParts.getOrPut(part) { part.makeShared() }
			}
		}.toTypedArray()
	}

	companion object
	{
		/** The lock for accessing [allCanonicalMessageParts]. */
		val allCanonicalMessagePartsLock = ReentrantReadWriteLock()

		/**
		 * A map from [A_String] to *canonical* [A_String] for each message part
		 * in every method name.  All [MessageSplitter]s hold only canonical
		 * [A_String]s to minimize footprint (and presumably improve
		 * performance).
		 *
		 * Must be accessed within the [allCanonicalMessagePartsLock].
		 */
		val allCanonicalMessageParts = mutableMapOf<A_String, A_String>()
	}
}
