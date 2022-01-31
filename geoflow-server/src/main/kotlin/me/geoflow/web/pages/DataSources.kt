package me.geoflow.web.pages

import io.ktor.application.ApplicationCall
import kotlinx.coroutines.runBlocking
import kotlinx.html.FlowContent
import kotlinx.html.STYLE
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h3
import kotlinx.html.id
import kotlinx.html.label
import kotlinx.html.onClick
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.select
import kotlinx.html.style
import kotlinx.html.textArea
import kotlinx.html.textInput
import me.geoflow.core.database.tables.DataSourceContacts
import me.geoflow.core.web.html.JSElement
import me.geoflow.web.api.NoBody
import me.geoflow.web.api.makeApiCall
import me.geoflow.core.web.html.addParamsAsJsGlobalVariables
import me.geoflow.core.web.html.basicModal
import me.geoflow.core.web.html.basicTable
import me.geoflow.core.web.html.confirmModal
import me.geoflow.core.web.html.formModal
import me.geoflow.core.web.html.tableButton
import me.geoflow.web.session
import me.geoflow.core.database.tables.DataSources as DataSourcesTable

/** */
class DataSources(call: ApplicationCall) : BasePage() {

    override val styles: STYLE.() -> Unit = {}

    override val content: FlowContent.() -> Unit = {
        val hasCreate = runBlocking {
            val session = call.session
            requireNotNull(session)
            val response = makeApiCall<NoBody, String>(
                endPoint = "/api/users/has-role/ds_create",
                apiToken = session.apiToken,
            )
            response.contains("payload")
        }
        val addButton = if (hasCreate) {
            tableButton(
                name = "btnAddDataSource",
                text = "Add Data Source",
                icon = "fa-plus",
                event = "newDataSource()",
                title = "Add new data source to be load",
            )
        } else null
        basicTable<DataSourcesTable>(
            tableId = TABLE_ID,
            dataUrl = "data-sources",
            dataField = "payload",
            clickableRows = false,
            tableButtons = listOfNotNull(
                addButton,
            ),
            freezeColumnsStart = 2,
            freezeColumnsEnd = 1,
        )
        basicModal(
            modalId = CONTACT_MODAL_ID,
            headerText = "View Contacts",
            size = "modal-xl",
        ) {
            basicTable<DataSourceContacts>(
                tableId = "contactTable",
                dataField = "payload",
                clickableRows = false,
                tableButtons = listOf(
                    tableButton(
                        name = "btnAddContact",
                        text = "Add Contact",
                        icon = "fa-plus",
                        event = "newContact()",
                        title = "Add new data source to be load",
                    )
                ),
            )
            form {
                id = CONTACT_FORM_ID
                action = ""
                style = "display: none;"
                h3 {}
                div(classes = "form-group") {
                    label {
                        htmlFor = "editName"
                        +"Full Name"
                    }
                    textInput(classes = "form-control") {
                        id = "editName"
                        name = "name"
                    }
                }
                div(classes = "form-group") {
                    label {
                        htmlFor = "editEmail"
                        +"Email"
                    }
                    textInput(classes = "form-control") {
                        id = "editEmail"
                        name = "email"
                    }
                }
                div(classes = "form-group") {
                    label {
                        htmlFor = "editWebsite"
                        +"Website"
                    }
                    textInput(classes = "form-control") {
                        id = "editWebsite"
                        name = "website"
                    }
                }
                div(classes = "form-group") {
                    label {
                        htmlFor = "editType"
                        +"Type"
                    }
                    textInput(classes = "form-control") {
                        id = "editType"
                        name = "type"
                    }
                }
                div(classes = "form-group") {
                    label {
                        htmlFor = "editNotes"
                        +"Notes"
                    }
                    textArea(classes = "form-control") {
                        id = "editNotes"
                        name = "notes"
                    }
                }
                p(classes = "invalidInput")
                div(classes = "form-row") {
                    div(classes = "col-1") {
                        button(classes = "btn btn-secondary") {
                            onClick = "exitContacts(event)"
                            +"Exit"
                        }
                    }
                    div(classes = "col-1") {
                        button(classes = "btn btn-secondary") {
                            onClick = "submitContact(event)"
                            +"Save"
                        }
                    }
                }
            }
        }
        formModal(
            modalId = SOURCE_MODAL_ID,
            headerText = "Edit Data Source",
            okClickFunction = "submitSource()",
            size = "modal-xl",
        ) {
            id = SOURCE_FORM_ID
            action = ""
            div(classes = "form-group") {
                div(classes = "form-row") {
                    div(classes = "col") {
                        label {
                            htmlFor = "editCode"
                            +"Source Code"
                        }
                        textInput(classes = "form-control") {
                            id = "editCode"
                            name = "code"
                        }
                    }
                    div(classes = "col") {
                        label {
                            htmlFor = "editRadius"
                            +"Search Radius"
                        }
                        textInput(classes = "form-control") {
                            id = "editRadius"
                            name = "radius"
                        }
                    }
                    div(classes = "col") {
                        label {
                            htmlFor = "editCollectionPipeline"
                            +"Collection Pipeline"
                        }
                        select(classes = "custom-select") {
                            id = "editCollectionPipeline"
                            name = "collectionPipeline"
                        }
                    }
                }
            }
            div(classes = "form-group") {
                div(classes = "form-row") {
                    div(classes = "col") {
                        label {
                            htmlFor = "editCountry"
                            +"Country"
                        }
                        select(classes = "custom-select") {
                            id = "editCountry"
                            name = "country"
                        }
                    }
                    div(classes = "col") {
                        label {
                            htmlFor = "editWarehouseType"
                            +"Warehouse Type"
                        }
                        select(classes = "custom-select") {
                            id = "editWarehouseType"
                            name = "warehouseType"
                        }
                    }
                    div(classes = "col") {
                        label {
                            htmlFor = "editLoadPipeline"
                            +"Load Pipeline"
                        }
                        select(classes = "custom-select") {
                            id = "editLoadPipeline"
                            name = "loadPipeline"
                        }
                    }
                }
            }
            div(classes = "form-group") {
                div(classes = "form-row") {
                    div(classes = "col") {
                        label {
                            htmlFor = "editProv"
                            +"Prov"
                        }
                        select(classes = "custom-select") {
                            id = "editProv"
                            name = "prov"
                        }
                    }
                    div(classes = "col-4") {
                        label {
                            htmlFor = "editReportType"
                            +"Report Type"
                        }
                        textInput(classes = "form-control") {
                            id = "editReportType"
                            name = "reportType"
                        }
                    }
                    div(classes = "col") {
                        label {
                            htmlFor = "editCheckPipeline"
                            +"Check Pipeline"
                        }
                        select(classes = "custom-select") {
                            id = "editCheckPipeline"
                            name = "checkPipeline"
                        }
                    }
                }
            }
            div(classes = "form-group") {
                div(classes = "form-row") {
                    div(classes = "col") {
                        label {
                            htmlFor = "editFileLocation"
                            +"Files Location"
                        }
                        textInput(classes = "form-control") {
                            id = "editFileLocation"
                            name = "fileLocation"
                        }
                    }
                    div(classes = "col") {
                        label {
                            htmlFor = "editAssignedUser"
                            +"Assigned User"
                        }
                        select(classes = "custom-select") {
                            id = "editAssignedUser"
                            name = "assignedUser"
                        }
                    }
                    div(classes = "col") {
                        label {
                            htmlFor = "editQaPipeline"
                            +"QA Pipeline"
                        }
                        select(classes = "custom-select") {
                            id = "editQaPipeline"
                            name = "qaPipeline"
                        }
                    }
                }
            }
            div(classes = "form-group") {
                label {
                    htmlFor = "editDescription"
                    +"Description"
                }
                textArea(classes = "form-control") {
                    id = "editDescription"
                    name = "description"
                }
            }
            div(classes = "form-group") {
                label {
                    htmlFor = "editComments"
                    +"Comments"
                }
                textArea(classes = "form-control") {
                    id = "editComments"
                    name = "comments"
                }
            }
        }
        confirmModal(
            confirmModalId = CONFIRM_DELETE_CONTACT_MODAL_ID,
            confirmMessage = "Are you sure you want to delete the contact?",
            resultFunction = "deleteContact()",
        )
    }

