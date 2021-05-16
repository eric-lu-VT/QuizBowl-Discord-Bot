import java.io.*;
import java.util.*;

import com.google.cloud.texttospeech.v1beta1.SsmlVoiceGender;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.managers.AudioManager;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.EmbedBuilder;
import vocalcord.CommandChain;
import vocalcord.UserStream;
import vocalcord.VocalCord;

public class Bot extends ListenerAdapter implements VocalCord.Callbacks {
    private static boolean init = false;

    private static boolean qAsk = false;
    private static Question curQ = null;
    private static boolean paused = false;
    private int score = 0;

    private ArrayDeque<Question> tossup = null;
    private ArrayDeque<Question> bonus = null;

    private final VocalCord cord = VocalCord.newConfig(this).withWakeDetection
            ("C:\\Users\\abcde\\Documents\\Programming Projects\\Discord Bot - QuizBowl 4.0\\native\\linux\\libjni_porcupine.dll",
                    "C:\\Users\\abcde\\Documents\\Programming Projects\\Discord Bot - QuizBowl 4.0\\native\\linux\\libpv_porcupine.dll",
                    "C:\\Users\\abcde\\Documents\\Programming Projects\\Discord Bot - QuizBowl 4.0\\native\\porcupine_params.pv",
                    0.5f,
                    "C:\\Users\\abcde\\Documents\\Programming Projects\\Discord Bot - QuizBowl 4.0\\native\\wake_phrase.ppn").withTTS
            (SsmlVoiceGender.MALE, true).build();

    public static void main(String[] args) throws Exception {
        JDA jda = JDABuilder.createDefault("[YOUR BOT TOKEN HERE]").build();
        jda.getPresence().setStatus(OnlineStatus.IDLE);
        jda.getPresence().setActivity(Activity.watching("QUIZBOWL!!!"));
        jda.addEventListener(new Bot());
    }

    /*
     * This callback defines which users are allowed to access VocalCord.
     * Note, you want to be restrictive on this, especially for large servers,
     * running wake word detection on like 50+ users simultaneously is untested and
     * may affect performance.
     */
    @Override
    public boolean canWakeBot(User user) {
        return true;
    }

    /*
     * This method is called when an authorized user (canWakeBot(..) returned true)
     * woke up the bot, the keywordIndex defines which keyword woke the bot (this depends
     * on the order you specified keywords to when setting up VocalCord) If you only have one
     * keyword, this will be 0. This method is useful for giving the user some feedback that the
     * bot is listening, here for example, the bot will say "Yes?" when it's woken up. Immediately after
     * this call, VocalCord will start generating a voice transcript of what the user said. If you want to cancel
     * voice recognition here, you can call userStream.sleep()
     */
    @Override
    public void onWake(UserStream userStream, int keywordIndex) {
        // cord.say("Yes?");
    }

