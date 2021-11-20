package html

import database.Database
import database.tables.Roles
import io.ktor.html.Template
import kotlinx.html.FormMethod
import kotlinx.html.HTML
import kotlinx.html.checkBoxInput
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h3
import kotlinx.html.id
import kotlinx.html.label
import kotlinx.html.option
import kotlinx.html.passwordInput
import kotlinx.html.script
import kotlinx.html.select
import kotlinx.html.style
import kotlinx.html.submitInput
import kotlinx.html.textInput

/** Page for creating a new user and editing an existing user */
object CreateEditUser {

    private const val isAdmin = "isAdmin"
    private const val rolesSelect = "roles"

    /** Creates a [BasePage] with a form in the content section to collect information on the new user */
    @Suppress("LongMethod")
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
                        name = rolesSelect
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
                        name = isAdmin
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
                submitInput(classes = "btn btn-primary") {
                    value = "Create"
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
