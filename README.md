# SMS Gateway
This app turns Android device into SMS gateway which allows sending SMS through Android device using `HTTP` requests over local Wi-Fi network or by directly connecting to the device's **Hotspot**

![demo](https://user-images.githubusercontent.com/35717992/195159994-c2906ede-8c43-405c-8c0b-a0a91ee77b18.gif)


# Usage
To send SMS, HTTP client must provide `phone` and `message` parameters to path `/sendSMS` via `POST` method

# Example

Using Node Js a simple http `POST` request would be :-
```javascript
const request = require('request');


request.post({
  url: 'http://192.168.0.101:8081/sendSMS',
  form: {
    phone: '03475144819',
    message: 'Hello World !'
  }
}, function (err, httpResponse, body) { 

    console.log(body);

 })
```
# HTTP status code return by server

|Code|Description|
|----|-----------|
|200 (OK)| When SMS is successfully sent  |
|400 (BAD REQUEST)| When either `phone`,`message` or `password` parameter is missing|
|405 (METHOD NOT ALLOWED)| When `POST` method is not used|
|415 (UN SUPPORTED MEDIA TYPE)| When `Content-Type` sent by client is not `application/x-www-form-urlencoded`|
|404 (NOT FOUND)| When `/sendSMS` is not used|
|401 (UNAUTHORIZED)| When client provides invalid `password`|
|403 (FORBIDDEN)| When app has no permission to send SMS |
|500 (INTERNAL SERVER ERROR)| When some exception occur while sending SMS |


# Note
As per Android offical docs https://developer.android.com/about/versions/kitkat/android-4.4#SMS 
>Beginning with Android 4.4, the system settings allow users to select a "default SMS app." Once selected, only the default SMS app is able to write to the SMS Provider and only the default SMS app receives the SMS_DELIVER_ACTION broadcast when the user receives an SMS

[SMS_DELIVER_ACTION](https://developer.android.com/reference/android/provider/Telephony.Sms.Intents#SMS_DELIVER_ACTION) is intent which is broadcast by Android OS to apps when delivery report arrives from SMSC (sms center)

So, Android SMS server app (non default sms app) has no way to notify http clients about whether delivery was successfull or not. It can only tell whether sms was successfully sent or not. 


# Download APK
Download latest APK from [release section](https://github.com/umer0586/AndroidSMSServer/releases) *(requires Android 5.0)*.
