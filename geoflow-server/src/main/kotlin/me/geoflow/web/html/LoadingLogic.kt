package me.geoflow.web.html

import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.i
import kotlinx.html.id
import kotlinx.html.label
import kotlinx.html.onClick
import kotlinx.html.script
import kotlinx.html.select
import me.geoflow.core.web.html.JSElement
import me.geoflow.core.web.html.addParamsAsJsGlobalVariables
import me.geoflow.core.web.html.basicTable
import me.geoflow.core.web.html.tableButton

private const val SOURCE_TABLE_SELECTOR = "logicSourceTableSelector"
private const val PARENT_TABLE_SELECTOR = "parentTableSelector"
private const val LOGIC_FORM = "logicForm"
private const val SOURCE_FIELDS_LIST = "sourceFieldsList"
private const val GENERATED_FIELDS_LIST = "generatedFieldsList"
private const val PARENT_LINKING_KEY = "parentLinkingKey"
private const val PARENT_LINKING_KEY_ROW = "parentLinkingKeyRow"
private const val LINKING_KEY = "linkingKey"

/** */
@Suppress("LongMethod")
fun FlowContent.loadingLogic() {
    form {
        id = LOGIC_FORM
        action = ""
        div(classes = "form-row") {
            label {
                htmlFor = SOURCE_TABLE_SELECTOR
                +"Source Table"
            }
            select(classes = "custom-select") {
                id = SOURCE_TABLE_SELECTOR
            }
        }
        div(classes = "form-row") {
            label {
                htmlFor = PARENT_TABLE_SELECTOR
                +"Parent Table"
            }
            select(classes = "custom-select") {
                id = PARENT_TABLE_SELECTOR
            }
        }
        div(classes = "form-row hidden") {
            id = PARENT_LINKING_KEY_ROW
            div(classes = "col") {
                label {
                    htmlFor = PARENT_LINKING_KEY
                    +"Parent Linking Key"
                }
                select(classes = "custom-select") {
                    id = PARENT_LINKING_KEY
                }
                button(classes = "btn btn-secondary") {
                    type = ButtonType.button
                    onClick = "addParentKeyField()"
                    +"Add Parent Key Field"
                    i(classes = "fas fa-plus p-1")
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
                button(classes = "btn btn-secondary") {
                    type = ButtonType.button
                    onClick = "addLinkingKeyField()"
                    +"Add Linking Key Field"
                    i(classes = "fas fa-plus p-1")
                }
            }
        }
        div(classes = "form-row") {
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
        div(classes = "form-row") {
//            ol(classes = "list-group") {
//                id = SOURCE_FIELDS_LIST
//            }
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
                basicTable(
                    tableId = GENERATED_FIELDS_LIST,
                    fields = mapOf(
                        "name" to mapOf("title" to "Name"),
                        "label" to mapOf("title" to "Label"),
                        "expression" to mapOf("title" to "Expression"),
                    ),
                    dataField = "payload",
                    tableButtons = listOf(
                        tableButton(
                            name = "btnAddGeneratedField",
                            icon = "fa-plus",
                            event = "addGeneratedField",
                            text = "Add Generated Field",
                            title = "Add an expression to be created while loading the data into the pipeline",
                        )
                    ),
                    clickableRows = false,
                )
            }
        }
//        button(classes = "btn btn-secondary") {
//            type = ButtonType.button
//            onClick = "addGeneratedField()"
//            +"Add Generated Field"
//            i(classes = "fas fa-plus p-1")
//        }
//        div(classes = "form-row") {
//            label {
//                htmlFor = GENERATED_FIELDS_LIST
//                +"Generated Fields"
//            }
//        }
//        div(classes = "form-row") {
//            ol(classes = "list-group") {
//                id = GENERATED_FIELDS_LIST
//            }
//        }
    }
    script {
        addParamsAsJsGlobalVariables(
            "sourceTableSelector" to JSElement(id = SOURCE_TABLE_SELECTOR, makeJQuery = false),
            "logicForm" to JSElement(id = LOGIC_FORM, makeJQuery = false),
            "parentTableSelector" to JSElement(id = PARENT_TABLE_SELECTOR, makeJQuery = false),
            "sourceFieldsList" to JSElement(id = SOURCE_FIELDS_LIST),
            "generatedFieldsList" to JSElement(id = GENERATED_FIELDS_LIST, makeJQuery = false),
            "parentLinkingKey" to JSElement(id = PARENT_LINKING_KEY, makeJQuery = false),
            "parentLinkingKeyRow" to JSElement(id = PARENT_LINKING_KEY_ROW, makeJQuery = false),
            "linkingKey" to JSElement(id = LINKING_KEY, makeJQuery = false),
        )
    }
    script {
        src = "/assets/loading-logic.js"
    }
}
