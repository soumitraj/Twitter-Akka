###Benchmarking using ab - Apache HTTP server benchmarking tool

ab is a tool for benchmarking your server. This especially shows you how many requests per second your server installation is capable of serving.

| API  | AB Command |
| ------------- | ------------- |
| Search Tweet API  |  ```ab -n 10000 -c 5000 -r -s 6000 http://192.168.1.2:8083/search?searchString=Hello```  |
| Send Private Mesaage API  |   ```ab -n 10000 -c 1000 -r -s 6000 'http://192.168.1.4:8084/sendMessage/add?sourceId=u1&targetId=u5&message=howareyou'```  |
| Add Friendship API |```ab -n 10000 -c 1000 -r -s 6000 'http://192.168.1.4:8084/friendship/add?sourceId=u1&targetId=u2'```|
| Add Friendship API |```ab -n 10000 -c 1000 -r -s 6000 'http://192.168.1.4:8084/friendship/add?sourceId=u1&targetId=u3'```|
| Add Friendship API |```ab -n 10000 -c 1000 -r -s 6000 'http://192.168.1.4:8084/friendship/add?sourceId=u2&targetId=u1'```  |
|  Get Followers for the userId API  | ```ab -n 10000 -c 1000 -r -s 6000 'http://192.168.1.4:8084/followers/ids?userid=u1'```  |
| Destroy Friendship API  | ```ab -n 10000 -c 1000 -r -s 6000 'http://192.168.1.4:8084/friendship/destroy?sourceId=u3&targetId=u1'```  |
| Add Tweet API  | ```ab -n 10000 -c 1000 -r -s 6000 'http://192.168.1.4:8084/status/add?tweetText=GoodMorning&senderId=u1'```  |
| Add Usertime line Tweet API  | ```ab -n 10000 -c 1000 -r -s 6000 'http://192.168.1.4:8084/statuses/user_timeline?id=u1'```  |
|  Add Hometime line Tweet API  | ```b -n 10000 -c 1000 -r -s 6000 'http://192.168.1.4:8084/statuses/home_timeline?id=u1'```  |
