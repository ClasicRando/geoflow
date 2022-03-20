package me.geoflow.web.html

import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.i
import kotlinx.html.id
import kotlinx.html.label
import kotlinx.html.onClick
import kotlinx.html.script
import kotlinx.html.select
import kotlinx.html.textInput
import me.geoflow.core.database.tables.GeneratedTableColumns
import me.geoflow.core.web.html.JSElement
import me.geoflow.core.web.html.addParamsAsJsGlobalVariables
import me.geoflow.core.web.html.basicTable
import me.geoflow.core.web.html.confirmModal
import me.geoflow.core.web.html.formModal
import me.geoflow.core.web.html.tableButton

private const val SOURCE_TABLE_SELECTOR = "logicSourceTableSelector"
private const val PARENT_TABLE_SELECTOR = "parentTableSelector"
private const val LOGIC_FORM = "logicForm"
private const val SOURCE_FIELDS_LIST = "sourceFieldsList"
private const val GENERATED_FIELDS_LIST = "generatedFieldsList"
private const val PARENT_LINKING_KEY = "parentLinkingKey"
private const val PARENT_LINKING_KEY_ROW = "parentLinkingKeyRow"
private const val LINKING_KEY = "linkingKey"
private const val LINKING_KEY_BUTTON_ROW = "linkingKeyButtonRow"
private const val EDIT_SOURCE_FIELD = "editSourceField"
private const val EDIT_GENERATED_FIELD = "editGeneratedField"
private const val SOURCE_FIELD_NAME = "sourceFieldName"
private const val SOURCE_FIELD_LABEL = "sourceFieldLabel"
private const val SOURCE_FIELD_REPORT_GROUP = "sourceFieldReportGroup"
private const val GENERATED_FIELD_NAME = "generatedFieldName"
private const val GENERATED_FIELD_LABEL = "generatedFieldLabel"
private const val GENERATED_FIELD_EXPRESSION = "generatedFieldExpression"
private const val GENERATED_FIELD_REPORT_GROUP = "generatedFieldReportGroup"
private const val GENERATED_FIELD_DELETE_MODAL = "generatedFieldDeleteModal"

/** */
@Suppress("LongMethod")
fun FlowContent.loadingLogic() {
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
    }
    div(classes = "row py-1") {
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
    div(classes = "row py-1 hidden") {
        id = PARENT_LINKING_KEY_ROW
        div(classes = "col") {
            label {
                htmlFor = PARENT_LINKING_KEY
                +"Parent Linking Key"
            }
            select(classes = "custom-select") {
                id = PARENT_LINKING_KEY
            }
        }
        div(classes = "col") {
            label {
                htmlFor = LINKING_KEY
                +"Linking Key"
            }
            select(classes = "custom-select") {
                id = LINKING_KEY
            }
        }
    }
    div(classes = "row py-1 hidden") {
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
    div(classes = "row py-1") {
        div(classes = "col") {
            label {
                htmlFor = SOURCE_FIELDS_LIST
                +"Source Fields"
            }
        }
        div(classes = "col") {
            label {
                htmlFor = GENERATED_FIELDS_LIST
                +"Generated Fields"
            }
        }
    }
    div(classes = "row pb-1") {
        div(classes = "col") {
            basicTable(
                tableId = SOURCE_FIELDS_LIST,
                fields = mapOf(
                    "name" to mapOf("title" to "Name"),
                    "label" to mapOf("title" to "Label"),
                    "report_group" to mapOf("title" to "Report Group"),
                    "action" to mapOf("formatter" to "sourceFieldListEdit"),
                ),
                dataField = "payload",
                clickableRows = false,
            )
        }
        div(classes = "col") {
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
            "sourceTableSelector" to JSElement(id = SOURCE_TABLE_SELECTOR, makeJQuery = false),
            "logicForm" to JSElement(id = LOGIC_FORM, makeJQuery = false),
            "parentTableSelector" to JSElement(id = PARENT_TABLE_SELECTOR, makeJQuery = false),
            "sourceFieldsList" to JSElement(id = SOURCE_FIELDS_LIST),
            "generatedFieldsList" to JSElement(id = GENERATED_FIELDS_LIST),
            "parentLinkingKey" to JSElement(id = PARENT_LINKING_KEY, makeJQuery = false),
            "parentLinkingKeyRow" to JSElement(id = PARENT_LINKING_KEY_ROW, makeJQuery = false),
            "linkingKey" to JSElement(id = LINKING_KEY, makeJQuery = false),
            "linkingKeyButtonRow" to JSElement(id = LINKING_KEY_BUTTON_ROW, makeJQuery = false),
            "editSourceField" to JSElement(id = EDIT_SOURCE_FIELD),
            "editGeneratedField" to JSElement(id = EDIT_GENERATED_FIELD),
            "generatedFieldDeleteModal" to JSElement(id = GENERATED_FIELD_DELETE_MODAL, makeElement = false),
        )
    }
    script {
        src = "/assets/loading-logic.js"
    }
}
