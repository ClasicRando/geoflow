package html

import database.Database
import database.tables.Roles
import io.ktor.html.*
import kotlinx.html.*

/** Page for creating a new user and editing an existing user */
object CreateEditUser {

    private const val isAdmin = "isAdmin"
    private const val rolesSelect = "roles"

    fun createUser(): Template<HTML> {
        return BasePage.withContent {
            form {
                action = ""
                method = FormMethod.post
                style = "width: 600px"
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
                        required = true
                    }
                }
                div(classes = "form-group") {
                    label {
                        htmlFor = rolesSelect
                        +"Roles"
                    }
                    select(classes = "custom-select") {
                        id = rolesSelect
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
                        id = isAdmin
                        checked = false
                    }
                    label {
                        htmlFor = isAdmin
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
                        required = true
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
                        required = true
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
                        required = true
                    }
                }
            }
        }.withScript {
            script {
                addParamsAsJsGlobalVariables(
                    mapOf(
                        ::isAdmin.name to isAdmin,
                        ::rolesSelect.name to rolesSelect,
                    )
                )
            }
            script {
                src = "/assets/create-edit-user.js"
            }
        }
    }
}