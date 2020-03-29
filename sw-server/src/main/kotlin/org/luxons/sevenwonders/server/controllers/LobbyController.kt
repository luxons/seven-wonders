package org.luxons.sevenwonders.server.controllers

import org.hildan.livedoc.core.annotations.Api
import org.luxons.sevenwonders.model.api.actions.ReorderPlayersAction
import org.luxons.sevenwonders.model.api.actions.UpdateSettingsAction
import org.luxons.sevenwonders.server.api.toDTO
import org.luxons.sevenwonders.server.lobby.Lobby
import org.luxons.sevenwonders.server.lobby.Player
import org.luxons.sevenwonders.server.repositories.LobbyRepository
import org.luxons.sevenwonders.server.repositories.PlayerRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import java.security.Principal

/**
 * Handles actions in the game's lobby. The lobby is the place where players gather before a game.
 */
@Api(name = "Lobby")
@Controller
class LobbyController @Autowired constructor(
    private val lobbyRepository: LobbyRepository,
    private val playerRepository: PlayerRepository,
    private val template: SimpMessagingTemplate
) {
    private val Principal.player: Player
        get() = playerRepository.find(name)

    /**
     * Leaves the current lobby.
     *
     * @param principal
     * the connected user's information
     */
    @MessageMapping("/lobby/leave")
    fun leave(principal: Principal) {
        val player = principal.player
        val lobby = player.lobby
        lobby.removePlayer(principal.name)
        if (lobby.getPlayers().isEmpty()) {
            lobbyRepository.remove(lobby.id)
        }

        logger.info("Player {} left game '{}'", player, lobby.name)
        sendLobbyUpdateToPlayers(lobby)
    }

    /**
     * Reorders the players in the current lobby. This can only be done by the lobby's owner.
     *
     * @param action
     * the action to reorder the players
     * @param principal
     * the connected user's information
     */
    @MessageMapping("/lobby/reorderPlayers")
    fun reorderPlayers(@Validated action: ReorderPlayersAction, principal: Principal) {
        val lobby = principal.player.ownedLobby
        lobby.reorderPlayers(action.orderedPlayers)

        logger.info("Players in game '{}' reordered to {}", lobby.name, action.orderedPlayers)
        sendLobbyUpdateToPlayers(lobby)
    }

    /**
     * Updates the game settings. This can only be done by the lobby's owner.
     *
     * @param action
     * the action to update the settings
     * @param principal
     * the connected user's information
     */
    @MessageMapping("/lobby/updateSettings")
    fun updateSettings(@Validated action: UpdateSettingsAction, principal: Principal) {
        val lobby = principal.player.ownedLobby
        lobby.settings = action.settings

        logger.info("Updated settings of game '{}'", lobby.name)
        sendLobbyUpdateToPlayers(lobby)
    }

    internal fun sendLobbyUpdateToPlayers(lobby: Lobby) {
        lobby.getPlayers().forEach {
            template.convertAndSendToUser(it.username, "/queue/lobby/updated", lobby.toDTO())
        }
        template.convertAndSend("/topic/games", listOf(lobby.toDTO()))
    }

    /**
     * Starts the game.
     *
     * @param principal
     * the connected user's information
     */
    @MessageMapping("/lobby/startGame")
    fun startGame(principal: Principal) {
        val lobby = principal.player.ownedLobby
        val game = lobby.startGame()

        logger.info("Game {} successfully started", game.id)
        template.convertAndSend("/topic/lobby/" + lobby.id + "/started", Unit)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LobbyController::class.java)
    }
}
