var localRow = {};
function pickup() {
    $(`#${modalId}`).modal('toggle');
    localRow['pickup'] = 'true';
    post(localRow);
}
function handleRowClick(row) {
    localRow = row;
    const url = new URL(window.location.href);
    const urlParams = new URLSearchParams(url.search);
    const code = urlParams.get('code');
    if (row[`${code}_user`] === '') {
        $(`#${modalId}`).modal('toggle');
    } else {
        post(row);
    }
}
$(`#${tableId}`).on('click-row.bs.table', (e, row, element, field) => { handleRowClick(row) });