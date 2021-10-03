package html

import kotlinx.html.*
import orm.enums.FileCollectType
import orm.tables.PipelineRunTasks

class PipelineTasks(runId: Long): BasePage() {
    private val taskDataModalId = "taskData"
    private val sourceTableModalId = "sourceTableData"
    private val taskTableId = "tasks"
    private val sourceTablesTableId = "source-tables"
    private val tableButtons = listOf(
        TableButton(
            "btnRun",
            "Run Next Task",
            "fa-play",
            """
            let ${'$'}table = ${'$'}('#$taskTableId');
            let options = ${'$'}table.bootstrapTable('getOptions');
            if (options.autoRefreshStatus === false) {
                showMessageBox('Error', 'Please turn on auto refresh to run tasks');
                return;
            }
            let data = ${'$'}table.bootstrapTable('getData');
            if (data.find(row => row.task_status === 'Running' || row.task_status === 'Scheduled') !== undefined) {
                showMessageBox('Error', 'Task already running');
                return;
            }
            let row = data.find(row => row.task_status === 'Waiting');
            if (row == undefined) {
                showMessageBox('Error', 'No task to run');
                return;
            }
            const params = new URLSearchParams(window.location.href.replace(/^[^?]+/g, ''));
            postValue(`/api/run-task?runId=${'$'}{params.get('runId')}&prTaskId=${'$'}{row.pipeline_run_task_id}`);
            """.trimIndent(),
            "Run the next available task if there is no other tasks running",
        ),
        TableButton(
            "btnRunAll",
            "Run All Tasks",
            "fa-fast-forward",
            """
            let ${'$'}table = ${'$'}('#$taskTableId');
            let options =  ${'$'}table.bootstrapTable('getOptions');
            if (options.autoRefreshStatus === false) {
                showMessageBox('Error', 'Please turn on auto refresh to run tasks');
                return;
            }
            let data = ${'$'}table.bootstrapTable('getData');
            if (data.find(row => row.task_status === 'Running' || row.task_status === 'Scheduled') !== undefined) {
                showMessageBox('Error', 'Task already running');
                return;
            }
            let row = data.find(row => row.task_status === 'Waiting');
            if (row == undefined) {
                showMessageBox('Error', 'No task to run');
                return;
            }
            const params = new URLSearchParams(window.location.href.replace(/^[^?]+/g, ''));
            postValue(`/api/run-all?runId=${'$'}{params.get('runId')}&prTaskId=${'$'}{row.pipeline_run_task_id}`);
            """.trimIndent(),
            "Run the next available tasks if there is no other tasks running. Stops upon task failure or User Task",
        ),
    )
    private val headerButtons = listOf(
        HeaderButton(
            "btnSourceTables",
            """
                function sourceTables() {
                    $('#$sourceTablesTableId').bootstrapTable('refresh');
                    $('#$sourceTableModalId').modal('show');
                }
            """.trimIndent(),
        ) {
            li(classes = "header-button") {
                button(classes = "btn btn-secondary") {
                    onClick = "sourceTables()"
                    +"Source Tables"
                }
            }
        }
    )
    init {
        setStyles {
            unsafe {
                raw("""
                    .header-button-list {
                        margin 0;
                        padding 0;
                    }
                    .header-button {
                        margin-right 10px;
                        padding: 0 10px;
                        display: inline-block;
                    }
                """.trimIndent())
            }
        }
        setContent {
            autoRefreshTable(
                taskTableId,
                "/api/pipeline-run-tasks?runId=$runId",
                PipelineRunTasks.tableDisplayFields,
                tableButtons = tableButtons,
                headerButtons = headerButtons,
            )
            dataDisplayModal(
                taskDataModalId,
                "Task Details",
            )
            sourceTablesModal(
                sourceTableModalId,
                sourceTablesTableId,
                "/api/source-tables?runId=$runId",
            )
            messageBoxModal()
        }
        setScript {
            script {
                src = "https://unpkg.com/bootstrap-table@1.18.3/dist/extensions/auto-refresh/bootstrap-table-auto-refresh.js"
            }
            postValue()
            script {
                unsafe {
                    raw("""
                        function statusFormatter(value, row) {
                            switch(value) {
                                case 'Waiting':
                                    return '';
                                case 'Scheduled':
                                    return '<i class="fa fa-arrow-circle-right"></i>';
                                case 'Running':
                                    return '<i class="fa fa-cog fa-spin"></i>';
                                case 'Complete':
                                    return '<i class="fa fa-check"></i>';
                                case 'Failed':
                                    return '<i class="fa fa-exclamation"></i>';
                            }
                        }
                        function boolFormatter(value, row) {
                            return value ? '<i class="fa fa-check"></i>' : '';
                        }
                        function titleCase(title) {
                            return title.replace(
                                /\w\S*/g,
                                function(txt) {
                                    return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
                                }
                            ).replace(
                                'Id',
                                'ID'
                            );
                        }
                        function showDataDisplayModal(action, data) {
                            let options = $('#$taskTableId').bootstrapTable('getOptions');
                            if (options.autoRefreshStatus === false) {
                                showMessageBox('Error', 'Please turn on auto refresh to select tasks');
                                return;
                            }
                            let ${'$'}modalBody = $('#${taskDataModalId}Body');
                            ${'$'}modalBody.empty();
                            const div = document.createElement('div');
                            switch(action) {
                                case 'choice':
                                    const btnInfo = document.createElement('button');
                                    const btnReset = document.createElement('button');
                                    btnInfo.innerHTML = 'Task Info';
                                    btnReset.innerHTML = 'Rest Task';
                                    btnInfo.classList.add('btn','btn-secondary', 'mx-2');
                                    btnReset.classList.add('btn','btn-secondary', 'mx-2');
                                    btnInfo.onclick = () => {showDataDisplayModal('info', data);};
                                    btnReset.onclick = () => {showDataDisplayModal('reset', data);};
                                    div.appendChild(btnInfo);
                                    div.appendChild(btnReset);
                                    ${'$'}modalBody.append(div);
                                    $('#$taskDataModalId').modal('show');
                                    break;
                                case 'reset':
                                    const params = new URLSearchParams(window.location.href.replace(/^[^?]+/g, ''));
                                    const prTaskId = data.pipeline_run_task_id;
                                    postValue(`/api/reset-task?runId=${'$'}{params.get('runId')}&prTaskId=${'$'}{prTaskId}`);
                                    $('#$taskDataModalId').modal('hide');
                                    break;
                                case 'info':
                                    for (const [key, value] of Object.entries(data)) {
                                        const label = document.createElement('label');
                                        label['for'] = key.replace(/\s+/g, '_');
                                        label.innerHTML = titleCase(key.replace(/_+/g, ' '));
                                        div.appendChild(label);
                                        const textValue = document.createElement('p');
                                        textValue.id = key.replace(/\s+/g, '_');
                                        textValue.innerHTML = value === '' ? ' ' : value;
                                        textValue.classList.add('border', 'rounded', 'p-3');
                                        div.appendChild(textValue);
                                    }
                                    ${'$'}modalBody.append(div);
                                    break;
                            }
                        }
                        function editSourceTableRow(row) {
                            let ${'$'}table = $('#$sourceTablesTableId');
                            let ${'$'}formBody = $('#${sourceTableModalId}EditRowBody');
                            ${'$'}formBody.empty();
                            let allColumns = ${'$'}table.bootstrapTable('getOptions')['columns'][0];
                            let columns = allColumns.filter(column => column.visible && column.editable);
                            let columnNames = columns.map(column => column.field);
                            for (const [key, value] of Object.entries(row)) {
                                if (!columnNames.includes(key)) {
                                    continue;
                                }
                                const div = document.createElement('div');
                                div.classList.add('form-group');
                                const label = document.createElement('label');
                                label['for'] = key;
                                label.innerHTML = columns.filter(column => column.field === key)[0].title;
                                let field;
                                if (typeof(value) === 'boolean') {
                                    field = document.createElement('input');
                                    field.type = 'checkbox';
                                    field.value = value;
                                    field.classList.add('form-check-input');
                                    label.classList.add('form-check-label');
                                } else if (key === 'collect_type') {
                                    field = document.createElement('select');
                                    let types = [${FileCollectType.values().joinToString("','", "'", "'")}];
                                    for (type of types) {
                                        const option = document.createElement('option');
                                        option.value = type;
                                        option.innerHTML = type;
                                        field.appendChild(option);
                                    }
                                    field.value = value;
                                    field.classList.add('form-control');
                                } else {
                                    field = document.createElement('input');
                                    field.type = 'text';
                                    field.value = value;
                                    field.classList.add('form-control');
                                }
                                field.id = key;
                                field.name = key;
                                div.appendChild(label);
                                div.appendChild(field);
                                ${'$'}formBody.append(div);
                            }
                            $('#${sourceTableModalId}EditRow').modal('show');
                        }
                        $('#$taskTableId').on('click-row.bs.table', (e, row, element, field) => {
                            showDataDisplayModal('choice', row);
                        });
                        $('#$sourceTablesTableId').on('click-row.bs.table', (e, row, element, field) => {
                            editSourceTableRow(row);
                        });
                    """.trimIndent())
                }
            }
        }
    }
}