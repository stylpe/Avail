/*
 * AvailServerChannel.kt
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

package com.avail.server.io

import com.avail.io.TextInterface
import com.avail.server.AvailServer
import com.avail.server.messages.Command
import com.avail.server.messages.CommandMessage
import com.avail.server.messages.Message
import com.avail.server.messages.UpgradeCommandMessage
import com.avail.utility.evaluation.Continuation0
import java.util.*
import java.util.concurrent.atomic.AtomicLong

/**
 * An `AvailServerChannel` represents an abstract connection between an
 * [AvailServer] and a client (represented by a [TransportAdapter]). It provides
 * mechanisms for sending and receiving [messages][Message].
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
abstract class AvailServerChannel : AutoCloseable
{
	/** `true` if the channel is open, `false` otherwise. */
	abstract val isOpen: Boolean

	/**
	 * The unique channel [identifier][UUID] used to distinguish this
	 * [AvailServerChannel] from `AvailServerChannel`s.
	 *
	 * Only command channels will keep their UUID assigned upon construction.
	 * All subordinate IO-channels will have their identifiers set to the
	 * [Command.UPGRADE] token received from a client-sent
	 * [UpgradeCommandMessage].
	 */
	var id: UUID = UUID.randomUUID()

	/**
	 * The [id] of the command [channel][AvailServerChannel] that is responsible
	 * for spawning this channel if this channel were created through the
	 * [upgrade][Command.UPGRADE] process.
	 */
	var parentId: UUID? = null

	/**
	 * `true` if this [AvailServerChannel] is an IO channel for a
	 * [parent][parentId] command channel.
	 */
	val isIOChannel get() = state == ProtocolState.IO

	/** The current [protocol state][ProtocolState].  */
	var state = ProtocolState.VERSION_NEGOTIATION
		set (newState)
		{
			assert(this.state.allowedSuccessorStates.contains(newState))
			field = newState
		}

	/**
	 * The [text interface][TextInterface], or `null` if the
	 * [receiver][AvailServerChannel] is not an upgraded I/O channel.
	 */
	var textInterface: TextInterface? = null

	/**
	 * The [UUID]s of any upgrade requests issued by this
	 * [channel][AvailServerChannel].
	 */
	private val requestedUpgrades = HashSet<UUID>()

	/** The next [command][CommandMessage] [identifier][AtomicLong] to issue. */
	private val commandId = AtomicLong(1)

	/**
	 * The [server][AvailServer] that created this
	 * [channel][AbstractTransportChannel].
	 */
	abstract val server: AvailServer

	/**
	 * Enqueue the given [message][Message]. When the message has been enqueued,
	 * then execute the [continuation][Continuation0].
	 *
	 * @param message
	 *   A message.
	 * @param enqueueSucceeded
	 *   What to do when the message has been successfully enqueued.
	 */
	abstract fun enqueueMessageThen(
		message: Message,
		enqueueSucceeded: ()->Unit)

	/**
	 * Receive an incoming [message][Message].
	 *
	 * @param message
	 *   A message.
	 */
	abstract fun receiveMessage(message: Message)

	/**
	 * `ProtocolState` represents the communication state of a [server
	 * channel][AvailServerChannel].
	 *
	 * @author Todd L Smith &lt;todd@availlang.org&gt;
	 */
	enum class ProtocolState
	{
		/** Protocol version must be negotiated.  */
		VERSION_NEGOTIATION
		{
			override val allowedSuccessorStates
				get() = setOf(ELIGIBLE_FOR_UPGRADE)
			override val versionNegotiated get() = false
		},

		/**
		 * The [channel][AvailServerChannel] is eligible for upgrade.
		 */
		ELIGIBLE_FOR_UPGRADE
		{
			override val allowedSuccessorStates: Set<ProtocolState>
				get() = EnumSet.of(COMMAND, IO)
			override val eligibleForUpgrade get() = true
		},

		/**
		 * The [channel][AvailServerChannel] should henceforth be used to issue
		 * [commands][CommandMessage].
		 */
		COMMAND
		{
			override val allowedSuccessorStates: Set<ProtocolState>
				get() = emptySet()
		},

		/**
		 * The [channel][AvailServerChannel] should henceforth be used for
		 * general text I/O.
		 */
		IO
		{
			override val allowedSuccessorStates: Set<ProtocolState>
				get() = emptySet()
			override val generalTextIO get() = true
		};

		/** The allowed successor [states][ProtocolState] of the receiver. */
		internal abstract val allowedSuccessorStates: Set<ProtocolState>

		/**
		 * Does this [state][ProtocolState] indicate that the version has
		 * already been negotiated?
		 *
		 * @return
		 *   `true` if the version has already been negotiated, `false`
		 *   otherwise.
		 */
		open val versionNegotiated get() = true

		/**
		 * Does this [state][ProtocolState] indicate eligibility for upgrade?
		 *
		 * @return
		 *   `true` if the state indicates eligibility for upgrade, `false`
		 *   otherwise.
		 */
		open val eligibleForUpgrade get() = false

		/**
		 * Does this [state][ProtocolState] indicate a capability to do general
		 * text I/O?
		 *
		 * @return
		 *   `true` if the state indicates the capability, `false` otherwise.
		 */
		open val generalTextIO get() = false
	}

	/**
	 * Upgrade the [channel][AvailServerChannel] for general I/O.
	 */
	fun upgradeToIOChannel()
	{
		state = ProtocolState.IO
		textInterface = TextInterface(
			ServerInputChannel(this),
			ServerOutputChannel(this),
			ServerErrorChannel(this))
	}

	/**
	 * Record an upgrade request instigated by this
	 * [channel][AvailServerChannel].
	 *
	 * @param uuid
	 *   The [UUID] that identifies the upgrade request.
	 */
	fun recordUpgradeRequest(uuid: UUID)
	{
		synchronized(requestedUpgrades) {
			requestedUpgrades.add(uuid)
		}
	}

	protected fun finalize()
	{
		try
		{
			server.discontinueUpgradeRequests(requestedUpgrades)
		}
		catch (e: Throwable)
		{
			// Do not prevent destruction of this channel.
		}

	}

	/**
	 * The next [command identifier][CommandMessage] from the
	 * [channel][AvailServerChannel]'s internal sequence.
	 */
	val nextCommandId = commandId.getAndIncrement()

	override fun toString(): String =
		if (isIOChannel)
		{
			"($id) $state: [$parentId] "
		}
		else
		{
			"($id) $state"
		}
}
