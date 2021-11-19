var runId = -1;
function pickup() {
    $(`#${modalId}`).modal('toggle');
    post({'run_id': runId});
}
function handleRowClick(row) {
    runId = row.run_id;
    const url = new URL(window.location.href);
    const urlParams = new URLSearchParams(url.search);
    const code = urlParams.get('code');
    if (row[`${code}_user`] === '') {
        $(`#${modalId}`).modal('toggle');
    } else {
        redirect(`/tasks/${row.run_id}`);
    }
}
$(document).ready(function() {
    $(`#${tableId}`).on('click-row.bs.table', (e, row, element, field) => { handleRowClick(row) });
});