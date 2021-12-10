const validationRules = {
    username: 'required',
    password: 'required',
};
const validationMessages = {
    username: 'Please provide a username',
    password: 'Please provide a password',
};

let validator;
$(document).ready(() => {
    validator = $('#loginForm').validate({
        rules: validationRules,
        messages: validationMessages,
        errorClass: 'invalidInput',
        validClass: 'validInput',
        submitHandler: () => {},
    });
    $('#submit').click(async (e) => {
        const $button = $(e.target)
        $button.prop('disabled', true);
        e.target.innerHTML = `
            <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
            Loading...
        `;
        const user = {
            username: $('#username').val(),
            password: $('#password').val(),
        };
        const response = await fetchPOST('/login', user);
        if (response.status === 200) {
            if (response.headers.get('Content-Type').includes('application/json')) {
                const json = await response.json();
                if ('success' in json) {
                    redirect('/index');
                } else {
                    resetButton($button);
                    $('#message').text(json.error);
                }
            } else {
                resetButton($button);
                console.log(await response.text());
                $('#message').text('Response content not json. See console for details');
            }
        } else {
            resetButton($button);
            console.log(await response.text());
            $('#message').text('Invalid Response');
        }
    });
});

function resetButton($button) {
    $button.empty();
    $button.text('Submit');
    $button.prop('disabled', false);
}