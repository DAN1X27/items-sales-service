async function login() {
    const email = prompt("Email");
    const password = prompt("Password")
    const URL = "http://localhost:8080/auth/login";
    try {
        const response = await fetch(URL, {
            method: "POST",
            body: JSON.stringify({
                email: email,
                password: password
            }),
            headers: {
                "Content-Type" : "application/json"
            }
        });
        const data = await response.json();
        if (!response.ok) {
            console.log(`Login error: ${data}`);
        } else {
            let token = data["jwt-token"];
            localStorage.setItem("jwt-token", token);
        }
    } catch (error) {
        console.log(`Login error: ${error}`)
    }
}

async function connect() {
    let token = localStorage.getItem("jwt-token");
    if (!token) {
        await login();
        token = localStorage.getItem("jwt-token");
    }
    const id = Number(prompt("Chat id"))
    const URL = "http://localhost:8080/ws";
    const socket = new SockJS(URL);
    const stompClient = Stomp.over(socket);
    const headers = {
        Authorization: `Bearer ${token}`
    };
    stompClient.connect(headers, (frame) => {
        console.log(`Connected: ${frame}`);
        stompClient.subscribe(`/topic/chat/${id}`, (message) => {
            console.log(`Message: ${message}`);
        })
    })
}
connect();