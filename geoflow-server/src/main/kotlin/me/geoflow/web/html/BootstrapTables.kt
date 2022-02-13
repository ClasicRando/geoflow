package me.geoflow.web.html

import kotlinx.html.ButtonType
import me.geoflow.core.database.tables.SourceTables
import kotlinx.html.FlowContent
import kotlinx.html.button
import kotlinx.html.checkBoxInput
import kotlinx.html.div
import kotlinx.html.i
import kotlinx.html.id
import kotlinx.html.label
import kotlinx.html.ol
import kotlinx.html.onClick
import kotlinx.html.option
import kotlinx.html.script
import kotlinx.html.select
import kotlinx.html.textInput
import me.geoflow.core.database.enums.FileCollectType
import me.geoflow.core.database.tables.PlottingFields
import me.geoflow.core.database.tables.PlottingMethods
import me.geoflow.core.database.tables.SourceTableColumns
import me.geoflow.core.web.html.JSElement
import me.geoflow.core.web.html.basicTable
import me.geoflow.core.web.html.tableButton
import me.geoflow.core.web.html.addParamsAsJsGlobalVariables
import me.geoflow.core.web.html.basicModal
import me.geoflow.core.web.html.confirmModal
import me.geoflow.core.web.html.formModal
import me.geoflow.core.web.html.subTableDetails

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
        subTableDetails = subTableDetails<SourceTableColumns>(
            url = "source-table-columns/{id}",
            idField = "st_oid",
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
                    select(classes = "custom-select") {
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
            "sourceTablesTable" to JSElement(id = SOURCE_TABLES_TABLE_ID, makeElement = false),
            "sourceTableModal" to JSElement(id = SOURCE_TABLES_MODAL_ID),
            "deleteSourceTableConfirm" to JSElement(id = DELETE_SOURCE_TABLE_CONFIRM_ID, makeElement = false),
        )
    }
    script {
        src = "/assets/source-tables.js"
    }
}

private const val PLOTTING_FIELDS_MODAL = "plottingFieldsModal"
private const val PLOTTING_FIELDS_FORM = "plottingFieldsForm"
private const val PLOTTING_FIELDS_TABLE = "plottingFieldsTable"
private const val CONFIRM_DELETE_PLOTTING_FIELDS = "confirmDeletePlottingFields"

/** */
@Suppress("LongMethod")
fun FlowContent.plottingFields(runId: Long) {
    basicTable<PlottingFields>(
        tableId = PLOTTING_FIELDS_TABLE,
        dataUrl = "plotting-fields/${runId}",
        dataField = "payload",
        clickableRows = false,
    )
    confirmModal(
        confirmModalId = CONFIRM_DELETE_PLOTTING_FIELDS,
        confirmMessage = "Are you sure you want to delete a plotting fields record?",
        resultFunction = "deletePlottingFields()",
    )
    formModal(
        modalId = PLOTTING_FIELDS_MODAL,
        headerText = "Plotting Fields",
        okClickFunction = "submitPlottingFields()",
        resetFormButton = true,
    ) {
        id = PLOTTING_FIELDS_FORM
        div(classes = "form-group") {
            label {
                htmlFor = "mergeKey"
                +"Merge Key"
            }
            select(classes = "custom-select") {
                id = "mergeKey"
                name = "mergeKey"
            }
        }
        div(classes = "form-group") {
            label {
                htmlFor = "companyName"
                +"Company Name"
            }
            select(classes = "custom-select") {
                id = "companyName"
                name = "companyName"
            }
        }
        div(classes = "form-group") {
            label {
                htmlFor = "addressLine1"
                +"Address Line 1"
            }
            select(classes = "custom-select") {
                id = "addressLine1"
                name = "addressLine1"
            }
        }
        div(classes = "form-group") {
            label {
                htmlFor = "addressLine2"
                +"Address Line 2"
            }
            select(classes = "custom-select") {
                id = "addressLine2"
                name = "addressLine2"
            }
        }
        div(classes = "form-group") {
            label {
                htmlFor = "city"
                +"City"
            }
            select(classes = "custom-select") {
                id = "city"
                name = "city"
            }
        }
        div(classes = "form-group") {
            label {
                htmlFor = "alternateCities"
                +"Alternate Cities"
            }
            select(classes = "custom-select") {
                id = "alternateCities"
                name = "alternateCities"
                multiple = true
            }
        }
        div(classes = "form-group") {
            label {
                htmlFor = "mailCode"
                +"Mail Code"
            }
            select(classes = "custom-select") {
                id = "mailCode"
                name = "mailCode"
            }
        }
        div(classes = "form-group") {
            label {
                htmlFor = "prov"
                +"Prov/State"
            }
            select(classes = "custom-select") {
                id = "prov"
                name = "prov"
            }
        }
        div(classes = "form-group") {
            label {
                htmlFor = "latitude"
                +"Latitude"
            }
            select(classes = "custom-select") {
                id = "latitude"
                name = "latitude"
            }
        }
        div(classes = "form-group") {
            label {
                htmlFor = "longitude"
                +"Longitude"
            }
            select(classes = "custom-select") {
                id = "longitude"
                name = "longitude"
            }
        }
    }
    script {
        addParamsAsJsGlobalVariables(
            "types" to FileCollectType.values(),
            "plottingFieldsTable" to JSElement(id = PLOTTING_FIELDS_TABLE, makeElement = false),
            "confirmDeletePlottingFields" to JSElement(id = CONFIRM_DELETE_PLOTTING_FIELDS),
            "plottingFieldsModal" to JSElement(id = PLOTTING_FIELDS_MODAL, makeElement = false),
            "plottingFieldsForm" to JSElement(id = PLOTTING_FIELDS_FORM, makeJQuery = false),
        )
    }
    script {
        src = "/assets/plotting-fields.js"
    }
}

private const val PLOTTING_METHODS_TABLE = "plottingMethodsTable"
private const val PLOTTING_METHODS_MODAL = "plottingMethodsModal"
private val plottingMethodButtons = listOf(
    tableButton(
        name = "btnEditPlottingMethods",
        text = "Edit Plotting Methods",
        icon = "edit",
        event = "editPlottingMethods()",
        title = "Edit the current plotting methods for the run",
    )
)

/** */
fun FlowContent.plottingMethods(runId: Long) {
    basicTable<PlottingMethods>(
        tableId = PLOTTING_METHODS_TABLE,
        dataUrl = "plotting-methods/${runId}",
        dataField = "payload",
        tableButtons = plottingMethodButtons,
        clickableRows = false,
    )
    basicModal(
        modalId = PLOTTING_METHODS_MODAL,
        headerText = "Plotting Methods",
        okClickFunction = "setPlottingFields()",
        size = "modal-xl",
    ) {
        button(classes = "btn btn-secondary") {
            type = ButtonType.button
            onClick = "addPlottingMethod()"
            +"Add Method"
            i(classes = "fas fa-plus p-1")
        }
        ol(classes = "list-group") {

        }
    }
    script {
        addParamsAsJsGlobalVariables(
                "plottingMethodsModal" to JSElement(id = PLOTTING_METHODS_MODAL),
                "plottingMethodsTable" to JSElement(id = PLOTTING_METHODS_TABLE, makeElement = false),
        )
    }
    script {
        src = "/assets/plotting-methods.js"
    }
}
