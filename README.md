# How to run RM-diffTool

Go to `src/diffTool/Run.java`

Update the `url` variable and run the code.

Project SDK: Java version 17

Don't forget to update `github-oauth.properties`.

GUI is implemented using ApacheSpark. The Webserver will be running on `port=5678`.

You might run into the problem if you execute two instances of this program on same port.
In case of problems, execute the following commands in cmd:

Find the processID by:

`netstat -ano | findstr :5678`

And then kill the process (update `<PID>` in the following command):

`taskkill /PID <PID> /f`