    /*
     * Note: There are two onTranscribed(..) methods, you should only use one of them (this one is better)
     * This callback is where you'll store all your voice commands. Importantly, voice transcripts aren't always
     * 100% accurate. If you hard code a list of commands, being off by just one word wouldn't register the command,
     * or trying to use lots of String.contains(..) calls could easily intermix commands. This callback employs
     * CommandChain, which will generate document vectors and a document term matrix in order to compute the cosine
     * similarity between a candidate transcription. Essentially, CommandChain will automatically run an algorithm to
     * determine which command was most likely said. This means that a user doesn't have to be 100% accurate on matching a command,
     * and instead only needs to capture the meaning of a command.
     */
    @Override
    public CommandChain onTranscribed() {
        return new CommandChain.Builder().addPhrase("answer", (user, transcript, args) -> {
            if(init && qAsk && paused && transcript.contains(curQ.getAnswer().substring(0, curQ.getAnswer().length() - 1))) {
                cord.say("Correct!");
                score += 10;
            }
            else {
                cord.say("Incorrect. The answer is: " + curQ.getAnswer());
                score -= 5;
            }
            qAsk = false;
            paused = false;
        })
                .addPhrase("knock knock", (user, transcript, args) -> {
                    cord.say("Who's there?");
                }).withFallback(((user, transcript, args) -> {
                    cord.say("I'm sorry, I didn't get that");
                })).withMinThreshold(0.5f).build();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String msg = event.getMessage().getContentRaw();
        if(msg.equals("$ping")) {
            VoiceChannel authorVoiceChannel = event.getMember().getVoiceState().getChannel();
            cord.connect(authorVoiceChannel);
            AudioManager audioManager = event.getGuild().getAudioManager();
            audioManager.openAudioConnection(authorVoiceChannel);

            MessageChannel channel = event.getChannel();
            long time = System.currentTimeMillis();
            channel.sendMessage("PONG!")
                    .queue(response /* => Message */ -> {
                        response.editMessageFormat("PONG! %d ms", System.currentTimeMillis() - time).queue();
                    });
            cord.say("PING PING PING PING PING");
        }
        else if(msg.equals("$help")) {
            EmbedBuilder info = new EmbedBuilder();
            info.setTitle("Help");
            info.setDescription("List of Commands:");
            info.addField("$ping","Checks the bot's response time to Discord.",true);
            info.addField("$help", "Brings up the list of the bot's commands.", false);
            info.addField("$init [filePath]", "Attempts to initiate new round, if there is none in progress, " +
                    "by loading in the packet at the requested file path. ", true);
            info.addField("$nexttoss", "Plays the next tossup question, if able to.", false);
            info.addField("$nextbonus", "Plays the next bonus question, if able to.", false);
            info.addField("$buzz", "Stops the question reading and readies the bot to accept an answer." +
                    " The bot will accept an answer either verbally by saying \"answer\" followed by your answer, " +
                    "or by typing your answer in the chat with the '$answer' command.", true);
            info.addField("$answer [answer]", "Typing option to answer the question, if able to.", false);
            info.setColor(0x99FF00);
            info.setFooter("Bot created by Some1");

            event.getChannel().sendMessage(info.build()).queue();
        }
        else if(msg.length() > 5 && msg.substring(0, 5).equals("$init")) {
            EmbedBuilder info = new EmbedBuilder();
            info.setTitle("Initiate new round...");
            info.setFooter("Requested by " + event.getMember(), event.getMember().getUser().getAvatarUrl());
            if(init) {
                info.setDescription("ERROR: Round already in progress!");
                info.setColor(0xf45642);
            }
            else {
                try {
                    tossup = load1(msg.substring(6));
                    bonus = load2(msg.substring(6));
                    VoiceChannel authorVoiceChannel = event.getMember().getVoiceState().getChannel();
                    cord.connect(authorVoiceChannel);
                    AudioManager audioManager = event.getGuild().getAudioManager();
                    audioManager.openAudioConnection(authorVoiceChannel);
                    cord.say("Voice connected.");

                    init = true;
                    info.setDescription("Download successful!\nReady to Play!");
                    info.setColor(0x99FF00);
                }
                catch(Exception e) {
                    info.setDescription("ERROR: Download failed!");
                    info.setColor(0xf45642);
                }
            }
            event.getChannel().sendMessage(info.build()).queue();
        }
        else if(msg.equals("$nexttoss")) {
            if(!init) {
                event.getChannel().sendMessage("Error: No round in progress.").queue();
            }
            else if(tossup == null || tossup.isEmpty()) {
                event.getChannel().sendMessage("Error: No questions loaded.").queue();
            }
            else if(qAsk) {
                event.getChannel().sendMessage("Error: Question still in progress.").queue();
            }
            else {
                qAsk = true;
                curQ = tossup.pollFirst();
                cord.say(curQ.getQuestion());
            }
        }
        else if(msg.equals("$nextbonus")) {
            if(!init) {
                event.getChannel().sendMessage("Error: No round in progress.").queue();
            }
            else if(bonus == null || bonus.isEmpty()) {
                event.getChannel().sendMessage("Error: No questions loaded.").queue();
            }
            else if(qAsk) {
                event.getChannel().sendMessage("Error: Question still in progress.").queue();
            }
            else {
                qAsk = true;
                curQ = bonus.pollFirst();
                cord.say(curQ.getQuestion());
            }
        }
        else if(msg.equals("$buzz")) {
            if(!init) {
                event.getChannel().sendMessage("Error: No round in progress.").queue();
            }
            else if(!qAsk) {
                event.getChannel().sendMessage("Error: No question in progress.").queue();
            }
            else if(paused) {
                event.getChannel().sendMessage("Error: User has buzzed.").queue();
            }
            else {
                cord.say("Answer?");
                paused = true;
            }
        }
        else if(msg.length() > 7 && msg.substring(0, 7).equals("$answer")) {
            if(!init) {
                event.getChannel().sendMessage("Error: No round in progress.").queue();
            }
            else if(!qAsk) {
                event.getChannel().sendMessage("Error: No question in progress.").queue();
            }
            else if(!paused) {
                event.getChannel().sendMessage("Error: User has buzzed.").queue();
            }
            else {
                if(curQ.getAnswer().contains(msg.substring(8)) && msg.substring(8).length() > 5) {
                    cord.say("Correct!");
                    score += 10;
                }
                else {
                    cord.say("Incorrect. The answer is: " + curQ.getAnswer());
                    score -= 5;
                }
                qAsk = false;
                paused = false;
            }
        }
        else if(msg.equals("$reset")) {
            try {
                event.getGuild().getAudioManager().closeAudioConnection();
                tossup = null;
                init = false;
                qAsk = false;
                paused = false;
                score = 0;
                event.getChannel().sendMessage("Settings reset.").queue();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
        else if(msg.equals("$score")) {
            if(!init) {
                event.getChannel().sendMessage("Error: No round in progress.");
            }
            else {
                event.getChannel().sendMessage("Score: " + score).queue();
            }
        }
        else if(msg.equals("$shutdown")) {
            event.getChannel().sendMessage("Goodbye.").queue();
            System.exit(0);
        }
    }

    private ArrayDeque<Question> load1(String path) throws Exception {
        // String fileName = "C:\Users\ericl\Downloads\Packet 01.pdf"; // provide the path to pdf file
        ArrayDeque<Question> queue = new ArrayDeque<>();

        PDDocument document = PDDocument.load(new File(path));
        PDFTextStripper pdfStripper = new PDFTextStripper();
        String pdfText = pdfStripper.getText(document);
        Scanner in = new Scanner(pdfText);

        while(in.hasNext()) {
            String s = in.next();
            if(s.equals("Tossups")) {
                int i = 1;
                while(in.hasNext()) {
                    s = in.next();
                    if(s.contains(new Integer(i).toString() + ".")) {
                        StringBuilder question = new StringBuilder();
                        StringBuilder answer = new StringBuilder();
                        question.append(new Integer(i).toString() + ". "); // maybe temp

                        s = in.next();
                        while(!s.equals("ANSWER:")) {
                            if (s.contains("*")) {
                                s = in.next();
                                continue;
                            }
                            question.append(s);
                            question.append(" ");
                            s = in.next();
                        }
                        s = in.next();
                        while(!s.contains("[") && !s.contains("<")) {
                            answer.append(s);
                            answer.append(" ");
                            s = in.next();
                        }
                        queue.addLast(new Question(question.toString(), answer.toString(), i));
                        i++;
                    }
                }
            }
        }
        in.close();
        document.close();
        return queue;
    }

    private ArrayDeque<Question> load2(String path) throws Exception {
        // String fileName = "C:\Users\ericl\Downloads\Packet 01.pdf"; // provide the path to pdf file
        ArrayDeque<Question> queue = new ArrayDeque<>();

        PDDocument document = PDDocument.load(new File(path));
        PDFTextStripper pdfStripper = new PDFTextStripper();
        String pdfText = pdfStripper.getText(document);
        Scanner in = new Scanner(pdfText);

        while(in.hasNext()) {
            String s = in.next();
            if(s.equals("Bonuses")) {
                int i = 1;
                while(in.hasNext()) {
                    s = in.next();
                    if(s.contains(new Integer(i).toString() + ".")) {
                        Question[] arr = new Question[3];

                        for(int j = 0; j < 3; j++) {
                            StringBuilder question = new StringBuilder();
                            StringBuilder answer = new StringBuilder();
                            if (j == 0 ) {
                                question.append(new Integer(i).toString() + ". "); // maybe temp
                            }
                            s = in.next();
                            while(!s.equals("ANSWER:")) {
                                if (s.contains("[")) {
                                    s = in.next();
                                    continue;
                                }
                                question.append(s);
                                question.append(" ");
                                s = in.next();
                            }
                            s = in.next();
                            while(!s.contains("[") && !s.contains("<")) {
                                answer.append(s);
                                answer.append(" ");
                                s = in.next();
                            }
                            while(!s.contains("]") && !s.contains(">")) {
                                s = in.next();
                            }
                            arr[j] = new Question(question.toString(), answer.toString(), i);
                        }

                        queue.addLast(arr[0]);
                        queue.addLast(arr[1]);
                        queue.addLast(arr[2]);
                        i++;
                    }
                }
            }
        }
        in.close();
        document.close();
        return queue;
    }
}

class Question implements Comparable<Question> {
    private String question;
    private String answer;
    private int id;

    Question(String question, String answer, int id) {
        this.question = question;
        this.answer = answer;
        this.id = id;
    }

    public String getQuestion() {
        return this.question;
    }

    public String getAnswer() {
        return this.answer;
    }

    public int getId() {
        return this.id;
    }

    public int compareTo(Question o) {
        return this.id - o.getId();
    }
}