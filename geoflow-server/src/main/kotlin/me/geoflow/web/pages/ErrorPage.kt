package me.geoflow.web.pages

import kotlinx.html.FlowContent

/** Generic class for rendering pages with error messages */
class ErrorPage(
    /** Error message to display in the HTML output */
    private val message: String
) : BasePage() {

    override val content: FlowContent.() -> Unit = {
        +message
    }

}
