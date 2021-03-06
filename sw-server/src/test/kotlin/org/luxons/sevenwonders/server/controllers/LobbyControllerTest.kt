package org.luxons.sevenwonders.server.controllers

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.Before
import org.junit.Test
import org.luxons.sevenwonders.model.Settings
import org.luxons.sevenwonders.model.api.State
import org.luxons.sevenwonders.model.api.actions.ReorderPlayersAction
import org.luxons.sevenwonders.model.api.actions.UpdateSettingsAction
import org.luxons.sevenwonders.server.lobby.Lobby
import org.luxons.sevenwonders.server.lobby.Player
import org.luxons.sevenwonders.server.lobby.PlayerIsNotOwnerException
import org.luxons.sevenwonders.server.lobby.PlayerNotInLobbyException
import org.luxons.sevenwonders.server.repositories.LobbyRepository
import org.luxons.sevenwonders.server.repositories.PlayerNotFoundException
import org.luxons.sevenwonders.server.repositories.PlayerRepository
import org.luxons.sevenwonders.server.test.MockMessageChannel
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.util.*
import kotlin.test.*

class LobbyControllerTest {

    private lateinit var playerRepository: PlayerRepository

    private lateinit var lobbyRepository: LobbyRepository

    private lateinit var lobbyController: LobbyController

    @Before
    fun setUp() {
        val meterRegistry = SimpleMeterRegistry()
        val template = SimpMessagingTemplate(MockMessageChannel())
        playerRepository = PlayerRepository(meterRegistry)
        lobbyRepository = LobbyRepository(meterRegistry)
        lobbyController = LobbyController(template, lobbyRepository, playerRepository, "UNUSED", meterRegistry)
    }

    @Test
    fun init_succeeds() {
        val owner = playerRepository.createOrUpdate("testuser", "Test User")
        val lobby = lobbyRepository.create("Test Game", owner)

        assertTrue(lobby.getPlayers().contains(owner))
        assertSame(lobby, owner.lobby)
        assertEquals(owner, lobby.owner)
        assertTrue(owner.isInLobby)
        assertFalse(owner.isInGame)
    }

    @Test
    fun leave_failsWhenPlayerDoesNotExist() {
        val principal = TestPrincipal("I don't exist")

        assertFailsWith<PlayerNotFoundException> {
            lobbyController.leave(principal)
        }
    }

    @Test
    fun leave_failsWhenNotInLobby() {
        playerRepository.createOrUpdate("testuser", "Test User")
        val principal = TestPrincipal("testuser")

        assertFailsWith<PlayerNotInLobbyException> {
            lobbyController.leave(principal)
        }
    }

    @Test
    fun leave_ownerAloneInLobby_succeedsAndRemovesLobby() {
        val player = playerRepository.createOrUpdate("testuser", "Test User")
        val lobby = lobbyRepository.create("Test Game", player)

        val principal = TestPrincipal("testuser")
        lobbyController.leave(principal)

        assertFalse(lobbyRepository.list().contains(lobby))
        assertFalse(player.isInLobby)
        assertFalse(player.isInGame)
    }

    @Test
    fun leave_ownerInLobbyWithOthers_succeedsAndTransfersOwnership() {
        val player1 = playerRepository.createOrUpdate("testuser", "Test User")
        val lobby = lobbyRepository.create("Test Game", player1)
        val player2 = addPlayer(lobby, "testuser2")

        val principal = TestPrincipal("testuser")
        lobbyController.leave(principal)

        assertTrue(lobbyRepository.list().contains(lobby))
        assertFalse(lobby.getPlayers().contains(player1))
        assertEquals(lobby.owner, player2)

        assertTrue(player2.isGameOwner)
        assertTrue(player2.isInLobby)
        assertFalse(player2.isInGame)

        assertFalse(player1.isGameOwner)
        assertFalse(player1.isInLobby)
        assertFalse(player1.isInGame)
    }

    @Test
    fun leave_succeedsWhenInALobby_asJoiner() {
        val player = playerRepository.createOrUpdate("testuser", "Test User")
        val lobby = lobbyRepository.create("Test Game", player)
        val player2 = addPlayer(lobby, "testuser2")

        val principal = TestPrincipal("testuser2")
        lobbyController.leave(principal)

        assertFalse(lobby.getPlayers().contains(player2))
        assertFalse(player2.isInLobby)
        assertFalse(player2.isInGame)
    }

