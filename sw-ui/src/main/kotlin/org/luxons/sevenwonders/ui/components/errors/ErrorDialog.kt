package org.luxons.sevenwonders.ui.components.errors

import com.palantir.blueprintjs.Classes
import com.palantir.blueprintjs.Intent
import com.palantir.blueprintjs.bpButton
import com.palantir.blueprintjs.bpDialog
import kotlinx.browser.window
import org.luxons.sevenwonders.ui.redux.*
import org.luxons.sevenwonders.ui.router.Navigate
import org.luxons.sevenwonders.ui.router.Route
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.p
import styled.css
import styled.styledDiv

interface ErrorDialogStateProps : RProps {
    var errorMessage: String?
}

interface ErrorDialogDispatchProps : RProps {
    var goHome: () -> Unit
}

interface ErrorDialogProps : ErrorDialogDispatchProps, ErrorDialogStateProps

class ErrorDialogPresenter(props: ErrorDialogProps) : RComponent<ErrorDialogProps, RState>(props) {
    override fun RBuilder.render() {
        val errorMessage = props.errorMessage
        bpDialog(
            isOpen = errorMessage != null,
            title = "Oops!",
            icon = "error",
            iconIntent = Intent.DANGER,
            onClose = { goHomeAndRefresh() }
        ) {
            styledDiv {
                css {
                    classes.add(Classes.DIALOG_BODY)
                }
                p {
                    +(errorMessage ?: "fatal error")
                }
            }
            styledDiv {
                css {
                    classes.add(Classes.DIALOG_FOOTER)
                }
                bpButton(icon = "log-out", onClick = { goHomeAndRefresh() }) {
                    +"HOME"
                }
            }
        }
    }
}

private fun goHomeAndRefresh() {
    // we don't use a redux action here because we actually want to redirect and refresh the page
    window.location.href = Route.HOME.path
}

fun RBuilder.errorDialog() = errorDialog {}

private val errorDialog = connectStateAndDispatch<ErrorDialogStateProps, ErrorDialogDispatchProps, ErrorDialogProps>(
    clazz = ErrorDialogPresenter::class,
    mapStateToProps = { state, _ ->
        errorMessage = state.fatalError
    },
    mapDispatchToProps = { dispatch, _ ->
        goHome = { dispatch(Navigate(Route.HOME)) }
    },
)
