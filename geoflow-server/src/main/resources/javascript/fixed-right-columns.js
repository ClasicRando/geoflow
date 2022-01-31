document.addEventListener('DOMContentLoaded', () => {
    forEach(document.querySelectorAll('table[data-fixed-right-number]'), (/** @type {HTMLTableElement} */table) => {
        const fixedRightNumber = table.attributes.getNamedItem('data-fixed-right-number');
        $(table).bootstrapTable('refreshOptions', {fixedRightNumber: fixedRightNumber});
    });
});