# RESTful API for Bows Formula One

## Info

This is a Scala web application using SBT and the PlayFramework. It is built to handle Json API calls to a Mongo database using Reactive Mongo.

To run this project, you will require an instance of Mongo running. You can also test the functionality using an application such as Postman.

As this is a PlayFramework project, I'd recommend running this API on port 9000, using sbt run in your terminal or command line.

To call the Endpoints below, please call them using localhost:9000 followed by the appropriate route below.


## Routes

|Action|Method|Description|Endpoint|
|------|------|-----------|--------|
|Activate Card|GET|Starts session and welcomes the user/deletes session and says Goodbye to the user or returns an error message|/activateCard/:card|
|Find member|GET|Retrieves members information or returns an error message|/findMember/:card|
|Register member (with Json in body)|POST|Registers member or returns an error message|/registerMember|
|Check funds|GET|Returns total funds or returns an error message|/checkFunds:card|
|Add funds|POST|Increases the total funds or returns an error message|/addFunds/:card/:funds|
|Transaction|POST|Decreases the total funds or returns an error message|/transaction/:card/:cost|
|Update name|POST|Updates members name in members collection or returns an error message|/updateName/:card/:newName|
|Update mobile number|POST|Updates members mobile number in members collection or returns an error message|/updateMobileNumber/:card/:newNumber|
|Update mobile number|POST|Updates members email address in members collection or returns an error message|/updateEmail/:card/:newEmail|
|Remove member|POST|Deletes member from database or returns an error message|/removeMember:card|

