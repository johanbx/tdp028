# TDP028
Instructions: https://www.ida.liu.se/~TDP028/index.sv.shtml  
Examinations: https://www.ida.liu.se/~TDP028/exam/index.sv.shtml  

# The Prison
Welcome to prison! Can you gather the power that is required, break out and run away from the guards?

### API
#### Notifications
Notifications is sent once the prisoner tries to escape (triggers on initial enter geofence)  
Another notification is sent once the prisoner escapes the prison area (trigger on geofence exit)
#### Multi Language Support
Two languages are supported: Swedish and English. The user can go to the settingsmenu to switch between the two languages on the go. The languages are automatically used if the language on the device is either Swedish or English.
#### Location
Location is used to "start" the prisonarea once the user tries to escape. The location is also a requirement for the app to start.
### Firebase
#### Analythics
5 Events are logged:
- The user shares the app
- The user was invited from a dynamic link
- The sound was switched (on/off)
- The user won the game
- The user changed the language (English/Swedish)
#### Authentication
4 Ways of logging in is supported
- All users are automatically logged in as anonymous users (forced)
- The anonymous user can "sync" the data to
  - Google Login
  - Facebook Login
  - Provided Email & Password
#### Cloud Functions
- The oncreate user cloud function is triggered and will send an welcome email to the user
#### Invites
- The user can share the app with an dynamic link. The share message also provides the users current power
#### Remote Config
- An development message is displayed in a textview if it has any value (functional)
- The textcolor on power/steps/pushups/situps can be remotely configurated (theming)
#### Performance Monotoring
- Turned on and displayed in the firebase console.

### Other
- A stepcounter which gives the prisoner power on each step
- A mediaplayer that plays game background music
- A settings menu that can
    - Turn on/off sound 
    - Change the language on the go
    - Delete & Logout the user
