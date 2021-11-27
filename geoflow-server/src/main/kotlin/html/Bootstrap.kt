package html

import database.enums.FileCollectType
import kotlinx.html.ButtonType
import kotlinx.html.DIV
import kotlinx.html.FORM
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.checkBoxInput
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h5
import kotlinx.html.id
import kotlinx.html.label
import kotlinx.html.li
import kotlinx.html.onClick
import kotlinx.html.option
import kotlinx.html.p
import kotlinx.html.role
import kotlinx.html.script
import kotlinx.html.select
import kotlinx.html.textInput
import kotlinx.html.ul

/** ID of the generic messagebox modal */
const val MESSAGE_BOX_ID: String = "msgBox"

/**
 * Creates a basic Bootstrap modal with an [id][modalId], [header text][headerText], [body message][bodyMessage] and
 * name of a [function][okClickFunction] that is called when the ok button is pressed.
 */
fun FlowContent.basicModal(modalId: String, headerText: String, bodyMessage: String, okClickFunction: String) {
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
                        onClick = "$okClickFunction()"
                        +"OK"
                    }
                }
            }
        }
    }
}

/** Generic modal with the ability to add a custom form body */
inline fun FlowContent.formModal(
    modalId: String,
    headerText: String,
    okClickFunction: String,
    crossinline body: FORM.() -> Unit,
) {
    div(classes = "modal fade") {
        id = modalId
        attributes["data-backdrop"] = "static"
        attributes["data-keyboard"] = "false"
        attributes["tabindex"] = "-1"
        attributes["aria-labelledby"] = "staticBackdropLabel"
        attributes["aria-hidden"] = "true"
        div(classes = "modal-dialog modal-dialog-centered modal-dialog-scrollable") {
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
                    button(classes = "btn btn-secondary") {
                        type = ButtonType.button
                        onClick = "$okClickFunction()"
                        +"OK"
                    }
                }
            }
        }
    }
}

/**
 * Creates a blank modal that can have various values displayed during webpage operation.
 */
