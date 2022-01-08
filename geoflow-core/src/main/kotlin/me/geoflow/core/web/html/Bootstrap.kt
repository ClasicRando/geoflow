package me.geoflow.core.web.html

import kotlinx.html.ButtonType
import kotlinx.html.DIV
import kotlinx.html.FORM
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h5
import kotlinx.html.id
import kotlinx.html.li
import kotlinx.html.onClick
import kotlinx.html.p
import kotlinx.html.role
import kotlinx.html.ul

/** Generic modal with the ability to add a custom form body */
fun FlowContent.emptyModal(
    modalId: String,
    headerText: String,
    size: String = "",
) {
    div(classes = "modal fade") {
        id = modalId
        attributes["data-backdrop"] = "static"
        attributes["data-keyboard"] = "false"
        attributes["tabindex"] = "-1"
        attributes["aria-labelledby"] = "staticBackdropLabel"
        attributes["aria-hidden"] = "true"
        div(classes = "modal-dialog modal-dialog-centered modal-dialog-scrollable $size") {
            div(classes = "modal-content") {
                div(classes = "modal-header") {
                    h5(classes = "modal-title") {
                        id = "staticBackdropLabel"
                        +headerText
                    }
                }
                div(classes = "modal-body") {
                    id = "${modalId}Body"
                }
                div(classes = "modal-footer") {
                    button(classes = "btn btn-secondary") {
                        type = ButtonType.button
                        attributes["data-dismiss"] = "modal"
                        +"Close"
                    }
                }
            }
        }
    }
}

/** Generic modal with the ability to add a custom form body */
@Suppress("LongParameterList")
inline fun FlowContent.formModal(
    modalId: String,
    headerText: String,
    okClickFunction: String,
    resetFormButton: Boolean = false,
    size: String = "",
    crossinline body: FORM.() -> Unit,
) {
    div(classes = "modal fade") {
        id = modalId
        attributes["data-backdrop"] = "static"
        attributes["data-keyboard"] = "false"
        attributes["tabindex"] = "-1"
        attributes["aria-labelledby"] = "staticBackdropLabel"
        attributes["aria-hidden"] = "true"
        div(classes = "modal-dialog modal-dialog-centered modal-dialog-scrollable $size") {
            div(classes = "modal-content") {
                div(classes = "modal-header") {
                    h5(classes = "modal-title") {
                        id = "staticBackdropLabel"
                        +headerText
                    }
                }
                div(classes = "modal-body") {
                    form {
                        body()
                    }
                }
                div(classes = "modal-footer") {
                    p(classes = "invalidInput") {
                        id = "${modalId}ResponseErrorMessage"
                    }
                    button(classes = "btn btn-secondary") {
                        type = ButtonType.button
                        attributes["data-dismiss"] = "modal"
                        +"Close"
                    }
                    if (resetFormButton) {
                        button(classes = "btn btn-secondary") {
                            type = ButtonType.button
                            onClick = "resetForm($('#${modalId}'))"
                            +"Reset"
                        }
                    }
                    button(classes = "btn btn-secondary") {
                        type = ButtonType.button
                        onClick = okClickFunction
                        +"OK"
                    }
                }
            }
        }
    }
}

/**
 * Simple confirmation modal that prompts the user with a [message][confirmMessage] to make sure they want to perform
 * the current action. After the user blocks the OK button the [resultFunction] is called.
 */
fun FlowContent.confirmModal(confirmModalId: String, confirmMessage: String, resultFunction: String) {
    div(classes = "modal fade") {
        id = confirmModalId
        attributes["data-backdrop"] = "static"
        attributes["data-keyboard"] = "false"
        attributes["tabindex"] = "-1"
        attributes["aria-labelledby"] = "${confirmModalId}Header"
        attributes["aria-hidden"] = "true"
        div(classes = "modal-dialog modal-dialog-centered modal-dialog-scrollable") {
            div(classes = "modal-content") {
                div(classes = "modal-header") {
                    h5(classes = "modal-title") {
                        id = "${confirmModalId}Header"
                        +"Confirm Action"
                    }
                }
                div(classes = "modal-body") {
                    p {
                        +confirmMessage
                    }
                }
                div(classes = "modal-footer") {
                    button(classes = "btn btn-secondary") {
                        id = "${confirmModalId}Confirm"
                        type = ButtonType.button
                        onClick = resultFunction
                        +"OK"
                    }
                    button(classes = "btn btn-secondary") {
                        type = ButtonType.button
                        attributes["data-dismiss"] = "modal"
                        +"Close"
                    }
                }
            }
        }
    }
}

/** Container for a tab nav element. Accepts a DIV lambda for the user to customize the tab content */
data class TabNav(
    /** label in the tab navbar */
    val label: String,
    /** lambda that evaluates to html elements as the content of the tab */
    val content: DIV.() -> Unit
) {
    /** Name of the tab as used in html properties. Normalizes whitespace and case of the text */
    val name: String = label
        .replace(Regex("\\s+"), "_")
        .trim('_')
        .lowercase()
}

/** Returns a [TabNav] with the [label] and [content] */
fun tabNav(label: String, content: DIV.() -> Unit): TabNav {
    return TabNav(label, content)
}

/** Generic tab layout with the [tabs] provided */
fun FlowContent.tabLayout(vararg tabs: TabNav) {
    ul(classes = "nav nav-tabs") {
        id = "tabs"
        role = "tablist"
        for ((i, tab) in tabs.withIndex()) {
            li(classes = "nav-item") {
                role = "presentation"
                a {
                    classes = buildSet {
                        add("nav-link")
                        if (i == 0) {
                            add("active")
                        }
                    }
                    id = "${tab.name}-tab"
                    href = "#${tab.name}"
                    role = "tab"
                    attributes["data-toggle"] = "tab"
                    attributes["aria-controls"] = tab.name
                    attributes["aria-selected"] = (i == 0).toString()
                    +tab.label
                }
            }
        }
    }
    div(classes = "tab-content") {
        id = "tabContent"
        for ((i, tab) in tabs.withIndex()) {
            div {
                classes = buildSet {
                    add("tab-pane")
                    add("fade")
                    if (i == 0) {
                        add("show")
                        add("active")
                    }
                }
                id = tab.name
                role = "tabpanel"
                attributes["aria-labelled-by"] = "${tab.name}-tab"
                tab.content.invoke(this)
            }
        }
    }
}
