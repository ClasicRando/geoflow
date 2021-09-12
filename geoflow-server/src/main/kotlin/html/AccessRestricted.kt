package html

import kotlinx.html.h3
import kotlinx.html.p

class AccessRestricted(missingRole: String): BasePage() {
    init {
        setContent {
            h3 {
                +"Access Restricted"
            }
            p {
                +"Missing required role ($missingRole) to access this location"
            }
        }
    }
}