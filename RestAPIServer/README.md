URLS Currently Working :


http://localhost:8080/hello

http://localhost:8080/hello2

http://localhost:8080/burn/remaining

http://localhost:8080/sendMesg

To create friendship between two users -
http://localhost:8080/friendship/add?sourceId=u3&targetId=u5

To send message between two users
http://localhost:8080/sendMessage/add?sourceId=u1&targetId=u2&message=howareyou


To destroy friendship between two users
http://localhost:8080/friendship/destroy?sourceId=u1&targetId=u2

Returns a single Tweet, specified by the id parameter. The Tweet?s author will also be embedded within the tweet.
localhost:8080/statuses/show?id=tweetId

Destroys the status specified by the required ID parameter.
http://localhost:8080/statuses/distroy?id=141810096000