fun FlowContent.dataDisplayModal(modalId: String, headerText: String) {
    div(classes = "modal fade") {
        id = modalId
        attributes["data-backdrop"] = "static"
        attributes["data-keyboard"] = "false"
        attributes["tabindex"] = "-1"
        attributes["aria-labelledby"] = "staticBackdropLabel"
        attributes["aria-hidden"] = "true"
        div(classes = "modal-dialog modal-dialog-centered modal-dialog-scrollable") {
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

/**
 * Creates a modal that can be used to display a simple message during webpage operations. Only call this function
 * once per webpage templating to avoid having more than 1 messagebox popup during message display call.
 */
fun FlowContent.messageBoxModal() {
    div(classes = "modal fade") {
        id = MESSAGE_BOX_ID
        attributes["data-backdrop"] = "static"
        attributes["data-keyboard"] = "false"
        attributes["tabindex"] = "-1"
        attributes["aria-labelledby"] = "msgBoxHeader"
        attributes["aria-hidden"] = "true"
        div(classes = "modal-dialog modal-dialog-centered modal-dialog-scrollable") {
            div(classes = "modal-content") {
                div(classes = "modal-header") {
                    h5(classes = "modal-title") {
                        id = "msgBoxHeader"
                    }
                }
                div(classes = "modal-body") {
                    p {
                        id = "msgBoxBody"
                    }
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
    script {
        addParamsAsJsGlobalVariables(
            mapOf(
                "messageBoxId" to MESSAGE_BOX_ID,
            )
        )
    }
    script {
        src = "/assets/messagebox.js"
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
                        onClick = "$resultFunction()"
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

private const val SOURCE_TABLES_MODAL_ID = "sourceTableDataEditRow"
private const val SOURCE_TABLES_RESPONSE_ERROR_MESSAGE_ID = "sourceTableDataEditRowResponseErrorMessage"
private const val DELETE_SOURCE_TABLE_CONFIRM_ID = "deleteSourceTable"
private const val SOURCE_TABLE_RECORD_LABEL_ID = "sourceTableRecordLabel"

/**
 * Modal for creating/editing source table records. Accepts information in a form based input to produce an object for
 * an api call. Also includes a confirmation modal for deleting source table records
 */
@Suppress("LongMethod")
fun FlowContent.sourceTableEditModal() {
    div(classes = "modal fade") {
        id = SOURCE_TABLES_MODAL_ID
        attributes["data-backdrop"] = "static"
        attributes["data-keyboard"] = "false"
        attributes["tabindex"] = "-1"
        attributes["aria-labelledby"] = "sourceTableRecordLabel"
        attributes["aria-hidden"] = "true"
        div(classes = "modal-dialog modal-dialog-centered modal-dialog-scrollable modal-xl") {
            div(classes = "modal-content") {
                div(classes = "modal-header") {
                    h5(classes = "modal-title") {
                        id = SOURCE_TABLE_RECORD_LABEL_ID
                        +"Edit Row"
                    }
                }
                div(classes = "modal-body") {
                    form {
                        action = ""
                        id = "${SOURCE_TABLES_MODAL_ID}Body"
                        div(classes = "form-group row") {
                            div(classes = "col") {
                                div(classes = "form-group") {
                                    label {
                                        htmlFor = "table_name"
                                        +"Table Name"
                                    }
                                    textInput(classes = "form-control") {
                                        id = "table_name"
                                        name = "table_name"
                                    }
                                }
                                div(classes = "form-group") {
                                    label {
                                        htmlFor = "file_id"
                                        +"File ID"
                                    }
                                    textInput(classes = "form-control") {
                                        id = "file_id"
                                        name = "file_id"
                                    }
                                }
                                div(classes = "form-group") {
                                    label {
                                        htmlFor = "file_name"
                                        +"File Name"
                                    }
                                    textInput(classes = "form-control") {
                                        id = "file_name"
                                        name = "file_name"
                                    }
                                }
                                div(classes = "form-group") {
                                    label {
                                        htmlFor = "sub_table"
                                        +"Sub Table"
                                    }
                                    textInput(classes = "form-control") {
                                        id = "sub_table"
                                        name = "sub_table"
                                    }
                                }
                            }
                            div(classes = "col") {
                                div(classes = "form-group") {
                                    label {
                                        htmlFor = "collect_type"
                                        +"Collect Type"
                                    }
                                    select(classes = "form-control") {
                                        id = "collect_type"
                                        name = "collect_type"
                                        for (type in FileCollectType.values()) {
                                            option {
                                                value = type.name
                                                +type.name
                                            }
                                        }
                                    }
                                }
                                div(classes = "form-group") {
                                    label {
                                        htmlFor = "url"
                                        +"URL"
                                    }
                                    textInput(classes = "form-control") {
                                        id = "url"
                                        name = "url"
                                    }
                                }
                                div(classes = "form-group") {
                                    label {
                                        htmlFor = "comments"
                                        +"Comments"
                                    }
                                    textInput(classes = "form-control") {
                                        id = "comments"
                                        name = "comments"
                                    }
                                }
                                div(classes = "form-group") {
                                    label {
                                        htmlFor = "delimiter"
                                        +"Delimiter"
                                    }
                                    textInput(classes = "form-control") {
                                        id = "delimiter"
                                        name = "delimiter"
                                    }
                                }
                            }
                        }
                        div(classes = "form-group row") {
                            div(classes = "custom-control custom-checkbox custom-control-inline") {
                                checkBoxInput(classes = "custom-control-input") {
                                    id = "qualified"
                                    name = "qualified"
                                }
                                label(classes = "custom-control-label") {
                                    htmlFor = "qualified"
                                    +"Qualified"
                                }
                            }
                            div(classes = "custom-control custom-checkbox custom-control-inline") {
                                checkBoxInput(classes = "custom-control-input") {
                                    id = "analyze"
                                    name = "analyze"
                                }
                                label(classes = "custom-control-label") {
                                    htmlFor = "analyze"
                                    +"Analyze"
                                }
                            }
                            div(classes = "custom-control custom-checkbox custom-control-inline") {
                                checkBoxInput(classes = "custom-control-input") {
                                    id = "load"
                                    name = "load"
                                }
                                label(classes = "custom-control-label") {
                                    htmlFor = "load"
                                    +"Load"
                                }
                            }
                        }
                    }
                }
                div(classes = "modal-footer") {
                    p(classes = "invalidInput") {
                        id = SOURCE_TABLES_RESPONSE_ERROR_MESSAGE_ID
                    }
                    button(classes = "btn btn-secondary") {
                        id = "saveChanges"
                        type = ButtonType.button
                        onClick = "saveSourceTableChanges()"
                        +"Save"
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
    confirmModal(
        confirmModalId = DELETE_SOURCE_TABLE_CONFIRM_ID,
        confirmMessage = "Are you sure you want to delete this record?",
        resultFunction = "deleteSourceTable",
    )
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
