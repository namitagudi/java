package com.escapethecode;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class EscapeTheCodeApp extends Application {
    private static final String BG = "#101427", PANEL = "#1a2038", PANEL2 = "#202943";
    private static final String TEXT = "#eef2ff", MUTED = "#9ba9c8", CYAN = "#66e7ff", PURPLE = "#a98bff";
    private final List<Room> rooms = List.of(
        new Room("01", "LOCKED LABORATORY", "LOGIC / PATTERNS", "Beginner", "A strange sequence is etched into the lab door.", "What number comes next?   2   •   4   •   8   •   16   •   ?", "32", "The sequence doubles each time: 2, 4, 8, 16, 32.", "LAB KEY"),
        new Room("02", "FORGOTTEN SERVER", "JAVA / OUTPUT", "Intermediate", "A terminal is blocking the server access.", "What is printed?   int x = 3;  System.out.println(x * 2 + 1);", "7", "Multiplication happens first: 3 × 2 + 1 = 7.", "SERVER KEY"),
        new Room("03", "CIPHER VAULT", "DECODING / CIPHER", "Intermediate", "A message is encrypted on the vault wall.", "Decode this Caesar shift (-1):   DBU", "CAT", "Each letter shifts one step backward: D→C, B→A, U→T.", "VAULT KEY")
    );
    private final Set<Integer> unlocked = new HashSet<>(Set.of(0));
    private final Set<Integer> cleared = new HashSet<>();
    private int xp = 0, hints = 2, current = 0;
    private Label xpLabel, roomStatus, timerLabel;
    private ProgressBar xpBar;
    private Timeline timer;
    private int seconds = 180;
    private Stage stage;

    @Override public void start(Stage stage) {
        this.stage = stage;
        loadProgress();
        showHome();
        stage.setTitle("Escape the Code — Think. Decode. Escape.");
        stage.setMinWidth(1000); stage.setMinHeight(680);
        stage.setWidth(1280); stage.setHeight(820);
        stage.show();
    }

    private void showHome() {
        BorderPane root = base();
        VBox sidebar = sidebar();
        root.setLeft(sidebar);
        VBox content = new VBox(20);
        content.setPadding(new Insets(30, 34, 30, 34));
        HBox header = new HBox(16, label("MISSION CONTROL", 12, CYAN, true), spacer(), pill("PLAYER 01", PURPLE));
        header.setAlignment(Pos.CENTER_LEFT);
        VBox hero = new VBox(12);
        hero.setPadding(new Insets(28));
        hero.setBackground(gradientPanel());
        hero.setBorder(border("#3d4a72", 18));
        Label title = label("Your escape begins here.", 31, TEXT, true);
        Label subtitle = label("Explore rooms. Crack puzzles. Collect keys. Escape the system.", 14, MUTED, false);
        HBox heroStats = new HBox(12, stat("LEVEL", cleared.size()+1), stat("XP", xp), stat("HINT TOKENS", hints));
        hero.getChildren().addAll(label("WELCOME BACK, PLAYER", 11, CYAN, true), title, subtitle, heroStats);
        VBox map = new VBox(14);
        HBox mapHead = new HBox(label("THE ESCAPE MAP", 17, TEXT, true), spacer(), label(cleared.size()+"/3 ROOMS CLEARED", 11, CYAN, true));
        mapHead.setAlignment(Pos.CENTER_LEFT);
        HBox roomCards = new HBox(14);
        for (int i=0;i<rooms.size();i++) roomCards.getChildren().add(roomCard(i));
        map.getChildren().addAll(mapHead, roomCards);
        VBox bottom = new VBox(12);
        bottom.setPadding(new Insets(22));
        bottom.setBackground(panelBg());
        bottom.setBorder(border("#303c5d", 16));
        Label btitle = label("HOW TO PLAY", 15, TEXT, true);
        HBox steps = new HBox(20, step("01", "Explore", "Choose an unlocked room"), step("02", "Solve", "Crack the puzzle before time runs out"), step("03", "Collect", "Earn XP and room keys"), step("04", "Escape", "Clear all rooms to win"));
        bottom.getChildren().addAll(btitle, steps);
        content.getChildren().addAll(header, hero, map, bottom);
        root.setCenter(content);
        Scene scene = new Scene(root, 1280, 820);
        stage.setScene(scene);
    }

    private VBox roomCard(int i) {
        Room r = rooms.get(i);
        boolean isUnlocked = unlocked.contains(i);
        boolean isCleared = cleared.contains(i);
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setMinWidth(230); card.setPrefWidth(270); card.setMaxWidth(Double.MAX_VALUE);
        card.setBackground(panelBg());
        card.setBorder(border(isCleared ? "#44d6b2" : isUnlocked ? "#6979b9" : "#343c56", 16));
        Label num = label("ROOM " + r.id, 11, isUnlocked?CYAN:MUTED, true);
        Label icon = label(isCleared ? "✓" : isUnlocked ? "◈" : "🔒", 27, isCleared?"#44d6b2":PURPLE, true);
        Label name = label(r.name, 15, TEXT, true);
        Label type = label(r.category, 10, MUTED, true);
        Label description = label(r.description, 12, MUTED, false);
        description.setWrapText(true);
        Button action = button(isCleared ? "REPLAY ROOM" : isUnlocked ? "ENTER ROOM  →" : "LOCKED", isUnlocked);
        action.setMaxWidth(Double.MAX_VALUE);
        if(isUnlocked) action.setOnAction(e -> openRoom(i));
        card.getChildren().addAll(num, icon, name, type, description, action);
        return card;
    }

    private void openRoom(int index) {
        current = index; seconds = 180;
        Room r = rooms.get(index);
        BorderPane root = base();
        root.setLeft(sidebar());
        VBox game = new VBox(18);
        game.setPadding(new Insets(26));
        HBox top = new HBox(14, button("← MAP", true), label("ROOM " + r.id + "  /  " + r.name, 17, TEXT, true), spacer(), pill(r.difficulty.toUpperCase(), PURPLE), timerLabel = label("03:00", 20, CYAN, true));
        ((Button)top.getChildren().get(0)).setOnAction(e->{stopTimer();showHome();});
        top.setAlignment(Pos.CENTER_LEFT);
        VBox stageCard = new VBox(18);
        stageCard.setPadding(new Insets(28));
        stageCard.setBackground(gradientPanel());
        stageCard.setBorder(border("#465582", 20));
        Label sceneTitle = label("MISSION BRIEF", 11, CYAN, true);
        Label desc = label(r.description, 15, MUTED, false); desc.setWrapText(true);
        Label puzzle = label(r.puzzle, 22, TEXT, true); puzzle.setWrapText(true);
        TextField answer = new TextField();
        answer.setPromptText("Enter your answer to unlock the exit…");
        answer.setStyle("-fx-background-color:#10182d;-fx-text-fill:#f3f5ff;-fx-prompt-text-fill:#7887a8;-fx-background-radius:10;-fx-border-color:#465582;-fx-border-radius:10;-fx-padding:14;-fx-font-size:14px;");
        Label feedback = label("Your answer is your key.", 13, MUTED, false);
        HBox controls = new HBox(12);
        Button submit = button("SUBMIT ANSWER  →", true);
        Button hint = button("USE HINT  ("+hints+")", false);
        controls.getChildren().addAll(submit, hint);
        Label answerGuide = label("Tip: Think it through. You have 3 minutes.", 12, MUTED, false);
        stageCard.getChildren().addAll(sceneTitle, desc, puzzle, answer, controls, feedback, answerGuide);
        VBox inventory = new VBox(10);
        inventory.setPadding(new Insets(18));
        inventory.setBackground(panelBg()); inventory.setBorder(border("#34415f", 14));
        Label invTitle = label("INVENTORY", 13, TEXT, true);
        Label keyInfo = label(cleared.contains(index) ? "✓ " + r.key : "◌  No key collected yet", 13, cleared.contains(index)?"#44d6b2":MUTED, true);
        Label hintInfo = label("Hint tokens: "+hints, 12, MUTED, false);
        inventory.getChildren().addAll(invTitle,keyInfo,hintInfo);
        submit.setOnAction(e -> {
            String value=answer.getText().trim().replaceAll("\\s+","");
            if(value.equalsIgnoreCase(r.answer.replaceAll("\\s+",""))) {
                if(!cleared.contains(index)) { cleared.add(index); xp+=100; if(index+1<rooms.size()) unlocked.add(index+1); saveProgress(); }
                feedback.setText("ACCESS GRANTED — "+r.key+" collected! +100 XP");
                feedback.setTextFill(Color.web("#44d6b2"));
                answer.setDisable(true); submit.setDisable(true);
                keyInfo.setText("✓ "+r.key); keyInfo.setTextFill(Color.web("#44d6b2"));
                hintInfo.setText("Hint tokens: "+hints);
                Button next=button(index==rooms.size()-1?"FINISH ESCAPE  →":"NEXT ROOM  →", true);
                next.setOnAction(ev->{stopTimer();showHome();});
                controls.getChildren().add(next);
            } else { feedback.setText("ACCESS DENIED — check your answer and try again."); feedback.setTextFill(Color.web("#ff829b")); }
        });
        hint.setOnAction(e -> {
            if(hints<=0){feedback.setText("No hint tokens left. Solve it yourself!");feedback.setTextFill(Color.web("#ffbe69"));return;}
            hints--; hint.setText("USE HINT ("+hints+")"); hintInfo.setText("Hint tokens: "+hints);
            feedback.setText("HINT: "+r.hint); feedback.setTextFill(Color.web("#ffbe69"));
            saveProgress();
        });
        game.getChildren().addAll(top, stageCard, inventory);
        root.setCenter(game);
        stage.setScene(new Scene(root, 1280, 820));
        startTimer();
    }

    private void startTimer() {
        stopTimer();
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            seconds--;
            if(timerLabel!=null) timerLabel.setText(String.format("%02d:%02d",seconds/60,seconds%60));
            if(seconds<=0){stopTimer(); Alert a=new Alert(Alert.AlertType.INFORMATION,"Time's up! The room remains available—take a breath and try again.");a.setHeaderText("MISSION TIMER");a.showAndWait();showHome();}
        }));
        timer.setCycleCount(Timeline.INDEFINITE);timer.play();
    }
    private void stopTimer(){if(timer!=null)timer.stop();}

    private VBox sidebar() {
        VBox side = new VBox(18);
        side.setPadding(new Insets(26,18,24,18));
        side.setPrefWidth(220);
        side.setBackground(new Background(new BackgroundFill(Color.web("#151b30"),CornerRadii.EMPTY,Insets.EMPTY)));
        Label logo = label("⌁  ESCAPE\n     THE CODE", 23, TEXT, true);
        Label tagline = label("THINK  •  DECODE  •  ESCAPE", 9, CYAN, true);
        Button play = button("▶  MISSION MAP", true); play.setMaxWidth(Double.MAX_VALUE); play.setOnAction(e->showHome());
        VBox profile = new VBox(6, label("PLAYER 01",12,TEXT,true),label("Level "+(cleared.size()+1)+" explorer",11,MUTED,false),xpLabel=label("XP  "+xp,12,CYAN,true));
        profile.setPadding(new Insets(16));profile.setBackground(panelBg());profile.setBorder(border("#33415f",12));
        VBox menu = new VBox(12,label("MISSIONS",10,MUTED,true),play,label("◈  Room progression",12,TEXT,false),label("◇  Puzzle archive",12,MUTED,false),label("✦  Achievements",12,MUTED,false));
        Button reset=button("RESET GAME",false);reset.setMaxWidth(Double.MAX_VALUE);
        reset.setOnAction(e->{cleared.clear();unlocked.clear();unlocked.add(0);xp=0;hints=2;saveProgress();showHome();});
        side.getChildren().addAll(logo,tagline,new Separator(),profile,menu,spacer(),reset,label("ESCAPE THE CODE  •  JAVA FX",9,MUTED,true));
        return side;
    }
    private VBox step(String n,String title,String desc){VBox v=new VBox(5,label(n,12,CYAN,true),label(title,13,TEXT,true),label(desc,11,MUTED,false));v.setPrefWidth(180);return v;}
    private VBox stat(String title,int val){return new VBox(3,label(title,10,MUTED,true),label(String.valueOf(val),18,TEXT,true));}
    private Button button(String text,boolean primary){
        Button b=new Button(text);
        String bg=primary?"#6658db":"#27314b";
        b.setStyle("-fx-background-color:"+bg+";-fx-text-fill:#f7f8ff;-fx-font-weight:bold;-fx-font-size:11px;-fx-padding:12 16;-fx-background-radius:10;-fx-border-color:"+(primary?"#988aff":"#465582")+";-fx-border-radius:10;");
        return b;
    }
    private Label label(String text,double size,String color,boolean bold){
        Label l=new Label(text);l.setTextFill(Color.web(color));l.setFont(Font.font("System",bold?FontWeight.BOLD:FontWeight.NORMAL,size));return l;
    }
    private Region spacer(){Region r=new Region();HBox.setHgrow(r,Priority.ALWAYS);return r;}
    private HBox pill(String text,String color){Label l=label(text,10,color,true);HBox h=new HBox(l);h.setPadding(new Insets(7,12,7,12));h.setBackground(new Background(new BackgroundFill(Color.web("#242d49"),new CornerRadii(18),Insets.EMPTY)));h.setBorder(border("#45547c",18));return h;}
    private BorderPane base(){BorderPane p=new BorderPane();p.setBackground(new Background(new BackgroundFill(Color.web(BG),CornerRadii.EMPTY,Insets.EMPTY)));return p;}
    private Background panelBg(){return new Background(new BackgroundFill(Color.web(PANEL),new CornerRadii(16),Insets.EMPTY));}
    private Background gradientPanel(){return new Background(new BackgroundFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#202b4b")),new Stop(1,Color.web("#292346"))),new CornerRadii(18),Insets.EMPTY));}
    private Border border(String color,double radius){return new Border(new BorderStroke(Color.web(color),BorderStrokeStyle.SOLID,new CornerRadii(radius),new BorderWidths(1)));}
    private void saveProgress(){
        try{Properties p=new Properties();p.setProperty("xp",String.valueOf(xp));p.setProperty("hints",String.valueOf(hints));p.setProperty("cleared",String.join(",",cleared.stream().sorted().map(String::valueOf).toList()));p.setProperty("unlocked",String.join(",",unlocked.stream().sorted().map(String::valueOf).toList()));
            var dir=java.nio.file.Path.of(System.getProperty("user.home"),".escape-the-code");java.nio.file.Files.createDirectories(dir);try(var out=java.nio.file.Files.newOutputStream(dir.resolve("progress.properties"))){p.store(out,"Escape the Code progress");}
        }catch(Exception ignored){}
    }
    private void loadProgress(){
        try{var path=java.nio.file.Path.of(System.getProperty("user.home"),".escape-the-code","progress.properties");if(!java.nio.file.Files.exists(path))return;Properties p=new Properties();try(var in=java.nio.file.Files.newInputStream(path)){p.load(in);}xp=Integer.parseInt(p.getProperty("xp","0"));hints=Integer.parseInt(p.getProperty("hints","2"));readSet(p.getProperty("cleared",""),cleared);readSet(p.getProperty("unlocked","0"),unlocked);if(unlocked.isEmpty())unlocked.add(0);
        }catch(Exception ignored){}
    }
    private void readSet(String raw,Set<Integer> set){for(String s:raw.split(","))try{if(!s.isBlank())set.add(Integer.parseInt(s));}catch(Exception ignored){}}
    @Override public void stop(){stopTimer();}
    public static void main(String[] args){launch(args);}
    private record Room(String id,String name,String category,String difficulty,String description,String puzzle,String answer,String hint,String key){}
}
