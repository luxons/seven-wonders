package org.luxons.sevenwonders.server

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.produceIn
import org.junit.runner.RunWith
import org.luxons.sevenwonders.client.SevenWondersClient
import org.luxons.sevenwonders.client.SevenWondersSession
import org.luxons.sevenwonders.client.joinGameAndWaitLobby
import org.luxons.sevenwonders.model.Action
import org.luxons.sevenwonders.model.api.GameListEvent
import org.luxons.sevenwonders.model.api.LobbyDTO
import org.luxons.sevenwonders.server.test.runAsyncTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.junit4.SpringRunner
import kotlin.test.*

@OptIn(FlowPreview::class)
@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SevenWondersTest {

    @LocalServerPort
    private val randomServerPort: Int = 0

    private suspend fun connectNewClient(): SevenWondersSession {
        val client = SevenWondersClient()
        val serverUrl = "ws://localhost:$randomServerPort"
        return client.connect(serverUrl)
    }

    private suspend fun disconnect(vararg sessions: SevenWondersSession) {
        for (session in sessions) {
            session.disconnect()
        }
    }

    @Test
    fun chooseName_succeedsWithCorrectDisplayName() = runAsyncTest {
        val session = connectNewClient()
        val playerName = "Test User"
        val player = session.chooseName(playerName)
        assertNotNull(player)
        assertEquals(playerName, player.displayName)
        session.disconnect()
    }

    private suspend fun newPlayer(name: String): SevenWondersSession = connectNewClient().apply {
        chooseName(name)
    }

    @Test
    fun lobbySubscription_ignoredForOutsiders() = runAsyncTest {
        val ownerSession = newPlayer("GameOwner")
        val session1 = newPlayer("Player1")
        val session2 = newPlayer("Player2")
        val gameName = "Test Game"

        val lobby = ownerSession.createGameAndWaitLobby(gameName)

        session1.joinGameAndWaitLobby(lobby.id)
        session2.joinGameAndWaitLobby(lobby.id)

        val outsiderSession = newPlayer("Outsider")
        val started = launch { outsiderSession.awaitGameStart(lobby.id) }

        ownerSession.startGame()
        val nothing = withTimeoutOrNull(50) { started.join() }
        assertNull(nothing)
        started.cancel()
        disconnect(ownerSession, session1, session2, outsiderSession)
    }

    @Test
    fun createGame_success() = runAsyncTest {
        val ownerSession = newPlayer("GameOwner")

        val gameName = "Test Game"
        val lobby = ownerSession.createGameAndWaitLobby(gameName)
        assertNotNull(lobby)
        assertEquals(gameName, lobby.name)

        disconnect(ownerSession)
    }

    @Test
    fun createGame_seenByConnectedPlayers() = runAsyncTest {
        val otherSession = newPlayer("OtherPlayer")
        val games = otherSession.watchGames().produceIn(this)

        val initialListEvent = withTimeout(500) { games.receive() }
        assertTrue(initialListEvent is GameListEvent.ReplaceList)
        assertEquals(0, initialListEvent.lobbies.size)

        val ownerSession = newPlayer("GameOwner")
        val gameName = "Test Game"
        val createdLobby = ownerSession.createGameAndWaitLobby(gameName)

        val afterGameListEvent = withTimeout(500) { games.receive() }
        assertTrue(afterGameListEvent is GameListEvent.CreateOrUpdate)
        val receivedLobby = afterGameListEvent.lobby
        assertEquals(createdLobby.id, receivedLobby.id)
        assertEquals(createdLobby.name, receivedLobby.name)

        disconnect(ownerSession, otherSession)
    }

    @Test
    fun startGame_3players() = runAsyncTest(30000) {
        val session1 = newPlayer("Player1")
        val session2 = newPlayer("Player2")

        val lobby = session1.createGameAndWaitLobby("Test Game")
        session2.joinGameAndWaitLobby(lobby.id)

        val session3 = newPlayer("Player3")
        session3.joinGameAndWaitLobby(lobby.id)

        listOf(session1, session2, session3).forEachIndexed { i, session ->
            launch {
                println("startGame_3players [launch ${i + 1}] awaiting game start...")
                val firstTurn = session.awaitGameStart(lobby.id)
                assertEquals(Action.SAY_READY, firstTurn.action)
                val turns = session.watchTurns().produceIn(this)
                println("startGame_3players [launch ${i + 1}] saying ready...")
                session.sayReady()
                println("startGame_3players [launch ${i + 1}] ready, receiving first turn...")
                val turn = turns.receive()
                assertNotNull(turn)
                println("startGame_3players [launch ${i + 1}] turn OK, disconnecting...")
                session.disconnect()
            }
        }
        println("startGame_3players: player 1 starting the game...")
        delay(50) // ensure awaitGameStart actually subscribed in all sessions
        session1.startGame()
        println("startGame_3players: end of test method (main body)")
    }
}

private suspend fun SevenWondersSession.createGameAndWaitLobby(gameName: String): LobbyDTO {
    val joinedLobbies = watchLobbyJoined()
    createGame(gameName)
    val lobby = joinedLobbies.first()
    updateSettings(lobby.settings.copy(askForReadiness = true))
    return lobby
}
