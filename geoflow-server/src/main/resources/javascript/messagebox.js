function showMessageBox(title, message) {
    $('#msgBoxHeader').text(title);
    $('#msgBoxBody').text(message);
    $(`#${messageBoxId}`).modal('toggle');
}