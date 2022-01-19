package me.geoflow.web.pages

import io.ktor.html.Placeholder
import io.ktor.html.Template
import io.ktor.html.insert
import kotlinx.html.FlowContent
import kotlinx.html.HTML
import kotlinx.html.STYLE
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.lang
import kotlinx.html.li
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.nav
import kotlinx.html.script
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.ul
import kotlinx.html.unsafe

/**
 * Base template for all standard pages in the application. Contains links, scripts and placeholders for later
 * additions when the page is created. All classes that implement this class must set the functions used to populate
 * the placeholders as properties.
 */
abstract class BasePage : Template<HTML> {

    /** placeholder for style component of the page */
    private val stylesPlaceholder: Placeholder<STYLE> = Placeholder()
    /** placeholder for general content of the page */
    private val contentPlaceholder: Placeholder<FlowContent> = Placeholder()
    /** placeholder for script content of the page */
    private val scriptPlaceholder: Placeholder<FlowContent> = Placeholder()

    /** Function used to populate the [stylesPlaceholder]. Can be an empty function */
    abstract val styles: STYLE.() -> Unit
    /** Function used to populate the [contentPlaceholder]. Can be an empty function */
    abstract val content: FlowContent.() -> Unit
    /** Function used to populate the [scriptPlaceholder]. Can be an empty function */
    abstract val script: FlowContent.() -> Unit

    /** Method from [Template] to create HTML document. Contains most of the layout with some placeholders */
    @Suppress("LongMethod")
    override fun HTML.apply() {
        stylesPlaceholder { styles() }
        contentPlaceholder { content() }
        scriptPlaceholder { this.script() }
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
                href = "https://use.fontawesome.com/releases/v5.15.4/css/all.css"
            }
            link {
                rel = "stylesheet"
                href = "https://unpkg.com/bootstrap-table@1.18.3/dist/bootstrap-table.min.css"
            }
            script {
                src = "https://use.fontawesome.com/releases/v5.15.4/js/all.js"
                attributes["data-auto-replace-svg"] = "nest"
            }
            script {
                src = "https://cdn.jsdelivr.net/npm/lodash@4.17.21/lodash.min.js"
            }
            script {
                src = "https://cdn.jsdelivr.net/npm/jquery/dist/jquery.min.js"
            }
            script {
                src = "https://cdn.jsdelivr.net/npm/jquery-validation@1.19.3/dist/jquery.validate.min.js"
            }
            script {
                src = "/assets/utils.js"
            }
            script {
                src = "/assets/form-validator.js"
            }
            style {
                unsafe {
                    raw("""
                        .clickCell {
                            cursor: pointer;
                        }
                        .validInput {
                            border-color: #28a745;
                        }
                        .invalidInput {
                            color: #dc3545;
                            border-color: #dc3545;
                        }
                        .hidden {
                            display: none;
                        }
                    """.trimIndent())
                }
            }
            style {
                insert(stylesPlaceholder)
            }
        }
        body {
            div(classes = "container-fluid") {
                nav(classes = "navbar navbar-expand-lg navbar-dark bg-dark") {
                    a(classes = "navbar-brand") {
                        href = "#"
                        +"GeoFlow"
                    }
                    ul(classes = "navbar-nav") {
                        li(classes = "nav-item") {
                            a(classes = "nav-link") {
                                href = "/index"
                                +"Home"
                            }
                        }
                        li(classes = "nav-item") {
                            a(classes = "nav-link") {
                                href = "/logout"
                                +"Logout"
                            }
                        }
                    }
                }
                insert(contentPlaceholder)
                script {
                    src = "/assets/subscribe-table.js"
                }
                script {
                    src = "/assets/sub-table.js"
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
                    src = "https://unpkg.com/bootstrap-table@1.18.3/dist/bootstrap-table.min.js"
                }
                insert(scriptPlaceholder)
                div {
                    id = "toasts"
                    attributes["aria-live"] = "polite"
                    attributes["aria-atomic"] = "atomic"
                    style = "position: absolute; bottom: 10px; right: 10px; min-height: 10px;"
                }
            }
        }
    }
}
