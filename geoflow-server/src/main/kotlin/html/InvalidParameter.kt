package html

import kotlinx.html.h3
import kotlinx.html.p

class InvalidParameter(message: String): BasePage() {
    init {
        setContent {
            h3 {
                +"Invalid URL Parameter"
            }
            p {
                +message
            }
        }
    }
}