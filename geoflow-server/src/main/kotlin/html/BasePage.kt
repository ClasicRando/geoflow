package html

import io.ktor.html.*
import kotlinx.html.*

class BasePage: Template<HTML> {

    val content = Placeholder<FlowContent>()
    val script = Placeholder<FlowContent>()
    override fun HTML.apply() {
        head {
            link {
                rel = "stylesheet"
                href = "https://unpkg.com/bootstrap-table@1.18.3/dist/bootstrap-table.min.css"
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
            div {
                +"GeoFlow"
                a {
                    href = "/index"
                    +"Home"
                }
            }
            hr()
            insert(content)
            insert(script)
            script {
                src = "https://unpkg.com/bootstrap-table@1.18.3/dist/bootstrap-table.min.js"
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
}