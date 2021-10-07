package html

import kotlinx.html.*
import orm.enums.FileCollectType
import orm.tables.SourceTables

const val messageBoxId = "msgBox"

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
                        onClick = "${okClickFunction}()"
                        +"OK"
                    }
                }
            }
        }
    }
}

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

fun FlowContent.messageBoxModal() {
    div(classes = "modal fade") {
        id = messageBoxId
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
        unsafe {
            raw("var messageBoxId = '$messageBoxId';")
        }
    }
    script {
        src = "assets/messagebox.js"
    }
}

private const val sourceTableModalId = "sourceTableData"
private const val sourceTablesTableId = "source-tables"

fun FlowContent.sourceTablesModal(runId: Long) {
    div(classes = "modal fade") {
        id = sourceTableModalId
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
                    id = "${sourceTableModalId}Body"
                    basicTable(
                        sourceTablesTableId,
                        "/api/source-tables?runId=$runId",
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
        id = "${sourceTableModalId}EditRow"
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
                        id = "${sourceTableModalId}EditRowBody"
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
                        id = "deleteRecord"
                        type = ButtonType.button
                        onClick = "deleteSourceTable()"
                        +"Delete"
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