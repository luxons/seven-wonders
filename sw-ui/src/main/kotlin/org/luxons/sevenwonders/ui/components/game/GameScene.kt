package org.luxons.sevenwonders.ui.components.game

import com.palantir.blueprintjs.Intent
import com.palantir.blueprintjs.bpButton
import com.palantir.blueprintjs.bpButtonGroup
import com.palantir.blueprintjs.bpOverlay
import kotlinx.css.*
import kotlinx.css.properties.*
import org.luxons.sevenwonders.model.Action
import org.luxons.sevenwonders.model.PlayerMove
import org.luxons.sevenwonders.model.PlayerTurnInfo
import org.luxons.sevenwonders.model.api.PlayerDTO
import org.luxons.sevenwonders.model.cards.HandCard
import org.luxons.sevenwonders.ui.components.GlobalStyles
import org.luxons.sevenwonders.ui.redux.GameState
import org.luxons.sevenwonders.ui.redux.RequestPrepareMove
import org.luxons.sevenwonders.ui.redux.RequestSayReady
import org.luxons.sevenwonders.ui.redux.RequestUnprepareMove
import org.luxons.sevenwonders.ui.redux.connectStateAndDispatch
import react.RBuilder
import react.RClass
import react.RComponent
import react.RProps
import react.RState
import react.ReactElement
import react.dom.*
import styled.css
import styled.styledDiv

interface GameSceneStateProps : RProps {
    var playerIsReady: Boolean
    var players: List<PlayerDTO>
    var gameState: GameState?
    var preparedMove: PlayerMove?
    var preparedCard: HandCard?
}

interface GameSceneDispatchProps : RProps {
    var sayReady: () -> Unit
    var prepareMove: (move: PlayerMove) -> Unit
    var unprepareMove: () -> Unit
}

interface GameSceneProps : GameSceneStateProps, GameSceneDispatchProps

private class GameScene(props: GameSceneProps) : RComponent<GameSceneProps, RState>(props) {

    override fun RBuilder.render() {
        styledDiv {
            css {
                background = "url('images/background-papyrus3.jpg')"
                backgroundSize = "cover"
                +GlobalStyles.fullscreen
            }
            val turnInfo = props.gameState?.turnInfo
            if (turnInfo == null) {
                p { +"Error: no turn info data" }
            } else {
                turnInfoScene(turnInfo)
            }
        }
    }

    private fun RBuilder.sayReadyButton(): ReactElement {
        val isReady = props.playerIsReady
        val intent = if (isReady) Intent.SUCCESS else Intent.PRIMARY
        return styledDiv {
            css {
                position = Position.absolute
                bottom = 6.rem
                left = 50.pct
                transform { translate(tx = (-50).pct) }
            }
            bpButtonGroup {
                bpButton(
                    large = true,
                    disabled = isReady,
                    intent = intent,
                    icon = if (isReady) "tick-circle" else "play",
                    onClick = { props.sayReady() }
                ) {
                    +"READY"
                }
                // not really a button, but nice for style
                bpButton(
                    large = true,
                    icon = "people",
                    disabled = isReady,
                    intent = intent
                ) {
                    +"${props.players.count { it.isReady }}/${props.players.size}"
                }
            }
        }
    }

    private fun RBuilder.turnInfoScene(turnInfo: PlayerTurnInfo) {
        val board = turnInfo.table.boards[turnInfo.playerIndex]
        div {
            // TODO use blueprint's Callout component without header and primary intent
            p { +turnInfo.message }
            boardComponent(board = board)
            val hand = turnInfo.hand
            if (hand != null) {
                handComponent(
                    cards = hand,
                    wonderUpgradable = turnInfo.wonderBuildability.isBuildable,
                    preparedMove = props.preparedMove,
                    prepareMove = props.prepareMove
                )
            }
            val card = props.preparedCard
            if (card != null) {
                preparedMove(card)
            }
            if (turnInfo.action == Action.SAY_READY) {
                sayReadyButton()
            }
            productionBar(gold = board.gold, production = board.production)
        }
    }

    private fun RBuilder.preparedMove(card: HandCard) {
        bpOverlay(isOpen = true, onClose = props.unprepareMove) {
            styledDiv {
                css { +GlobalStyles.fixedCenter }
                cardImage(card)
                styledDiv {
                    css {
                        position = Position.absolute
                        top = 0.px
                        right = 0.px
                    }
                    bpButton(
                        icon = "cross",
                        title = "Cancel prepared move",
                        small = true,
                        intent = Intent.DANGER,
                        onClick = { props.unprepareMove() }
                    )
                }
            }
        }
    }
}

fun RBuilder.gameScene() = gameScene {}

private val gameScene: RClass<GameSceneProps> =
        connectStateAndDispatch<GameSceneStateProps, GameSceneDispatchProps, GameSceneProps>(
            clazz = GameScene::class,
            mapDispatchToProps = { dispatch, _ ->
                prepareMove = { move -> dispatch(RequestPrepareMove(move)) }
                unprepareMove = { dispatch(RequestUnprepareMove()) }
                sayReady = { dispatch(RequestSayReady()) }
            },
            mapStateToProps = { state, _ ->
                playerIsReady = state.currentPlayer?.isReady == true
                players = state.gameState?.players ?: emptyList()
                gameState = state.gameState
                preparedMove = state.gameState?.currentPreparedMove
                preparedCard = state.gameState?.currentPreparedCard
            }
        )