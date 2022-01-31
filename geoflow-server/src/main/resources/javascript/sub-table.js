document.addEventListener('DOMContentLoaded', () => {
    const elements = document.querySelectorAll('[data-detail-view=true]');
    const elementsCount = elements.length;
    for (let i = 0; i < elementsCount; i++) {
        const element = elements[i];
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
                await getData(dataUrl.value, row[idField.value], columns, $detail);
            } else {
                $detail.html('<table></table>').find('table').bootstrapTable({
                    columns: columns,
                    data: [row],
                });
            }
        })
    }
});

/**
 * 
 * @param {string} url 
 * @param {string} idField 
 * @param {Object.<string, string>} columns 
 * @param {JQuery} $detail 
 * @returns 
 */
async function getData(url, idField, columns, $detail) {
    const response = await fetchApi(url.replace('{id}', idField), FetchMethods.GET);
    if (!response.success) {
        console.log(response);
        return;
    }
    $detail.html('<table></table>').find('table').bootstrapTable({
        columns: columns,
        data: response.payload,
    });
}
