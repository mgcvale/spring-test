import { getCookie } from "./cookies.js";
const serverip = "http://localhost:8080";

document.getElementById("submit-button").addEventListener('click', submit());

async function submit() {
    const username = document.getElementById("username").value;
    var deleteDirs = document.getElementById("delete-dirs");
    const token = getCookie('token');
    deleteDirs = deleteDirs != true ? false : true;
    
    console.log("token: " + token);
    console.log(username);
    console.log("deleteDirs: " + deleteDirs);
        
    const formData = new URLSearchParams();
    formData.append('logintoken', token);
    formData.append('deletedir', deleteDirs);
    formData.append('user', username);

    const response = await fetch(serverip + "/users/remove", {
        method: "POST",
        body: formData,
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        }
    }).then(response => {
        if(!response.ok) {
            throw new Error("Network response wasnt ok: " + response.statusText);
        }
        return response.json();
    }).then(json => {
        console.log(json);
        return json;
    }).catch(error => {
        console.error('Error:', error);
    });

    console.log(response);
}

