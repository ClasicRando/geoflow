package html

import kotlinx.html.*

fun FlowContent.basicModal(modalId: String, headerText: String, bodyMessage: String) {
    div(classes = "modal fade") {
        id = modalId
        attributes["data-backdrop"] = "static"
        attributes["data-keyboard"] = "false"
        attributes["tabindex"] = "-1"
        attributes["aria-labelledby"] = "staticBackdropLabel"
        attributes["aria-hidden"] = "true"
        div(classes = "modal-dialog modal-dialog-centered") {
            div(classes = "modal-content") {
                div(classes = "modal-header") {
                    h5(classes = "modal-title") {
                        id = "staticBackdropLabel"
                        +headerText
                    }
                    button(classes = "close") {
                        type = ButtonType.button
                        attributes["data-dismiss"] = "modal"
                        attributes["aria-label"] = "Close"
                        span {
                            attributes["aria-hidden"] = "true"
                            +"&times;"
                        }
                    }
                }
                div(classes = "modal-body") {
                    p {
                        +bodyMessage
                    }
                }
                div(classes = "modal-footer") {
                    button(classes = "btn btn-secondary") {
                        type = ButtonType.button
                        attributes["data-dismiss"] = "modal"
                        +"Close"
                    }
                    button(classes = "btn btn-secondary") {
                        type = ButtonType.button
                        onClick = "pickup()"
                        +"OK"
                    }
                }
            }
        }
    }
}