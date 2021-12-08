package me.geoflow.web.pages

import me.geoflow.core.database.Database
import me.geoflow.core.database.tables.InternalUsers
import me.geoflow.core.database.tables.Roles
import me.geoflow.web.html.addParamsAsJsGlobalVariables
import me.geoflow.web.html.basicTable
import me.geoflow.web.html.formModal
import me.geoflow.web.html.tableButton
import kotlinx.html.FlowContent
import kotlinx.html.STYLE
import kotlinx.html.checkBoxInput
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.label
import kotlinx.html.option
import kotlinx.html.passwordInput
import kotlinx.html.script
import kotlinx.html.select
import kotlinx.html.textInput

/**
 * Home for admin related tasks. Shows data for users, runs and the worker application
 */
object AdminDashboard : BasePage() {

    private const val USER_TABLE_ID = "users"
    private const val USER_CREATE_MODAL = "userCreate"
    private const val USER_EDIT_MODAL = "userEdit"
    private const val IS_ADMIN = "isAdmin"
    private const val ROLES_SELECT = "roles"
    private const val CREATE_USER_FORM_ID = "createUserForm"
    private const val EDIT_USER_FORM_ID = "editUserForm"

    private val tableButtons = listOf(
        tableButton(
            name = "btnCreate",
            text = "Create New User",
            icon = "fa-plus",
            event = "openNewUserModal()",
            title = "Create new user for the application in a modal window",
        ),
    )

    override val styles: STYLE.() -> Unit = {}

    override val content: FlowContent.() -> Unit = {
        div(classes = "row") {
            div(classes = "col") {
                basicTable(
                    tableId = USER_TABLE_ID,
                    dataUrl = "users",
                    fields = InternalUsers.tableDisplayFields,
                    dataField = "payload",
                    tableButtons = tableButtons,
                    clickableRows = false,
                )
                formModal(
                    modalId = USER_CREATE_MODAL,
                    headerText = "Create User",
                    okClickFunction = "createUser",
                ) {
                    id = CREATE_USER_FORM_ID
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
                            htmlFor = ROLES_SELECT
                            +"Roles"
                        }
                        select(classes = "custom-select required") {
                            id = ROLES_SELECT
                            name = ROLES_SELECT
                            multiple = true
                            for (role in Database.runWithConnectionBlocking { Roles.getRecords(it) }) {
                                if (role.name == "admin") {
                                    continue
                                }
                                option {
                                    value = role.name
                                    +role.description
                                }
                            }
                        }
                    }
                    div(classes = "form-group") {
                        checkBoxInput {
                            id = IS_ADMIN
                            name = IS_ADMIN
                            checked = false
                        }
                        label {
                            htmlFor = IS_ADMIN
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
                        passwordInput(classes = "form-control") {
                            id = "password"
                            name = "password"
                        }
                    }
                    div(classes = "form-group") {
                        label {
                            htmlFor = "repeatPassword"
                            +"Repeat Password"
                        }
                        passwordInput(classes = "form-control") {
                            id = "repeatPassword"
                            name = "repeatPassword"
                        }
                    }
                }
                formModal(
                    modalId = USER_EDIT_MODAL,
                    headerText = "Edit User",
                    okClickFunction = "editUser",
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
                            htmlFor = ROLES_SELECT
                            +"Roles"
                        }
                        select(classes = "custom-select required") {
                            id = ROLES_SELECT
                            name = ROLES_SELECT
                            multiple = true
                            for (role in Database.runWithConnectionBlocking { Roles.getRecords(it) }) {
                                if (role.name == "admin") {
                                    continue
                                }
                                option {
                                    value = role.name
                                    +role.description
                                }
                            }
                        }
                    }
                    div(classes = "form-group") {
                        checkBoxInput {
                            id = IS_ADMIN
                            name = IS_ADMIN
                        }
                        label {
                            htmlFor = IS_ADMIN
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
                }
            }
        }
    }

    override val script: FlowContent.() -> Unit = {
        script {
            addParamsAsJsGlobalVariables(
                "userTable" to USER_TABLE_ID,
                "userCreateModal" to USER_CREATE_MODAL,
                "userCreateForm" to CREATE_USER_FORM_ID,
                "userEditModal" to USER_EDIT_MODAL,
                "userEditForm" to EDIT_USER_FORM_ID,
            )
        }
        script {
            src = "/assets/admin-dashboard.js"
        }
    }

}
