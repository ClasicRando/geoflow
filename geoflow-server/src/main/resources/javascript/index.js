$(document).ready(function() {
    $(`#${operationsTableId}`).on('click-row.bs.table', (e, row, element, field) => { redirect(row.href) });
    $(`#${actionsTableId}`).on('click-row.bs.table', (e, row, element, field) => { redirect(row.href) });
});