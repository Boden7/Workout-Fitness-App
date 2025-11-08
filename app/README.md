### Testing Account 
You can use all valid user accounts in users collection. 
I have added two columns `totalTime` and `course` for all valid users. 
For all new created users, they will be created a default value for `totalTime` and `course`.


- email: 1234@gmail.com 
- password: 123123 

### Recent Updates

#### 2025-11-07 by Livia 

##### File Structure



##### New features

- added navigation bar 
- added home fragment 
- updated `CreateAccount.java` to generate default value of `totalTime` and `course`

##### Firestore 

- added tow columns `course` and `totalTime` in users collection 
- updated `isValidUserCreate()` and `isValidUserUpdate()`