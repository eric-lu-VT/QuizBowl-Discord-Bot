# QuizBowl-Discord-Bot
A single player simulation of the high school question-and-answer academic competition "Quiz Bowl" (also known as "Scholar's Bowl") using a Discord bot and Google text-to-speech and speech-to-text services.

# Impressions
insert video

# What is Quiz Bowl?

# Commands
- ```$ping```: Checks the bot's response time to Discord.
- ```$help```: Brings up the list of the bot's commands.
- ```$init [filePath]```: Attempts to initiate new round, if there is none in progress, by loading in the packet at the requested file path.
- ```$nexttoss```: Plays the next tossup question, if able to.
- ```$nextbonus```: Plays the next bonus question, if able to.
- ```$buzz```: Stops the question reading and readies the bot to accept an answer. The bot will accept an answer either verbally by saying "answer" followed by your answer, or by typing your answer in the chat with the '$answer' command.
- ```$answer [answer]```: Typing option to answer the question, if able to.

# Installation & Usage
Currently, this is a self-hosted bot — meaning you will need to host and maintain your own instance of it. (Perhaps later, I will maintain a public version of this bot.)

The following are the steps to take to set this bot up yourself:

### Part 1: Discord Bot
1) Download the repository and save it wherever you plan on hosting the bot.
   - Make sure your directory stuff is correct.
      - Gradle should reconfigure most of the directory stuff for you, but you should check the ```build.gradle``` file to make sure it is pointing stuff in the right direction.
      - In line 32 of ```Bot.java```, make sure that the file paths for ```jniLocation:```, ```porcupineLocation:```, ```porcupineParams:```, and ```...wakePhrasePaths:``` are correct specific to your directories.
3) Go to the [Discord Developer Console](https://discord.com/developers/applications) and click "New application".
4) On the left sidebar, select "Bot".
5) Click "Add Bot"
6) Press "Click to Reveal Token" and copy the listed token.
7) Go to ```Bot.java``` and paste the token from Step 5) in place of ```[YOUR BOT TOKEN HERE]```.
8) On the left sidebar, select "OAuth2".
9) Under "Scopes", check off "bot".
10) Under "Bot permissions", select the permissions you wish to give the bot.
   - At minimum, you will need to give the bot the following permissions:
      - ```Connect```
      - ```Speak```
      - ```Use Voice Activity```
      - ```Send Messages```
      - ```Read Message History```
   - Alternatively, you can give the bot ```Administrator``` and be done with it, although depending on the server you might not want to or be allowed to do so.
11) After Step 9), Discord will auto generate a link to you. Go to that address. From there, you will be able to select which server you'd like to add the bot to.
   - To create a brand new server to add the bot to, press the green plus button on the left sidebar on the normal Discord window (```Add a server```), then click ```Create a server```, input whatever server name you want and then finally click ```Create```.

### Part 2: Google APIs
This part of the guide is copied over from [widavies](https://github.com/widavies)' [VocalCord API](https://github.com/widavies/VocalCord) page on GitHub. 

1) Navigate to the [Google Cloud Console](https://console.cloud.google.com/) website.
2) In the lop left, select the projects drop down and create a new project.
3) Once your project is created, click the "APIs & Services" card.
4) From here, select the "Dashboard" tab on the left sidebar, click "Enable APIs and Services."
5) Search for and enable ```Cloud Speech-to-Text API``` and ```Cloud Text-to-Speech API```.
6) On the left sidebar, select "Credentials", then under "Service Accounts", selected "Manage service accounts". Give your service account a name, and everything at its default. You will need to click the "Create Key" button, make sure JSON is selected, and hit "Create". This will download a JSON file. This is your credentials for using Google APIs, keep it secret! Save it to a location where you will remember where it is.
7) On Windows, open the start menu and search "Edit the system environment variables". Click "Environment Variables" and under System Variables, click "New".
   - For "Variable name", enter ```GOOGLE_APPLICATION_CREDENTIALS```.
   - For "Variable value", enter the path to your Google Credentials JSON, for example: ```C:\Users\abcde\Documents\Programming Projects\API.json```. It does not matter where you put this .json file on your system, as long as the PATH points correctly to it.

### Part 3: Using the Bot

1) Download a Quiz Bowl packet as a PDF. I personally find my packets on [quizbowlpackets.com](https://quizbowlpackets.com/); I'm sure you can find other sources online as well.
2) Copy the file location of the PDF you just downloaded (it will be something like ```C:\Users\abcde\Downloads\Packet 01```.)
3) In the Discord server with the bot added to it, join a voice channel and call ```$init [filePath]```, with the file location in the previous step being added in place of ```[filePath]```.
4) From here, you are ready to use the bot.
   - Use ```$nexttoss``` to play the next tossup question in the packet, or ```$nextbonus``` to play the next bonus question in the packet.
   - When you are ready to answer, enter ```$buzz``` in order for the bot to prompt you for your answer.
   - You can answer either by verbally stating "answer" ([your answer]) while unmuted in the voice channel, or by typing your answer in the chat with ```$answer [answer]```.


# Limitations
- The single biggest limitation is that VocalCord does not seem to handle multithreading very well. Essentially what that means is that when the bot is outputting text-to-speech, it cannot be interrupted without stopping the process (ie, the thread) entirely. Once that is done, you can't go back to where you left off in the thread.
  - This makes implementing multiplayer very difficult.
  - This also makes it hard to support more than one instance of the ```Bot``` at a time (which means that it can only handle requests from one server at a time.)
- There are no bonuses, and negs always apply (even if the question has been finished reading.)
- Currently, you have to host the PDF from your local server.

# Roadmap
Upcoming features/changes (hopefully):
- Implement loading Scholar's Bowl packets into the bot directly from website URL, rather than having to download the PDF onto your local machine.
- Fix scoring (add bonuses & fix negs.)
- Add more comments to ```Bot.java```.
- Host a version of this bot myself.

# Credits:
- This bot would not be possible without [DV8FromTheWorld](https://github.com/DV8FromTheWorld)'s [Java Discord API](https://github.com/DV8FromTheWorld/JDA), allowing me to connect my Java code to Discord.
- The backbone of this bot's services was lifted heavily by Google Cloud Platform's [Speech-to-Text](https://cloud.google.com/speech-to-text) and [Text-to-Speech](https://cloud.google.com/text-to-speech) APIs, which was then connected to Discord through [widavies](https://github.com/widavies)' [VocalCord API](https://github.com/widavies/VocalCord). 
- I also used the [Apache PDFBox API](https://pdfbox.apache.org/) to easily convert PDFs to text. 






