package html

import kotlinx.html.*

fun HTML.index() {
    with(Index()) {
        apply()
    }
}

fun HTML.pipelineStatus(workflowCode: String) {
    with(PipelineStatus(workflowCode)) {
        apply()
    }
}

fun HTML.pipelineTasks(runId: Long) {
    with(PipelineTasks(runId)) {
        apply()
    }
}

fun HTML.errorPage(message: String) {
    with(BasePage()) {
        setContent {
            +message
        }
        apply()
    }
}

fun HTML.login(message: String) {
    lang = "en-US"
    head {
        title("GeoFlow")
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
    }
    body {
        form(classes = "mx-auto") {
            action = ""
            method = FormMethod.post
            style = "width: 400px"
            h3 {
                +"Login to GeoFlow"
            }
            if (message.isNotEmpty()) {
                p {
                    +message
                }
            }
            div("form-group") {
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
            div("form-group") {
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
            submitInput(classes = "btn btn-primary") {
                value = "Submit"
            }
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
    }
}