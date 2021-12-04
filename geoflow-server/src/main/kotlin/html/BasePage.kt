package html

import io.ktor.html.Template
import io.ktor.html.Placeholder
import io.ktor.html.insert
import kotlinx.html.FlowContent
import kotlinx.html.script
import kotlinx.html.HTML
import kotlinx.html.STYLE
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.lang
import kotlinx.html.li
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.nav
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.ul
import kotlinx.html.unsafe

/**
 * Base template for all standard pages in the application. Contains a links, scripts and placeholders for later
 * additions when the page is created.
 *
 * Instances are created using static methods that fill in placeholder values by name. These methods return the instance
 * itself so more placeholders can be filled after the initial fill.
 */
class BasePage private constructor (
    /** placeholder for style component of the page */
    val styles: Placeholder<STYLE> = Placeholder(),
    /** placeholder for general content of the page */
    val content: Placeholder<FlowContent> = Placeholder(),
    /** placeholder for script content of the page */
    val script: Placeholder<FlowContent> = Placeholder(),
) : Template<HTML> {

    /** Applies [block] to the [styles] Placeholder */
    fun withStyles(block: STYLE.() -> Unit): BasePage {
        styles { block() }
        return this
    }

    /** Applies [block] to the [content] Placeholder */
    fun withContent(block: FlowContent.() -> Unit): BasePage {
        content { block() }
        return this
    }

    /** Applies [block] to the [script] Placeholder */
    fun withScript(block: FlowContent.() -> Unit): BasePage {
        script { block() }
        return this
    }

    /** Method from [Template] to create HTML document. Contains most of the layout with some placeholders */
    @Suppress("LongMethod")
    override fun HTML.apply() {
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
                href = "https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@5.15.4/css/all.min.css"
            }
            link {
                rel = "stylesheet"
                href = "https://unpkg.com/bootstrap-table@1.18.3/dist/bootstrap-table.min.css"
            }
            script {
                src = "https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@5.15.4/js/all.min.js"
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
            style {
                unsafe {
                    raw("""
                        .clickCell {
                            cursor: pointer;
                        }
                        .inTableButton {
                            display: inline;
                            border: none;
                            background: none;
                        }
                        .inTableButton:hover {
                            border: 1px solid;
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
                insert(styles)
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
                insert(content)
                script {
                    src = "/assets/subscribe-table.js"
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
                insert(script)
            }
        }
    }
    companion object {
        /** Creates a [BasePage] instance, applies [block] to the [styles] Placeholder and returns the instance */
        fun withStyles(block: STYLE.() -> Unit): BasePage {
            return BasePage().apply { withStyles(block) }
        }
        /** Creates a [BasePage] instance, applies [block] to the [content] Placeholder and returns the instance */
        fun withContent(block: FlowContent.() -> Unit): BasePage {
            return BasePage().apply { withContent(block) }
        }
        /** Creates a [BasePage] instance, applies [block] to the [script] Placeholder and returns the instance */
        fun withScript(block: FlowContent.() -> Unit): BasePage {
            return BasePage().apply { withScript(block) }
        }
    }
}