    override val script: suspend FlowContent.() -> Unit = {
        val session = call.session
        requireNotNull(session)
        val collectionUsers = makeApiCall<NoBody, String>(
            endPoint = "/api/users/collection",
            apiToken = session.apiToken,
        ).takeIf { it.contains("payload") } ?: DEFAULT_RESPONSE
        val provs = makeApiCall<NoBody, String>(
            endPoint = "/api/provs",
            apiToken = session.apiToken,
        ).takeIf { it.contains("payload") } ?: DEFAULT_RESPONSE
        val warehouseTypes = makeApiCall<NoBody, String>(
            endPoint = "/api/rec-warehouse-types",
            apiToken = session.apiToken,
        ).takeIf { it.contains("payload") } ?: DEFAULT_RESPONSE
        val pipelines = makeApiCall<NoBody, String>(
            endPoint = "/api/pipelines",
            apiToken = session.apiToken,
        ).takeIf { it.contains("payload") } ?: DEFAULT_RESPONSE
        script {
            addParamsAsJsGlobalVariables(
                "dataSourceTable" to JSElement(query = "#$TABLE_ID", makeSelector = false),
                "contactModal" to JSElement(query = "#$CONTACT_MODAL_ID"),
                "contactForm" to JSElement(query = "#$CONTACT_FORM_ID", makeJQuery = false),
                "sourceModal" to JSElement(query = "#$SOURCE_MODAL_ID"),
                "sourceForm" to JSElement(query = "#$SOURCE_FORM_ID", makeJQuery = false),
                "collectionUsersJson" to collectionUsers,
                "provsJson" to provs,
                "warehouseTypesJson" to warehouseTypes,
                "pipelinesJson" to pipelines,
                "confirmDeleteContact" to JSElement(query = "#$CONFIRM_DELETE_CONTACT_MODAL_ID", makeSelector = false),
                "contactTable" to JSElement(query = "#$CONTACT_TABLE_ID", makeSelector = false),
            )
        }
        script {
            src = "/assets/data-sources.js"
        }
    }

    companion object {
        private const val TABLE_ID = "dataSources"
        private const val CONTACT_TABLE_ID = "contactTable"
        private const val CONTACT_MODAL_ID = "contactModal"
        private const val CONTACT_FORM_ID = "contactForm"
        private const val SOURCE_MODAL_ID = "sourceModal"
        private const val SOURCE_FORM_ID = "sourceForm"
        private const val CONFIRM_DELETE_CONTACT_MODAL_ID = "confirmDeleteContact"
        private const val DEFAULT_RESPONSE = "{\"payload\": []}"
    }

}
