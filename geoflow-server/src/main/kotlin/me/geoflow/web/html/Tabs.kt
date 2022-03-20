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
import me.geoflow.core.database.tables.GeneratedTableColumns
import me.geoflow.core.database.tables.PipelineRelationshipFields
import me.geoflow.core.database.tables.PipelineRelationships
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
private const val EDIT_SOURCE_FIELD = "editSourceField"
private const val SOURCE_FIELD_NAME = "sourceFieldName"
private const val SOURCE_FIELD_LABEL = "sourceFieldLabel"
private const val SOURCE_FIELD_REPORT_GROUP = "sourceFieldReportGroup"

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
    formModal(
        modalId = EDIT_SOURCE_FIELD,
        headerText = "Edit Source Field",
        okClickFunction = "commitSourceField()",
    ) {
        div(classes = "form-group") {
            label {
                htmlFor = SOURCE_FIELD_NAME
                +"Field Name"
            }
            textInput(classes = "form-control") {
                id = SOURCE_FIELD_NAME
                name = "name"
                readonly = true
            }
        }
        div(classes = "form-group") {
            label {
                htmlFor = SOURCE_FIELD_LABEL
                +"Report Label"
            }
            textInput(classes = "form-control") {
                id = SOURCE_FIELD_LABEL
                name = "label"
            }
        }
        div(classes = "form-group") {
            label {
                htmlFor = SOURCE_FIELD_REPORT_GROUP
                +"Report Group"
            }
            textInput(classes = "form-control") {
                id = SOURCE_FIELD_REPORT_GROUP
                name = "reportGroup"
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
            "editSourceField" to JSElement(id = EDIT_SOURCE_FIELD),
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

private const val GENERATED_FIELDS_LIST = "generatedFieldsList"
private const val EDIT_GENERATED_FIELD = "editGeneratedField"
private const val GENERATED_SOURCE_TABLE_SELECT = "generatedSourceTableSelect"
private const val GENERATED_FIELD_DELETE_MODAL = "generatedFieldDeleteModal"
private const val GENERATED_FIELD_NAME = "generatedFieldName"
private const val GENERATED_FIELD_LABEL = "generatedFieldLabel"
private const val GENERATED_FIELD_EXPRESSION = "generatedFieldExpression"
private const val GENERATED_FIELD_REPORT_GROUP = "generatedFieldReportGroup"

/** */
@Suppress("LongMethod")
fun FlowContent.generatedFields() {
    div(classes = "row py-1") {
        div(classes = "col") {
            label {
                htmlFor = GENERATED_SOURCE_TABLE_SELECT
                +"Source Table"
            }
            select(classes = "custom-select") {
                id = GENERATED_SOURCE_TABLE_SELECT
            }
        }
    }
    basicTable<GeneratedTableColumns>(
        tableId = GENERATED_FIELDS_LIST,
        dataField = "payload",
        tableButtons = listOf(
            tableButton(
                name = "btnAddGeneratedField",
                icon = "fa-plus",
                event = "addGeneratedField()",
                text = "Add Generated Field",
                title = "Add an expression to be created while loading the data into the pipeline",
            )
        ),
        clickableRows = false,
    )
    formModal(
        modalId = EDIT_GENERATED_FIELD,
        headerText = "Edit Generated Field",
        okClickFunction = "commitGeneratedField()",
    ) {
        div(classes = "form-group") {
            label {
                htmlFor = GENERATED_FIELD_NAME
                +"Field Name"
            }
            textInput(classes = "form-control") {
                id = GENERATED_FIELD_NAME
                name = "name"
            }
        }
        div(classes = "form-group") {
            label {
                htmlFor = GENERATED_FIELD_LABEL
                +"Label"
            }
            textInput(classes = "form-control") {
                id = GENERATED_FIELD_LABEL
                name = "label"
            }
        }
        div(classes = "form-group") {
            label {
                htmlFor = GENERATED_FIELD_REPORT_GROUP
                +"Report Group"
            }
            textInput(classes = "form-control") {
                id = GENERATED_FIELD_REPORT_GROUP
                name = "reportGroup"
            }
        }
        div(classes = "form-group") {
            label {
                htmlFor = GENERATED_FIELD_EXPRESSION
                +"Expression"
            }
            textInput(classes = "form-control") {
                id = GENERATED_FIELD_EXPRESSION
                name = "expression"
            }
        }
    }
    confirmModal(
        confirmModalId = GENERATED_FIELD_DELETE_MODAL,
        confirmMessage = "Are you sure you want to delete this generated Field?",
        resultFunction = "commitGeneratedFieldDelete()",
    )
    script {
        addParamsAsJsGlobalVariables(
            "generatedFieldsList" to JSElement(id = GENERATED_FIELDS_LIST),
            "generatedSourceTableSelect" to JSElement(id = GENERATED_SOURCE_TABLE_SELECT, makeJQuery = false),
            "generatedFieldDeleteModal" to JSElement(id = GENERATED_FIELD_DELETE_MODAL, makeElement = false),
            "editGeneratedField" to JSElement(id = EDIT_GENERATED_FIELD),
        )
    }
    script {
        src = "/assets/generated-fields.js"
    }
}

private const val RELATIONSHIPS_TABLE = "relationshipsTable"
private const val RELATIONSHIPS_MODAL = "relationshipsModal"
private const val SOURCE_TABLE_SELECTOR = "logicSourceTableSelector"
private const val PARENT_TABLE_SELECTOR = "parentTableSelector"
private const val LINKING_KEY_BUTTON_ROW = "linkingKeyButtonRow"

/** */
@Suppress("LongMethod")
fun FlowContent.relationships(runId: Long) {
    basicTable<PipelineRelationships>(
        tableId = RELATIONSHIPS_TABLE,
        dataUrl = "pipeline-relationships/${runId}",
        dataField = "payload",
        tableButtons = listOf(
            tableButton(
                name = "btnAddRelationship",
                icon = "fa-plus",
                event = "addRelationship()",
                text = "Add Relationship",
                title = "Add a relationship between a child and parent source table",
            )
        ),
        clickableRows = false,
        subTableDetails = subTableDetails<PipelineRelationshipFields>(
            url = "pipeline-relationship-fields/{id}",
            idField = "st_oid"
        )
    )
    basicModal(
        modalId = RELATIONSHIPS_MODAL,
        headerText = "Add Relationship",
        okClickFunction = "commitRelationship()",
        size = "modal-xl",
    ) {
        div(classes = "row py-1") {
            div(classes = "col") {
                label {
                    htmlFor = SOURCE_TABLE_SELECTOR
                    +"Source Table"
                }
                select(classes = "custom-select") {
                    id = SOURCE_TABLE_SELECTOR
                }
            }
            div(classes = "col") {
                label {
                    htmlFor = PARENT_TABLE_SELECTOR
                    +"Parent Table"
                }
                select(classes = "custom-select") {
                    id = PARENT_TABLE_SELECTOR
                }
            }
        }
        div(classes = "row py-1") {
            div(classes = "col-5") {
                label {
                    +"Linking Key"
                }
            }
            div(classes = "col-5") {
                label {
                    +"Parent Linking Key"
                }
            }
            div(classes = "col-2")
        }
        ol(classes = "list-group") {
            id = ""
        }
        div(classes = "row py-1") {
            id = LINKING_KEY_BUTTON_ROW
            div(classes = "col") {
                button(classes = "btn btn-secondary") {
                    type = ButtonType.button
                    onClick = "addLinkingKeyField()"
                    +"Add Linking Key Field"
                    i(classes = "fas fa-plus p-1")
                }
            }
        }
    }
    script {
        addParamsAsJsGlobalVariables(
            "relationshipsTable" to JSElement(id = RELATIONSHIPS_TABLE),
            "relationshipsModal" to JSElement(id = RELATIONSHIPS_MODAL),
            "sourceTableSelector" to JSElement(id = SOURCE_TABLE_SELECTOR, makeJQuery = false),
            "parentTableSelector" to JSElement(id = PARENT_TABLE_SELECTOR, makeJQuery = false),
            "linkingKeyButtonRow" to JSElement(id = LINKING_KEY_BUTTON_ROW, makeJQuery = false),
        )
    }
    script {
        src = "/assets/pipeline-relationships.js"
    }
}
