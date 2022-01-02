package me.geoflow.web.html

import me.geoflow.core.database.enums.FileCollectType
import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.button
import kotlinx.html.checkBoxInput
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h5
import kotlinx.html.id
import kotlinx.html.label
import kotlinx.html.onClick
import kotlinx.html.option
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.select
import kotlinx.html.textInput
import me.geoflow.core.web.html.addParamsAsJsGlobalVariables
import me.geoflow.core.web.html.confirmModal

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
        resultFunction = "deleteSourceTable($('#${DELETE_SOURCE_TABLE_CONFIRM_ID}'))",
    )
    script {
        addParamsAsJsGlobalVariables(
            "sourceTableModalId" to SOURCE_TABLES_MODAL_ID,
            "sourceTableRecordLabelId" to SOURCE_TABLE_RECORD_LABEL_ID,
            "deleteSourceTableConfirmId" to DELETE_SOURCE_TABLE_CONFIRM_ID,
        )
    }
}
