package pages

import kotlinx.html.FlowContent
import kotlinx.html.STYLE

/** Generic class for rendering pages with error messages */
class ErrorPage(
    /** Error message to display in the HTML output */
    private val message: String
) : BasePage() {

    override val styles: STYLE.() -> Unit = {}

    override val content: FlowContent.() -> Unit = {
        +message
    }

    override val script: FlowContent.() -> Unit = {}
}
