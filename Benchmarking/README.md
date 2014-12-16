ab is a tool for benchmarking your server. This especially shows you how many requests per second your server installation is capable of serving.

Steps of usage:

1. Search Tweet API
  ab -n 10000 -c 5000 -r -s 6000 http://192.168.1.2:8083/search?searchString=Hello

2. Send Private Mesaage API
  ab -n 10000 -c 1000 -r -s 6000 'http://192.168.1.4:8084/sendMessage/add?sourceId=u1&targetId=u5&message=howareyou'

3. Add Friendship API
  ab -n 10000 -c 1000 -r -s 6000 'http://192.168.1.4:8084/friendship/add?sourceId=u1&targetId=u2'
  ab -n 10000 -c 1000 -r -s 6000 'http://192.168.1.4:8084/friendship/add?sourceId=u1&targetId=u3'
  ab -n 10000 -c 1000 -r -s 6000 'http://192.168.1.4:8084/friendship/add?sourceId=u2&targetId=u1'

4. Get Followers for the userId API
  ab -n 10000 -c 1000 -r -s 6000 'http://192.168.1.4:8084/followers/ids?userid=u1'

5. Destroy Friendship API
  ab -n 10000 -c 1000 -r -s 6000 'http://192.168.1.4:8084/friendship/destroy?sourceId=u3&targetId=u1'

6. Add Tweet API
  ab -n 10000 -c 1000 -r -s 6000 'http://192.168.1.4:8084/status/add?tweetText=GoodMorning&senderId=u1'

7. Add Usertime line Tweet API
  ab -n 10000 -c 1000 -r -s 6000 'http://192.168.1.4:8084/statuses/user_timeline?id=u1'

8. Add Hometime line Tweet API
  b -n 10000 -c 1000 -r -s 6000 'http://192.168.1.4:8084/statuses/home_timeline?id=u1'
