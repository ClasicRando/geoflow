package me.geoflow.web.pages

import kotlinx.html.ButtonType
import me.geoflow.core.database.tables.InternalUsers
import me.geoflow.core.web.html.addParamsAsJsGlobalVariables
import me.geoflow.core.web.html.formModal
import me.geoflow.core.web.html.tableButton
import kotlinx.html.FlowContent
import kotlinx.html.button
import kotlinx.html.checkBoxInput
import kotlinx.html.div
import kotlinx.html.i
import kotlinx.html.id
import kotlinx.html.label
import kotlinx.html.onClick
import kotlinx.html.passwordInput
import kotlinx.html.script
import kotlinx.html.select
import kotlinx.html.textInput
import me.geoflow.core.web.html.JSElement
import me.geoflow.core.web.html.basicTable

/**
 * Home for admin related tasks. Shows data for users, runs and the worker application
 */
object AdminDashboard : BasePage() {

    private const val USER_TABLE_ID = "users"
    private const val USER_EDIT_MODAL = "userEdit"
    private const val EDIT_USER_FORM_ID = "editUserForm"

    private val tableButtons = listOf(
        tableButton(
            name = "btnCreate",
            text = "Create New User",
            icon = "fa-plus",
            event = "openUserModal()",
            title = "Create new user for the application in a modal window",
        ),
    )

    override val content: FlowContent.() -> Unit = {
        div(classes = "row") {
            div(classes = "col") {
                basicTable<InternalUsers>(
                    tableId = USER_TABLE_ID,
                    dataUrl = "users",
                    dataField = "payload",
                    tableButtons = tableButtons,
                    clickableRows = false,
                )
                formModal(
                    modalId = USER_EDIT_MODAL,
                    headerText = "Create User",
                    okClickFunction = "submitEditUser(document.querySelector('#${EDIT_USER_FORM_ID}'))",
                ) {
                    id = EDIT_USER_FORM_ID
                    action = ""
                    div(classes = "form-group") {
                        label {
                            htmlFor = "fullName"
                            +"Full Name"
                        }
                        textInput(classes = "form-control") {
                            id = "fullName"
                            name = "fullName"
                        }
                    }
                    div(classes = "form-group") {
                        label {
                            htmlFor = "roles"
                            +"Roles"
                        }
                        select(classes = "custom-select required") {
                            id = "roles"
                            name = "roles"
                            multiple = true
                        }
                    }
                    div(classes = "form-group") {
                        checkBoxInput {
                            id = "isAdmin"
                            name = "isAdmin"
                            checked = false
                        }
                        label {
                            htmlFor = "isAdmin"
                            +"Is Admin?"
                        }
                    }
                    div(classes = "form-group") {
                        label {
                            htmlFor = "username"
                            +"Username"
                        }
                        textInput(classes = "form-control") {
                            id = "username"
                            name = "username"
                        }
                    }
                    div(classes = "form-group") {
                        label {
                            htmlFor = "password"
                            +"Password"
                        }
                        div(classes = "input-group") {
                            passwordInput(classes = "form-control") {
                                id = "password"
                                name = "password"
                            }
                            div(classes = "input-group-append") {
                                button(classes = "btn btn-outline-secondary") {
                                    onClick = "togglePasswordView(event,this)"
                                    type = ButtonType.button
                                    i(classes = "fas fa-eye-slash") {
                                        attributes["aria-hidden"] = "true"
                                    }
                                }
                            }
                        }
                    }
                    div(classes = "form-group") {
                        label {
                            htmlFor = "repeatPassword"
                            +"Repeat Password"
                        }
                        div(classes = "input-group") {
                            passwordInput(classes = "form-control") {
                                id = "repeatPassword"
                                name = "repeatPassword"
                            }
                            div(classes = "input-group-append") {
                                button(classes = "btn btn-outline-secondary") {
                                    onClick = "togglePasswordView(event,this)"
                                    type = ButtonType.button
                                    i(classes = "fas fa-eye-slash") {
                                        attributes["aria-hidden"] = "true"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override val script: suspend FlowContent.() -> Unit = {
        script {
            addParamsAsJsGlobalVariables(
                "userTable" to JSElement(id = USER_TABLE_ID, makeElement = false),
                "userModal" to JSElement(id = USER_EDIT_MODAL),
                "userForm" to JSElement(id = EDIT_USER_FORM_ID, makeJQuery = false),
            )
        }
        script {
            src = "/assets/admin-dashboard.js"
        }
    }

}
