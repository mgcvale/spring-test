import { deleteCookie } from './user/cookies.js'; 
const serverip = "http://localhost:8080";

document.getElementById('login').addEventListener('click', async function(event) {
    console.log("clicou");
    event.preventDefault();
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    const response = await fetch(`${serverip}/users/getToken?user=${username}&password=${password}`);

    if (response.ok) {
        const token = await response.text();
        document.cookie = `token=${token};expires=Fri, 31 Dec 9999 23:59:59 GMT;path=/`;
        alert('Login successful!');
    } else {
        alert('Login failed. Please check your credentials.');
    }
});

document.getElementById("remove-login-btn").addEventListener('click', function(event) {
    deleteCookie('token');
});

