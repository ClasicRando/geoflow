package html

import io.ktor.html.*
import kotlinx.html.FlowContent

class Index: BasePage() {
    init {
        setContent {
            +"This is the index"
        }
        setScript {

        }
    }
}