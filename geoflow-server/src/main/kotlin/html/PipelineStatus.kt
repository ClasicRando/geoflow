package html

import kotlinx.html.*
import orm.tables.PipelineRuns
import orm.tables.WorkflowOperations

class PipelineStatus(workflowCode: String): BasePage() {
    private val modalId = "selectReadyRun"
    init {
        setContent {
            basicTable(
                WorkflowOperations.workflowName(workflowCode),
                "runs",
                "/api/pipeline-runs?code=$workflowCode",
                PipelineRuns.tableDisplayFields
            )
            basicModal(
                modalId,
                "Select Run",
                "Pickup this run to collect?"
            )
        }
        setScript {
            postObject
            script {
                unsafe {
                    raw("""
                        var localRow = {};
                        function pickup() {
                            $('#selectReadyRun').modal('toggle');
                            localRow['pickup'] = 'true';
                            post(localRow);
                        }
                        function handleRowClick(row) {
                            localRow = row;
                            const url = new URL(window.location.href);
                            const urlParams = new URLSearchParams(url.search);
                            const code = urlParams.get('code');
                            if (row[`${'$'}{code}_user`] === '') {
                                $('#$modalId').modal('toggle');
                            } else {
                                post(row);
                            }
                        }
                        $('#runs').on('click-row.bs.table', (e, row, element, field) => { handleRowClick(row) });
                    """.trimIndent())
                }
            }
        }
    }
}