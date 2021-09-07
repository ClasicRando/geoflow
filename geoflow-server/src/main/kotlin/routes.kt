import html.index
import io.ktor.application.*
import io.ktor.html.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.index() {
    get("/") {
        call.respondRedirect("/index")
    }
    get("/index") {
        call.respondHtml {
            index()
        }
    }
    post("/index") {

    }
}