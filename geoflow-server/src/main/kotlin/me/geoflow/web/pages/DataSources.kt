package me.geoflow.web.pages

import io.ktor.application.ApplicationCall
import kotlinx.coroutines.runBlocking
import kotlinx.html.FlowContent
import kotlinx.html.STYLE
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.label
import kotlinx.html.script
import kotlinx.html.select
import kotlinx.html.textArea
import kotlinx.html.textInput
import me.geoflow.core.database.tables.DataSourceContacts
import me.geoflow.web.api.NoBody
import me.geoflow.web.api.makeApiCall
import me.geoflow.web.html.addParamsAsJsGlobalVariables
import me.geoflow.web.html.basicTable
import me.geoflow.web.html.confirmModal
import me.geoflow.web.html.formModal
import me.geoflow.web.html.subTableDetails
import me.geoflow.web.html.tableButton
import me.geoflow.web.session
import me.geoflow.web.utils.Quad
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
            subTableDetails = subTableDetails<DataSourceContacts>(
                url = "/data-source-contacts/{id}",
                idField = "ds_id",
            ),
            tableButtons = listOfNotNull(
                addButton,
            ),
        )
        formModal(
            modalId = CREATE_CONTACT_MODAL_ID,
            headerText = "Create Contact",
            okClickFunction = "postContact($('#${CREATE_CONTACT_FORM_ID}'))",
        ) {
            id = CREATE_CONTACT_FORM_ID
            action = ""
            div(classes = "form-group") {
                label {
                    htmlFor = "createName"
                    +"Full Name"
                }
                textInput(classes = "form-control") {
                    id = "createName"
                    name = "name"
                }
            }
            div(classes = "form-group") {
                label {
                    htmlFor = "createEmail"
                    +"Email"
                }
                textInput(classes = "form-control") {
                    id = "createEmail"
                    name = "email"
                }
            }
            div(classes = "form-group") {
                label {
                    htmlFor = "createWebsite"
                    +"Website"
                }
                textInput(classes = "form-control") {
                    id = "createWebsite"
                    name = "website"
                }
            }
            div(classes = "form-group") {
                label {
                    htmlFor = "createType"
                    +"Type"
                }
                textInput(classes = "form-control") {
                    id = "createType"
                    name = "type"
                }
            }
            div(classes = "form-group") {
                label {
                    htmlFor = "createNotes"
                    +"Notes"
                }
                textArea(classes = "form-control") {
                    id = "createNotes"
                    name = "notes"
                }
            }
        }
        formModal(
            modalId = EDIT_CONTACT_MODAL_ID,
            headerText = "Edit Contact",
            okClickFunction = "putContact($('#${EDIT_CONTACT_FORM_ID}'))",
        ) {
            id = EDIT_CONTACT_FORM_ID
            action = ""
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
        }
        formModal(
            modalId = CREATE_SOURCE_MODAL_ID,
            headerText = "Create Data Source",
            okClickFunction = "postDataSource($('#${CREATE_SOURCE_FORM_ID}'))",
            size = "modal-xl",
        ) {
            id = CREATE_SOURCE_FORM_ID
            action = ""
            div(classes = "form-group") {
                div(classes = "form-row") {
                    div(classes = "col") {
                        label {
                            htmlFor = "createCode"
                            +"Source Code"
                        }
                        textInput(classes = "form-control") {
                            id = "createCode"
                            name = "code"
                        }
                    }
                    div(classes = "col") {
                        label {
                            htmlFor = "createRadius"
                            +"Search Radius"
                        }
                        textInput(classes = "form-control") {
                            id = "createRadius"
                            name = "radius"
                        }
                    }
                    div(classes = "col") {
                        label {
                            htmlFor = "createCollectionPipeline"
                            +"Collection Pipeline"
                        }
                        select(classes = "custom-select") {
                            id = "createCollectionPipeline"
                            name = "collectionPipeline"
                        }
                    }
                }
            }
            div(classes = "form-group") {
                div(classes = "form-row") {
                    div(classes = "col") {
                        label {
                            htmlFor = "createCountry"
                            +"Country"
                        }
                        select(classes = "custom-select") {
                            id = "createCountry"
                            name = "country"
                        }
                    }
                    div(classes = "col") {
                        label {
                            htmlFor = "createWarehouseType"
                            +"Warehouse Type"
                        }
                        select(classes = "custom-select") {
                            id = "createWarehouseType"
                            name = "warehouseType"
                        }
                    }
                    div(classes = "col") {
                        label {
                            htmlFor = "createLoadPipeline"
                            +"Load Pipeline"
                        }
                        select(classes = "custom-select") {
                            id = "createLoadPipeline"
                            name = "loadPipeline"
                        }
                    }
                }
            }
            div(classes = "form-group") {
                div(classes = "form-row") {
                    div(classes = "col") {
                        label {
                            htmlFor = "createProv"
                            +"Prov"
                        }
                        select(classes = "custom-select") {
                            id = "createProv"
                            name = "prov"
                        }
                    }
                    div(classes = "col-4") {
                        label {
                            htmlFor = "createReportType"
                            +"Report Type"
                        }
                        textInput(classes = "form-control") {
                            id = "createReportType"
                            name = "reportType"
                        }
                    }
                    div(classes = "col") {
                        label {
                            htmlFor = "createCheckPipeline"
                            +"Check Pipeline"
                        }
                        select(classes = "custom-select") {
                            id = "createCheckPipeline"
                            name = "checkPipeline"
                        }
                    }
                }
            }
            div(classes = "form-group") {
                div(classes = "form-row") {
                    div(classes = "col") {
                        label {
                            htmlFor = "createFileLocation"
                            +"Files Location"
                        }
                        textInput(classes = "form-control") {
                            id = "createFileLocation"
                            name = "fileLocation"
                        }
                    }
                    div(classes = "col") {
                        label {
                            htmlFor = "createAssignedUser"
                            +"Assigned User"
                        }
                        select(classes = "custom-select") {
                            id = "createAssignedUser"
                            name = "assignedUser"
                        }
                    }
                    div(classes = "col") {
                        label {
                            htmlFor = "createQaPipeline"
                            +"QA Pipeline"
                        }
                        select(classes = "custom-select") {
                            id = "createQaPipeline"
                            name = "qaPipeline"
                        }
                    }
                }
            }
            div(classes = "form-group") {
                label {
                    htmlFor = "createDescription"
                    +"Description"
                }
                textArea(classes = "form-control") {
                    id = "createDescription"
                    name = "description"
                }
            }
            div(classes = "form-group") {
                label {
                    htmlFor = "createComments"
                    +"Comments"
                }
                textArea(classes = "form-control") {
                    id = "createComments"
                    name = "comments"
                }
            }
        }
        formModal(
            modalId = EDIT_SOURCE_MODAL_ID,
            headerText = "Edit Data Source",
            okClickFunction = "patchDataSource($('#${EDIT_SOURCE_FORM_ID}'))",
            size = "modal-xl",
        ) {
            id = EDIT_SOURCE_FORM_ID
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

    override val script: FlowContent.() -> Unit = {
        val (collectionUsers, provs, warehouseTypes, pipelines) = runBlocking {
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
            Quad(collectionUsers, provs, warehouseTypes, pipelines)
        }
        script {
            addParamsAsJsGlobalVariables(
                "dataSourceTableId" to TABLE_ID,
                "createContactModalId" to CREATE_CONTACT_MODAL_ID,
                "createContactFormId" to CREATE_CONTACT_FORM_ID,
                "editContactModalId" to EDIT_CONTACT_MODAL_ID,
                "editContactFormId" to EDIT_CONTACT_FORM_ID,
                "createSourceModalId" to CREATE_SOURCE_MODAL_ID,
                "createSourceFormId" to CREATE_SOURCE_FORM_ID,
                "editSourceModalId" to EDIT_SOURCE_MODAL_ID,
                "editSourceFormId" to EDIT_SOURCE_FORM_ID,
                "collectionUsersJson" to collectionUsers,
                "provsJson" to provs,
                "warehouseTypesJson" to warehouseTypes,
                "pipelinesJson" to pipelines,
                "confirmDeleteContactId" to CONFIRM_DELETE_CONTACT_MODAL_ID,
            )
        }
        script {
            src = "/assets/data-sources.js"
        }
    }

    companion object {
        private const val TABLE_ID = "dataSources"
        private const val CREATE_CONTACT_MODAL_ID = "createContactModal"
        private const val CREATE_CONTACT_FORM_ID = "createContactForm"
        private const val EDIT_CONTACT_MODAL_ID = "editContactModal"
        private const val EDIT_CONTACT_FORM_ID = "editContactForm"
        private const val CREATE_SOURCE_MODAL_ID = "createSourceModal"
        private const val CREATE_SOURCE_FORM_ID = "createSourceForm"
        private const val EDIT_SOURCE_MODAL_ID = "editSourceModal"
        private const val EDIT_SOURCE_FORM_ID = "editSourceForm"
        private const val CONFIRM_DELETE_CONTACT_MODAL_ID = "confirmDeleteContact"
        private const val DEFAULT_RESPONSE = "{\"payload\": []}"
    }

}
