package me.geoflow.web.html

import me.geoflow.core.database.tables.SourceTables
import kotlinx.html.FlowContent
import kotlinx.html.checkBoxInput
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.label
import kotlinx.html.option
import kotlinx.html.script
import kotlinx.html.select
import kotlinx.html.textInput
import me.geoflow.core.database.enums.FileCollectType
import me.geoflow.core.database.tables.SourceTableColumns
import me.geoflow.core.web.html.JSElement
import me.geoflow.core.web.html.SubTableDetails
import me.geoflow.core.web.html.basicTable
import me.geoflow.core.web.html.tableButton
import me.geoflow.core.web.html.addParamsAsJsGlobalVariables
import me.geoflow.core.web.html.confirmModal
import me.geoflow.core.web.html.formModal

private const val SOURCE_TABLES_TABLE_ID = "sourceTables"
private const val SOURCE_TABLES_MODAL_ID = "sourceTableDataEditRow"
private const val DELETE_SOURCE_TABLE_CONFIRM_ID = "deleteSourceTable"

/** Constructs a basic table for source table data */
@Suppress("LongMethod")
fun FlowContent.sourceTables(runId: Long) {
    basicTable<SourceTables>(
        tableId = SOURCE_TABLES_TABLE_ID,
        dataUrl = "source-tables/$runId",
        dataField = "payload",
        tableButtons = listOf(
            tableButton(
                name = "btnAddTable",
                text = "Add Source Table",
                icon = "fa-plus",
                event = "newSourceTableRow()",
                title = "Add new source table to the current run",
            ),
        ),
        customSortFunction = "sourceTableRecordSorting",
        clickableRows = false,
        subTableDetails = SubTableDetails(
            url = "source-table-columns/{id}",
            idField = "st_oid",
            fields = SourceTableColumns.tableDisplayFields,
        ),
    )
    formModal(
        modalId = SOURCE_TABLES_MODAL_ID,
        headerText = "Edit Row",
        okClickFunction = "saveSourceTableChanges()",
        size = "modal-xl",
    ) {
        action = ""
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
    confirmModal(
        confirmModalId = DELETE_SOURCE_TABLE_CONFIRM_ID,
        confirmMessage = "Are you sure you want to delete this record?",
        resultFunction = "deleteSourceTable()",
    )
    script {
        addParamsAsJsGlobalVariables(
            "sourceTablesTable" to JSElement(query = "#$SOURCE_TABLES_TABLE_ID", makeSelector = false),
            "sourceTableModal" to JSElement(query = "#$SOURCE_TABLES_MODAL_ID"),
            "deleteSourceTableConfirm" to JSElement(query = "#${DELETE_SOURCE_TABLE_CONFIRM_ID}", makeSelector = false),
        )
    }
    script {
        src = "/assets/source-tables.js"
    }
}
