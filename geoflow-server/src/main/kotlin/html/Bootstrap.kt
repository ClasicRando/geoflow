package html

import database.enums.FileCollectType
import database.tables.SourceTables
import kotlinx.html.*

const val MESSAGE_BOX_ID = "msgBox"

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

private const val SOURCE_TABLES_MODAL_ID = "sourceTableData"
private const val SOURCE_TABLES_TABLE_ID = "sourceTables"
private const val DELETE_SOURCE_TABLE_CONFIRM_ID = "deleteSourceTable"

/**
 * Creates a modal to show the source tables for a given pipeline run in a [basicTable]. This function also adds the
 * modal to edit table entries (including the [confirmModal] after editing). Only call this function once per webpage
 * templating to avoid having more than 1 modal popup during show call.
 */
fun FlowContent.sourceTablesModal(runId: Long) {
    div(classes = "modal fade") {
        id = SOURCE_TABLES_MODAL_ID
        attributes["data-backdrop"] = "static"
        attributes["data-keyboard"] = "false"
        attributes["tabindex"] = "-1"
        attributes["aria-labelledby"] = "sourceTableModalLabel"
        attributes["aria-hidden"] = "true"
        div(classes = "modal-dialog modal-dialog-centered modal-dialog-scrollable modal-xl") {
            style = "max-width: 90%"
            div(classes = "modal-content") {
                div(classes = "modal-header") {
                    h5(classes = "modal-title") {
                        id = "sourceTableModalLabel"
                        +"Source Tables"
                    }
                }
                div(classes = "modal-body") {
                    id = "${SOURCE_TABLES_MODAL_ID}Body"
                    basicTable(
                        SOURCE_TABLES_TABLE_ID,
                        "/api/source-tables/$runId",
                        SourceTables.tableDisplayFields,
                        tableButtons = listOf(
                            TableButton(
                                "btnAddTable",
                                "Add Source Table",
                                "fa-plus",
                                "newSourceTableRow()",
                                "Add new source table to the current run",
                            ),
                        ),
                        customSortFunction = "sourceTableRecordSorting",
                        clickableRows = false,
                    )
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
    div(classes = "modal fade") {
        id = "${SOURCE_TABLES_MODAL_ID}EditRow"
        attributes["data-backdrop"] = "static"
        attributes["data-keyboard"] = "false"
        attributes["tabindex"] = "-1"
        attributes["aria-labelledby"] = "sourceTableRecordLabel"
        attributes["aria-hidden"] = "true"
        div(classes = "modal-dialog modal-dialog-centered modal-dialog-scrollable modal-xl") {
            div(classes = "modal-content") {
                div(classes = "modal-header") {
                    h5(classes = "modal-title") {
                        id = "sourceTableRecordLabel"
                        +"Edit Row"
                    }
                }
                div(classes = "modal-body") {
                    form {
                        action = ""
                        id = "${SOURCE_TABLES_MODAL_ID}EditRowBody"
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
                                        FileCollectType.values().forEach { type ->
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
        DELETE_SOURCE_TABLE_CONFIRM_ID,
        "Are you sure you want to delete this record?",
        "deleteSourceTable",
    )
}
