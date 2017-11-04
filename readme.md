# Command-line based Chatroom

## Usage

### Launch Service
``` 
java Server
java Client
```

### Chatting

#### General format

|Format|Example|Feature|
|:-:|:-:|:-:|
|Text without any prefixes|Hello!|Broadcast a message|
|Text with one slash as prefix|/login haha|Command|
|Text with two slashes as prefix|//hi|Send preset message|

#### Broadcast

`<message>`

Everyone in the chatroom will receive a message like:

`<username> say to all: <message>`

#### Login

`/login <username>` 

Note:

1. The username must be unique in a chatroom
2. Whitespace is not allowed in a username
3. **All features are all unavailable before login**

#### Private chat
`/to <username> <message>`

Send `<message>` to `<username>`.

Note:

1. `<username>` will receive `<yourname> say to you: <message>`, you will receive `You say to <username>: <message>` if sending succeed.
2. If `<username>` is not existed, you will received `Can't find user <username>.`

#### Chatting history

`/history <begin_index> <end_index>`

Note:
1. `begin_index` and `end_index` is optional. If they are empty, all chatting history will be printed.

#### User list

`/who`

Display all online users.

#### Send preset messages

|Preset Command|Message|
|:-:|:-:|
|`//hi`|`Hi, everyone! I'm coming!`| 
|`//smile`|`There is a smile on <yourname>'s face`|