package danix.app.email_sender_service.services;

public interface EmailSenderService {

    void sendMessage(String to, String message);

    default String getHtmlContent(String message) {
        return String.format(
            """
            <html>
                <head>
                    <style>
                        body {
                            color: white;
                            background-color: #1a1a1a;
                            font-family: Arial, sans-serif;
                        }
                        .container {
                            background-color: #000720;
                            color: white;
                            padding: 20px;
                            border-radius: 10px;
                            border: 2px solid #ffcc00;
                            display: inline-block;
                            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.5);
                            margin-top: 20px;
                        }
                        h1 {
                            margin: 0;
                            text-align: center;
                            font-weight: bold;
                        }
                        .message {
                            font-size: 25px;
                            font-weight: normal;
                            white-space: pre-wrap;
                        }
                    </style>
                </head>
                <body style = "text-align: center">
                    <div class="container">
                        <h1>Items sales service</h1>
                        <p class="message">%s</p>
                    </div>
                </body>
            </html>
            """, message
        );
    }

}
