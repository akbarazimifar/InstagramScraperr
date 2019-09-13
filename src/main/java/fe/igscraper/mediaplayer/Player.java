package fe.igscraper.mediaplayer;

import com.sun.javafx.application.PlatformImpl;
import javafx.beans.property.*;
import javafx.scene.media.*;
import javafx.beans.value.*;

public class Player {
    private double sum;
    private int count;
    private BooleanProperty finished = new SimpleBooleanProperty();

    private MediaPlayer mp;

    public Player(Media media) {
        PlatformImpl.startup(() -> {});

        this.mp = new MediaPlayer(media);
        this.mp.currentTimeProperty().addListener((v, oldTime, newTime) -> {
            this.sum += newTime.subtract(oldTime).toMillis();
            ++this.count;
            if (this.sum / this.count > this.mp.getStopTime().subtract(newTime).toMillis()) {
                this.finished.set(true);
            }
        });
    }

    public Player(String path) {
        this(new Media(path));
    }

    public Player play() {
        this.mp.play();
        return this;
    }

    public Player addFinishedListener(ChangeListener<Boolean> c) {
        this.finished.addListener(c);
        return this;
    }

    public BooleanProperty onFinished() {
        return this.finished;
    }

    public MediaPlayer getPlayer() {
        return mp;
    }
}
