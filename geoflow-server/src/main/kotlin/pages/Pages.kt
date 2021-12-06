package pages

import io.ktor.html.Template
import kotlinx.html.HTML

/** Utility function to apply any HTML template */
fun HTML.applyTemplate(template: Template<HTML>): Unit = with(template) {
    apply()
}

/** Index page with static template. Opening page of web application */
fun HTML.index(): Unit = applyTemplate(Index)

/** PipelineStatus page with template created from [workflowCode] */
fun HTML.pipelineStatus(workflowCode: String): Unit = applyTemplate(PipelineStatus(workflowCode))

/** PipelineTasks page with template created from [runId] */
fun HTML.pipelineTasks(runId: Long): Unit = applyTemplate(PipelineTasks(runId))

/** BasePage template with simple message inserted */
fun HTML.errorPage(message: String): Unit = applyTemplate(ErrorPage(message))

/** Admin Dashboard page with data, tables and charts showing application administration related details */
fun HTML.adminDashboard(): Unit = applyTemplate(AdminDashboard)
