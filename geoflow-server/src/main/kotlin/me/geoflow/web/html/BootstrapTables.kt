package me.geoflow.web.html

import me.geoflow.core.database.tables.SourceTables
import kotlinx.html.FlowContent
import kotlinx.html.script
import me.geoflow.core.database.tables.SourceTableColumns
import me.geoflow.core.web.html.SubTableDetails
import me.geoflow.core.web.html.basicTable
import me.geoflow.core.web.html.tableButton
import me.geoflow.core.web.html.addParamsAsJsGlobalVariables

private const val SOURCE_TABLES_TABLE_ID = "sourceTables"

/** Constructs a basic table for source table data */
fun FlowContent.sourceTables(runId: Long) {
    basicTable<SourceTables>(
        tableId = SOURCE_TABLES_TABLE_ID,
        dataUrl = "source-tables/$runId",
        dataField = "payload",
        tableButtons = listOf(
            tableButton(
                name = "btnAddTable",
                text = "Add Source Table",
                icon = "fa-plus",
                event = "newSourceTableRow()",
                title = "Add new source table to the current run",
            ),
        ),
        customSortFunction = "sourceTableRecordSorting",
        clickableRows = false,
        subTableDetails = SubTableDetails(
            url = "source-table-columns/{id}",
            idField = "st_oid",
            fields = SourceTableColumns.tableDisplayFields,
        ),
    )
    script { addParamsAsJsGlobalVariables("sourceTablesTableId" to SOURCE_TABLES_TABLE_ID) }
    sourceTableEditModal()
}
