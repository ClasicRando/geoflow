package me.geoflow.web.pages

import kotlinx.html.ButtonType
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h3
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.label
import kotlinx.html.lang
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.p
import kotlinx.html.passwordInput
import kotlinx.html.script
import kotlinx.html.style
import kotlinx.html.textInput
import kotlinx.html.title

/** Simple login form page with an optional message if session expired or past login attempt was unsuccessful */
@Suppress("LongMethod")
fun HTML.login(message: String = "") {
    lang = "en-US"
    head {
        title(content = "GeoFlow")
        meta {
            charset = "utf-8"
        }
        meta {
            name = "author"
            content = "ClasicRando"
        }
        link {
            rel = "stylesheet"
            href = "https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css"
            integrity = "sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T"
            attributes["crossorigin"] = "anonymous"
        }
        link {
            rel = "stylesheet"
            href = "https://use.fontawesome.com/releases/v5.6.3/css/all.css"
            integrity = "sha384-UHRtZLI+pbxtHCWp1t77Bi1L4ZtiqrqD80Kn4Z8NTSRyMA2Fd33n5dQ8lWUE00s/"
            attributes["crossorigin"] = "anonymous"
        }
        script {
            src = "https://cdn.jsdelivr.net/npm/jquery/dist/jquery.min.js"
        }
        script {
            src = "https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js"
        }
        script {
            src = "https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js"
            integrity = "sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM"
            attributes["crossorigin"] = "anonymous"
        }
        script {
            src = "https://cdn.jsdelivr.net/npm/jquery-validation@1.19.3/dist/jquery.validate.min.js"
        }
        script {
            src = "/assets/utils.js"
        }
        script {
            src = "/assets/login.js"
        }
    }
    body {
        form(classes = "mx-auto") {
            id = "loginForm"
            action = ""
            style = "width: 400px"
            h3 {
                +"Login to GeoFlow"
            }
            p {
                id = "message"
                +message
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
            button(classes = "btn btn-primary") {
                id = "submit"
                type = ButtonType.button
                +"Submit"
            }
        }
    }
}