    @Test
    fun reorderPlayers_succeedsForOwner() {
        val player = playerRepository.createOrUpdate("testuser", "Test User")
        val lobby = lobbyRepository.create("Test Game", player)

        val player2 = addPlayer(lobby, "testuser2")
        val player3 = addPlayer(lobby, "testuser3")
        val player4 = addPlayer(lobby, "testuser4")

        val players = listOf(player, player2, player3, player4)
        assertEquals(players, lobby.getPlayers())

        val reorderedPlayers = listOf(player3, player, player2, player4)
        val playerNames = reorderedPlayers.map { it.username }
        val reorderPlayersAction = ReorderPlayersAction(playerNames)

        val principal = TestPrincipal("testuser")
        lobbyController.reorderPlayers(reorderPlayersAction, principal)

        assertEquals(reorderedPlayers, lobby.getPlayers())
    }

    @Test
    fun reorderPlayers_failsForJoiner() {
        val player = playerRepository.createOrUpdate("testuser", "Test User")
        val lobby = lobbyRepository.create("Test Game", player)

        val player2 = addPlayer(lobby, "testuser2")
        val player3 = addPlayer(lobby, "testuser3")

        val reorderedPlayers = listOf(player3, player, player2)
        val playerNames = reorderedPlayers.map { it.username }
        val reorderPlayersAction = ReorderPlayersAction(playerNames)

        val principal = TestPrincipal("testuser2")

        assertFailsWith<PlayerIsNotOwnerException> {
            lobbyController.reorderPlayers(reorderPlayersAction, principal)
        }
    }

    @Test
    fun updateSettings_succeedsForOwner() {
        val player = playerRepository.createOrUpdate("testuser", "Test User")
        val lobby = lobbyRepository.create("Test Game", player)

        addPlayer(lobby, "testuser2")
        addPlayer(lobby, "testuser3")
        addPlayer(lobby, "testuser4")

        assertEquals(Settings(), lobby.settings)

        val newSettings = Settings(12L, 5, false, 5, 5, 4, 10, 2, HashMap())
        val updateSettingsAction = UpdateSettingsAction(newSettings)

        val principal = TestPrincipal("testuser")
        lobbyController.updateSettings(updateSettingsAction, principal)

        assertEquals(newSettings, lobby.settings)
    }

    @Test
    fun updateSettings_failsForJoiner() {
        val player = playerRepository.createOrUpdate("testuser", "Test User")
        val lobby = lobbyRepository.create("Test Game", player)

        addPlayer(lobby, "testuser2")
        addPlayer(lobby, "testuser3")

        val updateSettingsAction = UpdateSettingsAction(Settings())

        val principal = TestPrincipal("testuser2")

        assertFailsWith<PlayerIsNotOwnerException> {
            lobbyController.updateSettings(updateSettingsAction, principal)
        }
    }

    @Test
    fun startGame_succeedsForOwner() {
        val player = playerRepository.createOrUpdate("testuser", "Test User")
        val lobby = lobbyRepository.create("Test Game", player)

        addPlayer(lobby, "testuser2")
        addPlayer(lobby, "testuser3")
        addPlayer(lobby, "testuser4")

        val principal = TestPrincipal("testuser")
        lobbyController.startGame(principal)

        assertSame(State.PLAYING, lobby.state)
    }

    @Test
    fun startGame_failsForJoiner() {
        val player = playerRepository.createOrUpdate("testuser", "Test User")
        val lobby = lobbyRepository.create("Test Game", player)

        addPlayer(lobby, "testuser2")
        addPlayer(lobby, "testuser3")

        val principal = TestPrincipal("testuser2")

        assertFailsWith<PlayerIsNotOwnerException> {
            lobbyController.startGame(principal)
        }
    }

    private fun addPlayer(lobby: Lobby, username: String): Player {
        val player = playerRepository.createOrUpdate(username, username)
        lobby.addPlayer(player)

        assertTrue(lobby.getPlayers().contains(player))
        assertSame(lobby, player.lobby)
        assertTrue(player.isInLobby)
        assertFalse(player.isInGame)
        return player
    }
}
