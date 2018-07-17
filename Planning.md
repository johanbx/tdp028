
## Planning (made 2018-06-05)
In this course some API:s are required to follow.  
Firebase is also a required and have it's own requirements.  
I should plan for all these API:s in advance  

## TDP028
Instructions: https://www.ida.liu.se/~TDP028/index.sv.shtml  
Examinations: https://www.ida.liu.se/~TDP028/exam/index.sv.shtml  

### API
#### Notifications
Might be able to combine with location.  
When the user/prisoner leaves the prison (my home) a notification pops up to the prioner, saying that the cops are on their way.  
#### Multi Language Support
No specific planning is required for this.
#### Location
Geofencing is required.  
Dark grey background with bars when in prison (my home) and green with vegatation and blue sky when outside the prison.

### Firebase
#### Analythics
Some thoughts:
- Downloads?
- Which page the user is on.
- What buttons the user clicks on.
- When a user gets a notification.
- If the notification is clicked on?
#### Authentication
- Login anonymously
- Google login.
- Facebook login?
- Convert anonymous login to Google or Facebook.
#### Cloud Functions
- Send welcome message by email when user create an account with their email.
#### Invites
- Send dynamic links to people that do not have the app.
#### Remote Config
- Free the prisoner (switch background/text to something like "you are free").
- Kill the prisoner (show gravestone and disable step counter).
#### Performance Monotoring
- Not a real requirements, but check the performance monotoring tab for cool stuff and perhaps optimize the startup of the app.

### Other
- Count steps taken when outside the prison and inside.
