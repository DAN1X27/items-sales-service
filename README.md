# DESCRIPTION
```
Service for selling items using announcements on Java Spring Boot, Spring Cloud.
```
# LAUNCH
```
Use command 'docker-compose up --build' in db, eureka-server, config-server and kafka, and use command 'docker network create items-sales-service-net'.
After these services are started, use this command for other services.
```
# ENDPOINTS
```
Use http://localhost:8080 for all requests.
All endpoints except login, registration and reset password require header 'Authorization': 'Bearer {your token}'
```
## 1. AUTHENTICATION
### POST: /auth/login
```
Login user and returns jwt-token.
```
#### REQUEST EXAMPLE:
```json
"request": {
  "email": "email",
  "password": "password"
}
```
### POST: /auth/registration
```
Sends registration code to user by email.
```
#### REQUEST EXAMPLE:
```json
{
  "email": "email",
  "username": "username",
  "password": "password"      
  "city": "city",
  "country": "country"
}
```
### POST: /auth/registration/confirm
```
Validate registraton code and register user.
```
#### REQUEST EXAMPLE:
```json
"request": {
  "email": "email",
  "key": 123456
}
```
### POST: /auth/password/reset/key
```
Send reset password code to user by email.
```
#### REQUEST EXAMPLE:
```
/auth/password/reset/key?email=email
```
### PATCH: /auth/password/reset
```
Validate email code and update user password.
```
#### REQUEST EXAMPLE:
```json
{
  "email": "email",
  "password": "new_password",
  "key": 123456
}
```
### POST: /auth/email/update/key
```
Send update email code to user by email.
```
#### REQUEST EXAMPLE:
```json
{
  "email": "new email",
  "password": "password"
}
```
### PATCH: /auth/email/update
```
Validate email code and update email.
```
### Request example:
```json
{
  "email": "new email",
  "key": 123456
}
```
### POST: /auth/logout
```
Logout user.
```
## 2. USER
### GET: /users/{id}
```
Searches user by id and returns info about him.
```
### GET: /users/{id}/comments?page=0&count=10
```
Searches user by id and returns comments by page and count in user account.
```
### GET: /users/info
```
Returns user info.
```
### DELETE: /users
```
Delete user account.
```
### PATCH: /users
```
Update user info, all fields in the request is not required.
```
#### REQUEST EXAMPLE:
```json
{
  "username": "new_username",
  "country": "new_country",
  "city": "new_city"
}
```
### PATCH: /users/password
```
Update user password.
```
#### REQUEST EXAMPLE:
```json
{
  "old_password": "old_password",
  "new_password": "new_password"
}
```
### POST: /users/avatar
```
Update user avatar, accepts image in form data.
```
### GET: /users/avatar
```
Returns user avatar.
```
### GET: /users/{id}/avatar
```
Searches user by id and returns user avatar.
```
### DELETE: /users/avatar
```
Delete user avatar and set it to default avatar.
```
### POST: /users/{id}/comment
```
Searches user by id and create comment in user account.
```
#### REQUEST EXAMPLE:
```json
{
  "comment": "comment"
}
```
### DELETE: /users/comment/{id}
```
Delete comment by id if user owner of comment, or else returns error.
```
### POST: /users/{id}/grade?stars={stars}
```
Add grade to user. Accepts stars int request params, max value 5.
```
### GET: /users/reports?page={page}&count={count}
```
Returns all reports by page and count. ADMIN role required. 
```
### DELETE: /users/report/{id}
```
Delete report by id. ADMIN role required. 
```
### POST: /users/{id}/report
```
Find user by id and report him.
```
#### REQUEST EXAMPLE:
```json
{
  "cause": "cause"
}
```
### GET: /users/banned?page={page}&count={count}
```
Returns all banned users by page and count. ADMIN role required. 
```
### POST: /users/{id}/ban
```
Find user by id and ban him. ADMIN role required. 
```
#### REQUEST EXAMPLE:
```json
{
  "cause": "cause"
}
```
### DELETE: /users/{id}/unban
```
Find user by id and unban him. ADMIN role required.
```
### POST: /users/{id}/block
```
Find user by id and block him.
```
### DELETE: /user/{id}/unblock
```
Find user by id and unblock him.
```
### GET: /users/blocked?page={page}&count={count}
```
Returns all blocked users by page and count.
```
## 3. CHATS
### GET: /chats
```
Returns all user chats.
```
### GET: /chats/{id}/messages?page={page}&count={count}
```
Returns all chat messages by page and count.
```
### POST: /chats/{id}
```
Find user by id and create chat with him.
Returns id of the created chat.
```
### DELETE: /chats/{id}
```
Delete chat.
```
### POST: /chats/{id}/message
```
Send text message to chat.
Returns id of the sent message.
```
#### REQUEST EXAMPLE:
```json
{
  "message": "message"
}
```
### PATCH: /chats/message/{id}
```
Update message.
```
### Request example:
```json
{
  "message": "message"
}
```
### POST: /chats/{id}/image
```
Send image to chat. Accepts image in form data.
Returns id of the sent image.
```
### POST: /chats/{id}/video
```
Send video to chat. Accepts video in form data.
Returns id of the sent video.
```
### GET: /chats/video/{id}
```
Returns video by id.
```
### GET: /chats/image/{id}
```
Return image by id.
```
### DELETE: /chats/message/{id}
```
Delete message.
```
## 4. SUPPORT CHATS
### GET: /chats/support/user
```
Returns all user support chats.
```
### GET: /chats/support?page={page}&count={count}&sort={sort}
```
Returns all support chats by page, count and sort. If sort not specified, sort = ASC.
Allowed sort values: ASC, DESC.
ADMIN role is required. 
```
### GET: /chats/support/{id}/messages?page={page}&count={count}
```
Returns all support chat messages by page and count.
```
### POST: /chats/support
```
Create new support chat with message.
```
#### REQUEST EXAMPLE:
```json
{
  "message": "message"
}
```
### PATCH: /chats/support/{id}/close
```
Close support chat.
```
### PATCH: /chats/support/{id}/status/wait
```
Set support chat status to wait.
```
### PATCH: /chats/support/{id}/status/processing
```
Set support chat status to processing.
ADMIN role is required. 
```
### DELETE: /chats/support/{id}
```
Delete support chat.
```
### POST: /chats/support/{id}/message
```
Send text message to support chat.
Returns id of sent message.
```
#### REQUEST EXAMPLE:
```json
{
  "message": "message"
}
```
### PATCH: /chats/support/message/{id}
```
Update message.
```
#### REQUEST EXAMPLE:
```json
{
  "message": "message"
}
```
### POST: /chats/support/{id}/image
```
Send image to support chat.
Accepts image in form data.
Returns id of sent image.
```
### POST: /chats/support/{id}/video
```
Send video to support chat.
Accepts video in form data.
Returns id of sent video.
```
### GET: /chats/support/image/{id}
```
Returns sent image by id.
```
### GET: /chats/support/video/{id}
```
Returns sent video by id.
```
### DELETE: /chats/support/message/{id}
```
Delete message by id.
```
## 5. ANNOUNCEMENTS
### GET: /announcements?page={page}&count={count}
```
Returns all announcements by page and count. The user may not be authorized.
PARAMS (not required): 
1. currency - The currency in which you want to view the announcements (Default value - USD), available currencies: USD, BYN, RUB, EUR.
2. filters - Types of items according to which the response will be filtered.
3. city - city by which to sort (Required if user is not authorized).
4. country - country by which to sort (Required if user is not authorized).
BODY (not required):
1. type - Sort type.
Available sort types: LIKES, PRICE, ID, WATCHES.
2. direction - Sort direction.
Available directions: ASC, DESC.
```
#### REQUEST EXAMPLE:
##### URL: http://localhost:8080/announcemets?page=0&count=10&currency=BYN&filters=PHONES,PODS
##### BODY (not required):
```json
{
  "type": "LIKES",
  "direction": "DESC"
}
```
### GET: /announcements/find?title={title}&page={page}&count={count}
```
Returns all announcements by page, count and title. The user may not be authorized.
PARAMS (not required): 
1. currency - The currency in which you want to view the announcements (Default value - USD), available currencies: USD, BYN, RUB, EUR.
2. filters - Types of items according to which the response will be filtered.
3. city - City by which to sort (Required if user is not authorized).
4. country - Country by which to sort (Required if user is not authorized).
5. title - Title for search.
BODY (not required):
1. type - Sort type.
Available sort types: LIKES, PRICE, ID, WATCHES.
2. direction - Sort direction.
Available directions: ASC, DESC.
```
#### REQUEST EXAMPLE:
##### URL: http://localhost:8080/announcemets/find?title={title}&page=0&count=10&currency=BYN&filters=PHONES,PODS
##### BODY (not required):
```json
{
  "type": "LIKES",
  "direction": "DESC"
}
```
### GET: /announcements/user/{id}?page={page}&count={count}
```
Returns all announcements by user id, page and count.
PARAMS: 
currency - The currency in which you want to view the announcements (Not required, default value - USD), available currencies: USD, BYN, RUB, EUR.
```
### GET: /announcements/{id}
```
Returns announcement by id. The user may not be authorized.
PARAMS: 
currency - The currency in which you want to view the announcements (Not required, default value - USD), available currencies: USD, BYN, RUB, EUR.
```
### POST: /announcements
```
Create new announcement.
PARAMS:
currency - The currency that will be converted into dollars (Not required, default value - USD), available currencies: USD, BYN, RUB, EUR.
```
#### REQUEST EXAMPLE:
##### URL: http://localhost:8080/announcemets?currency=BYN
##### BODY:
```json
{
  "title": "Ihpone 16",
  "description": "description",
  "price": 3000,
  "type": "PHONES",
  "phone_number": "phone_number",
  "country": "Belarus",
  "city": "Minsk"
}
```
### POST: /announcements/{id}/like
```
Add like to announcement.
```
### DELETE: /announcements/{id}/like
```
Delete like from announcement.
```
### GET: /announcements/reports?page={page}&count={count}
```
Returns all reports by page and count.
Admin role is required.
PARAMS:
sort - type of sort reports, available values: ASC, DESC (Not required, default value = DESC).
```
### GET: /announcements/report/{id}
```
Show report information.
Admin role is required.
PARAMS: 
currency - The currency in which you want to view the announcements (Not required, default value - USD), available currencies: USD, BYN, RUB, EUR.
```
### DELETE: /announcements/report/{id}
```
Delete report.
Admin role is required.
```
### PATCH: /announcements/{id}
```
Update announcement.
All params in body are not required.
```
#### REQUEST EXAMPLE:
```json
{
  "title": "Iphone 16",
  "descrition": "new description",
  "price": 3000,
  "currency": "BYN",
  "phone_number": "new phone number",
  "country": "Belarus",
  "city": "Minsk",
  "type": "Phones"
}
```
### GET: /announcements/image/{id}
```
Returns announcement image by image id.
```
### POST: /announcements/{id}/image
```
Add image to announcement, accepts image in form data.
```
### DELETE: /announcements/image/{id}
```
Delete announcement image by image id.
```
### DELETE: /announcements/{id}
```
Delete announcement.
```
### DELETE: /announcements/{id}/ban
```
Ban announcement.
Admin role is required.
```