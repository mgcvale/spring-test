import { getCookie } from "./cookies.js";
const token = getCookie('token');

if(token != null) {
    const warnings = document.getElementsByClassName("login-warning");

    for (const warning of warnings) {
        warning.remove();
    }

}
