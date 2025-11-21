# Server Protocol Documentation

## Client → Server Commands

### LOGIN
```
LOGIN <username>
```
- Join the group with a unique username
- Username must not already exist

### MESSAGE
```
MESSAGE <subject>|<content>
MESSAGE <content>
```
- Post a message to the group
- If no `|` separator, subject defaults to "(no subject)"
- Requires successful LOGIN first

### GET_MESSAGE
```
GET_MESSAGE <id>
```
- Retrieve full content of message by ID
- Requires successful LOGIN first

### QUIT
```
QUIT
```
- Leave the group

## Server → Client Responses

### Connection
- `WELCOME` - Initial greeting on connect

### Broadcast Messages (sent to all clients)
- `USER_JOINED username` - User joined the group
- `USER_LEFT username` - User left the group
- `NEW_MESSAGE id|sender|date|subject` - New message posted

### LOGIN Responses
- `OK LOGIN` - Login successful
- `USERS user1,user2,user3` - List of existing users (`USERS` if empty)
- `MESSAGE_SUMMARY id|sender|date|subject` - Last 2 messages (one per line, may be 0-2 lines)
- `ERR USERNAME_EXISTS` - Username already taken
- `ERR INVALID_USERNAME` - Empty or invalid username
- `ERR ALREADY_LOGGED_IN` - Client is already logged in

### MESSAGE Responses
- `OK MESSAGE` - Message posted successfully
- `ERR NOT_LOGGED_IN` - Must login first

### GET_MESSAGE Responses
- `MESSAGE_CONTENT content` - Message content text only
- `ERR MESSAGE_NOT_FOUND` - Invalid message ID
- `ERR INVALID_MESSAGE_ID` - Non-numeric ID
- `ERR NOT_LOGGED_IN` - Must login first

### QUIT Responses
- `BYE` - Confirmation before disconnect

### Other
- `ERR UNKNOWN_COMMAND` - Invalid command

