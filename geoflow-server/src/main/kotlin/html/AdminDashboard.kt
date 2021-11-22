package html

import database.Database
import database.tables.InternalUsers
import database.tables.Roles
import io.ktor.html.Template
import kotlinx.html.HTML
import kotlinx.html.checkBoxInput
import kotlinx.html.div
import kotlinx.html.h3
import kotlinx.html.id
import kotlinx.html.label
import kotlinx.html.option
import kotlinx.html.passwordInput
import kotlinx.html.script
import kotlinx.html.select
import kotlinx.html.textInput

/**
 *
 */
object AdminDashboard {
    private const val USER_TABLE_ID = "users"
    private const val USER_CREATE_MODAL = "userCreate"
    private const val IS_ADMIN = "isAdmin"
    private const val ROLES_SELECT = "roles"
    private const val CREATE_USER_FORM_ID = "createUserForm"

    private val tableButtons = listOf(
        TableButton(
            "btnCreate",
            "Create New User",
            "fa-plus",
            "openNewUserModal()",
            "Create new user for the application in a modal window",
        ),
    )

    /** */
    val page: Template<HTML> = BasePage.withContent {
        div(classes = "row") {
            div(classes = "col") {
                basicTable(
                    tableId = USER_TABLE_ID,
                    dataUrl = "/api/users",
                    fields = InternalUsers.tableDisplayFields,
                    tableButtons = tableButtons,
                )
                formModal(
                    modalId = USER_CREATE_MODAL,
                    headerText = "Create User",
                    okClickFunction = "postCreateUser",
                ) {
                    id = CREATE_USER_FORM_ID
                    action = ""
                    h3 {
                        +"Create New User"
                    }
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
            }
        }
        messageBoxModal()
    }.withScript {
        script {
            addParamsAsJsGlobalVariables(
                mapOf(
                    "userCreateModal" to USER_CREATE_MODAL,
                    "isAdmin" to IS_ADMIN,
                    "rolesSelect" to ROLES_SELECT,
                    "createUserForm" to CREATE_USER_FORM_ID,
                )
            )
        }
        script {
            src = "/assets/admin-dashboard.js"
        }
    }
}
