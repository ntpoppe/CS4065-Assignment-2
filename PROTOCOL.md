# Server Protocol Documentation

## Client -> Server Commands

### LOGIN
```
LOGIN <username>
```
- Login with a unique username
- Username must not already exist
- User must join groups separately after logging in

### JOIN
```
JOIN <group_id_or_name>
```
- Join a private group by ID or name
- Requires successful LOGIN first
- User can join multiple groups simultaneously
- Returns group information and recent messages

### LEAVE
```
LEAVE <group_id_or_name>
```
- Leave a specific group by ID or name
- Requires successful LOGIN first
- User must be a member of the group

### MESSAGE
```
MESSAGE <group_id> <subject>|<content>
MESSAGE <group_id> <content>
```
- Post a message to a specific group
- Requires successful LOGIN first
- User must be a member of the specified group
- If no `|` separator, subject defaults to "(no subject)"

### GET_MESSAGE
```
GET_MESSAGE <id>
```
- Retrieve full content of message by ID
- Requires successful LOGIN first
- User must be a member of the group containing the message

### USERS
```
USERS <group_id>
```
- Get list of users in a specific group
- Requires successful LOGIN first
- User must be a member of the specified group

### GROUPS
```
GROUPS
```
- List all available groups on the server
- Can be called at any time

### QUIT
```
QUIT
```
- Disconnect from the server
- Leaves all joined groups

## Server -> Client Responses

### Connection
- `WELCOME` - Initial greeting on connect
- `GROUPS id1:name1,id2:name2,...` - List of 5 available groups sent immediately after WELCOME

### Broadcast Messages (sent to all members of a group)
- `USER_JOINED <group_id> <username>` - User joined the group
- `USER_LEFT <group_id> <username>` - User left the group
- `NEW_MESSAGE <group_id> <id|sender|date|subject>` - New message posted to the group
- Note: Users only see broadcast messages from groups they are members of

### LOGIN Responses
- `OK LOGIN` - Login successful
- `ERR USERNAME_EXISTS` - Username already taken
- `ERR INVALID_USERNAME` - Empty or invalid username
- `ERR ALREADY_LOGGED_IN` - Client is already logged in

### JOIN Responses
- `OK JOIN <group_name>` - Successfully joined the group
- `USERS <group_id> [user1,user2,user3]` - List of existing users in the group (excludes self)
- `USERS <group_id>` - Empty user list (only sender in group)
- `MESSAGE_SUMMARY <group_id> <id|sender|date|subject>` - Last 2 messages from the group (one per line, may be 0-2 lines)
- `ERR NOT_LOGGED_IN` - Must login first
- `ERR GROUP_NOT_FOUND` - Invalid group ID or name
- `ERR ALREADY_JOINED` - Already a member of this group

### LEAVE Responses
- `OK LEAVE <group_name>` - Successfully left the group
- `ERR NOT_LOGGED_IN` - Must login first
- `ERR GROUP_NOT_FOUND` - Invalid group ID or name
- `ERR NOT_MEMBER` - Not a member of this group

### MESSAGE Responses
- `OK MESSAGE` - Message posted successfully
- `ERR NOT_LOGGED_IN` - Must login first
- `ERR GROUP_NOT_FOUND` - Invalid group ID
- `ERR NOT_MEMBER` - Not a member of the specified group
- `ERR INVALID_FORMAT` - Invalid command format

### GET_MESSAGE Responses
- `<content>` - Message content text only
- `ERR MESSAGE_NOT_FOUND` - Invalid message ID or user doesn't have access to message (not in group)
- `ERR INVALID_MESSAGE_ID` - Non-numeric ID
- `ERR NOT_LOGGED_IN` - Must login first

### USERS Responses
- `USERS <group_id> [user1,user2,user3]` - List of users in the group (excludes self)
- `USERS <group_id>` - Empty user list (only sender in group)
- `ERR NOT_LOGGED_IN` - Must login first
- `ERR GROUP_NOT_FOUND` - Invalid group ID
- `ERR NOT_MEMBER` - Not a member of the specified group

### GROUPS Responses
- `GROUPS id1:name1,id2:name2,...` - List of all available groups

### QUIT Responses
- `BYE` - Confirmation before disconnect

### Other
- `ERR UNKNOWN_COMMAND` - Invalid command

