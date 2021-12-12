import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import me.geoflow.core.database.tables.InternalUsers
import me.geoflow.api.utils.User
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.ContentNegotiation
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.websocket.WebSockets
import it.justwrote.kjob.Job
import it.justwrote.kjob.KJob
import it.justwrote.kjob.Mongo
import it.justwrote.kjob.dsl.ScheduleContext
import it.justwrote.kjob.job.JobExecutionType
import it.justwrote.kjob.kjob
import mu.KLogger
import mu.KotlinLogging
import me.geoflow.api.paths.ApiActions
import me.geoflow.api.paths.ApiJobs
import me.geoflow.api.paths.ApiOperations
import me.geoflow.api.paths.ApiPipelineRunTasks
import me.geoflow.api.paths.ApiPipelineRuns
import me.geoflow.api.paths.ApiPlottingFields
import me.geoflow.api.paths.ApiPlottingMethodTypes
import me.geoflow.api.paths.ApiPlottingMethods
import me.geoflow.api.paths.ApiSourceTables
import me.geoflow.api.paths.ApiUsers
import me.geoflow.core.database.Database
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import java.util.concurrent.TimeUnit

private const val JWT_CACHE_SIZE = 10L
private const val JWT_EXPIRE_HOURS = 24L
private const val JWT_LEEWAY = 10L
private const val JWT_BUCKET_SIZE = 10L
private const val JWT_REFILL_RATE = 1L
private const val EXPIRE_DURATION = 57600000

private const val KEEP_ALIVE_PERIOD = 60L
private const val CLEANUP_PERIOD = 60L
private const val CLEANUP_SIZE = 50
private const val EXPIRE_LOCK = 5L

/** logger used for the KJob instance */
val logger: KLogger = KotlinLogging.logger {}
/** Kjob instance used by the API to schedule jobs for the worker application */
private val kjob: KJob = kjob(Mongo) {
    nonBlockingMaxJobs = 1
    blockingMaxJobs = 1
    maxRetries = 0
    defaultJobExecutor = JobExecutionType.NON_BLOCKING

    exceptionHandler = { t -> logger.error("Unhandled exception", t) }
    keepAliveExecutionPeriodInSeconds = KEEP_ALIVE_PERIOD
    jobExecutionPeriodInSeconds = 1
    cleanupPeriodInSeconds = CLEANUP_PERIOD
    cleanupSize = CLEANUP_SIZE

    connectionString = "mongodb://127.0.0.1:27017"
    databaseName = "kjob"
    jobCollection = "kjob-jobs"
    lockCollection = "kjob-locks"
    expireLockInMinutes = EXPIRE_LOCK
}.start()

/**
 * Access point for scheduling jobs for the kjob instance. Provides the same api as the [schedule][KJob.schedule]
 * function but does not provide access to the instance itself
 */
suspend fun<J: Job> scheduleJob(job: J, action: ScheduleContext<J>.(J) -> Unit) {
    kjob.schedule(job) {
        action(job)
    }
}

/** Main entry of the API. Initializes the server engine. */
fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

/** Module describing server features and routing */
@Suppress("unused", "LongMethod")
fun Application.module() {

    val privateKeyString = environment.config.property("jwt.privateKey").getString()
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val myRealm = environment.config.property("jwt.realm").getString()
    val jwkProvider = JwkProviderBuilder(issuer)
        .cached(JWT_CACHE_SIZE, JWT_EXPIRE_HOURS, TimeUnit.HOURS)
        .rateLimited(JWT_BUCKET_SIZE, JWT_REFILL_RATE, TimeUnit.MINUTES)
        .build()

    /** */
    install(Authentication) {
        jwt("auth-jwt") {
            realm = myRealm
            verifier(jwkProvider, issuer) {
                acceptLeeway(JWT_LEEWAY)
            }
            validate { credential ->
                if (credential.payload.getClaim("user_oid").asLong() != 0L) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    /** Install JSON serialization to API responses. */
    install(ContentNegotiation) {
        json()
    }

    /** Install WebSockets for pub/sub pattern. */
    install(WebSockets)

    /** Base routing of application */
    routing {
        authenticate("auth-jwt") {
            route("/api") {
                ApiOperations.generatePath(this)
                ApiActions.generatePath(this)
                ApiPipelineRuns.generatePath(this)
                ApiPipelineRunTasks.generatePath(this)
                ApiSourceTables.generatePath(this)
                ApiUsers.generatePath(this)
                ApiJobs.generatePath(this)
                ApiPlottingFields.generatePath(this)
                ApiPlottingMethods.generatePath(this)
                ApiPlottingMethodTypes.generatePath(this)
            }
        }
        post(path = "/login") {
            val user = call.receive<User>()
            val validation = Database.runWithConnection {
                InternalUsers.validateUser(it, user.username, user.password)
            }
            val response = when(validation) {
                is InternalUsers.ValidationResponse.Success -> {
                    val internalUser = Database.runWithConnection { InternalUsers.getUser(it, user.username) }
                    val publicKey = jwkProvider.get("6f8856ed-9189-488f-9011-0ff4b6c08edc").publicKey
                    val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyString))
                    val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpecPKCS8)
                    val token = JWT.create()
                        .withAudience(audience)
                        .withIssuer(issuer)
                        .withClaim("user_oid", internalUser.userOid)
                        .withExpiresAt(Date(System.currentTimeMillis() + EXPIRE_DURATION))
                        .sign(Algorithm.RSA256(publicKey as RSAPublicKey, privateKey as RSAPrivateKey))
                    hashMapOf("token" to token)
                }
                is InternalUsers.ValidationResponse.Failure -> {
                    hashMapOf("error" to InternalUsers.ValidationResponse.Failure.ERROR_MESSAGE)
                }
            }
            call.respond(response)
        }
        static(remotePath = ".well-known") {
            resources(resourcePackage = "certs")
        }
    }
}
