$(document).ready(function() {
    for (element of document.querySelectorAll('[data-detail-view=true]')) {
        const dataUrl = element.attributes['data-sub-table-url'];
        const idField = element.attributes['data-sub-table-id'];
        const columns = [];
        for (const attr of element.attributes) {
            if (!(attr.localName.includes('data-sub-table-field'))) {
                continue;
            }
            const column = {};
            for (const pair of attr.value.split('&')) {
                const [key, value] = pair.split('=');
                column[key] = value;
            }
            if (!('title' in column)) {
                column['title'] = titleCase(column.field);
            }
            columns.push(column);
        }
        $(element).on('expand-row.bs.table', async (e, index, row, $detail) => {
            if (dataUrl !== undefined) {
                getData(dataUrl.value, row[idField.value], columns, $detail)
            } else {
                $detail.html('<table></table>').find('table').bootstrapTable({
                    columns: columns,
                    data: [row],
                });
            }
        })
    }
});

async function getData(url, idField, columns, $detail) {
    const response = await fetch(url.replace('{id}', idField));
    if (response.status !== 200) {
        console.log(await response.text());
        return;
    }
    const json = await response.json();
    if (!('payload' in json)) {
        console.log(json);
        return;
    }
    const payload = json.payload;
    if ((payload.length||0) === 0) {
        console.log(payload);
        return;
    }
    $detail.html('<table></table>').find('table').bootstrapTable({
        columns: columns,
        data: payload,
    });
}
